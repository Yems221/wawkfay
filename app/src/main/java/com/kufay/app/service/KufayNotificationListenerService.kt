package com.kufay.app.service

import android.app.Notification
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.kufay.app.data.db.entities.Notification as KufayNotification
import com.kufay.app.data.repository.NotificationRepository
import com.kufay.app.utils.NotificationUtils
import com.kufay.app.data.preferences.UserPreferences
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class KufayNotificationListenerService : NotificationListenerService() {

    @Inject
    lateinit var notificationRepository: NotificationRepository

    @Inject
    lateinit var notificationUtils: NotificationUtils

    @Inject
    lateinit var ttsService: TTSService

    @Inject
    lateinit var userPreferences: UserPreferences

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName

        // Check if this notification is from a target app
        if (!notificationUtils.isTargetPackage(packageName)) {
            return
        }

        // For Google Messages, filter by keywords
        if (packageName == "com.google.android.apps.messaging") {
            val notification = sbn.notification
            val extras = notification.extras
            val title = extras.getString(Notification.EXTRA_TITLE) ?: return

            // Skip if the title doesn't contain target keywords
            if (!notificationUtils.containsTargetKeywords(title)) {
                return
            }
        }
        // Check if this notification is from a target app
        if (!notificationUtils.isTargetPackage(packageName)) {
            return
        }

// For Google Messages, filter by keywords
        if (packageName == "com.google.android.apps.messaging") {
            val notification = sbn.notification
            val extras = notification.extras
            val title = extras.getString(Notification.EXTRA_TITLE) ?: return

            // Skip if the title doesn't contain target keywords
            if (!notificationUtils.containsTargetKeywords(title)) {
                return
            }

            // Filter out phone credit notifications for Orange Money and Mixx by Yas
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
            if ((title.contains("OrangeMoney", ignoreCase = true) ||
                        title.contains("Mixx by Yas", ignoreCase = true)) &&
                (text.contains("credit telephonique", ignoreCase = true) ||
                        text.contains("TXN Id:RC", ignoreCase = true ) ||
                        text.contains("#123#", ignoreCase = true) ||
                        text.contains("detail des compteurs", ignoreCase = true))) {
                Log.d("KUFAY_SERVICE", "Skipping phone credit notification: $title")
                return
            }
        }

        // Extract notification data
        processNotification(sbn)
    }

    private fun processNotification(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        val notification = sbn.notification
        val extras = notification.extras

        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val timestamp = sbn.postTime

        // Get app name (display name)
        val packageManager = applicationContext.packageManager
        val appName = try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName.split(".").last()
        }

        // Extract financial data
        val (amount, currency, labelPair) = notificationUtils.extractFinancialData(packageName, title, text)
        val (amountText, label) = labelPair

        // Determine if this is an incoming transaction
        val isIncomingTransaction = when {
            packageName == "com.wave.personal" &&
                    text.contains("avez reçu", ignoreCase = true) -> true
            packageName == "com.wave.business" &&
                    (text.contains("votre encaissement de", ignoreCase = true) ||
                            text.contains("reçu", ignoreCase = true)) -> true
            packageName == "com.google.android.apps.messaging" &&
                    title.contains("OrangeMoney", ignoreCase = true) &&
                    (text.contains("recu", ignoreCase = true) ||
                            text.contains("reçu", ignoreCase = true)) -> true
            packageName == "com.google.android.apps.messaging" &&
                    title.contains("Mixx by Yas", ignoreCase = true) &&
                    (text.contains("recu", ignoreCase = true) ||
                            text.contains("reçu", ignoreCase = true)) -> true
            else -> false
        }

        // Determine appTag
        val appTag = when {
            packageName == "com.wave.personal" && !title.contains("business", ignoreCase = true) -> "WAVE_PERSONAL"
            packageName == "com.wave.business" -> "WAVE_BUSINESS"
            packageName == "com.google.android.apps.messaging" && title.contains("OrangeMoney", ignoreCase = true) -> "ORANGE_MONEY"
            packageName == "com.google.android.apps.messaging" && title.contains("Mixx by Yas", ignoreCase = true) -> "MIXX"
            else -> null
        }

        // Create Kufay notification object
        val kufayNotification = KufayNotification(
            packageName = packageName,
            appName = appName,
            title = title,
            text = text,
            timestamp = timestamp,
            amount = amount,
            amountText = amountText,
            currency = currency,
            label = label,
            isIncomingTransaction = isIncomingTransaction,
            appTag = appTag  // Add the new appTag field
        )

        // Save to database
        serviceScope.launch {
            val notificationId = notificationRepository.saveNotification(kufayNotification)

            // Check if this notification pattern is recognized and should be auto-read
            val isRecognizedPattern = notificationUtils.isRecognizedNotificationPattern(packageName, title, text)

            // Log notification details and pattern recognition
            Log.d("KUFAY_SERVICE", "Saved notification: $title ($packageName)")
            Log.d("KUFAY_SERVICE", "Is recognized pattern: $isRecognizedPattern")
            Log.d("KUFAY_SERVICE", "Is incoming transaction: $isIncomingTransaction")
            Log.d("KUFAY_SERVICE", "App Tag: $appTag")

            // Get the current auto-read setting from UserPreferences
            val autoReadEnabled = userPreferences.autoReadEnabled.first()

            // Auto-read if enabled AND if this is a recognized notification pattern
            if (autoReadEnabled && isRecognizedPattern) {
                ttsService.speakNotification(kufayNotification, isRecognizedPattern)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // We don't need to handle notification removal
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
}