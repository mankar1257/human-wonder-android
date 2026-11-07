"""Render every preview screen side by side at one common phone size (393 x 852 dp, text 100%).

Usage: python3 design/preview/tour.py  ->  design/preview/test-renders/tour.png
Unlike render_test.py (which stress-tests sizes), this is the "what the app looks like" sheet.
Live parts (aurora, star trails, satellite) are started and allowed to run for a few seconds.
"""
import json, pathlib, re, subprocess

HERE = pathlib.Path(__file__).resolve().parent
OUT = HERE / "test-renders"
OUT.mkdir(exist_ok=True)
ROOT = HERE.parents[1]
FONTS = (ROOT / "app/src/main/res/font").resolve()
PHOTOS = {"cfc316bc9f5dd90bd5cd3874418cb0b0": (ROOT / "app/src/main/res/drawable-nodpi/bg_liwa_dunes.jpg").resolve()}
SCREENS = [("Main", "1  Welcome"), ("Name", "2  Your name"), ("Home", "3  Home"), ("Drawer", "4  Side menu"),
           ("Chat", "5  Chat"), ("Transmission", "6  Answer"), ("Profile", "7  Profile & settings")]
W, H = 393, 852


def piece(name):
    src = (HERE / "project" / f"{name}.dc.html").read_text()
    for blob, path in PHOTOS.items():
        src = src.replace(f"/_blob/{blob}", f"file://{path}")
    css = src.split("<helmet>\n<style>")[1].split("</style>")[0]
    css = re.sub(r"url\(/_blob/[0-9a-f]+\)", "", css).replace("font-family: 'Geist'; src: ;", "")
    body = src.split("<x-dc>")[1].split("</x-dc>")[0]
    body = re.sub(r"<helmet>[\s\S]*?</helmet>", "", body)
    body = body.replace("width: 390px", f"width: {W}px").replace("height: 844px", f"height: {H}px")
    js = src.split("data-dc-script")[1].split(">", 1)[1].split("</script>")[0]
    # Each screen's script declares `class Component`; rename so all seven can live on one page.
    js = re.sub(r"\bclass Component\b", f"class Component_{name}", js)
    return {"name": name, "css": css, "tpl": body, "js": js}


