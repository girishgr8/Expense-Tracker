package com.expensetracker.presentation.ui.accounts

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.hilt.navigation.compose.hiltViewModel
import com.expensetracker.R
import com.expensetracker.domain.model.BalanceAdjustment
import com.expensetracker.domain.model.BankAccount
import com.expensetracker.domain.model.CreditCard
import com.expensetracker.domain.model.PaymentMode
import com.expensetracker.domain.model.PaymentModeType
import com.expensetracker.domain.model.Transaction
import com.expensetracker.domain.model.TransactionType
import com.expensetracker.presentation.components.AppBottomBar
import com.expensetracker.presentation.components.CategoryIconBubble
import com.expensetracker.presentation.components.EmptyState
import com.expensetracker.presentation.theme.ExpenseRed
import com.expensetracker.presentation.theme.IncomeGreen
import com.expensetracker.presentation.theme.TextWarning
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs

// ─── Screen ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)

// ─── Amount formatting helper ─────────────────────────────────────────────────
/** Formats an amount without trailing .00 — e.g. 10.0 → "10", 10.5 → "10.50" */
private fun fmtAmt(amount: Double): String {
    val long = amount.toLong()
    return if (amount == long.toDouble()) "%,d".format(long) else "%,.2f".format(amount)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToHome: () -> Unit = {},
    onNavigateToAnalysis: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToTransaction: (Long) -> Unit,
    viewModel: AccountsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val standaloneModes = remember(uiState.allModes) { viewModel.standaloneModes() }
    val sym = uiState.currencySymbol

    // Credit cards toggle: Available / Outstanding
    var cardShowAvailable by remember { mutableStateOf(true) }

    val showDetail = uiState.selectedDetailMode != null ||
            uiState.selectedDetailAccount != null ||
            uiState.selectedDetailCard != null

    Scaffold(
        bottomBar = {
            AppBottomBar(
                currentRoute = "accounts",
                onHome = onNavigateToHome,
                onAnalysis = onNavigateToAnalysis,
                onAccounts = {},
                onSettings = onNavigateToSettings,
                onAddTransaction = { onNavigateToTransaction(-1L) }
            )
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "All Accounts", fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                actions = {
                    // Add account button
                    OutlinedButton(
                        onClick = { viewModel.openAddSheet() },
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Add, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add account", style = MaterialTheme.typography.labelLarge)
                    }
                    Spacer(Modifier.width(8.dp))
                }
            )
        }
    ) { padding ->
        val isEmpty = uiState.accounts.isEmpty() &&
                standaloneModes.isEmpty() && uiState.creditCards.isEmpty()

        if (isEmpty) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding), contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    icon = Icons.Default.AccountBalance,
                    title = "No accounts yet",
                    subtitle = "Tap '+ Add account' to get started"
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp,
                    top = 12.dp, bottom = 32.dp
                ),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // ── Summary tiles ─────────────────────────────────────────────
                item(key = "summary") {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SummaryTile(
                            label = "Available Balance",
                            value = "$sym${fmtAmt(uiState.totalBankBalance)}",
                            modifier = Modifier.weight(1f)
                        )
                        SummaryTile(
                            label = "Available Credit",
                            value = "$sym${fmtAmt(uiState.totalCreditAvailable)}",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // ── Bank accounts ─────────────────────────────────────────────
                if (uiState.accounts.isNotEmpty()) {
                    item(key = "bank_header") {
                        AccountsSectionHeader(
                            icon = Icons.Default.AccountBalance,
                            title = "Bank Accounts"
                        )
                    }
                    item(key = "bank_group") {
                        GroupedListCard {
                            uiState.accounts.forEachIndexed { idx, account ->
                                AccountRow(
                                    name = account.name,
                                    value = "$sym${fmtAmt(account.balance)}",
                                    valueColor = MaterialTheme.colorScheme.onSurface,
                                    onClick = { viewModel.openAccountDetail(account) }
                                )
                                if (idx < uiState.accounts.size - 1) RowDivider()
                            }
                        }
                    }
                }

                // ── Credit cards ──────────────────────────────────────────────
                if (uiState.creditCards.isNotEmpty()) {
                    item(key = "cc_header") {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            AccountsSectionHeader(
                                icon = Icons.Default.CreditCard,
                                title = "Credit Cards"
                            )
                            // Available / Outstanding toggle
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(3.dp)
                            ) {
                                listOf(
                                    true to "Available",
                                    false to "Outstanding"
                                ).forEach { (avail, label) ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(
                                                if (cardShowAvailable == avail)
                                                    MaterialTheme.colorScheme.surface
                                                else Color.Transparent
                                            )
                                            .clickable { cardShowAvailable = avail }
                                            .padding(horizontal = 12.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            label,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (cardShowAvailable == avail)
                                                FontWeight.Bold else FontWeight.Normal,
                                            color = if (cardShowAvailable == avail)
                                                MaterialTheme.colorScheme.onSurface
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item(key = "cc_group") {
                        GroupedListCard {
                            uiState.creditCards.forEachIndexed { idx, card ->
                                val displayValue = if (cardShowAvailable)
                                    "$sym${fmtAmt(card.availableLimit)}"
                                else
                                    "$sym${fmtAmt(card.totalLimit - card.availableLimit)}"
                                AccountRow(
                                    name = card.name,
                                    value = displayValue,
                                    valueColor = if (!cardShowAvailable) ExpenseRed
                                    else MaterialTheme.colorScheme.onSurface,
                                    onClick = { viewModel.openCardDetail(card) }
                                )
                                if (idx < uiState.creditCards.size - 1) RowDivider()
                            }
                        }
                    }
                }

                // ── Cash / Other payment modes ────────────────────────────────
                if (standaloneModes.isNotEmpty()) {
                    standaloneModes.groupBy { it.type }.forEach { (type, modes) ->
                        val sectionIcon: ImageVector = when (type) {
                            PaymentModeType.CASH -> Icons.Default.Payments
                            PaymentModeType.WALLET -> Icons.Default.AccountBalanceWallet
                            else -> Icons.Default.Payment
                        }
                        item(key = "standalone_header_${type.name}") {
                            AccountsSectionHeader(icon = sectionIcon, title = type.displayName())
                        }
                        item(key = "standalone_group_${type.name}") {
                            GroupedListCard {
                                modes.forEachIndexed { idx, mode ->
                                    val balance = uiState.modeBalances[mode.id]
                                    val valueText = if (balance != null)
                                        "${if (balance < 0) "-" else ""}$sym${fmtAmt(abs(balance))}"
                                    else "—"
                                    AccountRow(
                                        name = mode.type.displayName() +
                                                if (mode.identifier.isNotEmpty()) " (${mode.identifier})" else "",
                                        value = valueText,
                                        valueColor = if ((balance ?: 0.0) < 0) ExpenseRed
                                        else MaterialTheme.colorScheme.onSurface,
                                        onClick = { viewModel.openModeDetail(mode) }
                                    )
                                    if (idx < modes.size - 1) RowDivider()
                                }
                            }
                        }
                    }
                }

                item(key = "bottom_spacer") { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    // ── Dialogs ────────────────────────────────────────────────────────────────

    // Unified Add Account bottom sheet
    if (uiState.showAddSheet) {
        AddAccountSheet(
            currencySymbol = uiState.currencySymbol,
            onDismiss = viewModel::closeAddSheet,
            onSaveBank = { name, balance, colorHex, accountIdentifier, modes ->
                viewModel.saveAccountWithModes(name, balance, colorHex, accountIdentifier, modes)
            },
            onSaveWallet = { identifier ->
                viewModel.saveMode(null, PaymentModeType.WALLET, identifier)
                viewModel.closeAddSheet()
            },
            onSaveCard = { name, avail, total, billing, due, color ->
                viewModel.saveCreditCard(name, avail, total, billing, due, color)
                viewModel.closeAddSheet()
            }
        )
    }

    if (uiState.showAccountDialog) {
        AddEditAccountDialog(
            editing = uiState.editingAccount,
            onDismiss = viewModel::hideAccountDialog,
            onSave = { name, bal, color -> viewModel.saveAccount(name, bal, color) }
        )
    }

    if (uiState.showModeDialog) {
        AddEditPaymentModeDialog(
            editing = uiState.editingMode,
            accounts = uiState.accounts,
            preselectedAccountId = uiState.preselectedAccountId,
            onDismiss = viewModel::hideModeDialog,
            onSave = { bankId, type, id -> viewModel.saveMode(bankId, type, id) }
        )
    }

    if (uiState.showCreditCardDialog) {
        AddEditCreditCardDialog(
            editing = uiState.editingCreditCard,
            onDismiss = viewModel::hideCreditCardDialog,
            onSave = { name, avail, total, billing, due, color ->
                viewModel.saveCreditCard(name, avail, total, billing, due, color)
            }
        )
    }

    // ── BackHandler: layered back navigation ─────────────────────────────────
    // Layer 1: card transactions screen — back closes it (returns to detail)
    BackHandler(enabled = uiState.showCardTransactions) {
        viewModel.closeCardTransactions()
    }
    // Layer 2: detail screen — back closes it (returns to accounts list)
    BackHandler(enabled = showDetail && !uiState.showCardTransactions) {
        viewModel.closeDetail()
    }

    // ── Full-screen detail overlay ────────────────────────────────────────────

    AnimatedVisibility(
        visible = showDetail,
        enter = slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) +
                fadeIn(animationSpec = tween(300)),
        exit = slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(250)) +
                fadeOut(animationSpec = tween(250))
    ) {
        val detailTitle = when {
            uiState.selectedDetailMode != null -> uiState.selectedDetailMode!!.type.displayName()
            uiState.selectedDetailAccount != null -> uiState.selectedDetailAccount!!.name
            uiState.selectedDetailCard != null -> uiState.selectedDetailCard!!.name
            else -> ""
        }
        DetailScreen(
            title = detailTitle,
            balance = uiState.detailBalance,
            transactions = uiState.detailTransactions,
            adjustments = uiState.detailAdjustments,
            currencySymbol = sym,
            isCard = uiState.selectedDetailCard != null,
            mode = uiState.selectedDetailMode,
            account = uiState.selectedDetailAccount,
            card = uiState.selectedDetailCard,
            linkedModes = uiState.detailLinkedModes,
            showEditSheet = uiState.showEditAccountSheet,
            showEditModeBalanceSheet = uiState.showEditModeBalanceSheet,
            showEditCardSheet = uiState.showEditCardSheet,
            showEditLimitSheet = uiState.showEditLimitSheet,
            showCardTransactions = uiState.showCardTransactions,
            cardTransactionsIsCurrentCycle = uiState.cardTransactionsIsCurrentCycle,
            onNavigateBack = viewModel::closeDetail,
            onEditAccount = viewModel::openEditAccountSheet,
            onEditModeBalance = viewModel::openEditModeBalanceSheet,
            onEditCard = viewModel::openEditCardSheet,
            onCloseEditSheet = viewModel::closeEditAccountSheet,
            onCloseEditModeBalanceSheet = viewModel::closeEditModeBalanceSheet,
            onCloseEditCardSheet = viewModel::closeEditCardSheet,
            onOpenEditLimitSheet = viewModel::openEditLimitSheet,
            onCloseEditLimitSheet = viewModel::closeEditLimitSheet,
            onSaveCardAvailableLimit = { card, limit ->
                viewModel.saveCardAvailableLimit(card, limit)
            },
            onSaveEditCard = { name, avail, total, billing, due, color ->
                uiState.selectedDetailCard?.let { card ->
                    viewModel.saveEditedCard(card, name, avail, total, billing, due, color)
                    viewModel.closeEditCardSheet()
                }
            },
            onSaveEditAccount = { acc, name, bal, modes ->
                viewModel.saveEditedAccount(acc, name, bal, modes)
            },
            onSaveEditModeBalance = { mode, balance ->
                viewModel.saveEditedModeBalance(mode, balance)
            },
            onDeleteAccount = { acc ->
                viewModel.deleteAccount(acc)
                viewModel.closeDetail()
            },
            onDeleteCard = { card ->
                viewModel.deleteCreditCard(card)
                viewModel.closeDetail()
            },
            onDeleteMode = viewModel::deleteModeFromDetail,
            onOpenCardTransactions = { viewModel.openCardTransactions(isCurrentCycle = false) },
            onOpenCurrentSpends = { viewModel.openCardTransactions(isCurrentCycle = true) },
            onCloseCardTransactions = viewModel::closeCardTransactions,
            onNavigateToTransaction = onNavigateToTransaction
        )
    }
}

// ─── Reusable layout helpers ──────────────────────────────────────────────────

/** Two-column summary tile at the top of the screen */
@Composable
private fun SummaryTile(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = modifier
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            AutoSizeText(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                maxFontSize = 22.sp,
                minFontSize = 11.sp
            )
        }
    }
}

/**
 * Text that automatically shrinks its font size step-by-step to fit on one line.
 * Starts at [maxFontSize] and reduces by 1.sp per recompose until the text fits,
 * down to [minFontSize]. Uses drawWithContent to hide intermediate frames.
 */
@Composable
private fun AutoSizeText(
    text: String,
    style: androidx.compose.ui.text.TextStyle,
    fontWeight: FontWeight,
    maxFontSize: androidx.compose.ui.unit.TextUnit,
    minFontSize: androidx.compose.ui.unit.TextUnit,
    modifier: Modifier = Modifier
) {
    var fontSize by remember(text) { mutableStateOf(maxFontSize) }
    var readyToDraw by remember(text) { mutableStateOf(false) }

    Text(
        text = text,
        style = style.copy(fontSize = fontSize),
        fontWeight = fontWeight,
        maxLines = 1,
        softWrap = false,
        modifier = modifier.drawWithContent {
            if (readyToDraw) drawContent()
        },
        onTextLayout = { result ->
            if (result.didOverflowWidth) {
                val next = (fontSize.value - 1f).sp
                if (next.value >= minFontSize.value) {
                    fontSize = next          // shrink and recompose
                } else {
                    readyToDraw = true       // at minimum size, draw anyway
                }
            } else {
                readyToDraw = true
            }
        }
    )
}

/** Section header with a leading icon — e.g. 🏦 Bank Accounts */
@Composable
private fun AccountsSectionHeader(icon: ImageVector, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            icon, null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

/** Single rounded card that groups multiple rows — matches the image's dark grouped list style */
@Composable
private fun GroupedListCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.fillMaxWidth(), content = content)
    }
}

