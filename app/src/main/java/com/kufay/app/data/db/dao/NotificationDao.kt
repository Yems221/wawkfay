package com.kufay.app.data.db.dao

import androidx.room.*
import com.kufay.app.data.db.entities.Notification
import kotlinx.coroutines.flow.Flow

// Data class to hold package name and amount for query results
data class AmountByPackage(
    @ColumnInfo(name = "packageName") val packageName: String,
    @ColumnInfo(name = "title") val title: String? = null,
    @ColumnInfo(name = "totalAmount") val amount: Double
)

@Dao
interface NotificationDao {
    @Insert
    suspend fun insert(notification: Notification): Long

    @Update
    suspend fun update(notification: Notification)

    @Query("SELECT * FROM notifications WHERE isDeleted = 0 ORDER BY timestamp DESC")
    fun getAllActiveNotifications(): Flow<List<Notification>>

    @Query("SELECT * FROM notifications WHERE isDeleted = 1 ORDER BY timestamp DESC")
    fun getAllDeletedNotifications(): Flow<List<Notification>>

    @Query("SELECT * FROM notifications WHERE " +
            "(" +
            "   packageName = :packageName OR " +
            "   (:packageName = 'com.wave.personal' AND packageName = :packageName AND title NOT LIKE '%business%') OR " +
            "   (:packageName = 'com.wave.business' AND (packageName = :packageName OR (packageName = 'com.wave.personal' AND title LIKE '%business%'))) OR " +
            "   (:packageName = 'com.google.android.apps.messaging' AND packageName = :packageName AND (" +
            "       LOWER(title) LIKE '%orangemoney%' OR " +
            "       LOWER(title) LIKE '%mixx%' OR " +
            "       LOWER(text) LIKE '%orangemoney%' OR " +
            "       LOWER(text) LIKE '%mixx%'" +
            "   ))" +
            ") AND isDeleted = 0 ORDER BY timestamp DESC")
    fun getNotificationsByApp(packageName: String): Flow<List<Notification>>

    @Query("SELECT * FROM notifications WHERE timestamp BETWEEN :startTime AND :endTime AND isDeleted = 0 ORDER BY timestamp DESC")
    fun getNotificationsByDateRange(startTime: Long, endTime: Long): Flow<List<Notification>>

    @Query("SELECT * FROM notifications WHERE amount >= :minAmount AND amount <= :maxAmount AND isDeleted = 0 ORDER BY timestamp DESC")
    fun getNotificationsByAmountRange(minAmount: Double, maxAmount: Double): Flow<List<Notification>>

    @Query("SELECT * FROM notifications WHERE label = :label AND isDeleted = 0 ORDER BY timestamp DESC")
    fun getNotificationsByLabel(label: String): Flow<List<Notification>>

    @Query("SELECT * FROM notifications WHERE (LOWER(title) LIKE '%' || LOWER(:query) || '%' " +
            "OR LOWER(text) LIKE '%' || LOWER(:query) || '%' " +
            "OR LOWER(appName) LIKE '%' || LOWER(:query) || '%') " +
            "AND isDeleted = 0 ORDER BY timestamp DESC")
    fun searchNotifications(query: String): Flow<List<Notification>>

    @Query("UPDATE notifications SET isDeleted = 1, deletionDate = :deletionDate WHERE id = :id")
    suspend fun moveToTrash(id: Long, deletionDate: Long)

    @Query("UPDATE notifications SET isDeleted = 0, deletionDate = NULL WHERE id = :id")
    suspend fun restoreFromTrash(id: Long)

    @Query("DELETE FROM notifications WHERE isDeleted = 1 AND deletionDate <= :timestamp")
    suspend fun permanentlyDeleteExpiredNotifications(timestamp: Long): Int

    @Query("UPDATE notifications SET isRead = :isRead WHERE id = :id")
    suspend fun markAsRead(id: Long, isRead: Boolean)

