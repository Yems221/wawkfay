package com.kufay.app.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kufay.app.R
import com.kufay.app.ui.viewmodels.WelcomeViewModel

@Composable
fun WelcomeScreen(
    viewModel: WelcomeViewModel,
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .padding(15.dp)
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(5.dp))

                // Kufay Logo
                Image(
                    painter = painterResource(id = R.drawable.logo_kufay),
                    contentDescription = "Logo Kufay",
                    modifier = Modifier.size(150.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Title
                Text(
                    text = "Pourquoi Kufay demande accès à vos notifications et SMS ?",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Simplify life section - Using Notifications icon for payment alerts
                ExplanationItem(
                    emoji = "💡",
                    icon = Icons.Outlined.Notifications,
                    title = "Pour vous simplifier la vie",
                    description = "Kufay lit uniquement vos notifications et SMS liés aux paiements (Wave, Orange Money, etc.) pour vous éviter d'arreter votre activité pour verifier combien vous avez reçu."
                )

                Spacer(modifier = Modifier.height(25.dp))

                // Your data stays with you section - Using Security icon for data privacy
                ExplanationItem(
                    emoji = "🔒",
                    icon = Icons.Outlined.Security,
                    title = "Vos données restent à vous",
                    description = buildAnnotatedString {
                        append("Rien n'est stocké ailleurs, rien n'est partagé. Tout se passe sur votre téléphone, et vous gardez le contrôle. ")
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append("POUR TOUJOURS")
                        }
                    }
                )

                Spacer(modifier = Modifier.height(25.dp))

                // You can stop anytime section - Using ManageAccounts for user control
                ExplanationItem(
                    emoji = "⚙️",
                    icon = Icons.Outlined.ManageAccounts,
                    title = "Vous pouvez dire stop à tout moment",
                    description = "Si vous changez d'avis, vous pouvez retirer l'accès en un clic dans les paramètres er désinstaller l'application."
                )

                Spacer(modifier = Modifier.height(25.dp))

                // Footer message
                Text(
                    text = "Kufay pour vous aider et vous simplifier la vie.",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Grant access button
                Button(
                    onClick = {
                        // Open Android notification access settings
                        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                        context.startActivity(intent)
                        viewModel.setOnboardingCompleted()
                        onContinue()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(
                        text = "Accorder l'accès",
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ExplanationItem(
    emoji: String,
    icon: ImageVector,
    title: String,
    description: Any // Can be String or AnnotatedString
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // Use Material icons instead of emojis for better visual consistency
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = when (emoji) {
                "💡" -> Color(0xFF4CAF50) // Green for notifications/simplify
                "🔒" -> Color(0xFF2196F3) // Blue for security/privacy
                "⚙️" -> Color(0xFFFFC107) // Amber for settings/control
                else -> MaterialTheme.colorScheme.primary
            }
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            if (description is String) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Text(
                    text = description as androidx.compose.ui.text.AnnotatedString,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}