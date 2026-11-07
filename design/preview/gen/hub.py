"""Generates the Home, Drawer (side menu) and Profile artboards of the preview canvas.

Run: python3 design/preview/gen/hub.py  (writes design/preview/project/{Home,Drawer,Profile}.dc.html)
Shared CSS comes from Chat.dc.html so every screen uses the same fonts and classes.
"""
import pathlib

HERE = pathlib.Path(__file__).resolve().parent
PROJECT = HERE.parent / "project"
CHAT = (PROJECT / "Chat.dc.html").read_text()
CSS = CHAT.split("<helmet>\n<style>")[1].split("</style>")[0]
CHAT_JS = CHAT.split("data-dc-script")[1].split(">", 1)[1].split("</script>")[0]
# (Home no longer draws the aurora; the chat keeps it.)
_UNUSED = CHAT_JS[CHAT_JS.index("  initGalaxy() {"):CHAT_JS.index("  drawGalaxy(now) {")]

EXTRA_CSS = """
.hw-row:active, .hw-chip:active { background: rgba(255, 255, 255, 0.08); }
.hw-cta { transition: transform 0.15s ease; }
.hw-cta:active { transform: scale(0.98); }
.hw-cta:focus-visible, .hw-row:focus-visible, .hw-field:focus-within { outline: 2px solid #83ffe5; outline-offset: 2px; }
.hw-switch { position: relative; width: 52px; height: 32px; border-radius: 999px; flex-shrink: 0; box-sizing: border-box; transition: background 0.2s; }
.hw-switch > span { position: absolute; top: 50%; border-radius: 50%; transform: translateY(-50%); transition: left 0.2s, width 0.2s, height 0.2s, background 0.2s; }
"""

