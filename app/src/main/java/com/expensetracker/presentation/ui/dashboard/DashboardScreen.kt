package com.expensetracker.presentation.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.expensetracker.domain.model.BudgetProgress
import com.expensetracker.domain.model.BudgetPeriod
import com.expensetracker.domain.model.MonthlySummary
import com.expensetracker.domain.model.Transaction
import com.expensetracker.domain.model.TransactionType
import com.expensetracker.presentation.components.AppBottomBar
import com.expensetracker.presentation.components.CategoryIconBubble
import com.expensetracker.presentation.components.EmptyState
import com.expensetracker.presentation.components.LoadingOverlay
import com.expensetracker.presentation.components.LocalCurrencyFormat
import com.expensetracker.presentation.components.LocalCurrencySymbol
import com.expensetracker.presentation.theme.CardGradientEnd
import com.expensetracker.presentation.theme.CardGradientStart
import com.expensetracker.presentation.theme.ExpenseRed
import com.expensetracker.presentation.theme.IncomeGreen
import com.expensetracker.presentation.theme.TransferBlue
import com.expensetracker.util.FormatUtils.formatAmountForDisplay
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.min

// ─── Amount formatting helper ─────────────────────────────────────────────────
/** Formats an amount without trailing .00 — e.g. 10.0 → "10", 10.5 → "10.50" */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToAddTransaction: () -> Unit,
    onNavigateToAddScheduledTransaction: () -> Unit = onNavigateToAddTransaction,
    onNavigateToTransactions: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToAccounts: () -> Unit,
    onNavigateToBudget: () -> Unit,
    onNavigateToSetBudget: () -> Unit = onNavigateToBudget,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToAnalysis: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currencySymbol = LocalCurrencySymbol.current
    val currencyFormat = LocalCurrencyFormat.current
    var showMonthlyBudget by remember { mutableStateOf(true) }

    LaunchedEffect(uiState.monthlyBudgetProgress, uiState.annualBudgetProgress) {
        if (showMonthlyBudget && uiState.monthlyBudgetProgress == null && uiState.annualBudgetProgress != null) {
            showMonthlyBudget = false
        } else if (!showMonthlyBudget && uiState.annualBudgetProgress == null && uiState.monthlyBudgetProgress != null) {
            showMonthlyBudget = true
        }
    }
    val selectedBudgetProgress = if (showMonthlyBudget) {
        uiState.monthlyBudgetProgress
    } else {
        uiState.annualBudgetProgress
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            AppBottomBar(
                currentRoute = "dashboard",
                onHome = {},
                onAnalysis = onNavigateToAnalysis,
                onAccounts = onNavigateToAccounts,
                onSettings = onNavigateToSettings,
                onAddTransaction = onNavigateToAddTransaction
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // ── Header ────────────────────────────────────────────────────────
            item {
                DashboardHeader(
                    userName = uiState.userName,
                    onSearch = onNavigateToTransactions,
                    onAvatar = { onNavigateToSettings() }
                )
            }

            // ── Cash Flow Card ────────────────────────────────────────────────
            item {
                CashFlowCard(
                    summary = uiState.summary,
                    selectedPeriod = uiState.selectedPeriod,
                    currencySymbol = currencySymbol,
                    currencyFormat = currencyFormat,
                    onPeriodChange = viewModel::selectPeriod,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // ── Quick Actions ─────────────────────────────────────────────────
            item {
                Spacer(Modifier.height(20.dp))
                QuickActionsGrid(
                    onTransactions = onNavigateToTransactions,
                    onCategories = onNavigateToCategories,
                    onAccounts = onNavigateToAccounts,
                    onBudget = onNavigateToBudget
                )
            }

            // ── Recent Transactions ───────────────────────────────────────────
            item {
                Spacer(Modifier.height(6.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Recent Transactions",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(
                        onClick = onNavigateToTransactions,
                        contentPadding = PaddingValues(top = 0.dp, end = 0.dp),
                        modifier = Modifier
                            .background(color = MaterialTheme.colorScheme.surfaceVariant)
                            .clip(CircleShape)
                    ) {
                        Text(
                            "See all",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            Icons.Default.ChevronRight, null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // ── Transactions list ─────────────────────────────────────────────
            if (uiState.recentTransactions.isEmpty()) {
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyState(
                            icon = Icons.AutoMirrored.Filled.ReceiptLong,
                            title = "No transactions yet",
                            subtitle = "Tap + to add your first transaction"
                        )
                    }
                }
            } else {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        ),
                        elevation = CardDefaults.cardElevation(2.dp),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Column {
                            uiState.recentTransactions.forEachIndexed { index, txn ->
                                DashboardTransactionRow(
                                    txn = txn,
                                    currencySymbol = currencySymbol,
                                    currencyFormat = currencyFormat
                                )
                                if (index < uiState.recentTransactions.size - 1) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant
                                            .copy(alpha = 0.5f),
                                        thickness = 0.5.dp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Budgets section ───────────────────────────────────────────────
            item {
                Spacer(Modifier.height(18.dp))
                Text(
                    "Budgets",
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(12.dp))
                DashboardBudgetSection(
                    budgetProgress = selectedBudgetProgress,
                    showMonthly = showMonthlyBudget,
                    currencySymbol = currencySymbol,
                    currencyFormat = currencyFormat,
                    onShowMonthly = { showMonthlyBudget = true },
                    onShowYearly = { showMonthlyBudget = false },
                    onSetBudget = onNavigateToSetBudget,
                    onTapBudget = onNavigateToBudget
                )
            }

            item {
                Spacer(Modifier.height(18.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Scheduled Transaction",
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .clickable { onNavigateToAddScheduledTransaction() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "+",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                ScheduledTransactionsCard(
                    upcomingTransactions = uiState.upcomingScheduledTransactions,
                    currencySymbol = currencySymbol,
                    currencyFormat = currencyFormat,
                    onAddScheduled = onNavigateToAddScheduledTransaction
                )
            }
        }

        if (uiState.isLoading) LoadingOverlay(true)
    }
}

// ─── Budget Section (empty state or progress card) ────────────────────────────

@Composable
private fun DashboardBudgetSection(
    budgetProgress: BudgetProgress?,
    showMonthly: Boolean,
    currencySymbol: String,
    currencyFormat: String,
    onShowMonthly: () -> Unit,
    onShowYearly: () -> Unit,
    onSetBudget: () -> Unit,
    onTapBudget: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
    ) {
        if (budgetProgress == null) {
            // ── No budget yet — empty state matching image 1 ──────────────────

            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // Monthly / Annual toggle (decorative — just shows state)
                BudgetPeriodToggle(
                    isMonthly = showMonthly,
                    onMonthly = onShowMonthly,
                    onYearly = onShowYearly
                )

                Spacer(Modifier.height(28.dp))

                // Money bag icon in a circle
                Box(
                    Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.MonetizationOn, null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    "No Budget Yet?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    if (showMonthly) {
                        "Set a monthly budget to achieve your\nsaving goals."
                    } else {
                        "Set an annual budget to plan your\nyearly spending."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = onSetBudget,
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.height(44.dp)
                ) {
                    Text(
                        "Set Budget",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            // ── Budget exists — progress card ─────────────────────────────────
            BudgetProgressInline(
                bp = budgetProgress,
                showMonthly = showMonthly,
                currencySymbol = currencySymbol,
                currencyFormat = currencyFormat,
                onSelectMonthly = onShowMonthly,
                onSelectYearly = onShowYearly,
                onEdit = onSetBudget,
                onTap = onTapBudget
            )
        }
    }
}

@Composable
private fun BudgetPeriodPill(
    selectedMonthly: Boolean,
    onMonthly: () -> Unit,
    onYearly: () -> Unit
) {
    Box(
        Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(Modifier.padding(4.dp)) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (selectedMonthly) MaterialTheme.colorScheme.onSurface
                        else Color.Transparent
                    )
                    .clickable { onMonthly() }
                    .padding(horizontal = 24.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Monthly",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (selectedMonthly) MaterialTheme.colorScheme.surface
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box(
                Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (!selectedMonthly) MaterialTheme.colorScheme.onSurface
                        else Color.Transparent
                    )
                    .clickable { onYearly() }
                    .padding(horizontal = 24.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Annual",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (!selectedMonthly) MaterialTheme.colorScheme.surface
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PeriodPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent
            )
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.surface
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BudgetPeriodToggle(
    isMonthly: Boolean,
    onMonthly: () -> Unit,
    onYearly: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        Modifier
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(Modifier.padding(4.dp)) {
            PeriodPill(
                label = "Monthly",
                selected = isMonthly,
                onClick = onMonthly,
                modifier = Modifier.weight(1f)
            )
            PeriodPill(
                label = "Yearly",
                selected = !isMonthly,
                onClick = onYearly,
                modifier = Modifier.weight(1f)
            )
        }
    }
}


@Composable
private fun BudgetProgressInline(
    bp: BudgetProgress,
    showMonthly: Boolean,
    currencySymbol: String,
    currencyFormat: String,
    onSelectMonthly: () -> Unit,
    onSelectYearly: () -> Unit,
    onEdit: () -> Unit,
    onTap: () -> Unit
) {
    val pct = bp.percentage.coerceIn(0f, 100f)
    val progress = (pct / 100f).coerceIn(0f, 1f)
    val spent = bp.spent
    val limit = bp.budget.totalLimit
    val delta = limit - spent
    val remaining = abs(delta)
    val isExceeded = delta < 0
    val barColor = when {
        pct >= 90f -> Color(0xFFFFC107)
        pct >= 70f -> Color(0xFFFFC107)
        else -> IncomeGreen
    }
    val cardTitle = if (bp.budget.period == BudgetPeriod.MONTHLY && bp.budget.month != null) {
        YearMonth.of(bp.budget.year, bp.budget.month).month.name.lowercase()
            .replaceFirstChar { it.uppercase() } + " ${bp.budget.year}"
    } else {
        bp.budget.year.toString()
    }
    val spendHint = buildDashboardBudgetHint(bp, currencySymbol, currencyFormat)
    Column(
        Modifier
            .fillMaxWidth()
            .clickable { onTap() }
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Monthly / Annual toggle (decorative — just shows state)
        BudgetPeriodToggle(
            isMonthly = showMonthly,
            onMonthly = onSelectMonthly,
            onYearly = onSelectYearly
        )

        Spacer(Modifier.height(16.dp))

        Text(
            cardTitle,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth()
        )

        Box(
            Modifier
                .fillMaxWidth()
                .height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
            val sweepDeg = 180f * progress
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                val strokeWidth = 20.dp.toPx()
                val diameter = min(size.width, size.height * 2f) - strokeWidth
                val topLeft = Offset(
                    x = (size.width - diameter) / 2f,
                    y = size.height - diameter / 2f - strokeWidth / 2f
                )
                val arcSize = Size(diameter, diameter)
                drawArc(
                    color = trackColor,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                if (sweepDeg > 0f) {
                    drawArc(
                        color = barColor,
                        startAngle = 180f,
                        sweepAngle = sweepDeg,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 54.dp)
            ) {
                Text(
                    text = if (isExceeded) "EXCEEDED" else "REMAINING",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                AutoResizingAmountText(
                    text = "$currencySymbol${formatAmountForDisplay(remaining, currencyFormat)}",
                    style = MaterialTheme.typography.headlineLarge,
                    color = if (isExceeded) ExpenseRed else Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    maxFontSize = MaterialTheme.typography.headlineLarge.fontSize,
                    minFontSize = 22.sp
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Text(
                    "Spent",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "$currencySymbol${formatAmountForDisplay(spent, currencyFormat)}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "Limit",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit budget",
                        modifier = Modifier
                            .size(14.dp)
                            .clickable { onEdit() },
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    "$currencySymbol${formatAmountForDisplay(limit, currencyFormat)}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
        Spacer(Modifier.height(14.dp))

        Text(
            spendHint,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ScheduledTransactionsCard(
    upcomingTransactions: List<UpcomingScheduledTransaction>,
    currencySymbol: String,
    currencyFormat: String,
    onAddScheduled: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clickable { onAddScheduled() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (upcomingTransactions.isEmpty()) 24.dp else 12.dp, vertical = if (upcomingTransactions.isEmpty()) 24.dp else 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (upcomingTransactions.isEmpty()) {
                Box(contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Event,
                            contentDescription = null,
                            modifier = Modifier.size(34.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Box(
                        modifier = Modifier
                            .padding(start = 40.dp, top = 36.dp)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4FC3F7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.White
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))

                Text(
                    "Ready to Plan Ahead?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    "Automate your finances with scheduled transactions. Tap '+' to set up your first one.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    upcomingTransactions.forEachIndexed { index, scheduled ->
                        UpcomingScheduledTransactionRow(
                            scheduled = scheduled,
                            currencySymbol = currencySymbol,
                            currencyFormat = currencyFormat
                        )
                        if (index < upcomingTransactions.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                thickness = 0.5.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UpcomingScheduledTransactionRow(
    scheduled: UpcomingScheduledTransaction,
    currencySymbol: String,
    currencyFormat: String
) {
    val amountColor = when (scheduled.type) {
        TransactionType.INCOME -> IncomeGreen
        TransactionType.EXPENSE -> ExpenseRed
        TransactionType.TRANSFER -> TransferBlue
    }
    val prefix = when (scheduled.type) {
        TransactionType.INCOME -> "+"
        TransactionType.EXPENSE -> "-"
        TransactionType.TRANSFER -> ""
    }
    val dateLabel = scheduled.nextRunAt.toLocalDate().let { date ->
        val today = LocalDate.now()
        when (date) {
            today -> "Today"
            today.plusDays(1) -> "Tomorrow"
            else -> scheduled.nextRunAt.format(DateTimeFormatter.ofPattern("dd MMM"))
        }
    }
    val timeLabel = scheduled.nextRunAt.format(DateTimeFormatter.ofPattern("hh:mm a"))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryIconBubble(
            iconKey = scheduled.categoryIcon,
            colorHex = scheduled.categoryColorHex,
            size = 44
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                scheduled.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                buildString {
                    append(dateLabel)
                    append(" • ")
                    append(timeLabel)
                    if (scheduled.paymentLabel.isNotBlank()) {
                        append(" • ")
                        append(scheduled.paymentLabel)
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                scheduled.frequencyLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            "$prefix$currencySymbol${formatAmountForDisplay(scheduled.amount, currencyFormat)}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = amountColor,
            maxLines = 1
        )
    }
}

private fun buildDashboardBudgetHint(
    budgetProgress: BudgetProgress,
    currencySymbol: String,
    currencyFormat: String
): String {
    val spent = budgetProgress.spent
    val limit = budgetProgress.budget.totalLimit
    val delta = limit - spent
    if (delta < 0) {
        return "Budget exceeded by $currencySymbol${formatAmountForDisplay(abs(delta), currencyFormat)}."
    }

    return if (budgetProgress.budget.period == BudgetPeriod.MONTHLY && budgetProgress.budget.month != null) {
        val today = LocalDate.now()
        val budgetMonth = YearMonth.of(budgetProgress.budget.year, budgetProgress.budget.month)
        val daysRemaining = if (
            budgetMonth.year == today.year &&
            budgetMonth.monthValue == today.monthValue
        ) {
            (budgetMonth.lengthOfMonth() - today.dayOfMonth + 1).coerceAtLeast(1)
        } else {
            budgetMonth.lengthOfMonth()
        }
        val safePerDay = delta / daysRemaining
        "You may exceed the limit by month-end, reduce daily spend to $currencySymbol${
            formatAmountForDisplay(safePerDay, currencyFormat)
        }."
    } else {
        "Remaining budget: $currencySymbol${formatAmountForDisplay(delta, currencyFormat)}."
    }
}

// ─── Header ───────────────────────────────────────────────────────────────────

@Composable
private fun DashboardHeader(
    userName: String,
    onSearch: () -> Unit,
    onAvatar: () -> Unit
) {
    val firstName = userName.split(" ").firstOrNull()?.ifEmpty { "User" } ?: "User"
    val greeting = run {
        val h = LocalTime.now().hour
        when {
            h < 12 -> "Good Morning"; h < 17 -> "Good Afternoon"; else -> "Good Evening"
        }
    }
    // Animated initials background color cycling through brand palette
    val initials = userName.split(" ")
        .take(2).joinToString("") { it.firstOrNull()?.uppercase() ?: "" }
        .ifEmpty { "U" }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                greeting,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                firstName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        // Search button
        IconButton(
            onClick = onSearch,
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Icon(
                Icons.Default.Search, contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.width(8.dp))

        // Avatar circle
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .clickable { onAvatar() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                initials,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

// ─── Cash Flow Card ───────────────────────────────────────────────────────────

@Composable
private fun CashFlowCard(
    summary: MonthlySummary,
    selectedPeriod: SummaryPeriod,
    currencySymbol: String,
    currencyFormat: String,
    onPeriodChange: (SummaryPeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(listOf(CardGradientStart, CardGradientEnd))
            )
    ) {
        Column(Modifier.padding(20.dp)) {

            // Top row: label + period selector
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Dynamic label based on selected period
                val cardLabel = when (selectedPeriod) {
                    SummaryPeriod.THIS_MONTH -> {
                        val now = YearMonth.now()
                        now.month.name.lowercase()
                            .replaceFirstChar { it.uppercase() } + " " + now.year
                    }

                    SummaryPeriod.THIS_YEAR -> "Year " + LocalDate.now().year.toString()
                    SummaryPeriod.ALL_TIME -> "All Time"
                }
                Text(
                    cardLabel,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )

                // Period pill selector
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.18f))
                ) {
                    var expanded by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier
                            .clickable { expanded = true }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            selectedPeriod.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            Icons.Default.KeyboardArrowDown, null,
                            tint = Color.White, modifier = Modifier.size(16.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        SummaryPeriod.entries.forEach { period ->
                            DropdownMenuItem(
                                text = { Text(period.label) },
                                onClick = { onPeriodChange(period); expanded = false },
                                trailingIcon = if (period == selectedPeriod) {
                                    { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Spending & Income side by side
            Row(Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "SPENDING",
                        style = MaterialTheme.typography.labelSmall,
                        color = ExpenseRed.copy(alpha = 0.9f),
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    AutoResizingAmountText(
                        text = "$currencySymbol${
                            formatAmountForDisplay(
                                summary.totalExpense,
                                currencyFormat
                            )
                        }",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text(
                        "INCOME",
                        style = MaterialTheme.typography.labelSmall,
                        color = IncomeGreen.copy(alpha = 0.9f),
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    AutoResizingAmountText(
                        text = "$currencySymbol${
                            formatAmountForDisplay(
                                summary.totalIncome,
                                currencyFormat
                            )
                        }",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Net Balance divider row
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.25f))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Net Balance",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                    AutoResizingAmountText(
                        text = "${if (summary.netBalance >= 0) "+" else ""}$currencySymbol${
                            formatAmountForDisplay(
                                summary.netBalance,
                                currencyFormat
                            )
                        }",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (summary.netBalance >= 0) IncomeGreen else ExpenseRed,
                        modifier = Modifier
                            .weight(1f)
                            .wrapContentWidth(Alignment.End),
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
private fun AutoResizingAmountText(
    text: String,
    style: TextStyle,
    color: Color,
    fontWeight: FontWeight,
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
        color = color,
        fontWeight = fontWeight,
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

// ─── Quick Actions ────────────────────────────────────────────────────────────

@Composable
private fun QuickActionsGrid(
    onTransactions: () -> Unit,
    onCategories: () -> Unit,
    onAccounts: () -> Unit,
    onBudget: () -> Unit
) {
    val actions = listOf(
        QuickAction(
            Icons.AutoMirrored.Filled.ReceiptLong, "Transactions",
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer, onTransactions
        ),
        QuickAction(
            Icons.Default.Category, "Categories",
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer, onCategories
        ),
        QuickAction(
            Icons.Default.AccountBalance, "Accounts",
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer, onAccounts
        ),
        QuickAction(
            Icons.Default.PieChart, "Budget",
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer, onBudget
        )
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        actions.forEach { action ->
            QuickActionTile(action = action, modifier = Modifier.weight(1f))
        }
    }
}

private data class QuickAction(
    val icon: ImageVector,
    val label: String,
    val containerColor: Color,
    val contentColor: Color,
    val onClick: () -> Unit
)

@Composable
private fun QuickActionTile(action: QuickAction, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(action.containerColor)
            .clickable { action.onClick() }
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            action.icon, contentDescription = action.label,
            tint = action.contentColor,
            modifier = Modifier.size(24.dp)
        )
        Text(
            action.label,
            style = MaterialTheme.typography.labelSmall,
            color = action.contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ─── Transaction Row (inside the grouped card) ────────────────────────────────

@Composable
private fun DashboardTransactionRow(
    txn: Transaction,
    currencySymbol: String,
    currencyFormat: String
) {
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd")
    val title = txn.note.ifEmpty { txn.categoryName.ifEmpty { "Uncategorized" } }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Category icon bubble using the category's own color
        CategoryIconBubble(
            iconKey = txn.categoryIcon.ifEmpty { "category" },
            colorHex = txn.categoryColorHex.ifEmpty { "#6750A4" },
            size = 44
        )

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // Payment mode — strip any parenthetical balance info e.g. "(X available)"
            if (txn.paymentModeName.isNotEmpty()) {
                val modeDisplay = txn.paymentModeName
                    .replace(Regex("""\s*\([^)]*available\)"""), "").trim()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.CreditCard, null,
                        modifier = Modifier.size(11.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Text(
                        modeDisplay,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(Modifier.width(8.dp))

        // Right side: amount + date
        Column(horizontalAlignment = Alignment.End) {
            val amountColor = when (txn.type) {
                TransactionType.INCOME -> IncomeGreen
                TransactionType.EXPENSE -> ExpenseRed
                TransactionType.TRANSFER -> TransferBlue
            }
            val prefix = when (txn.type) {
                TransactionType.INCOME -> "+"
                TransactionType.EXPENSE -> "-"
                TransactionType.TRANSFER -> ""
            }
            Text(
                "$prefix$currencySymbol${formatAmountForDisplay(txn.amount, currencyFormat)}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = amountColor
            )
            val today = LocalDate.now()
            val txnDate = txn.dateTime.toLocalDate()
            val dateLabel = when (txnDate) {
                today -> "Today"
                today.minusDays(1) -> "Yesterday"
                else -> txn.dateTime.format(dateFormatter)
            }
            Text(
                dateLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
