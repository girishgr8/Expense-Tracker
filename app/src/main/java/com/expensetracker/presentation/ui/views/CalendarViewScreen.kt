package com.expensetracker.presentation.ui.views

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.expensetracker.presentation.components.LocalCurrencyFormat
import com.expensetracker.presentation.components.LocalCurrencySymbol
import com.expensetracker.util.FormatUtils.smartFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

// ── Amount pill colors — exactly matching the screenshot ──────────────────────
private val ExpensePillBg = Color(0xFF5C2626)   // dark red/maroon pill
private val ExpensePillText = Color(0xFFFFCDD2)  // light pink text
private val IncomePillBg = Color(0xFF1B4D3E)   // dark green pill
private val IncomePillText = Color(0xFFA5D6A7)  // light green text

@Composable
fun CalendarViewScreen(
    onNavigateBack: () -> Unit,
    onDayClick: (LocalDate) -> Unit = {},
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currencySymbol = LocalCurrencySymbol.current
    val currencyFormat = LocalCurrencyFormat.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Top bar ───────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(Modifier.width(4.dp))
            Text(
                "Calendar View",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp)
        ) {
            // ── Month navigation header ────────────────────────────────────────
            MonthHeader(
                yearMonth = uiState.yearMonth,
                txnCount = uiState.monthTransactionCount,
                onPrevious = viewModel::previousMonth,
                onNext = viewModel::nextMonth
            )

            Spacer(Modifier.height(20.dp))

            // ── Day-of-week header row ─────────────────────────────────────────
            DayOfWeekHeader()

            Spacer(Modifier.height(8.dp))

            // ── Calendar grid ─────────────────────────────────────────────────
            CalendarGrid(
                yearMonth = uiState.yearMonth,
                dayDataMap = uiState.dayDataMap,
                currencySymbol = currencySymbol,
                currencyFormat = currencyFormat,
                onDayClick     = onDayClick
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ─── Month navigation pill ────────────────────────────────────────────────────

@Composable
private fun MonthHeader(
    yearMonth: YearMonth,
    txnCount: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    val monthFmt = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault())

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 4.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onPrevious) {
                Icon(
                    Icons.Default.ChevronLeft, "Previous month",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    yearMonth.format(monthFmt),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (txnCount > 0) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "$txnCount TRANSACTIONS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.2.sp
                    )
                }
            }

            IconButton(onClick = onNext) {
                Icon(
                    Icons.Default.ChevronRight, "Next month",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ─── S  M  T  W  T  F  S header ──────────────────────────────────────────────

@Composable
private fun DayOfWeekHeader() {
    // Sunday-first week, matching the screenshot
    val labels = listOf("S", "M", "T", "W", "T", "F", "S")
    Row(modifier = Modifier.fillMaxWidth()) {
        labels.forEach { lbl ->
            Text(
                lbl,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ─── Calendar grid ────────────────────────────────────────────────────────────

@Composable
private fun CalendarGrid(
    yearMonth: YearMonth,
    dayDataMap: Map<LocalDate, DayData>,
    currencySymbol: String,
    currencyFormat: String,
    onDayClick: (LocalDate) -> Unit = {}
) {
    val firstDay = yearMonth.atDay(1)
    // DayOfWeek.SUNDAY = 7 in Java's enum — convert to 0-based Sunday-first index
    val startOffset = when (firstDay.dayOfWeek) {
        DayOfWeek.SUNDAY -> 0
        DayOfWeek.MONDAY -> 1
        DayOfWeek.TUESDAY -> 2
        DayOfWeek.WEDNESDAY -> 3
        DayOfWeek.THURSDAY -> 4
        DayOfWeek.FRIDAY -> 5
        DayOfWeek.SATURDAY -> 6
    }
    val daysInMonth = yearMonth.lengthOfMonth()
    // Total cells needed (pad to complete last row)
    val totalCells = startOffset + daysInMonth
    val rows = (totalCells + 6) / 7   // ceil division

    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        repeat(rows) { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                repeat(7) { col ->
                    val cellIndex = row * 7 + col
                    val dayNum = cellIndex - startOffset + 1

                    if (dayNum !in 1..daysInMonth) {
                        // Empty cell
                        Box(modifier = Modifier.weight(1f))
                    } else {
                        val date = yearMonth.atDay(dayNum)
                        val dayData = dayDataMap[date]
                        DayCell(
                            dayNum = dayNum,
                            dayData = dayData,
                            currencySymbol = currencySymbol,
                            currencyFormat = currencyFormat,
                            onClick = { onDayClick(date) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

// ─── Individual day cell ──────────────────────────────────────────────────────

@Composable
private fun DayCell(
    dayNum: Int,
    dayData: DayData?,
    currencySymbol: String,
    currencyFormat: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    // Thin divider lines between cells — matching the screenshot
    Box(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)
            )
            .clickable { onClick() }
            .padding(1.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 2.dp, vertical = 6.dp)
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // Day number — large, top of cell
                Text(
                    text = dayNum.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Amount pills — shown only when there are transactions
                if (dayData != null) {
                    // Income pill (green) — shown if income > 0
                    if (dayData.totalIncome > 0) {
                        AmountPill(
                            amount = dayData.totalIncome,
                            currencySymbol = currencySymbol,
                            currencyFormat = currencyFormat,
                            bgColor = IncomePillBg,
                            textColor = IncomePillText
                        )
                    }
                    // Expense pill (red) — shown if expense > 0
                    if (dayData.totalExpense > 0) {
                        AmountPill(
                            amount = dayData.totalExpense,
                            currencySymbol = currencySymbol,
                            currencyFormat = currencyFormat,
                            bgColor = ExpensePillBg,
                            textColor = ExpensePillText
                        )
                    }
                } else {
                    // Placeholder to keep cell height uniform when no data
                    Spacer(Modifier.height(22.dp))
                }
            }
        }
    }
}

// ─── Colored amount pill ──────────────────────────────────────────────────────

@Composable
private fun AmountPill(
    amount: Double,
    currencySymbol: String,
    currencyFormat: String,
    bgColor: Color,
    textColor: Color
) {
    // Use smartFormat for compact display matching the screenshot style
    val formatted = amount.smartFormat("default")
    val text = "$currencySymbol$formatted"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .padding(horizontal = 2.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = textColor,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 8.sp
        )
    }
}