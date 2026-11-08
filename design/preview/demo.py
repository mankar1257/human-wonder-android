"""Record a real-speed walkthrough of the preview screens as an MP4 (no sign-in needed to watch).

Usage: python3 design/preview/demo.py  ->  design/preview/test-renders/human-wonder-demo.mp4
Plays: Welcome -> Your name (typing) -> Home -> Side menu -> Chat -> Answer -> Profile, at a 393 x 852
phone size, with every animation at its real speed (Orbit 412 takes 90 s per pass).
"""
import asyncio, base64, json, os, pathlib, shutil, subprocess, sys

HERE = pathlib.Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import tour  # noqa: E402  (reuses its screen extraction and template renderer)

OUT = HERE / "test-renders"
FRAMES = OUT / "demo-frames"
W, H = 393, 852
SCALE = 2
NAME = "Vivek"
# (screen, seconds on screen, action)
PLAN = [("Main", 6, None), ("Name", 7, "type"), ("Chat", 5, None), ("Transmission", 11, None),
        ("Welcome", 5, None), ("Drawer", 4, None), ("Profile", 5, None)]


def build_page():
    parts = [tour.piece(n) for n, _, _ in PLAN]
    tour_html = (OUT / "tour.html").read_text() if (OUT / "tour.html").exists() else None
    if tour_html is None:
        tour.main()
        tour_html = (OUT / "tour.html").read_text()
    # Pull the renderer (DCLogic, get, closeOf, render, fill) out of the tour page.
    js = tour_html.split("<script>", 1)[1].split("</script>", 1)[0]
    runtime = js.split("const PARTS =")[0]
    helpers = js[js.index("const get ="):js.index("const grid =")]
    scripts = "\n".join(p["js"] for p in parts)
    comps = ", ".join(f"{p['name']}: Component_{p['name']}" for p in parts)
    fonts = tour.FONTS
    return f"""<!doctype html><html><head><meta charset="utf-8"><style>
@font-face {{ font-family: 'Geist'; src: url(file://{fonts}/geist.ttf); font-weight: 100 900; }}
html, body {{ margin: 0; background: #000; overflow: hidden; }}
.hw-settled .hw-fadein, .hw-settled .hw-msg, .hw-settled .hw-rise {{ animation: none !important; }}
#phone {{ position: relative; width: {W}px; height: {H}px; overflow: hidden; background: #000; transition: opacity 260ms ease; }}
</style></head><body><div id="phone"></div>
<script>
{runtime.split("class DCLogic")[0]}
class DCLogic {{
  constructor(p) {{ this.props = p || {{}}; }}
  setState(u) {{
    this.state = Object.assign({{}}, this.state, typeof u === 'function' ? u(this.state) : u);
    if (this._paint) {{ clearTimeout(this._pending); this._pending = setTimeout(() => this._paint && this._paint(), 0); }}
  }}
}}
{scripts}
const PARTS = {json.dumps({p['name']: p for p in parts})};
const COMPS = {{ {comps} }};
{helpers}
const phone = document.getElementById('phone');
let current = null;
function mount(name) {{
  if (current) {{ try {{ current.componentWillUnmount && current.componentWillUnmount(); }} catch (e) {{}} current._paint = null; }}
  phone.innerHTML = '';
  const p = PARTS[name];
  const st = document.createElement('style'); st.textContent = p.css; phone.appendChild(st);
  const holder = document.createElement('div'); phone.appendChild(holder);
  const comp = new COMPS[name]({{ name: '{NAME}' }});
  comp._paint = () => {{
    const keep = {{}};
    for (const el of holder.querySelectorAll('canvas[data-ref]')) keep[el.getAttribute('data-ref')] = el;
    holder.innerHTML = render(p.tpl.replace('width: 390px', 'width: {W}px').replace('height: 844px', 'height: {H}px'), comp.renderVals());
    for (const el of holder.querySelectorAll('[data-ref]')) {{
      const r = el.getAttribute('data-ref'); let node = el;
      if (keep[r] && el.tagName === 'CANVAS') {{ el.replaceWith(keep[r]); node = keep[r]; }}
      comp[r.replace(/Ref$/, 'El')] = node;
    }}
  }};
  comp._paint();
  // This harness re-renders with innerHTML, which would restart entrance animations on every
  // update; once they have played, switch them off (the real canvas diffs the DOM instead).
  setTimeout(() => holder.classList.add('hw-settled'), 1400);
  try {{ comp.componentDidMount && comp.componentDidMount(); }} catch (e) {{ console.log(name, e); }}
  current = comp;
}}
window.show = (name) => new Promise((res) => {{
  if (!current) {{ mount(name); return res(); }}
  phone.style.opacity = 0;
  setTimeout(() => {{ mount(name); phone.style.opacity = 1; res(); }}, 270);
}});
window.typeName = async (text) => {{
  for (let i = 1; i <= text.length; i++) {{
    current.setState({{ name: text.slice(0, i) }});
    await new Promise(r => setTimeout(r, 190));
  }}
}};
</script></body></html>"""


