import SwiftUI
import Observation

/// One chat with Orbit 412 (the Android app's ChatViewModel). The question and the streaming
/// answer are shown at once and saved under the same ids.
@MainActor
@Observable
final class ChatModel {
    struct Line: Identifiable, Equatable {
        let id: String
        let fromUser: Bool
        var text: String
        var complete: Bool
        /// A frame from Orbit's archive sent with this answer (an id in frames/frames.json).
        var frameId: String? = nil
    }

    unowned let app: AppModel
    var conversationId: String?
    var lines: [Line] = []
    var generating = false
    var perspective: Perspective = .orbit
    var tone: Tone = .balanced
    var input = ""
    /// Counts questions sent (the sky's uplink pulse follows it).
    var uplinks = 0

    @ObservationIgnored private var task: Task<Void, Never>?
    @ObservationIgnored private var activeAnswerId: String?

    init(app: AppModel) {
        self.app = app
    }

    var isEmpty: Bool { lines.isEmpty }

    /// A fresh, empty sky with the profile's point of view and tone (entering the chat).
    func startSession() {
        newConversation()
        perspective = .from(app.profile.perspective)
        tone = .from(app.profile.tone)
    }

    func cycleTone() { tone = tone.next }

    func send(_ raw: String) {
        let question = Persona.sanitize(raw)
        guard !question.isEmpty, !generating, app.sky == .ready else { return }
        app.speaker.stop()
        if app.profile.haptics { Haptics.tap() }

        let userId = UUID().uuidString
        let answerId = UUID().uuidString
        let frame = pickFrame(for: question)
        withAnimation(.easeOut(duration: 0.45)) {
            lines.append(Line(id: userId, fromUser: true, text: question, complete: true))
            lines.append(Line(id: answerId, fromUser: false, text: "", complete: false, frameId: frame?.id))
        }
        input = ""
        generating = true
        uplinks += 1
        activeAnswerId = answerId

        let id: String
        if let current = conversationId, app.conversation(current) != nil {
            id = current
        } else {
            id = app.createConversation(title: Persona.titleFor(question))
            conversationId = id
        }
        let history = (app.conversation(id)?.messages ?? []).map { (role: $0.role, content: $0.content) }
        app.addMessage(to: id, role: "user", content: question, id: userId)
        let system = Persona.systemPrompt(name: app.profile.name, city: app.profile.city)
        let prompt = Persona.userPrompt(question, perspective: perspective, tone: tone, frame: frame)

        let sink = AnswerSink(model: self, answerId: answerId)
        task = Task { [weak self] in
            var raw = ""
            var failed = false
            do {
                raw = try await LlamaEngine.shared.answer(system: system, history: history, prompt: prompt) { partial in
                    sink.push(partial)
                }
            } catch {
                failed = true
            }
            guard let self else { return }
            if failed { self.fail(answerId) } else { self.finish(id, answerId, raw: raw) }
        }
    }

    /// The frame from Orbit's archive to send with the answer, chosen before the model starts so it
    /// can describe it (never one already sent in this conversation).
    private func pickFrame(for question: String) -> Frame? {
        let lastQuestion = lines.last(where: { $0.fromUser })?.text ?? ""
        let lastAnswer = lines.last(where: { !$0.fromUser })?.text ?? ""
        return FramePicker.pick(FrameCatalog.shared, question: question,
                                context: lastQuestion + " " + lastAnswer,
                                shown: Set(lines.compactMap(\.frameId)),
                                hour: Calendar.current.component(.hour, from: Date()),
                                city: app.profile.city)
    }

    fileprivate func update(_ answerId: String, raw: String) {
        guard let i = lines.firstIndex(where: { $0.id == answerId }), !lines[i].complete else { return }
        lines[i].text = Persona.visibleAnswer(Persona.clean(raw, final: false))
    }

    private func endTurn(_ answerId: String) {
        guard answerId == activeAnswerId else { return }
        activeAnswerId = nil
        generating = false
    }

    private func finish(_ conversation: String, _ answerId: String, raw: String) {
        let current = answerId == activeAnswerId
        endTurn(answerId)
        guard let i = lines.firstIndex(where: { $0.id == answerId }) else { return }
        let text = Persona.visibleAnswer(Persona.clean(raw, final: true))
        lines[i].text = text.isEmpty ? "…" : text
        lines[i].complete = true
        guard !text.isEmpty else {
            lines[i].frameId = nil
            return
        }
        app.addMessage(to: conversation, role: "assistant", content: text, id: answerId, frameId: lines[i].frameId)
        if current {
            if app.profile.haptics { Haptics.landed() }
            if app.profile.readAloud { app.speaker.speak(text) }
        }
        Demo.answered(text)
    }

    private func fail(_ answerId: String) {
        endTurn(answerId)
        guard let i = lines.firstIndex(where: { $0.id == answerId }) else { return }
        lines[i].text = Persona.fadedSignal
        lines[i].complete = true
        lines[i].frameId = nil
    }

    /// The stop square: the answer ends with what has arrived.
    func stop() {
        task?.cancel()
    }

    /// "+" in the header: an empty sky again (the old conversation stays in the side menu).
    func newConversation() {
        task?.cancel()
        task = nil
        app.speaker.stop()
        activeAnswerId = nil
        generating = false
        lines = []
        conversationId = nil
        input = ""
    }

    /// A conversation picked in the side menu.
    func open(_ id: String) {
        guard id != conversationId else { return }
        newConversation()
        conversationId = id
        lines = (app.conversation(id)?.messages ?? []).map {
            Line(id: $0.id, fromUser: $0.role == "user", text: $0.content, complete: true, frameId: $0.frameId)
        }
    }

    /// Only for the screen checks.
    func seed(lines: [Line], conversationId: String?) {
        self.lines = lines
        self.conversationId = conversationId
    }
}

/// Carries the streaming answer from the engine to the chat on the main thread.
private final class AnswerSink: @unchecked Sendable {
    weak var model: ChatModel?
    let answerId: String

    init(model: ChatModel, answerId: String) {
        self.model = model
        self.answerId = answerId
    }

    func push(_ raw: String) {
        Task { @MainActor in self.model?.update(self.answerId, raw: raw) }
    }
}
