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

        // ===== WAVE PERSONAL =====
        if (packageName == "com.wave.personal") {

            // ✅ NEW: ENGLISH - Transfer received
            if (title.contains("Transfer received", ignoreCase = true) ||
                text.contains("You received", ignoreCase = true)) {

                val receivedRegex = """You received\s+(\d+(?:,\d+)?(?:\.\d+)?)\s*F""".toRegex(RegexOption.IGNORE_CASE)
                val match = receivedRegex.find(text)

                if (match != null) {
                    val amountText = match.groupValues[1]
                    val numericValue = amountText.replace(",", "").replace("F", "").toDoubleOrNull()

                    // Extract sender: "From [name] ([phone])"
                    val senderRegex = """From\s+([^(]+)(?:\s*\()?""".toRegex(RegexOption.IGNORE_CASE)
                    val sender = senderRegex.find(text)?.groupValues?.getOrNull(1)?.trim()

                    Log.d("KUFAY_DEBUG", "Wave Transfer Received (EN): $amountText, value: $numericValue, sender: $sender")
                    return Triple(numericValue, "Franc CFA", Pair(amountText + "F", sender))
                }
            }

            // ✅ NEW: ENGLISH - Transfer sent
            else if (title.contains("Transfer sent", ignoreCase = true) ||
                text.contains("You sent", ignoreCase = true)) {

                val sentRegex = """You sent\s+(\d+(?:,\d+)?(?:\.\d+)?)\s*F""".toRegex(RegexOption.IGNORE_CASE)
                val match = sentRegex.find(text)

                if (match != null) {
                    val amountText = match.groupValues[1]
                    val numericValue = amountText.replace(",", "").replace("F", "").toDoubleOrNull()

                    // Extract recipient: "To [name] ([phone])"
                    val recipientRegex = """To\s+([^(]+)(?:\s*\()?""".toRegex(RegexOption.IGNORE_CASE)
                    val recipient = recipientRegex.find(text)?.groupValues?.getOrNull(1)?.trim()

                    Log.d("KUFAY_DEBUG", "Wave Transfer Sent (EN): $amountText, value: $numericValue, recipient: $recipient")
                    return Triple(numericValue, "Franc CFA", Pair(amountText + "F", recipient))
                }
            }

            // ✅ NEW: ENGLISH - Payment made
            else if (title.contains("Payment successful", ignoreCase = true) ||
                text.contains("You have paid", ignoreCase = true)) {

                val paymentRegex = """You have paid\s+(\d+(?:,\d+)?(?:\.\d+)?)\s*F""".toRegex(RegexOption.IGNORE_CASE)
                val match = paymentRegex.find(text)

                if (match != null) {
                    val amountText = match.groupValues[1]
                    val numericValue = amountText.replace(",", "").replace("F", "").toDoubleOrNull()

                    // Extract merchant: "at [merchant name]"
                    val merchantRegex = """at\s+([^\n\.]+)""".toRegex(RegexOption.IGNORE_CASE)
                    val merchant = merchantRegex.find(text)?.groupValues?.getOrNull(1)?.trim()

                    Log.d("KUFAY_DEBUG", "Wave Payment (EN): $amountText, value: $numericValue, merchant: $merchant")
                    return Triple(numericValue, "Franc CFA", Pair(amountText + "F", merchant))
                }
            }

            // FRANÇAIS - Payment made - "Paiement réussi!"
            else if (title.contains("Paiement réussi", ignoreCase = true)) {
                val specificPaymentRegex = """(?:Vous avez payé|payé)\s+(\d+(?:\.\d+)?F)(?:\s+à)""".toRegex()
                val specificMatch = specificPaymentRegex.find(text)

                if (specificMatch != null) {
                    val amountText = specificMatch.groupValues[1]
                    val numericValue = amountText.replace("F", "").replace(".", "").toDoubleOrNull()
                    Log.d("KUFAY_DEBUG", "Wave Payment (specific pattern): $amountText, value: $numericValue")
                    return Triple(numericValue, "Franc CFA", Pair(amountText, extractLabel(text)))
                }

                val paymentPattern = """(?:Vous avez payé|payé)\s+(\d+(?:\.\d+)?F)""".toRegex()
                val match = paymentPattern.find(text)

                if (match != null) {
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

            // FRANÇAIS - Transfer sent - "Transfert réussi!"
            else if (title.contains("Transfert réussi", ignoreCase = true)) {
                val specificTransferRegex = """Vous avez envoyé\s+(\d+(?:\.\d+)?F)""".toRegex()
                val specificMatch = specificTransferRegex.find(text)

                if (specificMatch != null) {
                    val amountText = specificMatch.groupValues[1]
                    val numericValue = amountText.replace("F", "").replace(".", "").toDoubleOrNull()
                    Log.d("KUFAY_DEBUG", "Wave Transfer Sent (specific pattern): $amountText, value: $numericValue")
                    return Triple(numericValue, "Franc CFA", Pair(amountText, extractLabel(text)))
                }

                val transferRegex = """(?:Vous avez envoyé|envoyé)\s+(\d+(?:\.\d+)?F)""".toRegex()
                val match = transferRegex.find(text)

                if (match != null) {
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

            // FRANÇAIS - Transfer received - "Transfert reçu"
            else if (title.contains("Transfert reçu", ignoreCase = true)) {
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

        // ===== WAVE BUSINESS =====
        else if (packageName == "com.wave.business") {
            // Transfer sent - "Transfert envoyé"
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

            // Zero fees notification (Zéro frais) with "sur votre encaissement de"
            else if (text.contains("sur votre encaissement de", ignoreCase = true)) {
                val encaissementRegex = """sur votre encaissement de\s+(\d+(?:[.,]\d+)?)""".toRegex(RegexOption.IGNORE_CASE)
                val match = encaissementRegex.find(text)

                if (match != null) {
                    var amountText = match.groupValues[1]
                    if (!amountText.endsWith("F", ignoreCase = true)) {
                        amountText += "F"
                    }
                    val numericValue = amountText.replace("F", "").replace(".", "").replace(",", "").toDoubleOrNull()
                    Log.d("KUFAY_DEBUG", "Wave Business Receipt: $amountText, value: $numericValue")

                    val recipient = if (text.contains("themo i bah", ignoreCase = true)) {
                        "themo i bah"
                    } else {
                        extractLabel(text)
                    }

                    return Triple(numericValue, "Franc CFA", Pair(amountText, recipient))
                }
            }

            // À DISTANCE reçu - payment received at a distance
            else if (text.contains("À DISTANCE reçu", ignoreCase = true) ||
                text.contains("A DISTANCE reçu", ignoreCase = true)) {

                val paymentRegex = """a payé\s+(\d+(?:[.,]\d+)?(?:F|))""".toRegex(RegexOption.IGNORE_CASE)
                val amountMatch = paymentRegex.find(text)

                if (amountMatch != null) {
                    var amountText = amountMatch.groupValues[1]
                    if (!amountText.endsWith("F", ignoreCase = true)) {
                        amountText += "F"
                    }
                    val numericValue = amountText.replace("F", "").replace(".", "").replace(",", "").toDoubleOrNull()
                    Log.d("KUFAY_DEBUG", "Wave Business DISTANCE reçu: $amountText, value: $numericValue")

                    val senderRegex = """DISTANCE reçu:\s*([^(]*)(?:\(|a)""".toRegex(RegexOption.IGNORE_CASE)
                    val sender = senderRegex.find(text)?.groupValues?.getOrNull(1)?.trim() ?: extractLabel(text)

                    return Triple(numericValue, "Franc CFA", Pair(amountText, sender))
                }
            }
        }

        // ===== ORANGE MONEY & MIXX (SMS messages) =====
        else if (packageName == "com.google.android.apps.messaging") {
            if (title.contains("OrangeMoney", ignoreCase = true) || title.contains("Mixx by Yas", ignoreCase = true)) {
                val specificPattern = """(\d+(?:,\d+)?(?:\.\d+)?)\s*(?:F|CFA|XOF)""".toRegex(RegexOption.IGNORE_CASE)
                val specificMatch = specificPattern.find(text)

                if (specificMatch != null) {
                    val amountStr = specificMatch.groupValues[1]
                    val parsedAmount = if ((title.contains("OrangeMoney", ignoreCase = true) || title.contains("Mixx by Yas", ignoreCase = true)) && amountStr.contains(".")) {
                        val parts = amountStr.split(".")
                        parts[0].replace(",", "").toDoubleOrNull()
                    } else {
                        amountStr.replace(",", "").replace(".", "").toDoubleOrNull()
                    }
                    Log.d("KUFAY_DEBUG", "Orange Money/Mixx amount: $amountStr, parsed: $parsedAmount")
                    return Triple(parsedAmount, "Franc CFA", Pair(amountStr, extractLabel(text)))
                }

                val generalPattern = """(\d+(?:,\d+)?(?:\.\d+)?)""".toRegex()
                val match = generalPattern.find(text)

                if (match != null) {
                    val amountStr = match.groupValues[1]
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

        // Fallback extraction
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
            if (packageName == "com.wave.personal" && title.contains("Paiement réussi", ignoreCase = true)) {
                val specificPattern = """(?:Vous avez payé|payé)\s+(\d+(?:\.\d{3})*F?)(?:\s+à)""".toRegex()
                val specificMatch = specificPattern.find(text)

                if (specificMatch != null) {
                    val amountText = specificMatch.groupValues[1]
                    val numericValue = amountText
                        .replace("F", "")
                        .replace(".", "")
                        .toDoubleOrNull()
                    Log.d("KUFAY_DEBUG", "Found specific payment amount: $amountText ($numericValue)")
                    return Pair(numericValue, amountText)
                }

                val soldePosition = text.indexOf("Solde Wave", ignoreCase = true)
                if (soldePosition > 0) {
                    val textBeforeSolde = text.substring(0, soldePosition)
                    val amountPattern = """(\d+(?:\.\d{3})*F?)""".toRegex()
                    val match = amountPattern.findAll(textBeforeSolde).lastOrNull()

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
        }

        if (packageName == "com.google.android.apps.messaging") {
            if (title.contains("OrangeMoney", ignoreCase = true) || title.contains("Mixx by Yas", ignoreCase = true)) {
                val specificPattern = """(\d+(?:,\d+)?(?:\.\d+)?)\s*(?:F|CFA|XOF)""".toRegex(RegexOption.IGNORE_CASE)
                val specificMatch = specificPattern.find(text)

                if (specificMatch != null) {
                    val amountStr = specificMatch.groupValues[1]
                    val parsedAmount = amountStr
                        .replace(",", "")
                        .replace(".", "")
                        .toDoubleOrNull()

                    return Pair(parsedAmount, amountStr)
                }

                val generalPattern = """(\d+(?:,\d+)?(?:\.\d+)?)""".toRegex()
                val match = generalPattern.find(text)

                if (match != null) {
                    val amountStr = match.groupValues[1]
                    val parsedAmount = amountStr
                        .replace(",", "")
                        .replace(".", "")
                        .toDoubleOrNull()

                    return Pair(parsedAmount, amountStr)
                }
            }
        }

        val generalPattern = """(\d+(?:,\d+)?(?:\.\d+)?)""".toRegex()
        val match = generalPattern.find(text) ?: return Pair(null, null)
        val amountStr = match.groupValues[1]

        Log.d("KUFAY_DEBUG", "Fallback pattern matched: '$amountStr'")

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
        // ✅ NEW: Extract sender for ENGLISH received money
        val receivedMoneyEnRegex = """You received\s+\d+(?:,\d+)?(?:\.\d+)?\s*F\s+[Ff]rom\s+([^(]+)""".toRegex()
        receivedMoneyEnRegex.find(text)?.groupValues?.getOrNull(1)?.let {
            return it.trim()
        }

        // FRANÇAIS: Extract sender for received money
        val receivedMoneyRegex = """Vous avez reçu\s+\d+(?:\.\d+)?F\s+de\s+([^\.]+)""".toRegex()
        receivedMoneyRegex.find(text)?.groupValues?.getOrNull(1)?.let {
            return it.trim()
        }

        // ✅ NEW: Extract merchant for ENGLISH payments
        val merchantEnRegex = """at\s+([^\n\.]+)""".toRegex(RegexOption.IGNORE_CASE)
        merchantEnRegex.find(text)?.groupValues?.getOrNull(1)?.let {
            return it.trim()
        }

        // FRANÇAIS: Extract recipient for Wave payments
        val waveRecipientRegex = """(?:payé \d+(?:\.\d+)?F à|à)\s+([^\.]+)""".toRegex()
        waveRecipientRegex.find(text)?.groupValues?.getOrNull(1)?.let {
            return it.trim()
        }

        // FRANÇAIS: Extract recipient for Wave transfers
        val transferRecipientRegex = """A ([^\(]+)""".toRegex()
        transferRecipientRegex.find(text)?.groupValues?.getOrNull(1)?.let {
            return it.trim()
        }

        if (text.contains("themo i bah", ignoreCase = true)) {
            return "themo i bah"
        }

        val labelRegex = """for\s+(.+?)(?:\s+at|\s*$)""".toRegex(RegexOption.IGNORE_CASE)
        return labelRegex.find(text)?.groupValues?.getOrNull(1)
    }

    fun isAutoReadEnabled(): Boolean {
        return prefs.getBoolean("auto_read_enabled", true)
    }

    fun isRecognizedNotificationPattern(packageName: String, title: String, text: String): Boolean {
        // ✅ Wave Personal - ENGLISH patterns
        if (packageName == "com.wave.personal") {
            if (title.contains("Transfer received", ignoreCase = true) ||
                title.contains("Payment successful", ignoreCase = true) ||
                text.contains("You received", ignoreCase = true) ||
                text.contains("You sent", ignoreCase = true) ||
                text.contains("You have paid", ignoreCase = true)) {
                return true
            }

            // FRANÇAIS patterns (existants)
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

        // Mixx by Yas
        else if (packageName == "com.google.android.apps.messaging" &&
            title.contains("Mixx by Yas", ignoreCase = true)) {
            val hasEnvoye = text.contains("envoy", ignoreCase = true)
            val hasRecu = text.contains("recu", ignoreCase = true) ||
                    text.contains("reçu", ignoreCase = true)

            return hasEnvoye || hasRecu
        }

        return false
    }
}
