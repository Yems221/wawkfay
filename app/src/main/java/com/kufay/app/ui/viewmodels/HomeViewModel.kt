package com.kufay.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kufay.app.data.db.entities.Notification
import com.kufay.app.data.preferences.UserPreferences
import com.kufay.app.data.repository.NotificationRepository
import com.kufay.app.service.TTSService
import com.kufay.app.ui.components.DateFilterType
import com.kufay.app.ui.models.AppType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val userPreferences: UserPreferences,
    private val ttsService: TTSService
) : ViewModel() {

    // Search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // App type filter - now supports multiple selections
    private val _selectedAppTypes = MutableStateFlow<Set<AppType>>(emptySet())
    val selectedAppTypes: StateFlow<Set<AppType>> = _selectedAppTypes.asStateFlow()

    // Date filter
    private val _dateFilterType = MutableStateFlow(DateFilterType.ALL_TIME)
    val dateFilterType: StateFlow<DateFilterType> = _dateFilterType.asStateFlow()

    // Selected single date (for single day filter)
    private val _selectedDate = MutableStateFlow<Long?>(null)
    val selectedDate: StateFlow<Long?> = _selectedDate.asStateFlow()

    // Selected date range (for date range filter)
    private val _selectedDateRange = MutableStateFlow<Pair<Long, Long>?>(null)
    val selectedDateRange: StateFlow<Pair<Long, Long>?> = _selectedDateRange.asStateFlow()

    // Total incoming money amounts
    private val _totalIncomingAmount = MutableStateFlow<Double?>(null)
    val totalIncomingAmount: StateFlow<Double?> = _totalIncomingAmount.asStateFlow()

    // Total incoming money by app
    private val _incomingAmountByApp = MutableStateFlow<Map<String, Double>>(emptyMap())
    val incomingAmountByApp: StateFlow<Map<String, Double>> = _incomingAmountByApp.asStateFlow()

    // Daily incoming money (resets at midnight)
    private val _dailyIncomingAmount = MutableStateFlow<Double?>(null)
    val dailyIncomingAmount: StateFlow<Double?> = _dailyIncomingAmount.asStateFlow()

    // For debugging - list of all incoming transactions with descriptions
    private val _incomingTransactions = MutableStateFlow<List<Pair<String, Double>>>(emptyList())
    val incomingTransactions: StateFlow<List<Pair<String, Double>>> = _incomingTransactions.asStateFlow()

    // Notifications with all filters applied
    @OptIn(ExperimentalCoroutinesApi::class)
    val notifications = combine(
        _searchQuery.debounce(300),
        _selectedAppTypes,
        _dateFilterType,
        _selectedDate,
        _selectedDateRange
    ) { query, appTypes, dateFilterType, singleDate, dateRange ->
        FilterParams(query, appTypes, dateFilterType, singleDate, dateRange)
    }.flatMapLatest { filterParams ->
        applyFilters(filterParams)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // User preferences
    val autoReadEnabled = userPreferences.autoReadEnabled
    val autoDeleteDays = userPreferences.autoDeleteDays

    init {
        // Start collecting incoming amounts
        viewModelScope.launch {
            // Total incoming amount without filters
            notificationRepository.getTotalIncomingAmount().collect { amount ->
                _totalIncomingAmount.value = amount
            }
        }

        viewModelScope.launch {
            // Daily incoming amount by app
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

            notificationRepository.getTotalIncomingAmountByAppAndDateRange(startTime, endTime).collect { amountByApp ->
                _incomingAmountByApp.value = amountByApp
            }
        }

        viewModelScope.launch {
            // Daily incoming amount (resets at midnight)
            notificationRepository.getDailyIncomingAmount().collect { amount ->
                _dailyIncomingAmount.value = amount
            }
        }

        // Load the individual transactions for debugging
        loadIncomingTransactions()

        // Apply filters to incoming amount calculations
        launchFilteredIncomingAmountCalculation()
    }

    private fun loadIncomingTransactions() {
        viewModelScope.launch {
            notificationRepository.getAllIncomingTransactions().collect { notifications ->
                _incomingTransactions.value = notifications.map { notification ->
                    // Create a descriptive string + amount pair
                    val description = when {
                        notification.packageName == "com.wave.personal" ->
                            "Wave: ${notification.title.take(30)}..."
                        notification.packageName == "com.wave.business" ->
                            "Wave Business: ${notification.title.take(30)}..."
                        notification.packageName.contains("messaging") &&
                                notification.title.contains("OrangeMoney", ignoreCase = true) ->
                            "Orange Money: ${notification.text.take(30)}..."
                        notification.packageName.contains("messaging") &&
                                notification.title.contains("Mixx", ignoreCase = true) ->
                            "Mixx: ${notification.text.take(30)}..."
                        else -> "${notification.appName}: ${notification.title.take(30)}..."
                    }

                    // FIXED: Ensure we get the whole number amount (no decimals)
                    val amount = notification.amount ?: 0.0

                    Pair(description, amount)
                }
            }
        }
    }

    private fun applyFilters(filterParams: FilterParams): Flow<List<Notification>> {
        // Start with all active notifications as the base flow
        val baseFlow = notificationRepository.getAllActiveNotifications()

        // Apply app type filter if any app types are selected
        val appFilteredFlow = if (filterParams.appTypes.isNotEmpty()) {
            baseFlow.map { allNotifications ->
                allNotifications.filter { notification ->
                    // Check if the notification matches any of the selected app types
                    filterParams.appTypes.any { appType ->
                        matchesAppType(notification, appType)
                    }
                }
            }
        } else {
            baseFlow
        }

        // Apply date filters to the app-filtered flow
        val dateFilteredFlow = when (filterParams.dateFilterType) {
            DateFilterType.SINGLE_DAY -> {
                if (filterParams.singleDate != null) {
                    // Create a range for the entire day
                    val calendar = Calendar.getInstance().apply {
                        timeInMillis = filterParams.singleDate
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                    }
                    val startOfDay = calendar.timeInMillis

                    calendar.apply {
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                    }
                    val endOfDay = calendar.timeInMillis

                    // Filter notifications by date range
                    appFilteredFlow.map { notifications ->
                        notifications.filter { notification ->
                            notification.timestamp in startOfDay..endOfDay
                        }
                    }
                } else {
                    appFilteredFlow
                }
            }
            DateFilterType.DATE_RANGE -> {
                if (filterParams.dateRange != null) {
                    // Create a range from start of first day to end of last day
                    val startCalendar = Calendar.getInstance().apply {
                        timeInMillis = filterParams.dateRange.first
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                    }

                    val endCalendar = Calendar.getInstance().apply {
                        timeInMillis = filterParams.dateRange.second
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                    }

                    val startTime = startCalendar.timeInMillis
                    val endTime = endCalendar.timeInMillis

                    // Filter notifications by date range
                    appFilteredFlow.map { notifications ->
                        notifications.filter { notification ->
                            notification.timestamp in startTime..endTime
                        }
                    }
                } else {
                    appFilteredFlow
                }
            }
            else -> appFilteredFlow // ALL_TIME
        }

        // Finally, apply the search query filter if it exists
        return if (filterParams.query.isNotEmpty()) {
            dateFilteredFlow.map { notifications ->
                notifications.filter { notification ->
                    notification.title.contains(filterParams.query, ignoreCase = true) ||
                            notification.text.contains(filterParams.query, ignoreCase = true) ||
                            notification.appName.contains(filterParams.query, ignoreCase = true)
                }
            }
        } else {
            dateFilteredFlow
        }
    }

    private fun launchFilteredIncomingAmountCalculation() {
        // We'll use the same filter parameters to calculate filtered incoming amounts
        combine(
            _dateFilterType,
            _selectedDate,
            _selectedDateRange
        ) { dateFilterType, singleDate, dateRange ->
            Triple(dateFilterType, singleDate, dateRange)
        }.onEach { (dateFilterType, singleDate, dateRange) ->
            when (dateFilterType) {
                DateFilterType.SINGLE_DAY -> {
                    if (singleDate != null) {
                        // Create a range for the entire day
                        val calendar = Calendar.getInstance().apply {
                            timeInMillis = singleDate
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                        }
                        val startOfDay = calendar.timeInMillis

                        calendar.apply {
                            set(Calendar.HOUR_OF_DAY, 23)
                            set(Calendar.MINUTE, 59)
                            set(Calendar.SECOND, 59)
                        }
                        val endOfDay = calendar.timeInMillis

                        // Get income for this day
                        notificationRepository.getTotalIncomingAmountByDateRange(startOfDay, endOfDay)
                            .collect { amount ->
                                _totalIncomingAmount.value = amount
                            }

                        // Get income by app for this day
                        notificationRepository.getTotalIncomingAmountByAppAndDateRange(startOfDay, endOfDay)
                            .collect { amountByApp ->
                                _incomingAmountByApp.value = amountByApp
                            }

                        // Also update debug transactions
                        notificationRepository.getIncomingTransactionsByDateRange(startOfDay, endOfDay)
                            .collect { notifications ->
                                updateDebugTransactions(notifications)
                            }
                    }
                }
                DateFilterType.DATE_RANGE -> {
                    if (dateRange != null) {
                        // Create a range from start of first day to end of last day
                        val startCalendar = Calendar.getInstance().apply {
                            timeInMillis = dateRange.first
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                        }

                        val endCalendar = Calendar.getInstance().apply {
                            timeInMillis = dateRange.second
                            set(Calendar.HOUR_OF_DAY, 23)
                            set(Calendar.MINUTE, 59)
                            set(Calendar.SECOND, 59)
                        }

                        val startTime = startCalendar.timeInMillis
                        val endTime = endCalendar.timeInMillis

                        // Get income for this date range
                        notificationRepository.getTotalIncomingAmountByDateRange(startTime, endTime)
                            .collect { amount ->
                                _totalIncomingAmount.value = amount
                            }

                        // Get income by app for this date range
                        notificationRepository.getTotalIncomingAmountByAppAndDateRange(startTime, endTime)
                            .collect { amountByApp ->
                                _incomingAmountByApp.value = amountByApp
                            }

                        // Also update debug transactions
                        notificationRepository.getIncomingTransactionsByDateRange(startTime, endTime)
                            .collect { notifications ->
                                updateDebugTransactions(notifications)
                            }
                    }
                }
                else -> {
                    // ALL_TIME - use the unfiltered total
                    notificationRepository.getTotalIncomingAmount().collect { amount ->
                        _totalIncomingAmount.value = amount
                    }

                    notificationRepository.getTotalIncomingAmountByApp().collect { amountByApp ->
                        _incomingAmountByApp.value = amountByApp
                    }

                    // Also load all debug transactions
                    loadIncomingTransactions()
                }
            }
        }.launchIn(viewModelScope)
    }

    // Helper to update debug transactions based on filtered notifications
    private fun updateDebugTransactions(notifications: List<Notification>) {
        _incomingTransactions.value = notifications.map { notification ->
            // Create a descriptive string + amount pair
            val description = when {
                notification.packageName == "com.wave.personal" ->
                    "Wave: ${notification.title.take(30)}..."
                notification.packageName == "com.wave.business" ->
                    "Wave Business: ${notification.title.take(30)}..."
                notification.packageName.contains("messaging") &&
                        notification.title.contains("OrangeMoney", ignoreCase = true) ->
                    "Orange Money: ${notification.text.take(30)}..."
                notification.packageName.contains("messaging") &&
                        notification.title.contains("Mixx", ignoreCase = true) ->
                    "Mixx: ${notification.text.take(30)}..."
                else -> "${notification.appName}: ${notification.title.take(30)}..."
            }

            // FIXED: Ensure we get the whole number amount (no decimals)
            val amount = notification.amount ?: 0.0

            Pair(description, amount)
        }
    }

    // Helper function to check if a notification matches an app type
    private fun matchesAppType(notification: Notification, appType: AppType): Boolean {
        return when (appType) {
            AppType.WAVE_PERSONAL ->
                notification.packageName == "com.wave.personal" &&
                        !notification.title.contains("business", ignoreCase = true)

            AppType.WAVE_BUSINESS ->
                notification.packageName == "com.wave.business" ||
                        (notification.packageName == "com.wave.personal" &&
                                notification.title.contains("business", ignoreCase = true))

            AppType.ORANGE_MONEY ->
                notification.packageName == "com.google.android.apps.messaging" &&
                        notification.title.contains("OrangeMoney", ignoreCase = true)

            AppType.MIXX ->
                notification.packageName == "com.google.android.apps.messaging" &&
                        notification.title.contains("Mixx by Yas", ignoreCase = true)
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedAppTypes(appTypes: Set<AppType>) {
        _selectedAppTypes.value = appTypes
    }

    fun addAppType(appType: AppType) {
        _selectedAppTypes.value = _selectedAppTypes.value + appType
    }

    fun removeAppType(appType: AppType) {
        _selectedAppTypes.value = _selectedAppTypes.value - appType
    }

    fun toggleAppType(appType: AppType) {
        val currentAppTypes = _selectedAppTypes.value
        _selectedAppTypes.value = if (currentAppTypes.contains(appType)) {
            currentAppTypes - appType
        } else {
            currentAppTypes + appType
        }
    }

    fun setDateFilterType(dateFilterType: DateFilterType) {
        _dateFilterType.value = dateFilterType
    }

    fun setSelectedDate(date: Long?) {
        if (date != null) {
            // Normalize the date to start of day (00:00:00)
            val calendar = Calendar.getInstance().apply {
                timeInMillis = date
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            _selectedDate.value = calendar.timeInMillis
        } else {
            _selectedDate.value = null
        }
    }

    fun setSelectedDateRange(dateRange: Pair<Long, Long>?) {
        if (dateRange != null) {
            // Normalize start date to start of day (00:00:00)
            val startCalendar = Calendar.getInstance().apply {
                timeInMillis = dateRange.first
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            // Normalize end date to end of day (23:59:59)
            val endCalendar = Calendar.getInstance().apply {
                timeInMillis = dateRange.second
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }

            _selectedDateRange.value = Pair(startCalendar.timeInMillis, endCalendar.timeInMillis)
        } else {
            _selectedDateRange.value = null
        }
    }

    fun moveToTrash(notification: Notification) {
        viewModelScope.launch {
            notificationRepository.moveToTrash(notification.id)
        }
    }

    fun restoreFromTrash(notification: Notification) {
        viewModelScope.launch {
            notificationRepository.restoreFromTrash(notification.id)
        }
    }

    fun readNotification(notification: Notification) {
        // For Orange Money notifications that don't have "recu" or "reçu", use manuallySpeak
        if (notification.packageName == "com.google.android.apps.messaging" &&
            notification.title.contains("OrangeMoney", ignoreCase = true) &&
            !notification.text.contains("recu", ignoreCase = true) &&
            !notification.text.contains("reçu", ignoreCase = true)) {

            ttsService.manuallySpeak(notification)
        } else {
            // For all other notifications, use the regular speakNotification method
            ttsService.speakNotification(notification)
        }

        // Mark as read if it's not already
        if (!notification.isRead) {
            viewModelScope.launch {
                notificationRepository.markAsRead(notification.id, true)
            }
        }
    }

    fun stopReading() {
        ttsService.stop()
    }

    fun updateAutoReadSetting(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setAutoReadEnabled(enabled)
        }
    }

    fun updateAutoDeleteDays(days: Int) {
        viewModelScope.launch {
            userPreferences.setAutoDeleteDays(days)
        }
    }

    // Select today for single day filter
    fun selectToday() {
        val calendar = Calendar.getInstance()
        setSelectedDate(calendar.timeInMillis)
    }

    // Select current week for date range filter
    fun selectCurrentWeek() {
        val calendar = Calendar.getInstance()
        val endDate = calendar.timeInMillis

        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val startDate = calendar.timeInMillis

        setSelectedDateRange(Pair(startDate, endDate))
    }

    // Select current month for date range filter
    fun selectCurrentMonth() {
        val calendar = Calendar.getInstance()
        val endDate = calendar.timeInMillis

        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val startDate = calendar.timeInMillis

        setSelectedDateRange(Pair(startDate, endDate))
    }

    fun readDailyTotal(text: String) {
        ttsService.speakText(text)
    }

    // Add this to HomeViewModel.kt
    // In HomeViewModel.kt
    private val _fixOperationResult = MutableStateFlow<String?>(null)
    val fixOperationResult: StateFlow<String?> = _fixOperationResult


    override fun onCleared() {
        ttsService.stop()
        super.onCleared()
    }
}

// Helper class to pass multiple filter parameters
data class FilterParams(
    val query: String,
    val appTypes: Set<AppType>,
    val dateFilterType: DateFilterType,
    val singleDate: Long?,
    val dateRange: Pair<Long, Long>?
)
