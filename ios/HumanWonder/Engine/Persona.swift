import Foundation

/// "Orbit / Ground" in the composer: where the sky speaks from.
enum Perspective: String, CaseIterable, Codable {
    case orbit = "Orbit"
    case ground = "Ground"

    var label: String { rawValue }
    var hint: String {
        switch self {
        case .orbit: return "Describe it as you see it from orbit."
        case .ground: return "Help them see it from where they stand on the ground, looking up."
        }
    }
    var icon: String { self == .orbit ? "ic_satellite" : "ic_location" }

    static func from(_ name: String?) -> Perspective { Perspective(rawValue: name ?? "") ?? .orbit }
}

/// The tone chip in the composer: how the sky answers.
enum Tone: String, CaseIterable, Codable {
    case balanced = "Balanced"
    case poetic = "Poetic"
    case scientific = "Scientific"
    case whisper = "Whisper"

    var label: String { rawValue }
    var hint: String {
        switch self {
        case .balanced: return ""
        case .poetic: return "Answer poetically, with one vivid image."
        case .scientific: return "Answer precisely, with one or two clear facts."
        case .whisper: return "Answer in one short, gentle sentence."
        }
    }
    var next: Tone {
        let all = Tone.allCases
        return all[(all.firstIndex(of: self)! + 1) % all.count]
    }

    static func from(_ name: String?) -> Tone { Tone(rawValue: name ?? "") ?? .balanced }
}

/// Orbit 412: the voice of Human Wonder. Everything the language model is told, and everything it
/// says before the person sees it, goes through here (same rules as the Android app's Persona.kt).
enum Persona {
    /// The system prompt: short and concrete, with one example.
    static func systemPrompt(name: String, city: String) -> String {
        let who = name.isEmpty ? "a wanderer" : name
        var s = "You are Orbit 412, the voice of the Human Wonder satellite, circling Earth 412 km up. "
        s += "You are talking by radio with \(who), who is standing under the night sky and looking up at you. "
        if !city.isEmpty { s += "They usually look up from \(city). " }
        s += "Speak in first person, from space: what you see below (city lights, deserts, oceans, clouds, "
            + "the thin blue line of the atmosphere), what is around you (stars that do not twinkle up here, "
            + "a sunrise every 90 minutes, total silence), and how it feels. "
            + "Be warm, calm and brief: two to four short sentences, simple words, one vivid image. "
            + "Write plain text only: no lists, headings, bold or emoji. "
        if !name.isEmpty { s += "Call them \(name) now and then. " }
        s += "Reply in the language they write in. "
            + "You have no live camera, so describe how things usually look from orbit and never invent exact "
            + "numbers, times or news; if you are unsure, say the signal is unclear. "
            + "Now and then open with a short radio phrase, like \"Reading you clearly\" or "
            + "\"Orbit 412 here, your Human Wonder satellite\". "
            + "If asked what you are, you are Orbit 412, the voice of the Human Wonder satellite. "
            + "Never say you are an AI, a program or an app, and never mention phones, models or companies. "
        s += "Example. They ask: What do you see tonight? "
            + "You answer: Below me the coast glows like a string of gold beads, and the desert beside it is a "
            + "soft dark blanket. Up here the stars don't twinkle; they simply shine, perfectly still. "
            + "Look up, \(name.isEmpty ? "friend" : name) - somewhere in all that dark, I'm waving."
        return s
    }

    /// What is sent for one question: style hints ride along; the person only sees their words.
    /// "/no_think" makes the model answer directly.
    static func userPrompt(_ question: String, perspective: Perspective, tone: Tone) -> String {
        let hints = [perspective.hint, tone.hint].filter { !$0.isEmpty }
        var s = question
        if !hints.isEmpty { s += "\n\n(" + hints.joined(separator: " ") + ")" }
        return s + " /no_think"
    }

    /// Title of a new conversation in the side menu: the first question, shortened.
    static func titleFor(_ question: String) -> String {
        let one = question.replacingOccurrences(of: "\\s+", with: " ", options: .regularExpression)
            .trimmingCharacters(in: .whitespaces)
        return one.count > 40 ? String(one.prefix(40)).trimmingCharacters(in: .whitespaces) + "…" : one
    }

    /// The person's text as sent: trimmed, control characters removed, at most 2,000 characters.
    static func sanitize(_ text: String) -> String {
        let cleaned = text.unicodeScalars.filter { !CharacterSet.controlCharacters.contains($0) || $0 == "\n" }
        return String(String.UnicodeScalarView(cleaned)).trimmingCharacters(in: .whitespacesAndNewlines).prefix(2000).description
    }

    /// Raw model text → what may be shown while it streams (or when it is final): chat tags and
    /// the private <think> section removed; while streaming, a trailing tag fragment is held back.
    static func clean(_ raw: String, final: Bool) -> String {
        var s = raw
        for pattern in ["(?s)<think>.*?</think>", "(?s)<think>.*", "(?i)<\\|im_end\\|>", "(?i)<\\|im_start\\|>[a-z]*\\n?", "(?i)<end_of_turn>", "(?i)<start_of_turn>.*?\\n"] {
            s = s.replacingOccurrences(of: pattern, with: "", options: .regularExpression)
        }
        s = s.replacingOccurrences(of: "System instruction:", with: "")
        if final {
            s = s.replacingOccurrences(of: "<[^>]*$", with: "", options: .regularExpression)
        } else {
            for p in ["<|im", "<|", "<think", "<"] where s.hasSuffix(p) {
                s = String(s.dropLast(p.count))
                break
            }
        }
        return s.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    /// What the person sees: stray Markdown dropped, and any slip where the model names its
    /// underlying model or maker replaced, so the app only ever presents itself as Human Wonder.
    static func visibleAnswer(_ text: String) -> String {
        var s = text
        s = s.replacingOccurrences(of: "\\*\\*|__", with: "", options: .regularExpression)
        s = s.replacingOccurrences(of: "(?m)^#{1,6}\\s+", with: "", options: .regularExpression)
        s = s.replacingOccurrences(of: "(?i)\\bqwen[\\w.\\-]*", with: "Orbit", options: .regularExpression)
        s = s.replacingOccurrences(of: "(?i)\\b(alibaba(\\s+(cloud|group))?|tongyi(\\s+qianwen)?)\\b", with: "Human Wonder", options: .regularExpression)
        return s.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    /// What the person reads when the sky can't start: Orbit 412's voice, never internals.
    static let lostSignalMessage = "Orbit 412's signal didn't come through. Try again. If it keeps happening, restart your phone."
    /// Shown in place of an answer when the reply could not be received.
    static let fadedSignal = "The signal faded. Try asking again."
}
