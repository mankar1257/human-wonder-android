"""Illustrations for the iPhone tester guide: simplified drawings of each screen, with what to tap
ringed in teal and numbered. Rendered to PNG with headless Chrome at 2x.

    python3 figs.py            -> img/*.png
"""
import subprocess
from pathlib import Path

HERE = Path(__file__).resolve().parent
OUT = HERE / "img"
OUT.mkdir(exist_ok=True)
TEAL = "#0FA88F"

CSS = f"""
* {{ box-sizing: border-box; margin: 0; padding: 0; }}
body {{ font-family: 'Inter', sans-serif; background: #fff; color: #1d1d1f; -webkit-font-smoothing: antialiased; }}
.fig {{ display: flex; align-items: flex-start; gap: 22px; padding: 22px 26px; background: #F4F6F7; border-radius: 18px; width: max-content; }}
.col {{ display: flex; flex-direction: column; align-items: center; gap: 10px; }}
.cap {{ font-size: 13px; color: #444; font-weight: 500; text-align: center; max-width: 230px; line-height: 1.35; }}
.arrow {{ align-self: center; margin-bottom: 30px; font-size: 30px; color: #9aa3a8; font-weight: 300; }}
.phone {{ width: 236px; height: 480px; border-radius: 38px; background: #1d1d1f; padding: 9px; position: relative; flex: none; }}
.screen {{ width: 100%; height: 100%; border-radius: 30px; background: #F2F2F7; overflow: hidden; position: relative; display: flex; flex-direction: column; }}
.screen.white {{ background: #fff; }}
.screen.dark {{ background: #0b0f12; color: #fff; }}
.status {{ height: 30px; display: flex; justify-content: space-between; align-items: center; padding: 0 22px; font-size: 11px; font-weight: 600; flex: none; }}
.island {{ position: absolute; top: 8px; left: 50%; transform: translateX(-50%); width: 70px; height: 20px; background: #1d1d1f; border-radius: 12px; }}
.nav {{ padding: 4px 14px 8px; font-size: 11.5px; color: #0a84ff; height: 22px; flex: none; }}
.title {{ font-size: 21px; font-weight: 700; padding: 0 14px 10px; letter-spacing: -0.2px; flex: none; }}
.group {{ margin: 0 10px 12px; background: #fff; border-radius: 10px; overflow: hidden; flex: none; }}
.gh {{ font-size: 9.5px; color: #6e6e73; padding: 0 22px 4px; text-transform: uppercase; letter-spacing: .3px; flex: none; }}
.row {{ display: flex; align-items: center; gap: 9px; padding: 9px 12px; font-size: 12px; border-bottom: 0.5px solid #e5e5ea; position: relative; }}
.row:last-child {{ border-bottom: 0; }}
.row .v {{ margin-left: auto; color: #8e8e93; font-size: 11px; }}
.row .chev {{ color: #c4c4c7; font-size: 13px; margin-left: 6px; }}
.ico {{ width: 22px; height: 22px; border-radius: 6px; flex: none; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 11px; font-weight: 700; }}
.hl {{ outline: 3px solid {TEAL}; outline-offset: -2px; border-radius: 8px; box-shadow: 0 0 0 6px rgba(15,168,143,.18); z-index: 2; }}
.badge {{ position: absolute; width: 22px; height: 22px; border-radius: 11px; background: {TEAL}; color: #fff; font-size: 12px; font-weight: 700; display: flex; align-items: center; justify-content: center; z-index: 5; box-shadow: 0 1px 3px rgba(0,0,0,.25); }}
.dim {{ position: absolute; inset: 0; background: rgba(0,0,0,.35); display: flex; align-items: center; justify-content: center; z-index: 3; }}
.alert {{ width: 200px; background: rgba(246,246,248,.98); border-radius: 13px; text-align: center; overflow: visible; position: relative; }}
.alert h4 {{ font-size: 12.5px; font-weight: 600; padding: 14px 12px 4px; line-height: 1.3; }}
.alert p {{ font-size: 10.5px; color: #333; padding: 0 12px 12px; line-height: 1.35; }}
.alert .btns {{ display: flex; border-top: 0.5px solid #c8c8cc; }}
.alert .btns div {{ flex: 1; padding: 10px 4px; font-size: 12.5px; color: #0a84ff; position: relative; }}
.alert .btns div + div {{ border-left: 0.5px solid #c8c8cc; }}
.alert .btns.stack {{ flex-direction: column; }}
.alert .btns.stack div + div {{ border-left: 0; border-top: 0.5px solid #c8c8cc; }}
.b {{ font-weight: 600; }}
.btn {{ background: #0a84ff; color: #fff; border-radius: 12px; padding: 11px 16px; font-size: 13px; font-weight: 600; text-align: center; position: relative; }}
.pill {{ white-space: nowrap; background: #e9f1ff; color: #0a84ff; border-radius: 12px; padding: 4px 9px; font-size: 10px; font-weight: 700; position: relative; }}
.tabbar {{ margin-top: auto; height: 46px; background: rgba(250,250,252,.95); border-top: 0.5px solid #d1d1d6; display: flex; justify-content: space-around; align-items: center; font-size: 9px; color: #8e8e93; flex: none; }}
.tabbar .on {{ color: #0a84ff; font-weight: 600; }}
.toggle {{ width: 34px; height: 20px; border-radius: 10px; background: #e5e5ea; position: relative; margin-left: auto; }}
.toggle::after {{ content: ''; position: absolute; top: 2px; left: 2px; width: 16px; height: 16px; border-radius: 8px; background: #fff; box-shadow: 0 1px 2px rgba(0,0,0,.3); }}
.toggle.on {{ background: #34c759; }}
.toggle.on::after {{ left: 16px; }}
.mac {{ background: #fff; border-radius: 11px; box-shadow: 0 8px 24px rgba(0,0,0,.12), 0 0 0 0.5px rgba(0,0,0,.2); overflow: hidden; flex: none; }}
.mac .bar {{ height: 30px; background: #ececec; display: flex; align-items: center; gap: 7px; padding: 0 11px; font-size: 11.5px; color: #555; border-bottom: 0.5px solid #d0d0d0; }}
.mac .bar i {{ width: 11px; height: 11px; border-radius: 6px; background: #d0d0d0; display: inline-block; }}
.mac .bar span {{ margin-left: 10px; }}
.mac .body {{ padding: 16px 18px; font-size: 12.5px; position: relative; }}
.url {{ background: #fff; border: 0.5px solid #ccc; border-radius: 7px; padding: 4px 10px; font-size: 11px; color: #333; margin-left: 12px; flex: 1; }}
.file {{ display: flex; align-items: center; gap: 10px; padding: 8px 10px; border-bottom: 0.5px solid #e6e6e6; font-size: 12px; position: relative; }}
.file .sz {{ margin-left: auto; color: #888; font-size: 11px; }}
.docico {{ width: 22px; height: 26px; border-radius: 3px; background: #e8ebee; border: 1px solid #c9ced3; flex: none; }}
.big {{ font-size: 15px; font-weight: 600; }}
.note {{ font-size: 11px; color: #6e6e73; }}
.hwicon {{ width: 52px; height: 52px; border-radius: 12px; background: url(hw-icon.png) center/cover; }}
.appico {{ width: 52px; height: 52px; border-radius: 12px; }}
.label {{ font-size: 9.5px; text-align: center; margin-top: 4px; color: #fff; }}
"""


