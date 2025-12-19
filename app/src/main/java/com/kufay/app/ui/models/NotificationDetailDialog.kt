package com.kufay.app.ui.models

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.kufay.app.data.db.entities.Notification
import java.text.SimpleDateFormat
import java.util.*
import com.kufay.app.ui.theme.Lato // Adjust the import path to match where you defined Lato


@Composable
fun NotificationDetailDialog(
    notification: Notification,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.Start
            ) {
                // Title from first line or app name
                val title = notification.text.split("\n").firstOrNull() ?: notification.title
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = Lato,
                        fontWeight = FontWeight.W500 // This is equivalent to 400

                    ),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Extract and display key information
                val lines = notification.text.split("\n")
                lines.drop(1).forEach { line ->
                    if (line.isNotEmpty()) {
                        Text(
                            text = line,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }

                // Display the amount with special handling for Wave notifications
                val displayAmount = when {
                    // For Wave Personal payment notifications
                    notification.packageName == "com.wave.personal" &&
                            notification.title.contains("Paiement réussi", ignoreCase = true) -> {
                        // Extract the payment amount directly from the notification text
                        val paymentPattern = """Vous avez payé (\d+(?:\.\d+)?F)""".toRegex()
                        val paymentMatch = paymentPattern.find(notification.text)

                        if (paymentMatch != null) {
                            "${paymentMatch.groupValues[1]} Franc CFA"
                        } else {
                            // Try alternative pattern
                            val altPattern = """payé (\d+(?:\.\d+)?F)""".toRegex()
                            val altMatch = altPattern.find(notification.text)

                            if (altMatch != null) {
                                "${altMatch.groupValues[1]} Franc CFA"
                            } else {
                                // Fallback to formatter
                                NotificationFormatter.formatAmount(
                                    notification.amount,
                                    notification.currency
                                )
                            }
                        }
                    }

                    // For Wave Personal transfer sent notifications
                    notification.packageName == "com.wave.personal" &&
                            notification.title.contains("Transfert réussi", ignoreCase = true) -> {
                        // Extract the transfer amount directly from the notification text
                        val transferPattern = """Vous avez envoyé (\d+(?:\.\d+)?F)""".toRegex()
                        val transferMatch = transferPattern.find(notification.text)

                        if (transferMatch != null) {
                            "${transferMatch.groupValues[1]} Franc CFA"
                        } else {
                            // Fallback to formatter
                            NotificationFormatter.formatAmount(
                                notification.amount,
                                notification.currency
                            )
                        }
                    }

                    // For Wave Business encaissement notifications
                    notification.packageName == "com.wave.business" &&
                            notification.text.contains("sur votre encaissement de", ignoreCase = true) -> {
                        // Extract the encaissement amount directly from the notification text
                        val encaissementPattern = """sur votre encaissement de (\d+(?:\.\d+)?F?)""".toRegex()
                        val encaissementMatch = encaissementPattern.find(notification.text)

                        if (encaissementMatch != null) {
                            var amountText = encaissementMatch.groupValues[1]
                            if (!amountText.endsWith("F", ignoreCase = true)) {
                                amountText += "F"
                            }
                            "$amountText Franc CFA"
                        } else {
                            // Fallback to formatter
                            NotificationFormatter.formatAmount(
                                notification.amount,
                                notification.currency
                            )
                        }
                    }

                    // For other Wave notifications, try to extract amount with general patterns
                    notification.packageName == "com.wave.personal" ||
                            notification.packageName == "com.wave.business" -> {
                        // Try to find the amount pattern in the text
                        val amountPattern = """(\d+\.\d+F)""".toRegex()
                        val amountMatch = amountPattern.find(notification.text)

                        if (amountMatch != null) {
                            val exactAmount = amountMatch.groupValues[1] // e.g., "36.900F"
                            "$exactAmount Franc CFA"
                        } else if (notification.amount != null) {
                            // Fallback to standard amount
                            NotificationFormatter.formatAmount(
                                notification.amount,
                                notification.currency
                            )
                        } else {
                            ""
                        }
                    }

                    // For non-Wave notifications, use the standard formatter
                    notification.amount != null -> {
                        NotificationFormatter.formatAmount(
                            notification.amount,
                            notification.currency
                        )
                    }

                    else -> ""
                }

                // Display the amount if we have one
                if (displayAmount.isNotEmpty()) {
                    Text(
                        text = displayAmount,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                // Format and display date
                val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                val formattedDate = dateFormat.format(Date(notification.timestamp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }

                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "Close",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}