ICON = {
    "menu": "M120-240v-80h720v80H120Zm0-200v-80h720v80H120Zm0-200v-80h720v80H120Z",
    "search": "M784-120 532-372q-30 24-69 38t-83 14q-109 0-184.5-75.5T120-580q0-109 75.5-184.5T380-840q109 0 184.5 75.5T640-580q0 44-14 83t-38 69l252 252-56 56ZM380-400q75 0 127.5-52.5T560-580q0-75-52.5-127.5T380-760q-75 0-127.5 52.5T200-580q0 75 52.5 127.5T380-400Z",
    "chevron": "M504-480 320-664l56-56 240 240-240 240-56-56 184-184Z",
    "more": "M480-160q-33 0-56.5-23.5T400-240q0-33 23.5-56.5T480-320q33 0 56.5 23.5T560-240q0 33-23.5 56.5T480-160Zm0-240q-33 0-56.5-23.5T400-480q0-33 23.5-56.5T480-560q33 0 56.5 23.5T560-480q0 33-23.5 56.5T480-400Zm0-240q-33 0-56.5-23.5T400-720q0-33 23.5-56.5T480-800q33 0 56.5 23.5T560-720q0 33-23.5 56.5T480-640Z",
    "home": "M240-200h120v-240h240v240h120v-360L480-740 240-560v360Zm-80 80v-480l320-240 320 240v480H520v-240h-80v240H160Zm320-350Z",
    "add": "M440-440H200v-80h240v-240h80v240h240v80H520v240h-80v-240Z",
    "back": "m313-440 224 224-57 56-320-320 320-320 57 56-224 224h487v80H313Z",
    "upload": "M440-320v-326L336-542l-56-58 200-200 200 200-56 58-104-104v326h-80ZM240-160q-33 0-56.5-23.5T160-240v-120h80v120h480v-120h80v120q0 33-23.5 56.5T720-160H240Z",
    "delete": "M280-120q-33 0-56.5-23.5T200-200v-520h-40v-80h200v-40h240v40h200v80h-40v520q0 33-23.5 56.5T680-120H280Zm400-600H280v520h400v-520ZM360-280h80v-360h-80v360Zm160 0h80v-360h-80v360ZM280-720v520-520Z",
    "info": "M440-280h80v-240h-80v240Zm40-320q17 0 28.5-11.5T520-640q0-17-11.5-28.5T480-680q-17 0-28.5 11.5T440-640q0 17 11.5 28.5T480-600Zm0 520q-83 0-156-31.5T197-197q-54-54-85.5-127T80-480q0-83 31.5-156T197-763q54-54 127-85.5T480-880q83 0 156 31.5T763-763q54 54 85.5 127T880-480q0 83-31.5 156T763-197q-54 54-127 85.5T480-80Zm0-80q134 0 227-93t93-227q0-134-93-227t-227-93q-134 0-227 93t-93 227q0 134 93 227t227 93Zm0-320Z",
    "sat": "M560-32v-80q117 0 198.5-81.5T840-392h80q0 75-28.5 140.5t-77 114q-48.5 48.5-114 77T560-32Zm0-160v-80q50 0 85-35t35-85h80q0 83-58.5 141.5T560-192ZM222-57q-15 0-30-6t-27-17L23-222q-11-12-17-27t-6-30q0-16 6-30.5T23-335l127-127q23-23 57-23.5t57 22.5l50 50 28-28-50-50q-23-23-23-56t23-56l57-57q23-23 56.5-23t56.5 23l50 50 28-28-50-50q-23-23-23-56.5t23-56.5l127-127q12-12 27-18t30-6q15 0 29.5 6t26.5 18l142 142q12 11 17.5 25.5T895-730q0 15-5.5 30T872-673L745-546q-23 23-56.5 23T632-546l-50-50-28 28 50 50q23 23 22.5 56.5T603-405l-56 56q-23 23-56.5 23T434-349l-50-50-28 28 50 50q23 23 22.5 57T405-207L278-80q-11 11-25.5 17T222-57Zm0-79 42-42-142-142-42 42 142 142Zm85-85 42-42-142-142-42 42 142 142Zm184-184 56-56-142-142-56 56 142 142Zm198-198 42-42-142-142-42 42 142 142Zm85-85 42-42-142-142-42 42 142 142ZM448-504Z",
    "location": "M480-480q33 0 56.5-23.5T560-560q0-33-23.5-56.5T480-640q-33 0-56.5 23.5T400-560q0 33 23.5 56.5T480-480Zm0 294q122-112 181-203.5T720-552q0-109-69.5-178.5T480-800q-101 0-170.5 69.5T240-552q0 71 59 162.5T480-186Zm0 106Q319-217 239.5-334.5T160-552q0-150 96.5-239T480-880q127 0 223.5 89T800-552q0 100-79.5 217.5T480-80Zm0-480Z",
}

def icon(name, size, color="currentColor"):
    return (f'<svg width="{size}" height="{size}" viewBox="0 -960 960 960" aria-hidden="true" style="display: block; flex-shrink: 0">'
            f'<path d="{ICON[name]}" fill="{color}"></path></svg>')

def sparkle(size, color="currentColor"):
    return (f'<svg width="{size}" height="{size}" viewBox="0 0 24 24" aria-hidden="true" style="display: block; flex-shrink: 0">'
            f'<path d="M11 4.5c.5 4 3.5 7 7.5 7.5-4 .5-7 3.5-7.5 7.5-.5-4-3.5-7-7.5-7.5 4-.5 7-3.5 7.5-7.5Z" fill="none" stroke="{color}" stroke-width="1.6" stroke-linejoin="round"></path>'
            f'<path d="M19 2.5v4M17 4.5h4" fill="none" stroke="{color}" stroke-width="1.6" stroke-linecap="round"></path></svg>')

def mark(size=22, speed="12s"):
    ring = size - 4
    dot = round(size * 0.22, 1)
    return (f'<span aria-hidden="true" style="position: relative; display: block; width: {size}px; height: {size}px; flex-shrink: 0">'
            f'<span style="position: absolute; left: 2px; top: 2px; width: {ring}px; height: {ring}px; box-sizing: border-box; border-radius: 50%; border: 1.5px solid rgba(131, 255, 229, 0.9); background: rgba(47, 230, 200, 0.08)"></span>'
            f'<span class="hw-spin" style="animation-duration: {speed}"><span style="position: absolute; left: {size - dot - 1.5}px; top: 0.5px; width: {dot}px; height: {dot}px; border-radius: 50%; background: #ffffff; box-shadow: 0 0 5px rgba(131, 255, 229, 0.9)"></span></span>'
            f'</span>')

