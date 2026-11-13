import SwiftUI

/// A Material Symbol / Human Wonder icon from the asset catalog (24 x 24 box), tinted.
struct HWIcon: View {
    let name: String
    var color: Color = HW.iconMuted
    var size: CGFloat = 24

    var body: some View {
        Image(name)
            .resizable()
            .renderingMode(.template)
            .interpolation(.high)
            .scaledToFit()
            .frame(width: size, height: size)
            .foregroundStyle(color)
            .accessibilityHidden(true)
    }
}

/// Press feedback without any change of colour (the colours already say what a control can do).
struct PressScale: ButtonStyle {
    var scale: CGFloat = 0.96
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(configuration.isPressed ? scale : 1)
            .opacity(configuration.isPressed ? 0.85 : 1)
            .animation(.easeOut(duration: 0.12), value: configuration.isPressed)
    }
}

/// A 48pt round icon button (the header buttons on every screen).
struct IconCircle: View {
    let icon: String
    let label: String
    var color: Color = HW.iconMuted
    var size: CGFloat = 48
    var iconSize: CGFloat = 24
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HWIcon(name: icon, color: color, size: iconSize)
                .frame(width: size, height: size)
                .contentShape(Circle())
        }
        .buttonStyle(PressScale())
        .accessibilityLabel(label)
    }
}

/// Orbit's mark, alive: a ring (the Earth) with one point of light travelling round it. It circles
/// slowly at rest and quickly while Orbit 412 is answering.
struct OrbitMark: View {
    let size: CGFloat
    var active: Bool = false

    var body: some View {
        TimelineView(.animation(minimumInterval: 1.0 / 30.0)) { timeline in
            Canvas { ctx, box in
                let t = timeline.date.timeIntervalSinceReferenceDate
                let period = active ? 1.6 : 12.0
                let angle = (t.truncatingRemainder(dividingBy: period) / period) * 360
                let side = min(box.width, box.height)
                let r = side / 2 * 0.75
                let c = CGPoint(x: box.width / 2, y: box.height / 2)
                let ring = Path(ellipseIn: CGRect(x: c.x - r, y: c.y - r, width: 2 * r, height: 2 * r))
                ctx.fill(ring, with: .color(HW.primaryContainer.opacity(0.08)))
                ctx.stroke(ring, with: .color(HW.primary.opacity(0.9)), lineWidth: 1.5)
                let a = (angle - 45) * .pi / 180
                let p = CGPoint(x: c.x + r * cos(a), y: c.y + r * sin(a))
                let glow = Path(ellipseIn: CGRect(x: p.x - 4, y: p.y - 4, width: 8, height: 8))
                ctx.fill(glow, with: .radialGradient(
                    Gradient(colors: [HW.primary.opacity(0.7), .clear]), center: p, startRadius: 0, endRadius: 4))
                let dot = side * 0.105
                ctx.fill(Path(ellipseIn: CGRect(x: p.x - dot, y: p.y - dot, width: 2 * dot, height: 2 * dot)), with: .color(.white))
            }
        }
        .frame(width: size, height: size)
        .accessibilityHidden(true)
    }
}

/// The Human Wonder logo: the orbit mark and the wordmark. It is a logo, so it never changes size.
struct BrandLockup: View {
    var markSize: CGFloat = 22
    var textSize: CGFloat = 17

    var body: some View {
        HStack(spacing: markSize >= 22 ? 9 : 8) {
            OrbitMark(size: markSize)
            Text("Human Wonder")
                .font(.custom("HWGeist-Medium", fixedSize: textSize))
                .tracking(-0.01 * textSize)
                .foregroundStyle(.white)
                .lineLimit(1)
                .fixedSize()
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("Human Wonder")
    }
}

/// A small dot that breathes.
struct PulsingDot: View {
    let color: Color
    var size: CGFloat = 6
    var period: Double = 0.9
    @State private var dim = false

