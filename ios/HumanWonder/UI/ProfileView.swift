import SwiftUI
import UniformTypeIdentifiers

private let segmentTrack = Color(argb: 0x12FFFFFF) // white 7%
private let segmentOn = Color(argb: 0x24FFFFFF) // white 14%
private let chipFill = Color(argb: 0x14FFFFFF) // white 8%

var appVersion: String { Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "2.0" }

/// Profile & settings. Everything is kept on the phone and saved as it changes; a new name reaches
/// Orbit 412 with the next question.
struct ProfileView: View {
    @Environment(AppModel.self) private var app
    @State private var name = ""
    @State private var email = ""
    @State private var city = ""
    @State private var confirmClear = false
    @State private var showAbout = false
    @State private var exporting = false
    @State private var toast: String?
    @FocusState private var focus: Field?

    private enum Field { case name, email, city }

    var body: some View {
        let displayName = name.trimmingCharacters(in: .whitespaces).isEmpty ? "Wanderer" : name
        let emailInvalid = !email.trimmingCharacters(in: .whitespaces).isEmpty && !AppModel.isValidEmail(email.trimmingCharacters(in: .whitespaces))
        ZStack {
            HW.space.ignoresSafeArea()
            // Top glow: an ellipse 90% wide and 36% tall at the top, violet fading to teal.
            GeometryReader { geo in
                Canvas { ctx, size in
                    let rx = size.width * 0.9, ry = size.height * 0.36
                    ctx.scaleBy(x: 1, y: ry / rx)
                    let c = CGPoint(x: size.width / 2, y: 0)
                    ctx.fill(Path(ellipseIn: CGRect(x: c.x - rx, y: -rx, width: 2 * rx, height: 2 * rx)),
                             with: .radialGradient(Gradient(stops: [
                                .init(color: Color(argb: 0x477C5CFF), location: 0),
                                .init(color: Color(argb: 0x142FE6C8), location: 0.55),
                                .init(color: .clear, location: 1),
                             ]), center: c, startRadius: 0, endRadius: rx))
                }
                .frame(width: geo.size.width, height: geo.size.height)
            }
            .ignoresSafeArea()
            .allowsHitTesting(false)

            VStack(spacing: 0) {
                HStack(spacing: 0) {
                    IconCircle(icon: "ic_arrow_back", label: "Back", iconSize: 22) { app.screen = .chat }
                    Text("Profile & settings").hw(HWType.hubTitle).foregroundStyle(.white).lineLimit(1).frame(maxWidth: .infinity)
                    Color.clear.frame(width: 48, height: 48)
                }
                .padding(.horizontal, 4)
                .frame(height: 64)

                ScrollView {
                    VStack(alignment: .leading, spacing: 0) {
                        // Avatar: an 84pt disc in a 3pt teal-to-iris ring.
                        VStack(spacing: 0) {
                            Text(initialOf(displayName))
                                .font(HWType.avatarInitial.font)
                                .foregroundStyle(.white)
                                .frame(width: 84, height: 84)
                                .background(Circle().fill(HW.sheet))
                                .padding(3)
                                .background(Circle().fill(LinearGradient(colors: [HW.primary, HW.iris], startPoint: .topLeading, endPoint: .bottomTrailing)))
                            Text(displayName).hw(HWType.profileName).foregroundStyle(.white).lineLimit(2).multilineTextAlignment(.center).padding(.top, 14)
                            Text(app.profile.email.isEmpty ? "Your Human Wonder profile" : app.profile.email)
                                .hw(HWType.chipRegular).foregroundStyle(HW.inkSecondary).lineLimit(1).padding(.top, 2)
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.top, 8)
                        .padding(.bottom, 24)

                        SectionLabel(text: "You").padding(.horizontal, 8).padding(.bottom, 8)
                        VStack(spacing: 0) {
                            field("Name or callsign", text: $name, placeholder: "Your name or callsign", which: .name)
                                .textInputAutocapitalization(.words)
                            GlassDivider()
                            field("Email", text: $email, placeholder: "you@example.com", which: .email,
                                  helper: emailInvalid ? "That doesn’t look like an email address yet." : "Optional. Only kept on this phone.",
                                  helperIsError: emailInvalid)
                                .keyboardType(.emailAddress)
                                .textInputAutocapitalization(.never)
                            GlassDivider()
                            field("Where you look up from", text: $city, placeholder: "City or place, optional", which: .city)
                                .textInputAutocapitalization(.words)
                        }
                        .glass()

                        SectionLabel(text: "Orbit 412").padding(.horizontal, 8).padding(.top, 24).padding(.bottom, 8)
                        VStack(spacing: 0) {
                            VStack(alignment: .leading, spacing: 0) {
                                Text("Point of view").hw(HWType.rowTitle).foregroundStyle(HW.ink)
                                Text("How Orbit 412 describes things").hw(HWType.subline).foregroundStyle(HW.inkTertiary)
                                pointOfView.padding(.top, 12)
                            }
                            .padding(.horizontal, 16).padding(.top, 14).padding(.bottom, 16)
                            GlassDivider()
                            VStack(alignment: .leading, spacing: 0) {
                                Text("Tone").hw(HWType.rowTitle).foregroundStyle(HW.ink)
                                Text("How Orbit 412 sounds").hw(HWType.subline).foregroundStyle(HW.inkTertiary)
                                toneChips.padding(.top, 12)
                            }
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .padding(.horizontal, 16).padding(.top, 14).padding(.bottom, 16)
                            GlassDivider()
                            switchRow("Read answers aloud", "Orbit 412 speaks each answer", app.profile.readAloud) { on in app.updateProfile { $0.readAloud = on } }
                            GlassDivider()
                            switchRow("Haptics", "A soft tap when an answer lands", app.profile.haptics) { on in app.updateProfile { $0.haptics = on } }
                        }
                        .glass()

                        SectionLabel(text: "Your transmissions").padding(.horizontal, 8).padding(.top, 24).padding(.bottom, 8)
                        VStack(spacing: 0) {
                            navRow("ic_upload", "Export transmissions", "Save all conversations as a file") { exporting = true }
                            Rectangle().fill(HW.divider).frame(height: 1)
                            navRow("ic_delete", "Clear all transmissions", "This can’t be undone", color: HW.error) { confirmClear = true }
                        }
                        .glass()

                        SectionLabel(text: "About").padding(.horizontal, 8).padding(.top, 24).padding(.bottom, 8)
                        VStack(spacing: 0) {
                            navRow("ic_info", "About Human Wonder", "Version \(appVersion)") { showAbout = true }
                            Rectangle().fill(HW.divider).frame(height: 1)
                            navRow("ic_info", "Open-source licenses", nil) { app.screen = .licenses }
                        }
                        .glass()

                        Text("Human Wonder \(appVersion)")
                            .hw(HWType.chatMeta).foregroundStyle(HW.inkTertiary)
                            .frame(maxWidth: .infinity)
                            .padding(.top, 28)
                    }
                    .padding(.horizontal, 16)
                    .padding(.top, 8)
                    .padding(.bottom, 32)
                }
                .scrollDismissesKeyboard(.interactively)
            }

            if let toast {
                VStack {
                    Spacer()
                    Text(toast)
                        .hw(HWType.menuLabel).foregroundStyle(HW.ink)
                        .padding(.horizontal, 18).padding(.vertical, 12)
                        .glass(radius: 22)
                        .padding(.bottom, 32)
                }
                .transition(.opacity)
            }
        }
        .edgeSwipeBack { app.screen = .chat }
        .onAppear {
            name = app.profile.name
            email = app.profile.email
            city = app.profile.city
        }
        .onChange(of: name) { _, value in
            let v = String(value.prefix(40))
            if v != value { name = v }
            app.updateProfile { $0.name = v }
        }
        .onChange(of: email) { _, value in
            let v = String(value.prefix(80))
            if v != value { email = v }
            let trimmed = v.trimmingCharacters(in: .whitespaces)
            if trimmed.isEmpty || AppModel.isValidEmail(trimmed) { app.updateProfile { $0.email = trimmed } }
        }
        .onChange(of: city) { _, value in
            let v = String(value.prefix(60))
            if v != value { city = v }
            app.updateProfile { $0.city = v }
        }
        .alert("Clear all transmissions?", isPresented: $confirmClear) {
            Button("Clear all", role: .destructive) {
                app.clearAll()
                show("All transmissions cleared.")
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("Every conversation with Orbit 412 will be deleted from this phone. This can’t be undone.")
        }
        .alert("Human Wonder", isPresented: $showAbout) {
            Button("Close", role: .cancel) {}
        } message: {
            Text("Version \(appVersion). Human Wonder gives the sky a voice: Orbit 412, a satellite passing over you tonight. Your profile and conversations stay on this phone.")
        }
        .fileExporter(isPresented: $exporting, document: TransmissionsFile(data: app.exportData()), contentType: .json,
                      defaultFilename: "human-wonder-transmissions") { result in
            if case .success = result { show("Transmissions saved.") } else { show("Couldn't save your transmissions. Try again.") }
        }
    }

    private func show(_ text: String) {
        withAnimation { toast = text }
        DispatchQueue.main.asyncAfter(deadline: .now() + 2.2) { withAnimation { toast = nil } }
    }

    /// A labelled field row (at least 64pt): label, input, optional helper.
    private func field(_ label: String, text: Binding<String>, placeholder: String, which: Field,
                       helper: String? = nil, helperIsError: Bool = false) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label).hw(HWType.subline).foregroundStyle(HW.inkSecondary)
            ZStack(alignment: .leading) {
                if text.wrappedValue.isEmpty {
                    Text(placeholder).hw(HWType.chatInput).foregroundStyle(HW.placeholder).lineLimit(1)
                }
                TextField("", text: text)
                    .hw(HWType.chatInput)
                    .foregroundStyle(.white)
                    .tint(HW.primary)
                    .autocorrectionDisabled()
                    .submitLabel(.done)
                    .focused($focus, equals: which)
                    .accessibilityLabel(label)
            }
            if let helper {
                Text(helper).hw(HWType.chatMeta).foregroundStyle(helperIsError ? HW.error : HW.inkTertiary)
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .frame(maxWidth: .infinity, minHeight: 64, alignment: .leading)
        .contentShape(Rectangle())
        .onTapGesture { focus = which }
    }

