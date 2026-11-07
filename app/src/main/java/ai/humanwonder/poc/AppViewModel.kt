package ai.humanwonder.poc

import ai.humanwonder.poc.ai.ModelManager
import ai.humanwonder.poc.ai.Persona
import ai.humanwonder.poc.data.local.entities.Conversation
import ai.humanwonder.poc.data.repository.ChatRepository
import ai.humanwonder.poc.data.repository.Profile
import ai.humanwonder.poc.data.repository.SettingsRepository
import ai.humanwonder.poc.utils.FileUtils
import android.app.Application
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** Maps the model's state to what the person sees (Aligning n% → Waking → Ready → Failed). */
fun ModelManager.ModelState.toSky(): SkyState = when (this) {
    is ModelManager.ModelState.Copying -> SkyState.Aligning(progress)
    ModelManager.ModelState.Loading -> SkyState.Waking
    ModelManager.ModelState.Ready -> SkyState.Ready
    is ModelManager.ModelState.Error -> SkyState.Failed(Persona.lostSignalMessage(cause))
}

/**
 * Activity-wide state: the profile, the sky waking up, the conversation list for the side menu
 * and Home, onboarding, and the Profile & settings actions.
 */
@HiltViewModel
class AppViewModel @Inject constructor(
    application: Application,
    private val settings: SettingsRepository,
    private val chatRepository: ChatRepository,
    private val modelManager: ModelManager,
) : AndroidViewModel(application) {

    val profile: StateFlow<Profile> = settings.profile

    val sky: StateFlow<SkyState> = modelManager.modelState
        .map { it.toSky() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, modelManager.modelState.value.toSky())

    /** Every conversation, newest first (Home shows the first three). */
    val conversations: StateFlow<List<Conversation>> = chatRepository.getAllConversations()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    /** The side menu's list: everything, or the matches for [searchQuery]. */
    private val _drawerConversations = MutableStateFlow<List<Conversation>>(emptyList())
    val drawerConversations: StateFlow<List<Conversation>> = _drawerConversations.asStateFlow()

    init {
        // First launch: start unpacking the sky while the person reads the welcome screens.
        wakeSky()
        viewModelScope.launch {
            _searchQuery.collectLatest { q ->
                val source = if (q.isBlank()) chatRepository.getAllConversations() else chatRepository.searchConversations(q.trim())
                source.collect { _drawerConversations.value = it }
            }
        }
    }

    val onboarded: Boolean get() = settings.profile.value.onboardingComplete

    private fun wakeSky() = modelManager.wake {
        val p = settings.profile.value
        Persona.systemPrompt(p.name, p.city)
    }

    fun retry() = wakeSky()

    // ------------------------------------------------------------------ onboarding

    /** "Meet Orbit 412": remember the name; the persona picks it up on the next question. */
    fun completeOnboarding(rawName: String) {
        val name = rawName.trim().take(40).ifBlank { "Wanderer" }
        settings.update { it.copy(name = name, onboardingComplete = true) }
    }

    // ------------------------------------------------------------------ profile (auto-saved)

    fun setName(value: String) = settings.update { it.copy(name = value.take(40)) }

    /** Saved only when blank or a valid address; the screen shows why otherwise. */
    fun setEmail(value: String) {
        val email = value.trim().take(80)
        if (email.isEmpty() || isValidEmail(email)) settings.update { it.copy(email = email) }
    }

    fun setCity(value: String) = settings.update { it.copy(city = value.take(60)) }

    fun setPerspective(p: Perspective) = settings.update { it.copy(perspective = p.name) }

    fun setTone(t: Tone) = settings.update { it.copy(tone = t.name) }

    fun setReadAloud(on: Boolean) = settings.update { it.copy(readAloud = on) }

    fun setHaptics(on: Boolean) = settings.update { it.copy(haptics = on) }

    // ------------------------------------------------------------------ conversations

    fun setSearchQuery(q: String) {
        _searchQuery.value = q.take(80)
    }

    fun renameConversation(id: String, title: String) {
        val clean = title.replace(Regex("\\s+"), " ").trim().take(60)
        if (clean.isEmpty()) return
        viewModelScope.launch { chatRepository.renameConversation(id, clean) }
    }

    fun deleteConversation(id: String) {
        viewModelScope.launch { chatRepository.deleteConversation(id) }
    }

    fun clearAllConversations() {
        viewModelScope.launch {
            chatRepository.deleteAllConversations()
            toast("All transmissions cleared.")
        }
    }

    /** Writes every conversation as JSON to the file the person picked (system file picker). */
    fun exportTo(uri: Uri) {
        viewModelScope.launch {
            val ok = try {
                val json = chatRepository.exportAllToJson()
                withContext(Dispatchers.IO) { FileUtils.writeTextToUri(getApplication<Application>(), uri, json) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("HumanWonder", "Export failed", e)
                false
            }
            toast(if (ok) "Transmissions saved." else "Couldn't save your transmissions. Try again.")
        }
    }

    private fun toast(text: String) {
        Toast.makeText(getApplication<Application>(), text, Toast.LENGTH_SHORT).show()
    }

    companion object {
        fun isValidEmail(value: String): Boolean = android.util.Patterns.EMAIL_ADDRESS.matcher(value).matches()
    }
}
