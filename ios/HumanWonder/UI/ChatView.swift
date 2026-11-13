import SwiftUI
import UIKit

/// The three suggestions from the Stitch "Talk to Space" screen.
private let suggestions = [
    "What are you seeing over Liwa dunes right now?",
    "How do city lights look from 400km up?",
    "Can you spot our campfire in the desert?",
]

/// What Orbit 412 says while your question travels up (one per answer, in turn).
private let listening = [
    "Receiving you across the dark…",
    "Your words are climbing 412 km…",
    "Turning the antenna your way…",
    "Listening across the dark…",
]

/// The conversation with Orbit 412: true black, an aurora that rises around the composer when the
/// sky is empty and sinks low once you talk, plain off-white answers that fade in as they arrive.
struct ChatView: View {
    @Environment(AppModel.self) private var app
    @State private var voice = VoiceInput()
    @FocusState private var inputFocused: Bool

    var body: some View {
        let chat = app.chat!
        let empty = chat.isEmpty
        GeometryReader { geo in
            let top = geo.safeAreaInsets.top
            ZStack {
                // The sky, edge to edge (it does not move with the keyboard).
                ZStack {
                    AuroraBackground(calm: empty ? 0 : 1, energy: chat.generating ? 1 : 0, pulseKey: chat.uplinks)
                    DotGrid()
                    SatelliteArc(placement: SatelliteArc.belowTop(top + 96), visibility: empty ? 1 : 0)
                        .animation(.easeInOut(duration: 1.4), value: empty)
                    VStack {
                        Spacer()
                        LinearGradient(colors: [.clear, .black.opacity(0.45)], startPoint: .top, endPoint: .bottom)
                            .frame(height: 200)
                            .padding(.bottom, 120)
                            .opacity(empty ? 0 : 1)
                            .animation(.easeInOut(duration: 1.4), value: empty)
                    }
                }
                .ignoresSafeArea()

                // The safe area (everything above the keyboard when it is open) decides what fits:
                // the tall empty-sky composer shrinks first, then the suggestions and the note
                // step aside, and the headline gives up its sub-line. The input bar stays whole.
                let roomy = geo.size.height >= 600
                let tight = geo.size.height < 440
                WeightedColumn {
                    ChatHeader(chat: chat, sky: app.sky, onMenu: openMenu, onNew: { chat.newConversation() })
                    if let message = app.sky.failureMessage {
                        LostSignal(message: message, retry: { app.wake() }).weight(1)
                    } else {
                        ZStack {
                            if empty {
                                Hero(name: app.profile.name).transition(.opacity)
                            } else {
                                ConversationList(chat: chat).transition(.opacity)
                            }
                        }
                        .animation(.easeInOut(duration: 0.4), value: empty)
                        .weight(1)
                        Composer(chat: chat, sky: app.sky, voice: voice, large: empty && roomy, focused: $inputFocused)
                        if empty && !tight {
                            Suggestions(enabled: app.sky == .ready) { chat.send($0) }
                            Color.clear.weight(0.45)
                        }
                    }
                    if !tight {
                        Text("Human Wonder’s Orbit 412 sees far, but it can still be wrong.")
                            .hw(HWType.chatMeta)
                            .foregroundStyle(HW.inkTertiary)
                            .multilineTextAlignment(.center)
                            .frame(maxWidth: .infinity)
                            .padding(.horizontal, 24)
                            .padding(.top, 10)
                            .padding(.bottom, 14)
                    } else {
                        Color.clear.frame(height: 10)
                    }
                }

                SideMenu()
            }
        }
        .alert("Voice isn't available", isPresented: $voice.unavailable) {
            Button("OK", role: .cancel) {}
        } message: {
            Text("Allow the microphone and speech recognition for Human Wonder in Settings. Your voice is only turned into text on this phone.")
        }
        .onAppear {
            if Demo.wantsKeyboard { DispatchQueue.main.asyncAfter(deadline: .now() + 0.6) { inputFocused = true } }
        }
        .onDisappear { voice.stop() }
    }

    private func openMenu() {
        inputFocused = false
        voice.stop()
        withAnimation(.easeOut(duration: 0.28)) { app.menuOpen = true }
    }
}

// MARK: - Header

private struct ChatHeader: View {
    let chat: ChatModel
    let sky: Sky
    let onMenu: () -> Void
    let onNew: () -> Void

