package com.expensetracker.presentation.ui.analysis

import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.expensetracker.domain.model.TransactionType
import com.expensetracker.presentation.components.AppBottomBar
import com.expensetracker.presentation.components.CategoryIconBubble
import com.expensetracker.presentation.components.LocalCurrencyFormat
import com.expensetracker.presentation.theme.ExpenseRed
import com.expensetracker.presentation.theme.IncomeGreen
import com.expensetracker.util.FormatUtils.formatAmountForDisplay
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)

// ─── Amount formatting helper ─────────────────────────────────────────────────
/** Formats an amount without trailing .00 — e.g. 10.0 → "10", 10.5 → "10.50" */
@Composable
private fun fmtAmt(amount: Double): String =
    formatAmountForDisplay(amount, LocalCurrencyFormat.current)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(
    onNavigateBack: () -> Unit,
    onNavigateToHome: () -> Unit = {},
    onNavigateToAccounts: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToAddTransaction: () -> Unit = {},
    viewModel: AnalysisViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }
    var showComparePicker by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            AppBottomBar(
                currentRoute = "analysis",
                onHome = onNavigateToHome,
                onAnalysis = {},
                onAccounts = onNavigateToAccounts,
                onSettings = onNavigateToSettings,
                onAddTransaction = onNavigateToAddTransaction
            )
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Analysis",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {

            // ── Period tab selector ───────────────────────────────────────────
            item {
                PeriodTabRow(
                    selected = uiState.period,
                    onSelect = { p ->
                        if (p == AnalysisPeriod.CUSTOM) showDatePicker = true
                        else viewModel.setPeriod(p)
                    }
                )
            }

            // ── Period navigation row ─────────────────────────────────────────
            item {
                if (uiState.period != AnalysisPeriod.CUSTOM) {
                    PeriodNavRow(
                        label = uiState.periodLabel,
                        count = uiState.transactionCount,
                        onPrev = viewModel::previousPeriod,
                        onNext = viewModel::nextPeriod
                    )
                } else {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            uiState.periodLabel,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // ── Summary card ──────────────────────────────────────────────────
            item {
                Spacer(Modifier.height(8.dp))
                SummaryCard(
                    sym = uiState.currencySymbol,
                    expense = uiState.totalExpense,
                    income = uiState.totalIncome,
                    balance = uiState.netBalance,
                    expenseChangePercent = uiState.expenseChangePercent,
                    incomeChangePercent = uiState.incomeChangePercent
                )
            }

            // ── Trends ────────────────────────────────────────────────────────
            item {
                SectionTitle(title = "Trends")
            }
            item {
                TrendCard(
                    period = uiState.period,
                    points = uiState.dailyPoints,
                    comparisonPoints = uiState.comparisonPoints,
                    dayWisePoints = uiState.dayWisePoints,
                    comparisonDayWisePoints = uiState.comparisonDayWisePoints,
                    compareEnabled = uiState.compareEnabled,
                    compareLabel = uiState.comparePeriodLabel,
                    periodLabel = uiState.periodLabel,
                    viewType = uiState.trendsViewType,
                    onToggleCompare = viewModel::setCompareEnabled,
                    onToggleViewType = { viewModel.setTrendsViewType(it) },
                    onPickCompare = { showComparePicker = true }
                )
            }

            // ── Categories ────────────────────────────────────────────────────
            item {
                SectionTitle(
                    title = "Categories",
                    viewType = uiState.categoriesViewType,
                    onToggle = { viewModel.setCategoriesViewType(it) }
                )
            }
            item {
                CategoryCard(
                    breakdown = uiState.categoryBreakdown,
                    viewType = uiState.categoriesViewType,
                    sym = uiState.currencySymbol
                )
            }

            // ── Payment modes ─────────────────────────────────────────────────
            if (uiState.paymentModeBreakdown.isNotEmpty()) {
                item {
                    SectionTitle(title = "Payment Modes")
                }
                item {
                    PaymentModesCard(
                        modes = uiState.paymentModeBreakdown,
                        sym = uiState.currencySymbol,
                        viewType = uiState.paymentViewType,
                        onToggle = { viewModel.setPaymentViewType(it) }
                    )
                }
            }

            // ── Stats ─────────────────────────────────────────────────────────
            item {
                SectionTitle(title = "Stats")
            }
            item {
                StatsCard(stats = uiState.stats, sym = uiState.currencySymbol)
            }
        }
    }

    if (showDatePicker) {
        CustomRangeDialog(
            onDismiss = { showDatePicker = false },
            onConfirm = { s, e ->
                viewModel.setCustomRange(s, e)
                showDatePicker = false
            }
        )
    }

    if (showComparePicker) {
        when (uiState.period) {
            AnalysisPeriod.MONTH -> MonthCompareDialog(
                initial = uiState.compareMonth ?: YearMonth.now().minusMonths(1),
                onDismiss = { showComparePicker = false },
                onConfirm = {
                    viewModel.setCompareMonth(it)
                    showComparePicker = false
                }
            )

            AnalysisPeriod.YEAR -> YearCompareDialog(
                initialYear = uiState.compareYear ?: (LocalDate.now().year - 1),
                onDismiss = { showComparePicker = false },
                onConfirm = {
                    viewModel.setCompareYear(it)
                    showComparePicker = false
                }
            )

            else -> Unit
        }
    }
}