def badge(n, top, left):
    return f'<div class="badge" style="top:{top}px;left:{left}px">{n}</div>'


def phone(inner, cls="", extra="", height=480):
    return f'<div class="phone" style="height:{height}px"><div class="screen {cls}"><div class="island"></div><div class="status"><span>9:41</span><span>●●● ▮</span></div>{inner}</div>{extra}</div>'


def rows(items):
    out = []
    for it in items:
        label, *rest = it
        opts = rest[0] if rest else {}
        ico = f'<div class="ico" style="background:{opts["ico"]}">{opts.get("glyph", "")}</div>' if "ico" in opts else ""
        v = f'<span class="v">{opts["v"]}</span>' if "v" in opts else ""
        tog = f'<div class="toggle {"on" if opts["toggle"] else ""}"></div>' if "toggle" in opts else ""
        chev = '<span class="chev">›</span>' if opts.get("chev", True) and "toggle" not in opts else ""
        hl = " hl" if opts.get("hl") else ""
        style = f' style="color:{opts["color"]}"' if "color" in opts else ""
        out.append(f'<div class="row{hl}"{style}>{ico}<span>{label}</span>{v}{tog}{chev}</div>')
    return '<div class="group">' + "".join(out) + "</div>"


def alert(title, msg, buttons, hl=None, stack=False):
    b = "".join(f'<div class="{"b " if i == len(buttons) - 1 else ""}{"hl" if i == hl else ""}">{t}</div>' for i, t in enumerate(buttons))
    return f'<div class="dim"><div class="alert"><h4>{title}</h4><p>{msg}</p><div class="btns {"stack" if stack else ""}">{b}</div></div></div>'


