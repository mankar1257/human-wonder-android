"""Generates the Name artboard (screen 2 of onboarding) of the preview canvas.

Run: python3 design/preview/gen/name.py  (rewrites design/preview/project/Name.dc.html in place,
keeping its <helmet> CSS). The look: black sky, the Earth at night from orbit below the question,
a plain field and the teal button, with Orbit 412 gliding along the horizon (the one motion).
"""
import pathlib, sys

sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))

HERE = pathlib.Path(__file__).resolve().parent
PATH = HERE.parent / "project" / "Name.dc.html"
EARTH = "/_blob/8ee8bb7af970b49cfab3b586c5e1f849"

FILL = "position: absolute; left: 0; top: 0; width: 390px; height: 844px"
BACK = "m313-440 224 224-57 56-320-320 320-320 57 56-224 224h487v80H313Z"
LOCK = ("M240-80q-33 0-56.5-23.5T160-160v-400q0-33 23.5-56.5T240-640h40v-80q0-83 58.5-141.5T480-920q83 0 141.5 58.5T680-720v80h40q33 0 56.5 23.5T800-560v400q0 33-23.5 56.5T720-80H240Zm0-80h480v-400H240v400Zm240-120q33 0 56.5-23.5T560-360q0-33-23.5-56.5T480-440q-33 0-56.5 23.5T400-360q0 33 23.5 56.5T480-280ZM360-640h240v-80q0-50-35-85t-85-35q-50 0-85 35t-35 85v80ZM240-160v-400 400Z")


def svg(path, size):
    return (f'<svg width="{size}" height="{size}" viewBox="0 -960 960 960" aria-hidden="true" style="display: block; flex-shrink: 0">'
            f'<path d="{path}" fill="currentColor"></path></svg>')


LOCKUP = '''<div style="display: flex; align-items: center; gap: 9px">
<span aria-hidden="true" style="position: relative; display: block; width: 22px; height: 22px; flex-shrink: 0">
<span style="position: absolute; left: 2px; top: 2px; width: 18px; height: 18px; box-sizing: border-box; border-radius: 50%; border: 1.5px solid rgba(131, 255, 229, 0.9); background: rgba(47, 230, 200, 0.08)"></span>
<span class="hw-spin" style="animation-duration: 12s"><span style="position: absolute; left: 15.5px; top: 0.5px; width: 5px; height: 5px; border-radius: 50%; background: #ffffff; box-shadow: 0 0 5px rgba(131, 255, 229, 0.9)"></span></span>
</span>
<span style="font-size: 17px; line-height: 22px; font-weight: 500; letter-spacing: -0.01em; color: #ffffff; white-space: nowrap">Human Wonder</span>
</div>'''

BODY = f'''<div style="position: relative; width: 390px; height: 844px; overflow: hidden; background: #000000; font-family: 'Geist', system-ui, sans-serif; color: #eceff0">
<div aria-hidden="true" style="position: absolute; left: 0; top: 17%; width: 100%; height: 83%; overflow: hidden">
<img src="{EARTH}" alt="" style="width: 100%; height: 100%; object-fit: cover; object-position: 50% 0%; display: block">
</div>
<div aria-hidden="true" style="{FILL}; background: linear-gradient(to bottom, #000000 0%, #000000 17%, rgba(0, 0, 0, 0) 33%, rgba(0, 0, 0, 0) 58%, rgba(0, 0, 0, 0.78) 76%, #000000 92%)"></div>
<canvas ref="{{{{orbitRef}}}}" aria-hidden="true" style="{FILL}; display: block; pointer-events: none"></canvas>
<div style="{FILL}; display: flex; flex-direction: column">
<header style="height: 64px; flex-shrink: 0; display: flex; align-items: center; padding: 0 4px">
<a href="Main.dc.html" aria-label="Back" class="hw-iconbtn" style="width: 48px; height: 48px; color: #c5cdcb">{svg(BACK, 22)}</a>
<div style="flex: 1; display: flex; justify-content: center">{LOCKUP}</div>
<span style="width: 48px"></span>
</header>
<div class="hw-rise" style="display: flex; flex-direction: column; align-items: center; padding: 28px 32px 0; text-align: center">
<h1 class="hw-balance" style="margin: 0; font-size: 32px; line-height: 38px; font-weight: 300; letter-spacing: -0.02em; color: #ffffff">What should Orbit&nbsp;412 call you?</h1>
<p style="margin: 12px 0 0; max-width: 300px; font-size: 15px; line-height: 22px; color: #a7b2b0">It will greet you by name every time it passes over.</p>
</div>
<div style="flex: 1"></div>
<div style="display: flex; flex-direction: column; gap: 12px; padding: 0 20px 24px">
<label for="hw-name" class="hw-sr">Your name</label>
<div class="hw-field" style="height: 56px; box-sizing: border-box; display: flex; align-items: center; padding: 0 18px; border-radius: 16px; background: rgba(14, 17, 19, 0.82); border: 1px solid rgba(255, 255, 255, 0.16); -webkit-backdrop-filter: blur(12px); backdrop-filter: blur(12px)">
<input id="hw-name" value="{{{{name}}}}" onChange="{{{{onName}}}}" maxlength="40" autocomplete="off" placeholder="Your name or callsign" style="flex: 1; min-width: 0; background: transparent; border: 0; outline: none; padding: 0; color: #ffffff; font-family: 'Geist', system-ui, sans-serif; font-size: 17px; line-height: 24px">
</div>
<a href="Chat.dc.html" class="hw-cta" style="height: 56px; box-sizing: border-box; display: flex; align-items: center; justify-content: center; border-radius: 999px; background: #83ffe5; color: #00201a; text-decoration: none; font-size: 16px; line-height: 24px; font-weight: 500">Meet Orbit 412</a>
<div style="display: flex; align-items: center; justify-content: center; gap: 6px; padding-top: 2px; color: #8d9997">
{svg(LOCK, 14)}
<span style="font-size: 13px; line-height: 18px">Your name stays on this phone.</span>
</div>
</div>
</div>
</div>
'''

from orbit_js import orbit_methods, NAME_PLACE

JS = '''class Component extends DCLogic {
  constructor(...args) {
    super(...args);
    this.state = { name: '' };
    this._orbitRef = (el) => { this.orbitEl = el; };
  }
''' + orbit_methods(NAME_PLACE, 90000, 0.22, True) + '''

  renderVals() {
    return {
      name: this.state.name,
      orbitRef: this._orbitRef,
      onName: (e) => this.setState({ name: e.target.value.slice(0, 40) }),
    };
  }
}
'''

src = PATH.read_text()
head, rest = src.split("</helmet>\n", 1)
_, tail = rest.split("</x-dc>", 1)
src = head + "</helmet>\n" + BODY + "</x-dc>" + tail
a, b = src.split("data-dc-script", 1)
attrs, b = b.split(">", 1)
_, b = b.split("</script>", 1)
PATH.write_text(a + "data-dc-script" + attrs + ">\n" + JS + "</script>" + b)
print("name written")
