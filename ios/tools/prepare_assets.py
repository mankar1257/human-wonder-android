"""Turn the Android app's assets into the iPhone app's (run once after they change; outputs are committed).

    python3 ios/tools/prepare_assets.py          (from the repo root; needs fonttools and rsvg-convert)

- Fonts: the variable Geist / Newsreader / JetBrains Mono files become static weights with their own
  PostScript names (HWGeist-Light ...), which iOS loads reliably by name.
- Icons: every Android vector drawable becomes a 72 px PNG (24 pt at 3x), tinted in code, so the
  iPhone app draws exactly the same Material Symbols and Human Wonder marks.
- Photos, the app icon (the Android launcher mark on its background colour) and the licence texts.
No asset catalog: compiling one makes Xcode demand an 8 GB simulator download.
"""
import re
import shutil
import subprocess
import xml.etree.ElementTree as ET
from pathlib import Path

from fontTools.ttLib import TTFont
from fontTools.varLib import instancer

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / "app/src/main/res"
APP = ROOT / "ios/HumanWonder"
RESOURCES = APP / "Resources"
A = "{http://schemas.android.com/apk/res/android}"

FONTS = [
    # (source, family, style, axes)
    ("geist.ttf", "HWGeist", "Light", {"wght": 300}),
    ("geist.ttf", "HWGeist", "Regular", {"wght": 400}),
    ("geist.ttf", "HWGeist", "Medium", {"wght": 500}),
    ("geist.ttf", "HWGeist", "SemiBold", {"wght": 600}),
    ("newsreader.ttf", "HWNewsreader", "Regular", {"wght": 400, "opsz": 16}),
    ("jetbrains_mono.ttf", "HWMono", "Regular", {"wght": 400}),
    ("jetbrains_mono.ttf", "HWMono", "Medium", {"wght": 500}),
]


def fonts():
    out = APP / "Resources/Fonts"
    out.mkdir(parents=True, exist_ok=True)
    names = []
    for src, family, style, axes in FONTS:
        font = instancer.instantiateVariableFont(TTFont(RES / "font" / src), axes)
        ps = f"{family}-{style}"
        name = font["name"]
        for rec in list(name.names):
            if rec.nameID in (1, 2, 3, 4, 6, 16, 17, 25):
                name.removeNames(nameID=rec.nameID)
        for nid, value in ((1, family), (2, style), (3, f"{ps};humanwonder"), (4, f"{family} {style}"), (6, ps)):
            name.setName(value, nid, 3, 1, 0x409)
            name.setName(value, nid, 1, 0, 0)
        for t in ("STAT",):
            if t in font:
                del font[t]
        file = f"{ps}.ttf"
        font.save(out / file)
        names.append(file)
    return names


def vector_to_svg(xml_path: Path) -> str:
    root = ET.parse(xml_path).getroot()
    vw = float(root.get(A + "viewportWidth"))
    vh = float(root.get(A + "viewportHeight"))

    def color(value):
        if not value or value.startswith("@"):
            return "none"
        v = value.lstrip("#")
        if len(v) == 8:
            a, rgb = int(v[:2], 16) / 255, v[2:]
        else:
            a, rgb = 1.0, v[-6:]
        return "none" if a == 0 else f"#{rgb}"

    def walk(el):
        parts = []
        for child in el:
            tag = child.tag
            if tag == "group":
                tx = child.get(A + "translateX", "0")
                ty = child.get(A + "translateY", "0")
                sx = child.get(A + "scaleX", "1")
                sy = child.get(A + "scaleY", "1")
                parts.append(f'<g transform="translate({tx},{ty}) scale({sx},{sy})">{walk(child)}</g>')
            elif tag == "path":
                d = child.get(A + "pathData")
                fill = color(child.get(A + "fillColor"))
                stroke = color(child.get(A + "strokeColor"))
                attrs = [f'd="{d}"', f'fill="{fill}"']
                if stroke != "none":
                    attrs.append(f'stroke="{stroke}"')
                    attrs.append(f'stroke-width="{child.get(A + "strokeWidth", "1")}"')
                    attrs.append(f'stroke-linecap="{child.get(A + "strokeLineCap", "butt")}"')
                    attrs.append(f'stroke-linejoin="{child.get(A + "strokeLineJoin", "miter")}"')
                alpha = child.get(A + "fillAlpha")
                if alpha:
                    attrs.append(f'fill-opacity="{alpha}"')
                if child.get(A + "fillType") == "evenOdd":
                    attrs.append('fill-rule="evenodd"')
                parts.append(f"<path {' '.join(attrs)}/>")
        return "".join(parts)

    return (
        f'<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 {vw:g} {vh:g}">'
        f"{walk(root)}</svg>"
    )


def icons():
    tmp = ROOT / "ios/build-tmp"
    tmp.mkdir(exist_ok=True)
    out = RESOURCES / "Icons"
    out.mkdir(parents=True, exist_ok=True)
    count = 0
    for xml in sorted((RES / "drawable").glob("ic_*.xml")):
        if xml.stem == "ic_launcher_foreground":
            continue
        svg = tmp / f"{xml.stem}.svg"
        svg.write_text(vector_to_svg(xml))
        subprocess.run(["rsvg-convert", "-w", "72", "-h", "72", "-o", str(out / f"{xml.stem}@3x.png"), str(svg)], check=True)
        count += 1
    return count


def photos():
    out = RESOURCES / "Photos"
    out.mkdir(parents=True, exist_ok=True)
    for name in ("bg_liwa_dunes", "bg_earth_orbit"):
        shutil.copyfile(RES / "drawable-nodpi" / f"{name}.jpg", out / f"{name}.jpg")


def app_icon():
    tmp = ROOT / "ios/build-tmp"
    fg = vector_to_svg(RES / "drawable/ic_launcher_foreground.xml")
    bg = re.search(r'name="ic_launcher_background">#(?:FF)?([0-9A-Fa-f]{6})', (RES / "values/colors.xml").read_text()).group(1)
    # The adaptive icon's 108dp canvas shows its middle 72dp; iOS shows the whole square.
    inner = fg.replace('width="24" height="24" viewBox="0 0 108 108"', 'width="1024" height="1024" viewBox="18 18 72 72"')
    svg = inner.replace(">", f'><rect x="0" y="0" width="108" height="108" fill="#{bg}"/>', 1)
    (tmp / "icon.svg").write_text(svg)
    out = RESOURCES / "AppIcon"
    out.mkdir(parents=True, exist_ok=True)
    for name, px in (("AppIcon60x60@2x.png", 120), ("AppIcon60x60@3x.png", 180)):
        subprocess.run(["rsvg-convert", "-w", str(px), "-h", str(px), "-o", str(out / name), str(tmp / "icon.svg")], check=True)


def licences():
    out = APP / "Resources/licenses"
    out.mkdir(parents=True, exist_ok=True)
    for f in (ROOT / "app/src/main/assets/licenses").glob("*.txt"):
        shutil.copyfile(f, out / f.name)


if __name__ == "__main__":
    shutil.rmtree(APP / "Assets.xcassets", ignore_errors=True)
    print("fonts:", fonts())
    print("icons:", icons())
    photos()
    app_icon()
    licences()
    shutil.rmtree(ROOT / "ios/build-tmp", ignore_errors=True)
    print("done")
