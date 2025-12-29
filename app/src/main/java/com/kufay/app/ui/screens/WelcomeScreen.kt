package com.kufay.app.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kufay.app.R
import com.kufay.app.ui.viewmodels.WelcomeViewModel
import kotlinx.coroutines.delay

@Composable
fun WelcomeScreen(
    viewModel: WelcomeViewModel,
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Get user's selected theme color
    val primaryColor = MaterialTheme.colorScheme.primary

    // Animation states
    var logoVisible by remember { mutableStateOf(false) }
    var contentVisible by remember { mutableStateOf(false) }
    var buttonVisible by remember { mutableStateOf(false) }

    // Bouncy animation for logo
    val logoScale by animateFloatAsState(
        targetValue = if (logoVisible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ), label = "logo_scale"
    )

    // Trigger animations
    LaunchedEffect(Unit) {
        delay(100)
        logoVisible = true
        delay(400)
        contentVisible = true
        delay(600)
        buttonVisible = true
    }

    // Simple background with primary color tint
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFFDF7)) // Warm white background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Logo with simple circle background
            Box(
                modifier = Modifier
                    .scale(logoScale)
                    .size(140.dp)
                    .clip(CircleShape)
                    .background(primaryColor.copy(alpha = 0.1f))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo_kufay),
                    contentDescription = "Logo Kufay",
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Title - Uses MaterialTheme typography (respects user text size)
            AnimatedVisibility(
                visible = contentVisible,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 3 })
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Bienvenue ! 👋",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = Color(0xFF2A2A2A)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Kufay vous aide à gérer vos paiements",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        color = Color(0xFF666666)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Simple feature cards
            AnimatedVisibility(
                visible = contentVisible,
                enter = fadeIn(animationSpec = tween(800, delayMillis = 200))
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Card 1
                    SimpleFeatureCard(
                        emoji = "🔔",
                        backgroundColor = Color(0xFFE8F5E9),
                        title = "On écoute pour vous",
                        description = "Kufay lit vos notifications de Wave, Orange Money... Pas besoin d'arrêter ce que vous faites !"
                    )

                    // Card 2
                    SimpleFeatureCard(
                        emoji = "🔒",
                        backgroundColor = Color(0xFFE3F2FD),
                        title = "100% sur votre téléphone",
                        description = "Vos données ne quittent JAMAIS votre téléphone. C'est promis !"
                    )

                    // Card 3
                    SimpleFeatureCard(
                        emoji = "⚙️",
                        backgroundColor = Color(0xFFFFF9C4),
                        title = "Vous décidez de tout",
                        description = "Vous pouvez tout désactiver quand vous voulez. Simple comme bonjour !"
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Footer message
            AnimatedVisibility(
                visible = contentVisible,
                enter = fadeIn(animationSpec = tween(600, delayMillis = 400))
            ) {
                Text(
                    text = "✓ Kufay, simple et efficace",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = primaryColor,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Disclaimer and checkbox
            AnimatedVisibility(
                visible = buttonVisible,
                enter = fadeIn() + expandVertically()
            ) {
                var isChecked by remember { mutableStateOf(false) }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Disclaimer card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFF9E6) // Light yellow background
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "ℹ️ Important",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF8B6914)
                            )

                            Text(
                                text = "Kufay est un outil indépendant d'aide à la gestion des transactions numériques.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF555555)
                            )

                            Text(
                                text = "Les données financières sont traitées exclusivement dans l'appareil où Kufay est installé.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF555555)
                            )

                            Text(
                                text = "Kufay n'est pas en contrat avec les opérateurs.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF555555)
                            )

                            Text(
                                text = "Donner à Kufay le droit de traiter pour vous vos informations des transactions Mobile Money sur votre appareil, pour votre compte.",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF2A2A2A)
                            )
                        }
                    }

                    // Checkbox agreement
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { isChecked = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = primaryColor
                            )
                        )

                        Text(
                            text = "J'ai lu et j'accepte ces conditions",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF2A2A2A),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // CTA button - enabled only when checked
                    Button(
                        onClick = {
                            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                            context.startActivity(intent)
                            viewModel.setOnboardingCompleted()
                            onContinue()
                        },
                        enabled = isChecked,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor,
                            disabledContainerColor = primaryColor.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 2.dp
                        ),
                        contentPadding = PaddingValues(vertical = 16.dp, horizontal = 24.dp)
                    ) {
                        Text(
                            text = "C'est parti ! 🚀",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SimpleFeatureCard(
    emoji: String,
    backgroundColor: Color,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Emoji in simple circle
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = emoji,
                    style = MaterialTheme.typography.headlineMedium
                )
            }

            // Text content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2A2A2A)
                )

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF555555)
                )
            }
        }
    }
}
