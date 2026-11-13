"""Contact sheets of the downloaded time-lapse frames, to choose where each loop starts and ends.

    python3 tools/frames/contact_sheet.py <media-src/sheets folder> <out folder>
One PNG per sequence: every downloaded frame as a small tile labelled with its frame number.
"""
import sys
from pathlib import Path

from PIL import Image, ImageDraw


def sheet(folder: Path, out: Path, cols=8, tile=(240, 160)):
    files = sorted(folder.glob("*.jpg"), key=lambda p: int(p.stem.split("-E-")[1]))
    if not files:
        return None
    rows = (len(files) + cols - 1) // cols
    img = Image.new("RGB", (cols * tile[0], rows * (tile[1] + 18)), "black")
    d = ImageDraw.Draw(img)
    for i, f in enumerate(files):
        try:
            t = Image.open(f).convert("RGB")
        except OSError:
            continue
        t.thumbnail(tile)
        x, y = (i % cols) * tile[0], (i // cols) * (tile[1] + 18)
        img.paste(t, (x, y + 18))
        d.text((x + 4, y + 3), f.stem.split("-E-")[1], fill="white")
    path = out / f"{folder.name}.png"
    img.save(path)
    return path, len(files), Image.open(files[0]).size


if __name__ == "__main__":
    src, out = Path(sys.argv[1]), Path(sys.argv[2])
    out.mkdir(parents=True, exist_ok=True)
    for d in sorted(p for p in src.iterdir() if p.is_dir()):
        print(d.name, sheet(d, out))
