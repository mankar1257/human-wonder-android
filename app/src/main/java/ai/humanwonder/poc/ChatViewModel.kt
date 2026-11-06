package ai.humanwonder.poc

import ai.humanwonder.poc.model.LocalModel
import ai.humanwonder.poc.model.ModelSpec
import ai.humanwonder.poc.model.ModelStore
import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arm.aichat.AiChat
import com.arm.aichat.InferenceEngine
import com.arm.aichat.isModelLoaded
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/** What the phone looks like, shown on the consent screen before downloading. */
data class DeviceCheck(
    val totalRamBytes: Long,
    val freeStorageBytes: Long,
    val bytesNeeded: Long,
    val alreadyDownloaded: Long,
    val onMeteredNetwork: Boolean,
    val online: Boolean,
) {
    val enoughStorage get() = freeStorageBytes >= bytesNeeded
    val lowRam get() = totalRamBytes in 1 until 3L * 1024 * 1024 * 1024
}

sealed interface Phase {
    data object Starting : Phase
    /** No model on the phone yet: ask the user before downloading anything. */
    data class NeedsConsent(val check: DeviceCheck) : Phase
    data class Downloading(val done: Long, val total: Long) : Phase
    data class Verifying(val done: Long, val total: Long) : Phase
    data class Importing(val done: Long, val total: Long) : Phase
    data object LoadingModel : Phase
    data object Ready : Phase
    data class Failed(val message: String, val offerRedownload: Boolean) : Phase
}

data class ChatMessage(
    val id: Long,
    val fromUser: Boolean,
    val text: String,
    val complete: Boolean = true,
)

data class UiState(
    val phase: Phase = Phase.Starting,
    val messages: List<ChatMessage> = emptyList(),
    val generating: Boolean = false,
    val modelLabel: String = "",
    val tokensPerSecond: Float? = null,
)

class ChatViewModel(app: Application) : AndroidViewModel(app) {

    private val store = ModelStore(app)
    private val engine: InferenceEngine by lazy { AiChat.getInferenceEngine(app) }

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    private var activeModel: LocalModel? = null
    private var workJob: Job? = null
    private var generationJob: Job? = null
    private var nextId = 0L

    init {
        val local = store.localModel()
        if (local != null) loadModel(local) else _ui.update { it.copy(phase = Phase.NeedsConsent(deviceCheck())) }
    }

    // ---------------------------------------------------------------- model setup

    /** Called only after the user taps "Allow download" on the consent screen. */
    fun startDownload() {
        if (workJob?.isActive == true) return
        workJob = viewModelScope.launch {
            try {
                _ui.update { it.copy(phase = Phase.Downloading(store.partialBytes(), ModelSpec.SIZE_BYTES)) }
                store.download { done, total ->
                    _ui.update { it.copy(phase = Phase.Downloading(done, total)) }
                }
                _ui.update { it.copy(phase = Phase.Verifying(0, ModelSpec.SIZE_BYTES)) }
                val ok = store.verifyDownload { done, total ->
                    _ui.update { it.copy(phase = Phase.Verifying(done, total)) }
                }
                if (!ok) {
                    fail("The download was corrupted and has been deleted. Please try again.", offerRedownload = true)
                    return@launch
                }
                store.localModel()?.let { loadModel(it) }
            } catch (e: CancellationException) {
                // Paused by the user: the partial file is kept, so the next attempt resumes.
                _ui.update { it.copy(phase = Phase.NeedsConsent(deviceCheck())) }
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Download failed", e)
                fail(
                    "Download interrupted: ${e.message ?: "network error"}. " +
                        "Your progress is saved; tap retry to resume.",
                    offerRedownload = true,
                )
            }
        }
    }

    fun pauseDownload() {
        workJob?.cancel()
    }

    fun importModel(uri: Uri) {
        if (workJob?.isActive == true) return
        workJob = viewModelScope.launch {
            try {
                _ui.update { it.copy(phase = Phase.Importing(0, -1)) }
                val model = store.importFrom(getApplication<Application>().contentResolver, uri) { done, total ->
                    _ui.update { it.copy(phase = Phase.Importing(done, total)) }
                }
                loadModel(model)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Import failed", e)
                fail(e.message ?: "Could not import that file.", offerRedownload = false)
            }
        }
    }

    fun retry() {
        val local = store.localModel()
        if (local != null) loadModel(local) else _ui.update { it.copy(phase = Phase.NeedsConsent(deviceCheck())) }
    }