def lockup(size=22, text=17):
    return (f'<span style="display: flex; align-items: center; gap: 9px">{mark(size)}'
            f'<span style="font-size: {text}px; line-height: 22px; font-weight: 500; letter-spacing: -0.01em; color: #ffffff; white-space: nowrap">Human Wonder</span></span>')

def avatar(size, font):
    return (f'<span aria-hidden="true" style="width: {size}px; height: {size}px; border-radius: 50%; flex-shrink: 0; box-sizing: border-box; display: flex; align-items: center; justify-content: center; '
            f'background: #1f2629; border: 1px solid rgba(255, 255, 255, 0.12); color: #eceff0; font-size: {font}px; line-height: 1; font-weight: 500">{{{{initial}}}}</span>')

EARTH = "/_blob/8ee8bb7af970b49cfab3b586c5e1f849"

ROOT = "position: relative; width: 390px; height: 844px; overflow: hidden; background: #000000; font-family: 'Geist', system-ui, sans-serif; color: #eceff0"
FILL = "position: absolute; left: 0; top: 0; width: 390px; height: 844px"
ICONBTN = "width: 48px; height: 48px; display: flex; align-items: center; justify-content: center; border-radius: 50%; color: #c5cdcb; text-decoration: none"
SECTION = "margin: 0; font-size: 13px; line-height: 18px; font-weight: 500; color: #8d9997"
GLASS = "border-radius: 24px; background: rgba(20, 24, 30, 0.72); border: 1px solid rgba(255, 255, 255, 0.10)"
CTA = "height: 56px; box-sizing: border-box; display: flex; align-items: center; justify-content: center; border-radius: 999px; background: #83ffe5; color: #00201a; text-decoration: none; font-size: 16px; line-height: 24px; font-weight: 500"
GROUP = "border-radius: 20px; background: rgba(255, 255, 255, 0.04); border: 1px solid rgba(255, 255, 255, 0.08)"
CHIP = "max-width: 100%; min-height: 40px; box-sizing: border-box; display: flex; align-items: center; padding: 9px 16px; border-radius: 999px; border: 1px solid rgba(255, 255, 255, 0.14); color: #d5dcdb; font-size: 14px; line-height: 20px; text-decoration: none"

RECENT = [
    ("City lights from 400 km up", "9:42 PM"),
    ("Can you spot our campfire?", "Yesterday"),
    ("Why don't stars twinkle up there?", "Mon"),
]

