package com.kufay.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kufay.app.R
import com.kufay.app.ui.components.PinVerificationDialog
import com.kufay.app.ui.viewmodels.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val autoReadEnabled by viewModel.autoReadEnabled.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val ttsLanguage by viewModel.ttsLanguage.collectAsState()
    val useWolofRecordings by viewModel.useWolofRecordings.collectAsState()
    val ttsVoiceGender by viewModel.ttsVoiceGender.collectAsState()
    val ttsSpeechRate by viewModel.ttsSpeechRate.collectAsState()
    val ttsSpeechPitch by viewModel.ttsSpeechPitch.collectAsState()
    val appColor by viewModel.appMainColor.collectAsState()

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("Paramètres") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App Color Theme
            Text(
                text = "Couleur du Theme",
                style = MaterialTheme.typography.titleLarge
            )

            // Color selection section
            ColorSelection(
                selectedColor = appColor,
                onColorSelected = { viewModel.updateAppMainColor(it) }
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // App Language section
            Text(
                text = "Langue",
                style = MaterialTheme.typography.titleLarge
            )

            // App Language selection
            Row(
                modifier = Modifier.padding(top = 8.dp)
            ) {
                val languages = listOf("fr" to "Français",)
                // enleve la parenthese de fin et le debut de commentaire ci-dessous pour re-afficher le choix de langue de l'appli en anglais
              //      "en" to "English")


                languages.forEach { (code, name) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        RadioButton(
                            selected = appLanguage == code,
                            onClick = { viewModel.updateAppLanguage(code) }
                        )

                        Text(
                            text = name,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // TTS Settings section
            Text(
                text = "Paramètres de la voix",
                style = MaterialTheme.typography.titleLarge
            )

            // Auto-read toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Activer la lecture automatique des notifications",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )

                Switch(
                    checked = autoReadEnabled,
                    onCheckedChange = viewModel::updateAutoReadEnabled
                )
            }

            // TTS Language selection with flags
            Column {
                Text(
                    text = "Langue de la synthèse vocale",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Wolof option (first line)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    RadioButton(
                        selected = ttsLanguage == "wo",
                        onClick = { viewModel.updateTtsLanguage("wo") }
                    )

                    // Flag icon and text
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .border(0.5.dp, Color.Gray, CircleShape)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.senegal_flag),
                                contentDescription = "Senegal Flag",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Text(
                            text = "Wolof",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                // Test Wolof Recordings Button (visible seulement si la langue TTS est Wolof)
                if (ttsLanguage == "wo") {
                    Button(
                        onClick = {
                            // Afficher une boîte de dialogue avec la liste des enregistrements Wolof
                            // L'utilisateur pourra sélectionner un enregistrement à tester
                            // Cette partie sera implémentée avec un état pour la boîte de dialogue
                        },
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .fillMaxWidth()
                    ) {
                        Text("Test Wolof Recordings")
                    }
                }

                // French option (second line)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    RadioButton(
                        selected = ttsLanguage == "fr",
                        onClick = { viewModel.updateTtsLanguage("fr") }
                    )

                    // Flag icon and text
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .border(0.5.dp, Color.Gray, CircleShape)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.french_flag),
                                contentDescription = "French Flag",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Text(
                            text = "French",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                // English option (third line)
            //    Row(
            //        verticalAlignment = Alignment.CenterVertically
            //    ) {
            //        RadioButton(
            //            selected = ttsLanguage == "en",
            //            onClick = { viewModel.updateTtsLanguage("en") }
            //        )

                    // Flag icon and text
            //        Row(
            //            verticalAlignment = Alignment.CenterVertically,
            //            modifier = Modifier.padding(start = 4.dp)
            //        ) {
            //            Box(
            //                modifier = Modifier
            //                    .size(24.dp)
            //                    .clip(CircleShape)
            //                    .border(0.5.dp, Color.Gray, CircleShape)
            //            ) {
            //                Image(
            //                    painter = painterResource(id = R.drawable.english_flag),
            //                    contentDescription = "English Flag",
            //                    contentScale = ContentScale.Crop,
            //                    modifier = Modifier.fillMaxSize()
            //                )
            //            }

            //            Text(
            //                text = "English",
            //                modifier = Modifier.padding(start = 8.dp)
            //            )
            //        }
            //    }
            }

            // Wolof Recordings toggle (only visible when Wolof is selected)
            if (ttsLanguage == "wo") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text(
                        text = "Use Wolof recordings",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )

                    Switch(
                        checked = useWolofRecordings,
                        onCheckedChange = viewModel::updateUseWolofRecordings
                    )
                }
            }

            // Voice gender selection
            Column {
                Text(
                    text = "Voice",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 8.dp, top = 16.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        RadioButton(
                            selected = ttsVoiceGender == "female",
                            onClick = { viewModel.updateTtsVoiceGender("female") }
                        )

                        Text(
                            text = "Feminine",
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }

            //        Row(
            //            verticalAlignment = Alignment.CenterVertically
            //        ) {
            //            RadioButton(
            //                selected = ttsVoiceGender == "male",
            //                onClick = { viewModel.updateTtsVoiceGender("male") }
            //            )

            //            Text(
            //                text = "Male",
            //                modifier = Modifier.padding(start = 4.dp)
            //            )
            //        }
                }
            }

            // Speech rate slider
            Column(
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text(
                    text = "Speech Rate: ${ttsSpeechRate}x",
                    style = MaterialTheme.typography.bodyLarge
                )

                Slider(
                    value = ttsSpeechRate,
                    onValueChange = { viewModel.updateTtsSpeechRate(it) },
                    valueRange = 0.5f..2.0f,
                    steps = 6,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("0.5x", style = MaterialTheme.typography.labelSmall)
                    Text("1.0x", style = MaterialTheme.typography.labelSmall)
                    Text("1.5x", style = MaterialTheme.typography.labelSmall)
                    Text("2.0x", style = MaterialTheme.typography.labelSmall)
                }
            }

            // Speech pitch slider
            Column(
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text(
                    text = "Speech Pitch: ${ttsSpeechPitch}x",
                    style = MaterialTheme.typography.bodyLarge
                )

                Slider(
                    value = ttsSpeechPitch,
                    onValueChange = { viewModel.updateTtsSpeechPitch(it) },
                    valueRange = 0.5f..2.0f,
                    steps = 6,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Lower", style = MaterialTheme.typography.labelSmall)
                    Text("Normal", style = MaterialTheme.typography.labelSmall)
                    Text("Higher", style = MaterialTheme.typography.labelSmall)
                }
            }

            // Add this near the Speech Rate and Speech Pitch sections in SettingsScreen.kt

// Test TTS Button
            Button(
                onClick = {
                    // Call a method to test the current TTS settings
                    viewModel.testTtsSettings()
                },
                modifier = Modifier
                    .padding(top = 16.dp)
                    .fillMaxWidth()
            ) {
                Text("Test Current TTS Settings")
            }

// Test Wolof Recordings Button (visible only when TTS language is Wolof)
            if (ttsLanguage == "wo") {
                Button(
                    onClick = {
                        viewModel.testWolofFiles()
                    },
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth()
                ) {
                    Text("Test Wolof files")
                }
            }


            // Add this code just before the "Permissions" section in SettingsScreen.kt
// Around line 333 (before the Divider and Permissions title)

            Divider(modifier = Modifier.padding(vertical = 16.dp))

// Security section
            Text(
                text = "Sécurité",
                style = MaterialTheme.typography.titleLarge
            )

// PIN Protection toggle
            val isPinProtectionEnabled by viewModel.isPinProtectionEnabled.collectAsState(initial = false)
            val pinVerificationRequired by viewModel.pinVerificationRequired.collectAsState()
            val pinVerificationType by viewModel.pinVerificationType.collectAsState()

            // If PIN verification is required, show verification dialog
            if (pinVerificationRequired) {
                PinVerificationDialog(
                    verificationType = pinVerificationType,
                    onPinVerified = viewModel::onPinVerified,
                    onCancel = { viewModel.cancelPinVerification() },
                    userPreferences = viewModel.getUserPreferences() // Add a method to get UserPreferences
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 8.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Protection par code PIN",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Exiger un code PIN à 4 chiffres pour accéder à l'application",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                Switch(
                    checked = isPinProtectionEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            // Navigate to PIN setup screen
                            viewModel.navigateToPinSetup()
                        } else if (isPinProtectionEnabled){
                            // Disable PIN
                            viewModel.requestDisablePin()
                        }
                    }
                )
            }