    var body: some View {
        let last = chat.lines.last
        let (status, busy, color): (String, Bool, Color) = {
            switch sky {
            case .waking: return ("Orbit 412 is finding your signal…", true, HW.gold)
            case .failed: return ("Orbit 412's signal is lost", false, HW.error)
            case .ready:
                if chat.generating && (last == nil || last!.text.isEmpty) { return ("Orbit 412 is receiving you…", true, HW.primary) }
                if chat.generating { return ("Orbit 412 is transmitting…", true, HW.primary) }
                return ("Orbit 412 is overhead now", false, HW.primary)
            }
        }()
        HStack(spacing: 0) {
            IconCircle(icon: "ic_menu", label: "Open menu", action: onMenu)
            VStack(spacing: 0) {
                BrandLockup(markSize: 20, textSize: 16)
                HStack(spacing: 6) {
                    if busy { PulsingDot(color: color).id(status) } else { Circle().fill(color).frame(width: 6, height: 6) }
                    Text(status).hw(HWType.chatMeta).foregroundStyle(HW.inkSecondary).lineLimit(1).truncationMode(.tail)
                }
                .accessibilityElement(children: .combine)
            }
            .frame(maxWidth: .infinity)
            IconCircle(icon: "ic_add", label: "New conversation", iconSize: 22, action: onNew)
        }
        .padding(.horizontal, 4)
        .frame(height: 64)
    }
}

// MARK: - Empty sky

private struct Hero: View {
    let name: String
    var body: some View {
        FitStack(spacing: 12, centered: true) {
            Text("Talk to the\ninfinite sky")
                .hw(HWType.chatHero)
                .foregroundStyle(.white)
                .multilineTextAlignment(.center)
                .fixedSize(horizontal: false, vertical: true)
        } sub: {
            Text("Orbit 412 here, \(name.isEmpty ? "Wanderer" : name), your Human Wonder satellite. I'm passing over you right now. What should I look at?")
                .hw(HWType.chatSub)
                .foregroundStyle(HW.inkSecondary)
                .multilineTextAlignment(.center)
                .frame(maxWidth: 300)
                .fixedSize(horizontal: false, vertical: true)
        }
        .padding(.horizontal, 28)
    }
}

private struct Suggestions: View {
    let enabled: Bool
    let pick: (String) -> Void

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(suggestions, id: \.self) { s in
                    Button { pick(s) } label: {
                        HStack(spacing: 6) {
                            HWIcon(name: "ic_sparkle", color: HW.primary, size: 16)
                            Text(s).hw(HWType.chatLabelRegular).foregroundStyle(HW.chip).lineLimit(1)
                        }
                        .padding(.leading, 12)
                        .padding(.trailing, 14)
                        .frame(maxWidth: 250)
                        .frame(height: 36)
                        .background(Capsule().fill(.black.opacity(0.35)))
                        .overlay(Capsule().strokeBorder(.white.opacity(0.16), lineWidth: 1))
                        .contentShape(Capsule())
                    }
                    .buttonStyle(PressScale())
                    .allowsHitTesting(enabled)
                }
            }
            .padding(.horizontal, 12)
        }
        .padding(.top, 16)
    }
}

// MARK: - Conversation

private struct ConversationList: View {
    let chat: ChatModel

    var body: some View {
        ScrollViewReader { proxy in
            ScrollView {
                LazyVStack(spacing: 24) {
                    ForEach(chat.lines) { line in
                        Group {
                            if line.fromUser { UserBubble(text: line.text) } else { OrbitAnswer(line: line) }
                        }
                        .id(line.id)
                        .transition(.asymmetric(insertion: .opacity.combined(with: .offset(y: 18)), removal: .opacity))
                    }
                    Color.clear.frame(height: 1).id("bottom")
                }
                .padding(.horizontal, 20)
                .padding(.top, 24)
                .padding(.bottom, 20)
            }
            .scrollDismissesKeyboard(.interactively)
            .defaultScrollAnchor(.bottom)
            // Older messages fade out under the header instead of being cut off.
            .mask(LinearGradient(stops: [.init(color: .clear, location: 0), .init(color: .black, location: 0.06), .init(color: .black, location: 1)], startPoint: .top, endPoint: .bottom))
            .onChange(of: chat.lines.count) { _, _ in
                withAnimation(.easeOut(duration: 0.3)) { proxy.scrollTo("bottom", anchor: .bottom) }
            }
            .onChange(of: chat.lines.last?.text) { _, _ in
                proxy.scrollTo("bottom", anchor: .bottom)
            }
        }
    }
}

private struct UserBubble: View {
    let text: String
    var body: some View {
        HStack {
            Spacer(minLength: 48)
            Text(text)
                .hw(HWType.chatInput)
                .foregroundStyle(HW.ink)
                .textSelection(.enabled)
                .padding(.horizontal, 16)
                .padding(.vertical, 12)
                .background(UnevenRoundedRectangle(topLeadingRadius: 22, bottomLeadingRadius: 22, bottomTrailingRadius: 6, topTrailingRadius: 22, style: .continuous).fill(HW.bubble))
                .frame(maxWidth: 290, alignment: .trailing)
        }
    }
}

