package ai.humanwonder.poc

/** "Orbit / Ground" toggle in the prompt box: where the sky speaks from. */
enum class Perspective(val label: String, val hint: String) {
    Orbit("Orbit", "Describe it as you see it from orbit."),
    Ground("Ground", "Help them see it from where they stand on the ground, looking up."),
    ;

    companion object {
        fun from(name: String?): Perspective = entries.firstOrNull { it.name == name } ?: Orbit
    }
}

/** The "✦ Balanced" chip in the prompt box: how the sky answers. */
enum class Tone(val label: String, val hint: String) {
    Balanced("Balanced", ""),
    Poetic("Poetic", "Answer poetically, with one vivid image."),
    Scientific("Scientific", "Answer precisely, with one or two clear facts."),
    Whisper("Whisper", "Answer in one short, gentle sentence."),
    ;

    companion object {
        fun from(name: String?): Tone = entries.firstOrNull { it.name == name } ?: Balanced
    }
}

/** First-launch progress of the sky, in the order the person sees it. */
sealed interface SkyState {
    /** Copying the bundled model out of the APK (first launch only). */
    data class Aligning(val progress: Float) : SkyState
    data object Waking : SkyState
    data object Ready : SkyState
    data class Failed(val message: String) : SkyState
}

/** One line of a conversation as the chat screen shows it. */
data class ChatMessage(
    val id: String,
    val fromUser: Boolean,
    val text: String,
    val complete: Boolean = true,
    val time: Long = System.currentTimeMillis(),
    /** A frame from Orbit's archive sent with this answer (an id in assets/frames/frames.json). */
    val frameId: String? = null,
)

/** Everything the chat screen draws. */
data class UiState(
    val name: String = "",
    val sky: SkyState = SkyState.Aligning(0f),
    val conversationId: String? = null,
    val messages: List<ChatMessage> = emptyList(),
    val generating: Boolean = false,
    val perspective: Perspective = Perspective.Orbit,
    val tone: Tone = Tone.Balanced,
    val haptics: Boolean = true,
)