/** One row inside a GroupedListCard: name on left, value + chevron on right */
@Composable
private fun AccountRow(
    name: String,
    value: String,
    valueColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
        Spacer(Modifier.width(6.dp))
        Icon(
            Icons.Default.ChevronRight, null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
        thickness = 0.5.dp
    )
}

// ─── Detail full-screen (shared for bank / card / cash) ───────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailScreen(
    title: String,
    balance: Double,
    transactions: List<Transaction>,
    adjustments: List<BalanceAdjustment>,
    currencySymbol: String,
    isCard: Boolean,
    mode: PaymentMode?,
    account: BankAccount?,
    card: CreditCard?,
    linkedModes: List<PaymentMode>,
    showEditSheet: Boolean,
    showEditModeBalanceSheet: Boolean,
    showEditCardSheet: Boolean,
    showEditLimitSheet: Boolean,
    showCardTransactions: Boolean,
    cardTransactionsIsCurrentCycle: Boolean,
    onNavigateBack: () -> Unit,
    onEditAccount: () -> Unit,
    onEditModeBalance: () -> Unit,
    onEditCard: () -> Unit,
    onCloseEditSheet: () -> Unit,
    onCloseEditModeBalanceSheet: () -> Unit,
    onCloseEditCardSheet: () -> Unit,
    onOpenEditLimitSheet: () -> Unit,
    onCloseEditLimitSheet: () -> Unit,
    onSaveCardAvailableLimit: (CreditCard, Double) -> Unit,
    onSaveEditCard: (String, Double, Double, Int, Int, String) -> Unit,
    onSaveEditAccount: (BankAccount, String, Double, List<PaymentMode>) -> Unit,
    onSaveEditModeBalance: (PaymentMode, Double) -> Unit,
    onDeleteAccount: (BankAccount) -> Unit,
    onDeleteCard: (CreditCard) -> Unit,
    onDeleteMode: (PaymentMode) -> Unit,
    onOpenCardTransactions: () -> Unit,
    onOpenCurrentSpends: () -> Unit,
    onCloseCardTransactions: () -> Unit,
    onNavigateToTransaction: (Long) -> Unit
) {
    // Intercept back when card transactions screen is open
    BackHandler(enabled = showCardTransactions) { onCloseCardTransactions() }

    var selectedTab by remember(account?.id, card?.id, mode?.id, title) { mutableIntStateOf(0) }
    val supportsAdjustments = account != null || card != null || mode != null
    val tabs = if (supportsAdjustments) {
        listOf("All", "Credit", "Debit", "Adjustments")
    } else {
        listOf("All", "Credit", "Debit")
    }

    val filteredTxns = when (selectedTab) {
        1 -> transactions.filter { it.type == TransactionType.INCOME }
        2 -> transactions.filter { it.type == TransactionType.EXPENSE }
        else -> transactions
    }

    val dateFormatter = DateTimeFormatter.ofPattern("dd MMM")
    val swipeThresholdPx = 72f

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        title, fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (account != null || card != null || mode != null) {
                        // Edit — opens card edit sheet for cards, account edit sheet for bank accounts
                        IconButton(
                            onClick = when {
                                card != null -> onEditCard
                                account != null -> onEditAccount
                                else -> onEditModeBalance
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainer)
                                .size(40.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit, contentDescription = "Edit",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        // + → navigate to Add Transaction
                        IconButton(
                            onClick = { onNavigateToTransaction(-1L) },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainer)
                                .size(40.dp)
                        ) {
                            Icon(
                                Icons.Default.Add, contentDescription = "Add Transaction",
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            if (card != null) {
                // ── Credit card summary card ──────────────────────────────────
                CreditCardDetailCard(
                    card = card,
                    transactions = transactions,
                    currencySymbol = currencySymbol,
                    onSeeTransactions = onOpenCardTransactions,
                    onSeeCurrentSpends = onOpenCurrentSpends,
                    onEditLimit = onOpenEditLimitSheet
                )
            } else {
                // ── Bank account / mode balance card ──────────────────────────
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            if (isCard) "Available Limit" else "Available Balance",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${if (balance < 0) "-" else ""}$currencySymbol${fmtAmt(abs(balance))}",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (balance >= 0) MaterialTheme.colorScheme.onSurface
                            else ExpenseRed
                        )
                        if (account != null) {
                            Spacer(Modifier.height(4.dp))
                            Row {
                                Text(
                                    "Incorrect balance? ",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "Edit",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.clickable { onEditAccount() })
                            }
                            Spacer(Modifier.height(12.dp))
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${linkedModes.size} Linked payment mode${if (linkedModes.size != 1) "s" else ""}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                OutlinedButton(
                                    onClick = onEditAccount,
                                    shape = RoundedCornerShape(20.dp),
                                    contentPadding = PaddingValues(
                                        horizontal = 14.dp,
                                        vertical = 6.dp
                                    )
                                ) {
                                    Icon(Icons.Default.Add, null, Modifier.size(14.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Link", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        } else if (mode != null) {
                            Spacer(Modifier.height(4.dp))
                            Row {
                                Text(
                                    "Incorrect balance? ",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "Edit",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.clickable { onEditModeBalance() })
                            }
                        } else if (balance < 0) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Incorrect balance? Edit",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            Column(
                modifier = Modifier.pointerInput(tabs.size, selectedTab) {
                    var totalHorizontalDrag = 0f
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, dragAmount ->
                            totalHorizontalDrag += dragAmount
                        },
                        onDragEnd = {
                            when {
                                totalHorizontalDrag > swipeThresholdPx && selectedTab > 0 -> {
                                    selectedTab -= 1
                                }

                                totalHorizontalDrag < -swipeThresholdPx && selectedTab < tabs.lastIndex -> {
                                    selectedTab += 1
                                }
                            }
                            totalHorizontalDrag = 0f
                        },
                        onDragCancel = {
                            totalHorizontalDrag = 0f
                        }
                    )
                }
            ) {
                // ── Tabs ─────────────────────────────────────────────────────
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    edgePadding = 2.dp
                ) {
                    tabs.forEachIndexed { idx, label ->
                        Tab(
                            selected = selectedTab == idx,
                            onClick = { selectedTab = idx },
                            text = {
                                Text(
                                    text = label,
                                    fontWeight = if (selectedTab == idx) {
                                        FontWeight.Bold
                                    } else {
                                        FontWeight.Normal
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                            }
                        )
                    }
                }

                if (supportsAdjustments && selectedTab == 3) {
                    if (adjustments.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                "No adjustments",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            items(adjustments, key = { it.id }) { adjustment ->
                                BalanceAdjustmentRow(adjustment, dateFormatter, currencySymbol)
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                                    thickness = 0.5.dp
                                )
                            }
                            item { Spacer(Modifier.height(32.dp)) }
                        }
                    }
                } else if (filteredTxns.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "No transactions",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        items(filteredTxns, key = { it.id }) { txn ->
                            DetailTransactionRow(
                                txn = txn,
                                dateFormatter = dateFormatter,
                                currencySymbol = currencySymbol,
                                onClick = { onNavigateToTransaction(txn.id) }
                            )
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                                thickness = 0.5.dp
                            )
                        }
                        item { Spacer(Modifier.height(32.dp)) }
                    }
                }
            }
        }
    }

    // ── Edit Account bottom sheet ─────────────────────────────────────────────
    if (showEditSheet && account != null) {
        EditAccountSheet(
            account = account,
            linkedModes = linkedModes,
            onDismiss = onCloseEditSheet,
            onSave = { name, bal, modes -> onSaveEditAccount(account, name, bal, modes) },
            onDelete = { onDeleteAccount(account) },
            onDeleteMode = onDeleteMode
        )
    }

    if (showEditModeBalanceSheet && mode != null) {
        EditStandaloneBalanceSheet(
            title = "Edit available balance",
            subtitle = "Current balance for ${mode.displayLabel}",
            currentBalance = balance,
            onDismiss = onCloseEditModeBalanceSheet,
            onSave = { onSaveEditModeBalance(mode, it) }
        )
    }

    // ── Edit Credit Card bottom sheet (full card details) ─────────────────────
    if (showEditCardSheet && card != null) {
        EditCreditCardSheet(
            card = card,
            onDismiss = onCloseEditCardSheet,
            onSave = { name, avail, total, billing, due, color ->
                onSaveEditCard(name, avail, total, billing, due, color)
            },
            onDelete = { onDeleteCard(card) }
        )
    }

    // ── Edit Available Limit bottom sheet (quick edit) ────────────────────────
    if (showEditLimitSheet && card != null) {
        EditAvailableLimitSheet(
            card = card,
            onDismiss = onCloseEditLimitSheet,
            onSave = { newLimit -> onSaveCardAvailableLimit(card, newLimit) }
        )
    }

    // ── Card transactions screen (full-screen overlay) ────────────────────────
    AnimatedVisibility(
        visible = showCardTransactions && card != null,
        enter = slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) +
                fadeIn(animationSpec = tween(300)),
        exit = slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(250)) +
                fadeOut(animationSpec = tween(250))
    ) {
        if (card != null) {
            CardTransactionsScreen(
                card = card,
                transactions = transactions,
                currencySymbol = currencySymbol,
                isCurrentCycle = cardTransactionsIsCurrentCycle,
                onNavigateBack = onCloseCardTransactions
            )
        }
    }
}

