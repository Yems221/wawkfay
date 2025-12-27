package com.kufay.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
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
import com.kufay.app.MainActivity
import com.kufay.app.R

@AndroidEntryPoint
class KufayNotificationListenerService : NotificationListenerService() {

    companion object {
        private const val FOREGROUND_NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "kufay_service_channel"
        private const val CHANNEL_NAME = "Kufay Service"
    }

    @Inject
    lateinit var notificationRepository: NotificationRepository

    @Inject
    lateinit var notificationUtils: NotificationUtils

    @Inject
    lateinit var ttsService: TTSService

    @Inject
    lateinit var userPreferences: UserPreferences

    // ✅ AJOUT : Injection du KufayNotificationManager
    @Inject
    lateinit var kufayNotificationManager: KufayNotificationManager

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Kufay notification listener service"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun createForegroundNotification(): android.app.Notification {
        createNotificationChannel()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Kufay est actif")
            .setContentText("Écoute des notifications financières...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("KUFAY_SERVICE", "Service created - starting as foreground")

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // Android 14+ (API 34)
                startForeground(
                    FOREGROUND_NOTIFICATION_ID,
                    createForegroundNotification(),
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                // Toutes les versions antérieures
                startForeground(FOREGROUND_NOTIFICATION_ID, createForegroundNotification())
            }
            Log.d("KUFAY_SERVICE", "Foreground service started successfully")
        } catch (e: Exception) {
            Log.e("KUFAY_SERVICE", "Error starting foreground service: ${e.message}")
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName

        if (!notificationUtils.isTargetPackage(packageName)) {
            return
        }

        if (packageName == "com.google.android.apps.messaging") {
            val notification = sbn.notification
            val extras = notification.extras
            val title = extras.getString(Notification.EXTRA_TITLE) ?: return

            if (!notificationUtils.containsTargetKeywords(title)) {
                return
            }

            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
            if ((title.contains("OrangeMoney", ignoreCase = true) ||
                        title.contains("Mixx by Yas", ignoreCase = true)) &&
                (text.contains("credit telephonique", ignoreCase = true) ||
                        text.contains("TXN Id:RC", ignoreCase = true) ||
                        text.contains("#123#", ignoreCase = true) ||
                        text.contains("detail des compteurs", ignoreCase = true))) {
                Log.d("KUFAY_SERVICE", "Skipping phone credit notification: $title")
                return
            }
        }

        processNotification(sbn)
    }

    private fun processNotification(sbn: StatusBarNotification) {
        val packageName = sbn.packageName
        val notification = sbn.notification
        val extras = notification.extras

        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val timestamp = sbn.postTime

        val packageManager = applicationContext.packageManager
        val appName = try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName.split(".").last()
        }

        val (amount, currency, labelPair) = notificationUtils.extractFinancialData(packageName, title, text)
        val (amountText, label) = labelPair

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

        val appTag = when {
            packageName == "com.wave.personal" && !title.contains("business", ignoreCase = true) -> "WAVE_PERSONAL"
            packageName == "com.wave.business" -> "WAVE_BUSINESS"
            packageName == "com.google.android.apps.messaging" && title.contains("OrangeMoney", ignoreCase = true) -> "ORANGE_MONEY"
            packageName == "com.google.android.apps.messaging" && title.contains("Mixx by Yas", ignoreCase = true) -> "MIXX"
            else -> null
        }

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
            appTag = appTag
        )

        serviceScope.launch {
            // Special handling for Mixx by Yas - check Ref first
            val isDuplicate = if (packageName == "com.google.android.apps.messaging" &&
                title.contains("Mixx by Yas", ignoreCase = true)) {

                // Check by Ref (unique transaction ID)
                val hasDuplicateRef = notificationRepository.isDuplicateByRef(
                    packageName = packageName,
                    text = text
                )

                if (hasDuplicateRef) {
                    Log.d("KUFAY_SERVICE", "🚫 DOUBLON MIXX DÉTECTÉ (Ref identique) - notification ignorée")
                    true
                } else {
                    // Fallback: check by amount + time window
                    amount?.let { validAmount ->
                        notificationRepository.isDuplicate(
                            packageName = packageName,
                            amount = validAmount,
                            timestamp = timestamp,
                            timeWindowMs = 5000
                        )
                    } ?: false
                }
            } else {
                // Pour autres apps: check normal par montant + timestamp
                amount?.let { validAmount ->
                    notificationRepository.isDuplicate(
                        packageName = packageName,
                        amount = validAmount,
                        timestamp = timestamp,
                        timeWindowMs = 5000
                    )
                } ?: false
            }

            if (isDuplicate) {
                Log.d("KUFAY_SERVICE", "🚫 DOUBLON DÉTECTÉ - notification ignorée: $title (${amount ?: "N/A"} $currency)")
                return@launch
            }

            val notificationId = notificationRepository.saveNotification(kufayNotification)
            val isRecognizedPattern = notificationUtils.isRecognizedNotificationPattern(packageName, title, text)

            Log.d("KUFAY_SERVICE", "Saved notification: $title ($packageName)")
            Log.d("KUFAY_SERVICE", "Is recognized pattern: $isRecognizedPattern")
            Log.d("KUFAY_SERVICE", "Is incoming transaction: $isIncomingTransaction")
            Log.d("KUFAY_SERVICE", "App Tag: $appTag")

            // ✅ AJOUT : Afficher la notification Kufay dans la barre de notification
            kufayNotificationManager.showKufayNotification(kufayNotification, notificationId)
            Log.d("KUFAY_SERVICE", "📱 Notification Kufay affichée dans la barre (ID: $notificationId)")

            val autoReadEnabled = userPreferences.autoReadEnabled.first()

            if (autoReadEnabled && isRecognizedPattern && isIncomingTransaction) {
                ttsService.speakNotification(kufayNotification, isRecognizedPattern)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // We don't need to handle notification removal
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("KUFAY_SERVICE", "onStartCommand called")
        return START_STICKY
    }

    override fun onDestroy() {
        Log.d("KUFAY_SERVICE", "Service destroyed")
        super.onDestroy()
    }
}
