package ai.humanwonder.poc.ai

import ai.humanwonder.poc.smollm.SmolLM
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

/**
 * Owns the one bundled model, ported from OfflineLLM's ModelManager.
 *
 * Human Wonder changes: there is exactly one model, bundled in the APK as `assets/hw-core.gguf`
 * (the `fetchModel` Gradle task puts it there), so the model registry, file import and download
 * code are gone. The first-launch copy is v1's ModelStore (atomic, resumable, checks free space;
 * same file location, so a phone that ran v1 does not copy it again). The native chat is pointed
 * at a conversation with [prepareChat], which resets the context instead of reloading the model.
 */
class ModelManager(
    private val context: Context,
    private val inferenceEngine: InferenceEngine,
) {
    sealed interface ModelState {
        /** Copying the model out of the APK (first launch only). */
        data class Copying(val progress: Float) : ModelState
        data object Loading : ModelState
        data object Ready : ModelState
        data class Error(val cause: Throwable) : ModelState
    }

    private val _modelState = MutableStateFlow<ModelState>(ModelState.Copying(0f))
    val modelState: StateFlow<ModelState> = _modelState.asStateFlow()

    private val modelsDir = File(context.filesDir, "models")
    private val modelFile = File(modelsDir, ASSET_NAME)
    private val marker = File(modelsDir, "$ASSET_NAME.ready")

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()
    private var wakeJob: Job? = null

    /** Conversation whose turns are in the native context (null: none, or a fresh one). */
    private var loadedConversationId: String? = null
    private var loadedSystemPrompt: String? = null

    /**
     * Copies the model out of the APK if needed and loads it. Safe to call again: it does nothing
     * while a load is running or once the model is ready, and retries after an error.
     */
    fun wake(systemPrompt: () -> String) {
        if (wakeJob?.isActive == true || _modelState.value == ModelState.Ready) return
        wakeJob = scope.launch {
            try {
                mutex.withLock {
                    _modelState.value = ModelState.Copying(0f)
                    val file = copyBundledModelIfNeeded { p -> _modelState.value = ModelState.Copying(p) }
                    _modelState.value = ModelState.Loading
                    inferenceEngine.initBackends(context.applicationInfo.nativeLibraryDir ?: "")
                    val prompt = systemPrompt()
                    inferenceEngine.loadModel(file.path, inferenceParams(), prompt)
                    loadedSystemPrompt = prompt
                    loadedConversationId = null
                }
                _modelState.value = ModelState.Ready
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                // Throwable on purpose: a native library that cannot load is an Error, and it must
                // become "Orbit 412's signal is lost" with a retry, never a crash.
                Log.e(TAG, "Waking the model failed", e)
                _modelState.value = ModelState.Error(e)
            }
        }
    }

    /** Suspends until the model is ready; throws the failure if waking it failed. */
    suspend fun awaitReady() {
        val state = modelState.first { it is ModelState.Ready || it is ModelState.Error }
        if (state is ModelState.Error) throw state.cause
    }

    /**
     * Makes sure the native chat holds [conversationId] with [systemPrompt] before a question is
     * sent. Resets the context (replaying [history], newest turns that fit) when the conversation
     * or the persona changed, when the context is nearly full, or when [force] is set.
     */
    suspend fun prepareChat(
        conversationId: String,
        systemPrompt: String,
        force: Boolean,
        tight: Boolean = false,
        history: suspend () -> List<Pair<String, String>>,
    ) {
        awaitReady()
        mutex.withLock {
            val nearlyFull = inferenceEngine.contextUsed() > CONTEXT_SIZE - RESERVED_TOKENS
            if (force || nearlyFull || conversationId != loadedConversationId || systemPrompt != loadedSystemPrompt) {
                val budget = if (tight) HISTORY_TOKENS / 2 else HISTORY_TOKENS
                inferenceEngine.resetConversation(systemPrompt, trimHistory(history(), budget))
                loadedConversationId = conversationId
                loadedSystemPrompt = systemPrompt
            }
        }
    }

    /** Newest turns that fit in about [budgetTokens] tokens (4 characters per token), oldest first. */
    private fun trimHistory(all: List<Pair<String, String>>, budgetTokens: Int): List<Pair<String, String>> {
        var used = 0
        val kept = all.asReversed().takeWhile { (_, content) ->
            used += content.length / 4 + 8
            used <= budgetTokens
        }.asReversed()
        // A turn must start with the person's question.
        return kept.dropWhile { it.first != "user" }
    }

    private fun inferenceParams() = SmolLM.InferenceParams(
        // v1's sampling for this model: calm, focused, a little variety.
        temperature = 0.7f,
        topP = 0.8f,
        topK = 20,
        minP = 0.0f,
        repeatPenalty = 1.1f,
        contextSize = CONTEXT_SIZE.toLong(),
        numThreads = (Runtime.getRuntime().availableProcessors() / 2).coerceIn(4, 8),
        numThreadsBatch = Runtime.getRuntime().availableProcessors(),
        nGpuLayers = 0,
        // OfflineLLM's default: reading the weights into memory once gives a faster first reply.
        useMmap = false,
        useMlock = false,
        kvCacheQ8 = false,
    )

    /** Returns the model file, copying it out of the APK first if needed (v1's ModelStore). */
    private suspend fun copyBundledModelIfNeeded(onProgress: (Float) -> Unit): File = withContext(Dispatchers.IO) {
        modelsDir.mkdirs()
        val (total, open) = openAsset()
        if (modelFile.length() == total && marker.isFile && marker.readText().trim() == total.toString()) {
            onProgress(1f)
            return@withContext modelFile
        }

        // Clear out anything older (e.g. a model left by an earlier POC build).
        modelsDir.listFiles()?.forEach { if (it.name != ASSET_NAME) it.delete() }
        if (total > 0 && modelsDir.usableSpace < total - modelFile.length() + 64L * 1024 * 1024) {
            throw NoRoomException(total / 1_000_000 + 64)
        }

        val tmp = File(modelsDir, "$ASSET_NAME.tmp")
        open().use { input ->
            FileOutputStream(tmp).use { output ->
                val buffer = ByteArray(1 shl 20)
                var done = 0L
                var lastReported = 0L
                while (true) {
                    coroutineContext.ensureActive()
                    val n = input.read(buffer)
                    if (n < 0) break
                    output.write(buffer, 0, n)
                    done += n
                    if (total > 0 && done - lastReported >= 4L * 1024 * 1024) {
                        onProgress(done.toFloat() / total)
                        lastReported = done
                    }
                }
                output.fd.sync()
            }
        }
        if (total > 0 && tmp.length() != total) throw IOException("Copy of the model was incomplete.")
        modelFile.delete()
        if (!tmp.renameTo(modelFile)) throw IOException("Could not move the model into place.")
        marker.writeText(modelFile.length().toString())
        onProgress(1f)
        modelFile
    }

    /** Size of the bundled asset (or -1 if unknown) and a way to open it. */
    private fun openAsset(): Pair<Long, () -> InputStream> = try {
        val afd = context.assets.openFd(ASSET_NAME) // works because the asset is stored uncompressed
        val length = afd.length
        afd.close()
        length to { context.assets.openFd(ASSET_NAME).createInputStream() }
    } catch (e: IOException) {
        // Fallback if the asset was compressed by a different build setup.
        -1L to { context.assets.open(ASSET_NAME) }
    }

    /** Not enough free storage to unpack the sky (shown to the person in Orbit 412's words). */
    class NoRoomException(val neededMb: Long) : IOException("Not enough free space: about $neededMb MB is needed.")

    companion object {
        private const val TAG = "HumanWonder"
        private const val ASSET_NAME = "hw-core.gguf"

        /** Context window, as in v1 (about 0.8 GB of memory while chatting). */
        const val CONTEXT_SIZE = 2048
        /** Room kept free for the next question and its answer. */
        private const val RESERVED_TOKENS = 900
        /** Earlier turns replayed into a fresh context, after the ~400-token persona. */
        private const val HISTORY_TOKENS = 700
    }
}