def main():
    parts = [piece(n) for n, _ in SCREENS]
    scripts = "\n".join(p["js"] for p in parts)
    page = rf"""<!doctype html><html><head><meta charset="utf-8"><style>
@font-face {{ font-family: 'Geist'; src: url(file://{FONTS}/geist.ttf); font-weight: 100 900; }}
body {{ margin: 0; background: #0b0d10; font-family: Geist, sans-serif; }}
.grid {{ display: grid; grid-template-columns: repeat(7, max-content); gap: 28px; padding: 28px; }}
.cap {{ color: #A7B2B0; font: 500 18px/24px Geist, sans-serif; margin: 0 0 12px 4px; }}
.phone {{ position: relative; overflow: hidden; border-radius: 28px; box-shadow: 0 0 0 1px rgba(255,255,255,.08); }}
</style></head><body><div class="grid" id="grid"></div>
<script>
class DCLogic {{
  constructor(p) {{ this.props = p || {{}}; }}
  setState(u) {{
    this.state = Object.assign({{}}, this.state, typeof u === 'function' ? u(this.state) : u);
    if (this._paint) {{ clearTimeout(this._pending); this._pending = setTimeout(() => this._paint(), 0); }}
  }}
}}
{scripts}
const PARTS = {json.dumps(parts)};
const COMPS = {{ {', '.join(f'{n}: Component_{n}' for n, _ in SCREENS)} }};
const CAPS = {json.dumps(dict(SCREENS))};
const get = (o, p) => p.split('.').reduce((a, k) => (a == null ? undefined : a[k]), o);
// Template renderer for the canvas's x-dc markup: <sc-for>/<sc-if> may nest, so match tags by depth.
function closeOf(tpl, tag, from) {{
  let depth = 1, i = from;
  const open = '<' + tag, close = '</' + tag + '>';
  while (depth > 0) {{
    const o = tpl.indexOf(open, i), c = tpl.indexOf(close, i);
    if (c < 0) return -1;
    if (o >= 0 && o < c) {{ depth++; i = o + open.length; }} else {{ depth--; i = c + close.length; if (!depth) return c; }}
  }}
}}
function render(tpl, scope) {{
  let out = '', i = 0;
  const re = /<sc-(for|if)\b([^>]*)>/g;
  let m;
  while ((m = re.exec(tpl))) {{
    const tag = 'sc-' + m[1], attrs = m[2];
    const bodyStart = m.index + m[0].length;
    const c = closeOf(tpl, tag, bodyStart);
    const inner = tpl.slice(bodyStart, c);
    out += fill(tpl.slice(i, m.index), scope);
    if (m[1] === 'for') {{
      const list = get(scope, /list="\{{\{{([\w.]+)\}}\}}"/.exec(attrs)[1]) || [];
      const as = /as="(\w+)"/.exec(attrs)[1];
      out += list.map(it => render(inner, Object.assign({{}}, scope, {{[as]: it}}))).join('');
    }} else {{
      if (get(scope, /value="\{{\{{([\w.]+)\}}\}}"/.exec(attrs)[1])) out += render(inner, scope);
    }}
    i = c + tag.length + 3;
    re.lastIndex = i;
  }}
  return out + fill(tpl.slice(i), scope);
}}
function fill(t, scope) {{
  t = t.replace(/\s(on[A-Z]\w*)="\{{\{{[\w.]+\}}\}}"/g, '');
  t = t.replace(/ref="\{{\{{(\w+)\}}\}}"/g, 'data-ref="$1"');
  return t.replace(/\{{\{{\s*([\w.$]+)\s*\}}\}}/g, (m, p) => {{ const v = get(scope, p); return v == null ? '' : String(v); }});
}}
const grid = document.getElementById('grid');
for (const p of PARTS) {{
  const wrap = document.createElement('div');
  const cap = document.createElement('div'); cap.className = 'cap'; cap.textContent = CAPS[p.name]; wrap.appendChild(cap);
  const box = document.createElement('div'); box.className = 'phone'; box.style.width = '{W}px'; box.style.height = '{H}px';
  const st = document.createElement('style'); st.textContent = p.css; box.appendChild(st);
  const comp = new COMPS[p.name]({{ name: 'Vivek' }});
  const holder = document.createElement('div'); box.appendChild(holder);
  wrap.appendChild(box); grid.appendChild(wrap);
  // Re-render on every setState, like the canvas runtime does. Canvases keep their drawing:
  // the old element is moved into the new markup instead of being replaced.
  comp._paint = () => {{
    const keep = {{}};
    for (const el of holder.querySelectorAll('canvas[data-ref]')) keep[el.getAttribute('data-ref')] = el;
    holder.innerHTML = render(p.tpl, comp.renderVals());
    for (const el of holder.querySelectorAll('[data-ref]')) {{
      const r = el.getAttribute('data-ref'); let node = el;
      if (keep[r] && el.tagName === 'CANVAS') {{ el.replaceWith(keep[r]); node = keep[r]; }}
      comp[r.replace(/Ref$/, 'El')] = node;
    }}
  }};
  comp._paint();
  try {{ if (comp.componentDidMount) comp.componentDidMount(); }} catch (e) {{ console.log(p.name, e); }}
}}
</script></body></html>"""
    path = OUT / "tour.html"
    path.write_text(page)
    width = 7 * (W + 28) + 28
    subprocess.run(["timeout", "120", "google-chrome", "--headless=new", "--no-sandbox", "--allow-file-access-from-files",
                    "--use-gl=angle", "--use-angle=swiftshader", "--enable-unsafe-swiftshader", "--virtual-time-budget=9000",
                    f"--window-size={width},{H + 100}", f"--screenshot={OUT / 'tour.png'}", f"file://{path}"],
                   stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    print(OUT / "tour.png")


if __name__ == "__main__":
    main()