    @Query("SELECT SUM(amount) FROM notifications WHERE isIncomingTransaction = 1 AND isDeleted = 0")
    fun getTotalIncomingAmount(): Flow<Double?>

    @Query("SELECT SUM(amount) FROM notifications WHERE isIncomingTransaction = 1 AND isDeleted = 0 AND timestamp BETWEEN :startTime AND :endTime")
    fun getTotalIncomingAmountByDateRange(startTime: Long, endTime: Long): Flow<Double?>

    @Query("""
    SELECT 
        CASE 
            WHEN appTag IS NOT NULL THEN appTag
            WHEN packageName = 'com.google.android.apps.messaging' 
            THEN 
                CASE 
                    WHEN LOWER(title) LIKE '%orangemoney%' THEN 'Orange Money'
                    WHEN LOWER(title) LIKE '%mixx by yas%' THEN 'Mixx by Yas'
                    ELSE 'Messaging Apps'
                END
            ELSE packageName 
        END AS packageName, 
        SUM(amount) as totalAmount 
    FROM notifications 
    WHERE isIncomingTransaction = 1 AND isDeleted = 0 
    GROUP BY 
        CASE 
            WHEN appTag IS NOT NULL THEN appTag
            WHEN packageName = 'com.google.android.apps.messaging' 
            THEN 
                CASE 
                    WHEN LOWER(title) LIKE '%orangemoney%' THEN 'Orange Money'
                    WHEN LOWER(title) LIKE '%mixx by yas%' THEN 'Mixx by Yas'
                    ELSE 'Messaging Apps'
                END
            ELSE packageName 
        END
""")
    fun getTotalIncomingAmountByApp(): Flow<List<AmountByPackage>>

    @Query("""
    SELECT 
        CASE 
            WHEN appTag IS NOT NULL THEN appTag
            WHEN packageName = 'com.google.android.apps.messaging' 
            THEN 
                CASE 
                    WHEN LOWER(title) LIKE '%orangemoney%' THEN 'Orange Money'
                    WHEN LOWER(title) LIKE '%mixx by yas%' THEN 'Mixx by Yas'
                    ELSE 'Messaging Apps'
                END
            ELSE packageName 
        END AS packageName, 
        SUM(amount) as totalAmount 
    FROM notifications 
    WHERE isIncomingTransaction = 1 AND isDeleted = 0 
    AND timestamp BETWEEN :startTime AND :endTime
    GROUP BY 
        CASE 
            WHEN appTag IS NOT NULL THEN appTag
            WHEN packageName = 'com.google.android.apps.messaging' 
            THEN 
                CASE 
                    WHEN LOWER(title) LIKE '%orangemoney%' THEN 'Orange Money'
                    WHEN LOWER(title) LIKE '%mixx by yas%' THEN 'Mixx by Yas'
                    ELSE 'Messaging Apps'
                END
            ELSE packageName 
        END
    """)
    fun getTotalIncomingAmountByAppAndDateRange(startTime: Long, endTime: Long): Flow<List<AmountByPackage>>

    @Query("SELECT * FROM notifications WHERE isIncomingTransaction = 1 AND isDeleted = 0 ORDER BY timestamp DESC")
    fun getAllIncomingTransactions(): Flow<List<Notification>>

    @Query("SELECT * FROM notifications WHERE isIncomingTransaction = 1 AND isDeleted = 0 AND timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getIncomingTransactionsByDateRange(startTime: Long, endTime: Long): Flow<List<Notification>>

    @Query("SELECT * FROM notifications WHERE id = :id")
    suspend fun getNotificationById(id: Long): Notification?

    @Query("SELECT * FROM notifications")
    suspend fun getAllNotifications(): List<Notification>

    @Query("SELECT * FROM notifications WHERE appTag = :appTag AND isDeleted = 0 ORDER BY timestamp DESC")
    fun getNotificationsByAppTag(appTag: String): Flow<List<Notification>>
}