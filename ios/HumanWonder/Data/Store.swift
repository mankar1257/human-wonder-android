import Foundation

/// The person's Human Wonder profile and preferences. Everything stays on the phone.
struct Profile: Codable, Equatable {
    var name = ""
    var email = ""
    var city = ""
    /// Default point of view for new chats (a Perspective name).
    var perspective = "Orbit"
    /// Default tone for new chats (a Tone name).
    var tone = "Balanced"
    var readAloud = false
    var haptics = true
    var onboardingComplete = false
}

struct StoredMessage: Codable, Identifiable, Equatable {
    let id: String
    /// "user" or "assistant".
    let role: String
    let content: String
    let timestamp: Date
}

struct Conversation: Codable, Identifiable, Equatable {
    let id: String
    var title: String
    let createdAt: Date
    var updatedAt: Date
    var messages: [StoredMessage]
}

/// Profile and conversations, saved as two small files in the app's own storage (protected by the
/// phone's passcode). Nothing leaves the phone.
final class Store {
    private let folder: URL?

    /// [inMemory] for the screen checks: nothing is read or written.
    init(inMemory: Bool = false) {
        if inMemory {
            folder = nil
            return
        }
        let base = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
            .appendingPathComponent("HumanWonder", isDirectory: true)
        try? FileManager.default.createDirectory(at: base, withIntermediateDirectories: true)
        var values = URLResourceValues()
        values.isExcludedFromBackup = true
        var url = base
        try? url.setResourceValues(values)
        folder = base
    }

    private func url(_ name: String) -> URL? { folder?.appendingPathComponent(name) }

    private func read<T: Decodable>(_ type: T.Type, _ name: String) -> T? {
        guard let url = url(name), let data = try? Data(contentsOf: url) else { return nil }
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .millisecondsSince1970
        return try? decoder.decode(type, from: data)
    }

    private func write<T: Encodable>(_ value: T, _ name: String) {
        guard let url = url(name) else { return }
        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .millisecondsSince1970
        guard let data = try? encoder.encode(value) else { return }
        try? data.write(to: url, options: [.atomic, .completeFileProtectionUntilFirstUserAuthentication])
    }

    func loadProfile() -> Profile { read(Profile.self, "profile.json") ?? Profile() }
    func save(_ profile: Profile) { write(profile, "profile.json") }

    func loadConversations() -> [Conversation] { read([Conversation].self, "transmissions.json") ?? [] }
    func save(_ conversations: [Conversation]) { write(conversations, "transmissions.json") }

    /// Every conversation as readable JSON (Profile ▸ Export transmissions).
    static func exportJSON(_ conversations: [Conversation]) -> Data {
        let formatter = ISO8601DateFormatter()
        let list: [[String: Any]] = conversations.map { c in
            [
                "title": c.title,
                "createdAt": formatter.string(from: c.createdAt),
                "updatedAt": formatter.string(from: c.updatedAt),
                "messages": c.messages.map { m in
                    ["role": m.role == "user" ? "you" : "Orbit 412", "content": m.content, "time": formatter.string(from: m.timestamp)]
                },
            ]
        }
        let root: [String: Any] = [
            "app": "Human Wonder",
            "exportedAt": formatter.string(from: Date()),
            "transmissions": list,
        ]
        return (try? JSONSerialization.data(withJSONObject: root, options: [.prettyPrinted, .sortedKeys])) ?? Data()
    }
}
