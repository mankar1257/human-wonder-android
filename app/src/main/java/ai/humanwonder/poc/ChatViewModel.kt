package ai.humanwonder.poc

import ai.humanwonder.poc.ai.InferenceEngine
import ai.humanwonder.poc.ai.ModelManager
import ai.humanwonder.poc.ai.Persona
import ai.humanwonder.poc.data.local.entities.Message
import ai.humanwonder.poc.data.repository.ChatRepository
import ai.humanwonder.poc.data.repository.SettingsRepository
import ai.humanwonder.poc.utils.SecurityUtils
import ai.humanwonder.poc.utils.TtsHelper
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * One chat with Orbit 412 (ported from OfflineLLM's ChatViewModel, with v1's persona rules).
 * Messages live in Room; the question and the streaming answer are shown at once and handed
 * over to Room under the same ids, so nothing flickers or animates twice.
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    application: Application,
    private val savedStateHandle: SavedStateHandle,
    private val chatRepository: ChatRepository,
    private val settings: SettingsRepository,
    private val modelManager: ModelManager,
    private val inferenceEngine: InferenceEngine,
) : AndroidViewModel(application) {

    private data class Local(
        val perspective: Perspective,
        val tone: Tone,
        val generating: Boolean = false,
    )

    private val conversationId = MutableStateFlow(savedStateHandle.get<String>(ARG_CONVERSATION))
    private val stored = MutableStateFlow<List<Message>>(emptyList())
    /** Lines not yet in Room: the question just sent and the answer streaming in (or an error). */
    private val pending = MutableStateFlow<List<ChatMessage>>(emptyList())
    private val local = MutableStateFlow(
        settings.profile.value.let { Local(Perspective.from(it.perspective), Tone.from(it.tone)) },
    )

    val ui: StateFlow<UiState> = combine(
        conversationId, stored, pending, local, settings.profile,
    ) { id, rows, waiting, l, profile ->
        // Earlier turns from Room, then this session's lines in the order they were sent (they are
        // saved to Room under the same ids, so each line is shown once and never re-animates).
        val sessionIds = waiting.mapTo(HashSet()) { it.id }
        val messages = rows.filter { it.id !in sessionIds }
            .map { ChatMessage(it.id, it.role == "user", it.content, complete = true, time = it.timestamp) } +
            waiting
        UiState(
            name = profile.name,
            conversationId = id,
            messages = messages,
            generating = l.generating,
            perspective = l.perspective,
            tone = l.tone,
            haptics = profile.haptics,
        )
    }.combine(modelManager.modelState) { state, model -> state.copy(sky = model.toSky()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, UiState(sky = modelManager.modelState.value.toSky()))

    /** False until this screen has put its conversation into the native context once. */
    private var primed = false
    /** The answer being received now; a late callback from an earlier one must not end it. */
    private var activeAnswerId: String? = null
    private var tts: TtsHelper? = null

    init {
        viewModelScope.launch {
            conversationId.collectLatest { id ->
                if (id == null) {
                    stored.value = emptyList()
                } else {
                    chatRepository.getMessagesForConversation(id).collect { stored.value = it }
                }
            }
        }
        // The speech engine takes a moment to start, so start it as soon as reading aloud is on.
        viewModelScope.launch {
            settings.profile.map { it.readAloud }.distinctUntilChanged().collect { on ->
                if (on && tts == null) tts = TtsHelper(getApplication<Application>())
                if (!on) tts?.stop()
            }
        }
        // A suggestion from Home: ask it as soon as the sky is ready (once, even across rotation).
        val question = savedStateHandle.get<String>(ARG_QUESTION)
        if (!question.isNullOrBlank() && savedStateHandle.get<Boolean>(KEY_ASKED) != true) {
            savedStateHandle[KEY_ASKED] = true
            viewModelScope.launch {
                try {
                    modelManager.awaitReady()
                    send(question)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Throwable) {
                    // The screen shows the lost signal and its retry.
                }
            }
        }
    }

    fun setPerspective(p: Perspective) = local.update { it.copy(perspective = p) }

    fun cycleTone() = local.update { it.copy(tone = Tone.entries[(it.tone.ordinal + 1) % Tone.entries.size]) }

    fun send(text: String) {
        val question = SecurityUtils.sanitizePrompt(text)
        // Read the model state directly: the combined [ui] flow may lag one emission behind.
        val ready = modelManager.modelState.value == ModelManager.ModelState.Ready
        if (question.isEmpty() || local.value.generating || !ready) return

        tts?.stop()
        val userId = UUID.randomUUID().toString()
        val answerId = UUID.randomUUID().toString()
        pending.update {
            it + ChatMessage(userId, fromUser = true, text = question) +
                ChatMessage(answerId, fromUser = false, text = "", complete = false)
        }
        local.update { it.copy(generating = true) }
        activeAnswerId = answerId
        val prompt = Persona.userPrompt(question, local.value.perspective, local.value.tone)

        viewModelScope.launch {
            try {
                val existing = conversationId.value?.let { chatRepository.getConversation(it) }
                val id = existing?.id ?: chatRepository.createConversation(Persona.titleFor(question)).id
                if (existing == null) {
                    conversationId.value = id
                    savedStateHandle[ARG_CONVERSATION] = id
                    primed = false
                }
                chatRepository.addMessage(id, "user", question, id = userId)
                prepare(id, force = !primed, tight = false, skipId = userId)
                primed = true
                generate(id, prompt, answerId, retried = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                Log.e(TAG, "Sending failed", e)
                failAnswer(answerId)
            }
        }
    }

    private suspend fun prepare(id: String, force: Boolean, tight: Boolean, skipId: String) {
        val p = settings.profile.value
        modelManager.prepareChat(id, Persona.systemPrompt(p.name, p.city), force = force, tight = tight) {
            // Earlier turns only: the question being asked now is added by the engine itself.
            chatRepository.getMessagesSync(id).filter { it.id != skipId }.map { it.role to it.content }
        }
    }

    private fun generate(id: String, prompt: String, answerId: String, retried: Boolean) {
        inferenceEngine.generateResponse(
            query = prompt,
            maxTokens = MAX_NEW_TOKENS,
            onToken = { partial -> setAnswer(answerId, Persona.visibleAnswer(partial), complete = false) },
            onComplete = { result -> finishAnswer(id, answerId, Persona.visibleAnswer(result.response)) },
            onCancelled = { finishAnswer(id, answerId, currentText(answerId)) },
            onError = { e ->
                val partial = currentText(answerId)
                when {
                    // The conversation outgrew the context mid-answer: keep what arrived.
                    InferenceEngine.isContextFull(e) && partial.isNotBlank() -> finishAnswer(id, answerId, partial)
                    // It did not fit at all: start the context again from the newest turns, once.
                    InferenceEngine.isContextFull(e) && !retried -> viewModelScope.launch {
                        try {
                            prepare(id, force = true, tight = true, skipId = userIdBefore(answerId))
                            generate(id, prompt, answerId, retried = true)
                        } catch (c: CancellationException) {
                            throw c
                        } catch (t: Throwable) {
                            Log.e(TAG, "Retry failed", t)
                            failAnswer(answerId)
                        }
                    }
                    else -> {
                        Log.e(TAG, "Generation failed", e)
                        failAnswer(answerId)
                    }
                }
            },
        )
    }

    /** The question that [answerId] answers (the line just before it). */
    private fun userIdBefore(answerId: String): String {
        val list = pending.value
        val i = list.indexOfFirst { it.id == answerId }
        return if (i > 0) list[i - 1].id else ""
    }

    private fun currentText(answerId: String): String =
        pending.value.firstOrNull { it.id == answerId }?.text.orEmpty()

    private fun setAnswer(answerId: String, text: String, complete: Boolean) {
        pending.update { list -> list.map { m -> if (m.id == answerId) m.copy(text = text, complete = complete) else m } }
    }

    private fun endTurn(answerId: String) {
        if (answerId != activeAnswerId) return
        activeAnswerId = null
        local.update { it.copy(generating = false) }
    }

    private fun finishAnswer(conversation: String, answerId: String, text: String) {
        val current = answerId == activeAnswerId
        endTurn(answerId)
        if (text.isBlank()) {
            setAnswer(answerId, "…", complete = true)
            return
        }
        setAnswer(answerId, text, complete = true)
        viewModelScope.launch {
            try {
                chatRepository.addMessage(conversation, "assistant", text, id = answerId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                Log.e(TAG, "Saving the answer failed", e)
            }
        }
        if (current && settings.profile.value.readAloud) tts?.speak(text)
    }

    private fun failAnswer(answerId: String) {
        endTurn(answerId)
        setAnswer(answerId, Persona.FADED_SIGNAL, complete = true)
    }

    fun stop() {
        inferenceEngine.stopGeneration()
    }

    /** "+" in the header: an empty sky again (the old conversation stays in the side menu). */
    fun newConversation() {
        if (local.value.generating) inferenceEngine.stopGeneration()
        tts?.stop()
        activeAnswerId = null
        local.update { it.copy(generating = false) }
        pending.value = emptyList()
        conversationId.value = null
        savedStateHandle.set<String?>(ARG_CONVERSATION, null)
        primed = false
    }

    /** A conversation picked in the side menu while this chat is open. */
    fun openConversation(id: String) {
        if (id == conversationId.value) return
        newConversation()
        conversationId.value = id
        savedStateHandle[ARG_CONVERSATION] = id
    }

    override fun onCleared() {
        super.onCleared()
        if (local.value.generating) inferenceEngine.stopGeneration()
        tts?.shutdown()
    }

    companion object {
        private const val TAG = "HumanWonder"
        private const val MAX_NEW_TOKENS = 384
        const val ARG_CONVERSATION = "conversationId"
        const val ARG_QUESTION = "question"
        private const val KEY_ASKED = "question_asked"
    }
}
