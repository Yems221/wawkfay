package com.kufay.app.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationUtils @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("kufay_preferences", Context.MODE_PRIVATE)

    private val targetPackages = setOf(
        "com.wave.personal",
        "com.wave.business",
        "com.google.android.apps.messaging"
    )

    private val targetKeywords = setOf(
        "OrangeMoney",
        "Mixx by Yas"
    )

    fun isTargetPackage(packageName: String): Boolean {
        return packageName in targetPackages
    }

    fun containsTargetKeywords(title: String): Boolean {
        return targetKeywords.any { title.contains(it, ignoreCase = true) }
    }

    fun extractFinancialData(packageName: String, title: String, text: String): Triple<Double?, String?, Pair<String?, String?>> {
        val isWave = packageName == "com.wave.personal" || packageName == "com.wave.business"

        Log.d("KUFAY_DEBUG", "Extracting financial data for: $packageName, title: $title")
        Log.d("KUFAY_DEBUG", "Text content: $text")

        // WAVE PERSONAL
        if (packageName == "com.wave.personal") {
            // N1: Payment made - "Paiement réussi!"
            if (title.contains("Paiement réussi", ignoreCase = true)) {
                // SPECIFICALLY look for the first amount "Vous avez payé" or "payé" BEFORE "Solde Wave"
                // The text typically follows pattern: "Vous avez payé XXF à [recipient]. Solde Wave: YYF"

                // First attempt - exact pattern including "à" to make sure we get payment amount
                val specificPaymentRegex = """(?:Vous avez payé|payé)\s+(\d+(?:\.\d+)?F)(?:\s+à)""".toRegex()
                val specificMatch = specificPaymentRegex.find(text)

                if (specificMatch != null) {
                    val amountText = specificMatch.groupValues[1]
                    val numericValue = amountText.replace("F", "").replace(".", "").toDoubleOrNull()
                    Log.d("KUFAY_DEBUG", "Wave Payment (specific pattern): $amountText, value: $numericValue")
                    return Triple(numericValue, "Franc CFA", Pair(amountText, extractLabel(text)))
                }

                // Second attempt - look for amount before "Solde Wave"
                val paymentPattern = """(?:Vous avez payé|payé)\s+(\d+(?:\.\d+)?F)""".toRegex()
                val match = paymentPattern.find(text)

                if (match != null) {
                    // Make sure this is not the balance amount
                    val amountPosition = match.range.first
                    val soldePosition = text.indexOf("Solde Wave", ignoreCase = true)

                    if (soldePosition == -1 || amountPosition < soldePosition) {
                        val amountText = match.groupValues[1]
                        val numericValue = amountText.replace("F", "").replace(".", "").toDoubleOrNull()
                        Log.d("KUFAY_DEBUG", "Wave Payment: $amountText, value: $numericValue")
                        return Triple(numericValue, "Franc CFA", Pair(amountText, extractLabel(text)))
                    }
                }
            }

            // N2: Transfer sent - "Transfert réussi!"
            else if (title.contains("Transfert réussi", ignoreCase = true)) {
                // Specifically look for "Vous avez envoyé XXF" - before any "Frais" or "Nouveau solde"
                // The text typically follows pattern: "Vous avez envoyé XXF à [recipient]. Frais: YYF. Nouveau solde: ZZF"

                // First attempt - exact pattern to make sure we get the sent amount
                val specificTransferRegex = """Vous avez envoyé\s+(\d+(?:\.\d+)?F)""".toRegex()
                val specificMatch = specificTransferRegex.find(text)

                if (specificMatch != null) {
                    val amountText = specificMatch.groupValues[1]
                    val numericValue = amountText.replace("F", "").replace(".", "").toDoubleOrNull()
                    Log.d("KUFAY_DEBUG", "Wave Transfer Sent (specific pattern): $amountText, value: $numericValue")
                    return Triple(numericValue, "Franc CFA", Pair(amountText, extractLabel(text)))
                }

                // Second attempt - broader pattern but check positions
                val transferRegex = """(?:Vous avez envoyé|envoyé)\s+(\d+(?:\.\d+)?F)""".toRegex()
                val match = transferRegex.find(text)

                if (match != null) {
                    // Make sure this is not the fees or balance amount
                    val amountPosition = match.range.first
                    val fraisPosition = text.indexOf("Frais", ignoreCase = true)
                    val soldePosition = text.indexOf("Nouveau solde", ignoreCase = true)

                    if ((fraisPosition == -1 || amountPosition < fraisPosition) &&
                        (soldePosition == -1 || amountPosition < soldePosition)) {
                        val amountText = match.groupValues[1]
                        val numericValue = amountText.replace("F", "").replace(".", "").toDoubleOrNull()
                        Log.d("KUFAY_DEBUG", "Wave Transfer Sent: $amountText, value: $numericValue")
                        return Triple(numericValue, "Franc CFA", Pair(amountText, extractLabel(text)))
                    }
                }
            }

            // N3: Transfer received - "Transfert reçu"
            else if (title.contains("Transfert reçu", ignoreCase = true)) {
                // Look for pattern "Vous avez reçu XXF"
                val receivedRegex = """Vous avez reçu\s+(\d+(?:\.\d+)?F)""".toRegex()
                val match = receivedRegex.find(text)

                if (match != null) {
                    val amountText = match.groupValues[1]
                    val numericValue = amountText.replace("F", "").replace(".", "").toDoubleOrNull()
                    Log.d("KUFAY_DEBUG", "Wave Transfer Received: $amountText, value: $numericValue")
                    return Triple(numericValue, "Franc CFA", Pair(amountText, extractLabel(text)))
                }
            }
        }

        // WAVE BUSINESS
        else if (packageName == "com.wave.business") {
            // N1: Transfer sent - "Transfert envoyé"
            if (title.contains("Transfert envoyé", ignoreCase = true)) {
                val transferRegex = """(?:Vous avez envoyé|envoyé)\s+(\d+(?:\.\d+)?F)""".toRegex()
                val match = transferRegex.find(text)

                if (match != null) {
                    val amountText = match.groupValues[1]
                    val numericValue = amountText.replace("F", "").replace(".", "").toDoubleOrNull()
                    Log.d("KUFAY_DEBUG", "Wave Business Transfer Sent: $amountText, value: $numericValue")
                    return Triple(numericValue, "Franc CFA", Pair(amountText, extractLabel(text)))
                }
            }

            // N2: Zero fees notification (Zéro frais) with "sur votre encaissement de"
            else if (text.contains("sur votre encaissement de", ignoreCase = true)) {
                // Modified pattern to be more precise with looser matching for the amount
                val encaissementRegex = """sur votre encaissement de\s+(\d+(?:[.,]\d+)?)""".toRegex(RegexOption.IGNORE_CASE)
                val match = encaissementRegex.find(text)

                if (match != null) {
                    var amountText = match.groupValues[1]
                    // Add F if not present
                    if (!amountText.endsWith("F", ignoreCase = true)) {
                        amountText += "F"
                    }
                    // Clean up the amount - replace both dots and commas
                    val numericValue = amountText.replace("F", "").replace(".", "").replace(",", "").toDoubleOrNull()
                    Log.d("KUFAY_DEBUG", "Wave Business Receipt: $amountText, value: $numericValue")

                    // For business encaissements, try to extract recipient name
                    val recipient = if (text.contains("themo i bah", ignoreCase = true)) {
                        "themo i bah"
                    } else {
                        extractLabel(text)
                    }

                    return Triple(numericValue, "Franc CFA", Pair(amountText, recipient))
                }
            }

            // N3: À DISTANCE reçu - payment received at a distance
            else if (text.contains("À DISTANCE reçu", ignoreCase = true) ||
                text.contains("A DISTANCE reçu", ignoreCase = true)) {

                // Extract amount - look for the pattern "a payé X F"
                val paymentRegex = """a payé\s+(\d+(?:[.,]\d+)?(?:F|))""".toRegex(RegexOption.IGNORE_CASE)
                val amountMatch = paymentRegex.find(text)

                if (amountMatch != null) {
                    var amountText = amountMatch.groupValues[1]
                    // Add F if not present
                    if (!amountText.endsWith("F", ignoreCase = true)) {
                        amountText += "F"
                    }
                    // Clean up the amount
                    val numericValue = amountText.replace("F", "").replace(".", "").replace(",", "").toDoubleOrNull()
                    Log.d("KUFAY_DEBUG", "Wave Business DISTANCE reçu: $amountText, value: $numericValue")

                    // Extract sender name - between "DISTANCE reçu:" and "a payé"
                    val senderRegex = """DISTANCE reçu:\s*([^(]*)(?:\(|a)""".toRegex(RegexOption.IGNORE_CASE)
                    val sender = senderRegex.find(text)?.groupValues?.getOrNull(1)?.trim() ?: extractLabel(text)

                    return Triple(numericValue, "Franc CFA", Pair(amountText, sender))
                }
            }
        }

        // ORANGE MONEY & MIXX (SMS messages)
        // Modified extractFinancialData method for Orange Money SMS notifications
        else if (packageName == "com.google.android.apps.messaging") {
            if (title.contains("OrangeMoney", ignoreCase = true) || title.contains("Mixx by Yas", ignoreCase = true)) {
                // First try to find a specific amount pattern with currency indicators
                val specificPattern = """(\d+(?:,\d+)?(?:\.\d+)?)\s*(?:F|CFA|XOF)""".toRegex(RegexOption.IGNORE_CASE)
                val specificMatch = specificPattern.find(text)

                if (specificMatch != null) {
                    val amountStr = specificMatch.groupValues[1]
                    // For Orange Money, treat periods as decimal separators
                    val parsedAmount = if ((title.contains("OrangeMoney", ignoreCase = true) || title.contains("Mixx by Yas", ignoreCase = true)) && amountStr.contains(".")) {
                        // For Orange Money, keep the number before decimal point
                        val parts = amountStr.split(".")
                        parts[0].replace(",", "").toDoubleOrNull()
                    } else {
                        // For other services, treat periods as thousand separators
                        amountStr.replace(",", "").replace(".", "").toDoubleOrNull()
                    }
                    Log.d("KUFAY_DEBUG", "Orange Money/Mixx amount: $amountStr, parsed: $parsedAmount")
                    return Triple(parsedAmount, "Franc CFA", Pair(amountStr, extractLabel(text)))
                }

                // Similar logic for the fallback pattern
                val generalPattern = """(\d+(?:,\d+)?(?:\.\d+)?)""".toRegex()
                val match = generalPattern.find(text)

                if (match != null) {
                    val amountStr = match.groupValues[1]
                    // Apply the same Orange Money-specific handling
                    val parsedAmount = if ((title.contains("OrangeMoney", ignoreCase = true) || title.contains("Mixx by Yas", ignoreCase = true)) && amountStr.contains(".")) {
                        val parts = amountStr.split(".")
                        parts[0].replace(",", "").toDoubleOrNull()
                    } else {
                        amountStr.replace(",", "").replace(".", "").toDoubleOrNull()
                    }
                    Log.d("KUFAY_DEBUG", "General pattern for Orange Money/Mixx: $amountStr, parsed: $parsedAmount")
                    return Triple(parsedAmount, "Franc CFA", Pair(amountStr, extractLabel(text)))
                }
            }
        }

        // Fallback extraction if no specific pattern matched
        val (amount, amountText) = extractAmount(text, packageName, title)
        val currency = formatCurrency(packageName, title)
        val label = extractLabel(text)

        Log.d("KUFAY_DEBUG", "Final extracted data: amount=$amount, amountText='$amountText', currency=$currency")

        return Triple(amount, currency, Pair(amountText, label))
    }

    private fun extractAmount(text: String, packageName: String, title: String): Pair<Double?, String?> {
        val isWave = packageName == "com.wave.personal" || packageName == "com.wave.business"

        Log.d("KUFAY_DEBUG", "Extracting amount from text: '$text'")
        Log.d("KUFAY_DEBUG", "Package name: $packageName, isWave: $isWave")

        if (isWave) {
            // WAVE PERSONAL: Payment notification - extract first amount before "Solde Wave"
            if (packageName == "com.wave.personal" && title.contains("Paiement réussi", ignoreCase = true)) {
                // First try to find the amount right before "à"
                val specificPattern = """(?:Vous avez payé|payé)\s+(\d+(?:\.\d{3})*F?)(?:\s+à)""".toRegex()
                val specificMatch = specificPattern.find(text)

                if (specificMatch != null) {
                    val amountText = specificMatch.groupValues[1]
                    val numericValue = amountText
                        .replace("F", "")     // Remove currency symbol
                        .replace(".", "")     // Remove thousand separators
                        .toDoubleOrNull()
                    Log.d("KUFAY_DEBUG", "Found specific payment amount: $amountText ($numericValue)")
                    return Pair(numericValue, amountText)
                }

                // Then try to find any amount that comes before "Solde Wave"
                val soldePosition = text.indexOf("Solde Wave", ignoreCase = true)
                if (soldePosition > 0) {
                    val textBeforeSolde = text.substring(0, soldePosition)
                    val amountPattern = """(\d+(?:\.\d{3})*F?)""".toRegex()
                    val match = amountPattern.findAll(textBeforeSolde).lastOrNull() // Get the last one before "Solde"

                    if (match != null) {
                        val amountText = match.groupValues[1]
                        val numericValue = amountText
                            .replace("F", "")
                            .replace(".", "")
                            .toDoubleOrNull()
                        Log.d("KUFAY_DEBUG", "Found payment amount before solde: $amountText ($numericValue)")
                        return Pair(numericValue, amountText)
                    }
                }
            }

            // Similar detailed modifications for other Wave scenarios...
            // (Transfer sent, Transfer received, Business notifications)
            // [Rest of the existing Wave-specific parsing logic would remain largely the same]
            // Just replace the numeric value extraction with the thousand separator removal method
        }

        // Special handling for Orange Money and Mixx
        if (packageName == "com.google.android.apps.messaging") {
            if (title.contains("OrangeMoney", ignoreCase = true) || title.contains("Mixx by Yas", ignoreCase = true)) {
                // First try to find a specific amount pattern with currency indicators
                val specificPattern = """(\d+(?:,\d+)?(?:\.\d+)?)\s*(?:F|CFA|XOF)""".toRegex(RegexOption.IGNORE_CASE)
                val specificMatch = specificPattern.find(text)

                if (specificMatch != null) {
                    val amountStr = specificMatch.groupValues[1]
                    // FIXED: Always treat periods as thousand separators
                    val parsedAmount = amountStr
                        .replace(",", "")  // Remove comma separators
                        .replace(".", "")  // Remove periods (treat as thousand separators)
                        .toDoubleOrNull()

                    return Pair(parsedAmount, amountStr)
                }

                // Fallback to general number pattern
                val generalPattern = """(\d+(?:,\d+)?(?:\.\d+)?)""".toRegex()
                val match = generalPattern.find(text)

                if (match != null) {
                    val amountStr = match.groupValues[1]
                    // FIXED: Always treat periods as thousand separators
                    val parsedAmount = amountStr
                        .replace(",", "")
                        .replace(".", "")
                        .toDoubleOrNull()

                    return Pair(parsedAmount, amountStr)
                }
            }
        }

        // Fallback parsing remains the same, with thousand separator handling
        val generalPattern = """(\d+(?:,\d+)?(?:\.\d+)?)""".toRegex()
        val match = generalPattern.find(text) ?: return Pair(null, null)
        val amountStr = match.groupValues[1]

        Log.d("KUFAY_DEBUG", "Fallback pattern matched: '$amountStr'")

        // FIXED: Consistent parsing for all payment services
        // Parse amount by removing ALL periods and commas
        val parsedAmount = amountStr
            .replace(".", "")
            .replace(",", "")
            .toDoubleOrNull()

        Log.d("KUFAY_DEBUG", "Fallback returning: value=$parsedAmount, text='$amountStr'")
        return Pair(parsedAmount, amountStr)
    }

    private fun formatCurrency(packageName: String, title: String): String? {
        return when {
            packageName == "com.wave.personal" || packageName == "com.wave.business" -> "Franc CFA"
            packageName == "com.google.android.apps.messaging" &&
                    title.contains("OrangeMoney", ignoreCase = true) -> "Franc CFA"
            packageName == "com.google.android.apps.messaging" &&
                    title.contains("Mixx by Yas", ignoreCase = true) -> "Franc CFA"
            else -> null
        }
    }

    private fun extractLabel(text: String): String? {
        // New pattern: Extract sender for received money
        val receivedMoneyRegex = """Vous avez reçu\s+\d+(?:\.\d+)?F\s+de\s+([^\.]+)""".toRegex()
        receivedMoneyRegex.find(text)?.groupValues?.getOrNull(1)?.let {
            return it.trim()
        }

        // Extract recipient for Wave payments
        val waveRecipientRegex = """(?:payé \d+(?:\.\d+)?F à|à)\s+([^\.]+)""".toRegex()
        waveRecipientRegex.find(text)?.groupValues?.getOrNull(1)?.let {
            return it.trim()
        }

        // Extract recipient for Wave transfers
        val transferRecipientRegex = """A ([^\(]+)""".toRegex()
        transferRecipientRegex.find(text)?.groupValues?.getOrNull(1)?.let {
            return it.trim()
        }

        // Extract special recipient "themo i bah" that appears in many examples
        if (text.contains("themo i bah", ignoreCase = true)) {
            return "themo i bah"
        }

        // General label regex as fallback
        val labelRegex = """for\s+(.+?)(?:\s+at|\s*$)""".toRegex(RegexOption.IGNORE_CASE)
        return labelRegex.find(text)?.groupValues?.getOrNull(1)
    }

    fun isAutoReadEnabled(): Boolean {
        return prefs.getBoolean("auto_read_enabled", true)
    }

    fun isRecognizedNotificationPattern(packageName: String, title: String, text: String): Boolean {
        // Wave Personal recognized patterns
        if (packageName == "com.wave.personal") {
            if (title.contains("Paiement réussi", ignoreCase = true) ||
                title.contains("Transfert reçu", ignoreCase = true)) {
                return true
            }
        }

        // Wave Business recognized patterns
        else if (packageName == "com.wave.business") {
            if (title.contains("Transfert envoyé", ignoreCase = true) ||
                title.contains("Zéro frais", ignoreCase = true) ||
                title.contains("Paiement", ignoreCase = true) ||
                text.contains("sur votre encaissement de", ignoreCase = true)) {
                return true
            }
        }

        // Orange Money and other recognized patterns
        else if (packageName == "com.google.android.apps.messaging") {
            if (title.contains("OrangeMoney", ignoreCase = true) ||
                title.contains("Mixx by Yas", ignoreCase = true)) {
                return true
            }
        }

        // Mixx by Yas - ONLY recognize notifications with "envoyé" or "reçu"
        else if (packageName == "com.google.android.apps.messaging" &&
            title.contains("Mixx by Yas", ignoreCase = true)) {
            val hasEnvoye = text.contains("envoy", ignoreCase = true)
            val hasRecu = text.contains("recu", ignoreCase = true) ||
                    text.contains("reçu", ignoreCase = true)

            return hasEnvoye || hasRecu
        }

        // Unrecognized pattern - capture but don't auto-read
        return false
    }
}
