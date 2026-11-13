import SwiftUI

/// Name: black sky over the Earth at night seen from orbit, then the chat's own input bar and send
/// arrow ("Meet Orbit 412"), with Orbit 412 gliding along the horizon. The photo starts 17% down.
struct NameView: View {
    @Environment(AppModel.self) private var app
    @State private var name = ""
    @FocusState private var focused: Bool

    private var canMeet: Bool { !name.trimmingCharacters(in: .whitespaces).isEmpty }

    var body: some View {
        ZStack {
            GeometryReader { geo in
                let h = geo.size.height
                ZStack(alignment: .top) {
                    HW.space
                    Image(uiImage: HWImages.photo("bg_earth_orbit"))
                        .resizable()
                        .scaledToFill()
                        .frame(width: geo.size.width, height: h * 0.83, alignment: .top)
                        .clipped()
                        .offset(y: h * 0.17)
                    LinearGradient(
                        stops: [
                            .init(color: .black, location: 0),
                            .init(color: .black, location: 0.17),
                            .init(color: .clear, location: 0.33),
                            .init(color: .clear, location: 0.58),
                            .init(color: .black.opacity(0.78), location: 0.76),
                            .init(color: .black, location: 0.92),
                            .init(color: .black, location: 1),
                        ],
                        startPoint: .top, endPoint: .bottom)
                    SatelliteArc(placement: SatelliteArc.nameScreen, startAt: 0.22, labelAbove: true)
                }
            }
            .ignoresSafeArea()
            .accessibilityHidden(true)

            WeightedColumn {
                HStack(spacing: 0) {
                    IconCircle(icon: "ic_arrow_back", label: "Back", iconSize: 22) { app.screen = .landing }
                    BrandLockup().frame(maxWidth: .infinity)
                    Color.clear.frame(width: 48, height: 48)
                }
                .padding(.horizontal, 4)
                .frame(height: 64)

                // With the keyboard open the question keeps what room is left (losing its
                // sub-line first) and the name bar below stays whole, above the keyboard.
                FitStack(spacing: 12) {
                    Text("What should Orbit\u{00A0}412 call you?")
                        .hw(HWType.nameTitle)
                        .foregroundStyle(.white)
                        .multilineTextAlignment(.center)
                        .fixedSize(horizontal: false, vertical: true)
                } sub: {
                    Text("It will greet you by name every time it passes over.")
                        .hw(HWType.chatSub)
                        .foregroundStyle(HW.inkSecondary)
                        .multilineTextAlignment(.center)
                        .frame(maxWidth: 300)
                        .fixedSize(horizontal: false, vertical: true)
                }
                .padding(.horizontal, 32)
                .padding(.top, 28)
                .rising()
                .weight(1)

                VStack(spacing: 14) {
                    HStack(spacing: 0) {
                        ZStack(alignment: .leading) {
                            if name.isEmpty {
                                Text("Your name or callsign").hw(HWType.chatInput).foregroundStyle(HW.inkTertiary)
                            }
                            TextField("", text: $name)
                                .hw(HWType.chatInput)
                                .foregroundStyle(.white)
                                .tint(HW.primary)
                                .textInputAutocapitalization(.words)
                                .autocorrectionDisabled()
                                .submitLabel(.go)
                                .focused($focused)
                                .onSubmit(submit)
                                .onChange(of: name) { _, value in if value.count > 40 { name = String(value.prefix(40)) } }
                                .accessibilityLabel("Your name")
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        SendButton(generating: false, canSend: canMeet, label: "Meet Orbit 412", action: submit)
                    }
                    .padding(.leading, 20)
                    .padding(.trailing, 8)
                    .padding(.vertical, 8)
                    .glass(radius: 28, border: focused ? HW.primary.opacity(0.45) : HW.glassBorder)
                    .animation(.easeOut(duration: 0.2), value: focused)

                    HStack(spacing: 6) {
                        HWIcon(name: "ic_lock", color: HW.inkTertiary, size: 14)
                        Text("Your name stays on this phone.").hw(HWType.subline).foregroundStyle(HW.inkTertiary)
                    }
                }
                .padding(.horizontal, 20)
                .padding(.bottom, 24)
            }
        }
        .edgeSwipeBack { app.screen = .landing }
        .onAppear {
            if Demo.isOn, Demo.wantsKeyboard { name = "Vivek" }
            if Demo.wantsKeyboard { DispatchQueue.main.asyncAfter(deadline: .now() + 0.6) { focused = true } }
        }
    }

    private func submit() {
        guard canMeet else { return }
        focused = false
        app.completeOnboarding(name)
    }
}
