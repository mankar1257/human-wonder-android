"""Turn the downloaded NASA frames (media-src/, from "Get Orbit Photos.command") into the app's files.

    python3 tools/frames/build_assets.py [media-src folder]

Writes app/src/main/assets/frames/ (the iPhone app bundles the same folder):
  <id>.jpg   photos, 1440 px on the long side, JPEG quality 82, no metadata
  <id>.webp  loops, 720 x 480, about 12 frames a second, the last frames cross-faded into the first
             so the loop has no jump
  frames.json the catalog (tools/frames/frames.json without the build-only fields)
and the credit notice NOTICE-orbit-archive.txt into both apps' licence folders.

Per-frame options in tools/frames/frames.json (build only, not shipped):
  "crop": [left, top, right, bottom] as fractions of the original, applied before resizing
  "loop": {"dir": "<media-src/loops folder>", "first": n, "last": n, "step": n, "fps": 12,
           "mode": "pingpong" to play forward then back instead of cross-fading the ends}
  "enhance": {"brightness": x, "contrast": x, "color": x} for dim night frames
"""
import json
import sys
from pathlib import Path

from PIL import Image, ImageEnhance

ROOT = Path(__file__).resolve().parents[2]
CATALOG = ROOT / "tools/frames/frames.json"
OUT = ROOT / "app/src/main/assets/frames"
LICENCE_DIRS = [ROOT / "app/src/main/assets/licenses", ROOT / "ios/HumanWonder/Resources/licenses"]
BUILD_ONLY = ("crop", "loop", "enhance")
PHOTO_LONG_SIDE = 1440
LOOP_SIZE = (720, 480)
CROSSFADE = 8


def crop_to(img: Image.Image, frac, aspect=None) -> Image.Image:
    w, h = img.size
    if frac:
        l, t, r, b = frac
        img = img.crop((round(l * w), round(t * h), round(r * w), round(b * h)))
        w, h = img.size
    if aspect:
        if w / h > aspect:
            nw = round(h * aspect)
            img = img.crop(((w - nw) // 2, 0, (w - nw) // 2 + nw, h))
        else:
            nh = round(w / aspect)
            img = img.crop((0, (h - nh) // 2, w, (h - nh) // 2 + nh))
    return img


def enhance(img: Image.Image, opts) -> Image.Image:
    if not opts:
        return img
    if "brightness" in opts:
        img = ImageEnhance.Brightness(img).enhance(opts["brightness"])
    if "contrast" in opts:
        img = ImageEnhance.Contrast(img).enhance(opts["contrast"])
    if "color" in opts:
        img = ImageEnhance.Color(img).enhance(opts["color"])
    return img


def photo(frame, media: Path) -> Path:
    src = media / "stills" / f"{frame['source']}.jpg"
    img = Image.open(src).convert("RGB")
    img = enhance(crop_to(img, frame.get("crop")), frame.get("enhance"))
    img.thumbnail((PHOTO_LONG_SIDE, PHOTO_LONG_SIDE), Image.LANCZOS)
    out = OUT / f"{frame['id']}.jpg"
    img.save(out, "JPEG", quality=82, optimize=True, progressive=True)
    return out


def loop(frame, media: Path) -> Path:
    spec = frame["loop"]
    folder = media / "loops" / spec["dir"]
    mission = frame["source"].split("-E-")[0]
    files = [folder / f"{mission}-E-{n}.jpg" for n in range(spec["first"], spec["last"] + 1, spec.get("step", 1))]
    files = [f for f in files if f.is_file()]
    if len(files) < CROSSFADE * 2 + 4:
        raise SystemExit(f"{frame['id']}: only {len(files)} frames in {folder}")
    aspect = LOOP_SIZE[0] / LOOP_SIZE[1]
    imgs = []
    for f in files:
        img = Image.open(f).convert("RGB")
        img = enhance(crop_to(img, frame.get("crop"), aspect), frame.get("enhance"))
        imgs.append(img.resize(LOOP_SIZE, Image.LANCZOS))
    if spec.get("mode") == "pingpong":
        # Forward then back: no seam at all, for scenes where a cross-fade would show a ghost.
        imgs = imgs + imgs[-2:0:-1]
    else:
        # Seamless: the last CROSSFADE frames dissolve into the first ones, which are then dropped.
        n = len(imgs)
        for i in range(CROSSFADE):
            a = (i + 1) / (CROSSFADE + 1)
            imgs[n - CROSSFADE + i] = Image.blend(imgs[n - CROSSFADE + i], imgs[i], a)
        imgs = imgs[CROSSFADE:]
    fps = spec.get("fps", 12)
    out = OUT / f"{frame['id']}.webp"
    imgs[0].save(out, "WEBP", save_all=True, append_images=imgs[1:], duration=round(1000 / fps), loop=0,
                 quality=70, method=6, minimize_size=True)
    return out


def notice(catalog) -> str:
    lines = [
        "Orbit's archive: photographs of Earth taken by astronauts on the International Space Station.",
        "",
        catalog["credit"] + ".",
        "Source: NASA/JSC Gateway to Astronaut Photography of Earth, https://eol.jsc.nasa.gov",
        "NASA material is not protected by copyright in the United States. Its use here does not imply",
        "endorsement by NASA. Photos were resized and cropped; loops were made from consecutive frames of",
        "the station's time-lapse sequences.",
        "",
        "Frames used:",
    ]
    for f in catalog["frames"]:
        lines.append(f"  {f['caption']}: {f['source']}")
    return "\n".join(lines) + "\n"


def main():
    media = Path(sys.argv[1]) if len(sys.argv) > 1 else ROOT / "media-src"
    catalog = json.loads(CATALOG.read_text(encoding="utf-8"))
    OUT.mkdir(parents=True, exist_ok=True)
    total = 0
    for f in catalog["frames"]:
        out = loop(f, media) if f["kind"] == "loop" else photo(f, media)
        size = out.stat().st_size
        total += size
        print(f"{out.name:28s} {size / 1024:8.0f} KB")
    shipped = dict(catalog)
    shipped["frames"] = [{k: v for k, v in f.items() if k not in BUILD_ONLY} for f in catalog["frames"]]
    (OUT / "frames.json").write_text(json.dumps(shipped, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
    text = notice(catalog)
    for d in LICENCE_DIRS:
        d.mkdir(parents=True, exist_ok=True)
        (d / "NOTICE-orbit-archive.txt").write_text(text, encoding="utf-8")
    print(f"total {total / 1024 / 1024:.1f} MB in {OUT.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