def col(html, cap):
    return f'<div class="col">{html}<div class="cap">{cap}</div></div>'


ARROW = '<div class="arrow">→</div>'


def render(name, body, width, height):
    page = HERE / f"_{name}.html"
    page.write_text(f"<!DOCTYPE html><html><head><meta charset='utf-8'><style>{CSS}</style></head><body>{body}</body></html>")
    png = OUT / f"{name}.png"
    subprocess.run(["google-chrome", "--headless=new", "--no-sandbox", "--disable-gpu", "--hide-scrollbars",
                    "--force-device-scale-factor=2", f"--window-size={width},{height}",
                    f"--screenshot={png}", f"file://{page}"], check=True, capture_output=True)
    # Trim the page margin around the figure.
    from PIL import Image, ImageChops
    im = Image.open(png).convert("RGB")
    bg = Image.new("RGB", im.size, (255, 255, 255))
    box = ImageChops.difference(im, bg).getbbox()
    if box:
        im = im.crop((max(box[0] - 4, 0), max(box[1] - 4, 0), min(box[2] + 4, im.width), min(box[3] + 4, im.height)))
    im.save(png, optimize=True)
    page.unlink()
    return png


# ---------- figures ----------

def overview():
    mac = '<div class="mac" style="width:200px"><div class="bar"><i></i><i></i><i></i><span>iloader</span></div><div class="body" style="height:110px;display:flex;flex-direction:column;justify-content:center;align-items:center;gap:8px"><div class="big">Your Mac</div><div class="note">runs iloader once</div></div></div>'
    ph1 = phone('<div style="padding:40px 16px;text-align:center"><div class="appico" style="background:#5856d6;margin:0 auto"></div><div style="margin-top:8px;font-size:12px;font-weight:600">SideStore</div><div class="note" style="margin-top:14px">installs and renews apps<br>over Wi-Fi</div></div>', "white", height=270)
    ph2 = phone('<div style="padding:40px 16px;text-align:center"><div class="hwicon" style="margin:0 auto"></div><div style="margin-top:8px;font-size:12px;font-weight:600">Human Wonder</div><div class="note" style="margin-top:14px">works offline</div></div>', "white", height=270)
    body = f'''<div class="fig" style="align-items:center">
      {col(mac, "<b>Once, with the cable:</b><br>iloader puts SideStore on the iPhone")}
      <div class="col" style="align-self:center;margin-bottom:40px"><div class="arrow" style="margin:0">⟶</div><div class="note">cable</div></div>
      {col(ph1, "<b>SideStore</b> on the iPhone signs apps with your Apple ID")}
      <div class="col" style="align-self:center;margin-bottom:40px"><div class="arrow" style="margin:0">⟶</div><div class="note">Wi-Fi</div></div>
      {col(ph2, "<b>Human Wonder</b> installed from the .ipa file")}
    </div>'''
    return render("01-overview", body, 1000, 640)


def localdevvpn():
    p1 = phone('<div class="nav">‹ Search</div><div class="title">LocalDevVPN</div>' + rows([("LocalDevVPN", {"ico": "#2c7be5", "glyph": "VPN", "v": '<span class="pill hl">Get</span>', "chev": False})]) + '<div class="note" style="padding:6px 16px">Search for it in the App Store and tap Get.</div>' + badge(1, 112, 186))
    p2 = phone('<div style="flex:1;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:14px"><div class="big">LocalDevVPN</div><div class="note">Not connected</div><div class="btn hl" style="width:150px">Connect</div></div>' + badge(2, 268, 196), "white")
    p3 = phone('<div style="flex:1"></div>' + alert("“LocalDevVPN” Would Like to Add VPN Configurations", "All network activity on this iPhone may be filtered or monitored when using VPN.", ["Allow", "Don’t Allow"], hl=0, stack=True) + badge(3, 268, 196), "white")
    body = f'<div class="fig">{col(p1, "Install LocalDevVPN")}{ARROW}{col(p2, "Open it and tap <b>Connect</b>")}{ARROW}{col(p3, "Tap <b>Allow</b>, then enter your passcode")}</div>'
    return render("02-localdevvpn", body, 1000, 640)


