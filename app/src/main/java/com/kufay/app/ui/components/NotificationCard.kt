package com.kufay.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kufay.app.data.db.entities.Notification
import com.kufay.app.ui.models.AppType
import com.kufay.app.ui.theme.AppTheme
import com.kufay.app.ui.theme.Lato
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NotificationCard(
    notification: Notification,
    isReading: Boolean,
    onPlayPauseClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onRestoreClick: () -> Unit,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier,
    inTrash: Boolean = false
) {
    var appType: AppType? = null

    try {
        appType = AppType.fromPackageName(notification.packageName, notification.title)
    } catch (e: IllegalArgumentException) {
        // Use a default if we can't determine the app type
    }

    // Use AppTheme.colors to get the app-specific color
    val appColor = when (appType) {
        AppType.WAVE_PERSONAL -> AppTheme.colors.wavePersonal
        AppType.WAVE_BUSINESS -> AppTheme.colors.waveBusiness
        AppType.ORANGE_MONEY -> AppTheme.colors.orangeMoney
        AppType.MIXX -> AppTheme.colors.mixx
        null -> Color.Gray
    }

    // Determine if this is an incoming notification
    val isIncoming = when {
        notification.packageName == "com.wave.personal" &&
                notification.text.contains("avez reçu", ignoreCase = true) -> true

        notification.packageName == "com.wave.business" &&
                (notification.text.contains("votre encaissement de", ignoreCase = true) ||
                        notification.text.contains("reçu", ignoreCase = true)) -> true

        notification.packageName == "com.google.android.apps.messaging" &&
                notification.title.contains("OrangeMoney", ignoreCase = true) &&
                (notification.text.contains("recu", ignoreCase = true) ||
                        notification.text.contains("reçu", ignoreCase = true)) -> true

        notification.packageName == "com.google.android.apps.messaging" &&
                notification.title.contains("Mixx by Yas", ignoreCase = true) &&
                (notification.text.contains("recu", ignoreCase = true) ||
                        notification.text.contains("reçu", ignoreCase = true)) -> true

        else -> false
    }

    // Display name for the app
    val displayName = if (appType == AppType.WAVE_PERSONAL) {
        "Wave"
    } else {
        appType?.displayName ?: notification.appName
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onCardClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(10.dp)
        ) {
            // Top row with app button and amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // App pill button with arrow if incoming
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(appColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = displayName,
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    // Incoming arrow next to app name
                    if (isIncoming) {
                        Icon(
                            imageVector = Icons.Default.ArrowDownward,
                            contentDescription = "Incoming",
                            tint = Color.White,
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .size(24.dp)
                                .background(
                                    color = appColor,
                                    shape = CircleShape
                                )
                                .padding(4.dp)
                        )
                    }
                }

                // Amount display
                if (notification.amount != null) {
                    Text(
                        text = "${notification.amount.toLong()} Franc CFA",
                        style = MaterialTheme.typography.titleMedium,
                        color = appColor,
                        fontWeight = FontWeight.Normal
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Title row
            // With this modified title logic that handles Orange Money and Mixx by Yas correctly:
            val dynamicTitle = when {
                // For incoming transactions from Orange Money and Mixx by Yas
                notification.packageName == "com.google.android.apps.messaging" &&
                        (notification.title.contains("OrangeMoney", ignoreCase = true) ||
                                notification.title.contains("Mixx by Yas", ignoreCase = true)) &&
                        notification.isIncomingTransaction -> "Transfert reçu"

                // For outgoing transfers from Orange Money - look for "transfert" and "vers"
                notification.packageName == "com.google.android.apps.messaging" &&
                        notification.title.contains("OrangeMoney", ignoreCase = true) &&
                        notification.text.contains("transfert", ignoreCase = true) &&
                        notification.text.contains("vers", ignoreCase = true) -> "Transfert envoyé"

                // For Mixx by Yas outgoing transfers - keep original condition
                notification.packageName == "com.google.android.apps.messaging" &&
                        notification.title.contains("Mixx by Yas", ignoreCase = true) &&
                        notification.text.contains("envoyé", ignoreCase = true) -> "Transfert envoyé"

                // For Orange Money payment operations
                notification.packageName == "com.google.android.apps.messaging" &&
                        notification.title.contains("OrangeMoney", ignoreCase = true) &&
                        notification.text.contains("operation", ignoreCase = true) -> "Paiement effectué"

                // Default fallback to original title
                else -> notification.title
            }

            Text(
                text = dynamicTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )


            // Text preview - commenté comme demandé
            /*
            Text(
                text = notification.text,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(vertical = 6.dp)
            )
            */

            // Bottom row with play/stop button and calendar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            )
            {
                // Bottom row with play/stop button and calendar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Play button on the left
                    if (!inTrash) {
                        IconButton(
                            onClick = onPlayPauseClick,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isReading) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = if (isReading) "Stop" else "Play",
                                tint = Color.White,
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(
                                        color = appColor,
                                        shape = CircleShape
                                    )
                                    .padding(4.dp)
                            )
                        }
                    }

                    // Timestamp with calendar icon in the middle
                    val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    val formattedDate = formatter.format(Date(notification.timestamp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.Gray
                        )
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }

                    // Delete/Restore button on the right
                    IconButton(
                        onClick = if (notification.isDeleted) onRestoreClick else onDeleteClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (notification.isDeleted) Icons.Default.Restore else Icons.Outlined.Delete,
                            contentDescription = if (notification.isDeleted) "Restore" else "Delete",
                            tint = Color.Red,
                            modifier = Modifier
                                .size(28.dp)
                                .background(
                                    color = Color.Red.copy(alpha = 0.1f),
                                    shape = CircleShape
                                )
                                .padding(4.dp)
                        )
                    }
                }
            }
        }
    }
}
// Helper function to format with dots for thousand separators
private fun formatWithDots(amountStr: String): String {
    val parts = amountStr.split(".")
    val integerPart = parts[0]

    // Only format if number is large enough to need separators
    if (integerPart.length <= 3) return integerPart

    val result = StringBuilder()
    for (i in integerPart.indices.reversed()) {
        result.insert(0, integerPart[i])
        if (i > 0 && (integerPart.length - i) % 3 == 0) {
            result.insert(0, ".")
        }
    }
    return result.toString()
}
