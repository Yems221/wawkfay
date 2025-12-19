package com.kufay.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.stringPreferencesKey
import com.kufay.app.data.preferences.dataStore
import kotlinx.coroutines.flow.map


// Extend with app color scheme
class AppColors(
    val wavePersonal: Color,
    val waveBusiness: Color,
    val orangeMoney: Color,
    val mixx: Color,
    val backgroundGray: Color
)

// Create composition local to provide app colors
val LocalAppColors = staticCompositionLocalOf {
    AppColors(
        wavePersonal = WavePersonalBlue,
        waveBusiness = WaveBusinessPurple,
        orangeMoney = OrangeMoneyOrange,
        mixx = MixxYellow,
        backgroundGray = BackgroundGray
    )
}

private val LightColors = lightColorScheme(
    primary = KufayGreen,
    onPrimary = Color.White,
    primaryContainer = KufayGreenLight,
    onPrimaryContainer = KufayGreenDark,
    secondary = KufayBlue,
    onSecondary = Color.White,
    secondaryContainer = KufayBlueLight,
    onSecondaryContainer = KufayBlueDark,
    tertiary = OrangeMoneyOrange, // Use OrangeMoney as tertiary color
    onTertiary = Color.White,
    tertiaryContainer = MixxYellow.copy(alpha = 0.3f), // Use MixxYellow as tertiary container
    onTertiaryContainer = Color(0xFF271900),
    error = Color(0xFFB00020),
    errorContainer = Color(0xFFFFDAD5),
    onError = Color.White,
    onErrorContainer = Color(0xFF410001),
    background = BackgroundGray, // Use our lighter gray for background
    onBackground = Color(0xFF1A1C18),
    surface = SurfaceWhite,
    onSurface = Color(0xFF1A1C18),
    surfaceVariant = Color(0xFFDDE5DB),
    onSurfaceVariant = Color(0xFF414941)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF88D288),
    onPrimary = Color(0xFF003A00),
    primaryContainer = Color(0xFF005300),
    onPrimaryContainer = Color(0xFFAAE5AA),
    secondary = Color(0xFF4DDCD6),
    onSecondary = Color(0xFF003739),
    secondaryContainer = Color(0xFF00484B),
    onSecondaryContainer = Color(0xFF84DDDA),
    tertiary = OrangeMoneyOrange.copy(alpha = 0.8f), // Slightly dimmed for dark theme
    onTertiary = Color(0xFF402D00),
    tertiaryContainer = MixxYellow.copy(alpha = 0.2f),
    onTertiaryContainer = MixxYellow,
    error = Color(0xFFCF6679),
    errorContainer = Color(0xFF8C0009),
    onError = Color(0xFF000000),
    onErrorContainer = Color(0xFFFFDAD5),
    background = Color(0xFF1A1C18),
    onBackground = Color(0xFFE3E3DC),
    surface = Color(0xFF1A1C18),
    onSurface = Color(0xFFE3E3DC),
    surfaceVariant = Color(0xFF414941),
    onSurfaceVariant = Color(0xFFC1C9BF)
)

// Add this object to resolve AppTheme references
object AppTheme {
    val colors: AppColors
        @Composable
        get() = LocalAppColors.current
}

@Composable
fun KufayTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Read app color from DataStore
    val context = LocalContext.current
    val appColorFlow = remember {
        context.dataStore.data.map { preferences ->
            preferences[stringPreferencesKey("app_main_color")] ?: "#006400"
        }
    }
    val appColor by appColorFlow.collectAsState(initial = "#006400")

    // Parse the color
    val primaryColor = try {
        Color(android.graphics.Color.parseColor(appColor))
    } catch (e: Exception) {
        Color(0xFF006400) // Fallback to green
    }

    // Create color schemes with dynamic primary color
    val colors = if (darkTheme) {
        darkColorScheme(
            primary = primaryColor,
            primaryContainer = primaryColor.copy(alpha = 0.7f),
            onPrimary = Color.White,
            onPrimaryContainer = primaryColor.copy(alpha = 0.9f),
            // Keep other colors from your existing dark color scheme
            secondary = DarkColors.secondary,
            secondaryContainer = DarkColors.secondaryContainer,
            tertiary = DarkColors.tertiary,
            background = DarkColors.background,
            surface = DarkColors.surface,
            // ... other colors from your existing dark color scheme
        )
    } else {
        lightColorScheme(
            primary = primaryColor,
            primaryContainer = primaryColor.copy(alpha = 0.5f),
            onPrimary = Color.White,
            onPrimaryContainer = primaryColor.copy(alpha = 0.9f),
            // Keep other colors from your existing light color scheme
            secondary = LightColors.secondary,
            secondaryContainer = LightColors.secondaryContainer,
            tertiary = LightColors.tertiary,
            background = LightColors.background,
            surface = LightColors.surface,
            // ... other colors from your existing light color scheme
        )
    }

    // Create app colors (adjust some colors for dark theme if needed)
    val appColors = if (darkTheme) {
        AppColors(
            wavePersonal = WavePersonalBlue.copy(alpha = 0.8f),
            waveBusiness = WaveBusinessPurple.copy(alpha = 0.8f),
            orangeMoney = OrangeMoneyOrange.copy(alpha = 0.8f),
            mixx = MixxYellow.copy(alpha = 0.8f),
            backgroundGray = Color(0xFF252525)
        )
    } else {
        AppColors(
            wavePersonal = WavePersonalBlue,
            waveBusiness = WaveBusinessPurple,
            orangeMoney = OrangeMoneyOrange,
            mixx = MixxYellow,
            backgroundGray = BackgroundGray
        )
    }

    // Provide both the Material theme and our custom app colors
    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = colors,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}