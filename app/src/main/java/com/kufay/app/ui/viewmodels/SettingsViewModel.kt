package com.kufay.app.ui.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kufay.app.data.preferences.UserPreferences
import com.kufay.app.service.TTSService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
// Add these imports to your SettingsViewModel.kt if not already present
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val ttsService: TTSService,
    @ApplicationContext private val context: Context // Added context properly
) : ViewModel() {

    // App Language (New)
    val appLanguage = userPreferences.appLanguage
        .stateIn(viewModelScope, SharingStarted.Eagerly, "fr")

    // TTS Settings
    val autoReadEnabled = userPreferences.autoReadEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val ttsLanguage = userPreferences.ttsLanguage
        .stateIn(viewModelScope, SharingStarted.Eagerly, "fr")

    // Use Wolof recordings (New)
    val useWolofRecordings = userPreferences.useWolofRecordings
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val ttsVoiceGender = userPreferences.ttsVoiceGender
        .stateIn(viewModelScope, SharingStarted.Eagerly, "female")

    val ttsSpeechRate = userPreferences.ttsSpeechRate
        .stateIn(viewModelScope, SharingStarted.Eagerly, 1.0f)

    val ttsSpeechPitch = userPreferences.ttsSpeechPitch
        .stateIn(viewModelScope, SharingStarted.Eagerly, 1.0f)

    // Trash Settings
    val autoDeleteDays = userPreferences.autoDeleteDays
        .stateIn(viewModelScope, SharingStarted.Eagerly, 30)

    // App Color Setting
    val appMainColor = userPreferences.appMainColor
        .stateIn(viewModelScope, SharingStarted.Eagerly, "#006400")

    // New method for app language
    fun updateAppLanguage(language: String) {
        viewModelScope.launch {
            userPreferences.setAppLanguage(language)
        }
    }

    fun updateAutoReadEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setAutoReadEnabled(enabled)
        }
    }

    fun updateTtsLanguage(language: String) {
        viewModelScope.launch {
            userPreferences.setTtsLanguage(language)

            // Also update the SharedPreferences directly for immediate effect
            context.getSharedPreferences("kufay_preferences", Context.MODE_PRIVATE).edit()
                .putString("tts_language", language)
                .apply()

            // Refresh TTS engine to apply changes immediately
            ttsService.refreshTtsSettings()

            // Log for debugging
            Log.d("KUFAY_SETTINGS", "Updated TTS language to: $language")
        }
    }

    // New method for Wolof recordings
    fun updateUseWolofRecordings(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setUseWolofRecordings(enabled)
        }
    }

    fun updateTtsVoiceGender(gender: String) {
        viewModelScope.launch {
            userPreferences.setTtsVoiceGender(gender)

            // Also update the SharedPreferences directly for immediate effect
            context.getSharedPreferences("kufay_preferences", Context.MODE_PRIVATE).edit()
                .putString("tts_voice_gender", gender)
                .apply()

            // Refresh TTS engine to apply changes immediately
            ttsService.refreshTtsSettings()

            // Log for debugging
            Log.d("KUFAY_SETTINGS", "Updated voice gender to: $gender")
        }
    }

    fun updateTtsSpeechRate(rate: Float) {
        viewModelScope.launch {
            userPreferences.setTtsSpeechRate(rate)

            // Also update the SharedPreferences directly for immediate effect
            context.getSharedPreferences("kufay_preferences", Context.MODE_PRIVATE).edit()
                .putFloat("tts_speech_rate", rate)
                .apply()

            // Refresh TTS engine to apply changes immediately
            ttsService.refreshTtsSettings()

            // Log for debugging
            Log.d("KUFAY_SETTINGS", "Updated speech rate to: $rate")
        }
    }

    fun updateTtsSpeechPitch(pitch: Float) {
        viewModelScope.launch {
            userPreferences.setTtsSpeechPitch(pitch)

            // Also update the SharedPreferences directly for immediate effect
            context.getSharedPreferences("kufay_preferences", Context.MODE_PRIVATE).edit()
                .putFloat("tts_speech_pitch", pitch)
                .apply()

            // Refresh TTS engine to apply changes immediately
            ttsService.refreshTtsSettings()

            // Log for debugging
            Log.d("KUFAY_SETTINGS", "Updated speech pitch to: $pitch")
        }
    }

    fun updateAutoDeleteDays(days: Int) {
        viewModelScope.launch {
            userPreferences.setAutoDeleteDays(days)
        }
    }

    fun updateAppMainColor(colorHex: String) {
        viewModelScope.launch {
            userPreferences.setAppMainColor(colorHex)
        }
    }

    // Test method to explicitly test the current settings
    fun testTtsSettings() {
        // Log current settings before testing
        val rate = context.getSharedPreferences("kufay_preferences", Context.MODE_PRIVATE)
            .getFloat("tts_speech_rate", 1.0f)
        val pitch = context.getSharedPreferences("kufay_preferences", Context.MODE_PRIVATE)
            .getFloat("tts_speech_pitch", 1.0f)
        val gender = context.getSharedPreferences("kufay_preferences", Context.MODE_PRIVATE)
            .getString("tts_voice_gender", "female")

        Log.d("KUFAY_SETTINGS", "Testing TTS with settings - Rate: $rate, Pitch: $pitch, Gender: $gender")

        // Call the test method
        ttsService.testCurrentSettings()
    }

    // Dans SettingsViewModel.kt
    fun testWolofFiles() {
        Log.e("KUFAY_SETTINGS", "Testing Wolof files")
        ttsService.testWolofMedia()
    }

    // PIN protection settings
    val isPinProtectionEnabled = userPreferences.pinEnabled
    private val _navigateToPinSetup = MutableStateFlow(false)
    val navigateToPinSetup: StateFlow<Boolean> = _navigateToPinSetup.asStateFlow()

    // Function to clear the PIN
    fun clearPin() {
        viewModelScope.launch {
            userPreferences.clearPin()
        }
    }

    // Function to trigger navigation to PIN setup
    fun navigateToPinSetup() {
        _navigateToPinSetup.value = true
    }

    // Function to reset navigation trigger after navigation is performed
    fun resetNavigateToPinSetup() {
        _navigateToPinSetup.value = false
    }

    // In SettingsViewModel.kt
    private val _pinVerificationRequired = MutableStateFlow(false)
    val pinVerificationRequired: StateFlow<Boolean> = _pinVerificationRequired.asStateFlow()

    private val _pinVerificationType = MutableStateFlow<PinVerificationType>(PinVerificationType.NONE)
    val pinVerificationType: StateFlow<PinVerificationType> = _pinVerificationType.asStateFlow()

    enum class PinVerificationType {
        NONE,
        DISABLE_PIN,
        CHANGE_PIN
    }

    // Function to request PIN verification before disabling
    fun requestDisablePin() {
        _pinVerificationRequired.value = true
        _pinVerificationType.value = PinVerificationType.DISABLE_PIN
    }

    // Function to request PIN verification before changing
    fun requestChangePin() {
        _pinVerificationRequired.value = true
        _pinVerificationType.value = PinVerificationType.CHANGE_PIN
    }

    // Function called after successful PIN verification
    fun onPinVerified() {
        when (_pinVerificationType.value) {
            PinVerificationType.DISABLE_PIN -> {
                viewModelScope.launch {
                    userPreferences.clearPin()
                }
            }
            PinVerificationType.CHANGE_PIN -> {
                navigateToPinSetup()
            }
            else -> {}
        }

        // Reset verification state
        _pinVerificationRequired.value = false
        _pinVerificationType.value = PinVerificationType.NONE
    }

    // In SettingsViewModel
    fun cancelPinVerification() {
        _pinVerificationRequired.value = false
        _pinVerificationType.value = PinVerificationType.NONE
    }

    fun getUserPreferences(): UserPreferences {
        return userPreferences
    }


}