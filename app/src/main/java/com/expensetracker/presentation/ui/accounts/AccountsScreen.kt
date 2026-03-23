package com.expensetracker.presentation.ui.accounts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.data.repository.AuthManager
import com.expensetracker.data.repository.BankAccountRepository
import com.expensetracker.data.repository.CreditCardRepository
import com.expensetracker.data.repository.PaymentModeRepository
import com.expensetracker.data.repository.TransactionRepository
import com.expensetracker.data.repository.UserPreferencesRepository
import com.expensetracker.domain.model.BankAccount
import com.expensetracker.domain.model.CreditCard
import com.expensetracker.domain.model.PaymentMode
import com.expensetracker.domain.model.PaymentModeType
import com.expensetracker.domain.model.Transaction
import com.expensetracker.domain.model.TransactionType
import com.expensetracker.presentation.components.EmptyState
import com.expensetracker.presentation.theme.ExpenseRed
import com.expensetracker.presentation.theme.IncomeGreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlin.math.abs

// ─── ViewModel ────────────────────────────────────────────────────────────────

data class AccountsUiState(
    val accounts: List<BankAccount> = emptyList(),
    val allModes: List<PaymentMode> = emptyList(),
    val creditCards: List<CreditCard> = emptyList(),
    // Pre-computed balance per standalone mode id (income − expense)
    val modeBalances: Map<Long, Double> = emptyMap(),
    // Summary tiles
    val totalBankBalance: Double = 0.0,
    val totalCreditAvailable: Double = 0.0,
    // Dialogs
    val showAddSheet: Boolean = false,   // unified Add Account bottom sheet
    val showAccountDialog: Boolean = false,
    val showModeDialog: Boolean = false,
    val showCreditCardDialog: Boolean = false,
    val editingAccount: BankAccount? = null,
    val editingMode: PaymentMode? = null,
    val editingCreditCard: CreditCard? = null,
    val preselectedAccountId: Long? = null,
    // Full-screen detail
    val selectedDetailMode: PaymentMode? = null,
    val selectedDetailAccount: BankAccount? = null,
    val selectedDetailCard: CreditCard? = null,
    val detailTransactions: List<Transaction> = emptyList(),
    val detailBalance: Double = 0.0,
    val detailLinkedModes: List<PaymentMode> = emptyList(),
    val showEditAccountSheet: Boolean = false,
    val currencySymbol: String = "₹"
)

