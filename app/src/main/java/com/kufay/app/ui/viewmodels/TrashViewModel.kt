package com.kufay.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kufay.app.data.db.entities.Notification
import com.kufay.app.data.preferences.UserPreferences
import com.kufay.app.data.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrashViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    val deletedNotifications = notificationRepository.getAllDeletedNotifications()
    val autoDeleteDays = userPreferences.autoDeleteDays

    fun restoreFromTrash(notification: Notification) {
        viewModelScope.launch {
            notificationRepository.restoreFromTrash(notification.id)
        }
    }

    fun updateAutoDeleteDays(days: Int) {
        viewModelScope.launch {
            userPreferences.setAutoDeleteDays(days)
        }
    }
}