def home_content():
    rows = "\n".join(
        f'''<a href="Transmission.dc.html" class="hw-row" style="display: flex; align-items: center; gap: 12px; min-height: 64px; padding: 10px 8px 10px 16px; box-sizing: border-box; text-decoration: none; color: #eceff0{'; border-top: 1px solid rgba(255, 255, 255, 0.07)' if i else ''}">
<span style="flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 2px">
<span class="hw-ellipsis" style="font-size: 16px; line-height: 22px">{t}</span>
<span style="font-size: 13px; line-height: 18px; color: #8d9997">{w}</span>
</span>
<span style="width: 40px; display: flex; justify-content: center; color: #6b7775">{icon('chevron', 20)}</span>
</a>''' for i, (t, w) in enumerate(RECENT))
    chips = "\n".join(
        f'<a href="Chat.dc.html" class="hw-chip" style="{CHIP}">{c}</a>'
        for c in ["What does Earth look like tonight?", "Where is the Moon right now?", "How fast are you moving?"])
    return f'''<div style="{FILL}; display: flex; flex-direction: column">
<header style="height: 64px; flex-shrink: 0; display: flex; align-items: center; padding: 0 4px">
<a href="Drawer.dc.html" aria-label="Open menu" style="{ICONBTN}">{icon('menu', 24)}</a>
<div style="flex: 1; display: flex; justify-content: center">{lockup()}</div>
<a href="Profile.dc.html" aria-label="Profile and settings" style="{ICONBTN}">{avatar(36, 15)}</a>
</header>
<div class="hw-noscroll" style="flex: 1; min-height: 0; overflow-y: auto">
<h1 style="margin: 0; padding: 16px 24px 0; font-size: 30px; line-height: 36px; font-weight: 300; letter-spacing: -0.02em; color: #ffffff">{{{{greeting}}}}, {{{{name}}}}</h1>
<section aria-label="Orbit 412" style="position: relative; margin: 20px 16px 0; display: flex; flex-direction: column; border-radius: 24px; overflow: hidden; background: #05080a; border: 1px solid rgba(255, 255, 255, 0.08)">
<img src="{EARTH}" alt="" aria-hidden="true" style="position: absolute; left: 0; top: 0; margin-top: -30%; width: 100%; height: auto; display: block">
<div aria-hidden="true" style="flex: 1 0 120px"></div>
<div style="position: relative; padding: 56px 20px 20px; background: linear-gradient(to bottom, rgba(0, 0, 0, 0) 0px, rgba(0, 0, 0, 0.8) 56px, rgba(0, 0, 0, 0.92) 100%)">
<span style="display: flex; align-items: center; gap: 8px; font-size: 13px; line-height: 18px; color: #c5cdcb"><span class="hw-pulse" style="display: block; flex-shrink: 0; width: 6px; height: 6px; border-radius: 50%; background: #83ffe5"></span>Overhead now, 412 km up</span>
<h2 style="margin: 6px 0 0; font-size: 24px; line-height: 30px; font-weight: 400; letter-spacing: -0.01em; color: #ffffff">Orbit 412</h2>
<p style="margin: 4px 0 16px; font-size: 15px; line-height: 22px; color: #c5cdcb">Your Human Wonder satellite. Ask it what it sees tonight.</p>
<a href="Chat.dc.html" class="hw-cta" style="{CTA}">Talk to Orbit 412</a>
</div>
</section>
<div style="display: flex; align-items: center; gap: 12px; padding: 28px 12px 8px 24px">
<h2 style="flex: 1; min-width: 0; margin: 0; font-size: 17px; line-height: 22px; font-weight: 500; color: #ffffff">Recent transmissions</h2>
<a href="Drawer.dc.html" style="flex-shrink: 0; min-height: 48px; display: flex; align-items: center; padding: 0 12px; font-size: 14px; line-height: 20px; font-weight: 500; color: #83ffe5; text-decoration: none; white-space: nowrap">See all</a>
</div>
<div style="margin: 0 16px; {GROUP}; overflow: hidden">
{rows}
</div>
<h2 style="margin: 0; padding: 28px 24px 12px; font-size: 17px; line-height: 22px; font-weight: 500; color: #ffffff">Try asking</h2>
<div style="display: flex; flex-wrap: wrap; gap: 8px; padding: 0 16px 32px">
{chips}
</div>
</div>
</div>'''

SKY_LAYERS = ''