@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val bankAccountRepository: BankAccountRepository,
    private val paymentModeRepository: PaymentModeRepository,
    private val creditCardRepository: CreditCardRepository,
    private val transactionRepository: TransactionRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val authManager: AuthManager
) : ViewModel() {

    private val userId get() = authManager.userId
    private val _uiState = MutableStateFlow(AccountsUiState())
    val uiState: StateFlow<AccountsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            bankAccountRepository.getAllAccounts(userId).collect { accounts ->
                _uiState.update {
                    it.copy(
                        accounts = accounts,
                        totalBankBalance = accounts.sumOf { a -> a.balance }
                    )
                }
            }
        }
        viewModelScope.launch {
            paymentModeRepository.getAllModes(userId).collect { modes ->
                _uiState.update { it.copy(allModes = modes) }
            }
        }
        viewModelScope.launch {
            creditCardRepository.getAllCards(userId).collect { cards ->
                _uiState.update {
                    it.copy(
                        creditCards = cards,
                        totalCreditAvailable = cards.sumOf { c -> c.availableLimit }
                    )
                }
            }
        }
        viewModelScope.launch {
            userPreferencesRepository.currencySymbol.collect { sym ->
                _uiState.update { it.copy(currencySymbol = sym) }
            }
        }
        // Eagerly compute balance per standalone mode
        viewModelScope.launch {
            transactionRepository.getAllTransactions(userId).collect { txns ->
                val standalone = _uiState.value.allModes.filter { it.bankAccountId == null }
                val balances = standalone.associate { mode ->
                    val income =
                        txns.filter { it.paymentModeId == mode.id && it.type == TransactionType.INCOME }
                            .sumOf { it.amount }
                    val expense =
                        txns.filter { it.paymentModeId == mode.id && it.type == TransactionType.EXPENSE }
                            .sumOf { it.amount }
                    mode.id to (income - expense)
                }
                _uiState.update { it.copy(modeBalances = balances) }
            }
        }
    }

    // ── Detail screens ────────────────────────────────────────────────────────

    fun openModeDetail(mode: PaymentMode) {
        viewModelScope.launch {
            val txns = withContext(Dispatchers.IO) {
                transactionRepository.getAllTransactionsOneShot(userId)
                    .filter { it.paymentModeId == mode.id }
                    .sortedByDescending { it.dateTime }
            }
            val income = txns.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val expense = txns.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            _uiState.update {
                it.copy(
                    selectedDetailMode = mode,
                    detailTransactions = txns,
                    detailBalance = income - expense
                )
            }
        }
    }

    fun openAccountDetail(account: BankAccount) {
        viewModelScope.launch {
            // Collect all payment mode ids linked to this account
            val modeIds = _uiState.value.allModes
                .filter { it.bankAccountId == account.id }
                .map { it.id }.toSet()
            val txns = withContext(Dispatchers.IO) {
                transactionRepository.getAllTransactionsOneShot(userId)
                    .filter { it.paymentModeId != null && it.paymentModeId in modeIds }
                    .sortedByDescending { it.dateTime }
            }
            val linkedModes = _uiState.value.allModes.filter { it.bankAccountId == account.id }
            _uiState.update {
                it.copy(
                    selectedDetailAccount = account,
                    detailTransactions = txns,
                    detailBalance = account.balance,
                    detailLinkedModes = linkedModes
                )
            }
        }
    }

    fun openCardDetail(card: CreditCard) {
        viewModelScope.launch {
            val txns = withContext(Dispatchers.IO) {
                transactionRepository.getAllTransactionsOneShot(userId)
                    .filter { it.creditCardId == card.id }
                    .sortedByDescending { it.dateTime }
            }
            _uiState.update {
                it.copy(
                    selectedDetailCard = card,
                    detailTransactions = txns,
                    detailBalance = card.availableLimit
                )
            }
        }
    }

    fun closeDetail() = _uiState.update {
        it.copy(
            selectedDetailMode = null,
            selectedDetailAccount = null,
            selectedDetailCard = null,
            detailTransactions = emptyList(),
            detailBalance = 0.0,
            detailLinkedModes = emptyList(),
            showEditAccountSheet = false
        )
    }

    fun openEditAccountSheet() = _uiState.update { it.copy(showEditAccountSheet = true) }
    fun closeEditAccountSheet() = _uiState.update { it.copy(showEditAccountSheet = false) }

    fun saveEditedAccount(account: BankAccount, name: String, balance: Double) {
        viewModelScope.launch {
            bankAccountRepository.updateAccount(account.copy(name = name, balance = balance))
            // Refresh linked modes for new detail state
            val linkedModes = _uiState.value.allModes.filter { it.bankAccountId == account.id }
            _uiState.update {
                it.copy(
                    showEditAccountSheet = false,
                    detailBalance = balance,
                    selectedDetailAccount = it.selectedDetailAccount?.copy(
                        name = name,
                        balance = balance
                    ),
                    detailLinkedModes = linkedModes
                )
            }
        }
    }

    fun deleteModeFromDetail(mode: PaymentMode) {
        viewModelScope.launch {
            paymentModeRepository.deleteMode(mode)
            _uiState.update { state ->
                state.copy(detailLinkedModes = state.detailLinkedModes.filter { it.id != mode.id })
            }
        }
    }

    fun addModeToAccount(accountId: Long, type: PaymentModeType, identifier: String) {
        viewModelScope.launch {
            paymentModeRepository.insertMode(
                PaymentMode(
                    bankAccountId = accountId,
                    type = type,
                    identifier = identifier,
                    userId = userId
                )
            )
            val linkedModes = _uiState.value.allModes.filter { it.bankAccountId == accountId }
            _uiState.update { it.copy(detailLinkedModes = linkedModes) }
        }
    }

    // ── Bank account actions ──────────────────────────────────────────────────

    fun openAddSheet() =
        _uiState.update { it.copy(showAddSheet = true) }

    fun closeAddSheet() =
        _uiState.update { it.copy(showAddSheet = false) }

    /** Saves a bank account then atomically inserts linked payment modes. */
    fun saveAccountWithModes(
        name: String, balance: Double, colorHex: String,
        pendingModes: List<Pair<PaymentModeType, String>>   // type to identifier
    ) {
        viewModelScope.launch {
            val accountId = bankAccountRepository.insertAccount(
                BankAccount(name = name, balance = balance, colorHex = colorHex, userId = userId)
            )
            pendingModes.forEach { (type, identifier) ->
                paymentModeRepository.insertMode(
                    PaymentMode(
                        bankAccountId = accountId, type = type,
                        identifier = identifier, userId = userId
                    )
                )
            }
            closeAddSheet()
        }
    }

    fun showAccountDialog(account: BankAccount? = null) =
        _uiState.update { it.copy(showAccountDialog = true, editingAccount = account) }

    fun hideAccountDialog() =
        _uiState.update { it.copy(showAccountDialog = false, editingAccount = null) }

    fun saveAccount(name: String, balance: Double, colorHex: String) {
        viewModelScope.launch {
            val editing = _uiState.value.editingAccount
            val account = BankAccount(
                id = editing?.id ?: 0,
                name = name,
                balance = balance,
                colorHex = colorHex,
                userId = userId
            )
            if (editing != null) bankAccountRepository.updateAccount(account)
            else bankAccountRepository.insertAccount(account)
            hideAccountDialog()
        }
    }

    fun deleteAccount(account: BankAccount) =
        viewModelScope.launch { bankAccountRepository.deleteAccount(account) }

    // ── Payment mode actions ──────────────────────────────────────────────────

    fun showModeDialog(mode: PaymentMode? = null, forAccountId: Long? = null) =
        _uiState.update {
            it.copy(
                showModeDialog = true, editingMode = mode,
                preselectedAccountId = forAccountId ?: mode?.bankAccountId
            )
        }

    fun hideModeDialog() =
        _uiState.update {
            it.copy(
                showModeDialog = false,
                editingMode = null,
                preselectedAccountId = null
            )
        }

    fun saveMode(bankAccountId: Long?, type: PaymentModeType, identifier: String) {
        viewModelScope.launch {
            val editing = _uiState.value.editingMode
            val mode = PaymentMode(
                id = editing?.id ?: 0, bankAccountId = bankAccountId,
                type = type, identifier = identifier, userId = userId
            )
            if (editing != null) paymentModeRepository.updateMode(mode)
            else paymentModeRepository.insertMode(mode)
            hideModeDialog()
        }
    }

    fun deleteMode(mode: PaymentMode) =
        viewModelScope.launch { paymentModeRepository.deleteMode(mode) }

    fun modesForAccount(accountId: Long) =
        _uiState.value.allModes.filter { it.bankAccountId == accountId }

    fun standaloneModes() =
        _uiState.value.allModes.filter { it.bankAccountId == null }

    // ── Credit card actions ───────────────────────────────────────────────────

    fun showCreditCardDialog(card: CreditCard? = null) =
        _uiState.update { it.copy(showCreditCardDialog = true, editingCreditCard = card) }

    fun hideCreditCardDialog() =
        _uiState.update { it.copy(showCreditCardDialog = false, editingCreditCard = null) }

    fun saveCreditCard(
        name: String, availableLimit: Double, totalLimit: Double,
        billingCycleDate: Int, paymentDueDate: Int, colorHex: String
    ) {
        viewModelScope.launch {
            val editing = _uiState.value.editingCreditCard
            val card = CreditCard(
                id = editing?.id ?: 0, name = name,
                availableLimit = availableLimit, totalLimit = totalLimit,
                billingCycleDate = billingCycleDate, paymentDueDate = paymentDueDate,
                colorHex = colorHex, userId = userId
            )
            if (editing != null) creditCardRepository.updateCard(card)
            else creditCardRepository.insertCard(card)
            hideCreditCardDialog()
        }
    }

    fun deleteCreditCard(card: CreditCard) =
        viewModelScope.launch { creditCardRepository.deleteCard(card) }
}

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
            Box(Modifier
                .fillMaxSize()
                .padding(padding), contentAlignment = Alignment.Center) {
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
            onSaveBank = { name, balance, colorHex, modes ->
                viewModel.saveAccountWithModes(name, balance, colorHex, modes)
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
            currencySymbol = sym,
            isCard = uiState.selectedDetailCard != null,
            account = uiState.selectedDetailAccount,
            linkedModes = uiState.detailLinkedModes,
            showEditSheet = uiState.showEditAccountSheet,
            onNavigateBack = viewModel::closeDetail,
            onEditAccount = viewModel::openEditAccountSheet,
            onCloseEditSheet = viewModel::closeEditAccountSheet,
            onSaveEditAccount = { acc, name, bal ->
                viewModel.saveEditedAccount(acc, name, bal)
            },
            onDeleteAccount = { acc ->
                viewModel.deleteAccount(acc)
                viewModel.closeDetail()
            },
            onLinkMode = { accId, type, id ->
                viewModel.addModeToAccount(accId, type, id)
            },
            onDeleteMode = viewModel::deleteModeFromDetail
        )
    }
}

