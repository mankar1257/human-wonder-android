import SwiftUI
import Observation

/// Whether Orbit 412 can be reached yet.
enum Sky: Equatable {
    case waking
    case ready
    case failed(String)

    var failureMessage: String? {
        if case .failed(let message) = self { return message }
        return nil
    }
}

enum Screen: Equatable {
    case landing, name, chat, profile, licenses
}

/// App-wide state: the profile, the sky waking up, the conversations and where the person is.
/// Same flow as the Android app: the welcome screen on every launch; swipe up → Name the first
/// time, otherwise straight into a new chat; back from the chat → the welcome screen.
@MainActor
@Observable
final class AppModel {
    var screen: Screen = .landing
    private(set) var profile: Profile
    private(set) var conversations: [Conversation]
    var sky: Sky = .waking
    var menuOpen = false
    var searchQuery = ""
    var chat: ChatModel! = nil

    let store: Store
    let speaker = Speaker()

    init() {
        let store = Store(inMemory: Demo.isOn)
        self.store = store
        profile = store.loadProfile()
        conversations = store.loadConversations()
        chat = ChatModel(app: self)
        if Demo.isOn { Demo.seed(self) }
        wake()
    }

    // MARK: sky

    func wake() {
        sky = .waking
        Task {
            do {
                try await LlamaEngine.shared.load()
                sky = .ready
                if Demo.scene == "ask" { Demo.ask(self) }
            } catch {
                sky = .failed(Persona.lostSignalMessage)
            }
        }
    }

    // MARK: flow

    var returning: Bool { profile.onboardingComplete && !profile.name.trimmingCharacters(in: .whitespaces).isEmpty }

    func continueFromLanding() {
        if returning {
            chat.startSession()
            screen = .chat
        } else {
            screen = .name
        }
    }

    /// "Meet Orbit 412": remember the name and open the chat.
    func completeOnboarding(_ rawName: String) {
        let name = String(rawName.trimmingCharacters(in: .whitespacesAndNewlines).prefix(40))
        updateProfile {
            $0.name = name.isEmpty ? "Wanderer" : name
            $0.onboardingComplete = true
        }
        chat.startSession()
        screen = .chat
    }

    func goHome() {
        chat.stop()
        menuOpen = false
        screen = .landing
    }

    // MARK: profile (saved as it changes)

    func updateProfile(_ change: (inout Profile) -> Void) {
        var next = profile
        change(&next)
        guard next != profile else { return }
        profile = next
        store.save(next)
    }

    static func isValidEmail(_ value: String) -> Bool {
        value.range(of: "^[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", options: .regularExpression) != nil
    }

    // MARK: conversations

    var sortedConversations: [Conversation] { conversations.sorted { $0.updatedAt > $1.updatedAt } }

    /// The side menu's list: everything, or the matches for the search.
    var menuConversations: [Conversation] {
        let q = searchQuery.trimmingCharacters(in: .whitespaces)
        guard !q.isEmpty else { return sortedConversations }
        return sortedConversations.filter { c in
            c.title.localizedCaseInsensitiveContains(q) || c.messages.contains { $0.content.localizedCaseInsensitiveContains(q) }
        }
    }

    func conversation(_ id: String) -> Conversation? { conversations.first { $0.id == id } }

    func createConversation(title: String) -> String {
        let now = Date()
        let c = Conversation(id: UUID().uuidString, title: title, createdAt: now, updatedAt: now, messages: [])
        conversations.append(c)
        store.save(conversations)
        return c.id
    }

    func addMessage(to id: String, role: String, content: String, id messageId: String, frameId: String? = nil) {
        guard let i = conversations.firstIndex(where: { $0.id == id }) else { return }
        conversations[i].messages.append(StoredMessage(id: messageId, role: role, content: content, timestamp: Date(), frameId: frameId))
        conversations[i].updatedAt = Date()
        store.save(conversations)
    }

    func rename(_ id: String, to title: String) {
        let clean = String(title.replacingOccurrences(of: "\\s+", with: " ", options: .regularExpression)
            .trimmingCharacters(in: .whitespaces).prefix(60))
        guard !clean.isEmpty, let i = conversations.firstIndex(where: { $0.id == id }) else { return }
        conversations[i].title = clean
        store.save(conversations)
    }

    func delete(_ id: String) {
        conversations.removeAll { $0.id == id }
        store.save(conversations)
        if chat.conversationId == id { chat.newConversation() }
    }

    func clearAll() {
        conversations.removeAll()
        store.save(conversations)
        chat.newConversation()
    }

    func exportData() -> Data { Store.exportJSON(sortedConversations) }

    /// Only for the screen checks.
    func seed(profile: Profile, conversations: [Conversation]) {
        self.profile = profile
        self.conversations = conversations
    }
}
