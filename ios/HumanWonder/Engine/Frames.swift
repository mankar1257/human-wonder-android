import Foundation

/// A frame from Orbit's archive: an astronaut photograph of Earth, or a short loop cut from the
/// station's time-lapses (the Android app's assets/frames folder, bundled as "frames/"). Same catalog as the Android app (frames.json).
struct Frame: Codable, Identifiable, Equatable {
    let id: String
    /// "photo" (a JPEG) or "loop" (an animated WebP).
    let kind: String
    /// "night", "day" or "any".
    let time: String
    /// The NASA frame number(s), for the credit.
    let source: String
    /// Short title under the picture.
    let caption: String
    /// What the picture shows, in words for the model.
    let says: String
    /// Words in a question that point to this picture, with how strongly (1 to 3).
    let keys: [String: Int]

    var isLoop: Bool { kind == "loop" }

    /// The picture inside the app.
    var url: URL? {
        Bundle.main.url(forResource: id, withExtension: isLoop ? "webp" : "jpg", subdirectory: "frames")
    }
}

struct FrameRegion: Codable {
    let id: String
    let keys: [String]
    let night: [String]
    let day: [String]
}

struct FrameCatalog: Codable {
    let credit: String
    let showWords: [String]
    let seeWords: [String]
    let aboutMeWords: [String]
    let loopWords: [String]
    let nightWords: [String]
    let dayWords: [String]
    let regions: [FrameRegion]
    let frames: [Frame]

    enum CodingKeys: String, CodingKey {
        case credit, regions, frames
        case showWords = "show_words"
        case seeWords = "see_words"
        case aboutMeWords = "about_me_words"
        case loopWords = "loop_words"
        case nightWords = "night_words"
        case dayWords = "day_words"
    }

    func frame(_ id: String?) -> Frame? {
        guard let id else { return nil }
        return frames.first { $0.id == id }
    }

    static let empty = FrameCatalog(credit: "", showWords: [], seeWords: [], aboutMeWords: [], loopWords: [],
                                    nightWords: [], dayWords: [], regions: [], frames: [])

    /// The bundled catalog (read once). Empty if it is missing, so chat still works.
    static let shared: FrameCatalog = {
        guard let url = Bundle.main.url(forResource: "frames", withExtension: "json", subdirectory: "frames"),
              let data = try? Data(contentsOf: url),
              let catalog = try? JSONDecoder().decode(FrameCatalog.self, from: data) else { return .empty }
        return catalog
    }()
}

/// Chooses the picture for an answer from the question alone, before the model starts, so the model
/// can be told what it is sending. Same steps as tools/frames/pick.py and the Android FramePicker.
enum FramePicker {
    private static let minScore = 3

    /// Lower case; anything that is not a letter, digit or combining mark becomes one space.
    static func normalize(_ text: String) -> String {
        var out = " "
        var space = true
        for scalar in text.lowercased().unicodeScalars {
            let keep: Bool
            switch scalar.properties.generalCategory {
            case .uppercaseLetter, .lowercaseLetter, .titlecaseLetter, .modifierLetter, .otherLetter,
                 .nonspacingMark, .spacingMark, .enclosingMark,
                 .decimalNumber, .letterNumber, .otherNumber:
                keep = true
            default:
                keep = false
            }
            if keep {
                out.unicodeScalars.append(scalar)
                space = false
            } else if !space {
                out += " "
                space = true
            }
        }
        if !space { out += " " }
        return out
    }

    static func has(_ text: String, _ key: String) -> Bool {
        let k = normalize(key).trimmingCharacters(in: .whitespaces)
        guard !k.isEmpty else { return false }
        if text.contains(" \(k) ") { return true }
        return k.unicodeScalars.allSatisfy(\.isASCII) && text.contains(" \(k)s ")
    }

    private static func score(_ text: String, _ frame: Frame) -> Int {
        frame.keys.reduce(0) { $0 + (has(text, $1.key) ? $1.value : 0) }
    }

    /// The picture to send with the answer to `question`, or nil. `context` is the previous question
    /// and answer; `shown` the ids already sent in this conversation; `hour` the local hour.
    static func pick(_ catalog: FrameCatalog, question: String, context: String = "", shown: Set<String> = [],
                     hour: Int? = nil, city: String = "") -> Frame? {
        guard !catalog.frames.isEmpty else { return nil }
        let q = normalize(question)
        let asks = question.contains("?") || question.contains("\u{061F}")
        let wantsShow = catalog.showWords.contains { has(q, $0) } || (asks && catalog.seeWords.contains { has(q, $0) })
        let wantsLoop = catalog.loopWords.contains { has(q, $0) }
        let saysNight = catalog.nightWords.contains { has(q, $0) }
        let saysDay = catalog.dayWords.contains { has(q, $0) }
        let clockNight = hour.map { $0 >= 19 || $0 < 6 } ?? false
        let night = saysNight || (!saysDay && clockNight)

        let frames = catalog.frames
        var scores = frames.map { score(q, $0) }
        if (scores.max() ?? 0) < minScore && wantsShow && !context.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            let c = normalize(context)
            scores = zip(scores, frames).map { $0 + score(c, $1) }
        }

        var best: Frame?
        var bestRank = Int.min
        for (s, f) in zip(scores, frames) where s >= minScore && !shown.contains(f.id) {
            var rank = s
            if wantsLoop && f.isLoop { rank += 2 }
            if f.time != "any" {
                if night {
                    rank += f.time == "night" ? 2 : -2
                } else if saysDay || hour != nil {
                    rank += f.time == "day" ? 2 : -2
                }
            }
            if rank > bestRank {
                best = f
                bestRank = rank
            }
        }
        if let best { return best }
        if !wantsShow || catalog.aboutMeWords.contains(where: { has(q, $0) }) { return nil }

        // "What do you see?" with no subject: a view over the person's part of the world.
        let whereText = q + " " + normalize(context) + " " + normalize(city)
        guard let region = catalog.regions.first(where: { r in r.keys.contains { has(whereText, $0) } }) ?? catalog.regions.last
        else { return nil }
        let choices = (night ? region.night : region.day).compactMap { catalog.frame($0) }
        let ordered = choices.filter { wantsLoop && $0.isLoop } + choices.filter { !(wantsLoop && $0.isLoop) }
        return ordered.first { !shown.contains($0.id) }
    }
}
