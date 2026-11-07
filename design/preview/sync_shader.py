"""Copy the sky shader from the Android app into the preview's chat artboards.

GalaxyBackground.kt is the single source of truth for the shader; run this after changing it,
then republish Chat.dc.html and Transmission.dc.html to the preview canvas.
"""
import pathlib
import re

HERE = pathlib.Path(__file__).resolve().parent
ROOT = HERE.parents[1]
kt = (ROOT / "app/src/main/java/ai/humanwonder/poc/ui/GalaxyBackground.kt").read_text()
frag = kt.split('const val FRAGMENT_SHADER = """', 1)[1].split('"""', 1)[0].strip()
for name in ["Chat.dc.html", "Transmission.dc.html"]:
    path = HERE / "project" / name
    src = path.read_text()
    out, n = re.subn(r"var fsSrc = `[\s\S]*?`;", lambda m: "var fsSrc = `" + frag + "`;", src, count=1)
    if n != 1:
        raise SystemExit(f"shader block not found in {name}")
    path.write_text(out)
    print(f"{name}: shader {'updated' if out != src else 'already in sync'}")
