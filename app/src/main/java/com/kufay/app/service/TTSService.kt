package com.kufay.app.service

import android.content.Context
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.util.Log
import com.kufay.app.R
import com.kufay.app.data.db.entities.Notification
import com.kufay.app.data.preferences.UserPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withContext

@Singleton
class TTSService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferences: UserPreferences
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private val prefs = context.getSharedPreferences("kufay_preferences", Context.MODE_PRIVATE)
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    init {
        tts = TextToSpeech(context, this)
        // Subscribe to preference changes
        serviceScope.launch {
            observePreferences()
        }
    }

    // New method to observe preference changes
    private suspend fun observePreferences() {
        // Observe speech rate changes
        userPreferences.ttsSpeechRate.collect { newRate ->
            Log.d("KUFAY_TTS", "Speech rate changed to: $newRate")
            tts?.setSpeechRate(newRate)
        }

        // Observe speech pitch changes
        userPreferences.ttsSpeechPitch.collect { newPitch ->
            Log.d("KUFAY_TTS", "Speech pitch changed to: $newPitch")
            tts?.setPitch(newPitch)
        }

        // Observe voice gender changes
        userPreferences.ttsVoiceGender.collect { newGender ->
            Log.d("KUFAY_TTS", "Voice gender changed to: $newGender")
            setPreferredVoice()
        }

        // Observe language changes
        userPreferences.ttsLanguage.collect { newLanguage ->
            Log.d("KUFAY_TTS", "Language changed to: $newLanguage")
            setLanguage(newLanguage)
        }
    }

    // New method to set language
    private fun setLanguage(languageCode: String) {
        val locale = when (languageCode) {
            "fr" -> Locale.FRENCH
            "wo" -> Locale.FRENCH // Default to French for Wolof TTS parts
            else -> Locale.FRENCH
        }

        val result = tts?.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA ||
            result == TextToSpeech.LANG_NOT_SUPPORTED
        ) {
            // Fallback to English if preferred language is not available
            tts?.setLanguage(Locale.ENGLISH)
        }
    }

    // Mapping of notification types to Wolof audio recording filenames
    // Dans TTSService.kt
    private val WOLOF_RECORDINGS = mapOf(
        "wave_business_payment" to R.raw.wolof_wave_business_payment,
        "wave_business_encaissement" to R.raw.wolof_wave_business_encaissement,
        "wave_business_distance" to R.raw.wolof_wave_business_distance,
        "orange_money_received" to R.raw.wolof_orange_money_received,
        "orange_money_general" to R.raw.wolof_orange_money_general,
        "mixx_received" to R.raw.wolof_mixx_received,
        "mixx_sent" to R.raw.wolof_mixx_sent,
        "wave_personal_payment" to R.raw.wolof_wave_personal_payment,
        "wave_personal_sent" to R.raw.wolof_wave_personal_sent,
        "wave_personal_received" to R.raw.wolof_wave_personal_received
    )

    init {
        tts = TextToSpeech(context, this)
    }

    /**
     * Refreshes TTS settings from preferences
     * Called when settings are changed to apply them immediately
     */
    // In TTSService.kt

// Modify the refreshTtsSettings method to properly apply settings
    // In TTSService.kt