// ─── Section Title with optional Spending/Income toggle ──────────────────────

@Composable
private fun SectionTitle(
    title: String,
    viewType: TransactionType? = null,
    onToggle: ((TransactionType) -> Unit)? = null
) {
    Spacer(Modifier.height(20.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        if (viewType != null && onToggle != null) {
            // Compact pill toggle aligned to the right of the section header
            CompactToggle(selected = viewType, onToggle = onToggle)
        }
    }
}

// ─── Compact Spending/Income Toggle ──────────────────────────────────────────

@Composable
private fun CompactToggle(selected: TransactionType, onToggle: (TransactionType) -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(3.dp)
    ) {
        listOf(TransactionType.EXPENSE to "Spending", TransactionType.INCOME to "Income")
            .forEach { (type, label) ->
                val active = selected == type
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (active) MaterialTheme.colorScheme.primaryContainer
                            else Color.Transparent
                        )
                        .clickable { onToggle(type) }
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                        color = if (active) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
    }
}

// ─── Period Tab Row ───────────────────────────────────────────────────────────

@Composable
private fun PeriodTabRow(selected: AnalysisPeriod, onSelect: (AnalysisPeriod) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        listOf(
            AnalysisPeriod.WEEK to "Week",
            AnalysisPeriod.MONTH to "Month",
            AnalysisPeriod.YEAR to "Year",
            AnalysisPeriod.CUSTOM to "Custom"
        ).forEach { (period, label) ->
            val isSelected = selected == period
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.surface
                        else Color.Transparent
                    )
                    .clickable { onSelect(period) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ─── Period Nav Row ───────────────────────────────────────────────────────────

@Composable
private fun PeriodNavRow(label: String, count: Int, onPrev: () -> Unit, onNext: () -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onPrev) {
                Icon(Icons.Default.ChevronLeft, "Previous")
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "$count TRANSACTIONS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.8.sp
                )
            }
            IconButton(onClick = onNext) {
                Icon(Icons.Default.ChevronRight, "Next")
            }
        }
    }
}

// ─── Summary Card ─────────────────────────────────────────────────────────────