def drawer_sheet():
    groups = [
        ("Today", [("City lights from 400 km up", True), ("The Moon over Liwa", False)]),
        ("Yesterday", [("Can you spot our campfire?", False), ("Sunrise every 90 minutes", False)]),
        ("Earlier", [("Why don't stars twinkle up there?", False), ("What the Gulf looks like at night", False), ("Aurora from orbit", False)]),
    ]
    items = []
    for label, convs in groups:
        items.append(f'<p style="{SECTION}; padding: 16px 28px 6px">{label}</p>')
        for title, sel in convs:
            bg = "background: rgba(131, 255, 229, 0.12); color: #ffffff" if sel else "color: #d5dcdb"
            more = f'<span aria-label="More options" style="width: 40px; height: 40px; display: flex; align-items: center; justify-content: center; color: #a7b2b0">{icon("more", 20)}</span>' if sel else ""
            items.append(f'<a href="Transmission.dc.html" class="hw-row" style="margin: 0 12px; min-height: 48px; display: flex; align-items: center; padding: 0 4px 0 16px; border-radius: 24px; text-decoration: none; {bg}"><span class="hw-ellipsis" style="flex: 1; min-width: 0; font-size: 15px; line-height: 20px">{title}</span>{more}</a>')
    return f'''<div aria-hidden="true" style="{FILL}; background: rgba(0, 0, 0, 0.6)"></div>
<nav aria-label="Transmissions" style="position: absolute; left: 0; top: 0; bottom: 0; width: 320px; display: flex; flex-direction: column; background: #0b0d12; border-radius: 0 24px 24px 0; border-right: 1px solid rgba(255, 255, 255, 0.06)">
<div style="height: 64px; flex-shrink: 0; display: flex; align-items: center; padding: 0 20px">{lockup()}</div>
<div style="display: flex; flex-direction: column; gap: 4px; padding: 4px 12px 8px">
<a href="Home.dc.html" class="hw-row" style="min-height: 48px; display: flex; align-items: center; gap: 14px; padding: 0 16px; border-radius: 24px; color: #eceff0; text-decoration: none; font-size: 15px; line-height: 20px">{icon('home', 22, '#c5cdcb')}Home</a>
<a href="Chat.dc.html" class="hw-row" style="min-height: 48px; display: flex; align-items: center; gap: 14px; padding: 0 16px; border-radius: 24px; color: #eceff0; text-decoration: none; font-size: 15px; line-height: 20px; background: rgba(255, 255, 255, 0.06)">{icon('add', 22, '#83ffe5')}New conversation</a>
</div>
<label class="hw-field" style="margin: 4px 12px 4px; min-height: 48px; display: flex; align-items: center; gap: 10px; padding: 0 16px; border-radius: 24px; background: rgba(255, 255, 255, 0.06)">
<span style="color: #8d9997">{icon('search', 20)}</span>
<span class="hw-sr">Search your transmissions</span>
<input placeholder="Search your transmissions" style="flex: 1; min-width: 0; background: transparent; border: 0; outline: none; padding: 0; color: #ffffff; font-family: 'Geist', system-ui, sans-serif; font-size: 15px; line-height: 20px">
</label>
<div class="hw-noscroll" style="flex: 1; min-height: 0; overflow-y: auto; display: flex; flex-direction: column; gap: 2px; padding-bottom: 12px">
{"".join(items)}
</div>
<a href="Profile.dc.html" class="hw-row" style="flex-shrink: 0; display: flex; align-items: center; gap: 12px; min-height: 72px; padding: 0 16px 0 20px; border-top: 1px solid rgba(255, 255, 255, 0.08); text-decoration: none">
{avatar(36, 15)}
<span style="flex: 1; min-width: 0; display: flex; flex-direction: column">
<span class="hw-ellipsis" style="font-size: 15px; line-height: 20px; font-weight: 500; color: #ffffff">{{{{name}}}}</span>
<span style="font-size: 12px; line-height: 16px; color: #a7b2b0">Profile &amp; settings</span>
</span>
<span style="color: #8d9997">{icon('chevron', 20)}</span>
</a>
</nav>'''

HOME_JS = """class Component extends DCLogic {
  renderVals() {
    var name = (this.props.name || '').trim() || 'Wanderer';
    var h = new Date().getHours();
    var greeting = h < 5 ? 'Good night' : h < 12 ? 'Good morning' : h < 17 ? 'Good afternoon' : h < 22 ? 'Good evening' : 'Good night';
    return { name: name, initial: name.charAt(0).toUpperCase(), greeting: greeting };
  }
}"""

def page(title, body, js, props='{"name":{"editor":"text","default":"Wanderer"},"$preview":{"width":390,"height":844}}'):
    return f"""<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<title>{title}</title>
<script src="./support.js"></script>
</head>
<body>
<x-dc>
<helmet>
<style>{CSS}{EXTRA_CSS}</style>
</helmet>
{body}
</x-dc>
<script type="text/x-dc" data-dc-script data-props='{props}'>
{js}
</script>
</body>
</html>
"""

