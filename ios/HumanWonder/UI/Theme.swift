import SwiftUI

// Human Wonder's look on iPhone: the same tokens as the Android app (ui/Theme.kt) and the preview.

extension Color {
    /// 0xAARRGGBB, as in the Android tokens.
    init(argb: UInt32) {
        self.init(
            .sRGB,
            red: Double((argb >> 16) & 0xFF) / 255,
            green: Double((argb >> 8) & 0xFF) / 255,
            blue: Double(argb & 0xFF) / 255,
            opacity: Double((argb >> 24) & 0xFF) / 255
        )
    }
}

enum HW {
    static let space = Color(argb: 0xFF000000)
    static let ink = Color(argb: 0xFFECEFF0)
    static let inkAnswer = Color(argb: 0xFFE8ECEC)
    static let inkSecondary = Color(argb: 0xFFA7B2B0)
    static let inkTertiary = Color(argb: 0xFF8D9997)
    static let inkDisabled = Color(argb: 0xFF6F7B79)
    static let iconMuted = Color(argb: 0xFFC5CDCB)
    static let bubble = Color(argb: 0xFF1D2225)
    static let chip = Color(argb: 0xFFD5DCDB)
    static let onAccent = Color(argb: 0xFF00201A)
    static let primary = Color(argb: 0xFF83FFE5)
    static let primaryContainer = Color(argb: 0xFF2FE6C8)
    static let gold = Color(argb: 0xFFEDBF70)
    static let error = Color(argb: 0xFFFFB4AB)
    /// White 12%: the edge of every glass surface.
    static let glassBorder = Color(argb: 0x1FFFFFFF)
    /// White 7%: dividers inside glass groups.
    static let divider = Color(argb: 0x12FFFFFF)
    /// The side menu sheet and dialogs.
    static let sheet = Color(argb: 0xFF0B0D12)
    static let iris = Color(argb: 0xFF7C5CFF)
    static let avatarFill = Color(argb: 0xFF1F2629)
    static let placeholder = Color(argb: 0x73BACAC4)

    /// The one surface of Human Wonder: near-black at the top-left lifting to slate violet. Every
    /// card, group, input bar and the side menu use it, so the app reads as one.
    static let glass = LinearGradient(
        stops: [
            .init(color: Color(argb: 0xEB0E0F14), location: 0),
            .init(color: Color(argb: 0xE01E2030), location: 0.55),
            .init(color: Color(argb: 0x9E464A80), location: 1),
        ],
        startPoint: .topLeading,
        endPoint: .bottomTrailing
    )
}

/// One step of the type scale: typeface, size and line height in points, letter spacing in em.
struct HWStyle {
    let face: String
    let size: CGFloat
    let lineHeight: CGFloat
    var tracking: CGFloat = 0

    init(_ face: String, _ size: CGFloat, _ lineHeight: CGFloat, _ tracking: CGFloat = 0) {
        self.face = face
        self.size = size
        self.lineHeight = lineHeight
        self.tracking = tracking
    }

    /// Follows the phone's text size (the app caps it, see RootView).
    var font: Font { .custom(face, size: size, relativeTo: .body) }
    /// Never changes size (the logo).
    var fixedFont: Font { .custom(face, fixedSize: size) }
    var uiFont: UIFont { UIFont(name: face, size: size) ?? .systemFont(ofSize: size) }
}

enum HWType {
    static let chatHero = HWStyle("HWGeist-Light", 34, 40, -0.02)
    static let welcomeHero = HWStyle("HWGeist-Light", 34, 40, -0.02)
    static let nameTitle = HWStyle("HWGeist-Light", 28, 34, -0.02)
    static let chatSub = HWStyle("HWGeist-Regular", 14, 21)
    static let chatTitle = HWStyle("HWGeist-Medium", 15, 21)
    static let chatBody = HWStyle("HWGeist-Regular", 15, 24)
    static let chatInput = HWStyle("HWGeist-Regular", 15, 22)
    static let chatLabel = HWStyle("HWGeist-Medium", 12, 17)
    static let chatLabelRegular = HWStyle("HWGeist-Regular", 12, 17)
    static let chatMeta = HWStyle("HWGeist-Regular", 12, 16)
    static let buttonLabel = HWStyle("HWGeist-Medium", 15, 22)
    static let hubTitle = HWStyle("HWGeist-Medium", 16, 22)
    static let rowTitle = HWStyle("HWGeist-Regular", 15, 21)
    static let menuLabel = HWStyle("HWGeist-Regular", 14, 20)
    static let menuLabelStrong = HWStyle("HWGeist-Medium", 14, 20)
    static let segmentLabel = HWStyle("HWGeist-Medium", 14, 20)
    static let subline = HWStyle("HWGeist-Regular", 12, 17)
    static let chipStrong = HWStyle("HWGeist-Medium", 13, 18)
    static let chipRegular = HWStyle("HWGeist-Regular", 13, 18)
    static let profileName = HWStyle("HWGeist-Regular", 20, 26)
    static let avatarInitial = HWStyle("HWGeist-Light", 32, 32)
    static let headlineMobile = HWStyle("HWNewsreader-Regular", 28, 36, -0.01)
    static let ascent = HWStyle("HWGeist-Medium", 14, 20, 0.01)
    static let satelliteLabel = HWStyle("HWGeist-Medium", 11, 15)
    static let mono = HWStyle("HWMono-Regular", 12, 18)
}

extension View {
    /// Typeface, line height and letter spacing of [style].
    func hw(_ style: HWStyle) -> some View {
        font(style.font)
            .lineSpacing(max(0, style.lineHeight - style.size * 1.22))
            .tracking(style.tracking * style.size)
    }

    /// The shared glass surface with a hairline edge; radius 24 for cards, 28 for input bars.
    func glass(radius: CGFloat = 24, border: Color = HW.glassBorder) -> some View {
        let shape = RoundedRectangle(cornerRadius: radius, style: .continuous)
        return background(shape.fill(HW.glass))
            .overlay(shape.strokeBorder(border, lineWidth: 1))
            .clipShape(shape)
    }
}