@Composable
private fun SummaryCard(
    sym: String,
    expense: Double,
    income: Double,
    balance: Double,
    expenseChangePercent: Float?,
    incomeChangePercent: Float?
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "SPENDING",
                        style = MaterialTheme.typography.labelSmall,
                        color = ExpenseRed, fontWeight = FontWeight.Bold, letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    AutoResizingAmountText(
                        text = "$sym${fmtAmt(expense)}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth()
                    )
                    ChangeIndicator(
                        changePercent = expenseChangePercent,
                        positiveColor = ExpenseRed,
                        negativeColor = IncomeGreen,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text(
                        "INCOME",
                        style = MaterialTheme.typography.labelSmall,
                        color = IncomeGreen, fontWeight = FontWeight.Bold, letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    AutoResizingAmountText(
                        text = "$sym${fmtAmt(income)}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End
                    )
                    ChangeIndicator(
                        changePercent = incomeChangePercent,
                        positiveColor = IncomeGreen,
                        negativeColor = ExpenseRed,
                        modifier = Modifier.padding(top = 4.dp),
                        textAlign = TextAlign.End
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Net Balance",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    AutoResizingAmountText(
                        text = "${if (balance >= 0) "+" else ""}$sym${fmtAmt(balance)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (balance >= 0) IncomeGreen else ExpenseRed,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.End,
                        maxFontSize = MaterialTheme.typography.titleMedium.fontSize,
                        minFontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ChangeIndicator(
    changePercent: Float?,
    positiveColor: Color,
    negativeColor: Color,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start
) {
    if (changePercent == null) {
        Spacer(Modifier.height(18.dp))
        return
    }

    val isNeutral = abs(changePercent) < 0.05f
    val isIncrease = changePercent > 0f
    val color = when {
        isNeutral -> MaterialTheme.colorScheme.onSurfaceVariant
        isIncrease -> positiveColor
        else -> negativeColor
    }
    val magnitude = abs(changePercent)

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (textAlign == TextAlign.End) Arrangement.End else Arrangement.Start
    ) {
        if (!isNeutral) {
            Icon(
                imageVector = if (isIncrease) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = "${"%.1f".format(magnitude)}%",
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.SemiBold,
            textAlign = textAlign
        )
    }
}

@Composable
private fun AutoResizingAmountText(
    text: String,
    style: TextStyle,
    fontWeight: FontWeight,
    color: Color,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start,
    maxFontSize: TextUnit = style.fontSize,
    minFontSize: TextUnit = 18.sp
) {
    var fontSize by remember(text) { mutableStateOf(maxFontSize) }
    var readyToDraw by remember(text) { mutableStateOf(false) }

    Text(
        text = text,
        style = style.copy(fontSize = fontSize),
        fontWeight = fontWeight,
        color = color,
        textAlign = textAlign,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        modifier = modifier.drawWithContent {
            if (readyToDraw) drawContent()
        },
        onTextLayout = { result ->
            if (result.didOverflowWidth) {
                val next = (fontSize.value - 1f).sp
                if (next.value >= minFontSize.value) {
                    fontSize = next
                } else {
                    readyToDraw = true
                }
            } else {
                readyToDraw = true
            }
        }
    )
}

// ─── Trend Card ───────────────────────────────────────────────────────────────

// ─── Chart Legend ─────────────────────────────────────────────────────────────

@Composable
private fun ChartLegend(
    currentLabel: String,
    comparisonLabel: String?,
    currentColor: Color,
    comparisonColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        LegendDot(color = currentColor, label = currentLabel)
        if (comparisonLabel != null) {
            LegendDot(
                color = comparisonColor,
                label = comparisonLabel,
                dashed = true
            )
        }
    }
}

@Composable
private fun LegendDot(
    color: Color,
    label: String,
    dashed: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        // Solid or dashed line swatch — 20×3dp, matches chart line style
        Canvas(Modifier.size(width = 20.dp, height = 3.dp)) {
            drawLine(
                color = color,
                start = Offset(0f, size.height / 2f),
                end = Offset(size.width, size.height / 2f),
                strokeWidth = size.height,
                cap = StrokeCap.Round,
                pathEffect = if (dashed)
                    PathEffect.dashPathEffect(floatArrayOf(6f, 5f))
                else null
            )
        }
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun TrendCard(
    period: AnalysisPeriod,
    points: List<DailyPoint>,
    comparisonPoints: List<DailyPoint>,
    dayWisePoints: List<DailyPoint>,
    comparisonDayWisePoints: List<DailyPoint>,
    compareEnabled: Boolean,
    compareLabel: String?,
    periodLabel: String,
    viewType: TransactionType,
    onToggleCompare: (Boolean) -> Unit,
    onToggleViewType: (TransactionType) -> Unit,
    onPickCompare: () -> Unit
) {
    Spacer(Modifier.height(12.dp))
    // Blue for current period (expense), green for income
    val currentColor = if (viewType == TransactionType.EXPENSE) Color(0xFF5AA2FF) else IncomeGreen
    // Salmon/red for comparison (matches screenshot)
    val comparisonColor = Color(0xFFE87878)
    Card(
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            if (points.isEmpty()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No data for this period",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // ── Spending/Income toggle sits inside the card, above the chart ──
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CompactToggle(selected = viewType, onToggle = onToggleViewType)
                }
                // Legend — only shown when comparison is active
                if (compareEnabled && compareLabel != null) {
                    ChartLegend(
                        currentLabel = periodLabel,
                        comparisonLabel = compareLabel,
                        currentColor = currentColor,
                        comparisonColor = comparisonColor
                    )
                }
                Spacer(Modifier.height(12.dp))

                when (period) {
                    // YEAR → monthly bar chart
                    AnalysisPeriod.YEAR -> {
                        ComparisonBarChart(
                            points = points,
                            comparisonPoints = if (compareEnabled) comparisonPoints else emptyList(),
                            currentColor = currentColor,
                            comparisonColor = comparisonColor,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        )
                    }
                    // MONTH → cumulative line + day-wise bar
                    AnalysisPeriod.MONTH -> {
                        Text(
                            "Cumulative",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(6.dp))
                        ComparisonLineChart(
                            points = points,
                            comparisonPoints = if (compareEnabled) comparisonPoints else emptyList(),
                            currentColor = currentColor,
                            comparisonColor = comparisonColor,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                        )
                        Spacer(Modifier.height(20.dp))
                        Text(
                            "Day-wise",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        ComparisonBarChart(
                            points = dayWisePoints,
                            comparisonPoints = if (compareEnabled) comparisonDayWisePoints else emptyList(),
                            currentColor = currentColor,
                            comparisonColor = comparisonColor,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        )
                    }
                    // WEEK / CUSTOM → line chart only
                    else -> {
                        ComparisonLineChart(
                            points = points,
                            comparisonPoints = if (compareEnabled) comparisonPoints else emptyList(),
                            currentColor = currentColor,
                            comparisonColor = comparisonColor,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                        )
                    }
                }

                if (period == AnalysisPeriod.MONTH || period == AnalysisPeriod.YEAR) {
                    Spacer(Modifier.height(18.dp))
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                        thickness = 0.5.dp
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.clickable { onPickCompare() },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                "Compare with",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                compareLabel
                                    ?: if (period == AnalysisPeriod.MONTH) "another month" else "another year",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Switch(
                            checked = compareEnabled,
                            onCheckedChange = onToggleCompare
                        )
                    }
                }
            }
        }
    }
}

// ─── Category Card ────────────────────────────────────────────────────────────
@Composable
private fun CategoryCard(
    breakdown: List<CategorySpend>,
    viewType: TransactionType,
    sym: String
) {
    Spacer(Modifier.height(12.dp))
    Card(
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            if (breakdown.isEmpty()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No ${if (viewType == TransactionType.EXPENSE) "spending" else "income"} data",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Donut chart — no toggle above it anymore, so no collision
                DonutChart(
                    breakdown = breakdown,
                    modifier = Modifier
                        .size(240.dp)
                        .padding(vertical = 8.dp)   // breathing room top/bottom
                        .align(Alignment.CenterHorizontally)
                )
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    thickness = 0.5.dp
                )
                Spacer(Modifier.height(12.dp))
                breakdown.forEach { cs ->
                    CategoryRow(cs = cs, sym = sym, viewType = viewType)
                    if (cs != breakdown.last()) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            thickness = 0.5.dp
                        )
                    }
                }
            }
        }
    }
}

