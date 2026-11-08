"""Generates the two welcome artboards from Main.dc.html (the desert-and-sky opening screen).

Run: python3 design/preview/gen/welcome.py
  - Main.dc.html     first launch: "The sky has something to tell you." -> swipe up -> Your name
  - Welcome.dc.html  profile already set up: "Welcome back, <name>." -> swipe up -> the chat

The opening screen is the Liwa dunes photo with Orbit 412 passing over. It ends in the "ascent":
three thin chevrons that light up one after another from the bottom (the top one in teal), over a
label with a slow light sweep. No button shape; tapping it works too. Idempotent: rewrites the
headline, the subtitle, the bottom block, the welcome CSS and the script's renderVals.
"""
import pathlib
import re

HERE = pathlib.Path(__file__).resolve().parent
PROJECT = HERE.parent / "project"
MAIN = PROJECT / "Main.dc.html"
WELCOME = PROJECT / "Welcome.dc.html"

NB = " "  # keeps "Orbit 412" on one line
FIRST_TITLE = "The sky has something to tell you."
FIRST_SUB = f"Human Wonder gives the sky a voice: Orbit{NB}412, a satellite passing over you tonight."
BACK_TITLE = "Welcome back, {{name}}."
BACK_SUB = f"{{{{greeting}}}}. Orbit{NB}412 is overhead now, and it remembers your conversations."

CSS = """/* hw-welcome:start */
@keyframes hw-ascend { 0%, 100% { opacity: 0.16; transform: translateY(0); } 35% { opacity: 1; transform: translateY(-3px); } 70% { opacity: 0.16; transform: translateY(0); } }
.hw-chev { display: block; color: #ffffff; animation: hw-ascend 2s cubic-bezier(0.4, 0, 0.2, 1) infinite; }
.hw-chev.c2 { animation-delay: 0.2s; }
.hw-chev.c3 { animation-delay: 0.4s; color: #83ffe5; filter: drop-shadow(0 0 6px rgba(131, 255, 229, 0.55)); }
@keyframes hw-sweep { from { background-position: 160% 0; } to { background-position: -60% 0; } }
.hw-sweep { background: linear-gradient(100deg, rgba(236, 239, 240, 0.62) 38%, #ffffff 50%, rgba(236, 239, 240, 0.62) 62%); background-size: 260% 100%; -webkit-background-clip: text; background-clip: text; color: transparent; animation: hw-sweep 3.2s linear infinite; }
.hw-ascent { transition: transform 0.2s ease; }
.hw-ascent:active { transform: translateY(-4px); }
.hw-ascent:focus-visible { outline: 2px solid #83ffe5; outline-offset: 4px; border-radius: 16px; }
@media (prefers-reduced-motion: reduce) { .hw-chev, .hw-sweep { animation: none; opacity: 1; color: rgba(236, 239, 240, 0.9); } }
/* hw-welcome:end */"""

CHEVRON = ('<svg class="hw-chev {c}" width="30" height="13" viewBox="0 0 30 13" aria-hidden="true">'
           '<path d="M3 11.5 L15 2.5 L27 11.5" fill="none" stroke="currentColor" stroke-width="2.2" '
           'stroke-linecap="round" stroke-linejoin="round"></path></svg>')


def ascent(target, label):
    chevrons = "\n".join(CHEVRON.format(c=c) for c in ("c3", "c2", "c1"))  # top to bottom
    return f'''<a href="{target}" aria-label="{label}" class="hw-ascent" style="align-self: center; display: flex; flex-direction: column; align-items: center; gap: 16px; padding: 12px 28px 34px; text-decoration: none">
<span aria-hidden="true" style="display: flex; flex-direction: column; align-items: center; gap: 2px">
{chevrons}
</span>
<span class="hw-sweep" aria-hidden="true" style="font-size: 15px; line-height: 20px; font-weight: 500; letter-spacing: 0.01em; text-align: center; text-wrap: balance">{label}</span>
</a>
'''


def build(src, target, label, title, subtitle, render_vals):
    # Bottom block: everything between the flex spacer and the end of the content column.
    src = re.sub(r'(<div style="flex: 1"></div>\n)[\s\S]*?(?=</div>\n</div>\n</x-dc>)',
                 lambda m: m.group(1) + ascent(target, label), src, count=1)
    src = re.sub(r'(<h1 class="hw-balance" style="margin: 0; font-size: 40px;[^"]*">)[^<]*(</h1>)',
                 lambda m: m.group(1) + title + m.group(2), src, count=1)
    src = re.sub(r'<p( class="hw-balance")? (style="margin: 14px 0 0; max-width: 310px;[^"]*">)[^<]*(</p>)',
                 lambda m: '<p class="hw-balance" ' + m.group(2) + subtitle + m.group(3), src, count=1)
    # Welcome CSS: drop any earlier version (marked, or the first swipe button's), then add this one.
    src = re.sub(r"/\* hw-welcome:start \*/[\s\S]*?/\* hw-welcome:end \*/\n", "", src)
    src = re.sub(r"\n@keyframes hw-bob[\s\S]*?\.hw-bob \{ animation: none; \} \}", "", src)
    src = src.replace("</style>\n</helmet>", CSS + "\n</style>\n</helmet>", 1)
    src = re.sub(r"  renderVals\(\) \{[\s\S]*?\n  \}\n", lambda m: render_vals, src, count=1)
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
MAIN.write_text(build(base, "Name.dc.html", "Swipe up to begin", FIRST_TITLE, FIRST_SUB, FIRST_VALS))
back = build(base, "Chat.dc.html", f"Swipe up to talk to Orbit{NB}412", BACK_TITLE, BACK_SUB, BACK_VALS)
back = back.replace("""data-props='{"$preview":""", """data-props='{"name":{"editor":"text","default":"Vivek"},"$preview":""", 1)
WELCOME.write_text(back)
print("welcome screens written")