    var body: some View {
        Circle()
            .fill(color)
            .frame(width: size, height: size)
            .opacity(dim ? 0.35 : 1)
            .onAppear {
                withAnimation(.easeInOut(duration: period / 2).repeatForever(autoreverses: true)) { dim = true }
            }
    }
}

/// The one primary action: a 40pt circle in a 48pt target. Teal with a dark arrow when it can go,
/// clear with a grey arrow when it can't, a stop square while Orbit 412 is answering.
struct SendButton: View {
    let generating: Bool
    let canSend: Bool
    var label: String = "Send"
    let action: () -> Void

    var body: some View {
        let background: Color = generating ? .white.opacity(0.14) : (canSend ? HW.primary : .clear)
        let foreground: Color = generating ? HW.ink : (canSend ? HW.onAccent : HW.inkDisabled)
        Button(action: action) {
            ZStack {
                Circle().fill(background).frame(width: 40, height: 40)
                HWIcon(name: generating ? "ic_stop" : "ic_arrow_upward", color: foreground, size: generating ? 18 : 20)
            }
            .frame(width: 48, height: 48)
            .contentShape(Circle())
        }
        .buttonStyle(PressScale(scale: 0.92))
        .allowsHitTesting(generating || canSend)
        .animation(.easeOut(duration: 0.2), value: canSend)
        .animation(.easeOut(duration: 0.2), value: generating)
        .accessibilityLabel(generating ? "Stop answer" : label)
    }
}

/// The primary action as a wide pill: solid teal, the send button's accent.
struct PrimaryButton: View {
    let title: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(title)
                .hw(HWType.buttonLabel)
                .foregroundStyle(HW.onAccent)
                .frame(maxWidth: .infinity, minHeight: 56)
                .background(Capsule().fill(HW.primary))
                .contentShape(Capsule())
        }
        .buttonStyle(PressScale(scale: 0.98))
    }
}

/// First letter of the name, upper case ("W" for Wanderer when there is no name yet).
func initialOf(_ name: String) -> String {
    let trimmed = name.trimmingCharacters(in: .whitespacesAndNewlines)
    guard let first = trimmed.first else { return "W" }
    return String(first).uppercased()
}

/// The person's initial on a quiet solid circle (white 12% edge).
struct InitialAvatar: View {
    let name: String
    var size: CGFloat = 36
    var fontSize: CGFloat = 15

    var body: some View {
        Text(initialOf(name))
            .font(.custom("HWGeist-Medium", size: fontSize, relativeTo: .body))
            .foregroundStyle(HW.ink)
            .frame(width: size, height: size)
            .background(Circle().fill(HW.avatarFill))
            .overlay(Circle().strokeBorder(.white.opacity(0.12), lineWidth: 1))
            .accessibilityHidden(true)
    }
}

/// Section label above a group.
struct SectionLabel: View {
    let text: String
    var body: some View {
        Text(text).hw(HWType.chatLabel).foregroundStyle(HW.inkTertiary)
    }
}

/// A 1pt white 7% divider inside glass groups.
struct GlassDivider: View {
    var inset: CGFloat = 16
    var body: some View {
        Rectangle().fill(HW.divider).frame(height: 1).padding(.horizontal, inset)
    }
}

/// A headline and its sub-line that never spill over the rest of the screen: when the space runs
/// out (keyboard open, short phone, big text) the sub-line goes first, then the headline.
struct FitStack<Headline: View, Sub: View>: View {
    var spacing: CGFloat
    var centered: Bool = false
    @ViewBuilder var headline: Headline
    @ViewBuilder var sub: Sub

    var body: some View {
        ViewThatFits(in: .vertical) {
            VStack(spacing: spacing) {
                headline
                sub
            }
            headline
            Color.clear.frame(height: 0)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: centered ? .center : .top)
    }
}

