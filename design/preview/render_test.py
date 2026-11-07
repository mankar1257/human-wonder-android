"""Render preview artboards at real phone sizes for review (Material 3: test widths and font scales).

Usage: python3 design/preview/render_test.py Home Profile Drawer  ->  design/preview/test-renders/<name>.png
Each sheet shows the screen at 360 / 393 / 412 dp wide and font scale 1.0 / 1.3 / 1.5.
Display text (>= 28px) is capped at 1.15x, the same rule the app uses.
Needs google-chrome (headless, SwiftShader WebGL).
"""
import json, pathlib, re, subprocess, sys

HERE = pathlib.Path(__file__).resolve().parent
OUT = HERE / "test-renders"
OUT.mkdir(exist_ok=True)
FONTS = (HERE.parents[1] / "app/src/main/res/font").resolve()
WIDTHS = [(360, 780), (393, 852), (412, 915)]
SCALES = [1.0, 1.3, 1.5]
_RES = HERE.parents[1] / "app/src/main/res/drawable-nodpi"
PHOTOS = {"cfc316bc9f5dd90bd5cd3874418cb0b0": (_RES / "bg_liwa_dunes.jpg").resolve(),
          "8ee8bb7af970b49cfab3b586c5e1f849": (_RES / "bg_earth_orbit.jpg").resolve()}

def scale_css(text, f):
    # The Human Wonder wordmark is a logo (fixed dp); everything else scales like sp.
    keep = {}
    def protect(m):
        key = f"@@FIXED{len(keep)}@@"
        keep[key] = m.group(0)
        return key
    text = re.sub(r'font-size: 1[67]px; line-height: 22px; font-weight: 500; letter-spacing: -0.01em; color: #ffffff; white-space: nowrap', protect, text)
    def fix(m):
        prop, num = m.group(1), float(m.group(2))
        k = min(f, 1.15) if num >= 28 else f
        return f"{prop}: {round(num * k, 1)}px"
    text = re.sub(r"(font-size|line-height): ([\d.]+)px", fix, text)
    for k, v in keep.items():
        text = text.replace(k, v)
    return text

def build(name, props):
    src = (HERE / "project" / f"{name}.dc.html").read_text()
    for blob, photo in PHOTOS.items():
        src = src.replace(f"/_blob/{blob}", f"file://{photo}")
    css = src.split("<helmet>\n<style>")[1].split("</style>")[0]
    css = re.sub(r"url\(/_blob/[0-9a-f]+\)", "", css)
    css = css.replace("font-family: 'Geist'; src: ;", "")
    body = src.split("<x-dc>")[1].split("</x-dc>")[0]
    body = re.sub(r"<helmet>[\s\S]*?</helmet>", "", body)
    js = src.split("data-dc-script")[1].split(">", 1)[1].split("</script>")[0]
    phones = []
    for w, h in WIDTHS:
        for f in SCALES:
            tpl = scale_css(body, f).replace("width: 390px", f"width: {w}px").replace("height: 844px", f"height: {h}px")
            phones.append({"w": w, "h": h, "f": f, "tpl": tpl, "css": scale_css(css, f)})
    page = f"""<!doctype html><html><head><meta charset="utf-8"><style>
@font-face {{ font-family: 'Geist'; src: url(file://{FONTS}/geist.ttf); font-weight: 100 900; }}
body {{ margin: 0; background: #2a2a2a; font-family: Geist, sans-serif; }}
.grid {{ display: grid; grid-template-columns: repeat(9, max-content); gap: 18px; padding: 18px; align-items: start; }}
.cap {{ color: #ddd; font: 13px/18px monospace; margin-bottom: 6px; }}
.phone {{ position: relative; overflow: hidden; border-radius: 18px; }}
.phone * {{ animation: none !important; transition: none !important; }}
</style></head><body><div class="grid" id="grid"></div>
<script>
class DCLogic {{ constructor(p) {{ this.props = p || {{}}; }} setState(u) {{ this.state = Object.assign({{}}, this.state, typeof u === 'function' ? u(this.state) : u); }} }}
{js}
const PHONES = {json.dumps(phones)};
const PROPS = {json.dumps(props)};
const get = (o, p) => p.split('.').reduce((a, k) => (a == null ? undefined : a[k]), o);
function render(tpl, scope) {{
  tpl = tpl.replace(/<sc-for list="\\{{\\{{([\\w.]+)\\}}\\}}" as="(\\w+)"[^>]*>([\\s\\S]*?)<\\/sc-for>/g, (m, p, as, inner) => (get(scope, p) || []).map(it => render(inner, Object.assign({{}}, scope, {{[as]: it}}))).join(''));
  tpl = tpl.replace(/<sc-if value="\\{{\\{{([\\w.]+)\\}}\\}}"[^>]*>([\\s\\S]*?)<\\/sc-if>/g, (m, p, inner) => get(scope, p) ? render(inner, scope) : '');
  tpl = tpl.replace(/\\s(on[A-Z]\\w*)="\\{{\\{{[\\w.]+\\}}\\}}"/g, '');
  tpl = tpl.replace(/ref="\\{{\\{{(\\w+)\\}}\\}}"/g, 'data-ref="$1"');
  return tpl.replace(/\\{{\\{{\\s*([\\w.$]+)\\s*\\}}\\}}/g, (m, p) => {{ const v = get(scope, p); return v == null ? '' : String(v); }});
}}
const grid = document.getElementById('grid');
for (const ph of PHONES) {{
  const wrap = document.createElement('div');
  const cap = document.createElement('div'); cap.className = 'cap'; cap.textContent = ph.w + 'dp  x' + ph.f; wrap.appendChild(cap);
  const box = document.createElement('div'); box.className = 'phone'; box.style.width = ph.w + 'px'; box.style.height = ph.h + 'px';
  const st = document.createElement('style'); st.textContent = ph.css; box.appendChild(st);
  const comp = new Component(PROPS);
  const vals = comp.renderVals();
  const holder = document.createElement('div'); holder.innerHTML = render(ph.tpl, vals); box.appendChild(holder);
  wrap.appendChild(box); grid.appendChild(wrap);
  for (const el of box.querySelectorAll('[data-ref]')) {{ const k = el.getAttribute('data-ref').replace(/Ref$/, 'El'); comp[k] = el; }}
  comp._t0 = 0;
  try {{ if (comp.initGalaxy) {{ comp.initGalaxy(); comp.drawGalaxy(20000); }} }} catch (e) {{ console.log(e); }}
}}
document.title = 'done';
</script></body></html>"""
    path = OUT / f"{name}.html"
    path.write_text(page)
    width = 9 * 430 + 60
    subprocess.run(["timeout", "90", "google-chrome", "--headless=new", "--no-sandbox", "--allow-file-access-from-files",
                    "--use-gl=angle", "--use-angle=swiftshader", "--enable-unsafe-swiftshader",
                    f"--window-size={width},{max(h for _, h in WIDTHS) + 80}", f"--screenshot={OUT / (name + '.png')}", f"file://{path}"],
                   stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    print(OUT / f"{name}.png")

if __name__ == "__main__":
    for n in sys.argv[1:]:
        build(n, {"name": "Vivek"})
