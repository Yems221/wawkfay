package com.kufay.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kufay.app.ui.models.AppType
import com.kufay.app.ui.theme.AppTheme
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun FilterSection(
    selectedAppTypes: Set<AppType>,
    onAppTypesSelected: (Set<AppType>) -> Unit,
    dateFilterType: DateFilterType,
    onDateFilterTypeSelected: (DateFilterType) -> Unit,
    selectedDate: Long?,
    selectedDateRange: Pair<Long, Long>?,
    onDateSelected: (Long?) -> Unit,
    onDateRangeSelected: (Pair<Long, Long>?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        // Title
        Text(
            text = "Filters",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // First row: All, Wave, Orange Money
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // "All" filter
            FilterChip(
                label = "All",
                isSelected = selectedAppTypes.isEmpty(),
                color = Color.LightGray,
                onClick = { onAppTypesSelected(emptySet()) }
            )

            // Wave (formerly Wave Personal)
            FilterChip(
                label = "Wave",
                isSelected = selectedAppTypes.contains(AppType.WAVE_PERSONAL),
                color = AppTheme.colors.wavePersonal,
                onClick = {
                    onAppTypesSelected(toggleAppType(selectedAppTypes, AppType.WAVE_PERSONAL))
                }
            )

            // Orange Money
            FilterChip(
                label = "Orange Money",
                isSelected = selectedAppTypes.contains(AppType.ORANGE_MONEY),
                color = AppTheme.colors.orangeMoney,
                onClick = {
                    onAppTypesSelected(toggleAppType(selectedAppTypes, AppType.ORANGE_MONEY))
                }
            )
        }

        // Second row: Wave Business, Mixx
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Wave Business
            FilterChip(
                label = "Wave Business",
                isSelected = selectedAppTypes.contains(AppType.WAVE_BUSINESS),
                color = AppTheme.colors.waveBusiness,
                onClick = {
                    onAppTypesSelected(toggleAppType(selectedAppTypes, AppType.WAVE_BUSINESS))
                }
            )

            // Mixx by Yas
            FilterChip(
                label = "Mixx by Yas",
                isSelected = selectedAppTypes.contains(AppType.MIXX),
                color = AppTheme.colors.mixx,
                onClick = {
                    onAppTypesSelected(toggleAppType(selectedAppTypes, AppType.MIXX))
                }
            )

            // Spacer to fill remaining space
            Spacer(modifier = Modifier.weight(1f))
        }

        // Date Range filters
        Text(
            text = "Date Range",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )

        // Date filter row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // All Time
            DateFilterChip(
                label = "All Time",
                isSelected = dateFilterType == DateFilterType.ALL_TIME,
                onClick = { onDateFilterTypeSelected(DateFilterType.ALL_TIME) }
            )

            // Single Day
            DateFilterChip(
                label = "Single Day",
                isSelected = dateFilterType == DateFilterType.SINGLE_DAY,
                onClick = { onDateFilterTypeSelected(DateFilterType.SINGLE_DAY) }
            )

            // Date Range
            DateFilterChip(
                label = "Date Range",
                isSelected = dateFilterType == DateFilterType.DATE_RANGE,
                onClick = { onDateFilterTypeSelected(DateFilterType.DATE_RANGE) }
            )
        }

        // Quick date selection options
        if (dateFilterType == DateFilterType.SINGLE_DAY) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Today button
                QuickDateButton(
                    text = "Today",
                    onClick = {
                        val today = Calendar.getInstance().timeInMillis
                        onDateSelected(today)
                    },
                    modifier = Modifier.weight(1f)
                )

                // Yesterday button
                QuickDateButton(
                    text = "Yesterday",
                    onClick = {
                        val yesterday = Calendar.getInstance()
                        yesterday.add(Calendar.DAY_OF_YEAR, -1)
                        onDateSelected(yesterday.timeInMillis)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        } else if (dateFilterType == DateFilterType.DATE_RANGE) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // This Week button
                QuickDateButton(
                    text = "This Week",
                    onClick = {
                        val calendar = Calendar.getInstance()
                        val endDate = calendar.timeInMillis

                        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                        val startDate = calendar.timeInMillis

                        onDateRangeSelected(Pair(startDate, endDate))
                    },
                    modifier = Modifier.weight(1f)
                )

                // This Month button
                QuickDateButton(
                    text = "This Month",
                    onClick = {
                        val calendar = Calendar.getInstance()
                        val endDate = calendar.timeInMillis

                        calendar.set(Calendar.DAY_OF_MONTH, 1)
                        val startDate = calendar.timeInMillis

                        onDateRangeSelected(Pair(startDate, endDate))
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Date selector bars
        Spacer(modifier = Modifier.height(8.dp))

        // Single day selector bar
        if (dateFilterType == DateFilterType.SINGLE_DAY) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFE0E0E0))
                    .clickable { onDateSelected(selectedDate ?: Calendar.getInstance().timeInMillis) },
                contentAlignment = Alignment.Center
            ) {
                if (selectedDate != null) {
                    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val formattedDate = dateFormat.format(Date(selectedDate))
                    Text(
                        text = "Date: $formattedDate - Appuyer pour changer",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                } else {
                    Text(
                        text = "Choisir une date",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }
        }

        // Date range selector bar
        if (dateFilterType == DateFilterType.DATE_RANGE) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFE0E0E0))
                    .clickable {
                        val now = Calendar.getInstance().timeInMillis
                        onDateRangeSelected(selectedDateRange ?: Pair(now, now))
                    },
                contentAlignment = Alignment.Center
            ) {
                if (selectedDateRange != null) {
                    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val formattedStartDate = dateFormat.format(Date(selectedDateRange.first))
                    val formattedEndDate = dateFormat.format(Date(selectedDateRange.second))
                    Text(
                        text = "Du $formattedStartDate au $formattedEndDate - Appuyer pour changer",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                } else {
                    Text(
                        text = "Sélectionner une période",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun FilterChip(
    label: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(if (isSelected) color else Color.White)
            .border(
                width = 1.dp,
                color = color,
                shape = RoundedCornerShape(24.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else color,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun DateFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Color(0xFFE0E0E0) else Color.White)
            .border(
                width = 1.dp,
                color = Color(0xFFD0D0D0),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.Black,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun QuickDateButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .border(
                width = 1.dp,
                color = Color(0xFFD0D0D0),
                shape = RoundedCornerShape(4.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

// Helper function to toggle app types in a set
private fun toggleAppType(currentSet: Set<AppType>, appType: AppType): Set<AppType> {
    return if (currentSet.contains(appType)) {
        // Remove the app type if it's already selected
        currentSet.filter { it != appType }.toSet()
    } else {
        // Add the app type if it's not already selected
        currentSet + appType
    }
}

// Define the date filter types
enum class DateFilterType {
    ALL_TIME,
    SINGLE_DAY,
    DATE_RANGE
}