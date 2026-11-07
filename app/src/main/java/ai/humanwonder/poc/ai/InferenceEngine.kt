package ai.humanwonder.poc.ai

import ai.humanwonder.poc.smollm.SmolLM
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.time.measureTime

/**
 * Thin, thread-safe wrapper around [SmolLM], ported from OfflineLLM's InferenceEngine.
 *
 * Human Wonder changes: CPU only (no GPU retry), [loadModel] is a suspend function, the native
 * library is loaded lazily (a failure becomes a "lost signal" instead of a crash at start-up),
 * [resetConversation] switches conversations without reloading the model, a reply is capped at
 * `maxTokens` pieces, stopping never touches native state from a second thread, and a stopped
 * reply always reports back through `onCancelled`.
 */
class InferenceEngine {

    private val instance: SmolLM by lazy { SmolLM() }
    private val stateLock = ReentrantLock()

    @Volatile
    private var generationJob: Job? = null

    val isModelLoaded = AtomicBoolean(false)

    @Volatile
    var isGenerating = false
        private set

    data class GenerationResult(
        val response: String,
        val tokensPerSecond: Float,
        val durationSeconds: Int,
        val contextLengthUsed: Int,
    )

    /** Loads ggml backend plugins (the best CPU variant for this phone) from the app's lib dir. */
    fun initBackends(nativeLibDir: String) = instance.loadBackends(nativeLibDir)

    /** Loads the model and seeds the chat with [systemPrompt] and [conversationHistory]. Throws on failure. */
    suspend fun loadModel(
        modelPath: String,
        params: SmolLM.InferenceParams,
        systemPrompt: String,
        conversationHistory: List<Pair<String, String>> = emptyList(),
    ) {
        unloadModel()
        instance.load(modelPath, params)
        withContext(Dispatchers.Default) {
            if (systemPrompt.isNotBlank()) instance.addSystemPrompt(systemPrompt)
            for ((role, content) in conversationHistory) instance.addChatMessage(role, content)
        }
        isModelLoaded.set(true)
    }

    /**
     * Forgets the current conversation (and its KV cache) and starts again from [systemPrompt]
     * and [conversationHistory], without reloading the model. Stops any reply in progress first.
     */
    suspend fun resetConversation(systemPrompt: String, conversationHistory: List<Pair<String, String>>) {
        cancelAndJoinGeneration()
        if (!isModelLoaded.get()) return
        withContext(Dispatchers.Default) {
            instance.resetChat()
            if (systemPrompt.isNotBlank()) instance.addSystemPrompt(systemPrompt)
            for ((role, content) in conversationHistory) instance.addChatMessage(role, content)
        }
    }

    /** Tokens currently held in the context window (0 when nothing is loaded). */
    fun contextUsed(): Int =
        if (isModelLoaded.get()) runCatching { instance.getContextLengthUsed() }.getOrDefault(0) else 0

    suspend fun unloadModel() {
        isModelLoaded.set(false)
        cancelAndJoinGeneration()
        try {
            instance.close()
        } catch (_: Exception) {
        }
    }

    private suspend fun cancelAndJoinGeneration() {
        val job = stateLock.withLock { generationJob }
        job?.cancel()
        // The streaming loop notices cancellation at its next token; wait for it to leave the
        // native code before anything else touches the context. 5 s guards against a stuck call.
        withTimeoutOrNull(5_000) {
            try {
                job?.join()
            } catch (_: Exception) {
            }
        }
        isGenerating = false
    }

