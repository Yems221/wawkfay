package com.kufay.app.data.repository

import com.kufay.app.data.db.dao.AmountByPackage
import com.kufay.app.data.db.dao.NotificationDao
import com.kufay.app.data.db.entities.Notification
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import android.util.Log

@Singleton
class NotificationRepository @Inject constructor(
    private val notificationDao: NotificationDao
) {
    fun getAllActiveNotifications(): Flow<List<Notification>> =
        notificationDao.getAllActiveNotifications()

    fun getAllDeletedNotifications(): Flow<List<Notification>> =
        notificationDao.getAllDeletedNotifications()

    fun getNotificationsByApp(packageName: String): Flow<List<Notification>> =
        notificationDao.getNotificationsByApp(packageName)

    fun getNotificationsByDateRange(startTime: Long, endTime: Long): Flow<List<Notification>> =
        notificationDao.getNotificationsByDateRange(startTime, endTime)

    fun getNotificationsByAmountRange(minAmount: Double, maxAmount: Double): Flow<List<Notification>> =
        notificationDao.getNotificationsByAmountRange(minAmount, maxAmount)

    fun getNotificationsByLabel(label: String): Flow<List<Notification>> =
        notificationDao.getNotificationsByLabel(label)

    fun searchNotifications(query: String): Flow<List<Notification>> =
        notificationDao.searchNotifications(query)

    suspend fun saveNotification(notification: Notification): Long =
        notificationDao.insert(notification)

    suspend fun isDuplicate(
        packageName: String,
        amount: Double,
        timestamp: Long,
        timeWindowMs: Long = 3000 // 5 secondes par défaut
    ): Boolean {
        return notificationDao.checkDuplicate(
            packageName = packageName,
            amount = amount,
            timestampStart = timestamp - timeWindowMs,
            timestampEnd = timestamp + timeWindowMs
        ) > 0
    }

    suspend fun moveToTrash(id: Long, deleteAfterDays: Int = 30) {
        val deletionDate = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, deleteAfterDays)
        }.timeInMillis
        notificationDao.moveToTrash(id, deletionDate)
    }

    suspend fun restoreFromTrash(id: Long) {
        // First restore the notification
        notificationDao.restoreFromTrash(id)

        // Then get the full notification details
        val notification = notificationDao.getNotificationById(id) ?: return

        // Check if it's an Orange Money notification
        if (notification.packageName == "com.google.android.apps.messaging" &&
            notification.title.contains("OrangeMoney", ignoreCase = true)) {

            // Reprocess the amount using fixed parsing logic
            val originalText = notification.text
            val specificPattern = """(\d+(?:,\d+)?(?:\.\d+)?)\s*(?:F|CFA|XOF)""".toRegex(RegexOption.IGNORE_CASE)
            val match = specificPattern.find(originalText)

            if (match != null) {
                val amountStr = match.groupValues[1]
                // If it contains a decimal point, keep only the part before it
                val correctedAmount = if (amountStr.contains(".")) {
                    val parts = amountStr.split(".")
                    parts[0].replace(",", "").toDoubleOrNull()
                } else {
                    amountStr.replace(",", "").replace(".", "").toDoubleOrNull()
                }

                // Only update if we successfully extracted a new amount
                if (correctedAmount != null) {
                    // Create updated notification with new amount but preserve all other fields
                    val updatedNotification = notification.copy(amount = correctedAmount)
                    notificationDao.update(updatedNotification)
                }
            }
        }
    }

    suspend fun cleanupExpiredNotifications() {
        val currentTime = System.currentTimeMillis()
        notificationDao.permanentlyDeleteExpiredNotifications(currentTime)
    }

    suspend fun markAsRead(id: Long, isRead: Boolean) {
        notificationDao.markAsRead(id, isRead)
    }

    fun getTotalIncomingAmount(): Flow<Double?> =
        notificationDao.getTotalIncomingAmount()

    fun getTotalIncomingAmountByDateRange(startTime: Long, endTime: Long): Flow<Double?> =
        notificationDao.getTotalIncomingAmountByDateRange(startTime, endTime)

    fun getTotalIncomingAmountByApp(): Flow<Map<String, Double>> =
        notificationDao.getTotalIncomingAmountByApp().map { amountByPackageList ->
            amountByPackageList.associate { amountByPackage ->
                amountByPackage.packageName to amountByPackage.amount
            }
        }

    fun getTotalIncomingAmountByAppAndDateRange(startTime: Long, endTime: Long): Flow<Map<String, Double>> =
        notificationDao.getTotalIncomingAmountByAppAndDateRange(startTime, endTime).map { amountByPackageList ->
            amountByPackageList.associate { amountByPackage ->
                amountByPackage.packageName to amountByPackage.amount
            }
        }

    // Add this function to NotificationRepository.kt
    fun getAllIncomingTransactions(): Flow<List<Notification>> =
        notificationDao.getAllIncomingTransactions()

    fun getIncomingTransactionsByDateRange(startTime: Long, endTime: Long): Flow<List<Notification>> =
        notificationDao.getIncomingTransactionsByDateRange(startTime, endTime)

    fun getDailyIncomingAmount(): Flow<Double?> {
        val calendar = Calendar.getInstance()
        // Set to start of today
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis

        // Set to end of today
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        val endTime = calendar.timeInMillis

        return notificationDao.getTotalIncomingAmountByDateRange(startTime, endTime)
    }

    // Updated implementation for NotificationRepository.kt
    suspend fun fixOrangeMoneyAmounts(): Int {
        // Get ALL notifications that might be Orange Money related
        val allNotifications = notificationDao.getAllNotifications()
        Log.d("KUFAY_FIX", "Checking ${allNotifications.size} total notifications")

        var fixedCount = 0

        for (notification in allNotifications) {
            // Only process potential Orange Money notifications
            if (notification.packageName == "com.google.android.apps.messaging" &&
                (notification.title.contains("OrangeMoney", ignoreCase = true) ||
                        notification.text.contains("OrangeMoney", ignoreCase = true))) {

                Log.d("KUFAY_FIX", "Found Orange Money notification: ${notification.id}")

                // Look for amount patterns in the text with decimal points
                val amountPattern = """(\d+)\.(\d+)[\s]*(?:F|CFA|FCFA|XOF)""".toRegex(RegexOption.IGNORE_CASE)
                val amountMatch = amountPattern.find(notification.text)

                if (amountMatch != null) {
                    val wholePart = amountMatch.groupValues[1].replace(",", "")
                    Log.d("KUFAY_FIX", "Found amount with decimal: ${amountMatch.value}, whole part: $wholePart")

                    val originalAmount = notification.amount
                    val newAmount = wholePart.toDoubleOrNull()

                    if (newAmount != null && (originalAmount == null || newAmount != originalAmount)) {
                        // Create updated notification
                        val updatedNotification = notification.copy(
                            amount = newAmount
                        )

                        // Update in database
                        notificationDao.update(updatedNotification)
                        fixedCount++

                        Log.d("KUFAY_FIX", "UPDATED: ID=${notification.id}, Old=${originalAmount}, New=${newAmount}")
                    }
                } else {
                    Log.d("KUFAY_FIX", "No decimal amount pattern found in: ${notification.text.take(50)}...")
                }
            }
        }

        Log.d("KUFAY_FIX", "Fixed $fixedCount Orange Money notifications")
        return fixedCount
    }
}