home = f'<div style="{ROOT}">\n{SKY_LAYERS}\n{home_content()}\n</div>'
drawer = f'<div style="{ROOT}">\n{SKY_LAYERS}\n{home_content()}\n{drawer_sheet()}\n</div>'
(PROJECT / "Home.dc.html").write_text(page("Human Wonder — Home", home, HOME_JS))
(PROJECT / "Drawer.dc.html").write_text(page("Human Wonder — Side menu", drawer, HOME_JS))
print("home + drawer written")

# ----------------------------------------------------------------------------- profile

def field(label, hole, placeholder, kind="text", helper=None):
    help_html = f'<span style="font-size: 12px; line-height: 16px; color: #8d9997">{helper}</span>' if helper else ""
    return f'''<label class="hw-field" style="display: flex; flex-direction: column; gap: 4px; padding: 12px 16px 12px; min-height: 64px; box-sizing: border-box">
<span style="font-size: 13px; line-height: 18px; color: #a7b2b0">{label}</span>
<input type="{kind}" value="{{{{{hole}}}}}" onChange="{{{{on_{hole}}}}}" placeholder="{placeholder}" autocomplete="off" style="background: transparent; border: 0; outline: none; padding: 0; color: #ffffff; font-family: 'Geist', system-ui, sans-serif; font-size: 16px; line-height: 24px">
{help_html}
</label>'''

def switch_row(label, sub, key):
    return f'''<button type="button" role="switch" aria-checked="{{{{{key}On}}}}" onClick="{{{{toggle_{key}}}}}" class="hw-row" style="width: 100%; display: flex; align-items: center; gap: 16px; min-height: 64px; padding: 10px 16px; border: 0; background: transparent; text-align: left; color: #eceff0">
<span style="flex: 1; min-width: 0; display: flex; flex-direction: column">
<span style="font-size: 16px; line-height: 22px">{label}</span>
<span style="font-size: 13px; line-height: 18px; color: #8d9997">{sub}</span>
</span>
<span class="hw-switch" style="background: {{{{{key}Track}}}}; border: 2px solid {{{{{key}Border}}}}"><span style="left: {{{{{key}Left}}}}; width: {{{{{key}Knob}}}}; height: {{{{{key}Knob}}}}; background: {{{{{key}Thumb}}}}"></span></span>
</button>'''

def nav_row(icon_name, label, color="#eceff0", sub=None, first=False):
    sub_html = f'<span style="font-size: 13px; line-height: 18px; color: #8d9997">{sub}</span>' if sub else ""
    border = "" if first else "border-top: 1px solid rgba(255, 255, 255, 0.07); "
    return f'''<button type="button" class="hw-row" style="width: 100%; display: flex; align-items: center; gap: 14px; min-height: 56px; padding: 8px 16px; border: 0; {border}background: transparent; text-align: left; color: {color}">
{icon(icon_name, 22, color if color != '#eceff0' else '#c5cdcb')}
<span style="flex: 1; min-width: 0; display: flex; flex-direction: column"><span style="font-size: 16px; line-height: 22px">{label}</span>{sub_html}</span>
<span style="color: #8d9997">{icon('chevron', 20)}</span>
</button>'''

DIV = '<div style="height: 1px; background: rgba(255, 255, 255, 0.07); margin: 0 16px"></div>'
TONES = ["Balanced", "Poetic", "Scientific", "Whisper"]