/// A headline and line of copy that rise into place once, when the screen opens.
struct Rising: ViewModifier {
    @State private var shown = false
    func body(content: Content) -> some View {
        content
            .opacity(shown ? 1 : 0)
            .offset(y: shown ? 0 : 24)
            .onAppear { withAnimation(.easeOut(duration: 1.2)) { shown = true } }
    }
}

extension View {
    func rising() -> some View { modifier(Rising()) }
}

// MARK: - Weighted column

private struct WeightKey: LayoutValueKey {
    static let defaultValue: CGFloat = 0
}

extension View {
    /// In a [WeightedColumn]: take this share of the space the other children leave.
    func weight(_ value: CGFloat) -> some View { layoutValue(key: WeightKey.self, value: value) }
}

/// A full-width column whose weighted children share the space left by the others (Compose's
/// Modifier.weight). When nothing is left (keyboard open), weighted children get no height.
struct WeightedColumn: Layout {
    var spacing: CGFloat = 0

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let width = proposal.width ?? subviews.map { $0.sizeThatFits(.unspecified).width }.max() ?? 0
        if let height = proposal.height { return CGSize(width: width, height: height) }
        let fixed = subviews.filter { $0[WeightKey.self] == 0 }
            .map { $0.sizeThatFits(ProposedViewSize(width: width, height: nil)).height }
            .reduce(0, +)
        return CGSize(width: width, height: fixed + spacing * CGFloat(max(0, subviews.count - 1)))
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        let width = bounds.width
        let weights = subviews.map { $0[WeightKey.self] }
        let heights = subviews.enumerated().map { index, view in
            weights[index] > 0 ? 0 : view.sizeThatFits(ProposedViewSize(width: width, height: nil)).height
        }
        let totalWeight = weights.reduce(0, +)
        let used = heights.reduce(0, +) + spacing * CGFloat(max(0, subviews.count - 1))
        let free = max(0, bounds.height - used)
        var y = bounds.minY
        for (index, view) in subviews.enumerated() {
            let h = weights[index] > 0 && totalWeight > 0 ? free * weights[index] / totalWeight : heights[index]
            view.place(at: CGPoint(x: bounds.minX, y: y), anchor: .topLeading, proposal: ProposedViewSize(width: width, height: h))
            y += h + spacing
        }
    }
}

// MARK: - Flow layout (tone chips)

struct FlowLayout: Layout {
    var spacing: CGFloat = 8

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let maxWidth = proposal.width ?? .infinity
        var x: CGFloat = 0, y: CGFloat = 0, rowHeight: CGFloat = 0, widest: CGFloat = 0
        for view in subviews {
            let s = view.sizeThatFits(.unspecified)
            if x > 0 && x + s.width > maxWidth {
                y += rowHeight + spacing
                x = 0
                rowHeight = 0
            }
            x += s.width + spacing
            rowHeight = max(rowHeight, s.height)
            widest = max(widest, x - spacing)
        }
        return CGSize(width: proposal.width ?? widest, height: y + rowHeight)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        var x = bounds.minX, y = bounds.minY, rowHeight: CGFloat = 0
        for view in subviews {
            let s = view.sizeThatFits(.unspecified)
            if x > bounds.minX && x + s.width > bounds.maxX {
                y += rowHeight + spacing
                x = bounds.minX
                rowHeight = 0
            }
            view.place(at: CGPoint(x: x, y: y), proposal: ProposedViewSize(s))
            x += s.width + spacing
            rowHeight = max(rowHeight, s.height)
        }
    }
}

// MARK: - Edge swipe back

extension View {
    /// Swiping in from the left edge goes back, as everywhere on iPhone.
    func edgeSwipeBack(_ action: @escaping () -> Void) -> some View {
        overlay(alignment: .leading) {
            Color.clear
                .frame(width: 20)
                .contentShape(Rectangle())
                .gesture(DragGesture(minimumDistance: 10).onEnded { value in
                    if value.translation.width > 60 { action() }
                })
                .ignoresSafeArea()
        }
    }
}
