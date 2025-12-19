package com.kufay.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.kufay.app.data.preferences.UserPreferences
import com.kufay.app.ui.viewmodels.SettingsViewModel
import kotlinx.coroutines.launch

@Composable
fun PinVerificationDialog(
    verificationType: SettingsViewModel.PinVerificationType,
    onPinVerified: () -> Unit,
    onCancel: () -> Unit,
    userPreferences: UserPreferences
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                text = when(verificationType) {
                    SettingsViewModel.PinVerificationType.DISABLE_PIN -> "Désactiver le code PIN"
                    SettingsViewModel.PinVerificationType.CHANGE_PIN -> "Changer le code PIN"
                    else -> "Vérification du code PIN"
                }
            )
        },
        text = {
            Column {
                Text("Veuillez entrer votre code PIN actuel pour continuer")

                Spacer(modifier = Modifier.height(16.dp))

                // PIN entry field
                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                            pin = it
                            error = null
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation()
                )

                // Show error if any
                if (error != null) {
                    Text(
                        text = error ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // Verify the PIN
                    coroutineScope.launch {
                        if (userPreferences.verifyPin(pin)) {
                            onPinVerified()
                        } else {
                            error = "Code PIN incorrect"
                        }
                    }
                },
                enabled = pin.length == 4
            ) {
                Text("Confirmer")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text("Annuler")
            }
        }
    )
}