// ─── Payment Modes Card ───────────────────────────────────────────────────────

@Composable
private fun PaymentModesCard(
    modes: List<PaymentModeSpend>,
    sym: String,
    viewType: TransactionType,
    onToggle: (TransactionType) -> Unit
) {
    Spacer(Modifier.height(12.dp))
    Card(
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            // Three-way toggle: Spending / Income / Transfers
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(3.dp)
            ) {
                listOf(
                    TransactionType.EXPENSE to "Spending",
                    TransactionType.INCOME to "Income",
                    TransactionType.TRANSFER to "Transfers"
                ).forEach { (type, label) ->
                    val active = viewType == type
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (active) MaterialTheme.colorScheme.primaryContainer
                                else Color.Transparent
                            )
                            .clickable { onToggle(type) }
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                            color = if (active) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            val filtered = modes.filter { mode ->
                when (viewType) {
                    TransactionType.EXPENSE -> mode.expense > 0
                    TransactionType.INCOME -> mode.income > 0
                    TransactionType.TRANSFER -> mode.transfer > 0
                }
            }

            if (filtered.isEmpty()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(80.dp), contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No data for this period",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                filtered.forEachIndexed { idx, mode ->
                    val amount = when (viewType) {
                        TransactionType.EXPENSE -> mode.expense
                        TransactionType.INCOME -> mode.income
                        TransactionType.TRANSFER -> mode.transfer
                    }
                    PaymentModeRow(
                        name = mode.modeName,
                        amount = amount,
                        sym = sym
                    )
                    if (idx < filtered.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            thickness = 0.5.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentModeRow(name: String, amount: Double, sym: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon based on mode name heuristic
        val icon = when {
            name.contains("UPI", ignoreCase = true) -> Icons.Default.PhoneAndroid
            name.contains("Cash", ignoreCase = true) -> Icons.Default.Payments
            name.contains("Credit", ignoreCase = true) -> Icons.Default.CreditCard
            name.contains("Debit", ignoreCase = true) -> Icons.Default.CreditCard
            name.contains("Net", ignoreCase = true) -> Icons.Default.Language
            name.contains("Cheque", ignoreCase = true) -> Icons.Default.EditNote
            else -> Icons.Default.AccountBalance
        }
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon, null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            "$sym${fmtAmt(amount)}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

// ─── Stats Card ───────────────────────────────────────────────────────────────

@Composable
private fun StatsCard(stats: AnalysisStats, sym: String) {
    Spacer(Modifier.height(12.dp))
    Card(
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

            // Average Spending
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "AVERAGE SPENDING",
                    style = MaterialTheme.typography.labelSmall,
                    color = ExpenseRed,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Row(Modifier.fillMaxWidth()) {
                    StatCell(
                        label = "Per day",
                        value = "$sym${fmtAmt(stats.avgExpensePerDay)}",
                        modifier = Modifier.weight(1f)
                    )
                    StatCell(
                        label = "Per transaction",
                        value = "$sym${fmtAmt(stats.avgExpensePerTxn)}",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                thickness = 0.5.dp
            )

            // Average Income
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "AVERAGE INCOME",
                    style = MaterialTheme.typography.labelSmall,
                    color = IncomeGreen,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Row(Modifier.fillMaxWidth()) {
                    StatCell(
                        label = "Per day",
                        value = "$sym${fmtAmt(stats.avgIncomePerDay)}",
                        modifier = Modifier.weight(1f)
                    )
                    StatCell(
                        label = "Per transaction",
                        value = "$sym${fmtAmt(stats.avgIncomePerTxn)}",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(2.dp))
        Text(
            value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

// ─── Line Chart (Canvas) ──────────────────────────────────────────────────────

@Composable
private fun ComparisonLineChart(
    points: List<DailyPoint>,
    comparisonPoints: List<DailyPoint>,
    currentColor: Color,
    comparisonColor: Color,
    modifier: Modifier = Modifier
) {
    val animProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(900, easing = EaseOutCubic),
        label = "line_anim"
    )
    val currencyFormat = LocalCurrencyFormat.current
    val currencySymbol = com.expensetracker.presentation.components.LocalCurrencySymbol.current

    // Touch state — index of selected point (-1 = none)
    var selectedIdx by remember { mutableIntStateOf(-1) }
    // Pixel X of the last touch (for positioning the tooltip line)
    var touchX by remember { mutableFloatStateOf(0f) }

    // Layout measurements captured from Canvas layout pass
    var padL by remember { mutableFloatStateOf(52f) }
    var chartW by remember { mutableFloatStateOf(1f) }
    var totalW by remember { mutableFloatStateOf(1f) }
    var stepX by remember { mutableFloatStateOf(1f) }

    val labelStep = maxOf(1, points.size / 6)

    Box(modifier = modifier) {
        // Tooltip overlay (drawn outside Canvas so it can use Compose layout)
        if (selectedIdx >= 0 && selectedIdx < points.size) {
            val pt = points[selectedIdx]
            val tooltipX = (padL + selectedIdx * stepX)
            val tooltipFraction = tooltipX / totalW.coerceAtLeast(1f)

            // Format label as date if numeric (day number), else keep raw label
            val displayLabel = pt.label
            val amountText =
                "$currencySymbol${formatAmountForDisplay(pt.cumulative, currencyFormat)}"

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.TopStart
            ) {
                (tooltipFraction * 1f)  // layout handled by offset below
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(
                            start = maxOf(
                                0.dp,
                                ((tooltipFraction * (totalW - padL * 2)) / totalW * 100).dp - 24.dp
                            )
                        )
                        .background(
                            MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.92f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        displayLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.inverseOnSurface,
                        fontWeight = FontWeight.Normal
                    )
                    Text(
                        amountText,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.inverseOnSurface,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(points.size) {
                    detectTapGestures { offset ->
                        if (points.size <= 1) return@detectTapGestures
                        val step = (size.width - padL - 12f) / (points.size - 1).coerceAtLeast(1)
                        val idx = ((offset.x - padL) / step).roundToInt()
                            .coerceIn(0, points.lastIndex)
                        selectedIdx = if (selectedIdx == idx) -1 else idx
                    }
                }
                .pointerInput(points.size) {
                    detectHorizontalDragGestures { change, _ ->
                        if (points.size <= 1) return@detectHorizontalDragGestures
                        val step = (size.width - padL - 12f) / (points.size - 1).coerceAtLeast(1)
                        val idx = ((change.position.x - padL) / step).roundToInt()
                            .coerceIn(0, points.lastIndex)
                        selectedIdx = idx
                        touchX = change.position.x
                    }
                }
        ) {
            val w = size.width
            val h = size.height
            val pL = 52f
            val padB = 32f
            val padT = 24f
            val cW = w - pL - 12f
            val cH = h - padB - padT
            val maxPointCount = maxOf(points.size, comparisonPoints.size).coerceAtLeast(1)
            val step = if (maxPointCount > 1) cW / (maxPointCount - 1) else cW

            // Capture for tooltip
            padL = pL
            chartW = cW
            totalW = w
            stepX = step

            val maxVal = maxOf(
                points.maxOfOrNull { it.cumulative } ?: 0.0,
                comparisonPoints.maxOfOrNull { it.cumulative } ?: 0.0,
                1.0
            )

            fun xOf(i: Int) = pL + i * step
            fun yOf(v: Double) = padT + cH * (1.0 - (v / maxVal)).toFloat()

            // ── Grid lines ─────────────────────────────────────────────────────
            val gridCount = 4
            for (g in 0..gridCount) {
                val y = padT + cH * g / gridCount.toFloat()
                drawLine(
                    Color.Gray.copy(alpha = 0.10f),
                    Offset(pL, y), Offset(w - 12f, y),
                    strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                )
                if (g < gridCount) {
                    val labelVal = maxVal * (1.0 - g / gridCount.toDouble())
                    drawContext.canvas.nativeCanvas.drawText(
                        formatAxisVal(labelVal, currencyFormat), 0f, y + 5f,
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.argb(140, 128, 128, 128)
                            textSize = 18f; isAntiAlias = true
                        }
                    )
                }
            }

            // ── Axes ───────────────────────────────────────────────────────────
            val axisAlpha = 0.25f
            drawLine(
                Color.Gray.copy(alpha = axisAlpha),
                Offset(pL, padT), Offset(pL, padT + cH), strokeWidth = 1f
            )
            drawLine(
                Color.Gray.copy(alpha = axisAlpha),
                Offset(pL, padT + cH), Offset(w - 12f, padT + cH), strokeWidth = 1f
            )

            // ── Smooth path builder ────────────────────────────────────────────
            fun buildPath(series: List<DailyPoint>, progress: Float): Path {
                val path = Path()
                if (series.isEmpty()) return path
                path.moveTo(xOf(0), yOf(series[0].cumulative * progress))
                for (i in 1 until series.size) {
                    val px = xOf(i - 1)
                    val cy2 = xOf(i)
                    val py = yOf(series[i - 1].cumulative * progress)
                    val qy = yOf(series[i].cumulative * progress)
                    val cp1x = px + (cy2 - px) * 0.5f
                    val cp2x = cy2 - (cy2 - px) * 0.5f
                    path.cubicTo(cp1x, py, cp2x, qy, cy2, qy)
                }
                return path
            }

            // ── Current series ─────────────────────────────────────────────────
            if (points.isNotEmpty()) {
                val linePath = buildPath(points, animProgress)

                // Gradient fill underline
                val fillPath = Path().apply {
                    addPath(linePath)
                    lineTo(xOf(points.lastIndex), padT + cH)
                    lineTo(xOf(0), padT + cH)
                    close()
                }
                drawPath(
                    fillPath,
                    brush = Brush.verticalGradient(
                        0f to currentColor.copy(alpha = 0.28f),
                        1f to Color.Transparent,
                        startY = padT, endY = padT + cH
                    )
                )

                // Line
                drawPath(
                    linePath,
                    color = currentColor,
                    style = Stroke(width = 3.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Dots — highlight selected
                for (i in points.indices) {
                    val cx = xOf(i)
                    val cy = yOf(points[i].cumulative * animProgress)
                    if (i == selectedIdx) {
                        // Vertical guide line
                        drawLine(
                            currentColor.copy(alpha = 0.25f),
                            Offset(cx, padT), Offset(cx, padT + cH),
                            strokeWidth = 1.5f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                        )
                        // Large highlighted dot
                        drawCircle(currentColor.copy(alpha = 0.20f), 12f, Offset(cx, cy))
                        drawCircle(currentColor, 6f, Offset(cx, cy))
                        drawCircle(Color.White, 3f, Offset(cx, cy))
                    } else {
                        drawCircle(currentColor, 3.5f, Offset(cx, cy))
                        drawCircle(Color.White, 1.8f, Offset(cx, cy))
                    }
                }
            }

            // ── Comparison series ──────────────────────────────────────────────
            if (comparisonPoints.isNotEmpty()) {
                val compPath = buildPath(comparisonPoints, animProgress)
                drawPath(
                    compPath,
                    color = comparisonColor,
                    style = Stroke(
                        width = 2.2f, cap = StrokeCap.Round, join = StrokeJoin.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
                    )
                )
                for (i in comparisonPoints.indices) {
                    val cx = xOf(i)
                    val cy = yOf(comparisonPoints[i].cumulative * animProgress)
                    if (i == selectedIdx) {
                        drawCircle(comparisonColor, 5.5f, Offset(cx, cy))
                        drawCircle(Color.White, 2.5f, Offset(cx, cy))
                    } else {
                        drawCircle(
                            comparisonColor, 3.5f, Offset(cx, cy),
                            style = Stroke(width = 2f)
                        )
                    }
                }
            }

            // ── X-axis labels ──────────────────────────────────────────────────
            for (i in points.indices step labelStep) {
                drawContext.canvas.nativeCanvas.drawText(
                    points[i].label,
                    xOf(i) - 14f, h - 4f,
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.argb(150, 128, 128, 128)
                        textSize = 19f; isAntiAlias = true
                    }
                )
            }
        }
    }
}

@Composable
private fun ComparisonBarChart(
    points: List<DailyPoint>,
    comparisonPoints: List<DailyPoint>,
    currentColor: Color,
    comparisonColor: Color,
    modifier: Modifier = Modifier
) {
    val currencyFormat = LocalCurrencyFormat.current
    val currencySymbol = com.expensetracker.presentation.components.LocalCurrencySymbol.current
    val labelStep = maxOf(1, points.size / 6)
    val maxPointCount = maxOf(points.size, comparisonPoints.size).coerceAtLeast(1)
    val currentAvg = if (points.isNotEmpty()) points.map { it.amount }.average() else 0.0
    val comparisonAvg =
        if (comparisonPoints.isNotEmpty()) comparisonPoints.map { it.amount }.average() else 0.0

    var selectedIdx by remember { mutableIntStateOf(-1) }
    var padLState by remember { mutableFloatStateOf(52f) }
    var stepState by remember { mutableFloatStateOf(1f) }

    Box(modifier = modifier) {
        // Tooltip
        if (selectedIdx >= 0 && selectedIdx < points.size) {
            val pt = points[selectedIdx]
            val cpt = comparisonPoints.getOrNull(selectedIdx)
            ((padLState + selectedIdx * stepState) / 1f).coerceIn(0f, 1f)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.TopStart
            ) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = maxOf(0.dp, (selectedIdx * 8 - 16).dp))
                        .background(
                            MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.92f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        pt.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.inverseOnSurface
                    )
                    Text(
                        "$currencySymbol${formatAmountForDisplay(pt.amount, currencyFormat)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = currentColor,
                        fontWeight = FontWeight.Bold
                    )
                    if (cpt != null && cpt.amount > 0) {
                        Text(
                            "$currencySymbol${formatAmountForDisplay(cpt.amount, currencyFormat)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = comparisonColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(points.size) {
                    detectTapGestures { offset ->
                        if (points.isEmpty()) return@detectTapGestures
                        val step =
                            (size.width - padLState - 12f) / (maxPointCount - 1).coerceAtLeast(1)
                        val idx = ((offset.x - padLState) / step).roundToInt()
                            .coerceIn(0, points.lastIndex)
                        selectedIdx = if (selectedIdx == idx) -1 else idx
                    }
                }
                .pointerInput(points.size) {
                    detectHorizontalDragGestures { change, _ ->
                        val step =
                            (size.width - padLState - 12f) / (maxPointCount - 1).coerceAtLeast(1)
                        val idx = ((change.position.x - padLState) / step).roundToInt()
                            .coerceIn(0, points.lastIndex)
                        selectedIdx = idx
                    }
                }
        ) {
            val w = size.width
            val h = size.height
            val pL = 52f
            val padB = 34f
            val padT = 24f
            val cW = w - pL - 12f
            val cH = h - padB - padT

            val maxVal = maxOf(
                points.maxOfOrNull { it.amount } ?: 0.0,
                comparisonPoints.maxOfOrNull { it.amount } ?: 0.0,
                1.0
            )
            val step = if (maxPointCount > 1) cW / (maxPointCount - 1) else cW
            padLState = pL; stepState = step

            val barWidth = if (comparisonPoints.isNotEmpty()) step * 0.22f else step * 0.38f
            val barRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)

            fun xOf(i: Int) = pL + i * step
            fun yOf(v: Double) = padT + cH * (1.0 - v / maxVal).toFloat()

            // Grid
            for (g in 0..4) {
                val y = padT + cH * g / 4f
                drawLine(
                    Color.Gray.copy(alpha = 0.09f),
                    Offset(pL, y), Offset(w - 12f, y),
                    strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 8f))
                )
                if (g < 4) {
                    val lv = maxVal * (1.0 - g / 4.0)
                    drawContext.canvas.nativeCanvas.drawText(
                        formatAxisVal(lv, currencyFormat), 0f, y + 5f,
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.argb(130, 128, 128, 128)
                            textSize = 18f; isAntiAlias = true
                        }
                    )
                }
            }
            drawLine(
                Color.Gray.copy(alpha = 0.2f),
                Offset(pL, padT + cH), Offset(w - 12f, padT + cH), strokeWidth = 1f
            )

            // Bars
            points.forEachIndexed { i, pt ->
                val xC = xOf(i)
                val top = yOf(pt.amount)
                val isSelected = i == selectedIdx

                // Glow for selected bar
                if (isSelected) {
                    drawRoundRect(
                        color = currentColor.copy(alpha = 0.15f),
                        topLeft = Offset(
                            xC - (if (comparisonPoints.isNotEmpty()) barWidth + 3f else barWidth / 2f) - 4f,
                            top - 4f
                        ),
                        size = Size(barWidth + 8f, cH + padT - top + 4f),
                        cornerRadius = barRadius
                    )
                }

                drawRoundRect(
                    brush = Brush.verticalGradient(
                        0f to currentColor.copy(alpha = if (isSelected) 1f else 0.85f),
                        1f to currentColor.copy(alpha = if (isSelected) 0.8f else 0.55f),
                        startY = top, endY = padT + cH
                    ),
                    topLeft = Offset(
                        xC - if (comparisonPoints.isNotEmpty()) barWidth + 3f else barWidth / 2f,
                        top
                    ),
                    size = Size(barWidth, padT + cH - top),
                    cornerRadius = barRadius
                )
            }

            comparisonPoints.forEachIndexed { i, pt ->
                xOf(i)
                val top = yOf(pt.amount)
                val isSelected = i == selectedIdx
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        0f to comparisonColor.copy(alpha = if (isSelected) 0.95f else 0.72f),
                        1f to comparisonColor.copy(alpha = 0.40f),
                        startY = top, endY = padT + cH
                    ),
                    topLeft = Offset(xOf(i) + 3f, top),
                    size = Size(barWidth, padT + cH - top),
                    cornerRadius = barRadius
                )
            }

            // Average lines
            fun drawAvgLine(avg: Double, color: Color, labelX: Float, alignRight: Boolean) {
                if (avg <= 0.0) return
                val y = yOf(avg)
                drawLine(
                    color.copy(alpha = 0.75f),
                    Offset(pL, y), Offset(w - 12f, y),
                    strokeWidth = 1.2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 7f))
                )
                drawContext.canvas.nativeCanvas.drawText(
                    "Avg: $currencySymbol${formatAmountForDisplay(avg, currencyFormat)}",
                    labelX, y - 6f,
                    android.graphics.Paint().apply {
                        this.color = color.copy(alpha = 0.9f).toArgb()
                        textSize = 20f; isAntiAlias = true
                        textAlign = if (alignRight) android.graphics.Paint.Align.RIGHT
                        else android.graphics.Paint.Align.LEFT
                    }
                )
            }
            drawAvgLine(currentAvg, currentColor, pL + 4f, false)
            if (comparisonPoints.isNotEmpty()) {
                drawAvgLine(comparisonAvg, comparisonColor, w - 14f, true)
            }

            // X labels
            for (i in points.indices step labelStep) {
                drawContext.canvas.nativeCanvas.drawText(
                    points[i].label, xOf(i) - 10f, h - 4f,
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.argb(140, 128, 128, 128)
                        textSize = 18f; isAntiAlias = true
                    }
                )
            }
        }
    }
}

