package com.expensetracker.presentation.ui.views

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
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
import com.expensetracker.util.FormatUtils.formatAmountForDisplay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

// ── Amount pill colors — exactly matching the screenshot ──────────────────────
private val ExpensePillBg = Color(0xFFA26363)   // dark red/maroon pill
private val ExpensePillText = Color(0xFFFFE9EB)  // light pink text
private val IncomePillBg = Color(0xFF588574)   // dark green pill
private val IncomePillText = Color(0xFFC7FFC9)  // light green text

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
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 20.sp),
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

            Spacer(Modifier.height(32.dp))

            // ── Day-of-week header row ─────────────────────────────────────────
            DayOfWeekHeader()

            Spacer(Modifier.height(8.dp))

            // ── Calendar grid ─────────────────────────────────────────────────
            CalendarGrid(
                yearMonth = uiState.yearMonth,
                dayDataMap = uiState.dayDataMap,
                currencySymbol = currencySymbol,
                currencyFormat = currencyFormat,
                onDayClick = onDayClick
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
                style = MaterialTheme.typography.bodyMedium,
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
                        // Empty cell — same height as real cells
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = CELL_HEIGHT.dp)
                        )
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
//
// Fixed-height design:
//   • Every cell is exactly CELL_HEIGHT dp tall — no wrapping, no expansion.
//   • Day number row: fixed 28dp, centred.
//   • Up to 2 pill rows each 18dp, always rendered (empty space when no data).
//   • This guarantees all cells in the same row are identical height.

private const val CELL_HEIGHT = 80  // dp — enough for day num + 2 pills

@Composable
private fun DayCell(
    dayNum: Int,
    dayData: DayData?,
    currencySymbol: String,
    currencyFormat: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f))
            .clickable { onClick() }
            .padding(1.dp)
            .background(MaterialTheme.colorScheme.background)
            .defaultMinSize(minHeight = CELL_HEIGHT.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 3.dp, vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            // Day number — fixed 28dp zone, centred
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = dayNum.toString(),
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 18.sp
                )
            }

            // Income pill — always renders as 18dp slot (transparent when no income)
            // Income pill slot — transparent with no text when income = 0
            AmountPill(
                amount = dayData?.totalIncome ?: 0.0,
                currencySymbol = currencySymbol,
                visible = (dayData?.totalIncome ?: 0.0) > 0,
                bgColor = IncomePillBg,
                textColor = IncomePillText
            )

            // Expense pill slot — transparent with no text when expense = 0
            AmountPill(
                amount = dayData?.totalExpense ?: 0.0,
                currencySymbol = currencySymbol,
                visible = (dayData?.totalExpense ?: 0.0) > 0,
                bgColor = ExpensePillBg,
                textColor = ExpensePillText
            )
        }
    }
}

// ─── Colored amount pill ──────────────────────────────────────────────────────

@Composable
private fun AmountPill(
    amount: Double,
    currencySymbol: String,
    visible: Boolean,
    bgColor: Color,
    textColor: Color
) {
    val rounded = amount.roundToInt().toDouble()
    val currencyFormat = LocalCurrencyFormat.current
    val text = if (visible)
        "$currencySymbol${formatAmountForDisplay(rounded, currencyFormat)}"
    else ""

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(18.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (visible) bgColor else Color.Transparent),
        contentAlignment = Alignment.Center
    ) {
        if (visible) {
            Text(
                text = text,
                color = textColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = 9.sp,
                lineHeight = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}