def download_iloader():
    win = f'''<div class="mac" style="width:640px"><div class="bar"><i></i><i></i><i></i><div class="url">github.com/nab138/iloader/releases/latest/download/iloader-darwin-universal.dmg</div><span style="margin-left:10px;font-size:15px">⤓</span></div>
      <div class="body" style="height:130px">
        <div class="note" style="margin-bottom:10px;max-width:260px;line-height:1.4">Click the link in this guide. The download starts by itself.</div>
        <div style="position:absolute;right:14px;top:10px;width:300px;background:#f7f7f7;border-radius:10px;box-shadow:0 6px 18px rgba(0,0,0,.16);padding:10px">
          <div style="font-weight:600;font-size:12px;margin-bottom:8px">Downloads</div>
          <div class="file hl" style="border-bottom:0"><div class="docico"></div><span>iloader-darwin-universal.dmg</span><span class="sz">✓</span></div>
        </div>
        {badge(1, 4, 318)}</div></div>'''
    body = f'<div class="fig">{col(win.replace("max-width: 230px", ""), "<span style=\'display:inline-block;max-width:460px\'>The file <b>iloader-darwin-universal.dmg</b> lands in your <b>Downloads</b> folder</span>")}</div>'
    return render("02-download-iloader", body, 780, 420)


def install_iloader():
    dmg = f'''<div class="mac" style="width:330px"><div class="bar"><i></i><i></i><i></i><span>iloader</span></div>
      <div class="body" style="height:170px;display:flex;align-items:center;justify-content:space-around">
        <div class="col"><div class="appico hl" style="background:#5b6b7a"></div><div class="note">iloader</div></div>
        <div class="arrow">⟶</div>
        <div class="col"><div class="appico" style="background:#7aa7e0"></div><div class="note">Applications</div></div>
        {badge(1, 40, 40)}</div></div>'''
    sec = f'''<div class="mac" style="width:330px"><div class="bar"><i></i><i></i><i></i><span>Privacy &amp; Security</span></div>
      <div class="body" style="height:170px"><div style="font-weight:600;margin-bottom:8px">Security</div>
      <div class="note" style="line-height:1.4;margin-bottom:14px">“iloader” was blocked to protect your Mac.</div>
      <div style="display:inline-block;border:0.5px solid #bbb;border-radius:6px;padding:5px 12px;font-size:12px;position:relative" class="hl">Open Anyway</div>
      {badge(2, 92, 128)}</div></div>'''
    body = f'<div class="fig">{col(dmg, "Open the downloaded file and drag <b>iloader</b> onto <b>Applications</b>")}{col(sec, "Only if macOS blocks it: <b>System Settings → Privacy &amp; Security → Open Anyway</b>")}</div>'
    return render("03-install-iloader", body, 820, 380)


def trust_computer():
    mac = '<div class="mac" style="width:220px"><div class="bar"><i></i><i></i><i></i><span>iloader</span></div><div class="body" style="height:120px;display:flex;align-items:center;justify-content:center"><div class="big">Your Mac</div></div></div>'
    ph = phone('<div style="flex:1"></div>' + alert("Trust This Computer?", "Your settings and data will be accessible from this computer when connected.", ["Trust", "Don’t Trust"], hl=0, stack=True) + badge(1, 268, 196), "white")
    body = f'''<div class="fig" style="align-items:center">{col(mac, "")}<div class="col"><div style="width:110px;height:6px;border-radius:3px;background:#bfc6cb"></div><div class="note">cable</div></div>
      {col(ph, "Unlock the iPhone, tap <b>Trust</b>, then enter your passcode")}</div>'''
    return render("04-trust-computer", body, 820, 640)