private fun formatAxisVal(v: Double, currencyFormat: String): String = when {
    currencyFormat == "none" -> formatAmountForDisplay(v, currencyFormat, decimalFmt = "none")
    currencyFormat == "lakhs" && v >= 1_00_000 -> "${"%.1f".format(v / 1_00_000)}L"
    currencyFormat != "lakhs" && v >= 1_000_000 -> "${"%.1f".format(v / 1_000_000)}M"
    v >= 1_000 -> "${"%.0f".format(v / 1_000)}K"
    else -> formatAmountForDisplay(v, currencyFormat, decimalFmt = "none")
}

@Composable
private fun MonthCompareDialog(
    initial: YearMonth,
    onDismiss: () -> Unit,
    onConfirm: (YearMonth) -> Unit
) {
    var year by remember(initial) { mutableIntStateOf(initial.year) }
    var month by remember(initial) { mutableIntStateOf(initial.monthValue) }
    val monthNames = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Compare Month") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { year -= 1 }) { Icon(Icons.Default.ChevronLeft, null) }
                    Text(
                        "$year",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { year += 1 }) { Icon(Icons.Default.ChevronRight, null) }
                }
                for (row in 0 until 3) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (col in 0 until 4) {
                            val monthIndex = row * 4 + col + 1
                            val selected = month == monthIndex
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceContainerHigh
                                    )
                                    .clickable { month = monthIndex }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    monthNames[monthIndex - 1],
                                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(YearMonth.of(year, month)) }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun YearCompareDialog(
    initialYear: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var year by remember(initialYear) { mutableIntStateOf(initialYear) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Compare Year") },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { year -= 1 }) { Icon(Icons.Default.ChevronLeft, null) }
                Text(
                    "$year",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { year += 1 }) { Icon(Icons.Default.ChevronRight, null) }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(year) }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// ─── Donut Chart (Canvas) ─────────────────────────────────────────────────────

@Composable
private fun DonutChart(breakdown: List<CategorySpend>, modifier: Modifier = Modifier) {
    val animProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(900, easing = EaseOutCubic),
        label = "donut_anim"
    )
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = minOf(cx, cy) - 12f   // extra inset so labels don't clip
        val stroke = radius * 0.36f

        var startAngle = -90f
        breakdown.forEach { cs ->
            val sweep = cs.percentage / 100f * 360f * animProgress
            val color = try {
                Color(cs.colorHex.toColorInt())
            } catch (e: Exception) {
                Color(0xFF6750A4)
            }

            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = Offset(cx - radius, cy - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = stroke, cap = StrokeCap.Butt)
            )

            // Percentage label on the arc midpoint — only for segments >= 5%
            if (cs.percentage >= 5f) {
                val midAngle = Math.toRadians((startAngle + sweep / 2.0))
                val lx = cx + radius * cos(midAngle).toFloat()
                val ly = cy + radius * sin(midAngle).toFloat()
                drawContext.canvas.nativeCanvas.drawText(
                    "${"%.1f".format(cs.percentage)}%",
                    lx - 24f, ly + 7f,
                    android.graphics.Paint().apply {
                        this.color = android.graphics.Color.WHITE
                        textSize = 24f
                        isFakeBoldText = true
                        isAntiAlias = true
                        setShadowLayer(3f, 0f, 0f, android.graphics.Color.BLACK)
                    }
                )
            }
            startAngle += sweep
        }
    }
}

