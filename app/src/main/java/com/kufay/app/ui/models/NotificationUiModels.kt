package com.kufay.app.ui.models

import androidx.compose.ui.graphics.Color
import com.kufay.app.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

// Inside com.kufay.app.ui.models.NotificationUiModels.kt

enum class AppType(val packageName: String, val displayName: String) {
    WAVE_PERSONAL("com.wave.personal", "Wave"),  // Changed from "Wave Personal" to "Wave"
    WAVE_BUSINESS("com.wave.business", "Wave Business"),
    ORANGE_MONEY("com.google.android.apps.messaging", "Orange Money"),
    MIXX("com.google.android.apps.messaging", "Mixx by Yas");

    companion object {
        fun fromPackageName(packageName: String, title: String): AppType {
            return when {
                packageName == WAVE_PERSONAL.packageName && !title.contains("business", ignoreCase = true) ->
                    WAVE_PERSONAL
                packageName == WAVE_BUSINESS.packageName ||
                        (packageName == WAVE_PERSONAL.packageName && title.contains("business", ignoreCase = true)) ->
                    WAVE_BUSINESS
                packageName == ORANGE_MONEY.packageName &&
                        title.contains("OrangeMoney", ignoreCase = true) ->
                    ORANGE_MONEY
                packageName == MIXX.packageName &&
                        title.contains("Mixx by Yas", ignoreCase = true) ->
                    MIXX
                else -> throw IllegalArgumentException("Unknown app type for package $packageName and title $title")
            }
        }

        fun getAppIconLetter(appType: AppType): String {
            return when (appType) {
                WAVE_PERSONAL -> "W"
                WAVE_BUSINESS -> "W"
                ORANGE_MONEY -> "O"
                MIXX -> "M"
            }
        }
    }

    fun getColor(): Color {
        return when (this) {
            WAVE_PERSONAL -> Color(0xFF28C7FD)
            WAVE_BUSINESS -> Color(0xFF00008B)
            ORANGE_MONEY -> Color(0xFFEE7D00)
            MIXX -> Color(0xFFFFD600)
        }
    }

    fun getIconLetter(): String {
        return when (this) {
            WAVE_PERSONAL -> "W"
            WAVE_BUSINESS -> "W"
            ORANGE_MONEY -> "O"
            MIXX -> "M"
        }
    }
}

object NotificationFormatter {
    fun formatAmount(amount: Double?, currency: String?, packageName: String? = null): String {
        if (amount == null) return ""

        // For Wave apps, keep the dot notation (thousand separators)
        if (packageName == "com.wave.personal" || packageName == "com.wave.business") {
            // Format with dots as thousand separators (preserve the original format)
            val amountStr = amount.toString()
            val parts = amountStr.split(".")

            // Keep integer part
            val integerPart = if (parts[0].length > 3) {
                // Only format if number is large enough to need separators
                formatWithThousandDots(parts[0])
            } else {
                parts[0]
            }

            // Return with Franc CFA instead of F
            return "$integerPart Franc CFA"
        }

        // For Orange Money and Mixx, format without decimals
        if (packageName == "com.google.android.apps.messaging") {
            val formattedAmount = amount.toLong().toString()
            return "$formattedAmount Franc CFA"
        }

        // Standard formatting for other apps
        val formattedAmount = amount.toLong().toString()
        return "$formattedAmount Franc CFA"
    }

    // Helper method to format with dots as thousand separators
    private fun formatWithThousandDots(amountStr: String): String {
        val result = StringBuilder()

        for (i in amountStr.indices.reversed()) {
            result.insert(0, amountStr[i])
            if (i > 0 && (amountStr.length - i) % 3 == 0) {
                result.insert(0, ".")
            }
        }

        return result.toString()
    }
}

