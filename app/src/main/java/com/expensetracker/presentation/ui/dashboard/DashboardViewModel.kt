package com.expensetracker.presentation.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.data.repository.*
import com.expensetracker.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.YearMonth
import javax.inject.Inject

enum class SummaryPeriod(val label: String) {
    THIS_MONTH("This Month"),
    THIS_YEAR("This Year"),
    ALL_TIME("All Time")
}

data class DashboardUiState(
    val recentTransactions: List<Transaction> = emptyList(),
    val summary: MonthlySummary = MonthlySummary(0.0, 0.0, 0.0, ""),
    val selectedPeriod: SummaryPeriod = SummaryPeriod.THIS_MONTH,
    val budgetProgress: BudgetProgress? = null,
    val isLoading: Boolean = false,
    val userName: String = "",
    val userEmail: String = ""
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
    private val authManager: AuthManager
) : ViewModel() {

    private val _selectedPeriod = MutableStateFlow(SummaryPeriod.THIS_MONTH)
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val userId get() = authManager.userId

    init {
        loadDashboard()
        observeUserInfo()
    }

    private fun observeUserInfo() {
        viewModelScope.launch {
            authManager.currentUser.collect { user ->
                _uiState.update {
                    it.copy(
                        userName = user?.displayName ?: "User",
                        userEmail = user?.email ?: ""
                    )
                }
            }
        }
    }

    private fun loadDashboard() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Recent transactions
            transactionRepository.getRecentTransactions(userId, 5).collect { txns ->
                _uiState.update { it.copy(recentTransactions = txns) }
            }
        }
        viewModelScope.launch {
            _selectedPeriod.collect { period ->
                refreshSummary(period)
            }
        }
    }

    private suspend fun refreshSummary(period: SummaryPeriod) {
        val (start, end, label) = when (period) {
            SummaryPeriod.THIS_MONTH -> {
                val ym = YearMonth.now()
                Triple(
                    ym.atDay(1).atStartOfDay(),
                    ym.atEndOfMonth().atTime(23, 59, 59),
                    ym.month.name.lowercase().replaceFirstChar { it.uppercase() } + " ${ym.year}"
                )
            }
            SummaryPeriod.THIS_YEAR -> {
                val year = java.time.LocalDate.now().year
                Triple(
                    LocalDateTime.of(year, 1, 1, 0, 0),
                    LocalDateTime.of(year, 12, 31, 23, 59, 59),
                    "Year $year"
                )
            }
            SummaryPeriod.ALL_TIME -> Triple(null, null, "All Time")
        }

        val income = transactionRepository.getTotalByType(userId, TransactionType.INCOME, start, end)
        val expense = transactionRepository.getTotalByType(userId, TransactionType.EXPENSE, start, end)

        // Budget for current month
        val now = YearMonth.now()
        val budget = budgetRepository.getBudgetForPeriod(userId, now.year, now.monthValue)
        val budgetProgress = budget?.let {
            val spent = transactionRepository.getExpenseByCategories(
                userId, it.applicableCategoryIds,
                now.atDay(1).atStartOfDay(),
                now.atEndOfMonth().atTime(23, 59, 59)
            )
            BudgetProgress(it, spent)
        }

        _uiState.update {
            it.copy(
                summary = MonthlySummary(income, expense, income - expense, label),
                selectedPeriod = period,
                budgetProgress = budgetProgress,
                isLoading = false
            )
        }
    }

    fun selectPeriod(period: SummaryPeriod) {
        _selectedPeriod.value = period
    }

    fun logout() {
        viewModelScope.launch { authManager.signOut() }
    }
}