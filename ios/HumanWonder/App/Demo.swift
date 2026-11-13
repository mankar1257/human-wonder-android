import Foundation

/// Screen checks: launched with `-hwDemo <scene>` (ios/tools/screens.sh), the app opens straight
/// on one screen with sample content and keeps nothing. Never active in normal use.
enum Demo {
    static var scene: String? { UserDefaults.standard.string(forKey: "hwDemo") }
    static var isOn: Bool { scene != nil }
    /// The scenes that open the keyboard.
    static var wantsKeyboard: Bool { scene?.hasSuffix("keyboard") == true }

    @MainActor
    static func seed(_ app: AppModel) {
        guard let scene else { return }
        let now = Date()
        func at(_ minutesAgo: Double) -> Date { now.addingTimeInterval(-minutesAgo * 60) }
        let profile = Profile(name: "Vivek", city: "Abu Dhabi", onboardingComplete: true)
        let talk = Conversation(
            id: "c1", title: "Liwa dunes tonight", createdAt: at(30), updatedAt: at(2),
            messages: [
                StoredMessage(id: "m1", role: "user", content: "What are you seeing over Liwa dunes right now?", timestamp: at(4)),
                StoredMessage(id: "m2", role: "assistant", content: "The dunes are a quiet sea of shadow tonight, Vivek. Their crests catch a thin silver line of moonlight, and far to the north the lights of Abu Dhabi glow like embers at the edge of the dark.", timestamp: at(4)),
                StoredMessage(id: "m3", role: "user", content: "Can you spot our campfire in the desert?", timestamp: at(2)),
                StoredMessage(id: "m4", role: "assistant", content: "A single warm point, south of the last road. From 412 km up it is smaller than a star, but I see it. Keep it burning and I will look for it on my next pass.", timestamp: at(2)),
            ])
        let older = [
            Conversation(id: "c2", title: "City lights from 400 km", createdAt: at(60 * 26), updatedAt: at(60 * 26), messages: []),
            Conversation(id: "c3", title: "Our campfire in the desert", createdAt: at(60 * 24 * 4), updatedAt: at(60 * 24 * 4), messages: []),
        ]
        switch scene {
        case "landing-first", "name", "name-keyboard":
            app.seed(profile: Profile(), conversations: [])
            app.screen = scene == "landing-first" ? .landing : .name
        case "landing-back":
            app.seed(profile: profile, conversations: [talk] + older)
            app.screen = .landing
        case "chat-empty", "chat-keyboard", "ask":
            app.seed(profile: profile, conversations: [talk] + older)
            app.chat.startSession()
            app.screen = .chat
        case "chat-conversation", "chat-conversation-keyboard", "side-menu":
            app.seed(profile: profile, conversations: [talk] + older)
            app.chat.startSession()
            app.chat.open("c1")
            app.screen = .chat
            app.menuOpen = scene == "side-menu"
        case "profile":
            app.seed(profile: profile, conversations: [talk] + older)
            app.screen = .profile
        case "licenses":
            app.seed(profile: profile, conversations: [])
            app.screen = .licenses
        default:
            break
        }
    }

    /// The "ask" scene: a real question to the real model, as soon as it is ready.
    @MainActor
    static func ask(_ app: AppModel) {
        started = Date()
        app.chat.send("What do you see over the desert tonight?")
    }

    private static var started: Date?

    /// The "ask" scene's answer and how long it took, for the screen check's report.
    static func answered(_ text: String) {
        guard scene == "ask", let started else { return }
        let seconds = Date().timeIntervalSince(started)
        let report = String(format: "seconds: %.1f\n\n%@\n", seconds, text)
        let url = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0].appendingPathComponent("hw-demo-answer.txt")
        try? report.write(to: url, atomically: true, encoding: .utf8)
    }
}