profile = f'''<div style="{ROOT}">
<div aria-hidden="true" style="{FILL}; background: radial-gradient(ellipse 90% 36% at 50% 0%, rgba(124, 92, 255, 0.28), rgba(47, 230, 200, 0.08) 55%, rgba(0, 0, 0, 0) 100%)"></div>
<div style="{FILL}; display: flex; flex-direction: column">
<header style="height: 64px; flex-shrink: 0; display: flex; align-items: center; padding: 0 4px">
<a href="Home.dc.html" aria-label="Back" style="{ICONBTN}">{icon('back', 22)}</a>
<h1 style="flex: 1; margin: 0; text-align: center; font-size: 17px; line-height: 22px; font-weight: 500; color: #ffffff">Profile &amp; settings</h1>
<span style="width: 48px"></span>
</header>
<div class="hw-noscroll" style="flex: 1; min-height: 0; overflow-y: auto; padding: 8px 16px 32px; display: flex; flex-direction: column">
<div style="display: flex; flex-direction: column; align-items: center; padding: 8px 0 24px">
<span style="padding: 3px; border-radius: 50%; background: linear-gradient(135deg, #83ffe5, #7c5cff)"><span style="width: 84px; height: 84px; border-radius: 50%; display: flex; align-items: center; justify-content: center; background: #0b0d12; color: #ffffff; font-size: 36px; line-height: 1; font-weight: 300">{{{{initial}}}}</span></span>
<span style="margin-top: 14px; font-size: 22px; line-height: 28px; font-weight: 400; color: #ffffff">{{{{displayName}}}}</span>
<span style="margin-top: 2px; font-size: 14px; line-height: 20px; color: #a7b2b0">{{{{emailLine}}}}</span>
</div>
<p style="{SECTION}; padding: 0 8px 8px">You</p>
<div style="{GLASS}">
{field("Name or callsign", "name", "Your name or callsign")}
{DIV}
{field("Email", "email", "you@example.com", "email", "Optional. Only kept on this phone.")}
{DIV}
{field("Where you look up from", "city", "City or place, optional")}
</div>
<p style="{SECTION}; padding: 24px 8px 8px">Orbit 412</p>
<div style="{GLASS}">
<div style="padding: 14px 16px 16px">
<span style="display: block; font-size: 16px; line-height: 22px; color: #eceff0">Point of view</span>
<span style="display: block; font-size: 13px; line-height: 18px; color: #8d9997">How Orbit 412 describes things</span>
<span role="group" aria-label="Point of view" style="margin-top: 12px; display: flex; padding: 3px; border-radius: 999px; background: rgba(255, 255, 255, 0.07)">
<button type="button" aria-pressed="{{{{orbitOn}}}}" onClick="{{{{pickOrbit}}}}" style="flex: 1; min-height: 44px; display: flex; align-items: center; justify-content: center; gap: 8px; border: 0; border-radius: 999px; background: {{{{orbitBg}}}}; color: {{{{orbitText}}}}; font-size: 15px; line-height: 20px; font-weight: 500">{icon('sat', 18)}Orbit</button>
<button type="button" aria-pressed="{{{{groundOn}}}}" onClick="{{{{pickGround}}}}" style="flex: 1; min-height: 44px; display: flex; align-items: center; justify-content: center; gap: 8px; border: 0; border-radius: 999px; background: {{{{groundBg}}}}; color: {{{{groundText}}}}; font-size: 15px; line-height: 20px; font-weight: 500">{icon('location', 18)}Ground</button>
</span>
</div>
{DIV}
<div style="padding: 14px 16px 16px">
<span style="display: block; font-size: 16px; line-height: 22px; color: #eceff0">Tone</span>
<span style="display: block; font-size: 13px; line-height: 18px; color: #8d9997">How Orbit 412 sounds</span>
<div role="group" aria-label="Tone" style="margin-top: 12px; display: flex; flex-wrap: wrap; gap: 8px">
<sc-for list="{{{{tones}}}}" as="t" hint-placeholder-count="4">
<button type="button" aria-pressed="{{{{t.on}}}}" onClick="{{{{t.pick}}}}" style="min-height: 40px; display: flex; align-items: center; gap: 6px; padding: 0 14px; border-radius: 999px; border: 1px solid {{{{t.border}}}}; background: {{{{t.bg}}}}; color: {{{{t.color}}}}; font-size: 14px; line-height: 20px; font-weight: 500">{sparkle(14)}{{{{t.label}}}}</button>
</sc-for>
</div>
</div>
{DIV}
{switch_row("Read answers aloud", "Orbit 412 speaks each answer", "voice")}
{DIV}
{switch_row("Haptics", "A soft tap when an answer lands", "haptics")}
</div>
<p style="{SECTION}; padding: 24px 8px 8px">Your transmissions</p>
<div style="{GLASS}; overflow: hidden">
{nav_row('upload', 'Export transmissions', sub='Save all conversations as a file', first=True)}
{nav_row('delete', 'Clear all transmissions', color='#ffb4ab', sub='This can’t be undone')}
</div>
<p style="{SECTION}; padding: 24px 8px 8px">About</p>
<div style="{GLASS}; overflow: hidden">
{nav_row('info', 'About Human Wonder', sub='Version 2.0', first=True)}
{nav_row('info', 'Open-source licenses')}
</div>
<p style="margin: 28px 0 0; text-align: center; font-size: 12px; line-height: 16px; color: #8d9997">Human Wonder 2.0</p>
</div>
</div>
</div>'''