// ─── Category Row ─────────────────────────────────────────────────────────────

@Composable
private fun CategoryRow(cs: CategorySpend, sym: String, viewType: TransactionType) {
    val catColor = try {
        Color(cs.colorHex.toColorInt())
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CategoryIconBubble(
                iconKey = cs.category.icon.ifEmpty { "category" },
                colorHex = cs.colorHex,
                size = 40
            )
            Spacer(Modifier.width(12.dp))
            // Name + percentage contribution to the left
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    cs.category.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${"%.1f".format(cs.percentage)}% of total",
                    style = MaterialTheme.typography.labelSmall,
                    color = catColor.copy(alpha = 0.85f),
                    fontWeight = FontWeight.Medium
                )
            }
            // Amount + change on the right
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "$sym${fmtAmt(cs.amount)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                if (cs.changePercent != null) {
                    val isNeutral = abs(cs.changePercent) < 0.05f
                    val isIncrease = cs.changePercent > 0f
                    val deltaColor = when {
                        isNeutral -> MaterialTheme.colorScheme.onSurfaceVariant
                        viewType == TransactionType.EXPENSE ->
                            if (isIncrease) ExpenseRed else IncomeGreen

                        viewType == TransactionType.INCOME ->
                            if (isIncrease) IncomeGreen else ExpenseRed

                        else -> MaterialTheme.colorScheme.primary
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!isNeutral) {
                            Icon(
                                imageVector = if (isIncrease) Icons.Default.KeyboardArrowUp
                                else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = deltaColor,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            "vs prev: ${"%.1f".format(abs(cs.changePercent))}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = deltaColor
                        )
                    }
                }
            }
        }

        // Category percentage bar
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(catColor.copy(alpha = 0.12f))
        ) {
            val animPct by animateFloatAsState(
                targetValue = (cs.percentage / 100f).coerceIn(0f, 1f),
                animationSpec = tween(700, easing = EaseOutCubic),
                label = "cat_bar_${cs.category.id}"
            )
            Box(
                Modifier
                    .fillMaxWidth(animPct)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(catColor.copy(alpha = 0.9f), catColor.copy(alpha = 0.6f))
                        )
                    )
            )
        }
    }
}

// ─── Custom Date Range Dialog ─────────────────────────────────────────────────

@Composable
private fun CustomRangeDialog(
    onDismiss: () -> Unit,
    onConfirm: (LocalDate, LocalDate) -> Unit
) {
    var startText by remember { mutableStateOf("") }
    var endText by remember { mutableStateOf("") }
    val fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom Date Range") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Format: YYYY-MM-DD",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = startText, onValueChange = { startText = it },
                    label = { Text("Start Date") },
                    placeholder = { Text("e.g. 2026-01-01") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = endText, onValueChange = { endText = it },
                    label = { Text("End Date") },
                    placeholder = { Text("e.g. 2026-03-31") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    try {
                        val s = LocalDate.parse(startText.trim(), fmt)
                        val e = LocalDate.parse(endText.trim(), fmt)
                        if (!s.isAfter(e)) onConfirm(s, e)
                    } catch (ex: Exception) {
                    }
                },
                enabled = startText.isNotBlank() && endText.isNotBlank()
            ) { Text("Apply") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}