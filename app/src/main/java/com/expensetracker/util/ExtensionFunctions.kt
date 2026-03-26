package com.expensetracker.util

import androidx.compose.ui.graphics.Color
import java.time.LocalDateTime
import java.time.YearMonth

// ─── Color ───────────────────────────────────────────────────────────────────

fun String.toComposeColor(fallback: Color = Color.Gray): Color =
    runCatching { Color(android.graphics.Color.parseColor(this)) }.getOrDefault(fallback)

// ─── Date helpers ─────────────────────────────────────────────────────────────

fun YearMonth.startOfMonth(): LocalDateTime = atDay(1).atStartOfDay()
fun YearMonth.endOfMonth(): LocalDateTime   = atEndOfMonth().atTime(23, 59, 59)

fun Int.toYearMonth(month: Int): YearMonth  = YearMonth.of(this, month)

val LocalDateTime.epochMilli: Long
    get() = atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()

fun Long.toLocalDateTime(): LocalDateTime =
    LocalDateTime.ofInstant(
        java.time.Instant.ofEpochMilli(this),
        java.time.ZoneId.systemDefault()
    )

// ─── List helpers ─────────────────────────────────────────────────────────────

fun <T> List<T>.safeSubList(fromIndex: Int, toIndex: Int): List<T> =
    subList(fromIndex.coerceAtLeast(0), toIndex.coerceAtMost(size))

// ─── String helpers ───────────────────────────────────────────────────────────

fun String.truncate(maxLength: Int, suffix: String = "…"): String =
    if (length <= maxLength) this else take(maxLength) + suffix

fun String.capitalizeWords(): String =
    split(" ").joinToString(" ") { it.lowercase().replaceFirstChar { c -> c.uppercase() } }

// ─── Number helpers ───────────────────────────────────────────────────────────

fun Double.roundTo(decimals: Int): Double {
    val factor = Math.pow(10.0, decimals.toDouble())
    return Math.round(this * factor) / factor
}
