package com.expensetracker.presentation.ui.dashboard

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.expensetracker.domain.model.BudgetProgress
import com.expensetracker.domain.model.Transaction
import com.expensetracker.domain.model.TransactionType
import com.expensetracker.presentation.components.CategoryIconBubble
import com.expensetracker.presentation.components.EmptyState
import com.expensetracker.presentation.components.LoadingOverlay
import com.expensetracker.presentation.theme.CardGradientEnd
import com.expensetracker.presentation.theme.CardGradientStart
import com.expensetracker.presentation.theme.ExpenseRed
import com.expensetracker.presentation.theme.IncomeGreen
import com.expensetracker.presentation.theme.TransferBlue
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)

// ─── Amount formatting helper ─────────────────────────────────────────────────
/** Formats an amount without trailing .00 — e.g. 10.0 → "10", 10.5 → "10.50" */
private fun fmtAmt(amount: Double): String {
    val long = amount.toLong()
    return if (amount == long.toDouble()) "%,d".format(long) else "%,.2f".format(amount)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToAddTransaction: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToAccounts: () -> Unit,
    onNavigateToBudget: () -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToAnalysis: () -> Unit = {},
    onLogout: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddTransaction,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(60.dp)
            ) {
                Icon(
                    Icons.Default.Add, contentDescription = "Add Transaction",
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        bottomBar = {
            DashboardBottomBar(
                onTransactions = onNavigateToTransactions,
                onAccounts = onNavigateToAccounts,
                onMore = { showMenu = true },
                onAnalysis = onNavigateToAnalysis
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
                    onAvatar = { showMenu = true }
                )
            }

            // ── Cash Flow Card ────────────────────────────────────────────────
            item {
                CashFlowCard(
                    summary = uiState.summary,
                    selectedPeriod = uiState.selectedPeriod,
                    onPeriodChange = viewModel::selectPeriod,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // ── Budget progress (if exists) ───────────────────────────────────
            uiState.budgetProgress?.let { bp ->
                item {
                    Spacer(Modifier.height(16.dp))
                    BudgetCard(bp = bp, onTap = onNavigateToBudget)
                }
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

            // ── Recent Transactions header ────────────────────────────────────
            item {
                Spacer(Modifier.height(24.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Recent Transactions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = onNavigateToTransactions) {
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
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(2.dp),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Column {
                            uiState.recentTransactions.forEachIndexed { index, txn ->
                                DashboardTransactionRow(txn = txn)
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
        }

        if (uiState.isLoading) LoadingOverlay(true)
    }

    // Overflow menu
    if (showMenu) {
        ModalBottomSheet(onDismissRequest = { showMenu = false }) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    "More",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                listOf(
                    Triple(Icons.Default.Settings, "Settings", onNavigateToSettings),
                    Triple(
                        Icons.AutoMirrored.Filled.Logout,
                        "Logout"
                    ) { viewModel.logout(); onLogout() }
                ).forEach { (icon, label, action) ->
                    ListItem(
                        headlineContent = { Text(label) },
                        leadingContent = { Icon(icon, null) },
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showMenu = false; action() }
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
        }
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
        val h = java.time.LocalTime.now().hour
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
                .background(MaterialTheme.colorScheme.surfaceVariant)
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
    summary: com.expensetracker.domain.model.MonthlySummary,
    selectedPeriod: SummaryPeriod,
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
                        val now = java.time.YearMonth.now()
                        now.month.name.lowercase()
                            .replaceFirstChar { it.uppercase() } + " " + now.year
                    }

                    SummaryPeriod.THIS_YEAR -> "Year " + java.time.LocalDate.now().year.toString()
                    SummaryPeriod.ALL_TIME -> "All Time"
                }
                Text(
                    cardLabel,
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White.copy(alpha = 0.85f),
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
                    Text(
                        "₹${fmtAmt(summary.totalExpense)}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold
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
                    Text(
                        "₹${fmtAmt(summary.totalIncome)}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold
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
                    Text(
                        "${if (summary.netBalance >= 0) "+" else ""}₹${fmtAmt(summary.netBalance)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (summary.netBalance >= 0) IncomeGreen else ExpenseRed
                    )
                }
            }
        }
    }
}

// ─── Budget Card ──────────────────────────────────────────────────────────────

@Composable
private fun BudgetCard(bp: BudgetProgress, onTap: () -> Unit) {
    val pct = (bp.percentage / 100f).coerceIn(0f, 1f)
    val barColor = when {
        bp.percentage >= 90f -> ExpenseRed
        bp.percentage >= 70f -> Color(0xFFFF9800)
        else -> IncomeGreen
    }
    Card(
        onClick = onTap,
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.PieChart, null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            "Monthly Budget",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "${bp.percentage.toInt()}% used",
                            style = MaterialTheme.typography.bodySmall,
                            color = barColor
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "₹${fmtAmt(bp.spent)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "of ₹${fmtAmt(bp.budget.totalLimit)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { pct },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = barColor,
                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )
        }
    }
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
            Icons.AutoMirrored.Filled.ReceiptLong,
            "Transactions",
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            onTransactions
        ),
        QuickAction(
            Icons.Default.Category, "Categories", MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer, onCategories
        ),
        QuickAction(
            Icons.Default.AccountBalance, "Accounts", MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer, onAccounts
        ),
        QuickAction(
            Icons.Default.PieChart, "Budget", MaterialTheme.colorScheme.errorContainer,
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
private fun DashboardTransactionRow(txn: Transaction) {
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
            // Payment mode — strip any parenthetical balance info e.g. "(₹X,XXX available)"
            if (txn.paymentModeName.isNotEmpty()) {
                val modeDisplay = txn.paymentModeName
                    .replace(Regex("""\s*\(₹[^)]+\)"""), "").trim()
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
                "$prefix₹${fmtAmt(txn.amount)}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = amountColor
            )
            Text(
                txn.dateTime.format(dateFormatter),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ─── Bottom Navigation Bar ────────────────────────────────────────────────────

@Composable
private fun DashboardBottomBar(
    onTransactions: () -> Unit,
    onAnalysis: () -> Unit,
    onAccounts: () -> Unit,
    onMore: () -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        NavigationBarItem(
            selected = true,
            onClick = {},
            icon = { Icon(Icons.Default.Home, null) },
            label = { Text("Home") }
        )
        NavigationBarItem(
            selected = false,
            onClick = onAnalysis,
            icon = { Icon(Icons.Default.BarChart, null) },
            label = { Text("Analysis") }
        )
        // Centre placeholder for FAB
        NavigationBarItem(
            selected = false,
            onClick = {},
            icon = { Spacer(Modifier.size(24.dp)) },
            label = { Text("") },
            enabled = false
        )
        NavigationBarItem(
            selected = false,
            onClick = onAccounts,
            icon = { Icon(Icons.Default.AccountBalance, null) },
            label = { Text("Accounts") }
        )
        NavigationBarItem(
            selected = false,
            onClick = onMore,
            icon = { Icon(Icons.Default.MoreHoriz, null) },
            label = { Text("More") }
        )
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────