PROFILE_JS = """class Component extends DCLogic {
  constructor(...args) {
    super(...args);
    this.state = { name: ((this.props && this.props.name) || '').trim(), email: '', city: '', view: 'orbit', tone: 0, voice: false, haptics: true };
  }

  sw(on) {
    return {
      On: on,
      Track: on ? '#83ffe5' : 'rgba(255, 255, 255, 0.10)',
      Border: on ? '#83ffe5' : 'rgba(255, 255, 255, 0.35)',
      Thumb: on ? '#00201a' : 'rgba(255, 255, 255, 0.75)',
      Left: on ? '24px' : '6px',
      Knob: on ? '22px' : '16px',
    };
  }

  renderVals() {
    var s = this.state;
    var name = s.name.trim() || (this.props.name || '').trim() || 'Wanderer';
    var orbit = s.view === 'orbit';
    var vals = {
      name: s.name, email: s.email, city: s.city, displayName: name,
      initial: name.charAt(0).toUpperCase(),
      emailLine: s.email.trim() || 'Your Human Wonder profile',
      on_name: (e) => this.setState({ name: e.target.value.slice(0, 40) }),
      on_email: (e) => this.setState({ email: e.target.value.slice(0, 80) }),
      on_city: (e) => this.setState({ city: e.target.value.slice(0, 60) }),
      orbitOn: orbit, groundOn: !orbit,
      orbitBg: orbit ? 'rgba(255, 255, 255, 0.14)' : 'transparent',
      groundBg: orbit ? 'transparent' : 'rgba(255, 255, 255, 0.14)',
      orbitText: orbit ? '#ffffff' : '#a7b2b0',
      groundText: orbit ? '#a7b2b0' : '#ffffff',
      pickOrbit: () => this.setState({ view: 'orbit' }),
      pickGround: () => this.setState({ view: 'ground' }),
      tones: ['Balanced', 'Poetic', 'Scientific', 'Whisper'].map((label, i) => ({
        label: label, on: s.tone === i,
        bg: s.tone === i ? 'rgba(131, 255, 229, 0.14)' : 'transparent',
        border: s.tone === i ? 'rgba(131, 255, 229, 0.6)' : 'rgba(255, 255, 255, 0.16)',
        color: s.tone === i ? '#ffffff' : '#c5cdcb',
        pick: () => this.setState({ tone: i }),
      })),
      toggle_voice: () => this.setState((st) => ({ voice: !st.voice })),
      toggle_haptics: () => this.setState((st) => ({ haptics: !st.haptics })),
    };
    var v = this.sw(s.voice), hp = this.sw(s.haptics);
    Object.keys(v).forEach((k) => { vals['voice' + k] = v[k]; vals['haptics' + k] = hp[k]; });
    return vals;
  }
}"""

(PROJECT / "Profile.dc.html").write_text(page("Human Wonder — Profile & settings", profile, PROFILE_JS))
print("profile written")
