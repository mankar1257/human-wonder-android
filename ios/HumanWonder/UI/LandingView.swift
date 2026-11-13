import SwiftUI

/// "Good morning|afternoon|evening|night" for the local time.
func greetingFor(_ date: Date = Date()) -> String {
    switch Calendar.current.component(.hour, from: date) {
    case 5...11: return "Good morning"
    case 12...16: return "Good afternoon"
    case 17...21: return "Good evening"
    default: return "Good night"
    }
}

/// The opening screen on every launch: the Liwa dunes under the Milky Way with Orbit 412 passing
/// over. No button: swipe up (or tap the ascent) to continue. The first time that leads to the
/// name; once a profile exists the headline is "Welcome back, <name>." and it goes into the chat.
struct LandingView: View {
    @Environment(AppModel.self) private var app
    @State private var pull: CGFloat = 0

    private let threshold: CGFloat = 96
    private let maxPull: CGFloat = 160

    var body: some View {
        let name: String? = app.returning ? app.profile.name : nil
        let label = name != nil ? "Swipe up to talk to Orbit\u{00A0}412" : "Swipe up to begin"
        ZStack {
            PhotoSky(
                image: "bg_liwa_dunes",
                scrim: LinearGradient(
                    stops: [
                        .init(color: .black.opacity(0.66), location: 0),
                        .init(color: .black.opacity(0.22), location: 0.32),
                        .init(color: .clear, location: 0.46),
                        .init(color: .clear, location: 0.62),
                        .init(color: .black.opacity(0.72), location: 1),
                    ],
                    startPoint: .top, endPoint: .bottom)
            )
            .ignoresSafeArea()
            SatelliteArc(placement: SatelliteArc.fraction(0.45, drop: 0.06), startAt: 0.18)
                .ignoresSafeArea()

            WeightedColumn {
                BrandLockup().frame(maxWidth: .infinity).frame(height: 64)
                FitStack(spacing: 14) {
                    Text(name.map { "Welcome back, \($0)." } ?? "The sky has something to tell you.")
                        .hw(HWType.welcomeHero)
                        .foregroundStyle(.white)
                        .multilineTextAlignment(.center)
                        .fixedSize(horizontal: false, vertical: true)
                } sub: {
                    Text(name != nil
                         ? "\(greetingFor()). Orbit\u{00A0}412 is overhead now, and it remembers your conversations."
                         : "Human Wonder gives the sky a voice: Orbit\u{00A0}412, a satellite passing over you tonight.")
                        .hw(HWType.chatSub)
                        .foregroundStyle(HW.ink.opacity(0.85))
                        .multilineTextAlignment(.center)
                        .frame(maxWidth: 310)
                        .fixedSize(horizontal: false, vertical: true)
                }
                .padding(.horizontal, 28)
                .padding(.top, 44)
                .rising()
                .weight(1)
                Ascent(label: label, action: go)
                    .frame(maxWidth: .infinity)
                    .padding(.bottom, 34)
            }
            // The words follow the finger a little, so the swipe feels physical.
            .offset(y: pull * 0.5)
            .opacity(1 - Double(-pull / maxPull) * 0.4)
        }
        .contentShape(Rectangle())
        .gesture(
            DragGesture(minimumDistance: 8)
                .onChanged { value in pull = min(0, max(-maxPull, value.translation.height)) }
                .onEnded { value in
                    let travelled = -value.translation.height
                    let flung = -value.predictedEndTranslation.height
                    withAnimation(.spring(response: 0.35, dampingFraction: 0.8)) { pull = 0 }
                    if travelled > threshold || (travelled > 32 && flung > threshold * 2) { go() }
                }
        )
    }

    private func go() { app.continueFromLanding() }
}

/// The swipe hint ("ascent"): three thin chevrons that light up one after another from the
/// bottom, the top one in teal with a soft glow, over a label with a slow light sweep. Tapping it
/// does what the swipe does. Same timing as the Android app (2 s wave, 3.2 s sweep).
struct Ascent: View {
    let label: String
    let action: () -> Void
    @State private var start = Date()

    var body: some View {
        Button(action: action) {
            TimelineView(.animation(minimumInterval: 1.0 / 30.0)) { timeline in
                let t = timeline.date.timeIntervalSince(start)
                let phase = (t / 2.0).truncatingRemainder(dividingBy: 1)
                let sweep = -0.6 + 2.2 * (t / 3.2).truncatingRemainder(dividingBy: 1)
                VStack(spacing: 16) {
                    Canvas { ctx, size in
                        let u = size.width / 30
                        for i in 0..<3 { // 0 = bottom
                            let local = ((phase - Double(i) * 0.1).truncatingRemainder(dividingBy: 1) + 1).truncatingRemainder(dividingBy: 1)
                            let lit: Double
                            if local < 0.35 { lit = Ascent.ease(local / 0.35) }
                            else if local < 0.70 { lit = 1 - Ascent.ease((local - 0.35) / 0.35) }
                            else { lit = 0 }
                            let top = CGFloat(2 - i) * 15 * u - 3 * CGFloat(lit)
                            var path = Path()
                            path.move(to: CGPoint(x: 3 * u, y: top + 11.5 * u))
                            path.addLine(to: CGPoint(x: 15 * u, y: top + 2.5 * u))
                            path.addLine(to: CGPoint(x: 27 * u, y: top + 11.5 * u))
                            let color: Color = i == 2 ? HW.primary : .white
                            let alpha = 0.16 + 0.84 * lit
                            if i == 2 {
                                ctx.stroke(path, with: .color(color.opacity(0.22 * alpha)), style: StrokeStyle(lineWidth: 7, lineCap: .round, lineJoin: .round))
                            }
                            ctx.stroke(path, with: .color(color.opacity(alpha)), style: StrokeStyle(lineWidth: 2.2 * u, lineCap: .round, lineJoin: .round))
                        }
                    }
                    .frame(width: 30, height: 43)
                    Text(label)
                        .hw(HWType.ascent)
                        .multilineTextAlignment(.center)
                        .foregroundStyle(LinearGradient(
                            stops: [
                                .init(color: HW.ink.opacity(0.62), location: 0),
                                .init(color: .white, location: 0.5),
                                .init(color: HW.ink.opacity(0.62), location: 1),
                            ],
                            startPoint: UnitPoint(x: sweep - 0.32, y: 0.5),
                            endPoint: UnitPoint(x: sweep + 0.32, y: 0.5)))
                }
                .padding(.horizontal, 28)
                .padding(.top, 12)
                .padding(.bottom, 4)
                .contentShape(Rectangle())
            }
        }
        .buttonStyle(PressScale(scale: 0.98))
        .accessibilityLabel(label)
    }

    /// Material's FastOutSlowIn curve, close enough for a glow.
    static func ease(_ x: Double) -> Double {
        let p = min(1, max(0, x))
        return p * p * (3 - 2 * p)
    }
}
