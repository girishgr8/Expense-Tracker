package com.expensetracker.presentation.ui.views

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.expensetracker.domain.model.Transaction
import com.expensetracker.domain.model.TransactionType
import com.expensetracker.presentation.components.CategoryIconBubble
import com.expensetracker.presentation.components.LocalCurrencyFormat
import com.expensetracker.presentation.components.LocalCurrencySymbol
import com.expensetracker.presentation.theme.ExpenseRed
import com.expensetracker.presentation.theme.IncomeGreen
import com.expensetracker.presentation.theme.TransferBlue
import com.expensetracker.util.FormatUtils.smartFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DayViewScreen(
    initialDate: LocalDate,
    onNavigateBack: () -> Unit,
    onNavigateToAddTransaction: () -> Unit = {},
    onNavigateToEditTransaction: (Long) -> Unit = {},
    viewModel: DayViewModel = hiltViewModel()
) {
    LaunchedEffect(initialDate) { viewModel.loadDay(initialDate) }

    val uiState by viewModel.uiState.collectAsState()
    val currencySymbol = LocalCurrencySymbol.current
    val currencyFormat = LocalCurrencyFormat.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddTransaction,
                shape = CircleShape,
                containerColor = Color.White,
                contentColor = Color.Black,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.Default.Add, "Add transaction", Modifier.size(28.dp))
            }
        }
    ) { scaffoldPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding)
        ) {
            // ── Top bar ───────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, "Back",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    "Day View",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                // + button top-right (circles)
                IconButton(onClick = onNavigateToAddTransaction) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Add, "Add",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp,
                    top = 4.dp, bottom = 96.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ── Day navigation pill ───────────────────────────────────────
                item {
                    DayNavigationHeader(
                        date = uiState.date,
                        txnCount = uiState.transactions.size,
                        onPrev = viewModel::previousDay,
                        onNext = viewModel::nextDay
                    )
                }

                // ── Summary card ──────────────────────────────────────────────
                item {
                    DaySummaryCard(
                        sym = currencySymbol,
                        expense = uiState.totalExpense,
                        income = uiState.totalIncome
                    )
                }

                // ── Transactions list card ────────────────────────────────────
                if (uiState.transactions.isNotEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(vertical = 8.dp)) {
                                uiState.transactions.forEachIndexed { idx, txn ->
                                    DayTransactionRow(
                                        txn = txn,
                                        sym = currencySymbol,
                                        currencyFormat = currencyFormat,
                                        onClick = { onNavigateToEditTransaction(txn.id) }
                                    )
                                    if (idx < uiState.transactions.lastIndex) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 16.dp),
                                            thickness = 0.5.dp,
                                            color = MaterialTheme.colorScheme.outlineVariant
                                                .copy(alpha = 0.3f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else if (!uiState.isLoading) {
                    item {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No transactions on this day",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Day navigation pill (matching CalendarScreen's MonthHeader exactly) ──────

@Composable
private fun DayNavigationHeader(
    date: LocalDate,
    txnCount: Int,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    val fmt = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy", Locale.getDefault())
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 4.dp, vertical = 10.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onPrev) {
                Icon(
                    Icons.Default.ChevronLeft, "Previous",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    date.format(fmt),
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
                    Icons.Default.ChevronRight, "Next",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ─── Day summary card (matching TransactionScreen SummaryCard style) ──────────

@Composable
private fun DaySummaryCard(
    sym: String,
    expense: Double,
    income: Double
) {
    val balance = income - expense

    Card(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            ExpenseRed.copy(alpha = 0.25f),
                            IncomeGreen.copy(alpha = 0.15f)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                // SPENDING / INCOME labels + amounts
                Row(Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "SPENDING",
                            style = MaterialTheme.typography.labelSmall,
                            color = ExpenseRed,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "$sym${expense.smartFormat("default")}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Column(
                        Modifier.weight(1f),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            "INCOME",
                            style = MaterialTheme.typography.labelSmall,
                            color = IncomeGreen,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "$sym${income.smartFormat("default")}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Net Balance row
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.25f))
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
                        Text(
                            "${if (balance >= 0) "" else "-"}$sym${
                                kotlin.math.abs(balance).smartFormat("default")
                            }",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (balance >= 0) IncomeGreen else ExpenseRed
                        )
                    }
                }
            }
        }
    }
}

// ─── Single transaction row ────────────────────────────────────────────────────

@Composable
private fun DayTransactionRow(
    txn: Transaction,
    sym: String,
    currencyFormat: String,
    onClick: () -> Unit
) {
    val title = txn.note.ifEmpty { txn.categoryName.ifEmpty { "Uncategorized" } }
    val timeFmt = DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault())
    val timeStr = txn.dateTime.format(timeFmt)
    val amtColor = when (txn.type) {
        TransactionType.INCOME -> IncomeGreen
        TransactionType.EXPENSE -> ExpenseRed
        TransactionType.TRANSFER -> TransferBlue
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Category icon bubble (44dp to match screenshot)
        CategoryIconBubble(
            iconKey = txn.categoryIcon.ifEmpty { "category" },
            colorHex = txn.categoryColorHex.ifEmpty { "#6750A4" },
            size = 44
        )

        Spacer(Modifier.width(12.dp))

        // Title + payment mode with small icon
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 15.sp
            )
            Spacer(Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Small payment-mode icon matching the screenshot
                val modeIcon = paymentModeIcon(txn.paymentModeName)
                Icon(
                    imageVector = modeIcon,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Text(
                    text = txn.paymentModeName.ifEmpty { "—" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        // Amount + time right-aligned
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "$sym${kotlin.math.abs(txn.amount).smartFormat("default")}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = amtColor,
                fontSize = 15.sp
            )
            Text(
                text = timeStr,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

// ─── Payment mode icon heuristic (matches AccountsScreen + screenshot) ────────

private fun paymentModeIcon(modeName: String): ImageVector = when {
    modeName.contains("UPI", ignoreCase = true) ||
            modeName.contains("GPay", ignoreCase = true) ||
            modeName.contains("PhonePe", ignoreCase = true) ||
            modeName.contains("Paytm", ignoreCase = true) -> Icons.Default.PhoneAndroid

    modeName.contains("Cash", ignoreCase = true) -> Icons.Default.Payments
    modeName.contains("Credit", ignoreCase = true) -> Icons.Default.CreditCard
    modeName.contains("Debit", ignoreCase = true) -> Icons.Default.CreditCard
    modeName.contains("Net", ignoreCase = true) -> Icons.Default.Language
    modeName.contains("Cheque", ignoreCase = true) -> Icons.Default.EditNote
    else -> Icons.Default.AccountBalance
}