// Make sure we immediately apply settings on init
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            Log.d("KUFAY_TTS", "TTS Engine initialized successfully")

            // Get initial settings from DataStore in a blocking way (just for init)
            val initialLanguage = runBlocking { userPreferences.ttsLanguage.first() }
            val initialRate = runBlocking { userPreferences.ttsSpeechRate.first() }
            val initialPitch = runBlocking { userPreferences.ttsSpeechPitch.first() }

            // Apply initial settings
            setLanguage(initialLanguage)
            tts?.setSpeechRate(initialRate)
            tts?.setPitch(initialPitch)
            setPreferredVoice()

            Log.d(
                "KUFAY_TTS", "Applied initial settings - Language: $initialLanguage, " +
                        "Rate: $initialRate, Pitch: $initialPitch"
            )
        } else {
            Log.e("KUFAY_TTS", "TTS Engine initialization failed with status: $status")
        }
    }

    // Create a new comprehensive method to apply all settings
    private fun applyAllTtsSettings() {
        // 1. Apply language
        val languageCode = prefs.getString("tts_language", "fr")
        Log.d("KUFAY_TTS", "Setting language to: $languageCode")

        val locale = when (languageCode) {
            "fr" -> Locale.FRENCH
            "wo" -> Locale.FRENCH // Default to French for Wolof TTS parts
            else -> Locale.FRENCH
        }

        val result = tts?.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w("KUFAY_TTS", "Language $languageCode not supported, falling back to English")
            tts?.setLanguage(Locale.ENGLISH)
        }

        // 2. Apply speech rate - be explicit with the value we're getting
        val speechRateStr = prefs.getFloat("tts_speech_rate", 1.0f).toString()
        val speechRate = speechRateStr.toFloatOrNull() ?: 1.0f
        Log.d("KUFAY_TTS", "Setting speech rate to: $speechRate")
        tts?.setSpeechRate(speechRate)

        // 3. Apply pitch - be explicit with the value we're getting
        val speechPitchStr = prefs.getFloat("tts_speech_pitch", 1.0f).toString()
        val speechPitch = speechPitchStr.toFloatOrNull() ?: 1.0f
        Log.d("KUFAY_TTS", "Setting speech pitch to: $speechPitch")
        tts?.setPitch(speechPitch)

        // 4. Try to set voice if available
        try {
            setPreferredVoice()
        } catch (e: Exception) {
            Log.e("KUFAY_TTS", "Error setting voice: ${e.message}")
        }

        // Log current TTS settings for debugging
        Log.d(
            "KUFAY_TTS", "Current TTS settings - Language: ${tts?.voice?.locale}, " +
                    "Speech Rate: $speechRate, Speech Pitch: $speechPitch"
        )
    }

    /**
     * Refreshes TTS settings from preferences
     * Called when settings are changed to apply them immediately
     */
    fun refreshTtsSettings() {
        serviceScope.launch {
            // Get current settings from DataStore
            val language = userPreferences.ttsLanguage.first()
            val rate = userPreferences.ttsSpeechRate.first()
            val pitch = userPreferences.ttsSpeechPitch.first()

            // Apply all settings
            withContext(Dispatchers.Main) {
                setLanguage(language)
                tts?.setSpeechRate(rate)
                tts?.setPitch(pitch)
                setPreferredVoice()

                Log.d(
                    "KUFAY_TTS", "Refreshed settings - Language: $language, " +
                            "Rate: $rate, Pitch: $pitch"
                )
            }
        }
    }

    // Update setPreferredVoice to be simpler and more reliable
    private fun setPreferredVoice() {
        val preferredGender = prefs.getString("tts_voice_gender", "female")
        Log.d("KUFAY_TTS", "Setting preferred voice gender: $preferredGender")

        val voices = tts?.voices ?: return
        if (voices.isEmpty()) {
            Log.w("KUFAY_TTS", "No voices available for TTS")
            return
        }

        // Current language
        val currentLocale = tts?.voice?.locale ?: Locale.FRENCH

        // First filter by language
        val languageVoices = voices.filter { it.locale.language == currentLocale.language }
        if (languageVoices.isEmpty()) {
            Log.w("KUFAY_TTS", "No voices for language: ${currentLocale.language}")
            return
        }

        // Then try to find a voice with preferred gender
        val genderedVoices = languageVoices.filter { voice ->
            when (preferredGender) {
                "male" -> !voice.name.contains("female", ignoreCase = true)
                else -> voice.name.contains("female", ignoreCase = true)
            }
        }

        // Use gendered voice if available, otherwise use any voice in the right language
        val selectedVoice =
            if (genderedVoices.isNotEmpty()) genderedVoices.first() else languageVoices.first()
        Log.d(
            "KUFAY_TTS",
            "Selected voice: ${selectedVoice.name} for locale: ${selectedVoice.locale}"
        )

        tts?.voice = selectedVoice
    }

    // Add a test method that explicitly applies settings before testing
    fun testCurrentSettings() {
        serviceScope.launch {
            val language = userPreferences.ttsLanguage.first()
            val rate = userPreferences.ttsSpeechRate.first()
            val pitch = userPreferences.ttsSpeechPitch.first()

            Log.d(
                "KUFAY_TTS", "Testing TTS with settings - Language: $language, " +
                        "Rate: $rate, Pitch: $pitch"
            )

            val testText = when (language) {
                "fr" -> "Test de la synthèse vocale. Le taux est $rate."
                "wo" -> "Test bu waxx wolof"
                else -> "Text to speech test. The rate is $rate."
            }

            withContext(Dispatchers.Main) {
                // Ensure settings are applied before speaking
                setLanguage(language)
                tts?.setSpeechRate(rate)
                tts?.setPitch(pitch)

                // Speak test text
                tts?.speak(
                    testText, TextToSpeech.QUEUE_FLUSH, null,
                    "tts_test_${System.currentTimeMillis()}"
                )
            }
        }
    }

    fun speakNotification(notification: Notification, isRecognizedPattern: Boolean = true) {
        // Check if we should use Wolof recordings
        val useWolofRecordings = runBlocking {
            userPreferences.useWolofRecordings.first()
        }
        val ttsLanguage = prefs.getString("tts_language", "fr")

        Log.d(
            "KUFAY_TTS",
            "Speaking notification: ${notification.title}, isRecognized: $isRecognizedPattern"
        )
        Log.d("KUFAY_TTS", "TTS Language: $ttsLanguage, Use Wolof: $useWolofRecordings")

        // For Mixx by Yas, only auto-read notifications with "recu"
        if (notification.packageName == "com.google.android.apps.messaging" &&
            notification.title.contains("Mixx by Yas", ignoreCase = true) &&
            !notification.text.contains("recu", ignoreCase = true) &&
            !notification.text.contains("reçu", ignoreCase = true)
        ) {
            Log.d("KUFAY_TTS", "Skipping Mixx by Yas notification without 'recu'")
            return
        }

        // For Orange Money, only auto-read notifications with "Vous avez recu"
        if (notification.packageName == "com.google.android.apps.messaging" &&
            notification.title.contains("OrangeMoney", ignoreCase = true) &&
            !notification.text.contains("Vous avez recu", ignoreCase = true) &&
            !notification.text.contains("Vous avez reçu", ignoreCase = true)
        ) {
            Log.d("KUFAY_TTS", "Skipping Orange Money notification without 'Vous avez recu'")
            return
        }

        // Determine if we should use Wolof recordings
        if (ttsLanguage == "wo" && useWolofRecordings && isRecognizedPattern) {
            playWolofNotification(notification)
        } else if (isRecognizedPattern) {
            val textToSpeak = prepareTextToSpeak(notification)
            tts?.speak(textToSpeak, TextToSpeech.QUEUE_ADD, null, "notification_${notification.id}")
        } else {
            Log.d("KUFAY_TTS", "Skipping auto-read for unrecognized pattern")
        }
    }

    // Method to manually speak a notification (for user-initiated playback)
    fun manuallySpeak(notification: Notification) {
        val useWolofRecordings = runBlocking {
            userPreferences.useWolofRecordings.first()
        }
        val ttsLanguage = prefs.getString("tts_language", "fr")

        if (ttsLanguage == "wo" && useWolofRecordings) {
            playWolofNotification(notification)
        } else {
            val textToSpeak = prepareTextToSpeak(notification)
            tts?.speak(
                textToSpeak,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "manual_notification_${notification.id}"
            )
        }
    }

    // Method to play Wolof recordings for different notification types
    private fun playWolofNotification(notification: Notification) {
        when {
            // WAVE BUSINESS NOTIFICATIONS
            notification.packageName == "com.wave.business" &&
                    notification.title.contains("Paiement réussi", ignoreCase = true) -> {
                // Extract amount
                val paymentRegex =
                    """a payé\s+(\d+(?:[.,]\d+)?(?:F|))""".toRegex(RegexOption.IGNORE_CASE)
                val amount = paymentRegex.find(notification.text)?.groupValues?.getOrNull(1)
                    ?.replace("F", "")?.replace(".", "")?.replace(",", "")
                    ?: notification.amount?.toLong()?.toString() ?: ""

                // Extract username
                val usernameRegex = """DISTANCE reçu:\s*([^(]*)(?:\(|a)""".toRegex()
                var username =
                    usernameRegex.find(notification.text)?.groupValues?.getOrNull(1)?.trim() ?: ""

                if (username.contains("*")) {
                    username = formatUsername(username)
                }

                playWolofRecordingWithText("wave_business_payment", "$amount Franc C F A de $username")
            }

            // WAVE BUSINESS - Zéro frais (encaissement)
            notification.packageName == "com.wave.business" &&
                    notification.title.contains("Zéro frais", ignoreCase = true) -> {
                val encaissementRegex =
                    """sur votre encaissement de\s+(\d+(?:[.,]\d+)?)""".toRegex(RegexOption.IGNORE_CASE)
                val amount = encaissementRegex.find(notification.text)?.groupValues?.getOrNull(1)
                    ?.replace(".", "")?.replace(",", "") ?: notification.amount?.toLong()
                    ?.toString() ?: ""

                val usernameRegex =
                    """[dD]e\s+([^(]*)(?:\(|\s+le)""".toRegex(RegexOption.IGNORE_CASE)
                var username =
                    usernameRegex.find(notification.text)?.groupValues?.getOrNull(1)?.trim() ?: ""

                if (username.contains("*")) {
                    username = formatUsername(username)
                }

                playWolofRecordingWithText(
                    "wave_business_encaissement",
                    "$amount Franc C F A de $username"
                )
            }

            // WAVE BUSINESS - À DISTANCE reçu
            notification.packageName == "com.wave.business" &&
                    (notification.text.contains("À DISTANCE reçu", ignoreCase = true) ||
                            notification.text.contains("A DISTANCE reçu", ignoreCase = true)) -> {

                val paymentRegex =
                    """a payé\s+(\d+(?:[.,]\d+)?(?:F|))""".toRegex(RegexOption.IGNORE_CASE)
                val amount = paymentRegex.find(notification.text)?.groupValues?.getOrNull(1)
                    ?.replace("F", "")?.replace(".", "")?.replace(",", "")
                    ?: notification.amount?.toLong()?.toString() ?: ""

                val usernameRegex = """DISTANCE reçu:\s*([^(]*)(?:\(|a)""".toRegex()
                var username =
                    usernameRegex.find(notification.text)?.groupValues?.getOrNull(1)?.trim() ?: ""

                if (username.contains("*")) {
                    username = formatUsername(username)
                }

                playWolofRecordingWithText(
                    "wave_business_distance",
                    "$amount Franc C F A de $username"
                )
            }

            // ORANGE MONEY - Received transfer
            notification.packageName == "com.google.android.apps.messaging" &&
                    notification.title.contains("OrangeMoney", ignoreCase = true) &&
                    (notification.text.contains("recu", ignoreCase = true) ||
                            notification.text.contains("reçu", ignoreCase = true)) -> {

                // Clean text
                val cleanedText =
                    if (notification.text.contains("Votre solde", ignoreCase = true)) {
                        notification.text.split("Votre solde", ignoreCase = true)[0].trim()
                    } else {
                        notification.text
                    }

                val amountRegex =
                    """recu un transfert de (\d+)(?:\.\d+)?(?:FCFA|F)""".toRegex(RegexOption.IGNORE_CASE)
                val amountMatch = amountRegex.find(cleanedText)
                val amount =
                    amountMatch?.groupValues?.get(1)?.trim() ?: notification.amount?.toLong()
                        ?.toString() ?: ""

                val usernameRegex =
                    """(?:FCFA|F) [dD]e ([^\.]*?)(?:Ref|\.)""".toRegex(RegexOption.IGNORE_CASE)
                val usernameMatch = usernameRegex.find(cleanedText)
                var username = usernameMatch?.groupValues?.get(1)?.trim() ?: ""

                // Process username if it contains digits
                if (username.isNotEmpty()) {
                    val parts = username.split(" ", limit = 2)
                    val numberPart = parts[0]
                    val namePart = if (parts.size > 1) parts[1] else ""

                    if (numberPart.length >= 4 && numberPart.all { it.isDigit() }) {
                        val first2 = numberPart.take(2)
                        val last2 = numberPart.takeLast(2)

                        username = if (namePart.isNotEmpty()) {
                            "$first2 étoile $last2 $namePart"
                        } else {
                            "$first2 étoile $last2"
                        }
                    }
                }

                playWolofRecordingWithText("orange_money_received", "$amount Franc C F A de $username")
            }

            // OTHER ORANGE MONEY
            notification.packageName == "com.google.android.apps.messaging" &&
                    notification.title.contains("OrangeMoney", ignoreCase = true) -> {

                val textToRead = if (notification.text.contains("Votre solde", ignoreCase = true)) {
                    notification.text.split("Votre solde", ignoreCase = true)[0].trim()
                } else if (notification.text.contains("Solde:", ignoreCase = true)) {
                    notification.text.split("Solde:", ignoreCase = true)[0].trim()
                } else {
                    notification.text.trim()
                }

                val amountRegex = """(\d+)(?:\.00)?(?:F|FCFA)""".toRegex(RegexOption.IGNORE_CASE)
                val amount = amountRegex.find(textToRead)?.groupValues?.get(1)
                    ?: notification.amount?.toLong()?.toString() ?: ""

                playWolofRecordingWithText("orange_money_general", "$amount , Franc C F A")
            }

            // MIXX BY YAS - Received
            notification.packageName == "com.google.android.apps.messaging" &&
                    notification.title.contains("Mixx by Yas", ignoreCase = true) &&
                    (notification.text.contains("recu", ignoreCase = true) ||
                            notification.text.contains("reçu", ignoreCase = true)) -> {

                // Clean text
                val cleanedText =
                    if (notification.text.contains("Votre nouveau solde", ignoreCase = true)) {
                        notification.text.split("Votre nouveau solde", ignoreCase = true)[0].trim()
                    } else if (notification.text.contains("Nouveau solde", ignoreCase = true)) {
                        notification.text.split("Nouveau solde", ignoreCase = true)[0].trim()
                    } else if (notification.text.contains("Votre solde", ignoreCase = true)) {
                        notification.text.split("Votre solde", ignoreCase = true)[0].trim()
                    } else {
                        notification.text.trim()
                    }

                val amountRegex =
                    """(\d+(?:[.,]\d+)?)(?:\s*(?:FCFA|F))""".toRegex(RegexOption.IGNORE_CASE)
                val amount = amountRegex.find(cleanedText)?.groupValues?.get(1)?.replace(",", "")
                    ?: notification.amount?.toLong()?.toString() ?: ""

                val senderRegex = """[Dd]e ([^\.]+)""".toRegex()
                val sender = senderRegex.find(cleanedText)?.groupValues?.get(1)?.trim() ?: ""
                val formattedSender = if (sender.isNotEmpty()) {
                    formatUsername(sender)
                } else {
                    ""
                }

                playWolofRecordingWithText("mixx_received", "$amount Franc C F A de $formattedSender")
            }

            // MIXX BY YAS - Sent
            notification.packageName == "com.google.android.apps.messaging" &&
                    notification.title.contains("Mixx by Yas", ignoreCase = true) &&
                    (notification.text.contains("envoy", ignoreCase = true)) -> {

                // Clean text
                val cleanedText =
                    if (notification.text.contains("Votre nouveau solde", ignoreCase = true)) {
                        notification.text.split("Votre nouveau solde", ignoreCase = true)[0].trim()
                    } else if (notification.text.contains("Nouveau solde", ignoreCase = true)) {
                        notification.text.split("Nouveau solde", ignoreCase = true)[0].trim()
                    } else if (notification.text.contains("Votre solde", ignoreCase = true)) {
                        notification.text.split("Votre solde", ignoreCase = true)[0].trim()
                    } else {
                        notification.text.trim()
                    }

                val amountRegex =
                    """(\d+(?:[.,]\d+)?)(?:\s*(?:FCFA|F))""".toRegex(RegexOption.IGNORE_CASE)
                val amount = amountRegex.find(cleanedText)?.groupValues?.get(1)?.replace(",", "")
                    ?: notification.amount?.toLong()?.toString() ?: ""

                val recipientRegex = """[àÀaA] ([^\.]+)""".toRegex()
                val recipient = recipientRegex.find(cleanedText)?.groupValues?.get(1)?.trim() ?: ""
                val formattedRecipient = if (recipient.isNotEmpty()) {
                    formatUsername(recipient)
                } else {
                    ""
                }

                playWolofRecordingWithText("mixx_sent", "$amount Franc C F A de $formattedRecipient")
            }

            // WAVE PERSONAL - Payment made
            notification.packageName == "com.wave.personal" &&
                    notification.title.contains("Paiement réussi", ignoreCase = true) -> {

                val paymentRegex = """(?:Vous avez payé|payé)\s+(\d+(?:\.\d+)?F)""".toRegex()
                val amount = paymentRegex.find(notification.text)?.groupValues?.getOrNull(1)
                    ?.replace("F", "")?.replace(".", "") ?: notification.amount?.toLong()
                    ?.toString() ?: ""

                playWolofRecordingWithText("wave_personal_payment", "$amount Franc C F A ")
            }

            // WAVE PERSONAL - Transfer sent
            notification.packageName == "com.wave.personal" &&
                    (notification.title.contains("Transfert réussi", ignoreCase = true) ||
                            notification.title.contains("Transfert envoyé", ignoreCase = true)) -> {

                // Get text before "Nouveau solde" if present
                val text = if (notification.text.contains("Nouveau solde", ignoreCase = true)) {
                    notification.text.substring(
                        0,
                        notification.text.indexOf("Nouveau solde", ignoreCase = true)
                    ).trim()
                } else {
                    notification.text
                }

                val transferRegex = """(?:Vous avez envoyé|envoyé)\s+(\d+(?:\.\d+)?F)""".toRegex()
                val amount = transferRegex.find(text)?.groupValues?.getOrNull(1)
                    ?.replace("F", "")?.replace(".", "") ?: notification.amount?.toLong()
                    ?.toString() ?: ""

                // Extract recipient name using a pattern that won't match timestamps
                val recipientRegex =
                    """A\s+([^0-9\s\(\)\.;:]+(?:\s+[^0-9\s\(\)\.;:]+)*)""".toRegex()
                val recipient = recipientRegex.find(text)?.groupValues?.getOrNull(1)?.trim() ?: ""

                playWolofRecordingWithText(
                    "wave_personal_sent",
                    "$amount Franc C F A  ${if (recipient.isNotEmpty()) recipient else ""}"
                )
            }

            // WAVE PERSONAL - Transfer received
            notification.packageName == "com.wave.personal" &&
                    notification.title.contains("Transfert reçu", ignoreCase = true) -> {

                val receivedRegex = """(?:Vous avez reçu|reçu)\s+(\d+(?:\.\d+)?F)""".toRegex()
                val amount = receivedRegex.find(notification.text)?.groupValues?.getOrNull(1)
                    ?.replace("F", "")?.replace(".", "") ?: notification.amount?.toLong()
                    ?.toString() ?: ""

                // Extract sender name
                val senderRegex =
                    """Vous avez reçu\s+\d+(?:\.\d+)?F\s+[dD]e\s+([^\.]+)""".toRegex(RegexOption.IGNORE_CASE)
                var sender =
                    senderRegex.find(notification.text)?.groupValues?.getOrNull(1)?.trim() ?: ""

                // If no sender found with specific pattern, try general "de" pattern
                if (sender.isEmpty()) {
                    val generalSenderRegex = """[dD]e\s+([^\.]+)""".toRegex(RegexOption.IGNORE_CASE)
                    sender = generalSenderRegex.find(notification.text)?.groupValues?.getOrNull(1)
                        ?.trim() ?: ""
                }

                // Format sender name if needed
                if (sender.contains("*") || sender.contains("(")) {
                    sender = formatUsername(sender)
                } else if (sender.matches("""^\d+.*""".toRegex())) {
                    // Handle numeric sender names
                    val parts = sender.split(" ", limit = 2)
                    val numberPart = parts[0]
                    val namePart = if (parts.size > 1) " " + parts[1] else ""

                    if (numberPart.length >= 4 && numberPart.all { it.isDigit() }) {
                        val first2 = numberPart.take(2)
                        val last2 = numberPart.takeLast(2)
                        sender = "$first2 étoile $last2$namePart"
                    }
                }

                playWolofRecordingWithText("wave_personal_received", "$amount Franc C F A a $sender")
            }

            // Fallback to regular TTS for unhandled cases
            else -> {
                val textToSpeak = prepareTextToSpeak(notification)
                tts?.speak(
                    textToSpeak,
                    TextToSpeech.QUEUE_ADD,
                    null,
                    "notification_${notification.id}"
                )
            }
        }
    }

    // Helper method to play Wolof recording followed by dynamic text with TTS
    // Dans TTSService.kt
    private fun playWolofRecordingWithText(recordingKey: String, dynamicText: String) {
        try {
            // Get the recording resource ID
            val resourceId = WOLOF_RECORDINGS[recordingKey]
            if (resourceId == null) {
                Log.e("KUFAY_TTS", "Wolof recording not found for key: $recordingKey")
                // Fall back to TTS
                tts?.speak(dynamicText, TextToSpeech.QUEUE_ADD, null, "dynamic_fallback")
                return
            }

            Log.d("KUFAY_TTS", "Playing Wolof recording with ID: $resourceId")

            // Create MediaPlayer and prepare the audio resource
            try {
                val mediaPlayer = MediaPlayer.create(context, resourceId)
                if (mediaPlayer != null) {
                    // Set completion listener to speak dynamic text after recording
                    mediaPlayer.setOnCompletionListener {
                        it.release()
                        // Speak the dynamic part using regular TTS (French or English)
                        tts?.speak(dynamicText, TextToSpeech.QUEUE_ADD, null, "dynamic_${System.currentTimeMillis()}")
                    }

                    // Start playback
                    mediaPlayer.start()
                } else {
                    Log.e("KUFAY_TTS", "Failed to create MediaPlayer for resource: $resourceId")
                    tts?.speak(dynamicText, TextToSpeech.QUEUE_ADD, null, "dynamic_error_fallback")
                }
            } catch (e: Exception) {
                Log.e("KUFAY_TTS", "Error playing Wolof recording: ${e.message}")
                // Fall back to regular TTS on error
                tts?.speak(dynamicText, TextToSpeech.QUEUE_ADD, null, "dynamic_error_fallback")
            }
        } catch (e: Exception) {
            Log.e("KUFAY_TTS", "Exception in playWolofRecordingWithText: ${e.message}")
            // Fall back to regular TTS on any exception
            tts?.speak(dynamicText, TextToSpeech.QUEUE_ADD, null, "exception_fallback")
        }
    }

    fun stop() {
        tts?.stop()
    }

    // Existing method
    private fun prepareTextToSpeak(notification: Notification): String {
        // Original implementation remains the same
        // Use "Wave" instead of "Wave Personal"
        val appName = if (notification.packageName == "com.wave.personal") {
            "Wave"
        } else {
            notification.appName
        }

        // WAVE BUSINESS NOTIFICATIONS
        if (notification.packageName == "com.wave.business") {
            // Existing implementation for Wave Business notifications
            val title = notification.title
            val text = notification.text

            // 1. "Paiement réussi!" - format: "Wave Business Paiement reçu [amount] de [username]!"
            if (title.contains("Paiement réussi", ignoreCase = true)) {
                // Extract amount
                val paymentRegex =
                    """a payé\s+(\d+(?:[.,]\d+)?(?:F|))""".toRegex(RegexOption.IGNORE_CASE)
                val amount = paymentRegex.find(text)?.groupValues?.getOrNull(1)
                    ?.replace("F", "")?.replace(".", "")?.replace(",", "") ?: ""

                // Extract username: between "DISTANCE reçu:" and "a payé"
                val usernameRegex = """DISTANCE reçu:\s*([^(]*)(?:\(|a)""".toRegex()
                var username = usernameRegex.find(text)?.groupValues?.getOrNull(1)?.trim() ?: ""

                // If username contains stars, format properly
                if (username.contains("*")) {
                    username = formatUsername(username)
                }

                return "Wave Business: Paiement reçu $amount Franc C F A de $username!"
            }

            // 2. "Zéro frais!" - format: "Wave Business Paiement encaissé [amount] de [username]!"
            else if (title.contains("Zéro frais", ignoreCase = true)) {
                // Extract amount
                val encaissementRegex =
                    """sur votre encaissement de\s+(\d+(?:[.,]\d+)?)""".toRegex(RegexOption.IGNORE_CASE)
                val amount = encaissementRegex.find(text)?.groupValues?.getOrNull(1)
                    ?.replace(".", "")?.replace(",", "") ?: ""

                // Extract username: between "de" and "le [date]"
                val usernameRegex =
                    """[dD]e\s+([^(]*)(?:\(|\s+le)""".toRegex(RegexOption.IGNORE_CASE)
                var username = usernameRegex.find(text)?.groupValues?.getOrNull(1)?.trim() ?: ""

                // If username contains stars, format properly
                if (username.contains("*")) {
                    username = formatUsername(username)
                }

                return "Wave Business: Paiement encaissé $amount Franc C F A de $username!"
            }

            // 3. "À DISTANCE reçu" - format similar to "Paiement réussi"
            else if (text.contains("À DISTANCE reçu", ignoreCase = true) ||
                text.contains("A DISTANCE reçu", ignoreCase = true)
            ) {

                // Extract amount
                val paymentRegex =
                    """a payé\s+(\d+(?:[.,]\d+)?(?:F|))""".toRegex(RegexOption.IGNORE_CASE)
                val amount = paymentRegex.find(text)?.groupValues?.getOrNull(1)
                    ?.replace("F", "")?.replace(".", "")?.replace(",", "") ?: ""

                // Extract username: between "DISTANCE reçu:" and "a payé"
                val usernameRegex = """DISTANCE reçu:\s*([^(]*)(?:\(|a)""".toRegex()
                var username = usernameRegex.find(text)?.groupValues?.getOrNull(1)?.trim() ?: ""

                // If username contains stars, format properly
                if (username.contains("*")) {
                    username = formatUsername(username)
                }

                return "Wave Business: Paiement reçu à distance $amount Franc C F A de $username!"
            }

            // Standard Wave Business notification
            else {
                // Extract amount
                val amount = notification.amount?.toLong()?.toString() ?: ""
                return "$appName: $title. $amount , Franc C F A"
            }
        }

        // Rest of the original implementation for other notification types
        // ORANGE MONEY NOTIFICATIONS with "Vous avez recu"
        else if (notification.packageName == "com.google.android.apps.messaging" &&
            notification.title.contains("OrangeMoney", ignoreCase = true) &&
            (notification.text.contains("recu", ignoreCase = true) ||
                    notification.text.contains("reçu", ignoreCase = true))
        ) {

// Clean text - remove anything after "Votre solde" if present
            val cleanedText = if (notification.text.contains("Votre solde", ignoreCase = true)) {
                notification.text.split("Votre solde", ignoreCase = true)[0].trim()
            } else {
                notification.text
            }

            // Extract amount - ONLY INTEGER PART
            val amountRegex =
                """recu un transfert de (\d+)(?:\.\d+)?(?:FCFA|F)""".toRegex(RegexOption.IGNORE_CASE)
            val amountMatch = amountRegex.find(cleanedText)
            var amount = amountMatch?.groupValues?.get(1)?.trim() ?: ""

            // If amount is empty from the regex, try to get it from the notification object
            if (amount.isEmpty() && notification.amount != null) {
                amount = notification.amount.toLong().toString()
            }

            // Make sure we have an amount to read
            Log.d("KUFAY_TTS", "Extracted Orange Money amount: $amount (integer part only)")

            // Extract username - get the part after "de" and before a period or "Ref"
            val usernameRegex =
                """(?:FCFA|F) [dD]e ([^\.]*?)(?:Ref|\.)""".toRegex(RegexOption.IGNORE_CASE)
            val usernameMatch = usernameRegex.find(cleanedText)
            var username = usernameMatch?.groupValues?.get(1)?.trim() ?: ""

            Log.d("KUFAY_TTS", "Extracted Orange Money username: $username")

            // Special handling for numeric usernames (like "781624779 Fofana")
            if (username.isNotEmpty()) {
                // Split into number part and name part (if exists)
                val parts = username.split(" ", limit = 2)
                val numberPart = parts[0]
                val namePart = if (parts.size > 1) parts[1] else ""

                // Format number part as requested (first 2 digits + "étoile" + last 2 digits)
                if (numberPart.length >= 4 && numberPart.all { it.isDigit() }) {
                    val first2 = numberPart.take(2)
                    val last2 = numberPart.takeLast(2)

                    username = if (namePart.isNotEmpty()) {
                        "$first2 étoile $last2 $namePart"
                    } else {
                        "$first2 étoile $last2"
                    }
                }
            }

            return "Orange Money: vous avez reçu $amount Franc C F A de $username!"
        }

        // OTHER ORANGE MONEY NOTIFICATIONS
        else if (notification.packageName == "com.google.android.apps.messaging" &&
            notification.title.contains("OrangeMoney", ignoreCase = true)
        ) {
            // Get just the part of text before "Solde:" or "Votre solde" if it exists
            val textToRead = if (notification.text.contains("Votre solde", ignoreCase = true)) {
                notification.text.split("Votre solde", ignoreCase = true)[0].trim()
            } else if (notification.text.contains("Solde:", ignoreCase = true)) {
                notification.text.split("Solde:", ignoreCase = true)[0].trim()
            } else {
                notification.text.trim()
            }
            var formattedText = textToRead

            // Improved regex for amount formatting in Orange Money notifications
            // This regex will look for numbers followed by ".00" pattern
            val amountPattern = """(\d+(?:,\d+)*)\.00(?:F|FCFA|Fcfa)?""".toRegex(RegexOption.IGNORE_CASE)

            // Format the text by replacing amounts with properly spaced versions
            amountPattern.findAll(textToRead).forEach { matchResult ->
                val originalAmount = matchResult.value
                val numericPart = matchResult.groupValues[1].replace(",", "")

                // Replace the original amount format with TTS-friendly format
                formattedText = formattedText.replace(
                    originalAmount,
                    "$numericPart, "
                )
            }

            // Format any phone numbers in the text
            val phonePattern = """(\d{9,})""".toRegex()
            phonePattern.findAll(formattedText).forEach { match ->
                val phoneNumber = match.value
                if (phoneNumber.length >= 4) {
                    val first2 = phoneNumber.take(2)
                    val last2 = phoneNumber.takeLast(2)
                    val formattedNumber = "$first2 étoile $last2"
                    formattedText = formattedText.replace(phoneNumber, formattedNumber)
                }
            }

            // Return the formatted text
            return "Orange Money: $formattedText"
        }

        // MIXX BY YAS NOTIFICATIONS with "reçu"
        else if (notification.packageName == "com.google.android.apps.messaging" &&
            notification.title.contains("Mixx by Yas", ignoreCase = true) &&
            (notification.text.contains("recu", ignoreCase = true) ||
                    notification.text.contains("reçu", ignoreCase = true))
        ) {

            // Clean text - remove anything after "Votre nouveau solde" if present
            val cleanedText =
                if (notification.text.contains("Votre nouveau solde", ignoreCase = true)) {
                    notification.text.split("Votre nouveau solde", ignoreCase = true)[0].trim()
                } else if (notification.text.contains("Nouveau solde", ignoreCase = true)) {
                    notification.text.split("Nouveau solde", ignoreCase = true)[0].trim()
                } else if (notification.text.contains("Votre solde", ignoreCase = true)) {
                    notification.text.split("Votre solde", ignoreCase = true)[0].trim()
                } else {
                    notification.text.trim()
                }

            // Extract amount
            val amountRegex =
                """(\d+(?:[.,]\d+)?)(?:\s*(?:FCFA|F))""".toRegex(RegexOption.IGNORE_CASE)
            val amountMatch = amountRegex.find(cleanedText)

            val amount = if (amountMatch != null) {
                amountMatch.groupValues[1].replace(",", "")
            } else if (notification.amount != null) {
                notification.amount.toLong().toString()
            } else {
                ""
            }

            // Extract sender for received money
            val senderRegex = """[Dd]e ([^\.]+)""".toRegex()
            val sender = senderRegex.find(cleanedText)?.groupValues?.get(1)?.trim() ?: ""

            val formattedSender = if (sender.isNotEmpty()) {
                formatUsername(sender)
            } else {
                ""
            }

            return if (formattedSender.isNotEmpty()) {
                "Mixx by Yasse: Vous avez reçu $amount , Franc C F A de $formattedSender"
            } else {
                "Mixx by Yasse: Vous avez reçu $amount , Franc C F A"
            }
        }

        // OTHER MIXX BY YAS NOTIFICATIONS with "envoyé" (these won't be auto-read)
        else if (notification.packageName == "com.google.android.apps.messaging" &&
            notification.title.contains("Mixx by Yas", ignoreCase = true) &&
            (notification.text.contains("envoy", ignoreCase = true))
        ) {

            // Clean text - remove anything after "Votre nouveau solde" if present
            val cleanedText =
                if (notification.text.contains("Votre nouveau solde", ignoreCase = true)) {
                    notification.text.split("Votre nouveau solde", ignoreCase = true)[0].trim()
                } else if (notification.text.contains("Nouveau solde", ignoreCase = true)) {
                    notification.text.split("Nouveau solde", ignoreCase = true)[0].trim()
                } else if (notification.text.contains("Votre solde", ignoreCase = true)) {
                    notification.text.split("Votre solde", ignoreCase = true)[0].trim()
                } else {
                    notification.text.trim()
                }

            // Extract amount
            val amountRegex =
                """(\d+(?:[.,]\d+)?)(?:\s*(?:FCFA|F))""".toRegex(RegexOption.IGNORE_CASE)
            val amountMatch = amountRegex.find(cleanedText)

            val amount = if (amountMatch != null) {
                amountMatch.groupValues[1].replace(",", "")
            } else if (notification.amount != null) {
                notification.amount.toLong().toString()
            } else {
                ""
            }

            // Extract recipient
            val recipientRegex = """[àÀaA] ([^\.]+)""".toRegex()
            val recipient = recipientRegex.find(cleanedText)?.groupValues?.get(1)?.trim() ?: ""

            val formattedRecipient = if (recipient.isNotEmpty()) {
                formatUsername(recipient)
            } else {
                ""
            }

            return if (formattedRecipient.isNotEmpty()) {
                "Mixx by Yass: Vous avez envoyé $amount , Franc C F A à $formattedRecipient"
            } else {
                "Mixx by Yass: Vous avez envoyé $amount , Franc C F A"
            }
        }

        // WAVE PERSONAL NOTIFICATIONS
        else if (notification.packageName == "com.wave.personal") {
            val title = notification.title
            var text = notification.text

            // For "Transfert réussi", remove part after "Nouveau solde"
            if (title.contains("Transfert réussi", ignoreCase = true) ||
                title.contains("Transfert envoyé", ignoreCase = true) ||
                title.contains("Transfert reçu!", ignoreCase = true)
            ) {
                val soldeIndex = text.indexOf("Nouveau solde", ignoreCase = true)
                if (soldeIndex > 0) {
                    // Cut off everything after "Nouveau solde"
                    text = text.substring(0, soldeIndex).trim()
                    Log.d("KUFAY_TTS", "Cleaned Wave Transfert text: $text")
                }

            }

            val extractedAmount = when {
                title.contains("Paiement réussi", ignoreCase = true) -> {
                    val paymentRegex = """(?:Vous avez payé|payé)\s+(\d+(?:\.\d+)?F)""".toRegex()
                    paymentRegex.find(text)?.groupValues?.getOrNull(1)
                        ?.replace("F", "")?.replace(".", "") ?: ""
                }

                title.contains("Transfert réussi", ignoreCase = true) ||
                        title.contains("Transfert envoyé", ignoreCase = true) -> {
                    val transferRegex =
                        """(?:Vous avez envoyé|envoyé)\s+(\d+(?:\.\d+)?F)""".toRegex()
                    transferRegex.find(text)?.groupValues?.getOrNull(1)
                        ?.replace("F", "")?.replace(".", "") ?: ""
                }

                title.contains("Transfert reçu", ignoreCase = true) -> {
                    val receivedRegex = """(?:Vous avez reçu|reçu)\s+(\d+(?:\.\d+)?F)""".toRegex()
                    receivedRegex.find(text)?.groupValues?.getOrNull(1)
                        ?.replace("F", "")?.replace(".", "") ?: ""
                }

                else -> notification.amount?.toLong()?.toString() ?: ""
            }

            if (extractedAmount.isNotEmpty()) {
                // For "Transfert reçu", specifically extract sender name
                if (title.contains("Transfert reçu", ignoreCase = true)) {
                    // Extract sender using specific patterns
                    val senderRegex =
                        """Vous avez reçu\s+\d+(?:\.\d+)?F\s+[dD]e\s+([^\.]+)""".toRegex(RegexOption.IGNORE_CASE)
                    var sender = senderRegex.find(text)?.groupValues?.getOrNull(1)?.trim() ?: ""

                    // If no sender found with specific pattern, try general "de" pattern
                    if (sender.isEmpty()) {
                        val generalSenderRegex =
                            """[dD]e\s+([^\.]+)""".toRegex(RegexOption.IGNORE_CASE)
                        sender =
                            generalSenderRegex.find(text)?.groupValues?.getOrNull(1)?.trim() ?: ""
                    }

                    // Format sender name if it contains stars or parentheses
                    if (sender.contains("*") || sender.contains("(")) {
                        sender = formatUsername(sender)
                    } else if (sender.matches("""^\d+.*""".toRegex())) {
                        // Handle numeric sender names (like "781234567 Diallo")
                        val parts = sender.split(" ", limit = 2)
                        val numberPart = parts[0]
                        val namePart = if (parts.size > 1) " " + parts[1] else ""

                        if (numberPart.length >= 4 && numberPart.all { it.isDigit() }) {
                            val first2 = numberPart.take(2)
                            val last2 = numberPart.takeLast(2)
                            sender = "$first2 étoile $last2$namePart"
                        }
                    }

                    return "Wave: $title. Vous avez reçu $extractedAmount , Franc C F A ....de $sender"
                }

                // For "Transfert réussi" - directly extract recipient name using a more specific pattern
                else if (title.contains("Transfert réussi", ignoreCase = true) ||
                    title.contains("Transfert envoyé", ignoreCase = true)
                ) {
                    // Extract recipient name using a pattern that won't match timestamps
                    val recipientRegex =
                        """A\s+([^0-9\s\(\)\.;:]+(?:\s+[^0-9\s\(\)\.;:]+)*)""".toRegex()
                    val recipient =
                        recipientRegex.find(text)?.groupValues?.getOrNull(1)?.trim() ?: ""

                    return if (recipient.isNotEmpty()) {
                        "Wave: $title. Vous avez envoyé $extractedAmount , Franc C F A... à $recipient"
                    } else {
                        "Wave: $title. Vous avez envoyé $extractedAmount , Franc C F A"
                    }
                }

                // For other Wave Personal notifications
                else {
                    return when {
                        title.contains("Paiement réussi", ignoreCase = true) ->
                            "Wave: $title. Vous avez payé $extractedAmount , Franc C F A"

                        else -> "Wave: $title. $extractedAmount , Franc C F A"
                    }
                }
            }
        }

        // DEFAULT FALLBACK - use standard format
        val title = notification.title

        // Clean text - remove anything after balance information
        val cleanedText = when {
            notification.text.contains("Votre nouveau solde", ignoreCase = true) ->
                notification.text.split("Votre nouveau solde", ignoreCase = true)[0].trim()

            notification.text.contains("Nouveau solde", ignoreCase = true) ->
                notification.text.split("Nouveau solde", ignoreCase = true)[0].trim()

            notification.text.contains("Votre solde", ignoreCase = true) ->
                notification.text.split("Votre solde", ignoreCase = true)[0].trim()

            else -> notification.text
        }

        val formattedAmount = notification.amount?.toLong()?.toString() ?: ""

        val builder = StringBuilder("$appName: $title. ")

        if (formattedAmount.isNotEmpty()) {
            builder.append("$formattedAmount , Franc C F A")

            if (notification.label != null) {
                builder.append(" pour ${notification.label}")
            }
        } else {
            builder.append(cleanedText)
        }

        return builder.toString()
    }

    // Format usernames with stars or parentheses
    private fun formatUsername(username: String): String {
        // Handle cases with parentheses - like "Fofana Cisse (78****79)" or "Fofana Cisse (781234567)"
        if (username.contains("(")) {
            val parts = username.split("(", limit = 2)
            val name = parts[0].trim()

            // If there's content in parentheses, format it
            if (parts.size > 1) {
                val parenthesisContent = parts[1].replace(")", "").trim()

                // If the content has stars, format as usual
                if (parenthesisContent.contains("*")) {
                    val firstDigitsMatch = """^(\d+)\*+""".toRegex().find(parenthesisContent)
                    val lastDigitsMatch = """\*+(\d+)$""".toRegex().find(parenthesisContent)

                    if (firstDigitsMatch != null && lastDigitsMatch != null) {
                        val firstDigits = firstDigitsMatch.groupValues[1]
                        val lastDigits = lastDigitsMatch.groupValues[1]
                        return "$name, $firstDigits étoile $lastDigits"
                    }
                }
                // If it's a full phone number (9 digits)
                else if (parenthesisContent.matches("""^\d{9,}$""".toRegex())) {
                    val first2 = parenthesisContent.take(2)
                    val last2 = parenthesisContent.takeLast(2)
                    return "$name, $first2 étoile $last2"
                }

                // Otherwise, just return the name part
                return name
            }

            // If no parenthesis content, just return the name
            return name
        }

        // Handle direct phone numbers with stars
        else if (username.contains("*")) {
            // Split the username to get parts
            val parts = username.split(" ", limit = 2)
            val phoneOrStars = parts[0]
            val namePart = if (parts.size > 1) " " + parts[1] else ""

            // Extract first digits before stars and last digits after stars
            val firstDigitsMatch = """^(\d+)\*+""".toRegex().find(phoneOrStars)
            val lastDigitsMatch = """\*+(\d+)$""".toRegex().find(phoneOrStars)

            if (firstDigitsMatch != null && lastDigitsMatch != null) {
                val firstDigits = firstDigitsMatch.groupValues[1]
                val lastDigits = lastDigitsMatch.groupValues[1]
                return "$firstDigits étoile $lastDigits$namePart"
            }

            // Simplify handling if we can't find exact pattern
            return phoneOrStars.replace("*", " étoile ") + namePart
        }

        // For numeric usernames (like full phone numbers)
        else if (username.matches("""^\d{7,}.*$""".toRegex())) {
            val parts = username.split(" ", limit = 2)
            val numberPart = parts[0]
            val namePart = if (parts.size > 1) " " + parts[1] else ""

            if (numberPart.length >= 4) {
                val first2 = numberPart.take(2)
                val last2 = numberPart.takeLast(2)
                return "$first2 étoile $last2$namePart"
            }
        }

        // No transformation needed
        return username
    }

    // Extract recipient from notification text
    private fun extractRecipient(text: String): String {
        // Extract for transfers ("A") - Uppercase A without accent, which is used for recipient
        val transferRegex = """A\s+([^0-9\s\(\)\.;:]+(?:\s+[^0-9\s\(\)\.;:]+)*)""".toRegex()
        transferRegex.find(text)?.groupValues?.getOrNull(1)?.trim()?.let {
            return it
        }

        // If the above didn't match, try a more general approach but avoid timestamps
        val generalRegex = """[àÀaA]\s+([^0-9\s\(\)\.;:]+(?:\s+[^0-9\s\(\)\.;:]+)*)""".toRegex()
        generalRegex.find(text)?.groupValues?.getOrNull(1)?.trim()?.let {
            // Additional check to avoid timestamp matches
            if (!it.contains("h") && it.length > 1) {
                return it
            }
        }

        return ""
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }

    // Add this to your TTSService class
    fun speakText(text: String) {
        // Use existing TTS engine to speak the provided text
        tts?.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "daily_total_${System.currentTimeMillis()}"
        )
    }


    // Dans TTSService.kt, modifiez les Log.d en Log.e
    // Dans TTSService.kt - Gardez cette méthode telle quelle
    fun testWolofMedia() {
        Log.e("KUFAY_TTS", "===== TESTING WOLOF MEDIA =====")
        try {
            val resourceId = R.raw.wolof_wave_personal_received
            val mediaPlayer = MediaPlayer.create(context, resourceId)

            if (mediaPlayer != null) {
                mediaPlayer.setOnCompletionListener {
                    Log.e("KUFAY_TTS", "Playback completed")
                    it.release()
                }

                mediaPlayer.start()
                Log.e("KUFAY_TTS", "Playing wolof_wave_personal_received from raw resources")
            } else {
                Log.e("KUFAY_TTS", "Failed to create MediaPlayer for wolof recording")
            }
        } catch (e: Exception) {
            Log.e("KUFAY_TTS", "Error testing wolof media: ${e.message}")
            e.printStackTrace()
        }
    }

}