private struct OrbitAnswer: View {
    let line: ChatModel.Line

    var body: some View {
        let streaming = !line.complete
        VStack(alignment: .leading, spacing: 0) {
            HStack(spacing: 8) {
                OrbitMark(size: 24, active: streaming)
                Text("Orbit 412").hw(HWType.chatLabel).foregroundStyle(HW.inkSecondary)
            }
            .frame(minHeight: 24)
            if streaming && line.text.isEmpty {
                ShimmerText(text: listening[line.id.unicodeScalars.reduce(0) { ($0 + Int($1.value)) % 997 } % listening.count]).padding(.top, 10)
            }
            if !line.text.isEmpty {
                Text(Self.ink(line.text, streaming: streaming))
                    .hw(HWType.chatBody)
                    .textSelection(.enabled)
                    .padding(.top, 10)
                    .fixedSize(horizontal: false, vertical: true)
            }
            if line.complete && !line.text.isEmpty && line.text != "…" {
                CopyAction(text: line.text)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    /// Plain, readable answer text; while it streams, the newest characters fade in.
    static func ink(_ text: String, streaming: Bool) -> AttributedString {
        guard streaming else {
            var s = AttributedString(text)
            s.foregroundColor = HW.inkAnswer
            return s
        }
        let characters = Array(text)
        let tail = min(18, characters.count)
        var result = AttributedString(String(characters.prefix(characters.count - tail)))
        result.foregroundColor = HW.inkAnswer
        for i in 0..<tail {
            var c = AttributedString(String(characters[characters.count - tail + i]))
            let fresh = Double(i + 1) / Double(tail) // newest are the faintest
            c.foregroundColor = HW.inkAnswer.opacity(1 - 0.75 * fresh)
            result += c
        }
        return result
    }
}

/// The "listening" line with a light sweeping across it.
private struct ShimmerText: View {
    let text: String
    @State private var start = Date()

    var body: some View {
        TimelineView(.animation(minimumInterval: 1.0 / 30.0)) { timeline in
            let p = (timeline.date.timeIntervalSince(start) / 1.8).truncatingRemainder(dividingBy: 1)
            let x = -1.3 + 1.6 * p
            Text(text)
                .hw(HWType.chatBody)
                .foregroundStyle(LinearGradient(
                    stops: [
                        .init(color: HW.inkTertiary, location: 0),
                        .init(color: HW.inkTertiary, location: 0.4),
                        .init(color: Color(argb: 0xFFF1F4F4), location: 0.5),
                        .init(color: HW.inkTertiary, location: 0.6),
                        .init(color: HW.inkTertiary, location: 1),
                    ],
                    startPoint: UnitPoint(x: x, y: 0.5), endPoint: UnitPoint(x: x + 2, y: 0.5)))
        }
    }
}

private struct CopyAction: View {
    let text: String
    @State private var copied = false

    var body: some View {
        HStack(spacing: 0) {
            IconCircle(icon: copied ? "ic_check" : "ic_copy", label: copied ? "Copied" : "Copy answer",
                       color: copied ? HW.primary : HW.inkSecondary, size: 40, iconSize: 18) {
                UIPasteboard.general.string = "\(text)\n\n— Orbit 412, Human Wonder"
                copied = true
                DispatchQueue.main.asyncAfter(deadline: .now() + 1.6) { copied = false }
            }
            if copied { Text("Copied").hw(HWType.chatMeta).foregroundStyle(HW.inkSecondary) }
        }
        .padding(.top, 6)
        .offset(x: -10)
    }
}

private struct LostSignal: View {
    let message: String
    let retry: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            OrbitMark(size: 64)
            Text("The signal is lost.")
                .hw(HWType.headlineMobile)
                .foregroundStyle(HW.ink)
                .multilineTextAlignment(.center)
                .padding(.top, 20)
            Text(message)
                .hw(HWType.chatSub)
                .foregroundStyle(HW.inkSecondary)
                .multilineTextAlignment(.center)
                .padding(.top, 8)
            PrimaryButton(title: "Try again", action: retry)
                .frame(maxWidth: 280)
                .padding(.top, 28)
        }
        .padding(32)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

// MARK: - Composer

private struct Composer: View {
    @Bindable var chat: ChatModel
    let sky: Sky
    let voice: VoiceInput
    let large: Bool
    var focused: FocusState<Bool>.Binding

    var body: some View {
        let ready = sky == .ready
        let placeholder = !ready ? "Orbit 412 is rising over the horizon…" : (chat.generating ? "Orbit 412 is transmitting…" : "Ask Orbit 412 anything")
        VStack(alignment: .leading, spacing: 0) {
            ZStack(alignment: .topLeading) {
                if chat.input.isEmpty {
                    Text(voice.listening ? "Listening…" : placeholder).hw(HWType.chatInput).foregroundStyle(HW.inkTertiary)
                }
                TextField("", text: $chat.input, axis: .vertical)
                    .hw(HWType.chatInput)
                    .foregroundStyle(.white)
                    .tint(HW.primary)
                    .lineLimit(1...5)
                    .textInputAutocapitalization(.sentences)
                    .submitLabel(.send)
                    .focused(focused)
                    .accessibilityLabel("Message to Orbit")
                    .onChange(of: chat.input) { _, value in
                        // Return sends (the field grows to five lines for long questions).
                        if value.contains("\n") {
                            let question = value.replacingOccurrences(of: "\n", with: " ")
                            chat.input = question
                            send(question)
                        }
                    }
            }
            .frame(maxWidth: .infinity, minHeight: large ? 96 : 26, alignment: .topLeading)
            .padding(.horizontal, 4)
            .animation(.easeInOut(duration: 0.4), value: large)

            // Mic and send never shrink; if the row is too narrow the tone label goes first,
            // then the Orbit / Ground labels.
            ViewThatFits(in: .horizontal) {
                controls(level: 0)
                controls(level: 1)
                controls(level: 2)
            }
            .padding(.top, 10)
        }
        .padding(.leading, 12)
        .padding(.trailing, 8)
        .padding(.top, 14)
        .padding(.bottom, 8)
        .glass(radius: 28, border: focused.wrappedValue ? HW.primary.opacity(0.45) : .white.opacity(0.12))
        .animation(.easeOut(duration: 0.2), value: focused.wrappedValue)
        .padding(.horizontal, 12)
    }

    private func controls(level: Int) -> some View {
        HStack(spacing: 4) {
            PerspectiveSwitch(selected: chat.perspective, labels: level < 2) { chat.perspective = $0 }
            ToneChip(tone: chat.tone, compact: level >= 1) { chat.cycleTone() }
            Spacer(minLength: 0)
            IconCircle(icon: "ic_mic", label: voice.listening ? "Stop listening" : "Voice input",
                       color: voice.listening ? HW.primary : HW.iconMuted, iconSize: 20) {
                voice.toggle { text in chat.input = text }
            }
            SendButton(generating: chat.generating, canSend: sky == .ready && !chat.input.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty) {
                if chat.generating { chat.stop() } else { send(chat.input) }
            }
        }
    }

    private func send(_ text: String) {
        voice.stop()
        chat.send(text)
    }
}

/// Orbit / Ground: the selected side shows its name, the other only its icon.
private struct PerspectiveSwitch: View {
    let selected: Perspective
    let labels: Bool
    let select: (Perspective) -> Void

    var body: some View {
        HStack(spacing: 0) {
            ForEach(Perspective.allCases, id: \.self) { p in
                let on = p == selected
                Button { withAnimation(.easeOut(duration: 0.2)) { select(p) } } label: {
                    HStack(spacing: 6) {
                        HWIcon(name: p.icon, color: on && p == .orbit ? HW.primary : (on ? .white : HW.inkSecondary), size: 16)
                        if on && labels { Text(p.label).hw(HWType.chatLabel).foregroundStyle(.white).lineLimit(1).fixedSize() }
                    }
                    .padding(.leading, 10)
                    .padding(.trailing, on && labels ? 14 : 10)
                    .frame(height: 36)
                    .background(Capsule().fill(on ? .white.opacity(0.14) : .clear))
                    .contentShape(Capsule())
                }
                .buttonStyle(PressScale())
                .accessibilityLabel("\(p.label) view")
                .accessibilityAddTraits(on ? .isSelected : [])
            }
        }
        .padding(2)
        .background(Capsule().fill(.white.opacity(0.07)))
        .fixedSize()
    }
}

private struct ToneChip: View {
    let tone: Tone
    let compact: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 6) {
                HWIcon(name: "ic_sparkle", color: HW.ink, size: 16)
                if !compact { Text(tone.label).hw(HWType.chatLabel).foregroundStyle(HW.ink).lineLimit(1).fixedSize() }
                HWIcon(name: "ic_expand_more", color: HW.inkSecondary, size: 16)
            }
            .padding(.leading, 12)
            .padding(.trailing, 8)
            .frame(height: 36)
            .background(Capsule().fill(.white.opacity(0.08)))
            .contentShape(Capsule())
        }
        .buttonStyle(PressScale())
        .fixedSize()
        .accessibilityLabel("Tone: \(tone.label)")
    }
}