def iloader_app():
    signin = f'''<div class="mac" style="width:330px"><div class="bar"><i></i><i></i><i></i><span>iloader</span></div>
      <div class="body" style="height:230px;display:flex;flex-direction:column;gap:10px">
        <div class="big">Sign in with your Apple ID</div>
        <div style="border:0.5px solid #c7c7cc;border-radius:7px;padding:7px 9px;color:#999">your-name@example.com</div>
        <div style="border:0.5px solid #c7c7cc;border-radius:7px;padding:7px 9px;color:#999">••••••••••</div>
        <div class="btn hl" style="width:110px;padding:7px">Sign in</div>
        <div class="note">Then type the 6-digit code Apple shows on your other device.</div>
        {badge(1, 118, 120)}</div></div>'''
    install = f'''<div class="mac" style="width:330px"><div class="bar"><i></i><i></i><i></i><span>iloader</span></div>
      <div class="body" style="height:230px;display:flex;flex-direction:column;gap:10px">
        <div class="note">Device</div>
        <div style="border:0.5px solid #c7c7cc;border-radius:7px;padding:7px 9px">Your iPhone ▾</div>
        <div class="btn hl" style="margin-top:6px">Install SideStore (Stable)</div>
        <div style="border:0.5px solid #c7c7cc;border-radius:12px;padding:9px;text-align:center;color:#555;font-size:12px">Other options</div>
        <div class="note">Wait until it says it is done. Keep the cable in.</div>
        {badge(2, 78, 300)}</div></div>'''
    body = f'<div class="fig">{col(signin, "Sign in with <b>your</b> Apple ID")}{ARROW}{col(install, "Choose your iPhone, click <b>Install SideStore (Stable)</b>")}</div>'
    return render("06-iloader", body, 860, 420)


def trust_developer():
    p1 = phone('<div class="title" style="padding-top:8px">Settings</div>' + rows([("Wi-Fi", {"ico": "#0a84ff", "v": "Home"}), ("Bluetooth", {"ico": "#0a84ff"}), ("General", {"ico": "#8e8e93", "hl": True}), ("Privacy &amp; Security", {"ico": "#0a84ff"})]) + badge(1, 150, 198))
    p2 = phone('<div class="nav">‹ Settings</div><div class="title">General</div>' + rows([("About", {}), ("Software Update", {})]) + rows([("Date &amp; Time", {}), ("Keyboard", {})]) + rows([("VPN &amp; Device Management", {"hl": True})]) + badge(1, 268, 198).replace(">1<", ">2<"))
    p3 = phone('<div class="nav">‹ General</div><div class="title" style="font-size:16px">VPN &amp; Device Management</div>' + rows([("VPN", {"v": "Connected"})]) + '<div class="gh">Developer App</div>' + rows([("Apple Development: your-name@…", {"hl": True})]) + badge(3, 186, 198))
    p4 = phone('<div class="nav">‹ Back</div><div style="padding:6px 14px;font-size:11px;color:#333;line-height:1.4">Apps from developer “Apple Development: your-name@…” are not trusted on this iPhone.</div>' + rows([("Trust “Apple Development: your-name@…”", {"color": "#0a84ff", "chev": False})]) + alert("Trust “Apple Development: your-name@…” Apps on This iPhone", "", ["Cancel", "Trust"], hl=1) + badge(4, 286, 168))
    body = f'<div class="fig">{col(p1, "Open <b>Settings</b> → <b>General</b>")}{col(p2, "Tap <b>VPN &amp; Device Management</b>")}{col(p3, "Tap your Apple ID under <b>Developer App</b>")}{col(p4, "Tap <b>Trust</b> (twice). The iPhone needs internet.")}</div>'
    return render("06-trust-developer", body, 1200, 640)


def developer_mode():
    p1 = phone('<div class="nav">‹ Settings</div><div class="title" style="font-size:17px">Privacy &amp; Security</div>' + rows([("Location Services", {"v": "On"}), ("Tracking", {})]) + '<div class="gh">Security</div>' + rows([("Lockdown Mode", {"v": "Off"}), ("Developer Mode", {"v": "Off", "hl": True})]) + '<div class="note" style="padding:2px 18px">Scroll to the very bottom.</div>' + badge(1, 228, 198))
    p2 = phone('<div class="nav">‹ Privacy &amp; Security</div><div class="title" style="font-size:17px">Developer Mode</div>' + rows([("Developer Mode", {"toggle": True})]) + alert("Turn On Developer Mode?", "You need to restart your iPhone to turn this on.", ["Cancel", "Restart"], hl=1) + badge(2, 286, 168))
    p3 = phone('<div style="flex:1;display:flex;align-items:flex-end;justify-content:center;padding-bottom:28px;color:#fff;font-size:11px">After the restart, unlock the iPhone</div>' + alert("Turn On Developer Mode?", "Turning on Developer Mode reduces the security of your iPhone.", ["Cancel", "Turn On"], hl=1) + badge(3, 286, 168), "dark")
    body = f'<div class="fig">{col(p1, "<b>Settings → Privacy &amp; Security</b> → <b>Developer Mode</b>")}{ARROW}{col(p2, "Switch it on, tap <b>Restart</b>")}{ARROW}{col(p3, "Tap <b>Turn On</b>, then enter your passcode")}</div>'
    return render("07-developer-mode", body, 1000, 640)