// If PIN is enabled, add option to change PIN
            if (isPinProtectionEnabled) {
                Button(
                    onClick = { viewModel.requestChangePin() },
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth()
                ) {
                    Text("Changer le code PIN")
                }
            }

// Then continue with the existing Divider and Permissions section
            Divider(modifier = Modifier.padding(vertical = 16.dp))

            // Permissions section
            Text(
                text = "Permissions",
                style = MaterialTheme.typography.titleLarge
            )

            // Permission status and guidance
            Text(
                text = "Kufay a besoin d'accéder aux notifications pour capturer " +
                        "les notifications financières. Si les notifications ne sont pas capturées, " +
                        "veuillez vérifier que cette autorisation est accordée dans les paramètres système.",
                style = MaterialTheme.typography.bodyMedium
            )

            Button(
                onClick = {
                    // Would launch system notification access settings
                    // In a real implementation
                },
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("Acceder aux Paramètres de notification")
            }

            Spacer(modifier = Modifier.height(16.dp))



            // About section
            Text(
                text = "About Kufay",
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = "Version 1.0.0",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "Kufay helps you track and manage your financial notifications.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun ColorSelection(
    selectedColor: String,
    onColorSelected: (String) -> Unit
) {
    val colors = listOf(
        "#2A7221" to "Green",   // Default
        "#0063fb" to "Blue/Bleu",
        "#F79AD3" to "Pink/Rose",
        "#A50104" to "Red/Rouge",
        "#F96E46" to "Orange",
        "#FEC601" to "Yellow/Jaune"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        colors.forEach { (colorHex, colorName) ->
            val color = Color(android.graphics.Color.parseColor(colorHex))
            val isSelected = selectedColor == colorHex

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(color, shape = CircleShape)
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                            shape = CircleShape
                        )
                        .clickable { onColorSelected(colorHex) }
                )
                Text(
                    text = colorName,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}