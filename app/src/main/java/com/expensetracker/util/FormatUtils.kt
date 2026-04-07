package com.expensetracker.util

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object FormatUtils {

    private val shortDate   = DateTimeFormatter.ofPattern("dd MMM yyyy")
    private val longDate    = DateTimeFormatter.ofPattern("EEEE, MMM dd yyyy")
    private val timeOnly    = DateTimeFormatter.ofPattern("hh:mm a")
    private val shortDT     = DateTimeFormatter.ofPattern("dd MMM, hh:mm a")
    private val monthYear   = DateTimeFormatter.ofPattern("MMMM yyyy")
    private val csvDateTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    fun LocalDateTime.toShortDate()   = format(shortDate)
    fun LocalDateTime.toLongDate()    = format(longDate)
    fun LocalDateTime.toTimeOnly()    = format(timeOnly)
    fun LocalDateTime.toShortDT()     = format(shortDT)
    fun LocalDateTime.toMonthYear()   = format(monthYear)
    fun LocalDateTime.toCsvDateTime() = format(csvDateTime)

    /**
     * Decimal format preference values:
     *   "default" — smart: whole numbers show no decimals, others show 2dp
     *   "none"    — always 0 decimal places
     *   "one"     — always 1 decimal place
     *   "two"     — always 2 decimal places
     */
    fun Double.smartFormat(
        decimalPref: String = "default",
        currencyFormat: String = "millions"
    ): String = formatAmountForDisplay(this, currencyFormat, decimalPref)

    fun Double.toCurrency(
        symbol: String = "₹",
        currencyFormat: String = "millions"
    ): String = "$symbol${smartFormat(currencyFormat = currencyFormat)}"

    fun Double.toCurrencyCompact(
        symbol: String = "₹",
        currencyFormat: String = "millions"
    ): String = when {
        currencyFormat == "none" -> "$symbol${formatAmountForDisplay(this, currencyFormat, decimalFmt = "none")}"
        currencyFormat == "lakhs" && this >= 1_00_000 -> "$symbol${"%.1f".format(this / 1_00_000)}L"
        currencyFormat != "lakhs" && this >= 1_000_000 -> "$symbol${"%.1f".format(this / 1_000_000)}M"
        this >= 1_000 -> "$symbol${"%.1f".format(this / 1_000)}K"
        else -> "$symbol${formatAmountForDisplay(this, currencyFormat, decimalFmt = "none")}"
    }

    fun formatAmountForDisplay(
        amount: Double,
        currencyFormat: String,
        decimalFmt: String = "default"
    ): String {
        val long = amount.toLong()
        val formatted = when (decimalFmt) {
            "none" -> "%.0f".format(amount)
            "one" -> "%.1f".format(amount)
            "two" -> "%.2f".format(amount)
            else -> if (amount == long.toDouble()) "%d".format(long) else "%.2f".format(amount)
        }
        val parts = formatted.split(".", limit = 2)
        val integerPart = parts.first()
        val decimalPart = parts.getOrNull(1)
        val sign = if (integerPart.startsWith("-")) "-" else ""
        val digits = integerPart.removePrefix("-")
        val groupedInteger = when (currencyFormat) {
            "none" -> digits
            "lakhs" -> groupIndianDigits(digits)
            else -> groupWesternDigits(digits)
        }
        return buildString {
            append(sign)
            append(groupedInteger)
            if (decimalPart != null) {
                append(".")
                append(decimalPart)
            }
        }
    }

    private fun groupWesternDigits(digits: String): String =
        digits.reversed().chunked(3).joinToString(",").reversed()

    private fun groupIndianDigits(digits: String): String {
        if (digits.length <= 3) return digits
        val suffix = digits.takeLast(3)
        val prefix = digits.dropLast(3)
        val groupedPrefix = prefix.reversed().chunked(2).joinToString(",").reversed()
        return "$groupedPrefix,$suffix"
    }

    fun Long.toFileSize(): String = when {
        this >= 1_048_576 -> "%.1f MB".format(this / 1_048_576f)
        this >= 1_024     -> "%.1f KB".format(this / 1_024f)
        else              -> "$this B"
    }

    fun String.toTitleCase(): String =
        split(" ").joinToString(" ") { it.lowercase().replaceFirstChar { c -> c.uppercase() } }

    val ALLOWED_MIME_TYPES = setOf(
        "application/pdf",
        "image/jpeg",
        "image/jpg",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    )

    fun isMimeTypeAllowed(mimeType: String) = mimeType in ALLOWED_MIME_TYPES
}