// ─── Reusable layout helpers ──────────────────────────────────────────────────

/** Two-column summary tile at the top of the screen */
@Composable
private fun SummaryTile(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
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
    currencySymbol: String,
    isCard: Boolean,
    account: BankAccount?,
    linkedModes: List<PaymentMode>,
    showEditSheet: Boolean,
    onNavigateBack: () -> Unit,
    onEditAccount: () -> Unit,
    onCloseEditSheet: () -> Unit,
    onSaveEditAccount: (BankAccount, String, Double) -> Unit,
    onDeleteAccount: (BankAccount) -> Unit,
    onLinkMode: (Long, PaymentModeType, String) -> Unit,
    onDeleteMode: (PaymentMode) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    val filteredTxns = when (selectedTab) {
        1 -> transactions.filter { it.type == TransactionType.INCOME }
        2 -> transactions.filter { it.type == TransactionType.EXPENSE }
        else -> transactions
    }

    val dateFormatter = DateTimeFormatter.ofPattern("dd MMM")
    val balanceLabel = if (isCard) "Available Limit" else "Available Balance"

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
                    if (account != null) {
                        // Edit account button
                        IconButton(
                            onClick = onEditAccount,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .size(40.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit, contentDescription = "Edit",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        // Add transaction shortcut
                        IconButton(
                            onClick = { /* could navigate to add transaction */ },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .size(40.dp)
                        ) {
                            Icon(
                                Icons.Default.Add, contentDescription = "Add",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier
            .fillMaxSize()
            .padding(padding)) {

            // ── Enhanced balance card ─────────────────────────────────────────
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        balanceLabel,
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
                    // Always show "Incorrect balance? Edit" for bank accounts
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

                        // Linked payment modes row
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
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Add, null, Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Link", style = MaterialTheme.typography.labelMedium)
                            }
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

            // ── Tabs ─────────────────────────────────────────────────────────
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                listOf("All", "Credit", "Debit").forEachIndexed { idx, label ->
                    Tab(
                        selected = selectedTab == idx,
                        onClick = { selectedTab = idx },
                        text = {
                            Text(
                                label, fontWeight = if (selectedTab == idx)
                                    FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            if (filteredTxns.isEmpty()) {
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
                        DetailTransactionRow(txn, dateFormatter, currencySymbol)
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

    // ── Edit Account bottom sheet ─────────────────────────────────────────────
    if (showEditSheet && account != null) {
        EditAccountSheet(
            account = account,
            linkedModes = linkedModes,
            onDismiss = onCloseEditSheet,
            onSave = { name, bal -> onSaveEditAccount(account, name, bal) },
            onDelete = { onDeleteAccount(account) },
            onAddMode = { type, id -> onLinkMode(account.id, type, id) },
            onDeleteMode = onDeleteMode
        )
    }
}

// ─── Edit Account Bottom Sheet ────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditAccountSheet(
    account: BankAccount,
    linkedModes: List<PaymentMode>,
    onDismiss: () -> Unit,
    onSave: (name: String, balance: Double) -> Unit,
    onDelete: () -> Unit,
    onAddMode: (PaymentModeType, String) -> Unit,
    onDeleteMode: (PaymentMode) -> Unit
) {
    var editName by remember(account) { mutableStateOf(account.name) }
    var editBalance by remember(account) { mutableStateOf(account.balance.toString()) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showModeAddRow by remember { mutableStateOf(false) }
    var newModeType by remember { mutableStateOf(PaymentModeType.DEBIT_CARD) }
    var newModeId by remember { mutableStateOf("") }

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
                linkedModes.forEach { mode ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Mode icon
                        val modeIcon = when (mode.type) {
                            PaymentModeType.DEBIT_CARD -> Icons.Default.CreditCard
                            PaymentModeType.UPI -> Icons.Default.PhoneAndroid
                            PaymentModeType.NET_BANKING -> Icons.Default.Language
                            PaymentModeType.CHEQUE -> Icons.Default.EditNote
                            else -> Icons.Default.Payment
                        }
                        Box(
                            Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                modeIcon, null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
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
                        // Edit mode button
                        IconButton(
                            onClick = { /* future: edit mode inline */ },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(
                                Icons.Default.Edit, null,
                                Modifier.size(15.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        // Delete mode button
                        IconButton(
                            onClick = { onDeleteMode(mode) },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(
                                Icons.Default.Delete, null,
                                Modifier.size(15.dp),
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
                                onAddMode(newModeType, newModeId.trim())
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
                        if (editName.isNotBlank()) onSave(editName, bal)
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
            text = {
                Text("Delete ${account.name} and all its linked payment modes? This cannot be undone.")
            },
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


@Composable
private fun DetailTransactionRow(
    txn: Transaction,
    dateFormatter: DateTimeFormatter,
    currencySymbol: String
) {
    val isExpense = txn.type == TransactionType.EXPENSE
    val amountColor = if (isExpense) ExpenseRed else IncomeGreen
    val prefix = if (isExpense) "-" else "+"
    val title = txn.note.ifEmpty { txn.categoryName.ifEmpty { "Transaction" } }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            txn.dateTime.format(dateFormatter),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(54.dp)
        )
        Text(
            title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
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
        name: String, balance: Double, colorHex: String,
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
                                    "You can add a Debit Card, UPI or other payment modes to use with this bank account.",
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
                                    Icon(
                                        when (type) {
                                            PaymentModeType.DEBIT_CARD -> Icons.Default.CreditCard
                                            PaymentModeType.UPI -> Icons.Default.PhoneAndroid
                                            PaymentModeType.NET_BANKING -> Icons.Default.Language
                                            PaymentModeType.CHEQUE -> Icons.Default.EditNote
                                            else -> Icons.Default.Payment
                                        }, null, Modifier.size(18.dp),
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

                        Spacer(Modifier.height(88.dp))  // room for Save button
                    }

                    // Save button pinned to bottom
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Button(
                            onClick = {
                                val bal = bankBalance.toDoubleOrNull() ?: 0.0
                                if (bankName.isNotBlank())
                                    onSaveBank(bankName, bal, "#6750A4", pendingModes)
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
                        val c = Color(android.graphics.Color.parseColor(hex))
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
                        val c = Color(android.graphics.Color.parseColor(hex))
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
                                    Color(android.graphics.Color.parseColor(account.colorHex))
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