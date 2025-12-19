package com.kufay.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kufay.app.ui.viewmodels.PinViewModel

@Composable
fun PinScreen(
    viewModel: PinViewModel,
    onAuthenticated: () -> Unit
) {
    val pin by viewModel.pin.collectAsState()
    val pinState by viewModel.pinState.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    // If authenticated, proceed to main app
    if (pinState is PinViewModel.PinState.Authenticated) {
        onAuthenticated()
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top section with title and instructions
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 40.dp)
        ) {
            // App logo or icon
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = "App Logo",
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Bienvenu",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = when (pinState) {
                    is PinViewModel.PinState.Setup -> "Créer un code PIN à 4 chiffres"
                    is PinViewModel.PinState.Confirm -> "Confirmer le PIN"
                    is PinViewModel.PinState.Login -> "Entrer votre PIN"
                    else -> ""
                },
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Error message if any
            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // PIN display
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(vertical = 16.dp)
            ) {
                for (i in 0 until 4) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(
                                if (i < pin.length) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                    )
                }
            }
        }

        // Middle section - if applicable
        Spacer(modifier = Modifier.weight(1f))

        // Bottom section with keypad
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // PIN keypad
            val keypadButtons = listOf(
                listOf('1', '2', '3'),
                listOf('4', '5', '6'),
                listOf('7', '8', '9'),
                listOf('*', '0', '<')
            )

            keypadButtons.forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    row.forEach { digit ->
                        KeypadButton(
                            digit = digit,
                            onClick = {
                                when (digit) {
                                    '<' -> viewModel.deleteDigit()
                                    '*' -> {} // Unused
                                    else -> viewModel.appendDigit(digit)
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Verify button
            if (pin.length == 4) {
                Button(
                    onClick = { viewModel.checkPin() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text("Valider")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun KeypadButton(
    digit: Char,
    onClick: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
    ) {
        TextButton(
            onClick = onClick,
            modifier = Modifier.fillMaxSize()
        ) {
            when (digit) {
                '<' -> {
                    Icon(
                        imageVector = Icons.Default.Backspace,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                '*' -> {
                    // Empty or another special button
                }
                else -> {
                    Text(
                        text = digit.toString(),
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
            }
        }
    }
}