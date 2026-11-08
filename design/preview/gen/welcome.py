"""Generates the two welcome artboards from Main.dc.html (the desert-and-sky opening screen).

Run: python3 design/preview/gen/welcome.py
  - Main.dc.html     first launch: "Swipe up to begin" -> Your name
  - Welcome.dc.html  profile already set up: "Good evening, <name>." -> straight into the chat

The opening screen is the Liwa dunes photo with Orbit 412 passing over; instead of a button it
ends in a teal swipe-up arrow (tap works too). Idempotent: rewrites only the bottom block, the
subtitle and the script's renderVals.
"""
import pathlib
import re

HERE = pathlib.Path(__file__).resolve().parent
PROJECT = HERE.parent / "project"
MAIN = PROJECT / "Main.dc.html"
WELCOME = PROJECT / "Welcome.dc.html"

ARROW_UP = "M480-528 296-344l-56-56 240-240 240 240-56 56-184-184Z"
FIRST_SUB = "Human Wonder gives the sky a voice: Orbit 412, a satellite passing over you tonight."
CSS = """
@keyframes hw-bob { 0%, 100% { transform: translateY(0); } 50% { transform: translateY(-6px); } }
.hw-bob { animation: hw-bob 1.8s ease-in-out infinite; }
.hw-swipe { transition: transform 0.15s ease; }
.hw-swipe:active { transform: scale(0.96); }
.hw-swipe:focus-visible { outline: 2px solid #83ffe5; outline-offset: 3px; }
@media (prefers-reduced-motion: reduce) { .hw-bob { animation: none; } }
"""


def swipe_block(target, label):
    return f'''<div class="hw-swipe-zone" style="display: flex; flex-direction: column; align-items: center; gap: 12px; padding: 0 20px 36px">
<a href="{target}" aria-label="{label}" class="hw-swipe" style="width: 56px; height: 56px; box-sizing: border-box; border-radius: 50%; display: flex; align-items: center; justify-content: center; background: #83ffe5; color: #00201a; text-decoration: none; box-shadow: 0 8px 24px rgba(0, 0, 0, 0.35)">
<span class="hw-bob" style="display: flex"><svg width="30" height="30" viewBox="0 -960 960 960" aria-hidden="true" style="display: block"><path d="{ARROW_UP}" fill="currentColor"></path></svg></span>
</a>
<span style="font-size: 14px; line-height: 20px; color: rgba(236, 239, 240, 0.88)">{label}</span>
</div>
'''


def build(src, target, label, subtitle, render_vals):
    # Bottom block: whatever sits after the flex spacer (old button or an earlier swipe zone).
    src = re.sub(r'<div style="flex: 1"></div>\n<div[^>]*>\n<a href="[^"]+"[\s\S]*?</div>\n(?=</div>\n</div>\n</x-dc>)',
                 '<div style="flex: 1"></div>\n' + swipe_block(target, label), src, count=1)
    src = re.sub(r'<p( class="hw-balance")? (style="margin: 14px 0 0; max-width: 310px;[^"]*">)[^<]*(</p>)',
                 r'<p class="hw-balance" \g<2>' + subtitle + r'\g<3>', src, count=1)
    if "@keyframes hw-bob" not in src:
        src = src.replace("</style>\n</helmet>", CSS.strip("\n") + "\n</style>\n</helmet>", 1)
    src = re.sub(r"  renderVals\(\) \{[\s\S]*?\n  \}\n", render_vals, src, count=1)
    return src


FIRST_VALS = """  renderVals() {
    return {
      orbitRef: this._orbitRef,
    };
  }
"""
BACK_VALS = """  renderVals() {
    var name = ((this.props && this.props.name) || '').trim() || 'Wanderer';
    var h = new Date().getHours();
    var greeting = h < 5 ? 'Good night' : h < 12 ? 'Good morning' : h < 17 ? 'Good afternoon' : h < 22 ? 'Good evening' : 'Good night';
    return {
      orbitRef: this._orbitRef,
      greeting: greeting,
      name: name,
    };
  }
"""

base = MAIN.read_text()
MAIN.write_text(build(base, "Name.dc.html", "Swipe up to begin", FIRST_SUB, FIRST_VALS))
back = build(base, "Chat.dc.html", "Swipe up to talk to Orbit 412",
             "{{greeting}}, {{name}}. Orbit 412 is overhead now.", BACK_VALS)
back = back.replace("""data-props='{"$preview":""", """data-props='{"name":{"editor":"text","default":"Wanderer"},"$preview":""", 1)
WELCOME.write_text(back)
print("welcome screens written")
