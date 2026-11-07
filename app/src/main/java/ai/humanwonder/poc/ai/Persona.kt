package ai.humanwonder.poc.ai

import ai.humanwonder.poc.Perspective
import ai.humanwonder.poc.Tone

/**
 * Orbit 412: the voice of Human Wonder. Everything the language model is told, and everything
 * it says before the person sees it, goes through here (ported from v1's ChatViewModel).
 */
object Persona {

    /**
     * The system prompt. Kept short and concrete (a small model follows a compact persona with
     * one example far better than a long list of rules). Rebuilt whenever the name changes.
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
                "Now and then open with a short radio phrase, like \"Reading you clearly\" or " +
                "\"Orbit 412 here, your Human Wonder satellite\". " +
                "If asked what you are, you are Orbit 412, the voice of the Human Wonder satellite. " +
                "Never say you are an AI, a program or an app, and never mention phones, models or companies. ",
        )
        append(
            "Example. They ask: What do you see tonight? " +
                "You answer: Below me the coast glows like a string of gold beads, and the desert beside it is a " +
                "soft dark blanket. Up here the stars don't twinkle; they simply shine, perfectly still. " +
                "Look up, ${name.ifBlank { "friend" }} - somewhere in all that dark, I'm waving.",
        )
    }

    /**
     * What is sent to the model for one question. Style hints ride along with the question; the
     * person only ever sees their own words. "/no_think" makes the model answer directly instead
     * of reasoning out loud first.
     */
    fun userPrompt(question: String, perspective: Perspective, tone: Tone): String {
        val hints = listOf(perspective.hint, tone.hint).filter { it.isNotBlank() }
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
    /** Answers are shown as plain text, so stray Markdown (bold markers, headings) is dropped. */
    private val MARKDOWN_MARKS = Regex("\\*\\*|__|(?m)^#{1,6}\\s+")
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
