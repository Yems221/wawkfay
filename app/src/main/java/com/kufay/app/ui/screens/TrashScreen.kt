package com.kufay.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kufay.app.ui.components.NotificationCard
import com.kufay.app.ui.models.NotificationDetailDialog
import com.kufay.app.ui.viewmodels.TrashViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    onNavigateBack: () -> Unit,
    viewModel: TrashViewModel = hiltViewModel()
) {
    val deletedNotifications by viewModel.deletedNotifications.collectAsState(initial = emptyList())
    val autoDeleteDays by viewModel.autoDeleteDays.collectAsState(initial = 1)

    // States
    val selectedNotification = remember { mutableStateOf<com.kufay.app.data.db.entities.Notification?>(null) }
    val showDeleteSettingsDialog = remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF5F5F5)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = onNavigateBack) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                            Column {
                                Text(
                                    "Trash",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Deleted Notifications",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { showDeleteSettingsDialog.value = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Trash Settings")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
            containerColor = Color(0xFFF5F5F5)
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (deletedNotifications.isEmpty()) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.LightGray
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Your trash is empty",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = Color.Gray
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        // Auto-delete info banner
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = "Items will be permanently deleted after ${
                                        when (autoDeleteDays) {
                                            1 -> "1 day"
                                            else -> "$autoDeleteDays days"
                                        }
                                    }",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Deleted notifications list
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(deletedNotifications) { notification ->
                                NotificationCard(
                                    notification = notification,
                                    isReading = false,
                                    onPlayPauseClick = { /* No playback in Trash */ },
                                    onDeleteClick = { /* No delete in Trash */ },
                                    onRestoreClick = {
                                        viewModel.restoreFromTrash(notification)
                                    },
                                    onCardClick = {
                                        selectedNotification.value = notification
                                    },
                                    inTrash = true
                                )
                            }
                        }
                    }
                }

                // Delete Settings Dialog
                if (showDeleteSettingsDialog.value) {
                    AlertDialog(
                        onDismissRequest = { showDeleteSettingsDialog.value = false },
                        title = { Text("Auto-delete Settings") },
                        text = {
                            Column {
                                Text("Choose when to permanently delete items in trash:")
                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    RadioButton(
                                        selected = autoDeleteDays == 1,
                                        onClick = { viewModel.updateAutoDeleteDays(1) }
                                    )
                                    Text("After 1 day", modifier = Modifier.padding(start = 8.dp))
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    RadioButton(
                                        selected = autoDeleteDays == 7,
                                        onClick = { viewModel.updateAutoDeleteDays(7) }
                                    )
                                    Text("After 7 days", modifier = Modifier.padding(start = 8.dp))
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    RadioButton(
                                        selected = autoDeleteDays == 30,
                                        onClick = { viewModel.updateAutoDeleteDays(30) }
                                    )
                                    Text("After 30 days", modifier = Modifier.padding(start = 8.dp))
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showDeleteSettingsDialog.value = false }) {
                                Text("Done")
                            }
                        }
                    )
                }

                // Notification detail dialog
                selectedNotification.value?.let { notification ->
                    NotificationDetailDialog(
                        notification = notification,
                        onDismiss = { selectedNotification.value = null }
                    )
                }
            }
        }
    }
}