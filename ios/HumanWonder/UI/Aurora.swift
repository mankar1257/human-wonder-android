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
                Rectangle()
                    .fill(.black)
                    .colorEffect(ShaderLibrary.aurora(
                        .float2(geo.size),
                        .float(time),
                        .float(Float(energyValue.value(at: now))),
                        .float(Float(calmValue.value(at: now))),
                        .float(pulse)
                    ))
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