    fun deleteModelAndRestart() {
        generationJob?.cancel()
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                unloadQuietly()
                store.deleteAll()
            }
            activeModel = null
            _ui.update { UiState(phase = Phase.NeedsConsent(deviceCheck())) }
        }
    }

    private fun loadModel(model: LocalModel) {
        _ui.update { it.copy(phase = Phase.LoadingModel, modelLabel = model.label) }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    // The engine initialises its native library asynchronously on creation.
                    withTimeout(30_000) {
                        engine.state.first {
                            it is InferenceEngine.State.Initialized || it.isModelLoaded ||
                                it is InferenceEngine.State.Error
                        }
                    }
                    unloadQuietly()
                    engine.loadModel(model.file.path)
                    engine.setSystemPrompt(SYSTEM_PROMPT)
                }
                activeModel = model
                _ui.update { it.copy(phase = Phase.Ready, modelLabel = model.label) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                Log.e(TAG, "Model load failed", e)
                fail(
                    "The model could not be loaded on this phone (${e.javaClass.simpleName}). " +
                        "You can delete it and download it again.",
                    offerRedownload = true,
                )
            }
        }
    }

    private fun unloadQuietly() {
        val state = engine.state.value
        if (state.isModelLoaded || state is InferenceEngine.State.Error) {
            runCatching { engine.cleanUp() }.onFailure { Log.w(TAG, "cleanUp failed", it) }
        }
    }

    // ---------------------------------------------------------------- chat

    fun send(text: String) {
        val question = text.trim()
        val state = _ui.value
        if (question.isEmpty() || state.generating || state.phase != Phase.Ready) return

        val userMsg = ChatMessage(nextId++, fromUser = true, text = question)
        val answerId = nextId++
        _ui.update {
            it.copy(
                messages = it.messages + userMsg + ChatMessage(answerId, false, "", complete = false),
                generating = true,
            )
        }

        // Qwen3 "thinks" out loud by default, which is slow on a phone. "/no_think" is its
        // documented switch for answering directly.
        val prompt = if (activeModel?.isBundledSpec == true) "$question /no_think" else question

        generationJob = viewModelScope.launch {
            val raw = StringBuilder()
            var pieces = 0
            var firstPieceAt = 0L
            var error: String? = null
            try {
                engine.sendUserPrompt(prompt, MAX_NEW_TOKENS).collect { piece ->
                    if (pieces == 0) firstPieceAt = SystemClock.elapsedRealtime()
                    pieces++
                    raw.append(piece)
                    setAnswer(answerId, visibleAnswer(raw.toString()), complete = false)
                }
            } catch (e: CancellationException) {
                // Stopped by the user; keep what was generated so far.
            } catch (e: Exception) {
                Log.e(TAG, "Generation failed", e)
                error = "Something went wrong while answering. Try again or start a new chat."
            } finally {
                val elapsed = SystemClock.elapsedRealtime() - firstPieceAt
                val speed = if (pieces > 1 && elapsed > 0) (pieces - 1) * 1000f / elapsed else null
                val answer = visibleAnswer(raw.toString())
                setAnswer(
                    answerId,
                    when {
                        error != null -> error
                        answer.isBlank() -> "…"
                        else -> answer
                    },
                    complete = true,
                )
                _ui.update { it.copy(generating = false, tokensPerSecond = speed ?: it.tokensPerSecond) }
            }
        }
    }

    fun stop() {
        generationJob?.cancel()
    }

    /** Clears the conversation (and the model's memory of it) by reloading the model. */
    fun newChat() {
        val model = activeModel ?: return
        generationJob?.cancel()
        _ui.update { it.copy(messages = emptyList(), generating = false) }
        loadModel(model)
    }

    private fun setAnswer(id: Long, text: String, complete: Boolean) {
        _ui.update { s ->
            s.copy(messages = s.messages.map { m -> if (m.id == id) m.copy(text = text, complete = complete) else m })
        }
    }

    private fun fail(message: String, offerRedownload: Boolean) {
        _ui.update { it.copy(phase = Phase.Failed(message, offerRedownload), generating = false) }
    }

    private fun deviceCheck(): DeviceCheck {
        val ctx = getApplication<Application>()
        val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mem = ActivityManager.MemoryInfo().also { am.getMemoryInfo(it) }
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.activeNetwork?.let { cm.getNetworkCapabilities(it) }
        val online = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        return DeviceCheck(
            totalRamBytes = mem.totalMem,
            freeStorageBytes = store.freeBytes(),
            bytesNeeded = store.bytesNeededToDownload(),
            alreadyDownloaded = store.partialBytes(),
            onMeteredNetwork = online && cm.isActiveNetworkMetered,
            online = online,
        )
    }

    companion object {
        private const val TAG = "HumanWonder"
        private const val MAX_NEW_TOKENS = 512

        // Kept short on purpose: a 0.6B model follows short instructions far better.
        const val SYSTEM_PROMPT =
            "You are Human Wonder, a warm and curious companion that helps people wonder " +
                "about the world and the sky. You run entirely on this phone, without internet. " +
                "Answer in plain words, in two to four short sentences unless asked for more. " +
                "Reply in the language the user writes in. If you are not sure, say so honestly " +
                "instead of guessing."

        private val THINK_BLOCK = Regex("<think>[\\s\\S]*?</think>")

        /**
         * Strips Qwen3's <think>…</think> section. While the model is still inside an
         * unfinished think block nothing is shown yet.
         */
        fun visibleAnswer(raw: String): String {
            var s = THINK_BLOCK.replace(raw, "")
            val open = s.indexOf("<think>")
            if (open >= 0) s = s.substring(0, open)
            return s.trim()
        }
    }
}
