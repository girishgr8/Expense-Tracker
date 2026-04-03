package com.expensetracker.util

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

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
    fun Double.smartFormat(decimalPref: String = "default"): String = when (decimalPref) {
        "none" -> String.format(Locale.getDefault(), "%,.0f", this)
        "one"  -> String.format(Locale.getDefault(), "%,.1f", this)
        "two"  -> String.format(Locale.getDefault(), "%,.2f", this)
        else   -> {                               // "default" — optimized for readability
            val long = toLong()
            if (this == long.toDouble()) String.format(Locale.getDefault(), "%,d", long)
            else String.format(Locale.getDefault(), "%,.2f", this)
        }
    }

    fun Double.toCurrency(symbol: String = "₹"): String =
        "$symbol${smartFormat()}"

    fun Double.toCurrencyCompact(symbol: String = "₹"): String = when {
        this >= 1_000_000 -> "$symbol${"%.1f".format(this / 1_000_000)}M"
        this >= 1_000     -> "$symbol${"%.1f".format(this / 1_000)}K"
        else              -> "$symbol${String.format("%,.0f", this)}"
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