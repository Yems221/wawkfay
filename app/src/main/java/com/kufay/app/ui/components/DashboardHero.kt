package com.kufay.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kufay.app.ui.theme.AppTheme
import com.kufay.app.ui.theme.Lato
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


@Composable
fun DashboardHero(
    modifier: Modifier = Modifier,
    appColor: Color,
    totalIncomingAmount: Double? = null,
    incomingAmountByApp: Map<String, Double> = emptyMap(),
    dailyIncomingAmount: Double? = null,
    selectedDate: Long? = null, // New Parameter
    isCollapsed: Boolean = false,
    incomingTransactions: List<Pair<String, Double>> = emptyList() // Format: (description, amount)
    ) {
    // State variables for expanded status and amount visibility

    var isExpanded by remember { mutableStateOf(false) }

    var isAmountVisible by remember { mutableStateOf(false) }

    // Debug state - controls visibility of the debug popup

    var showDebugPopup by remember { mutableStateOf(false) }

    // When in collapsed mode from scrolling, force the dashboard to be collapsed

    val actuallyExpanded = isExpanded && !isCollapsed

    // Transition for expandable behavior when scrolling

    val heightTransition =
        updateTransition(targetState = actuallyExpanded, label = "Expansion Transition")

    // Animation specs for smooth transitions

    val heightSpec = spring<Float>(

        dampingRatio = Spring.DampingRatioMediumBouncy,

        stiffness = Spring.StiffnessLow

    )

    // Check if the selected date is today

    val isToday = selectedDate == null || isSameDay(selectedDate, System.currentTimeMillis())


    // Format the selected date if not today

    val dateText = selectedDate?.let {

        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        "Données du ${dateFormat.format(Date(it))}"

    }

    // Total Amount Row

    val formattedTotalAmount = dailyIncomingAmount?.let {

        formatAmount(it)

    } ?: "0 Franc CFA"

    Text(

        text = if (isAmountVisible) formattedTotalAmount else "*****",

        style = if (isCollapsed) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleMedium,

        color = Color.White,

        fontWeight = FontWeight.Bold,

        modifier = Modifier.padding(end = 8.dp)

    )

    Column(

        modifier = modifier

            .fillMaxWidth()

            .background(appColor)

            .clickable { isExpanded = !isExpanded }

    ) {

        // Date indication for filtered date

        if (!isToday) {

            Text(

                text = dateText ?: "",

                style = MaterialTheme.typography.bodySmall,

                color = Color.White.copy(alpha = 0.7f),

                modifier = Modifier.padding(start = 16.dp, top = 8.dp)

            )

        }

        // Title Row - Only visible when not collapsed

        AnimatedVisibility(

            visible = !isCollapsed,

            enter = expandVertically() + fadeIn(),

            exit = shrinkVertically() + fadeOut()

        ) {

            Text(

                text = if (isToday) "Ku la fay ?" else "Ku la fay ? (Filtré)",

                style = MaterialTheme.typography.headlineSmall,

                color = Color.White,

                fontWeight = FontWeight.Bold,

                modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp)

            )

        }


        // Total Amount Row
        val formattedTotalAmount = dailyIncomingAmount?.let {
            formatAmount(it)
        } ?: "0 Franc CFA"

        Text(
            text = if (isAmountVisible) formattedTotalAmount else "*****",
            style = if (isCollapsed) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(end = 8.dp)
        )

        Column(
            modifier = modifier
                .fillMaxWidth()
                .background(appColor)
                .clickable { isExpanded = !isExpanded }
        ) {
            // Title Row - Only visible when not collapsed
            AnimatedVisibility(
                visible = !isCollapsed,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Text(
                    text = "Ku la fay ?",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(
                        start = 16.dp,
                        top = 16.dp,
                        end = 16.dp,
                        bottom = 8.dp
                    )
                )
            }

            // Total Amount Row - Always visible, but formatted based on collapse state
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = if (isCollapsed) 8.dp else 0.dp,
                        bottom = if (actuallyExpanded) 8.dp else if (isCollapsed) 8.dp else 16.dp
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Icon and Label
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = "Incoming",
                        tint = Color.White,
                        modifier = Modifier
                            .size(if (isCollapsed) 20.dp else 24.dp)
                            .background(
                                color = Color.White.copy(alpha = 0.3f),
                                shape = CircleShape
                            )
                            .padding(if (isCollapsed) 3.dp else 4.dp)
                    )

                    Text(
                        text = "Total Reçu \n Aujourd'hui",
                        style = if (isCollapsed) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                // Amount, Debug Button and Visibility Toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Display amount or stars based on visibility
                    val formattedTotalAmount = dailyIncomingAmount?.let {
                        formatAmount(it)
                    } ?: "0 Franc CFA"

                    Text(
                        text = if (isAmountVisible) formattedTotalAmount else "*****",
                        style = if (isCollapsed) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 8.dp)
                    )

                    // Debug button - small icon to open the amounts popup
                    if (!isCollapsed) {
                        IconButton(
                            onClick = { showDebugPopup = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.BugReport,
                                contentDescription = "Debug Amounts",
                                tint = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }

                    // Eye toggle button
                    IconButton(
                        onClick = { isAmountVisible = !isAmountVisible },
                        modifier = Modifier.size(if (isCollapsed) 24.dp else 28.dp)
                    ) {
                        Icon(
                            imageVector = if (isAmountVisible) Icons.Default.Visibility else Icons.Outlined.VisibilityOff,
                            contentDescription = if (isAmountVisible) "Hide Amount" else "Show Amount",
                            tint = Color.White
                        )
                        Icon(

                            imageVector = Icons.Default.PlayArrow,

                            contentDescription = "Read Total du Jour",

                            tint = Color.White,

                            modifier = Modifier.padding(horizontal = 8.dp)

                                .size(if (isCollapsed) 20.dp else 24.dp)

                                .background(

                                    color = Color.White.copy(alpha = 0.3f),

                                    shape = CircleShape

                                )

                                .padding(if (isCollapsed) 3.dp else 4.dp)

                        )
                    }
                }
            }

            // Expandable Content
            AnimatedVisibility(
                visible = actuallyExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Daily total
                    val formattedDailyAmount = dailyIncomingAmount?.let {
                        formatAmount(it)
                    } ?: "0 Franc CFA"


                    // By app breakdown
                    if (incomingAmountByApp.isNotEmpty()) {
                        Divider(
                            color = Color.White.copy(alpha = 0.3f),
                            thickness = 1.dp,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        Text(
                            text = "Par Application",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )

                        // Table-like layout for app breakdown
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.1f))
                        ) {
                            // List app breakdowns
                            incomingAmountByApp.forEach { (packageName, amount) ->
                                val appName = getAppNameFromPackage(packageName)
                                val formattedAmount = formatAmount(amount)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp, horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = appName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Normal
                                    )

                                    Text(
                                        text = if (isAmountVisible) formattedAmount else "*****",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        textAlign = TextAlign.End
                                    )
                                }

                                // Add divider between items (except after the last one)
                                if (packageName != incomingAmountByApp.keys.last()) {
                                    Divider(
                                        color = Color.White.copy(alpha = 0.1f),
                                        thickness = 0.5.dp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Debug popup dialog
            if (showDebugPopup) {
                AlertDialog(
                    onDismissRequest = { showDebugPopup = false },
                    title = { Text("Debug: Incoming Amounts") },
                    text = {
                        LazyColumn {
                            if (incomingTransactions.isEmpty()) {
                                item {
                                    Text("No incoming transactions found.")
                                }
                            } else {
                                items(incomingTransactions) { (description, amount) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = description,
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.weight(1f)
                                        )

                                        Text(
                                            text = formatAmount(amount),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Divider()
                                }

                                // Total amount
                                item {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "TOTAL",
                                            fontWeight = FontWeight.Bold
                                        )

                                        Text(
                                            text = formatAmount(incomingTransactions.sumOf { it.second }),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { showDebugPopup = false }
                        ) {
                            Text("Close")
                        }
                    }
                )
            }
        }

    }

// Helper function to check if two dates are on the same day

}



// Helper function to format amount with thousand separators
private fun formatAmount(amount: Double): String {
    return "${amount.toLong()} Franc CFA"
}

private fun isSameDay(date1: Long, date2: Long): Boolean {

    val cal1 = Calendar.getInstance().apply { timeInMillis = date1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = date2 }



    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&

            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)

}
// Helper function to get app name from package name
private fun getAppNameFromPackage(packageName: String): String {
    return when {
        packageName == "com.wave.personal" -> "Wave"
        packageName == "com.wave.business" -> "Wave Business"
        packageName.contains("OrangeMoney", ignoreCase = true) -> "Orange Money"
        packageName.contains("Mixx", ignoreCase = true) -> "Mixx by Yas"
        else -> packageName.split(".").last().capitalize()
    }
}