    fun generateResponse(
        query: String,
        maxTokens: Int,
        onToken: (String) -> Unit,
        onComplete: (GenerationResult) -> Unit,
        onCancelled: () -> Unit,
        onError: (Exception) -> Unit,
    ) {
        stateLock.withLock {
            if (!isModelLoaded.get()) {
                onError(IllegalStateException("Model not loaded"))
                return
            }

            generationJob?.cancel()

            generationJob = CoroutineScope(Dispatchers.Default).launch {
                try {
                    isGenerating = true
                    val fullResponse = StringBuilder()
                    var tokensSinceLastEmit = 0

                    val duration = measureTime {
                        instance.getResponseAsFlow(query)
                            .take(maxTokens)
                            .takeWhile { piece ->
                                fullResponse.append(piece)
                                // Only scan the tail for stop sequences.
                                val tail = if (fullResponse.length > 200) {
                                    fullResponse.substring(fullResponse.length - 200)
                                } else {
                                    fullResponse.toString()
                                }
                                !containsStopSequence(tail)
                            }
                            .collect {
                                // Batch UI updates every 3 tokens to keep recompositions light.
                                tokensSinceLastEmit++
                                if (tokensSinceLastEmit >= 3) {
                                    tokensSinceLastEmit = 0
                                    val displayResponse = cleanModelOutput(fullResponse.toString(), isFinal = false)
                                    withContext(Dispatchers.Main) { onToken(displayResponse) }
                                }
                            }
                    }

                    val finalResponse = cleanModelOutput(fullResponse.toString(), isFinal = true)
                    val speed = runCatching { instance.getResponseGenerationSpeed() }.getOrDefault(0f)
                    val used = runCatching { instance.getContextLengthUsed() }.getOrDefault(0)
                    withContext(Dispatchers.Main) {
                        isGenerating = false
                        onComplete(
                            GenerationResult(
                                response = finalResponse,
                                tokensPerSecond = speed,
                                durationSeconds = duration.inWholeSeconds.toInt(),
                                contextLengthUsed = used,
                            ),
                        )
                    }
                } catch (e: CancellationException) {
                    isGenerating = false
                    // The job is cancelled, so only a NonCancellable context can still report back.
                    withContext(NonCancellable + Dispatchers.Main) { onCancelled() }
                } catch (e: Exception) {
                    isGenerating = false
                    withContext(Dispatchers.Main) { onError(e) }
                }
            }
        }
    }

    private fun containsStopSequence(text: String): Boolean {
        val stops = listOf("<turn|", "<|turn_end|>", "<turn_end|>", "<start_of_turn>", "<end_of_turn>", "<|im_end|>")
        return stops.any { text.contains(it, ignoreCase = true) }
    }

    private fun cleanModelOutput(raw: String, isFinal: Boolean): String {
        var cleaned = raw
            .replace(Regex("<think>.*?</think>", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("<think>.*", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("<turn\\|.*?\\|>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<\\|turn_end\\|>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<turn_end\\|>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<\\|im_end\\|>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<start_of_turn>.*?\\n", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<end_of_turn>", RegexOption.IGNORE_CASE), "")
            .replace("System instruction:", "")

        if (!isFinal) {
            // Hold back a trailing tag fragment so it never flickers on screen.
            val potentialTagStart = listOf("<turn", "<|turn", "<|", "<start", "<")
            for (p in potentialTagStart) {
                if (cleaned.endsWith(p, ignoreCase = true)) {
                    return cleaned.substring(0, cleaned.length - p.length).trim()
                }
            }
        } else {
            // Final pass to remove any trailing tag fragments.
            cleaned = cleaned.replace(Regex("<.*$", RegexOption.IGNORE_CASE), "")
        }

        // Human Wonder change: no "(Empty response)" placeholder; the chat decides what to show.
        return cleaned.trim()
    }

    /** Stops the reply in progress; it reports back through `onCancelled` with what arrived. */
    fun stopGeneration() {
        stateLock.withLock {
            generationJob?.cancel()
        }
    }

    fun isReady(): Boolean = isModelLoaded.get() && !isGenerating

    companion object {
        /** True when [e] means the conversation no longer fits the context window. */
        fun isContextFull(e: Throwable): Boolean = e.message?.contains("context size", ignoreCase = true) == true
    }
}
