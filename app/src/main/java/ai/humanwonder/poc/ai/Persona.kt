package ai.humanwonder.poc.ai

import ai.humanwonder.poc.Perspective
import ai.humanwonder.poc.Tone

/**
 * Orbit 412: the voice of Human Wonder. Everything the language model is told, and everything
 * it says before the person sees it, goes through here (ported from v1's ChatViewModel).
 */
object Persona {

    /**
     * The system prompt. Kept short and concrete. It has no example answer: tested on the app's
     * model, an example was copied almost word for word into 40% of answers ("the coast glows like
     * a string of gold beads", even about the Moon), and without it none were (tools/frames/README.md).
     * Rebuilt whenever the name changes.
     */
    fun systemPrompt(name: String, city: String = ""): String = buildString {
        val who = name.ifBlank { "a wanderer" }
        append("You are Orbit 412, the voice of the Human Wonder satellite, circling Earth 412 km up. ")
        append("You are talking by radio with $who, who is standing under the night sky and looking up at you. ")
        if (city.isNotBlank()) append("They usually look up from $city. ")
        append(
            "Speak in first person, from space: what you see below (city lights, deserts, oceans, clouds, " +
                "the thin blue line of the atmosphere), what is around you (stars that do not twinkle up here, " +
                "a sunrise every 90 minutes, total silence), and how it feels. " +
                "Be warm, calm and brief: two to four short sentences, simple words, one vivid image. " +
                "Write plain text only: no lists, headings, bold or emoji. ",
        )
        if (name.isNotBlank()) append("Call them $name now and then. ")
        append(
            "Reply in the language they write in. " +
                "You have no live camera, so describe how things usually look from orbit and never invent exact " +
                "numbers, times or news; if you are unsure, say the signal is unclear. " +
                "Sometimes begin with a short radio phrase, like \"Reading you clearly.\" " +
                "If asked what you are, you are Orbit 412, the voice of the Human Wonder satellite. " +
                "Never say you are an AI, a program or an app, and never mention phones, models or companies.",
        )
    }

    /**
     * The hidden line added when a frame from Orbit's archive goes with the answer, so the words
     * describe the picture the person sees (tested: the answer then describes the photo 46 times
     * in 50, against 33 when the old prompt's example was kept).
     */
    fun frameHint(frame: Frame): String =
        "You are sending them a frame from your archive that shows ${frame.says}. " +
            "Begin by telling them you are sending a frame from your archive, then describe it."

    /**
     * What is sent to the model for one question. Style hints ride along with the question; the
     * person only ever sees their own words. "/no_think" makes the model answer directly instead
     * of reasoning out loud first.
     */
    fun userPrompt(question: String, perspective: Perspective, tone: Tone, frame: Frame? = null): String {
        val hints = listOf(perspective.hint, tone.hint, frame?.let { frameHint(it) }.orEmpty()).filter { it.isNotBlank() }
        return buildString {
            append(question)
            if (hints.isNotEmpty()) append("\n\n(").append(hints.joinToString(" ")).append(")")
            append(" /no_think")
        }
    }

    /** Title of a new conversation in the side menu: the first question, shortened. */
    fun titleFor(question: String): String {
        val oneLine = question.replace(Regex("\\s+"), " ").trim()
        return if (oneLine.length > 40) oneLine.take(40).trimEnd() + "…" else oneLine
    }

    private val THINK_BLOCK = Regex("<think>[\\s\\S]*?</think>")
    /** Answers are shown as plain text, so stray Markdown (bold and italic markers, headings) is dropped. */
    private val MARKDOWN_MARKS = Regex("\\*+(?=\\S)|(?<=\\S)\\*+|__")
    private val HEADING_MARKS = Regex("(?m)^#{1,6}\\s+")
    /** A hidden style or frame line repeated back, in brackets ("(Describe it as you see it from orbit.)"). */
    private val ECHOED_HINT_BRACKETS = Regex("(?i)\\((?:describe it|help them see|answer (?:poetically|precisely|in one)|you are sending)[^()]*\\)?")
    /** The frame line repeated back as a sentence ("You are sending a frame from your archive."). */
    private val ECHOED_HINT_SENTENCE = Regex("(?i)[^.!?\\n]*\\b(?:you are sending (?:them )?a frame|begin by telling them)[^.!?\\n]*[.!?]?")
    /** A radio label on a line of its own ("Radio:", "From Orbit:"), left over from a Markdown heading. */
    private val LABEL_LINE = Regex("(?im)^\\s*(?:radio(?: voice| message)?|transmission|from orbit)\\s*:\\s*$")
    /** A label in front of the answer ("Radio voice: Reading you clearly."). */
    private val LEADING_LABEL = Regex("(?i)^\\s*(?:radio(?: voice| message)?|transmission|from orbit)\\s*:\\s*")
    /** Emoji and pictographs (plain text only), with their joiners and variation selectors. */
    private val EMOJI = Regex("[\\x{1F000}-\\x{1FAFF}\\x{2600}-\\x{27BF}\\x{2B50}\\x{2B55}\\x{FE0F}]")
    /** A joiner left behind by a removed emoji (kept between letters, where Indic scripts need it). */
    private val LOOSE_JOINER = Regex("(?<![\\p{L}\\p{M}])\\x{200D}|\\x{200D}(?![\\p{L}\\p{M}])")
    private val SPACES = Regex("[ \\t]*\\n[\\s]*|[ \\t]{2,}")
    private val MODEL_NAME = Regex("(?i)\\bqwen[\\w.\\-]*")
    private val MAKER_NAME = Regex("(?i)\\b(alibaba(\\s+(cloud|group))?|tongyi(\\s+qianwen)?)\\b")

    /**
     * What the person sees: the model's private <think> section removed, and any slip where
     * it names its underlying model or maker replaced, so the app only ever presents itself
     * as Human Wonder.
     */
    fun visibleAnswer(raw: String): String {
        var s = THINK_BLOCK.replace(raw, "")
        val open = s.indexOf("<think>")
        if (open >= 0) s = s.substring(0, open)
        s = MARKDOWN_MARKS.replace(s, "")
        s = HEADING_MARKS.replace(s, "")
        s = ECHOED_HINT_BRACKETS.replace(s, "")
        s = ECHOED_HINT_SENTENCE.replace(s, "")
        s = LABEL_LINE.replace(s, "")
        s = EMOJI.replace(s, "")
        s = LOOSE_JOINER.replace(s, "")
        // One paragraph of plain text, as the persona asks; line breaks become spaces.
        s = SPACES.replace(s.trim(), " ")
        s = LEADING_LABEL.replace(s, "")
        s = MODEL_NAME.replace(s, "Orbit")
        s = MAKER_NAME.replace(s, "Human Wonder")
        return s.trim()
    }

    /** What the person reads when setup fails: Orbit 412's voice, never internals. */
    fun lostSignalMessage(e: Throwable): String = when (e) {
        is ModelManager.NoRoomException ->
            "Orbit 412 needs about ${e.neededMb} MB of free space on your phone to reach you. " +
                "Free up some space, then try again."
        else -> "Orbit 412's signal didn't come through. Try again. If it keeps happening, restart your phone."
    }

    /** Shown in place of an answer when the reply could not be received. */
    const val FADED_SIGNAL = "The signal faded. Try asking again."
}
