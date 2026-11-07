"""Shared JS for Orbit 412's pass over the Earth photo (Name screen and the Home card).

Geometry is measured on bg_earth_orbit.jpg (768 x 1376): the top of the airglow peaks at y=402
in the middle of the photo and sits ~26 px lower at the screen edges. Orbit 412 flies 45 px above
it (apex y=357, ends 40 px lower), i.e. in clear space just above the atmosphere. The Kotlin
SatelliteArc gets the same numbers (see EARTH_ARC_* in Sky.kt).
"""


def orbit_methods(place_js, period_ms, start_at, label):
    """place_js: JS expression body computing [arcY, drop] from w, h (canvas px)."""
    label_js = """
    ctx.font = '500 11px Geist, system-ui, sans-serif';
    ctx.textBaseline = 'bottom';
    ctx.fillStyle = 'rgba(236, 239, 240, 0.78)';
    ctx.fillText('Orbit 412', sat[0] + 9, sat[1] - 7);""" if label else ""
    return f"""
  componentDidMount() {{
    this._t0 = performance.now();
    var loop = (now) => {{ this.drawOrbit(now); this._raf = requestAnimationFrame(loop); }};
    this._raf = requestAnimationFrame(loop);
  }}

  componentWillUnmount() {{
    cancelAnimationFrame(this._raf);
  }}

  drawOrbit(now) {{
    var c = this.orbitEl;
    if (!c) return;
    var dpr = Math.min(2, window.devicePixelRatio || 1);
    var w = c.clientWidth, h = c.clientHeight;
    if (!w || !h) return;
    if (c.width !== Math.round(w * dpr) || c.height !== Math.round(h * dpr)) {{
      c.width = Math.round(w * dpr);
      c.height = Math.round(h * dpr);
    }}
    var ctx = c.getContext('2d');
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    ctx.clearRect(0, 0, w, h);
    var place = (() => {{ {place_js} }})();
    var arcY = place[0], drop = place[1];
    var p0x = -0.12 * w, p0y = arcY + drop, p1x = 0.5 * w, p1y = arcY - drop, p2x = 1.12 * w, p2y = arcY + drop;
    var at = (u) => {{
      var a = (1 - u) * (1 - u), b = 2 * (1 - u) * u, q = u * u;
      return [a * p0x + b * p1x + q * p2x, a * p0y + b * p1y + q * p2y];
    }};
    var circle = (x, y, r) => {{ ctx.beginPath(); ctx.arc(x, y, Math.max(0.1, r), 0, Math.PI * 2); }};
    var travel = ((now - this._t0) / {period_ms} + {start_at}) % 1;
    var u = 0.08 + 0.84 * travel;
    var sat = at(u);
    // Comet trail: only where Orbit 412 has just been.
    for (var k = 1; k <= 18; k++) {{
      var pt = at(Math.max(0, u - k * 0.004));
      ctx.fillStyle = 'rgba(131, 255, 229, ' + (0.32 * (1 - k / 19)).toFixed(3) + ')';
      circle(pt[0], pt[1], 2.0 - k * 0.09);
      ctx.fill();
    }}
    var glow = ctx.createRadialGradient(sat[0], sat[1], 0, sat[0], sat[1], 13);
    glow.addColorStop(0, 'rgba(131, 255, 229, 0.5)');
    glow.addColorStop(1, 'rgba(131, 255, 229, 0)');
    ctx.fillStyle = glow;
    circle(sat[0], sat[1], 13);
    ctx.fill();
    ctx.fillStyle = '#ffffff';
    circle(sat[0], sat[1], 2.3);
    ctx.fill();{label_js}
  }}
"""


# Name: photo box starts at 17% of the height, 83% tall, cover-scaled and top-aligned.
NAME_PLACE = "var boxH = 0.83 * h; var s = Math.max(w / 768, boxH / 1376); return [0.17 * h + 357 * s, 40 * s];"
# Home card: photo at card width, shifted up by 30% of the width.
CARD_PLACE = "var s = w / 768; return [357 * s - 0.30 * w, 40 * s];"
