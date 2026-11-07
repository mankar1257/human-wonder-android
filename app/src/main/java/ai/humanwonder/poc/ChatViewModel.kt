package ai.humanwonder.poc

import ai.humanwonder.poc.model.ModelStore
import android.app.Application
import android.content.Context
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
import java.io.File

/** The three screens from the Stitch design. */
enum class Screen { Landing, Name, Chat }

/** "Orbit / Ground" toggle in the prompt box: where the sky speaks from. */
enum class Perspective(val label: String, val hint: String) {
    Orbit("Orbit", "Describe it as you see it from orbit."),
    Ground("Ground", "Help them see it from where they stand on the ground, looking up."),
}

/** The "✦ Balanced" chip in the prompt box: how the sky answers. */
enum class Tone(val label: String, val hint: String) {
    Balanced("Balanced", ""),
    Poetic("Poetic", "Answer poetically, with one vivid image."),
    Scientific("Scientific", "Answer precisely, with one or two clear facts."),
    Whisper("Whisper", "Answer in one short, gentle sentence."),
}

sealed interface SkyState {
    /** Copying the bundled model out of the APK (first launch only). */
    data class Aligning(val progress: Float) : SkyState
    data object Waking : SkyState
    data object Ready : SkyState
    data class Failed(val message: String) : SkyState
}

data class ChatMessage(
    val id: Long,
    val fromUser: Boolean,
    val text: String,
    val complete: Boolean = true,
    val time: Long = System.currentTimeMillis(),
)

data class UiState(
    val screen: Screen = Screen.Landing,
    val name: String = "",
    val sky: SkyState = SkyState.Aligning(0f),
    val messages: List<ChatMessage> = emptyList(),
    val generating: Boolean = false,
    val perspective: Perspective = Perspective.Orbit,
    val tone: Tone = Tone.Balanced,
    val tokensPerSecond: Float? = null,
    /** True while the name screen waits for the sky to be ready before opening the chat. */
    val entering: Boolean = false,
)

class ChatViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("human_wonder", Context.MODE_PRIVATE)
    private val store = ModelStore(app)
    private val engine: InferenceEngine by lazy { AiChat.getInferenceEngine(app) }

    private val _ui = MutableStateFlow(
        UiState(
            screen = if (prefs.getBoolean(KEY_ONBOARDED, false)) Screen.Chat else Screen.Landing,
            name = prefs.getString(KEY_NAME, "").orEmpty(),
        ),
    )
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    private var modelFile: File? = null
    private var loadedForName: String? = null
    private var setupJob: Job? = null
    private var generationJob: Job? = null
    private var nextId = 0L

    init {
        startSky()
    }

    // ------------------------------------------------------------------ navigation

    fun begin() = _ui.update { it.copy(screen = Screen.Name) }

    fun editName() {
        generationJob?.cancel()
        _ui.update { it.copy(screen = Screen.Name) }
    }

    /** "Enter the Sky Sanctuary": remember the name, wake the sky with it, open the chat. */
    fun enter(rawName: String) {
        val name = rawName.trim().take(40)
        prefs.edit().putString(KEY_NAME, name).putBoolean(KEY_ONBOARDED, true).apply()
        val nameChanged = name != _ui.value.name
        _ui.update { it.copy(name = name, entering = true) }
        if (nameChanged && loadedForName != null) {
            _ui.update { it.copy(messages = emptyList()) }
            loadModel()
        } else if (_ui.value.sky is SkyState.Failed) {
            startSky()
        }
        viewModelScope.launch {
            ui.first { it.sky is SkyState.Ready || it.sky is SkyState.Failed }
            _ui.update { it.copy(screen = Screen.Chat, entering = false) }
        }
    }

    fun backFromName(): Boolean {
        if (prefs.getBoolean(KEY_ONBOARDED, false)) {
            _ui.update { it.copy(screen = Screen.Chat, entering = false) }
            return true
        }
        _ui.update { it.copy(screen = Screen.Landing) }
        return true
    }

    // ------------------------------------------------------------------ the sky (model)

    fun retry() = startSky()

    private fun startSky() {
        if (setupJob?.isActive == true) return
        setupJob = viewModelScope.launch {
            try {
                _ui.update { it.copy(sky = SkyState.Aligning(0f)) }
                modelFile = store.prepare { p -> _ui.update { it.copy(sky = SkyState.Aligning(p)) } }
                loadModelNow()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                Log.e(TAG, "Preparing the model failed", e)
                _ui.update { it.copy(sky = SkyState.Failed(lostSignalMessage(e))) }
            }
        }
    }

    /** (Re)loads the model, after any setup already in progress (never two loads at once). */
    private fun loadModel() {
        val previous = setupJob
        setupJob = viewModelScope.launch {
            try {
                previous?.join()
                loadModelNow()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                Log.e(TAG, "Loading the model failed", e)
                _ui.update { it.copy(sky = SkyState.Failed(lostSignalMessage(e))) }
            }
        }
    }

    private suspend fun loadModelNow() {
        val file = modelFile ?: return
        val name = _ui.value.name
        _ui.update { it.copy(sky = SkyState.Waking) }
        withContext(Dispatchers.IO) {
            withTimeout(30_000) {
                engine.state.first {
                    it is InferenceEngine.State.Initialized || it.isModelLoaded ||
                        it is InferenceEngine.State.Error
                }
            }
            unloadQuietly()
            if (engine.state.value is InferenceEngine.State.Error) {
                throw IllegalStateException("The on-device engine could not start on this phone.")
            }
            engine.loadModel(file.path)
            engine.setSystemPrompt(systemPrompt(name))
        }
        loadedForName = name
        // The name may have changed while the model was waking up: wake it again with the new one.
        if (_ui.value.name != name) {
            loadModelNow()
            return
        }
        _ui.update { it.copy(sky = SkyState.Ready) }
    }

    /** What the person reads when setup fails: Orbit 412's voice, never internals. */
    private fun lostSignalMessage(e: Throwable): String = when (e) {
        is ModelStore.NoRoomException ->
            "Orbit 412 needs about ${e.neededMb} MB of free space on your phone to reach you. " +
                "Free up some space, then try again."
        else -> "Orbit 412's signal didn't come through. Try again. If it keeps happening, restart your phone."
    }

    private fun unloadQuietly() {
        val state = engine.state.value
        if (state.isModelLoaded || state is InferenceEngine.State.Error) {
            runCatching { engine.cleanUp() }.onFailure { Log.w(TAG, "cleanUp failed", it) }
        }
    }

    // ------------------------------------------------------------------ chat

    fun setPerspective(p: Perspective) = _ui.update { it.copy(perspective = p) }

    fun cycleTone() = _ui.update { it.copy(tone = Tone.entries[(it.tone.ordinal + 1) % Tone.entries.size]) }

    fun send(text: String) {
        val question = text.trim()
        val state = _ui.value
        if (question.isEmpty() || state.generating || state.sky != SkyState.Ready) return

        val userMsg = ChatMessage(nextId++, fromUser = true, text = question)
        val answerId = nextId++
        _ui.update {
            it.copy(
                messages = it.messages + userMsg + ChatMessage(answerId, false, "", complete = false),
                generating = true,
            )
        }

        // Style hints ride along with the question; the user only ever sees their own words.
        // "/no_think" makes the model answer directly instead of reasoning out loud first.
        val hints = listOf(state.perspective.hint, state.tone.hint).filter { it.isNotBlank() }
        val prompt = buildString {
            append(question)
            if (hints.isNotEmpty()) append("\n\n(").append(hints.joinToString(" ")).append(")")
            append(" /no_think")
        }

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
                // Stopped by the user; keep what arrived so far.
            } catch (e: Exception) {
                Log.e(TAG, "Generation failed", e)
                error = "The signal faded. Try asking again."
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

    /** Starts a fresh conversation (the model forgets the previous one). */
    fun newConversation() {
        generationJob?.cancel()
        _ui.update { it.copy(messages = emptyList(), generating = false) }
        if (_ui.value.sky == SkyState.Ready || _ui.value.sky is SkyState.Failed) loadModel()
    }

    private fun setAnswer(id: Long, text: String, complete: Boolean) {
        _ui.update { s ->
            s.copy(messages = s.messages.map { m -> if (m.id == id) m.copy(text = text, complete = complete) else m })
        }
    }

    companion object {
        private const val TAG = "HumanWonder"
        private const val MAX_NEW_TOKENS = 384
        private const val KEY_NAME = "name"
        private const val KEY_ONBOARDED = "onboarded"

        /**
         * Orbit: the voice of Human Wonder. Kept short and concrete (a small model follows a
         * compact persona with one example far better than a long list of rules).
         */
        fun systemPrompt(name: String): String = buildString {
            val who = name.ifBlank { "a wanderer" }
            append("You are Orbit 412, the voice of the Human Wonder satellite, circling Earth 412 km up. ")
            append("You are talking by radio with $who, who is standing under the night sky and looking up at you. ")
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
                    "Now and then open with a short radio phrase, like \"Reading you clearly\" or \"Orbit 412 here\". " +
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

        private val THINK_BLOCK = Regex("<think>[\\s\\S]*?</think>")
        /** Answers are shown as plain text, so stray Markdown (bold markers, headings) is dropped. */
        private val MARKDOWN_MARKS = Regex("\\*\\*|__|(?m)^#{1,6}\\s+")
        private val MODEL_NAME = Regex("(?i)\\bqwen[\\w.\\-]*")
        private val MAKER_NAME = Regex("(?i)\\b(alibaba(\\s+(cloud|group))?|tongyi(\\s+qianwen)?)\\b")

        /**
         * What the user sees: the model's private <think> section removed, and any slip where
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
    }
}
