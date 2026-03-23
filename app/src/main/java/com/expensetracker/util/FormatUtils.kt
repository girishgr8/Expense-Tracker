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
     * Formats a monetary amount, omitting decimal places when they are zero.
     * e.g. 10.0 → "10", 10.5 → "10.50", 1000.0 → "1,000"
     */
    fun Double.smartFormat(): String {
        val long = toLong()
        return if (this == long.toDouble())
            String.format("%,d", long)          // whole number — no decimals
        else
            String.format("%,.2f", this)         // has decimals — show 2dp
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