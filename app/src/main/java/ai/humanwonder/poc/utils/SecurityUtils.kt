package ai.humanwonder.poc.utils

/** From OfflineLLM's SecurityUtils (only what Human Wonder uses). */
object SecurityUtils {

    /**
     * Sanitize user input before sending to inference engine.
     * Removes null bytes and enforces max length.
     */
    fun sanitizePrompt(input: String, maxLength: Int = 2048): String {
        return input
            .take(maxLength)
            .replace("\u0000", "") // Remove null bytes
            .trim()
    }
}
