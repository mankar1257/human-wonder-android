"""Render only the chat composer at 360/393/412 dp and font scale 1.0/1.3/1.5 (send/mic must never squeeze)."""
import json, pathlib, re, subprocess
HERE = pathlib.Path(__file__).resolve().parent
import importlib.util
spec = importlib.util.spec_from_file_location("rt", HERE / "render_test.py"); rt = importlib.util.module_from_spec(spec); spec.loader.exec_module(rt)
src = (HERE / "project/Chat.dc.html").read_text()
css = re.sub(r"url\(/_blob/[0-9a-f]+\)", "", src.split("<helmet>\n<style>")[1].split("</style>")[0])
body = src.split("<x-dc>")[1].split("</x-dc>")[0]
start = body.index('<div style="border-radius: 28px;')
# composer block = from its glass div to the matching close (the block ends before the chips row)
end = body.index('<div class="hw-noscroll" style="display: {{emptyDisplay}}', start)
block = body[start:end].rsplit('</div>', 1)[0]
js = src.split("data-dc-script")[1].split(">", 1)[1].split("</script>")[0]
cases = []
for w in (360, 393, 412):
    for f in (1.0, 1.3, 1.5):
        cases.append({"w": w, "f": f, "tpl": rt.scale_css(block, f), "css": rt.scale_css(css, f)})
page = f"""<!doctype html><html><head><meta charset="utf-8"><style>
@font-face {{ font-family: 'Geist'; src: url(file://{rt.FONTS}/geist.ttf); font-weight: 100 900; }}
body {{ margin: 0; background: #000; font-family: Geist; padding: 12px; }}
.cap {{ color: #9c9; font: 12px monospace; margin: 10px 0 4px; }}
* {{ animation: none !important; transition: none !important; }}
</style></head><body><div id="out"></div><script>
class DCLogic {{ constructor(p) {{ this.props = p || {{}}; }} setState(u) {{ this.state = Object.assign({{}}, this.state, typeof u === 'function' ? u(this.state) : u); }} }}
{js}
const CASES = {json.dumps(cases)};
const get = (o, p) => p.split('.').reduce((a, k) => (a == null ? undefined : a[k]), o);
function render(tpl, scope) {{
  tpl = tpl.replace(/<sc-if value="\\{{\\{{([\\w.]+)\\}}\\}}"[^>]*>([\\s\\S]*?)<\\/sc-if>/g, (m, p, inner) => get(scope, p) ? render(inner, scope) : '');
  tpl = tpl.replace(/\\s(on[A-Z]\\w*)="\\{{\\{{[\\w.]+\\}}\\}}"/g, '').replace(/ref="\\{{\\{{(\\w+)\\}}\\}}"/g, 'data-ref="$1"');
  return tpl.replace(/\\{{\\{{\\s*([\\w.$]+)\\s*\\}}\\}}/g, (m, p) => {{ const v = get(scope, p); return v == null ? '' : String(v); }});
}}
const out = document.getElementById('out');
for (const c of CASES) {{
  const cap = document.createElement('div'); cap.className = 'cap'; cap.textContent = c.w + 'dp x' + c.f; out.appendChild(cap);
  const box = document.createElement('div'); box.style.width = c.w + 'px'; box.style.padding = '0 12px'; box.style.boxSizing = 'border-box';
  const st = document.createElement('style'); st.textContent = c.css; box.appendChild(st);
  const comp = new Component({{ name: 'Vivek' }}); comp.state.sky = 'ready'; comp.state.input = 'Hi';
  const h = document.createElement('div'); h.innerHTML = render(c.tpl, comp.renderVals()); box.appendChild(h); out.appendChild(box);
  comp.rowEl = box.querySelector('[data-ref=rowRef]'); comp.fitComposer();
  const row = comp.rowEl; cap.textContent += '  level=' + comp.state.compact + (row.scrollWidth > row.clientWidth + 1 ? '  OVERFLOW!' : '  fits');
}}
</script></body></html>"""
(HERE / "test-renders/composer.html").write_text(page)
subprocess.run(["timeout", "60", "google-chrome", "--headless=new", "--no-sandbox", "--allow-file-access-from-files", "--window-size=460,1500",
                f"--screenshot={HERE / 'test-renders/composer.png'}", f"file://{HERE / 'test-renders/composer.html'}"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
print(HERE / "test-renders/composer.png")