def sidestore_setup():
    p1 = phone('<div style="flex:1"></div>' + alert("“SideStore” Would Like to Find and Connect to Devices on Your Local Network", "", ["Don’t Allow", "Allow"], hl=1) + '<div class="tabbar"><span>News</span><span>Browse</span><span>My Apps</span><span>Settings</span></div>' + badge(1, 290, 168))
    p2 = phone('<div class="title" style="padding-top:8px">Settings</div><div class="gh">Account</div>' + rows([("Sign in with Apple ID", {"color": "#0a84ff", "hl": True})]) + '<div class="note" style="padding:4px 18px">Use the same Apple ID as in iloader.</div><div class="tabbar"><span>News</span><span>Browse</span><span>My Apps</span><span class="on">Settings</span></div>' + badge(2, 108, 198))
    p3 = phone('<div class="title" style="padding-top:8px">My Apps</div>' + '<div class="group"><div class="row"><div class="ico" style="background:#5856d6"></div><span>SideStore</span><span style="margin-left:auto" class="pill hl">7 DAYS</span></div></div><div class="tabbar"><span>News</span><span>Browse</span><span class="on">My Apps</span><span>Settings</span></div>' + badge(3, 96, 190))
    body = f'<div class="fig">{col(p1, "Open SideStore, tap <b>Allow</b> (and allow notifications)")}{ARROW}{col(p2, "<b>Settings</b> tab → <b>Sign in</b> with your Apple ID")}{ARROW}{col(p3, "<b>My Apps</b> tab → tap <b>7 DAYS</b> next to SideStore")}</div>'
    return render("09-sidestore-setup", body, 1000, 640)


def airdrop():
    win = f'''<div class="mac" style="width:360px"><div class="bar"><i></i><i></i><i></i><span>Downloads</span></div>
      <div class="body" style="height:230px">
        <div class="file"><div class="docico"></div><span>HumanWonder-iPhone.ipa</span><span class="sz">400 MB</span></div>
        <div style="position:absolute;left:150px;top:46px;width:150px;background:#f6f6f6;border-radius:8px;box-shadow:0 4px 14px rgba(0,0,0,.18);padding:5px;font-size:12px">
          <div style="padding:4px 8px">Open</div><div style="padding:4px 8px">Get Info</div>
          <div style="padding:4px 8px;background:#0a84ff;color:#fff;border-radius:5px">Share… ›</div>
          <div style="padding:4px 8px" class="hl">AirDrop</div></div>
        {badge(1, 120, 312)}</div></div>'''
    ph = phone('<div class="nav"> </div><div class="title" style="font-size:16px">AirDrop</div><div style="padding:6px 16px;font-size:11.5px;line-height:1.4">HumanWonder-iPhone.ipa</div>' + rows([("Open with SideStore", {"color": "#0a84ff", "chev": False, "hl": True}), ("Save to Files", {"color": "#0a84ff", "chev": False})]) + '<div class="note" style="padding:6px 16px;line-height:1.4">If you only see “Save to Files”, save it and use the + button in SideStore (next picture).</div>' + badge(2, 128, 198), "")
    body = f'<div class="fig">{col(win, "On the Mac: right-click the file → <b>Share</b> → <b>AirDrop</b> → your iPhone")}{ARROW}{col(ph, "On the iPhone: open it with <b>SideStore</b>, or <b>Save to Files</b>")}</div>'
    return render("10-airdrop", body, 900, 640)


