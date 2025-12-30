package com.kufay.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

// Create a DataStore instance at the top level
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "kufay_settings")

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Keys for our preferences
    object PreferencesKeys {
        val AUTO_READ_ENABLED = booleanPreferencesKey("auto_read_enabled")
        val TTS_LANGUAGE = stringPreferencesKey("tts_language")
        val APP_LANGUAGE = stringPreferencesKey("app_language") // New key for app language
        val TTS_VOICE_GENDER = stringPreferencesKey("tts_voice_gender")
        val TTS_SPEECH_RATE = floatPreferencesKey("tts_speech_rate")
        val TTS_SPEECH_PITCH = floatPreferencesKey("tts_speech_pitch")
        val AUTO_DELETE_DAYS = intPreferencesKey("auto_delete_days")
        val APP_MAIN_COLOR = stringPreferencesKey("app_main_color")
        val USE_WOLOF_RECORDINGS = booleanPreferencesKey("use_wolof_recordings") // New key for Wolof recordings
        // Add these to the PreferencesKeys object along with your other keys
        val PIN_ENABLED = booleanPreferencesKey("pin_enabled")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        // Add the onboarding completed key
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    // AUTO READ
    val autoReadEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.AUTO_READ_ENABLED] ?: true
    }

    suspend fun setAutoReadEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_READ_ENABLED] = enabled
        }
    }

    // APP LANGUAGE (New)
    val appLanguage: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.APP_LANGUAGE] ?: "fr"
    }

    suspend fun setAppLanguage(language: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.APP_LANGUAGE] = language
        }
    }

    // TTS LANGUAGE
    val ttsLanguage: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.TTS_LANGUAGE] ?: "fr"
    }

    suspend fun setTtsLanguage(language: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TTS_LANGUAGE] = language
        }
    }

    // WOLOF RECORDINGS TOGGLE (New)
    val useWolofRecordings: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.USE_WOLOF_RECORDINGS] ?: false
    }

    suspend fun setUseWolofRecordings(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USE_WOLOF_RECORDINGS] = enabled
        }
    }

    // TTS VOICE GENDER
    val ttsVoiceGender: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.TTS_VOICE_GENDER] ?: "female"
    }

    suspend fun setTtsVoiceGender(gender: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TTS_VOICE_GENDER] = gender
        }
    }

    // TTS SPEECH RATE
    val ttsSpeechRate: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.TTS_SPEECH_RATE] ?: 1.0f
    }

    suspend fun setTtsSpeechRate(rate: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TTS_SPEECH_RATE] = rate
        }
    }

    // TTS SPEECH PITCH
    val ttsSpeechPitch: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.TTS_SPEECH_PITCH] ?: 1.0f
    }

    suspend fun setTtsSpeechPitch(pitch: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TTS_SPEECH_PITCH] = pitch
        }
    }

    // AUTO DELETE DAYS
    val autoDeleteDays: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.AUTO_DELETE_DAYS] ?: 30
    }

    suspend fun setAutoDeleteDays(days: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_DELETE_DAYS] = days
        }
    }

    // APP MAIN COLOR
    val appMainColor: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.APP_MAIN_COLOR] ?: "#006400" // Default to green
    }

    suspend fun setAppMainColor(colorHex: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.APP_MAIN_COLOR] = colorHex
        }
    }

    // Add these functions at the end of the UserPreferences class
// PIN ENABLED
    val pinEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.PIN_ENABLED] ?: false
    }

    suspend fun setPinEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PIN_ENABLED] = enabled
        }
    }

    suspend fun setPin(pin: String) {
        // Simple hash for storing the PIN
        val hashedPin = pin.hashCode().toString()
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PIN_HASH] = hashedPin
            preferences[PreferencesKeys.PIN_ENABLED] = true
        }
    }

    // Then update the verifyPin function
    suspend fun verifyPin(pin: String): Boolean {
        val hashedPin = pin.hashCode().toString()
        val preferences = context.dataStore.data.first()
        val storedPin = preferences[PreferencesKeys.PIN_HASH] ?: ""
        return hashedPin == storedPin
    }

    suspend fun clearPin() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PIN_ENABLED] = false
            preferences.remove(PreferencesKeys.PIN_HASH)
        }
    }

    // ONBOARDING COMPLETED (new)
    fun hasCompletedOnboarding(): Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.ONBOARDING_COMPLETED] ?: false
    }

    suspend fun setHasCompletedOnboarding(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] = completed
        }
    }
}