    /// Orbit / Ground: two 44pt segments on a white 7% track; the active one is white 14%.
    private var pointOfView: some View {
        let selected = Perspective.from(app.profile.perspective)
        return HStack(spacing: 0) {
            ForEach(Perspective.allCases, id: \.self) { p in
                let on = p == selected
                let color: Color = on ? .white : HW.inkSecondary
                Button { withAnimation(.easeOut(duration: 0.2)) { app.updateProfile { $0.perspective = p.rawValue } } } label: {
                    HStack(spacing: 8) {
                        HWIcon(name: p.icon, color: on && p == .orbit ? HW.primary : color, size: 18)
                        Text(p.label).hw(HWType.segmentLabel).foregroundStyle(color).lineLimit(1)
                    }
                    .padding(.horizontal, 8)
                    .frame(maxWidth: .infinity, minHeight: 44)
                    .background(Capsule().fill(on ? segmentOn : .clear))
                    .contentShape(Capsule())
                }
                .buttonStyle(PressScale(scale: 0.98))
                .accessibilityAddTraits(on ? .isSelected : [])
            }
        }
        .padding(3)
        .background(Capsule().fill(segmentTrack))
    }

    /// Four wrapping 40pt chips: white 8%, the active one white 14% with a teal sparkle.
    private var toneChips: some View {
        let selected = Tone.from(app.profile.tone)
        return FlowLayout(spacing: 8) {
            ForEach(Tone.allCases, id: \.self) { t in
                let on = t == selected
                let color: Color = on ? .white : HW.inkSecondary
                Button { app.updateProfile { $0.tone = t.rawValue } } label: {
                    HStack(spacing: 6) {
                        HWIcon(name: "ic_sparkle", color: on ? HW.primary : color, size: 14)
                        Text(t.label).hw(HWType.chipStrong).foregroundStyle(color).lineLimit(1).fixedSize()
                    }
                    .padding(.horizontal, 14)
                    .frame(minHeight: 40)
                    .background(Capsule().fill(on ? segmentOn : chipFill))
                    .contentShape(Capsule())
                }
                .buttonStyle(PressScale())
                .accessibilityAddTraits(on ? .isSelected : [])
            }
        }
    }

