package com.expensetracker.presentation.ui.wealth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.data.repository.AuthManager
import com.expensetracker.data.repository.WealthRepository
import com.expensetracker.domain.model.InvestmentSnapshot
import com.expensetracker.domain.model.InvestmentType
import com.expensetracker.domain.model.SavingsSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── Savings history state ─────────────────────────────────────────────────────

data class SavingsHistoryUiState(
    val institutionName: String = "",
    val snapshots: List<SavingsSnapshot> = emptyList(),
    val isLoading: Boolean = true
)

// ── Investment history state ──────────────────────────────────────────────────

data class InvestmentHistoryUiState(
    val type: InvestmentType = InvestmentType.INDIAN_MUTUAL_FUND,
    val subName: String = "",
    val snapshots: List<InvestmentSnapshot> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class WealthHistoryViewModel @Inject constructor(
    private val wealthRepository: WealthRepository,
    private val authManager: AuthManager
) : ViewModel() {

    private val userId get() = authManager.userId

    private val _savingsHistory = MutableStateFlow(SavingsHistoryUiState())
    val savingsHistory: StateFlow<SavingsHistoryUiState> = _savingsHistory.asStateFlow()

    private val _investmentHistory = MutableStateFlow(InvestmentHistoryUiState())
    val investmentHistory: StateFlow<InvestmentHistoryUiState> = _investmentHistory.asStateFlow()

    fun loadSavingsHistory(institutionName: String) {
        _savingsHistory.update { it.copy(institutionName = institutionName, isLoading = true) }
        viewModelScope.launch {
            wealthRepository.getSavingsHistory(userId, institutionName).collect { snapshots ->
                _savingsHistory.update {
                    it.copy(snapshots = snapshots, isLoading = false)
                }
            }
        }
    }

    fun loadInvestmentHistory(type: InvestmentType, subName: String) {
        _investmentHistory.update { it.copy(type = type, subName = subName, isLoading = true) }
        viewModelScope.launch {
            wealthRepository.getInvestmentHistory(userId, type, subName).collect { snapshots ->
                _investmentHistory.update {
                    it.copy(snapshots = snapshots, isLoading = false)
                }
            }
        }
    }
}