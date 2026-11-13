import SwiftUI

/// One calm pass of Orbit 412 across the screen (same everywhere; the owner asked for slow).
let orbitPassSeconds: Double = 90

/// Where Orbit 412's arc sits: the apex height and how much lower its ends are, from the canvas size.
typealias ArcPlacement = (CGSize) -> (apexY: CGFloat, drop: CGFloat)

/// Orbit 412 gliding across the sky with a comet trail and its name beside it. Same path, trail,
/// glow and label as the Android app and the preview.
struct SatelliteArc: View {
    var placement: ArcPlacement
    var visibility: Double = 1
    var period: Double = orbitPassSeconds
    var startAt: Double = 0.3
    var label = true
    var labelAbove = false
    @State private var start = Date()

    var body: some View {
        TimelineView(.animation(minimumInterval: 1.0 / 30.0)) { timeline in
            Canvas { ctx, size in
                let w = size.width
                let (arcY, drop) = placement(size)
                let p0 = CGPoint(x: -0.12 * w, y: arcY + drop)
                let p1 = CGPoint(x: 0.5 * w, y: arcY - drop)
                let p2 = CGPoint(x: 1.12 * w, y: arcY + drop)
                func at(_ u: CGFloat) -> CGPoint {
                    let a = (1 - u) * (1 - u), b = 2 * (1 - u) * u, c = u * u
                    return CGPoint(x: a * p0.x + b * p1.x + c * p2.x, y: a * p0.y + b * p1.y + c * p2.y)
                }
                let travel = (timeline.date.timeIntervalSince(start) / period + startAt).truncatingRemainder(dividingBy: 1)
                let u = 0.08 + 0.84 * CGFloat(travel)
                let sat = at(u)
                for k in 1...18 {
                    let r = 2.0 - CGFloat(k) * 0.09
                    let c = at(max(0, u - CGFloat(k) * 0.004))
                    ctx.fill(Path(ellipseIn: CGRect(x: c.x - r, y: c.y - r, width: 2 * r, height: 2 * r)),
                             with: .color(HW.primary.opacity(0.32 * (1 - Double(k) / 19))))
                }
                ctx.fill(Path(ellipseIn: CGRect(x: sat.x - 13, y: sat.y - 13, width: 26, height: 26)),
                         with: .radialGradient(Gradient(colors: [HW.primary.opacity(0.5), .clear]), center: sat, startRadius: 0, endRadius: 13))
                ctx.fill(Path(ellipseIn: CGRect(x: sat.x - 2.3, y: sat.y - 2.3, width: 4.6, height: 4.6)), with: .color(.white))
                if label {
                    let text = ctx.resolve(Text("Orbit 412").font(HWType.satelliteLabel.fixedFont).foregroundColor(HW.ink.opacity(0.78)))
                    let s = text.measure(in: CGSize(width: 200, height: 40))
                    let y = labelAbove ? sat.y - 7 - s.height : sat.y + 7
                    ctx.draw(text, at: CGPoint(x: sat.x + 9, y: y), anchor: .topLeading)
                }
            }
        }
        .opacity(visibility)
        .allowsHitTesting(false)
        .accessibilityHidden(true)
    }

    /// The arc at a fraction of the screen height (the welcome photo).
    static func fraction(_ arc: CGFloat, drop: CGFloat) -> ArcPlacement {
        { size in (size.height * arc, size.height * drop) }
    }

    /// The arc a fixed distance below the top (the chat).
    static func belowTop(_ y: CGFloat) -> ArcPlacement {
        { _ in (y, 70) }
    }

    // Orbit 412's pass over bg_earth_orbit.jpg (768 x 1376): it flies 45 px above the airglow
    // (apex y = 357, ends 40 px lower). The photo box starts 17% down and is 83% tall, cropped to
    // fill and top-aligned. Same numbers as the Android app and the preview.
    static let nameScreen: ArcPlacement = { size in
        let s = max(size.width / 768, 0.83 * size.height / 1376)
        return (0.17 * size.height + 357 * s, 40 * s)
    }
}

/// The faint dot grid shared by the chat screens.
struct DotGrid: View {
    var alpha: Double = 0.09
    var body: some View {
        Canvas { ctx, size in
            let step: CGFloat = 22
            var y = step / 2
            while y < size.height {
                var x = step / 2
                while x < size.width {
                    ctx.fill(Path(ellipseIn: CGRect(x: x - 1, y: y - 1, width: 2, height: 2)), with: .color(.white.opacity(alpha)))
                    x += step
                }
                y += step
            }
        }
        .allowsHitTesting(false)
        .accessibilityHidden(true)
    }
}

/// Full-bleed photograph, breathing with a slow zoom, with a scrim only where text sits.
struct PhotoSky: View {
    let image: String
    let scrim: LinearGradient
    @State private var zoomed = false

    var body: some View {
        GeometryReader { geo in
            Image(image)
                .resizable()
                .scaledToFill()
                .frame(width: geo.size.width, height: geo.size.height)
                .scaleEffect(zoomed ? 1.08 : 1.0)
                .clipped()
                .overlay(scrim)
        }
        .background(HW.space)
        .onAppear { withAnimation(.easeInOut(duration: 24).repeatForever(autoreverses: true)) { zoomed = true } }
        .accessibilityHidden(true)
    }
}

/// A value that eases from where it was to a new target over [duration] (the sky's "calm" and
/// "energy", which the aurora reads every frame).
struct Eased {
    private(set) var from: Double
    private(set) var to: Double
    private(set) var changed = Date.distantPast
    let duration: Double

    init(_ value: Double, duration: Double) {
        from = value
        to = value
        self.duration = duration
    }

    func value(at date: Date) -> Double {
        let p = min(1, max(0, date.timeIntervalSince(changed) / duration))
        let e = p < 0.5 ? 4 * p * p * p : 1 - pow(-2 * p + 2, 3) / 2
        return from + (to - from) * e
    }

    mutating func set(_ target: Double, now: Date = Date()) {
        guard target != to else { return }
        from = value(at: now)
        to = target
        changed = now
    }
}
