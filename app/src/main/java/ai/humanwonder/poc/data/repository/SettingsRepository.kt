package ai.humanwonder.poc.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** The person's Human Wonder profile and preferences. Everything stays on the phone. */
data class Profile(
    val name: String = "",
    val email: String = "",
    val city: String = "",
    /** Default point of view for new chats: a [ai.humanwonder.poc.Perspective] name. */
    val perspective: String = "Orbit",
    /** Default tone for new chats: a [ai.humanwonder.poc.Tone] name. */
    val tone: String = "Balanced",
    val readAloud: Boolean = false,
    val haptics: Boolean = true,
    val onboardingComplete: Boolean = false,
)

/**
 * Settings storage, ported from OfflineLLM's SettingsRepository (encrypted SharedPreferences).
 *
 * Human Wonder changes: only the profile is stored (no sampling, GPU, theme, font, lock or
 * translator settings), it is exposed as a [StateFlow] so the persona follows a name change, and
 * v1's plain "human_wonder" preferences (name, onboarded) are carried over on first run.
 */
@Singleton
class SettingsRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = openPrefs()

    private val _profile = MutableStateFlow(migrateV1(read()))
    val profile: StateFlow<Profile> = _profile.asStateFlow()

    /** Applies [transform] to the profile and saves it at once (auto-save). */
    fun update(transform: (Profile) -> Profile) {
        val next = transform(_profile.value)
        if (next == _profile.value) return
        write(next)
        _profile.value = next
    }

    private fun openPrefs(): SharedPreferences = try {
        // The master key is created in the Android Keystore on first use.
        MasterKey.Builder(context, MASTER_KEY_ALIAS)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            SECURE_PREFS,
            MASTER_KEY_ALIAS,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        // A broken keystore must not keep the person out: fall back to private plain storage.
        Log.w("HumanWonder", "Encrypted settings unavailable, using private storage", e)
        context.getSharedPreferences(FALLBACK_PREFS, Context.MODE_PRIVATE)
    }

    private fun read() = Profile(
        name = prefs.getString(KEY_NAME, "") ?: "",
        email = prefs.getString(KEY_EMAIL, "") ?: "",
        city = prefs.getString(KEY_CITY, "") ?: "",
        perspective = prefs.getString(KEY_PERSPECTIVE, "Orbit") ?: "Orbit",
        tone = prefs.getString(KEY_TONE, "Balanced") ?: "Balanced",
        readAloud = prefs.getBoolean(KEY_READ_ALOUD, false),
        haptics = prefs.getBoolean(KEY_HAPTICS, true),
        onboardingComplete = prefs.getBoolean(KEY_ONBOARDING_COMPLETE, false),
    )

    private fun write(p: Profile) {
        prefs.edit()
            .putString(KEY_NAME, p.name)
            .putString(KEY_EMAIL, p.email)
            .putString(KEY_CITY, p.city)
            .putString(KEY_PERSPECTIVE, p.perspective)
            .putString(KEY_TONE, p.tone)
            .putBoolean(KEY_READ_ALOUD, p.readAloud)
            .putBoolean(KEY_HAPTICS, p.haptics)
            .putBoolean(KEY_ONBOARDING_COMPLETE, p.onboardingComplete)
            .apply()
    }

    /** v1 kept the name and the onboarded flag in plain preferences; bring them over once. */
    private fun migrateV1(current: Profile): Profile {
        if (prefs.getBoolean(KEY_MIGRATED_V1, false)) return current
        val v1 = context.getSharedPreferences(V1_PREFS, Context.MODE_PRIVATE)
        val migrated = current.copy(
            name = current.name.ifBlank { v1.getString("name", "").orEmpty() },
            onboardingComplete = current.onboardingComplete || v1.getBoolean("onboarded", false),
        )
        write(migrated)
        prefs.edit().putBoolean(KEY_MIGRATED_V1, true).apply()
        return migrated
    }

    companion object {
        private const val SECURE_PREFS = "humanwonder_secure_prefs"
        private const val FALLBACK_PREFS = "humanwonder_settings"
        private const val V1_PREFS = "human_wonder"
        private const val MASTER_KEY_ALIAS = "humanwonder_secure_prefs_master_key"

        const val KEY_NAME = "name"
        const val KEY_EMAIL = "email"
        const val KEY_CITY = "city"
        const val KEY_PERSPECTIVE = "perspective"
        const val KEY_TONE = "tone"
        const val KEY_READ_ALOUD = "read_aloud"
        const val KEY_HAPTICS = "haptics"
        const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
        private const val KEY_MIGRATED_V1 = "migrated_v1"
    }
}