def install_app():
    p1 = phone('<div class="nav" style="font-size:18px;padding-top:0;height:34px"><span class="hl" style="padding:0 6px">+</span></div><div class="title">My Apps</div>' + '<div class="group"><div class="row"><div class="ico" style="background:#5856d6"></div><span>SideStore</span><span style="margin-left:auto" class="pill">7 DAYS</span></div></div><div class="tabbar"><span>News</span><span>Browse</span><span class="on">My Apps</span><span>Settings</span></div>' + badge(1, 36, 52))
    p2 = phone('<div class="nav">Cancel</div><div class="title" style="font-size:17px">Downloads</div><div style="display:flex;gap:16px;padding:8px 18px"><div class="col hl" style="padding:6px;position:relative"><div class="docico" style="width:46px;height:56px"></div><div class="note" style="font-size:9.5px;color:#1d1d1f">HumanWonder-<br>iPhone.ipa</div></div></div>' + badge(2, 108, 86))
    p3 = phone('<div class="title" style="padding-top:8px">My Apps</div><div class="group"><div class="row"><div class="ico" style="background:#5856d6"></div><span>SideStore</span><span style="margin-left:auto" class="pill">7 DAYS</span></div><div class="row"><div class="ico" style="background:url(hw-icon.png) center/cover"></div><span>Human Wonder</span><span class="v">Installing…</span></div></div><div style="margin:4px 18px;height:4px;border-radius:2px;background:#d1d1d6"><div style="width:60%;height:100%;border-radius:2px;background:#0a84ff"></div></div><div class="note" style="padding:8px 18px">A few minutes: the file is large.</div><div class="tabbar"><span>News</span><span>Browse</span><span class="on">My Apps</span><span>Settings</span></div>')
    p4 = phone('<div style="display:grid;grid-template-columns:repeat(4,1fr);gap:14px 6px;padding:14px 10px">' + "".join('<div class="col" style="gap:0"><div class="appico" style="background:#d9dee3;width:44px;height:44px"></div></div>' for _ in range(6)) + '<div class="col hl" style="gap:0;padding:2px;position:relative"><div class="hwicon" style="width:44px;height:44px"></div><div class="label">Human Wonder</div></div></div>' + badge(4, 110, 180).replace(">4<", ">3<"), "dark")
    body = f'<div class="fig">{col(p1, "LocalDevVPN on. In <b>My Apps</b>, tap <b>+</b>")}{col(p2, "Choose <b>HumanWonder-iPhone.ipa</b>")}{col(p3, "Wait while it installs")}{col(p4, "Open <b>Human Wonder</b> from the Home Screen")}</div>'
    return render("11-install-app", body, 1200, 640)


def weekly():
    p1 = phone('<div style="flex:1;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:14px"><div class="big">LocalDevVPN</div><div class="note" style="color:#34c759;font-weight:600">Connected</div><div class="btn" style="width:150px;background:#e5e5ea;color:#1d1d1f">Disconnect</div></div>', "white")
    p2 = phone('<div class="nav" style="display:flex;justify-content:space-between;height:34px;align-items:center"><span>+</span><span class="hl" style="padding:2px 6px">Refresh All</span></div><div class="title">My Apps</div><div class="group"><div class="row"><div class="ico" style="background:#5856d6"></div><span>SideStore</span><span style="margin-left:auto" class="pill">2 DAYS</span></div><div class="row"><div class="ico" style="background:url(hw-icon.png) center/cover"></div><span>Human Wonder</span><span style="margin-left:auto" class="pill">2 DAYS</span></div></div><div class="tabbar"><span>News</span><span>Browse</span><span class="on">My Apps</span><span>Settings</span></div>' + badge(2, 50, 184))
    body = f'<div class="fig">{col(p1, "Make sure LocalDevVPN says <b>Connected</b>")}{ARROW}{col(p2, "SideStore → <b>My Apps</b> → <b>Refresh All</b>. The days go back to 7.")}</div>'
    return render("12-weekly-refresh", body, 760, 640)


# ---------- simple path: iloader installs the .ipa directly ----------

def overview_simple():
    mac = '<div class="mac" style="width:220px"><div class="bar"><i></i><i></i><i></i><span>iloader</span></div><div class="body" style="height:120px;display:flex;flex-direction:column;justify-content:center;align-items:center;gap:8px"><div class="big">Your Mac</div><div class="note">iloader + the .ipa file</div></div></div>'
    ph = phone('<div style="padding:40px 16px;text-align:center"><div class="hwicon" style="margin:0 auto"></div><div style="margin-top:8px;font-size:12px;font-weight:600">Human Wonder</div><div class="note" style="margin-top:14px">works offline</div></div>', "white", height=270)
    body = f'''<div class="fig" style="align-items:center">
      {col(mac, "<b>On the Mac:</b> iloader signs the app with your Apple ID")}
      <div class="col" style="align-self:center;margin-bottom:40px"><div class="arrow" style="margin:0">⟶</div><div class="note">cable</div></div>
      {col(ph, "<b>On the iPhone:</b> Human Wonder, ready to use")}
    </div>'''
    return render("01-overview", body, 820, 520)


