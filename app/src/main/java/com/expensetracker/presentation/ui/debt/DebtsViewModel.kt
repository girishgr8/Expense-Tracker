package com.expensetracker.presentation.ui.debt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.data.repository.AuthManager
import com.expensetracker.data.repository.DebtRepository
import com.expensetracker.domain.model.Debt
import com.expensetracker.domain.model.DebtType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

enum class DebtTabFilter { ALL, LENDING, BORROWING }

data class DebtsUiState(
    val allDebts:      List<Debt> = emptyList(),
    val activeTab:     DebtTabFilter = DebtTabFilter.ALL,
    val isLoading:     Boolean = true
) {
    val visibleDebts: List<Debt> get() = when (activeTab) {
        DebtTabFilter.ALL       -> allDebts
        DebtTabFilter.LENDING   -> allDebts.filter { it.type == DebtType.LENDING }
        DebtTabFilter.BORROWING -> allDebts.filter { it.type == DebtType.BORROWING }
    }
    val totalLending:   Double get() = allDebts.filter { it.type == DebtType.LENDING && !it.isSettled }
        .sumOf { it.remainingAmount }
    val totalBorrowing: Double get() = allDebts.filter { it.type == DebtType.BORROWING && !it.isSettled }
        .sumOf { it.remainingAmount }
}

// UiState for Add / Edit Debt form
data class AddDebtUiState(
    val personName: String   = "",
    val amount:     String   = "",
    val dueDate:    LocalDate? = null,
    val note:       String   = "",
    val type:       DebtType  = DebtType.LENDING,
    val isSaved:    Boolean  = false,
    val error:      String?  = null
)

@HiltViewModel
class DebtsViewModel @Inject constructor(
    private val debtRepository: DebtRepository,
    private val authManager:    AuthManager
) : ViewModel() {

    private val userId get() = authManager.userId

    private val _uiState = MutableStateFlow(DebtsUiState())
    val uiState: StateFlow<DebtsUiState> = _uiState.asStateFlow()

    private val _addState = MutableStateFlow(AddDebtUiState())
    val addState: StateFlow<AddDebtUiState> = _addState.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            debtRepository.getAllDebts(userId).collect { debts ->
                _uiState.update { it.copy(allDebts = debts, isLoading = false) }
            }
        }
    }

    fun setTab(tab: DebtTabFilter) = _uiState.update { it.copy(activeTab = tab) }

    // ── Add form ──────────────────────────────────────────────────────────────
    fun initAddForm(type: DebtType) {
        _addState.value = AddDebtUiState(type = type)
    }

    fun setPersonName(v: String)  = _addState.update { it.copy(personName = v) }
    fun setAmount(v: String)      = _addState.update { it.copy(amount = v) }
    fun setDueDate(v: LocalDate?) = _addState.update { it.copy(dueDate = v) }
    fun setNote(v: String)        = _addState.update { it.copy(note = v) }
    fun setDebtType(v: DebtType)  = _addState.update { it.copy(type = v) }
    fun clearError()              = _addState.update { it.copy(error = null) }

    fun saveDebt() {
        val s = _addState.value
        val amt = s.amount.toDoubleOrNull()
        if (s.personName.isBlank()) {
            _addState.update { it.copy(error = "Name is required") }; return
        }
        if (amt == null || amt <= 0) {
            _addState.update { it.copy(error = "Enter a valid amount") }; return
        }
        viewModelScope.launch {
            debtRepository.insertDebt(
                Debt(
                    type       = s.type,
                    personName = s.personName.trim(),
                    amount     = amt,
                    dueDate    = s.dueDate,
                    note       = s.note.trim(),
                    createdAt  = LocalDateTime.now(),
                    userId     = userId
                )
            )
            _addState.update { it.copy(isSaved = true) }
        }
    }

    fun deleteDebt(debt: Debt) {
        viewModelScope.launch { debtRepository.deleteDebt(debt) }
    }

    fun settleDebt(debt: Debt) {
        viewModelScope.launch {
            debtRepository.updateDebt(debt.copy(
                paidAmount = debt.amount,
                isSettled  = true
            ))
        }
    }
}