    private func switchRow(_ label: String, _ sub: String, _ on: Bool, _ change: @escaping (Bool) -> Void) -> some View {
        Toggle(isOn: Binding(get: { on }, set: change)) {
            VStack(alignment: .leading, spacing: 0) {
                Text(label).hw(HWType.rowTitle).foregroundStyle(HW.ink)
                Text(sub).hw(HWType.subline).foregroundStyle(HW.inkTertiary)
            }
        }
        .tint(HW.primary)
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
        .frame(minHeight: 64)
    }

    private func navRow(_ icon: String, _ label: String, _ sub: String?, color: Color = HW.ink, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: 14) {
                HWIcon(name: icon, color: color == HW.ink ? HW.iconMuted : color, size: 22)
                VStack(alignment: .leading, spacing: 0) {
                    Text(label).hw(HWType.rowTitle).foregroundStyle(color)
                    if let sub { Text(sub).hw(HWType.subline).foregroundStyle(HW.inkTertiary) }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                HWIcon(name: "ic_chevron_right", color: HW.inkTertiary, size: 20)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 8)
            .frame(minHeight: 56)
            .contentShape(Rectangle())
        }
        .buttonStyle(PressScale(scale: 0.99))
    }
}

/// The exported transmissions, for the system's save sheet.
struct TransmissionsFile: FileDocument {
    static var readableContentTypes: [UTType] { [.json] }
    var data: Data