async def record():
    from playwright.async_api import async_playwright
    if FRAMES.exists():
        shutil.rmtree(FRAMES)
    FRAMES.mkdir(parents=True)
    page_path = OUT / "demo.html"
    page_path.write_text(build_page())
    frames = []
    async with async_playwright() as pw:
        browser = await pw.chromium.launch(
            executable_path=shutil.which("google-chrome"),
            args=["--allow-file-access-from-files", "--use-gl=angle", "--use-angle=swiftshader", "--enable-unsafe-swiftshader"])
        ctx = await browser.new_context(viewport={"width": W, "height": H}, device_scale_factor=SCALE, timezone_id="Asia/Kolkata")
        page = await ctx.new_page()
        await page.goto(f"file://{page_path}")
        await page.wait_for_timeout(800)
        cdp = await ctx.new_cdp_session(page)

        def on_frame(ev):
            i = len(frames)
            (FRAMES / f"{i:05d}.jpg").write_bytes(base64.b64decode(ev["data"]))
            frames.append(ev["metadata"]["timestamp"])
            asyncio.ensure_future(cdp.send("Page.screencastFrameAck", {"sessionId": ev["sessionId"]}))

        cdp.on("Page.screencastFrame", on_frame)
        await page.evaluate("show('Main')")
        await cdp.send("Page.startScreencast", {"format": "jpeg", "quality": 90, "everyNthFrame": 1})
        for name, secs, action in PLAN:
            await page.evaluate(f"show('{name}')")
            if action == "type":
                await page.wait_for_timeout(1500)
                await page.evaluate(f"typeName('{NAME}')")
                await page.wait_for_timeout(int(secs * 1000) - 1500)
            else:
                await page.wait_for_timeout(int(secs * 1000))
        await cdp.send("Page.stopScreencast")
        await browser.close()
    return frames


def encode(stamps):
    lst = FRAMES / "list.txt"
    with lst.open("w") as f:
        for i, t in enumerate(stamps):
            nxt = stamps[i + 1] if i + 1 < len(stamps) else t + 0.5
            f.write(f"file '{i:05d}.jpg'\nduration {max(0.001, nxt - t):.4f}\n")
        f.write(f"file '{len(stamps) - 1:05d}.jpg'\n")
    out = OUT / "human-wonder-demo.mp4"
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-f", "concat", "-safe", "0", "-i", str(lst),
                    "-vf", "fps=30,scale=trunc(iw/2)*2:trunc(ih/2)*2,format=yuv420p",
                    "-c:v", "libx264", "-preset", "veryfast", "-crf", "20", "-movflags", "+faststart", str(out)], check=True)
    return out


if __name__ == "__main__":
    stamps = asyncio.run(record())
    print(len(stamps), "frames over", round(stamps[-1] - stamps[0], 1), "s")
    print(encode(stamps))
