package com.expensetracker.presentation.ui.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.data.repository.AuthManager
import com.expensetracker.data.repository.BalanceAdjustmentRepository
import com.expensetracker.data.repository.BankAccountRepository
import com.expensetracker.data.repository.CreditCardRepository
import com.expensetracker.data.repository.PaymentModeRepository
import com.expensetracker.data.repository.TransactionRepository
import com.expensetracker.data.repository.UserPreferencesRepository
import com.expensetracker.domain.model.BalanceAdjustment
import com.expensetracker.domain.model.BankAccount
import com.expensetracker.domain.model.CreditCard
import com.expensetracker.domain.model.PaymentMode
import com.expensetracker.domain.model.PaymentModeType
import com.expensetracker.domain.model.Transaction
import com.expensetracker.domain.model.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

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
    val detailAdjustments: List<BalanceAdjustment> = emptyList(),
    val detailBalance: Double = 0.0,
    val detailLinkedModes: List<PaymentMode> = emptyList(),
    val showEditAccountSheet: Boolean = false,
    val showEditModeBalanceSheet: Boolean = false,
    val showCardTransactions: Boolean = false,
    val cardTransactionsIsCurrentCycle: Boolean = false,
    val showEditCardSheet: Boolean = false,   // full card edit dialog
    val showEditLimitSheet: Boolean = false,   // quick edit available limit
    val currencySymbol: String = "₹"
)

