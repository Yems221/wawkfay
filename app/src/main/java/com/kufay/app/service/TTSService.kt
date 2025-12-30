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
    private val recentlySpokenNotifications = mutableMapOf<Long, Long>()
    private val SPEECH_COOLDOWN_MS = 3000L
    private val MAX_CACHE_SIZE = 20

    init {
        tts = TextToSpeech(context, this)
        serviceScope.launch {
            observePreferences()
        }
    }

    private suspend fun observePreferences() {
        serviceScope.launch {
            userPreferences.ttsSpeechRate.collect { newRate ->
                Log.d("KUFAY_TTS", "Speech rate changed to: $newRate")
                withContext(Dispatchers.Main) {
                    tts?.setSpeechRate(newRate)
                }
            }
        }

        serviceScope.launch {
            userPreferences.ttsSpeechPitch.collect { newPitch ->
                Log.d("KUFAY_TTS", "Speech pitch changed to: $newPitch")
                withContext(Dispatchers.Main) {
                    tts?.setPitch(newPitch)
                }
            }
        }

        serviceScope.launch {
            userPreferences.ttsVoiceGender.collect { newGender ->
                Log.d("KUFAY_TTS", "Voice gender changed to: $newGender")
                withContext(Dispatchers.Main) {
                    setPreferredVoice()
                }
            }
        }

        serviceScope.launch {
            userPreferences.ttsLanguage.collect { newLanguage ->
                Log.d("KUFAY_TTS", "Language changed to: $newLanguage")
                withContext(Dispatchers.Main) {
                    setLanguage(newLanguage)
                }
            }
        }
    }

    private fun setLanguage(languageCode: String) {
        val locale = when (languageCode) {
            "fr" -> Locale.FRENCH
            "wo" -> Locale.FRENCH
            else -> Locale.FRENCH
        }

        val result = tts?.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA ||
            result == TextToSpeech.LANG_NOT_SUPPORTED
        ) {
            tts?.setLanguage(Locale.ENGLISH)
        }
    }

    private val WOLOF_RECORDINGS = mapOf(
        "wave_business_payment" to R.raw.wolof_wave_business_payment,
        "wave_business_encaissement" to R.raw.wolof_wave_business_encaissement,
        "wave_business_distance" to R.raw.wolof_wave_business_distance,
        "orange_money_received" to R.raw.wolof_orange_money_received,
        "orange_money_sent" to R.raw.wolof_orange_money_sent,
        "orange_money_payment" to R.raw.wolof_orange_money_payment,
        "mixx_received" to R.raw.wolof_mixx_received,
        "mixx_sent" to R.raw.wolof_mixx_sent,
        "wave_personal_payment" to R.raw.wolof_wave_personal_payment,
        "wave_personal_sent" to R.raw.wolof_wave_personal_sent,
        "wave_personal_received" to R.raw.wolof_wave_personal_received
    )

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            Log.d("KUFAY_TTS", "TTS Engine initialized successfully")

            val initialLanguage = runBlocking { userPreferences.ttsLanguage.first() }
            val initialRate = runBlocking { userPreferences.ttsSpeechRate.first() }
            val initialPitch = runBlocking { userPreferences.ttsSpeechPitch.first() }

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

    private fun applyAllTtsSettings() {
        val languageCode = prefs.getString("tts_language", "fr")
        Log.d("KUFAY_TTS", "Setting language to: $languageCode")

        val locale = when (languageCode) {
            "fr" -> Locale.FRENCH
            "wo" -> Locale.FRENCH
            else -> Locale.FRENCH
        }

        val result = tts?.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w("KUFAY_TTS", "Language $languageCode not supported, falling back to English")
            tts?.setLanguage(Locale.ENGLISH)
        }

        val speechRateStr = prefs.getFloat("tts_speech_rate", 1.0f).toString()
        val speechRate = speechRateStr.toFloatOrNull() ?: 1.0f
        Log.d("KUFAY_TTS", "Setting speech rate to: $speechRate")
        tts?.setSpeechRate(speechRate)

        val speechPitchStr = prefs.getFloat("tts_speech_pitch", 1.0f).toString()
        val speechPitch = speechPitchStr.toFloatOrNull() ?: 1.0f
        Log.d("KUFAY_TTS", "Setting speech pitch to: $speechPitch")
        tts?.setPitch(speechPitch)

        try {
            setPreferredVoice()
        } catch (e: Exception) {
            Log.e("KUFAY_TTS", "Error setting voice: ${e.message}")
        }

        Log.d(
            "KUFAY_TTS", "Current TTS settings - Language: ${tts?.voice?.locale}, " +
                    "Speech Rate: $speechRate, Speech Pitch: $speechPitch"
        )
    }

    fun refreshTtsSettings() {
        serviceScope.launch {
            val language = userPreferences.ttsLanguage.first()
            val rate = userPreferences.ttsSpeechRate.first()
            val pitch = userPreferences.ttsSpeechPitch.first()

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

    private fun setPreferredVoice() {
        val preferredGender = prefs.getString("tts_voice_gender", "female")
        Log.d("KUFAY_TTS", "Setting preferred voice gender: $preferredGender")

        val voices = tts?.voices ?: return
        if (voices.isEmpty()) {
            Log.w("KUFAY_TTS", "No voices available for TTS")
            return
        }

        val currentLocale = tts?.voice?.locale ?: Locale.FRENCH

        val languageVoices = voices.filter { it.locale.language == currentLocale.language }
        if (languageVoices.isEmpty()) {
            Log.w("KUFAY_TTS", "No voices for language: ${currentLocale.language}")
            return
        }

        val genderedVoices = languageVoices.filter { voice ->
            when (preferredGender) {
                "male" -> !voice.name.contains("female", ignoreCase = true)
                else -> voice.name.contains("female", ignoreCase = true)
            }
        }

        val selectedVoice =
            if (genderedVoices.isNotEmpty()) genderedVoices.first() else languageVoices.first()
        Log.d(
            "KUFAY_TTS",
            "Selected voice: ${selectedVoice.name} for locale: ${selectedVoice.locale}"
        )

        tts?.voice = selectedVoice
    }

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
                setLanguage(language)
                tts?.setSpeechRate(rate)
                tts?.setPitch(pitch)

                tts?.speak(
                    testText, TextToSpeech.QUEUE_FLUSH, null,
                    "tts_test_${System.currentTimeMillis()}"
                )
            }
        }
    }

    private fun shouldSpeakNotification(notificationId: Long): Boolean {
        val currentTime = System.currentTimeMillis()
        val lastSpokenTime = recentlySpokenNotifications[notificationId]

        if (lastSpokenTime == null) return true

        return (currentTime - lastSpokenTime) >= SPEECH_COOLDOWN_MS
    }

    private fun markAsSpoken(notificationId: Long) {
        val currentTime = System.currentTimeMillis()
        recentlySpokenNotifications[notificationId] = currentTime

        if (recentlySpokenNotifications.size > MAX_CACHE_SIZE) {
            val sortedEntries = recentlySpokenNotifications.entries
                .sortedByDescending { it.value }
                .take(MAX_CACHE_SIZE)
                .associate { it.key to it.value }

            recentlySpokenNotifications.clear()
            recentlySpokenNotifications.putAll(sortedEntries)
        }
    }

    fun speakNotification(notification: Notification, isRecognizedPattern: Boolean = true) {
        if (!shouldSpeakNotification(notification.id)) {
            Log.d("KUFAY_TTS", "Skipping duplicate notification ${notification.id}")
            return
        }

        val useWolofRecordings = runBlocking {
            userPreferences.useWolofRecordings.first()
        }
        val ttsLanguage = prefs.getString("tts_language", "fr")

        Log.d(
            "KUFAY_TTS",
            "Speaking notification: ${notification.title}, isRecognized: $isRecognizedPattern"
        )
        Log.d("KUFAY_TTS", "TTS Language: $ttsLanguage, Use Wolof: $useWolofRecordings")

        if (notification.packageName == "com.google.android.apps.messaging" &&
            notification.title.contains("Mixx by Yas", ignoreCase = true) &&
            !notification.text.contains("recu", ignoreCase = true) &&
            !notification.text.contains("reçu", ignoreCase = true)
        ) {
            Log.d("KUFAY_TTS", "Skipping Mixx by Yas notification without 'recu'")
            return
        }

        if (notification.packageName == "com.google.android.apps.messaging" &&
            notification.title.contains("OrangeMoney", ignoreCase = true) &&
            !notification.text.contains("Vous avez recu", ignoreCase = true) &&
            !notification.text.contains("Vous avez reçu", ignoreCase = true)
        ) {
            Log.d("KUFAY_TTS", "Skipping Orange Money notification without 'Vous avez recu'")
            return
        }

        markAsSpoken(notification.id)

        if (ttsLanguage == "wo" && useWolofRecordings && isRecognizedPattern) {
            playWolofNotification(notification)
        } else if (isRecognizedPattern) {
            val textToSpeak = prepareTextToSpeak(notification)
            tts?.speak(textToSpeak, TextToSpeech.QUEUE_ADD, null, "notification_${notification.id}")
        } else {
            Log.d("KUFAY_TTS", "Skipping auto-read for unrecognized pattern")
        }
    }

    fun manuallySpeak(notification: Notification) {
        markAsSpoken(notification.id)

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

    private fun playWolofNotification(notification: Notification) {
        when {
            // ===== WAVE PERSONAL - TRANSFER RECEIVED =====
            notification.packageName == "com.wave.personal" &&
                    (notification.title.contains("Transfert reçu", ignoreCase = true) ||
                            notification.title.contains("Transfer received", ignoreCase = true)) -> {

                val receivedEnRegex = """You received\s+(\d+(?:,\d+)?(?:\.\d+)?)\s*F""".toRegex(RegexOption.IGNORE_CASE)
                var amount = receivedEnRegex.find(notification.text)?.groupValues?.getOrNull(1)
                    ?.replace(",", "")?.replace("F", "")

                var sender: String? = null
                if (amount != null) {
                    val senderEnRegex = """From\s+([^(]+)(?:\s*\()?""".toRegex(RegexOption.IGNORE_CASE)
                    sender = senderEnRegex.find(notification.text)?.groupValues?.getOrNull(1)?.trim()
                }

                if (amount == null) {
                    val receivedRegex = """(?:Vous avez reçu|reçu)\s+(\d+(?:\.\d+)?F)""".toRegex()
                    amount = receivedRegex.find(notification.text)?.groupValues?.getOrNull(1)
                        ?.replace("F", "")?.replace(".", "")
                }

                if (sender == null) {
                    val senderRegex = """Vous avez reçu\s+\d+(?:\.\d+)?F\s+[dD]e\s+([^\.]+)""".toRegex(RegexOption.IGNORE_CASE)
                    sender = senderRegex.find(notification.text)?.groupValues?.getOrNull(1)?.trim()
                }

                if (sender == null) {
                    val generalSenderRegex = """[dD]e\s+([^\.]+)""".toRegex(RegexOption.IGNORE_CASE)
                    sender = generalSenderRegex.find(notification.text)?.groupValues?.getOrNull(1)?.trim()
                }

                if (amount == null) {
                    amount = notification.amount?.toLong()?.toString() ?: ""
                }

                sender = sender ?: ""

                if (sender.contains("*") || sender.contains("(")) {
                    sender = formatUsername(sender)
                } else if (sender.matches("""^\d+.*""".toRegex())) {
                    val parts = sender.split(" ", limit = 2)
                    val numberPart = parts[0]
                    val namePart = if (parts.size > 1) " " + parts[1] else ""

                    if (numberPart.length >= 4 && numberPart.all { it.isDigit() }) {
                        val first2 = numberPart.take(2)
                        val last2 = numberPart.takeLast(2)
                        sender = "$first2 étoile $last2$namePart"
                    }
                }

                playWolofRecordingWithText("wave_personal_received", "$amount Francs CFA de $sender")
            }

            // ===== WAVE PERSONAL - TRANSFER SENT =====
            notification.packageName == "com.wave.personal" &&
                    (notification.title.contains("Transfert réussi", ignoreCase = true) ||
                            notification.title.contains("Transfert envoyé", ignoreCase = true) ||
                            notification.title.contains("Transfer sent", ignoreCase = true)) -> {

                val text = when {
                    notification.text.contains("Nouveau solde", ignoreCase = true) ->
                        notification.text.substring(0, notification.text.indexOf("Nouveau solde", ignoreCase = true)).trim()
                    notification.text.contains("New balance", ignoreCase = true) ->
                        notification.text.substring(0, notification.text.indexOf("New balance", ignoreCase = true)).trim()
                    else -> notification.text
                }

                val sentEnRegex = """You sent\s+(\d+(?:,\d+)?(?:\.\d+)?)\s*F""".toRegex(RegexOption.IGNORE_CASE)
                var amount = sentEnRegex.find(text)?.groupValues?.getOrNull(1)
                    ?.replace(",", "")?.replace("F", "")

                var recipient: String? = null
                if (amount != null) {
                    val recipientEnRegex = """To\s+([^(]+)(?:\s*\()?""".toRegex(RegexOption.IGNORE_CASE)
                    recipient = recipientEnRegex.find(text)?.groupValues?.getOrNull(1)?.trim()
                }

                if (amount == null) {
                    val transferRegex = """(?:Vous avez envoyé|envoyé)\s+(\d+(?:\.\d+)?F)""".toRegex()
                    amount = transferRegex.find(text)?.groupValues?.getOrNull(1)
                        ?.replace("F", "")?.replace(".", "")
                }

                if (recipient == null) {
                    val recipientRegex = """A\s+([^0-9\s\(\)\.;:]+(?:\s+[^0-9\s\(\)\.;:]+)*)""".toRegex()
                    recipient = recipientRegex.find(text)?.groupValues?.getOrNull(1)?.trim()
                }

                if (amount == null) {
                    amount = notification.amount?.toLong()?.toString() ?: ""
                }

                recipient = recipient ?: ""

                playWolofRecordingWithText(
                    "wave_personal_sent",
                    "$amount Francs CFA ${if (recipient.isNotEmpty()) recipient else ""}"
                )
            }

            // ===== WAVE PERSONAL - PAYMENT MADE =====
            notification.packageName == "com.wave.personal" &&
                    (notification.title.contains("Paiement réussi", ignoreCase = true) ||
                            notification.title.contains("Payment successful", ignoreCase = true)) -> {

                val paymentEnRegex = """You have paid\s+(\d+(?:,\d+)?(?:\.\d+)?)\s*F""".toRegex(RegexOption.IGNORE_CASE)
                var amount = paymentEnRegex.find(notification.text)?.groupValues?.getOrNull(1)
                    ?.replace(",", "")?.replace("F", "")

                if (amount == null) {
                    val paymentRegex = """(?:Vous avez payé|payé)\s+(\d+(?:\.\d+)?F)""".toRegex()
                    amount = paymentRegex.find(notification.text)?.groupValues?.getOrNull(1)
                        ?.replace("F", "")?.replace(".", "")
                }

                if (amount == null) {
                    amount = notification.amount?.toLong()?.toString() ?: ""
                }

                playWolofRecordingWithText("wave_personal_payment", "$amount Francs CFA")
            }

            // ===== WAVE BUSINESS NOTIFICATIONS =====
            notification.packageName == "com.wave.business" &&
                    notification.title.contains("Paiement réussi", ignoreCase = true) -> {
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

                playWolofRecordingWithText("wave_business_payment", "$amount Francs CFA de $username")
            }

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
                    "$amount Francs CFA de $username"
                )
            }

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
                    "$amount Francs CFA de $username"
                )
            }

            notification.packageName == "com.google.android.apps.messaging" &&
                    notification.title.contains("OrangeMoney", ignoreCase = true) &&
                    notification.text.contains("Votre transfert", ignoreCase = true) -> {

                val cleanedText = if (notification.text.contains("Solde", ignoreCase = true)) {
                    notification.text.split("Solde", ignoreCase = true)[0].trim()
                } else {
                    notification.text
                }

                val amountRegex = """(\d+)(?:\.\d+)?(?:Fcfa|FCFA|F)""".toRegex(RegexOption.IGNORE_CASE)
                val amount = amountRegex.find(cleanedText)?.groupValues?.get(1)
                    ?: notification.amount?.toLong()?.toString() ?: ""

                val recipientRegex = """vers\s+(\d+)\s*([^\.a]*?)(?:\s+a\s+reussi|\.)""".toRegex(RegexOption.IGNORE_CASE)
                val recipientMatch = recipientRegex.find(cleanedText)

                val recipient = if (recipientMatch != null) {
                    val number = recipientMatch.groupValues[1]
                    val name = recipientMatch.groupValues[2].trim()
                    if (number.length >= 4) {
                        val first2 = number.take(2)
                        val last2 = number.takeLast(2)
                        if (name.isNotEmpty()) "$first2 étoile $last2 $name" else "$first2 étoile $last2"
                    } else {
                        "$number $name".trim()
                    }
                } else ""

                playWolofRecordingWithText("orange_money_sent", "$amount Francs CFA vers $recipient")
            }

            notification.packageName == "com.google.android.apps.messaging" &&
                    notification.title.contains("OrangeMoney", ignoreCase = true) &&
                    notification.text.contains("Votre operation", ignoreCase = true) -> {

                val cleanedText = if (notification.text.contains("Votre solde", ignoreCase = true)) {
                    notification.text.split("Votre solde", ignoreCase = true)[0].trim()
                } else {
                    notification.text
                }

                val amountRegex = """(\d+)(?:\.\d+)?(?:FCFA|Fcfa|F)""".toRegex(RegexOption.IGNORE_CASE)
                val amount = amountRegex.find(cleanedText)?.groupValues?.get(1)
                    ?: notification.amount?.toLong()?.toString() ?: ""

                playWolofRecordingWithText("orange_money_payment", "$amount Francs CFA")
            }

            notification.packageName == "com.google.android.apps.messaging" &&
                    notification.title.contains("OrangeMoney", ignoreCase = true) &&
                    (notification.text.contains("Vous avez recu", ignoreCase = true) ||
                            notification.text.contains("Vous avez reçu", ignoreCase = true)) -> {

                val cleanedText = if (notification.text.contains("Votre solde", ignoreCase = true)) {
                    notification.text.split("Votre solde", ignoreCase = true)[0].trim()
                } else {
                    notification.text
                }

                val amountRegex = """recu un transfert de (\d+)(?:\.\d+)?(?:FCFA|F)""".toRegex(RegexOption.IGNORE_CASE)
                val amountMatch = amountRegex.find(cleanedText)
                val amount = amountMatch?.groupValues?.get(1)?.trim()
                    ?: notification.amount?.toLong()?.toString() ?: ""

                val usernameRegex = """(?:FCFA|F) [dD]e ([^\.]*?)(?:Ref|\.)""".toRegex(RegexOption.IGNORE_CASE)
                val usernameMatch = usernameRegex.find(cleanedText)
                var username = usernameMatch?.groupValues?.get(1)?.trim() ?: ""

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

                playWolofRecordingWithText("orange_money_received", "$amount Francs CFA de $username")
            }

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

                playWolofRecordingWithText("orange_money_general", "$amount , Francs CFA")
            }

            notification.packageName == "com.google.android.apps.messaging" &&
                    notification.title.contains("Mixx by Yas", ignoreCase = true) &&
                    (notification.text.contains("recu", ignoreCase = true) ||
                            notification.text.contains("reçu", ignoreCase = true)) -> {

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

                playWolofRecordingWithText("mixx_received", "$amount Francs CFA de $formattedSender")
            }

            notification.packageName == "com.google.android.apps.messaging" &&
                    notification.title.contains("Mixx by Yas", ignoreCase = true) &&
                    (notification.text.contains("envoy", ignoreCase = true)) -> {

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

                playWolofRecordingWithText("mixx_sent", "$amount Francs CFA de $formattedRecipient")
            }

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

    private fun playWolofRecordingWithText(recordingKey: String, dynamicText: String) {
        try {
            val resourceId = WOLOF_RECORDINGS[recordingKey]
            if (resourceId == null) {
                Log.e("KUFAY_TTS", "Wolof recording not found for key: $recordingKey")
                tts?.speak(dynamicText, TextToSpeech.QUEUE_ADD, null, "dynamic_fallback")
                return
            }

            Log.d("KUFAY_TTS", "Playing Wolof recording with ID: $resourceId")

            try {
                val mediaPlayer = MediaPlayer.create(context, resourceId)
                if (mediaPlayer != null) {
                    mediaPlayer.setOnCompletionListener {
                        it.release()
                        tts?.speak(dynamicText, TextToSpeech.QUEUE_ADD, null, "dynamic_${System.currentTimeMillis()}")
                    }

                    mediaPlayer.start()
                } else {
                    Log.e("KUFAY_TTS", "Failed to create MediaPlayer for resource: $resourceId")
                    tts?.speak(dynamicText, TextToSpeech.QUEUE_ADD, null, "dynamic_error_fallback")
                }
            } catch (e: Exception) {
                Log.e("KUFAY_TTS", "Error playing Wolof recording: ${e.message}")
                tts?.speak(dynamicText, TextToSpeech.QUEUE_ADD, null, "dynamic_error_fallback")
            }
        } catch (e: Exception) {
            Log.e("KUFAY_TTS", "Exception in playWolofRecordingWithText: ${e.message}")
            tts?.speak(dynamicText, TextToSpeech.QUEUE_ADD, null, "exception_fallback")
        }
    }

    fun stop() {
        tts?.stop()
    }

    private fun prepareTextToSpeak(notification: Notification): String {
        val appName = if (notification.packageName == "com.wave.personal") {
            "Wave"
        } else {
            notification.appName
        }

        if (notification.packageName == "com.wave.business") {
            val title = notification.title
            val text = notification.text

            if (title.contains("Paiement réussi", ignoreCase = true)) {
                val paymentRegex =
                    """a payé\s+(\d+(?:[.,]\d+)?(?:F|))""".toRegex(RegexOption.IGNORE_CASE)
                val amount = paymentRegex.find(text)?.groupValues?.getOrNull(1)
                    ?.replace("F", "")?.replace(".", "")?.replace(",", "") ?: ""

                val usernameRegex = """DISTANCE reçu:\s*([^(]*)(?:\(|a)""".toRegex()
                var username = usernameRegex.find(text)?.groupValues?.getOrNull(1)?.trim() ?: ""

                if (username.contains("*")) {
                    username = formatUsername(username)
                }

                return "Wave Business: Paiement reçu $amount Francs CFA de $username!"
            }

            else if (title.contains("Zéro frais", ignoreCase = true)) {
                val encaissementRegex =
                    """sur votre encaissement de\s+(\d+(?:[.,]\d+)?)""".toRegex(RegexOption.IGNORE_CASE)
                val amount = encaissementRegex.find(text)?.groupValues?.getOrNull(1)
                    ?.replace(".", "")?.replace(",", "") ?: ""

                val usernameRegex =
                    """[dD]e\s+([^(]*)(?:\(|\s+le)""".toRegex(RegexOption.IGNORE_CASE)
                var username = usernameRegex.find(text)?.groupValues?.getOrNull(1)?.trim() ?: ""

                if (username.contains("*")) {
                    username = formatUsername(username)
                }

                return "Wave Business: Paiement encaissé $amount Francs CFA de $username!"
            }

            else if (text.contains("À DISTANCE reçu", ignoreCase = true) ||
                text.contains("A DISTANCE reçu", ignoreCase = true)
            ) {

                val paymentRegex =
                    """a payé\s+(\d+(?:[.,]\d+)?(?:F|))""".toRegex(RegexOption.IGNORE_CASE)
                val amount = paymentRegex.find(text)?.groupValues?.getOrNull(1)
                    ?.replace("F", "")?.replace(".", "")?.replace(",", "") ?: ""

                val usernameRegex = """DISTANCE reçu:\s*([^(]*)(?:\(|a)""".toRegex()
                var username = usernameRegex.find(text)?.groupValues?.getOrNull(1)?.trim() ?: ""

                if (username.contains("*")) {
                    username = formatUsername(username)
                }

                return "Wave Business: Paiement reçu à distance $amount Francs CFA de $username!"
            }

            else {
                val amount = notification.amount?.toLong()?.toString() ?: ""
                return "$appName: $title. $amount , Francs CFA"
            }
        }

        else if (notification.packageName == "com.google.android.apps.messaging" &&
            notification.title.contains("OrangeMoney", ignoreCase = true) &&
            (notification.text.contains("recu", ignoreCase = true) ||
                    notification.text.contains("reçu", ignoreCase = true))
        ) {

            val cleanedText = if (notification.text.contains("Votre solde", ignoreCase = true)) {
                notification.text.split("Votre solde", ignoreCase = true)[0].trim()
            } else {
                notification.text
            }

            val amountRegex =
                """recu un transfert de (\d+)(?:\.\d+)?(?:FCFA|F)""".toRegex(RegexOption.IGNORE_CASE)
            val amountMatch = amountRegex.find(cleanedText)
            var amount = amountMatch?.groupValues?.get(1)?.trim() ?: ""

            if (amount.isEmpty() && notification.amount != null) {
                amount = notification.amount.toLong().toString()
            }

            Log.d("KUFAY_TTS", "Extracted Orange Money amount: $amount (integer part only)")

            val usernameRegex =
                """(?:FCFA|F) [dD]e ([^\.]*?)(?:Ref|\.)""".toRegex(RegexOption.IGNORE_CASE)
            val usernameMatch = usernameRegex.find(cleanedText)
            var username = usernameMatch?.groupValues?.get(1)?.trim() ?: ""

            Log.d("KUFAY_TTS", "Extracted Orange Money username: $username")

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

            return "Orange Money: vous avez reçu $amount Francs CFA de $username!"
        }

        else if (notification.packageName == "com.google.android.apps.messaging" &&
            notification.title.contains("OrangeMoney", ignoreCase = true)
        ) {
            val textToRead = if (notification.text.contains("Votre solde", ignoreCase = true)) {
                notification.text.split("Votre solde", ignoreCase = true)[0].trim()
            } else if (notification.text.contains("Solde:", ignoreCase = true)) {
                notification.text.split("Solde:", ignoreCase = true)[0].trim()
            } else {
                notification.text.trim()
            }
            var formattedText = textToRead

            val amountPattern = """(\d+(?:,\d+)*)\.00(?:F|FCFA|Fcfa)?""".toRegex(RegexOption.IGNORE_CASE)

            amountPattern.findAll(textToRead).forEach { matchResult ->
                val originalAmount = matchResult.value
                val numericPart = matchResult.groupValues[1].replace(",", "")

                formattedText = formattedText.replace(
                    originalAmount,
                    "$numericPart, "
                )
            }

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

            return "Orange Money: $formattedText"
        }

        else if (notification.packageName == "com.google.android.apps.messaging" &&
            notification.title.contains("Mixx by Yas", ignoreCase = true) &&
            (notification.text.contains("recu", ignoreCase = true) ||
                    notification.text.contains("reçu", ignoreCase = true))
        ) {

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
            val amountMatch = amountRegex.find(cleanedText)

            val amount = if (amountMatch != null) {
                amountMatch.groupValues[1].replace(",", "")
            } else if (notification.amount != null) {
                notification.amount.toLong().toString()
            } else {
                ""
            }

            val senderRegex = """[Dd]e ([^\.]+)""".toRegex()
            val sender = senderRegex.find(cleanedText)?.groupValues?.get(1)?.trim() ?: ""

            val formattedSender = if (sender.isNotEmpty()) {
                formatUsername(sender)
            } else {
                ""
            }

            return if (formattedSender.isNotEmpty()) {
                "Mixx by Yasse: Vous avez reçu $amount , Francs CFA de $formattedSender"
            } else {
                "Mixx by Yasse: Vous avez reçu $amount , Francs CFA"
            }
        }

        else if (notification.packageName == "com.google.android.apps.messaging" &&
            notification.title.contains("Mixx by Yas", ignoreCase = true) &&
            (notification.text.contains("envoy", ignoreCase = true))
        ) {

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
            val amountMatch = amountRegex.find(cleanedText)

            val amount = if (amountMatch != null) {
                amountMatch.groupValues[1].replace(",", "")
            } else if (notification.amount != null) {
                notification.amount.toLong().toString()
            } else {
                ""
            }

            val recipientRegex = """[àÀaA] ([^\.]+)""".toRegex()
            val recipient = recipientRegex.find(cleanedText)?.groupValues?.get(1)?.trim() ?: ""

            val formattedRecipient = if (recipient.isNotEmpty()) {
                formatUsername(recipient)
            } else {
                ""
            }

            return if (formattedRecipient.isNotEmpty()) {
                "Mixx by Yass: Vous avez envoyé $amount , Francs CFA à $formattedRecipient"
            } else {
                "Mixx by Yass: Vous avez envoyé $amount , Francs CFA"
            }
        }

        else if (notification.packageName == "com.wave.personal") {
            val title = notification.title
            var text = notification.text

            val isTransferReceived = title.contains("Transfert reçu", ignoreCase = true) ||
                    title.contains("Transfer received", ignoreCase = true)
            val isTransferSent = title.contains("Transfert réussi", ignoreCase = true) ||
                    title.contains("Transfert envoyé", ignoreCase = true) ||
                    title.contains("Transfer sent", ignoreCase = true)
            val isPayment = title.contains("Paiement réussi", ignoreCase = true) ||
                    title.contains("Payment successful", ignoreCase = true)

            if (isTransferSent || isTransferReceived) {
                val soldeIndex = when {
                    text.contains("Nouveau solde", ignoreCase = true) ->
                        text.indexOf("Nouveau solde", ignoreCase = true)
                    text.contains("New balance", ignoreCase = true) ->
                        text.indexOf("New balance", ignoreCase = true)
                    else -> -1
                }
                if (soldeIndex > 0) {
                    text = text.substring(0, soldeIndex).trim()
                    Log.d("KUFAY_TTS", "Cleaned Wave text: $text")
                }
            }

            val extractedAmount = when {
                isPayment -> {
                    val paymentEnRegex = """You have paid\s+(\d+(?:,\d+)?(?:\.\d+)?)\s*F""".toRegex(RegexOption.IGNORE_CASE)
                    val enMatch = paymentEnRegex.find(text)?.groupValues?.getOrNull(1)
                        ?.replace(",", "")?.replace("F", "")

                    enMatch ?: run {
                        val paymentRegex = """(?:Vous avez payé|payé)\s+(\d+(?:\.\d+)?F)""".toRegex()
                        paymentRegex.find(text)?.groupValues?.getOrNull(1)
                            ?.replace("F", "")?.replace(".", "") ?: ""
                    }
                }

                isTransferSent -> {
                    val sentEnRegex = """You sent\s+(\d+(?:,\d+)?(?:\.\d+)?)\s*F""".toRegex(RegexOption.IGNORE_CASE)
                    val enMatch = sentEnRegex.find(text)?.groupValues?.getOrNull(1)
                        ?.replace(",", "")?.replace("F", "")

                    enMatch ?: run {
                        val transferRegex = """(?:Vous avez envoyé|envoyé)\s+(\d+(?:\.\d+)?F)""".toRegex()
                        transferRegex.find(text)?.groupValues?.getOrNull(1)
                            ?.replace("F", "")?.replace(".", "") ?: ""
                    }
                }

                isTransferReceived -> {
                    val receivedEnRegex = """You received\s+(\d+(?:,\d+)?(?:\.\d+)?)\s*F""".toRegex(RegexOption.IGNORE_CASE)
                    val enMatch = receivedEnRegex.find(text)?.groupValues?.getOrNull(1)
                        ?.replace(",", "")?.replace("F", "")

                    enMatch ?: run {
                        val receivedRegex = """(?:Vous avez reçu|reçu)\s+(\d+(?:\.\d+)?F)""".toRegex()
                        receivedRegex.find(text)?.groupValues?.getOrNull(1)
                            ?.replace("F", "")?.replace(".", "") ?: ""
                    }
                }

                else -> notification.amount?.toLong()?.toString() ?: ""
            }

            if (extractedAmount.isNotEmpty()) {
                if (isTransferReceived) {
                    val senderEnRegex = """From\s+([^(]+)(?:\s*\()?""".toRegex(RegexOption.IGNORE_CASE)
                    var sender = senderEnRegex.find(text)?.groupValues?.getOrNull(1)?.trim()

                    if (sender == null) {
                        val senderRegex = """Vous avez reçu\s+\d+(?:\.\d+)?F\s+[dD]e\s+([^\.]+)""".toRegex(RegexOption.IGNORE_CASE)
                        sender = senderRegex.find(text)?.groupValues?.getOrNull(1)?.trim()
                    }

                    if (sender == null) {
                        val generalSenderRegex = """[dD]e\s+([^\.]+)""".toRegex(RegexOption.IGNORE_CASE)
                        sender = generalSenderRegex.find(text)?.groupValues?.getOrNull(1)?.trim()
                    }

                    sender = sender ?: ""

                    if (sender.contains("*") || sender.contains("(")) {
                        sender = formatUsername(sender)
                    } else if (sender.matches("""^\d+.*""".toRegex())) {
                        val parts = sender.split(" ", limit = 2)
                        val numberPart = parts[0]
                        val namePart = if (parts.size > 1) " " + parts[1] else ""

                        if (numberPart.length >= 4 && numberPart.all { it.isDigit() }) {
                            val first2 = numberPart.take(2)
                            val last2 = numberPart.takeLast(2)
                            sender = "$first2 étoile $last2$namePart"
                        }
                    }

                    return "Wave: Transfert reçu. Vous avez reçu $extractedAmount Francs CFA de $sender"
                }

                else if (isTransferSent) {
                    val recipientEnRegex = """To\s+([^(]+)(?:\s*\()?""".toRegex(RegexOption.IGNORE_CASE)
                    var recipient = recipientEnRegex.find(text)?.groupValues?.getOrNull(1)?.trim()

                    if (recipient == null) {
                        val recipientRegex = """A\s+([^0-9\s\(\)\.;:]+(?:\s+[^0-9\s\(\)\.;:]+)*)""".toRegex()
                        recipient = recipientRegex.find(text)?.groupValues?.getOrNull(1)?.trim()
                    }

                    recipient = recipient ?: ""

                    return if (recipient.isNotEmpty()) {
                        "Wave: Transfert envoyé. Vous avez envoyé $extractedAmount Francs CFA à $recipient"
                    } else {
                        "Wave: Transfert envoyé. Vous avez envoyé $extractedAmount Francs CFA"
                    }
                }

                else {
                    return if (isPayment) {
                        "Wave: Paiement réussi. Vous avez payé $extractedAmount Francs CFA"
                    } else {
                        "Wave: $title. $extractedAmount Francs CFA"
                    }
                }
            }
        }

        val title = notification.title

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
            builder.append("$formattedAmount , Francs CFA")

            if (notification.label != null) {
                builder.append(" pour ${notification.label}")
            }
        } else {
            builder.append(cleanedText)
        }

        return builder.toString()
    }

    private fun formatUsername(username: String): String {
        if (username.contains("(")) {
            val parts = username.split("(", limit = 2)
            val name = parts[0].trim()

            if (parts.size > 1) {
                val parenthesisContent = parts[1].replace(")", "").trim()

                if (parenthesisContent.contains("*")) {
                    val firstDigitsMatch = """^(\d+)\*+""".toRegex().find(parenthesisContent)
                    val lastDigitsMatch = """\*+(\d+)$""".toRegex().find(parenthesisContent)

                    if (firstDigitsMatch != null && lastDigitsMatch != null) {
                        val firstDigits = firstDigitsMatch.groupValues[1]
                        val lastDigits = lastDigitsMatch.groupValues[1]
                        return "$name, $firstDigits étoile $lastDigits"
                    }
                }
                else if (parenthesisContent.matches("""^\d{9,}$""".toRegex())) {
                    val first2 = parenthesisContent.take(2)
                    val last2 = parenthesisContent.takeLast(2)
                    return "$name, $first2 étoile $last2"
                }

                return name
            }

            return name
        }

        else if (username.contains("*")) {
            val parts = username.split(" ", limit = 2)
            val phoneOrStars = parts[0]
            val namePart = if (parts.size > 1) " " + parts[1] else ""

            val firstDigitsMatch = """^(\d+)\*+""".toRegex().find(phoneOrStars)
            val lastDigitsMatch = """\*+(\d+)$""".toRegex().find(phoneOrStars)

            if (firstDigitsMatch != null && lastDigitsMatch != null) {
                val firstDigits = firstDigitsMatch.groupValues[1]
                val lastDigits = lastDigitsMatch.groupValues[1]
                return "$firstDigits étoile $lastDigits$namePart"
            }

            return phoneOrStars.replace("*", " étoile ") + namePart
        }

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

        return username
    }

    private fun extractRecipient(text: String): String {
        val transferRegex = """A\s+([^0-9\s\(\)\.;:]+(?:\s+[^0-9\s\(\)\.;:]+)*)""".toRegex()
        transferRegex.find(text)?.groupValues?.getOrNull(1)?.trim()?.let {
            return it
        }

        val generalRegex = """[àÀaA]\s+([^0-9\s\(\)\.;:]+(?:\s+[^0-9\s\(\)\.;:]+)*)""".toRegex()
        generalRegex.find(text)?.groupValues?.getOrNull(1)?.trim()?.let {
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

    fun speakText(text: String) {
        tts?.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "daily_total_${System.currentTimeMillis()}"
        )
    }

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