def iloader_ipa():
    signin = f'''<div class="mac" style="width:300px"><div class="bar"><i></i><i></i><i></i><span>iloader</span></div>
      <div class="body" style="height:240px;display:flex;flex-direction:column;gap:10px">
        <div class="big">Sign in with your Apple ID</div>
        <div style="border:0.5px solid #c7c7cc;border-radius:7px;padding:7px 9px;color:#999">your-name@example.com</div>
        <div style="border:0.5px solid #c7c7cc;border-radius:7px;padding:7px 9px;color:#999">••••••••••</div>
        <div class="btn hl" style="width:110px;padding:7px">Sign in</div>
        <div class="note">Then type the 6-digit code Apple shows on your other device.</div>
        {badge(1, 118, 120)}</div></div>'''
    pick = f'''<div class="mac" style="width:300px"><div class="bar"><i></i><i></i><i></i><span>iloader</span></div>
      <div class="body" style="height:240px;display:flex;flex-direction:column;gap:9px">
        <div class="note">Device</div>
        <div style="border:0.5px solid #c7c7cc;border-radius:7px;padding:7px 9px">Your iPhone ▾</div>
        <div style="border:0.5px solid #c7c7cc;border-radius:12px;padding:9px;text-align:center;color:#555;font-size:12px">Install SideStore</div>
        <div class="btn hl">Import IPA</div>
        <div class="note">Skip SideStore. Use the option for your own .ipa file (it may say “Import IPA” or “Install other IPA”).</div>
        {badge(2, 122, 270)}</div></div>'''
    choose = f'''<div class="mac" style="width:300px"><div class="bar"><i></i><i></i><i></i><span>Choose a file</span></div>
      <div class="body" style="height:240px;display:flex;flex-direction:column;gap:6px">
        <div class="note" style="margin-bottom:4px">Downloads</div>
        <div class="file hl"><div class="docico"></div><span>HumanWonder-iPhone.ipa</span><span class="sz">400 MB</span></div>
        <div class="file"><div class="docico"></div><span>iloader-darwin-universal.dmg</span></div>
        <div style="margin-top:auto;display:flex;justify-content:flex-end;gap:8px"><div style="border:0.5px solid #bbb;border-radius:6px;padding:4px 12px;font-size:12px">Cancel</div><div class="btn" style="padding:4px 14px;font-size:12px;border-radius:6px">Open</div></div>
        {badge(3, 22, 268)}</div></div>'''
    body = f'<div class="fig">{col(signin, "Sign in with <b>your</b> Apple ID")}{ARROW}{col(pick, "Choose your iPhone, then <b>Import IPA</b>")}{ARROW}{col(choose, "Pick <b>HumanWonder-iPhone.ipa</b> and wait until it is done")}</div>'
    return render("05-iloader-ipa", body, 1160, 420)


def open_app():
    p = phone('<div style="display:grid;grid-template-columns:repeat(4,1fr);gap:14px 6px;padding:14px 10px">' + "".join('<div class="col" style="gap:0"><div class="appico" style="background:#d9dee3;width:44px;height:44px"></div></div>' for _ in range(6)) + '<div class="col hl" style="gap:0;padding:2px;position:relative"><div class="hwicon" style="width:44px;height:44px"></div><div class="label">Human Wonder</div></div></div>' + badge(1, 110, 180), "dark")
    body = f'<div class="fig">{col(p, "Tap <b>Human Wonder</b> on the Home Screen")}</div>'
    return render("08-open-app", body, 420, 640)


if __name__ == "__main__":
    # The guide follows the simple path: iloader installs the .ipa directly over the cable.
    for f in (overview_simple, download_iloader, install_iloader, trust_computer, iloader_ipa,
              trust_developer, developer_mode, open_app):
        p = f()
        from PIL import Image
        print(p.name, Image.open(p).size, p.stat().st_size // 1024, "KB")
