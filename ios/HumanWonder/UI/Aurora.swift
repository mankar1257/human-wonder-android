import SwiftUI

/// The live aurora sky behind the chat (Aurora.metal). [calm] 0 = empty sky, 1 = conversation;
/// [energy] 1 while Orbit 412 answers; each change of [pulseKey] sends a ring up from below.
struct AuroraBackground: View {
    var calm: Double
    var energy: Double
    var pulseKey: Int

    @State private var start = Date()
    @State private var calmValue = Eased(0, duration: 1.4)
    @State private var energyValue = Eased(0, duration: 1.0)
    @State private var pulseStart: Date?

    var body: some View {
        GeometryReader { geo in
            TimelineView(.animation(minimumInterval: 1.0 / 30.0)) { timeline in
                let now = timeline.date
                let time = Float(now.timeIntervalSince(start).truncatingRemainder(dividingBy: 3600))
                let pulse = Float(pulseStart.map { now.timeIntervalSince($0) } ?? 99)
                #if HW_NO_METAL
                AuroraCanvas(time: Double(time), energy: energyValue.value(at: now), calm: calmValue.value(at: now), pulse: Double(pulse))
                #else
                Rectangle()
                    .fill(.black)
                    .colorEffect(ShaderLibrary.aurora(
                        .float2(geo.size),
                        .float(time),
                        .float(Float(energyValue.value(at: now))),
                        .float(Float(calmValue.value(at: now))),
                        .float(pulse)
                    ))
                #endif
            }
        }
        .onAppear {
            calmValue = Eased(calm, duration: 1.4)
            energyValue = Eased(energy, duration: 1.0)
        }
        .onChange(of: calm) { _, value in calmValue.set(value) }
        .onChange(of: energy) { _, value in energyValue.set(value) }
        .onChange(of: pulseKey) { _, _ in pulseStart = Date() }
        .allowsHitTesting(false)
        .accessibilityHidden(true)
    }
}

/// The same aurora drawn without a Metal shader, for builds made where Xcode has no Metal
/// compiler: the ribbon's glow, its haze and the far stars, with the same shape and colours.
struct AuroraCanvas: View {
    let time: Double
    let energy: Double
    let calm: Double
    let pulse: Double

    var body: some View {
        Canvas { ctx, size in
            ctx.fill(Path(CGRect(origin: .zero, size: size)), with: .color(.black))
            let w = size.width, h = size.height
            let base = (0.15 + (0.07 - 0.15) * calm) + 0.015 * energy
            let lift = 0.56 + (0.15 - 0.56) * calm
            func ridge(_ u: Double) -> Double {
                let edge = pow(abs(u - 0.5) * 2, 2.3)
                let wobble = 0.03 * sin(u * 5.1 + time * 0.12) + 0.02 * sin(u * 11.3 - time * 0.07)
                return base + lift * edge + wobble
            }
            var ribbon = Path()
            var haze = Path()
            haze.move(to: CGPoint(x: 0, y: h))
            let steps = 48
            for i in 0...steps {
                let u = Double(i) / Double(steps)
                let p = CGPoint(x: u * w, y: h * (1 - ridge(u)))
                if i == 0 { ribbon.move(to: p) } else { ribbon.addLine(to: p) }
                haze.addLine(to: p)
            }
            haze.addLine(to: CGPoint(x: w, y: h))
            haze.closeSubpath()
            let shift = 0.12 * sin(time * 0.06)
            func at(_ x: Double) -> Double { min(1, max(0, x + shift)) }
            let band = Gradient(stops: [
                .init(color: Color(red: 0.55, green: 0.40, blue: 1.0), location: at(0.15)),
                .init(color: Color(red: 0.28, green: 0.50, blue: 1.0), location: at(0.55)),
                .init(color: Color(red: 0.22, green: 0.92, blue: 0.82), location: at(0.95)),
            ])
            let strength = (1 - 0.42 * calm) * (1 + 0.3 * energy)
            ctx.drawLayer { layer in
                layer.opacity = 0.32 * strength
                layer.fill(haze, with: .linearGradient(band, startPoint: .zero, endPoint: CGPoint(x: w, y: 0)))
            }
            ctx.drawLayer { layer in
                layer.addFilter(.blur(radius: 26))
                layer.opacity = 0.9 * strength
                layer.stroke(ribbon, with: .linearGradient(band, startPoint: .zero, endPoint: CGPoint(x: w, y: 0)), lineWidth: 46)
            }
            ctx.drawLayer { layer in
                layer.addFilter(.blur(radius: 3))
                layer.opacity = 0.55 * strength
                layer.stroke(ribbon, with: .color(Color(red: 0.85, green: 0.85, blue: 1.0)), lineWidth: 2)
            }
            // Far stars above the ribbon (a fixed sky, twinkling slowly).
            var seed: UInt64 = 412
            func next() -> Double {
                seed = seed &* 6364136223846793005 &+ 1442695040888963407
                return Double(seed >> 11) / Double(1 << 53)
            }
            for _ in 0..<70 {
                let u = next(), v = next(), phase = next() * 40
                let y = h * (1 - v)
                guard v > ridge(u) + 0.04 else { continue }
                let a = (0.35 + 0.35 * (0.5 + 0.5 * sin(time * 1.6 + phase))) * (1 - 0.55 * calm)
                ctx.fill(Path(ellipseIn: CGRect(x: u * w - 1, y: y - 1, width: 2, height: 2)), with: .color(.white.opacity(a)))
            }
            if pulse < 2.4 {
                let r = pulse * 0.5 * h
                let fade = 1 - pulse / 2.4
                let c = CGPoint(x: w / 2, y: h * 1.05)
                ctx.stroke(Path(ellipseIn: CGRect(x: c.x - r, y: c.y - r, width: 2 * r, height: 2 * r)),
                           with: .color(HW.primary.opacity(0.35 * fade * fade)), lineWidth: 3)
            }
        }
    }
}