    init(data: Data) { self.data = data }
    init(configuration: ReadConfiguration) throws { data = configuration.file.regularFileContents ?? Data() }
    func fileWrapper(configuration: WriteConfiguration) throws -> FileWrapper { FileWrapper(regularFileWithContents: data) }
}

/// "Open-source licenses": every notice bundled with Human Wonder, as written.
struct LicensesView: View {
    @Environment(AppModel.self) private var app

    private static let titles = [
        "NOTICE.txt": "Human Wonder notices",
        "LICENSE-Apache-2.0.txt": "Apache License 2.0",
        "LICENSE-llama.cpp.txt": "MIT License",
        "NOTICE-model-and-icons.txt": "Bundled data and icons",
        "OFL-geist.txt": "Geist typeface (SIL Open Font License)",
        "OFL-jetbrainsmono.txt": "JetBrains Mono typeface (SIL Open Font License)",
        "OFL-newsreader.txt": "Newsreader typeface (SIL Open Font License)",
        "NOTICE-orbit-archive.txt": "Earth photographs (NASA)",
    ]

    private var texts: [(String, String)] {
        let urls = Bundle.main.urls(forResourcesWithExtension: "txt", subdirectory: "licenses") ?? []
        return urls
            .map { ($0.lastPathComponent, (try? String(contentsOf: $0, encoding: .utf8)) ?? "") }
            .sorted { a, b in a.0 == "NOTICE.txt" ? true : (b.0 == "NOTICE.txt" ? false : a.0 < b.0) }
    }

    var body: some View {
        ZStack {
            HW.space.ignoresSafeArea()
            VStack(spacing: 0) {
                HStack(spacing: 0) {
                    IconCircle(icon: "ic_arrow_back", label: "Back", iconSize: 22) { app.screen = .profile }
                    Text("Open-source licenses").hw(HWType.hubTitle).foregroundStyle(.white).lineLimit(1).frame(maxWidth: .infinity)
                    Color.clear.frame(width: 48, height: 48)
                }
                .padding(.horizontal, 4)
                .frame(height: 64)
                ScrollView {
                    LazyVStack(alignment: .leading, spacing: 12) {
                        ForEach(texts, id: \.0) { file, text in
                            VStack(alignment: .leading, spacing: 8) {
                                Text(Self.titles[file] ?? file).hw(HWType.hubTitle).foregroundStyle(.white)
                                Text(text).hw(HWType.mono).foregroundStyle(HW.inkSecondary).textSelection(.enabled)
                            }
                            .padding(16)
                            .frame(maxWidth: .infinity, alignment: .leading)
                            .glass()
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.bottom, 32)
                }
            }
        }
        .edgeSwipeBack { app.screen = .profile }
    }
}
