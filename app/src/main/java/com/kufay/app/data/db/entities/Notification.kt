package com.kufay.app.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class Notification(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val timestamp: Long,
    val amount: Double? = null,
    val amountText: String? = null,
    val currency: String? = null,
    val label: String? = null,
    val isRead: Boolean = false,
    val isDeleted: Boolean = false,
    val deletionDate: Long? = null,
    val isIncomingTransaction: Boolean = false,  // New field to track incoming money
    val appTag: String? = null
)