@Composable
private fun BalanceAdjustmentRow(
    adjustment: BalanceAdjustment,
    dateFormatter: DateTimeFormatter,
    currencySymbol: String
) {
    val amountColor = if (adjustment.amountDelta < 0) {
        ExpenseRed
    } else {
        IncomeGreen
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            adjustment.adjustedAt.format(dateFormatter),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(20.dp))
        Text(
            "Balance Adjustment",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "${if (adjustment.amountDelta < 0) "-" else "+"}$currencySymbol${fmtAmt(abs(adjustment.amountDelta))}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = amountColor
        )
    }
}

// ─── Credit Card Detail Card (summary card in detail screen) ─────────────────

@Composable
private fun CreditCardDetailCard(
    card: CreditCard,
    transactions: List<Transaction>,
    currencySymbol: String,
    onSeeTransactions: () -> Unit,
    onSeeCurrentSpends: () -> Unit,
    onEditLimit: () -> Unit
) {
    val usedLimit = card.totalLimit - card.availableLimit
    val usagePct = if (card.totalLimit > 0)
        (usedLimit / card.totalLimit).toFloat().coerceIn(0f, 1f) else 0f

    // Fix: Current cycle spends (not previous cycle)
    val today = LocalDate.now()
    val billingDay = card.billingCycleDate

    // Determine the start date of the current billing cycle
    val cycleStart = run {
        val candidate = try {
            today.withDayOfMonth(billingDay)
        } catch (e: Exception) {
            today.withDayOfMonth(today.lengthOfMonth()) // Handle months with fewer days
        }
        if (!candidate.isAfter(today)) candidate else candidate.minusMonths(1)
    }

    val currentSpends = transactions
        .filter { it.type == TransactionType.EXPENSE }
        .filter { !it.dateTime.toLocalDate().isBefore(cycleStart) }
        .sumOf { it.amount }

    val dueDay = card.paymentDueDate

    val candidateDue = try {
        today.withDayOfMonth(dueDay)
    } catch (e: Exception) {
        today.withDayOfMonth(today.lengthOfMonth())
    }

    val dueDate = if (!candidateDue.isBefore(today)) candidateDue else candidateDue.plusMonths(1)
    val daysUntilDue = java.time.temporal.ChronoUnit.DAYS.between(today, dueDate).toInt()

    // Apply urgency colors
    val dueTextColor = when {
        daysUntilDue <= 2 -> ExpenseRed // Critical
        daysUntilDue in 3..7 -> TextWarning // Warning Orange
        else -> MaterialTheme.colorScheme.onSurface
    }

    val onTrack = (usagePct < 0.8f) // Toggle "High Usage" warning at 80%

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {

            // Available limit + on-track badge
            Text(
                "Available Credit Limit",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "$currencySymbol${fmtAmt(card.availableLimit)}",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (onTrack) Color(0xFF1B5E20) else Color(0xFF7F0000))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            if (onTrack) Icons.Default.CheckCircle else Icons.Default.Warning,
                            null,
                            tint = if (onTrack) Color(0xFF69F0AE) else Color(0xFFFF5252),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            if (onTrack) "On Track" else "High Usage",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (onTrack) Color(0xFF69F0AE) else Color(0xFFFF5252)
                        )
                    }
                }
            }

            // Fix 2: "Incorrect? Edit" opens the available limit bottom sheet
            Spacer(Modifier.height(2.dp))
            Row {
                Text(
                    "Incorrect? ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Edit",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onEditLimit() })
            }

            // Usage progress bar
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { usagePct },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (onTrack) Color(0xFF69F0AE) else ExpenseRed,
                trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )

            // Fix 4: Current cycle spends (label updated) + total limit
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.clickable { onSeeCurrentSpends() }
                    ) {
                        Text(
                            "Current Spends",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(
                            Icons.Default.ChevronRight, null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        "$currencySymbol${fmtAmt(currentSpends)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Total Credit Limit",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "$currencySymbol${fmtAmt(card.totalLimit)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Fix 3: Payment due text with urgency color
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                thickness = 0.5.dp
            )
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Payment due in $daysUntilDue day${if (daysUntilDue != 1) "s" else ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = dueTextColor     // ← urgency colour
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onSeeTransactions() }
                ) {
                    Text(
                        "See Transactions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        Icons.Default.ChevronRight, null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Record Payment + Bell button row
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White, contentColor = Color.Black
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                ) {
                    Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Record Payment",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(
                    onClick = { },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    Icon(Icons.Default.Notifications, null, modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}

// ─── Sort Order ───────────────────────────────────────────────────────────────

private enum class TransactionSortOrder { DATE, AMOUNT, CATEGORY }

// ─── Card Transactions Screen ─────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CardTransactionsScreen(
    card: CreditCard,
    transactions: List<Transaction>,
    currencySymbol: String,
    isCurrentCycle: Boolean,
    onNavigateBack: () -> Unit
) {
    val today = LocalDate.now()
    val billingDay = card.billingCycleDate

    // Determine start of the *current* billing cycle
    val currentCycleStart: LocalDate = run {
        val candidate = try {
            today.withDayOfMonth(billingDay)
        } catch (e: Exception) {
            today.withDayOfMonth(today.lengthOfMonth())
        }
        if (!candidate.isAfter(today)) candidate
        else {
            val prevMonth = today.minusMonths(1)
            try {
                prevMonth.withDayOfMonth(billingDay)
            } catch (e: Exception) {
                prevMonth.withDayOfMonth(prevMonth.lengthOfMonth())
            }
        }
    }

    // Pick the correct cycle window
    val cycleStart: LocalDate
    val cycleEnd: LocalDate
    val cycleLabel: String

    if (isCurrentCycle) {
        // Current cycle: billing date this month → one month later minus 1 day
        cycleStart = currentCycleStart
        cycleEnd = currentCycleStart.plusMonths(1).minusDays(1)
        cycleLabel = "Current Billing Cycle"
    } else {
        // Previous cycle: one month before current cycle start → current cycle start minus 1 day
        cycleStart = currentCycleStart.minusMonths(1)
        cycleEnd = currentCycleStart.minusDays(1)
        cycleLabel = "Previous Billing Cycle"
    }

    val dateFmt = DateTimeFormatter.ofPattern("d MMM")

    // Filter to the selected billing cycle window
    val cycleTransactions = transactions.filter {
        val d = it.dateTime.toLocalDate()
        !d.isBefore(cycleStart) && !d.isAfter(cycleEnd)
    }

    val cycleTotal = cycleTransactions.sumOf { it.amount }

    // Sort state
    var sortOrder by remember { mutableStateOf(TransactionSortOrder.DATE) }
    var showSortSheet by remember { mutableStateOf(false) }

    val sortedTransactions = when (sortOrder) {
        TransactionSortOrder.DATE -> cycleTransactions.sortedByDescending { it.dateTime }
        TransactionSortOrder.AMOUNT -> cycleTransactions.sortedByDescending { it.amount }
        TransactionSortOrder.CATEGORY -> cycleTransactions.sortedBy { it.categoryName }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Transactions", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showSortSheet = true },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .size(40.dp)
                    ) {
                        Icon(Icons.Default.FilterList, null, Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            item {
                Spacer(Modifier.height(8.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "$cycleLabel • ${cycleStart.format(dateFmt)} – ${cycleEnd.format(dateFmt)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Total Amount *",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "$currencySymbol${fmtAmt(cycleTotal)}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "* Please refer to your official credit card statement for the accurate Total Amount Due.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            if (sortedTransactions.isEmpty()) {
                item {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 80.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ReceiptLong, null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                        Text(
                            "No transactions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "No transactions during this period!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                items(sortedTransactions, key = { it.id }) { txn ->
                    CardTransactionRow(txn, currencySymbol)
                }
            }
        }
    }

    // ── Sort Bottom Sheet ─────────────────────────────────────────────────────
    if (showSortSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSortSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    "Sort by",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                )
                listOf(
                    TransactionSortOrder.DATE to "Date Created",
                    TransactionSortOrder.AMOUNT to "Amount",
                    TransactionSortOrder.CATEGORY to "Category"
                ).forEach { (order, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                sortOrder = order
                                showSortSheet = false
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = sortOrder == order,
                            onClick = {
                                sortOrder = order
                                showSortSheet = false
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            label,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (sortOrder == order) FontWeight.SemiBold
                            else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}


// ─── Card Transaction Row (used inside CardTransactionsScreen) ────────────────

@Composable
private fun CardTransactionRow(txn: Transaction, currencySymbol: String) {
    val today = LocalDate.now()
    val txnDate = txn.dateTime.toLocalDate()
    val dateLabel = when (txnDate) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        else -> txn.dateTime.format(DateTimeFormatter.ofPattern("d MMM"))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Category icon bubble
        CategoryIconBubble(
            iconKey = txn.categoryIcon.ifEmpty { "category" },
            colorHex = txn.categoryColorHex.ifEmpty { "#6750A4" },
            size = 44
        )

        Spacer(Modifier.width(14.dp))

        // Middle: title + payment mode
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = txn.note.ifEmpty { txn.categoryName.ifEmpty { "Transaction" } },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CreditCard, null,
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(4.dp))
                val cardName = txn.paymentModeName.ifEmpty { "Credit Card" }
                    .replace(Regex("""\s*\(₹[^)]+\)"""), "").trim()
                Text(
                    text = cardName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        // Right: amount + date
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "$currencySymbol${fmtAmt(txn.amount)}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = dateLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditAccountSheet(
    account: BankAccount,
    linkedModes: List<PaymentMode>,
    onDismiss: () -> Unit,
    onSave: (name: String, balance: Double, linkedModes: List<PaymentMode>) -> Unit,
    onDelete: () -> Unit,
    onDeleteMode: (PaymentMode) -> Unit
) {
    var editName by remember(account) { mutableStateOf(account.name) }
    var editBalance by remember(account) { mutableStateOf(account.balance.toString()) }
    var localLinkedModes by remember(account.id, linkedModes) { mutableStateOf(linkedModes) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showModeAddRow by remember { mutableStateOf(false) }
    var newModeType by remember { mutableStateOf(PaymentModeType.DEBIT_CARD) }
    var newModeId by remember { mutableStateOf("") }
    var tempModeIdSeed by remember(account.id) { mutableStateOf(-1L) }
    var editingMode by remember { mutableStateOf<PaymentMode?>(null) }
    var modePendingDelete by remember { mutableStateOf<PaymentMode?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
        ) {
            // ── Sheet header ──────────────────────────────────────────────────
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp, top = 20.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Edit account",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                // Delete button
                IconButton(onClick = { showDeleteDialog = true }) {
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Delete, contentDescription = "Delete",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
                Spacer(Modifier.width(4.dp))
                // Close button
                IconButton(onClick = onDismiss) {
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Close, contentDescription = "Close",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // ── Name field ────────────────────────────────────────────────────
            Column(Modifier.padding(horizontal = 20.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.TextFields, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    BasicTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 16.dp),
                        textStyle = MaterialTheme.typography.headlineSmall.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        singleLine = true,
                        decorationBox = { inner ->
                            if (editName.isEmpty()) {
                                Text(
                                    "Name",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                        .copy(alpha = 0.5f)
                                )
                            }
                            inner()
                        }
                    )
                }
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant
                        .copy(alpha = 0.4f)
                )

                // ── Balance field ─────────────────────────────────────────────
                Spacer(Modifier.height(8.dp))
                Text(
                    "Current Balance",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "₹",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Light,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(8.dp))
                    BasicTextField(
                        value = editBalance,
                        onValueChange = { editBalance = it },
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 12.dp),
                        textStyle = MaterialTheme.typography.headlineMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        decorationBox = { inner ->
                            if (editBalance.isEmpty()) {
                                Text(
                                    "0",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                        .copy(alpha = 0.4f)
                                )
                            }
                            inner()
                        }
                    )
                }

                // ── Linked payment modes ──────────────────────────────────────
                Spacer(Modifier.height(20.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Linked payment modes",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedButton(
                        onClick = { showModeAddRow = true },
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Add, null, Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add", style = MaterialTheme.typography.labelMedium)
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Existing linked modes
                localLinkedModes.forEach { mode ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Mode icon — no background so UPI logo and other icons render cleanly
                        PaymentModeIcon(
                            type = mode.type,
                            modifier = Modifier.size(26.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            mode.type.displayName() +
                                    if (mode.identifier.isNotEmpty()) " / ${mode.identifier}" else "",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        // Edit mode button — no background, no fixed size clip, just plain IconButton
                        IconButton(onClick = { editingMode = mode }) {
                            Icon(
                                Icons.Default.Edit, "Edit",
                                Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        // Delete mode button
                        IconButton(onClick = { modePendingDelete = mode }) {
                            Icon(
                                Icons.Default.Delete, "Delete",
                                Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        thickness = 0.5.dp
                    )
                }

                // Inline add mode row
                if (showModeAddRow) {
                    Spacer(Modifier.height(12.dp))
                    val linkedTypes = listOf(
                        PaymentModeType.DEBIT_CARD, PaymentModeType.UPI,
                        PaymentModeType.NET_BANKING, PaymentModeType.CHEQUE
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(linkedTypes) { type ->
                            FilterChip(
                                selected = newModeType == type,
                                onClick = { newModeType = type; newModeId = "" },
                                label = {
                                    Text(
                                        type.displayName(),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newModeId,
                        onValueChange = { newModeId = it },
                        label = {
                            Text(
                                when (newModeType) {
                                    PaymentModeType.UPI -> "UPI ID (optional)"
                                    PaymentModeType.DEBIT_CARD -> "Last 4 digits (optional)"
                                    else -> "Identifier (optional)"
                                }
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            TextButton(onClick = {
                                localLinkedModes = localLinkedModes + PaymentMode(
                                    id = tempModeIdSeed,
                                    bankAccountId = account.id,
                                    type = newModeType,
                                    identifier = newModeId.trim(),
                                    userId = account.userId
                                )
                                tempModeIdSeed -= 1
                                newModeId = ""; showModeAddRow = false
                            }) { Text("Add") }
                        }
                    )
                    Spacer(Modifier.height(4.dp))
                    TextButton(
                        onClick = { showModeAddRow = false; newModeId = "" },
                        modifier = Modifier.align(Alignment.End)
                    ) { Text("Cancel") }
                }

                Spacer(Modifier.height(32.dp))

                // Save button
                Button(
                    onClick = {
                        val bal = editBalance.toDoubleOrNull() ?: account.balance
                        if (editName.isNotBlank()) onSave(editName, bal, localLinkedModes)
                    },
                    enabled = editName.isNotBlank(),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .height(52.dp)
                        .width(140.dp)
                        .align(Alignment.End)
                ) {
                    Text(
                        "Save", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Account") },
            text = { Text("Delete ${account.name} and all its linked payment modes? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteDialog = false }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (editingMode != null) {
        AddEditPaymentModeDialog(
            editing = editingMode,
            accounts = listOf(account),
            preselectedAccountId = account.id,
            onDismiss = { editingMode = null },
            onSave = { _, type, identifier ->
                localLinkedModes = localLinkedModes.map { mode ->
                    if (mode.id == editingMode?.id) {
                        mode.copy(type = type, identifier = identifier)
                    } else {
                        mode
                    }
                }
                editingMode = null
            }
        )
    }

    if (modePendingDelete != null) {
        AlertDialog(
            onDismissRequest = { modePendingDelete = null },
            title = { Text("Delete Payment Mode") },
            text = {
                Text(
                    "Delete ${modePendingDelete?.type?.displayName()}${
                        modePendingDelete?.identifier?.takeIf { it.isNotBlank() }?.let { " ($it)" } ?: ""
                    } from this account?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        modePendingDelete?.let { mode ->
                            localLinkedModes = localLinkedModes.filter { it.id != mode.id }
                        }
                        modePendingDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { modePendingDelete = null }) { Text("Cancel") }
            }
        )
    }
}

// ─── Payment mode icon with UPI logo support ──────────────────────────────────

@Composable
private fun PaymentModeIcon(
    type: PaymentModeType,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    when (type) {
        PaymentModeType.UPI -> {
            Image(
                painter = painterResource(id = R.drawable.ic_upi),
                contentDescription = "UPI",
                modifier = modifier
            )
        }
        PaymentModeType.DEBIT_CARD -> {
            Image(
                painter = painterResource(id = R.drawable.ic_payment_card_logo),
                contentDescription = "Debit Card",
                modifier = modifier
            )
        }
        PaymentModeType.WALLET -> {
            Image(
                painter = painterResource(id = R.drawable.ic_wallet_logo),
                contentDescription = "Wallet",
                modifier = modifier
            )
        }
        else -> {
            val icon = when (type) {
                PaymentModeType.NET_BANKING -> Icons.Default.Language
                PaymentModeType.CHEQUE -> Icons.Default.EditNote
                PaymentModeType.CASH -> Icons.Default.Payments
                else -> Icons.Default.Payment
            }
            Icon(icon, contentDescription = null, modifier = modifier, tint = tint)
        }
    }
}


@Composable
private fun DetailTransactionRow(
    txn: Transaction,
    dateFormatter: DateTimeFormatter,
    currencySymbol: String,
    onClick: () -> Unit
) {
    val isExpense = txn.type == TransactionType.EXPENSE
    val amountColor = if (isExpense) ExpenseRed else IncomeGreen
    val prefix = if (isExpense) "-" else "+"
    val title = txn.note.ifEmpty { txn.categoryName.ifEmpty { "Transaction" } }

    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            txn.dateTime.format(dateFormatter),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(60.dp)
        )
        Text(
            title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            "$prefix$currencySymbol${fmtAmt(txn.amount)}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = amountColor
        )
    }
}


// ─── Unified Add Account Bottom Sheet ────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddAccountSheet(
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSaveBank: (
        name: String, balance: Double, colorHex: String, accountIdentifier: String,
        modes: List<Pair<PaymentModeType, String>>
    ) -> Unit,
    onSaveWallet: (identifier: String) -> Unit,
    onSaveCard: (
        name: String, availableLimit: Double, totalLimit: Double,
        billingCycleDate: Int, paymentDueDate: Int, colorHex: String
    ) -> Unit
) {
    // 0 = Bank Account, 1 = Wallet, 2 = Credit Card
    var selectedTab by remember { mutableIntStateOf(0) }

    // ── Bank Account state ────────────────────────────────────────────────────
    var bankName by remember { mutableStateOf("") }
    var bankBalance by remember { mutableStateOf("") }
    var bankAccountLastFour by remember { mutableStateOf("") }
    var pendingModes by remember { mutableStateOf<List<Pair<PaymentModeType, String>>>(emptyList()) }
    var showModeAddRow by remember { mutableStateOf(false) }
    var newModeType by remember { mutableStateOf(PaymentModeType.DEBIT_CARD) }
    var newModeId by remember { mutableStateOf("") }

    // ── Wallet state ──────────────────────────────────────────────────────────
    var walletName by remember { mutableStateOf("") }
    var walletBalance by remember { mutableStateOf("") }

    // ── Credit Card state ─────────────────────────────────────────────────────
    var cardName by remember { mutableStateOf("") }
    var cardAvail by remember { mutableStateOf("") }
    var cardTotal by remember { mutableStateOf("") }
    var cardBilling by remember { mutableStateOf("1") }
    var cardDue by remember { mutableStateOf("15") }
    var cardColor by remember { mutableStateOf("#EA4335") }

    listOf(
        "#EA4335", "#E91E63", "#9C27B0", "#3F51B5",
        "#2196F3", "#009688", "#FF9800", "#795548"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            // ── Sheet header ──────────────────────────────────────────────────
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp, top = 20.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Add account",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Box(
                        Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Close, contentDescription = "Close",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // ── Tab selector: Bank Account / Wallet / Credit Card ─────────────
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                listOf("Bank Account", "Wallet", "Credit Card").forEachIndexed { idx, label ->
                    val active = selectedTab == idx
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (active) MaterialTheme.colorScheme.surface
                                else Color.Transparent
                            )
                            .clickable { selectedTab = idx }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                            color = if (active) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Tab content ───────────────────────────────────────────────────
            when (selectedTab) {

                // ── Bank Account ──────────────────────────────────────────────
                0 -> {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Name field — minimal style matching image
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.TextFields, null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            BasicTextField(
                                value = bankName,
                                onValueChange = { bankName = it },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 16.dp),
                                textStyle = MaterialTheme.typography.headlineSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                singleLine = true,
                                decorationBox = { inner ->
                                    if (bankName.isEmpty()) {
                                        Text(
                                            "Name",
                                            style = MaterialTheme.typography.headlineSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                .copy(alpha = 0.5f)
                                        )
                                    }
                                    inner()
                                }
                            )
                        }
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant
                                .copy(alpha = 0.4f)
                        )

                        // Balance field
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Current Balance",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                currencySymbol,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Light,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(8.dp))
                            BasicTextField(
                                value = bankBalance,
                                onValueChange = { bankBalance = it },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 12.dp),
                                textStyle = MaterialTheme.typography.headlineMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal
                                ),
                                decorationBox = { inner ->
                                    if (bankBalance.isEmpty()) {
                                        Text(
                                            "0",
                                            style = MaterialTheme.typography.headlineMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                .copy(alpha = 0.4f)
                                        )
                                    }
                                    inner()
                                }
                            )
                        }

                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Bank Account Last 4 Digits",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "#",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Light,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(8.dp))
                            BasicTextField(
                                value = bankAccountLastFour,
                                onValueChange = { input ->
                                    bankAccountLastFour = input.filter { it.isDigit() }.take(4)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 12.dp),
                                textStyle = MaterialTheme.typography.headlineMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                decorationBox = { inner ->
                                    if (bankAccountLastFour.isEmpty()) {
                                        Text(
                                            "1234",
                                            style = MaterialTheme.typography.headlineMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                .copy(alpha = 0.4f)
                                        )
                                    }
                                    inner()
                                }
                            )
                        }

                        Spacer(Modifier.height(20.dp))

                        // Linked payment modes section
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Linked payment modes",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            OutlinedButton(
                                onClick = { showModeAddRow = true },
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Add, null, Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Add", style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        val autoLinkedModeIdentifier = bankAccountLastFour
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PaymentModeIcon(
                                type = PaymentModeType.NET_BANKING,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Bank Account" +
                                    if (autoLinkedModeIdentifier.isNotEmpty()) " (${autoLinkedModeIdentifier})" else "",
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Default",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            thickness = 0.5.dp
                        )

                        if (pendingModes.isEmpty() && !showModeAddRow) {
                            // Empty state matching image
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Link, null,
                                    modifier = Modifier.size(44.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        .copy(alpha = 0.4f)
                                )
                                Text(
                                    "Link your payment modes",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                        .copy(alpha = 0.7f)
                                )
                                Text(
                                    "You can add a Debit Card, UPI or other payment " +
                                            "modes to use with this bank account.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                        .copy(alpha = 0.5f),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        } else {
                            // Show already-added modes
                            pendingModes.forEachIndexed { idx, (type, id) ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    PaymentModeIcon(
                                        type = type,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        type.displayName() +
                                                if (id.isNotEmpty()) " ($id)" else "",
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    IconButton(
                                        onClick = {
                                            pendingModes = pendingModes.toMutableList()
                                                .also { it.removeAt(idx) }
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close, null,
                                            Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant
                                        .copy(alpha = 0.3f),
                                    thickness = 0.5.dp
                                )
                            }
                        }

                        // Inline mode add row
                        if (showModeAddRow) {
                            Spacer(Modifier.height(8.dp))
                            // Mode type chips
                            val linkedTypes = listOf(
                                PaymentModeType.DEBIT_CARD,
                                PaymentModeType.UPI,
                                PaymentModeType.NET_BANKING,
                                PaymentModeType.CHEQUE
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(linkedTypes) { type ->
                                    FilterChip(
                                        selected = newModeType == type,
                                        onClick = { newModeType = type; newModeId = "" },
                                        label = {
                                            Text(
                                                type.displayName(),
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = newModeId,
                                onValueChange = { newModeId = it },
                                label = {
                                    Text(
                                        when (newModeType) {
                                            PaymentModeType.UPI -> "UPI ID (optional)"
                                            PaymentModeType.DEBIT_CARD -> "Last 4 digits (optional)"
                                            else -> "Identifier (optional)"
                                        }
                                    )
                                },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                trailingIcon = {
                                    TextButton(onClick = {
                                        pendingModes =
                                            pendingModes + (newModeType to newModeId.trim())
                                        newModeId = ""
                                        showModeAddRow = false
                                    }) { Text("Add") }
                                }
                            )
                            Spacer(Modifier.height(4.dp))
                            TextButton(
                                onClick = { showModeAddRow = false; newModeId = "" },
                                modifier = Modifier.align(Alignment.End)
                            ) { Text("Cancel") }
                        }

                        Spacer(Modifier.height(24.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                        Button(
                            onClick = {
                                val bal = bankBalance.toDoubleOrNull() ?: 0.0
                                if (bankName.isNotBlank())
                                    onSaveBank(
                                        bankName,
                                        bal,
                                        "#6750A4",
                                        bankAccountLastFour,
                                        pendingModes
                                    )
                            },
                            enabled = bankName.isNotBlank(),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .height(52.dp)
                                .width(140.dp)
                        ) {
                            Text(
                                "Save", style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }

                // ── Wallet ────────────────────────────────────────────────────
                1 -> {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Name field — same minimal style as Bank Account
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.TextFields, null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            BasicTextField(
                                value = walletName,
                                onValueChange = { walletName = it },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 16.dp),
                                textStyle = MaterialTheme.typography.headlineSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                singleLine = true,
                                decorationBox = { inner ->
                                    if (walletName.isEmpty()) {
                                        Text(
                                            "Name",
                                            style = MaterialTheme.typography.headlineSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                .copy(alpha = 0.5f)
                                        )
                                    }
                                    inner()
                                }
                            )
                        }
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant
                                .copy(alpha = 0.4f)
                        )

                        // Balance field
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Current Balance",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                currencySymbol,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Light,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(8.dp))
                            BasicTextField(
                                value = walletBalance,
                                onValueChange = { walletBalance = it },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 12.dp),
                                textStyle = MaterialTheme.typography.headlineMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                decorationBox = { inner ->
                                    if (walletBalance.isEmpty()) {
                                        Text(
                                            "0",
                                            style = MaterialTheme.typography.headlineMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                .copy(alpha = 0.4f)
                                        )
                                    }
                                    inner()
                                }
                            )
                        }

                        Spacer(Modifier.height(32.dp))
                        Button(
                            onClick = {
                                if (walletName.isNotBlank())
                                    onSaveWallet(walletName.trim())
                            },
                            enabled = walletName.isNotBlank(),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .height(52.dp)
                                .width(140.dp)
                                .align(Alignment.End)
                        ) {
                            Text(
                                "Save", style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                }

                // ── Credit Card ───────────────────────────────────────────────
                2 -> {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // ── Card Name ─────────────────────────────────────────
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.TextFields, null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            BasicTextField(
                                value = cardName, onValueChange = { cardName = it },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 16.dp),
                                textStyle = MaterialTheme.typography.headlineSmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                singleLine = true,
                                decorationBox = { inner ->
                                    if (cardName.isEmpty()) {
                                        Text(
                                            "Name",
                                            style = MaterialTheme.typography.headlineSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                .copy(alpha = 0.5f)
                                        )
                                    }
                                    inner()
                                }
                            )
                        }
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant
                                .copy(alpha = 0.4f)
                        )

                        // ── Current Available Limit ────────────────────────────
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Current Available Limit",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                currencySymbol,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Light,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(8.dp))
                            BasicTextField(
                                value = cardAvail, onValueChange = { cardAvail = it },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 12.dp),
                                textStyle = MaterialTheme.typography.headlineMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                decorationBox = { inner ->
                                    if (cardAvail.isEmpty()) {
                                        Text(
                                            "0", style = MaterialTheme.typography.headlineMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                .copy(alpha = 0.4f)
                                        )
                                    }
                                    inner()
                                }
                            )
                        }
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant
                                .copy(alpha = 0.4f)
                        )

                        // ── Total Credit Limit ────────────────────────────────
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Total Credit Limit",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                currencySymbol,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Light,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(8.dp))
                            BasicTextField(
                                value = cardTotal, onValueChange = { cardTotal = it },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 12.dp),
                                textStyle = MaterialTheme.typography.headlineMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                decorationBox = { inner ->
                                    if (cardTotal.isEmpty()) {
                                        Text(
                                            "0", style = MaterialTheme.typography.headlineMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                .copy(alpha = 0.4f)
                                        )
                                    }
                                    inner()
                                }
                            )
                        }
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant
                                .copy(alpha = 0.4f)
                        )

                        // ── Billing Cycle Start Date ───────────────────────────
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Billing Cycle Start Date",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CalendarMonth, null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            BasicTextField(
                                value = cardBilling,
                                onValueChange = { v ->
                                    val d = v.filter { it.isDigit() }
                                    if (d.isEmpty() || (d.toIntOrNull() ?: 0) <= 31) cardBilling = d
                                },
                                modifier = Modifier.padding(vertical = 12.dp),
                                textStyle = MaterialTheme.typography.headlineMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                decorationBox = { inner ->
                                    if (cardBilling.isEmpty()) {
                                        Text(
                                            "01",
                                            style = MaterialTheme.typography.headlineMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                .copy(alpha = 0.4f)
                                        )
                                    }
                                    inner()
                                }
                            )
                            Text(
                                " of every month",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant
                                .copy(alpha = 0.4f)
                        )

                        // ── Payment Due Date ───────────────────────────────────
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Payment Due Date",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.CalendarMonth, null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            BasicTextField(
                                value = cardDue,
                                onValueChange = { v ->
                                    val d = v.filter { it.isDigit() }
                                    if (d.isEmpty() || (d.toIntOrNull() ?: 0) <= 31) cardDue = d
                                },
                                modifier = Modifier.padding(vertical = 12.dp),
                                textStyle = MaterialTheme.typography.headlineMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                decorationBox = { inner ->
                                    if (cardDue.isEmpty()) {
                                        Text(
                                            "15",
                                            style = MaterialTheme.typography.headlineMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                .copy(alpha = 0.4f)
                                        )
                                    }
                                    inner()
                                }
                            )
                            Text(
                                " of every month",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(Modifier.height(32.dp))
                        val bInt = cardBilling.toIntOrNull() ?: 0
                        val dInt = cardDue.toIntOrNull() ?: 0
                        Button(
                            onClick = {
                                onSaveCard(
                                    cardName,
                                    cardAvail.toDoubleOrNull() ?: 0.0,
                                    cardTotal.toDoubleOrNull() ?: 0.0,
                                    bInt, dInt, cardColor
                                )
                            },
                            enabled = cardName.isNotBlank() &&
                                    cardAvail.toDoubleOrNull() != null &&
                                    cardTotal.toDoubleOrNull() != null &&
                                    bInt in 1..31 && dInt in 1..31,
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .height(52.dp)
                                .width(140.dp)
                                .align(Alignment.End)
                        ) {
                            Text(
                                "Save", style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

// ─── Edit Credit Card Sheet (full card details) ───────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditCreditCardSheet(
    card: CreditCard,
    onDismiss: () -> Unit,
    onSave: (String, Double, Double, Int, Int, String) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember(card) { mutableStateOf(card.name) }
    var availableLimit by remember(card) { mutableStateOf(card.availableLimit.toLong().toString()) }
    var totalLimit by remember(card) { mutableStateOf(card.totalLimit.toLong().toString()) }
    var billingDate by remember(card) { mutableStateOf(card.billingCycleDate.toString()) }
    var dueDate by remember(card) { mutableStateOf(card.paymentDueDate.toString()) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val bInt = billingDate.toIntOrNull() ?: 0
    val dInt = dueDate.toIntOrNull() ?: 0
    val isValid = name.isNotBlank() &&
            availableLimit.toDoubleOrNull() != null &&
            totalLimit.toDoubleOrNull() != null &&
            bInt in 1..31 && dInt in 1..31

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
        ) {
            // ── Header ────────────────────────────────────────────────────────
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp, top = 20.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Edit Card",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { showDeleteDialog = true }) {
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Delete, "Delete", Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = onDismiss) {
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Close, "Close", Modifier.size(16.dp))
                    }
                }
            }

            Column(Modifier.padding(horizontal = 20.dp)) {

                // ── Card Name ─────────────────────────────────────────────────
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CreditCard, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    BasicTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 16.dp),
                        textStyle = MaterialTheme.typography.headlineSmall.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        singleLine = true,
                        decorationBox = { inner ->
                            if (name.isEmpty()) {
                                Text(
                                    "Card Name",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                            inner()
                        }
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // ── Current Available Limit ───────────────────────────────────
                Spacer(Modifier.height(8.dp))
                Text(
                    "Current Available Limit",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "₹",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Light,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(8.dp))
                    BasicTextField(
                        value = availableLimit,
                        onValueChange = { availableLimit = it },
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 12.dp),
                        textStyle = MaterialTheme.typography.headlineMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        decorationBox = { inner ->
                            if (availableLimit.isEmpty()) {
                                Text(
                                    "0",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                            }
                            inner()
                        }
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // ── Total Credit Limit ────────────────────────────────────────
                Spacer(Modifier.height(8.dp))
                Text(
                    "Total Credit Limit",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "₹",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Light,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(8.dp))
                    BasicTextField(
                        value = totalLimit,
                        onValueChange = { totalLimit = it },
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 12.dp),
                        textStyle = MaterialTheme.typography.headlineMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        decorationBox = { inner ->
                            if (totalLimit.isEmpty()) {
                                Text(
                                    "0",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                            }
                            inner()
                        }
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // ── Billing Cycle Start Date ──────────────────────────────────
                Spacer(Modifier.height(8.dp))
                Text(
                    "Billing Cycle Start Date",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CalendarMonth, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    BasicTextField(
                        value = billingDate,
                        onValueChange = { v ->
                            val d = v.filter { it.isDigit() }
                            if (d.isEmpty() || (d.toIntOrNull() ?: 0) <= 31) billingDate = d
                        },
                        modifier = Modifier
                            .width(64.dp)
                            .padding(vertical = 12.dp),
                        textStyle = MaterialTheme.typography.displaySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        decorationBox = { inner ->
                            if (billingDate.isEmpty()) {
                                Text(
                                    "1",
                                    style = MaterialTheme.typography.displaySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                            }
                            inner()
                        }
                    )
                    Text(
                        "of every month",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // ── Payment Due Date ──────────────────────────────────────────
                Spacer(Modifier.height(8.dp))
                Text(
                    "Payment Due Date",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CalendarMonth, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    BasicTextField(
                        value = dueDate,
                        onValueChange = { v ->
                            val d = v.filter { it.isDigit() }
                            if (d.isEmpty() || (d.toIntOrNull() ?: 0) <= 31) dueDate = d
                        },
                        modifier = Modifier
                            .width(64.dp)
                            .padding(vertical = 12.dp),
                        textStyle = MaterialTheme.typography.displaySmall.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        decorationBox = { inner ->
                            if (dueDate.isEmpty()) {
                                Text(
                                    "1",
                                    style = MaterialTheme.typography.displaySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                            }
                            inner()
                        }
                    )
                    Text(
                        "of every month",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // ── Save Button ───────────────────────────────────────────────
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = {
                        onSave(
                            name,
                            availableLimit.toDoubleOrNull() ?: card.availableLimit,
                            totalLimit.toDoubleOrNull() ?: card.totalLimit,
                            bInt, dInt,
                            card.colorHex   // preserve existing color
                        )
                    },
                    enabled = isValid,
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier
                        .height(52.dp)
                        .width(140.dp)
                        .align(Alignment.End)
                ) {
                    Text(
                        "Save",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Credit Card") },
            text = { Text("Remove \"${card.name}\"? This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteDialog = false }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// ─── Edit Available Limit Sheet (quick edit, matching image 2) ────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditAvailableLimitSheet(
    card: CreditCard,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var limitInput by remember(card) { mutableStateOf(card.availableLimit.toString()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
        dragHandle = { BottomSheetDefaults.HiddenShape },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Edit available limit",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Close, "Close", Modifier.size(16.dp))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "Current available limit for ${card.name}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = limitInput,
                onValueChange = { limitInput = it },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Tip: Track all your transactions to keep your limit in sync.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { limitInput.toDoubleOrNull()?.let { onSave(it) } },
                enabled = limitInput.toDoubleOrNull() != null,
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier
                    .height(44.dp)
                    .width(88.dp)
                    .align(Alignment.End)
            ) {
                Text(
                    "Save",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditStandaloneBalanceSheet(
    title: String,
    subtitle: String,
    currentBalance: Double,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var balanceInput by remember(currentBalance) { mutableStateOf(currentBalance.toString()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
        dragHandle = { BottomSheetDefaults.HiddenShape },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Close, "Close", Modifier.size(16.dp))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = balanceInput,
                onValueChange = { balanceInput = it },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "This creates a balance adjustment entry instead of changing transaction history.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { balanceInput.toDoubleOrNull()?.let(onSave) },
                enabled = balanceInput.toDoubleOrNull() != null,
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ─── Add / Edit Bank Account Dialog ──────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditAccountDialog(
    editing: BankAccount?,
    onDismiss: () -> Unit,
    onSave: (name: String, balance: Double, colorHex: String) -> Unit
) {
    var name by remember { mutableStateOf(editing?.name ?: "") }
    var balance by remember { mutableStateOf(editing?.balance?.toString() ?: "0") }
    var colorHex by remember { mutableStateOf(editing?.colorHex ?: "#6750A4") }

    val colorOptions = listOf(
        "#6750A4", "#4285F4", "#EA4335", "#34A853",
        "#FF9800", "#00BCD4", "#795548", "#E91E63"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editing != null) "Edit Account" else "Add Bank Account") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Account Name") },
                    placeholder = { Text("e.g. SBI Savings, HDFC Current") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.AccountBalance, null) }
                )
                OutlinedTextField(
                    value = balance, onValueChange = { balance = it },
                    label = { Text("Current Balance") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.CurrencyRupee, null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                Text("Colour", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(colorOptions) { hex ->
                        val c = Color(hex.toColorInt())
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(c)
                                .then(
                                    if (hex == colorHex)
                                        Modifier.border(
                                            3.dp,
                                            Color.White,
                                            CircleShape
                                        ) else Modifier
                                )
                                .clickable { colorHex = hex }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) onSave(
                        name,
                        balance.toDoubleOrNull() ?: 0.0,
                        colorHex
                    )
                },
                enabled = name.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ─── Add / Edit Credit Card Dialog ───────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditCreditCardDialog(
    editing: CreditCard?,
    onDismiss: () -> Unit,
    onSave: (
        name: String, availableLimit: Double, totalLimit: Double,
        billingCycleDate: Int, paymentDueDate: Int, colorHex: String
    ) -> Unit
) {
    var name by remember { mutableStateOf(editing?.name ?: "") }
    var availableLimit by remember { mutableStateOf(editing?.availableLimit?.toString() ?: "") }
    var totalLimit by remember { mutableStateOf(editing?.totalLimit?.toString() ?: "") }
    var billingCycleDate by remember {
        mutableStateOf(
            editing?.billingCycleDate?.toString() ?: "1"
        )
    }
    var paymentDueDate by remember { mutableStateOf(editing?.paymentDueDate?.toString() ?: "15") }
    var colorHex by remember { mutableStateOf(editing?.colorHex ?: "#EA4335") }

    val colorOptions = listOf(
        "#EA4335", "#E91E63", "#9C27B0", "#3F51B5",
        "#2196F3", "#009688", "#FF9800", "#795548"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editing != null) "Edit Credit Card" else "Add Credit Card") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Card Name") },
                    placeholder = { Text("e.g. HDFC Regalia, Axis Magnus") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.CreditCard, null) })
                OutlinedTextField(
                    value = availableLimit, onValueChange = { availableLimit = it },
                    label = { Text("Current Available Limit (₹)") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.CurrencyRupee, null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = totalLimit, onValueChange = { totalLimit = it },
                    label = { Text("Total Credit Limit (₹)") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.CurrencyRupee, null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = billingCycleDate,
                        onValueChange = { v ->
                            val d = v.filter { it.isDigit() }
                            if (d.isEmpty() || (d.toIntOrNull() ?: 0) <= 31) billingCycleDate = d
                        },
                        label = { Text("Billing Cycle Date") }, placeholder = { Text("1–31") },
                        singleLine = true, modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        supportingText = { Text("Day of month") })
                    OutlinedTextField(
                        value = paymentDueDate,
                        onValueChange = { v ->
                            val d = v.filter { it.isDigit() }
                            if (d.isEmpty() || (d.toIntOrNull() ?: 0) <= 31) paymentDueDate = d
                        },
                        label = { Text("Payment Due Date") }, placeholder = { Text("1–31") },
                        singleLine = true, modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        supportingText = { Text("Day of month") })
                }
                Text("Card Colour", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(colorOptions) { hex ->
                        val c = Color(hex.toColorInt())
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(c)
                                .then(
                                    if (hex == colorHex)
                                        Modifier.border(
                                            3.dp,
                                            Color.White,
                                            CircleShape
                                        ) else Modifier
                                )
                                .clickable { colorHex = hex }
                        )
                    }
                }
            }
        },
        confirmButton = {
            val bInt = billingCycleDate.toIntOrNull() ?: 0
            val pInt = paymentDueDate.toIntOrNull() ?: 0
            Button(
                onClick = {
                    onSave(
                        name, availableLimit.toDoubleOrNull() ?: 0.0,
                        totalLimit.toDoubleOrNull() ?: 0.0, bInt, pInt, colorHex
                    )
                },
                enabled = name.isNotBlank() &&
                        availableLimit.toDoubleOrNull() != null &&
                        totalLimit.toDoubleOrNull() != null &&
                        bInt in 1..31 && pInt in 1..31
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ─── Add / Edit Payment Mode Dialog ──────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditPaymentModeDialog(
    editing: PaymentMode?,
    accounts: List<BankAccount>,
    preselectedAccountId: Long?,
    onDismiss: () -> Unit,
    onSave: (bankAccountId: Long?, type: PaymentModeType, identifier: String) -> Unit
) {
    var selectedType by remember { mutableStateOf(editing?.type ?: PaymentModeType.UPI) }
    var identifier by remember { mutableStateOf(editing?.identifier ?: "") }
    var selectedAccountId by remember {
        mutableStateOf(
            editing?.bankAccountId ?: preselectedAccountId
        )
    }
    val requiresAccount = selectedType.requiresBankAccount()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editing != null) "Edit Payment Mode" else "Add Payment Mode") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Mode Type", style = MaterialTheme.typography.labelLarge)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PaymentModeType.entries.chunked(3).forEach { row ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { type ->
                                FilterChip(
                                    selected = selectedType == type,
                                    onClick = {
                                        selectedType = type
                                        if (!type.requiresBankAccount()) selectedAccountId = null
                                    },
                                    label = {
                                        Text(
                                            type.displayName(),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            val rem = 3 - row.size
                            if (rem > 0) Box(Modifier.weight(rem.toFloat()))
                        }
                    }
                }
                if (requiresAccount) {
                    Text("Linked Bank Account", style = MaterialTheme.typography.labelLarge)
                    if (accounts.isEmpty()) {
                        Text(
                            "No bank accounts found. Add one first.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            accounts.forEach { account ->
                                val selected = account.id == selectedAccountId
                                val accent = runCatching {
                                    Color(account.colorHex.toColorInt())
                                }.getOrDefault(Color(0xFF6750A4))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (selected) accent.copy(alpha = 0.12f)
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        )
                                        .border(
                                            1.dp,
                                            if (selected) accent else Color.Transparent,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { selectedAccountId = account.id }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selected,
                                        onClick = { selectedAccountId = account.id },
                                        colors = RadioButtonDefaults.colors(selectedColor = accent)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        account.name, style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
                val idLabel = when (selectedType) {
                    PaymentModeType.UPI -> "UPI ID (optional)"
                    PaymentModeType.DEBIT_CARD -> "Last 4 digits (optional)"
                    PaymentModeType.WALLET -> "Wallet name (optional)"
                    else -> "Identifier (optional)"
                }
                OutlinedTextField(
                    value = identifier, onValueChange = { identifier = it },
                    label = { Text(idLabel) }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        if (requiresAccount) selectedAccountId else null,
                        selectedType, identifier.trim()
                    )
                },
                enabled = !requiresAccount || selectedAccountId != null
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