@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val bankAccountRepository: BankAccountRepository,
    private val balanceAdjustmentRepository: BalanceAdjustmentRepository,
    private val paymentModeRepository: PaymentModeRepository,
    private val creditCardRepository: CreditCardRepository,
    private val transactionRepository: TransactionRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val authManager: AuthManager
) : ViewModel() {

    private val userId get() = authManager.userId
    private val _uiState = MutableStateFlow(AccountsUiState())
    val uiState: StateFlow<AccountsUiState> = _uiState.asStateFlow()
    private var detailAdjustmentsJob: Job? = null

    init {
        viewModelScope.launch {
            bankAccountRepository.getAllAccounts(userId).collect { accounts ->
                _uiState.update {
                    it.copy(
                        accounts = accounts, totalBankBalance = accounts.sumOf { a -> a.balance })
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
                        totalCreditAvailable = cards.sumOf { c -> c.availableLimit })
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
            combine(
                transactionRepository.getAllTransactions(userId),
                balanceAdjustmentRepository.getAllAdjustments(userId)
            ) { txns, adjustments ->
                val standalone = _uiState.value.allModes.filter { it.bankAccountId == null }
                standalone.associate { mode ->
                    val income =
                        txns.filter { it.paymentModeId == mode.id && it.type == TransactionType.INCOME }
                            .sumOf { it.amount }
                    val expense =
                        txns.filter { it.paymentModeId == mode.id && it.type == TransactionType.EXPENSE }
                            .sumOf { it.amount }
                    val manualAdjustments = adjustments
                        .filter { it.paymentModeId == mode.id }
                        .sumOf { it.amountDelta }
                    mode.id to (income - expense + manualAdjustments)
                }
            }.collect { balances ->
                _uiState.update { it.copy(modeBalances = balances) }
            }
        }
    }

    // ── Detail screens ────────────────────────────────────────────────────────

    private fun observeAccountAdjustments(accountId: Long) {
        detailAdjustmentsJob?.cancel()
        detailAdjustmentsJob = viewModelScope.launch {
            balanceAdjustmentRepository.getAdjustmentsForAccount(accountId, userId).collect { adjustments ->
                _uiState.update { it.copy(detailAdjustments = adjustments) }
            }
        }
    }

    private fun observeCardAdjustments(cardId: Long) {
        detailAdjustmentsJob?.cancel()
        detailAdjustmentsJob = viewModelScope.launch {
            balanceAdjustmentRepository.getAdjustmentsForCreditCard(cardId, userId).collect { adjustments ->
                _uiState.update { it.copy(detailAdjustments = adjustments) }
            }
        }
    }

    private fun observeModeAdjustments(modeId: Long) {
        detailAdjustmentsJob?.cancel()
        detailAdjustmentsJob = viewModelScope.launch {
            balanceAdjustmentRepository.getAdjustmentsForPaymentMode(modeId, userId).collect { adjustments ->
                _uiState.update { state ->
                    val transactionNet = state.detailTransactions.sumOf { txn ->
                        when (txn.type) {
                            TransactionType.INCOME -> txn.amount
                            TransactionType.EXPENSE -> -txn.amount
                            TransactionType.TRANSFER -> 0.0
                        }
                    }
                    state.copy(
                        detailAdjustments = adjustments,
                        detailBalance = if (state.selectedDetailMode?.id == modeId) {
                            transactionNet + adjustments.sumOf { it.amountDelta }
                        } else {
                            state.detailBalance
                        }
                    )
                }
            }
        }
    }

    fun openModeDetail(mode: PaymentMode) {
        observeModeAdjustments(mode.id)
        viewModelScope.launch {
            val txns = withContext(Dispatchers.IO) {
                transactionRepository.getAllTransactionsOneShot(userId)
                    .filter { it.paymentModeId == mode.id }.sortedByDescending { it.dateTime }
            }
            val adjustments = balanceAdjustmentRepository
                .getAdjustmentsForPaymentMode(mode.id, userId)
                .first()
            val income = txns.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val expense = txns.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            _uiState.update {
                it.copy(
                    selectedDetailMode = mode,
                    detailTransactions = txns,
                    detailAdjustments = adjustments,
                    detailBalance = income - expense + adjustments.sumOf { it.amountDelta }
                )
            }
        }
    }

    fun openAccountDetail(account: BankAccount) {
        observeAccountAdjustments(account.id)
        viewModelScope.launch {
            // Collect all payment mode ids linked to this account
            val modeIds =
                _uiState.value.allModes.filter { it.bankAccountId == account.id }.map { it.id }
                    .toSet()
            val txns = withContext(Dispatchers.IO) {
                transactionRepository.getAllTransactionsOneShot(userId)
                    .filter { it.paymentModeId != null && it.paymentModeId in modeIds }
                    .sortedByDescending { it.dateTime }
            }
            val adjustments = balanceAdjustmentRepository
                .getAdjustmentsForAccount(account.id, userId)
                .first()
            val linkedModes = _uiState.value.allModes.filter { it.bankAccountId == account.id }
            _uiState.update {
                it.copy(
                    selectedDetailAccount = account,
                    detailTransactions = txns,
                    detailAdjustments = adjustments,
                    detailBalance = account.balance,
                    detailLinkedModes = linkedModes
                )
            }
        }
    }

    fun openCardDetail(card: CreditCard) {
        observeCardAdjustments(card.id)
        viewModelScope.launch {
            val txns = withContext(Dispatchers.IO) {
                transactionRepository.getAllTransactionsOneShot(userId)
                    .filter { it.creditCardId == card.id }.sortedByDescending { it.dateTime }
            }
            val adjustments = balanceAdjustmentRepository
                .getAdjustmentsForCreditCard(card.id, userId)
                .first()
            _uiState.update {
                it.copy(
                    selectedDetailCard = card,
                    detailTransactions = txns,
                    detailAdjustments = adjustments,
                    detailBalance = card.availableLimit
                )
            }
        }
    }

    fun closeDetail() = _uiState.update {
        detailAdjustmentsJob?.cancel()
        detailAdjustmentsJob = null
        it.copy(
            selectedDetailMode = null,
            selectedDetailAccount = null,
            selectedDetailCard = null,
            detailTransactions = emptyList(),
            detailAdjustments = emptyList(),
            detailBalance = 0.0,
            detailLinkedModes = emptyList(),
            showEditAccountSheet = false,
            showEditModeBalanceSheet = false,
            showCardTransactions = false,
            showEditCardSheet = false,
            showEditLimitSheet = false
        )
    }

    fun openCardTransactions(isCurrentCycle: Boolean = false) =
        _uiState.update {
            it.copy(
                showCardTransactions = true,
                cardTransactionsIsCurrentCycle = isCurrentCycle
            )
        }

    fun closeCardTransactions() = _uiState.update { it.copy(showCardTransactions = false) }

    fun openEditCardSheet() = _uiState.update { it.copy(showEditCardSheet = true) }

    fun closeEditCardSheet() = _uiState.update { it.copy(showEditCardSheet = false) }

    fun openEditLimitSheet() = _uiState.update { it.copy(showEditLimitSheet = true) }

    fun closeEditLimitSheet() = _uiState.update { it.copy(showEditLimitSheet = false) }

    fun saveCardAvailableLimit(card: CreditCard, newLimit: Double) {
        viewModelScope.launch {
            val delta = newLimit - card.availableLimit
            creditCardRepository.updateCard(card.copy(availableLimit = newLimit))
            if (delta != 0.0) {
                balanceAdjustmentRepository.insertAdjustment(
                    BalanceAdjustment(
                        creditCardId = card.id,
                        previousBalance = card.availableLimit,
                        newBalance = newLimit,
                        amountDelta = delta,
                        adjustedAt = LocalDateTime.now(),
                        userId = userId
                    )
                )
            }
            _uiState.update {
                it.copy(
                    showEditLimitSheet = false,
                    selectedDetailCard = it.selectedDetailCard?.copy(availableLimit = newLimit),
                    detailBalance = newLimit
                )
            }
        }
    }

    fun openEditAccountSheet() = _uiState.update { it.copy(showEditAccountSheet = true) }

    fun closeEditAccountSheet() = _uiState.update { it.copy(showEditAccountSheet = false) }

    fun openEditModeBalanceSheet() = _uiState.update { it.copy(showEditModeBalanceSheet = true) }

    fun closeEditModeBalanceSheet() = _uiState.update { it.copy(showEditModeBalanceSheet = false) }

    fun saveEditedAccount(account: BankAccount, name: String, balance: Double) {
        saveEditedAccount(account, name, balance, _uiState.value.detailLinkedModes)
    }

    fun saveEditedAccount(
        account: BankAccount,
        name: String,
        balance: Double,
        linkedModes: List<PaymentMode>
    ) {
        viewModelScope.launch {
            val updatedAccount = account.copy(name = name, balance = balance)
            val delta = balance - account.balance

            bankAccountRepository.updateAccount(updatedAccount)
            if (delta != 0.0) {
                balanceAdjustmentRepository.insertAdjustment(
                    BalanceAdjustment(
                        bankAccountId = account.id,
                        previousBalance = account.balance,
                        newBalance = balance,
                        amountDelta = delta,
                        adjustedAt = LocalDateTime.now(),
                        userId = userId
                    )
                )
            }
            val existingModes = _uiState.value.allModes.filter { it.bankAccountId == account.id }
            val requestedModeIds = linkedModes.filter { it.id > 0 }.map { it.id }.toSet()

            existingModes
                .filter { it.id !in requestedModeIds }
                .forEach { paymentModeRepository.deleteMode(it) }

            val savedModes = linkedModes.map { mode ->
                val normalized = mode.copy(bankAccountId = account.id, userId = userId)
                if (mode.id > 0) {
                    paymentModeRepository.updateMode(normalized)
                    normalized
                } else {
                    val insertedId = paymentModeRepository.insertMode(normalized.copy(id = 0))
                    normalized.copy(id = insertedId)
                }
            }

            _uiState.update {
                it.copy(
                    showEditAccountSheet = false,
                    detailBalance = balance,
                    selectedDetailAccount = updatedAccount,
                    detailLinkedModes = savedModes
                )
            }
        }
    }

    fun saveEditedModeBalance(mode: PaymentMode, newBalance: Double) {
        viewModelScope.launch {
            val currentBalance = _uiState.value.detailBalance
            val delta = newBalance - currentBalance
            if (delta != 0.0) {
                balanceAdjustmentRepository.insertAdjustment(
                    BalanceAdjustment(
                        paymentModeId = mode.id,
                        previousBalance = currentBalance,
                        newBalance = newBalance,
                        amountDelta = delta,
                        adjustedAt = LocalDateTime.now(),
                        userId = userId
                    )
                )
            }
            _uiState.update {
                it.copy(
                    showEditModeBalanceSheet = false,
                    detailBalance = newBalance
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
                    bankAccountId = accountId, type = type, identifier = identifier, userId = userId
                )
            )
            val linkedModes = _uiState.value.allModes.filter { it.bankAccountId == accountId }
            _uiState.update { it.copy(detailLinkedModes = linkedModes) }
        }
    }

    // ── Bank account actions ──────────────────────────────────────────────────

    fun openAddSheet() = _uiState.update { it.copy(showAddSheet = true) }

    fun closeAddSheet() = _uiState.update { it.copy(showAddSheet = false) }

    /** Saves a bank account then atomically inserts linked payment modes. */
    fun saveAccountWithModes(
        name: String,
        balance: Double,
        colorHex: String,
        accountIdentifier: String,
        pendingModes: List<Pair<PaymentModeType, String>>   // type to identifier
    ) {
        viewModelScope.launch {
            val accountId = bankAccountRepository.insertAccount(
                BankAccount(name = name, balance = balance, colorHex = colorHex, userId = userId)
            )
            val initialModes = buildList {
                add(PaymentModeType.NET_BANKING to accountIdentifier.trim())
                addAll(pendingModes)
            }.distinctBy { (type, identifier) -> type to identifier.trim().lowercase() }

            initialModes.forEach { (type, identifier) ->
                paymentModeRepository.insertMode(
                    PaymentMode(
                        bankAccountId = accountId,
                        type = type,
                        identifier = identifier,
                        userId = userId
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

    fun showModeDialog(mode: PaymentMode? = null, forAccountId: Long? = null) = _uiState.update {
        it.copy(
            showModeDialog = true,
            editingMode = mode,
            preselectedAccountId = forAccountId ?: mode?.bankAccountId
        )
    }

    fun hideModeDialog() = _uiState.update {
        it.copy(
            showModeDialog = false, editingMode = null, preselectedAccountId = null
        )
    }

    fun saveMode(bankAccountId: Long?, type: PaymentModeType, identifier: String) {
        viewModelScope.launch {
            val editing = _uiState.value.editingMode
            val mode = PaymentMode(
                id = editing?.id ?: 0,
                bankAccountId = bankAccountId,
                type = type,
                identifier = identifier,
                userId = userId
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

    fun standaloneModes() = _uiState.value.allModes.filter { it.bankAccountId == null }

    // ── Credit card actions ───────────────────────────────────────────────────

    fun showCreditCardDialog(card: CreditCard? = null) =
        _uiState.update { it.copy(showCreditCardDialog = true, editingCreditCard = card) }

    fun hideCreditCardDialog() =
        _uiState.update { it.copy(showCreditCardDialog = false, editingCreditCard = null) }

    fun saveCreditCard(
        name: String,
        availableLimit: Double,
        totalLimit: Double,
        billingCycleDate: Int,
        paymentDueDate: Int,
        colorHex: String
    ) {
        viewModelScope.launch {
            val editing = _uiState.value.editingCreditCard
            val card = CreditCard(
                id = editing?.id ?: 0,
                name = name,
                availableLimit = availableLimit,
                totalLimit = totalLimit,
                billingCycleDate = billingCycleDate,
                paymentDueDate = paymentDueDate,
                colorHex = colorHex,
                userId = userId
            )
            if (editing != null) creditCardRepository.updateCard(card)
            else creditCardRepository.insertCard(card)
            hideCreditCardDialog()
        }
    }

    fun deleteCreditCard(card: CreditCard) =
        viewModelScope.launch { creditCardRepository.deleteCard(card) }

    /**
     * Updates an existing credit card in the detail view (pencil button).
     * Always calls updateCard — never inserts — because the card object comes
     * directly from the detail state and always has a valid id.
     */
    fun saveEditedCard(
        card: CreditCard,
        name: String,
        availableLimit: Double,
        totalLimit: Double,
        billingCycleDate: Int,
        paymentDueDate: Int,
        colorHex: String
    ) {
        viewModelScope.launch {
            val delta = availableLimit - card.availableLimit
            val updated = card.copy(
                name = name,
                availableLimit = availableLimit,
                totalLimit = totalLimit,
                billingCycleDate = billingCycleDate,
                paymentDueDate = paymentDueDate,
                colorHex = colorHex
            )
            creditCardRepository.updateCard(updated)
            if (delta != 0.0) {
                balanceAdjustmentRepository.insertAdjustment(
                    BalanceAdjustment(
                        creditCardId = card.id,
                        previousBalance = card.availableLimit,
                        newBalance = availableLimit,
                        amountDelta = delta,
                        adjustedAt = LocalDateTime.now(),
                        userId = userId
                    )
                )
            }
            // Refresh the detail state so the header card reflects the new values immediately
            _uiState.update {
                it.copy(
                    selectedDetailCard = updated,
                    detailBalance = updated.availableLimit
                )
            }
        }
    }

    /**
     * Calculates the start and end epoch milliseconds for a billing cycle.
     * Example: Today is 29 Mar, cycleDate is 14.
     * If isCurrentCycle = true -> Returns 14 Mar to 13 Apr
     * If isCurrentCycle = false -> Returns 14 Feb to 13 Mar
     */
    fun getBillingCycleDates(billingCycleDate: Int, isCurrentCycle: Boolean): Pair<Long, Long> {
        val today = LocalDate.now()

        // Determine the start of the *current* cycle
        val currentCycleStart = if (today.dayOfMonth >= billingCycleDate) {
            today.withDayOfMonth(billingCycleDate)
        } else {
            today.minusMonths(1).withDayOfMonth(billingCycleDate)
        }

        // Shift back a month if we want the previous cycle
        val targetCycleStart = if (isCurrentCycle) {
            currentCycleStart
        } else {
            currentCycleStart.minusMonths(1)
        }

        // End date is exactly one month from start, minus 1 day
        val targetCycleEnd = targetCycleStart.plusMonths(1).minusDays(1)

        val startMillis =
            targetCycleStart.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMillis = targetCycleEnd.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant()
            .toEpochMilli()

        return Pair(startMillis, endMillis)
    }
}
