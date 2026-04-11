package com.expensetracker.presentation.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.data.repository.AuthManager
import com.expensetracker.data.repository.BudgetRepository
import com.expensetracker.data.repository.TransactionRepository
import com.expensetracker.domain.model.BudgetPeriod
import com.expensetracker.domain.model.BudgetProgress
import com.expensetracker.domain.model.MonthlySummary
import com.expensetracker.domain.model.Transaction
import com.expensetracker.domain.model.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
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
    val monthlyBudgetProgress: BudgetProgress? = null,
    val annualBudgetProgress: BudgetProgress? = null,
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
    private val _uiState = MutableStateFlow(DashboardUiState(isLoading = true))
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val userId get() = authManager.userId

    init {
        observeUserInfo()
        loadDashboard()
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

    /**
     * Creates a Flow for BudgetProgress that updates when the budget OR transactions change
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeBudgetProgress(period: BudgetPeriod): Flow<BudgetProgress?> {
        val now = YearMonth.now()
        val start = when (period) {
            BudgetPeriod.MONTHLY -> now.atDay(1).atStartOfDay()
            BudgetPeriod.YEARLY -> now.atDay(1).withDayOfYear(1).atStartOfDay()
        }
        val end = when (period) {
            BudgetPeriod.MONTHLY -> now.atEndOfMonth().atTime(23, 59, 59)
            BudgetPeriod.YEARLY -> now.atDay(1).withMonth(12).withDayOfMonth(31).atTime(23, 59, 59)
        }

        return budgetRepository.getAllBudgets(userId).flatMapLatest { budgets ->
            val activeBudget = when (period) {
                BudgetPeriod.MONTHLY -> budgets.find {
                    it.period == BudgetPeriod.MONTHLY &&
                        it.month == now.monthValue &&
                        it.year == now.year
                }
                BudgetPeriod.YEARLY -> budgets.find {
                    it.period == BudgetPeriod.YEARLY && it.year == now.year
                }
            }
            if (activeBudget == null) {
                flowOf(null)
            } else {
                transactionRepository.getExpenseByCategoriesFlow(
                    userId = userId,
                    categoryIds = activeBudget.applicableCategoryIds,
                    start = start,
                    end = end
                ).map { spent ->
                    BudgetProgress(activeBudget, spent)
                }
            }
        }
    }

    private fun getRangeForPeriod(period: SummaryPeriod): Triple<LocalDateTime?, LocalDateTime?, String> {
        return when (period) {
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
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun loadDashboard() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            // Recent transactions
            transactionRepository.getRecentTransactions(userId, 5).collect { txns ->
                _uiState.update { it.copy(recentTransactions = txns) }
            }
        }
        viewModelScope.launch {
            // 1. Observe Period Changes & Transaction Data
            _selectedPeriod.flatMapLatest { period ->
                val (start, end, label) = getRangeForPeriod(period)

                // 2. Combine all data streams into one UI State update
                combine(
                    transactionRepository.getRecentTransactions(userId, 5),
                    transactionRepository.getTotalByTypeFlow(
                        userId,
                        TransactionType.INCOME,
                        start,
                        end
                    ),
                    transactionRepository.getTotalByTypeFlow(
                        userId,
                        TransactionType.EXPENSE,
                        start,
                        end
                    ),
                    observeBudgetProgress(BudgetPeriod.MONTHLY),
                    observeBudgetProgress(BudgetPeriod.YEARLY)
                ) { recent, income, expense, monthlyBudget, annualBudget ->
                    DashboardUiState(
                        recentTransactions = recent,
                        summary = MonthlySummary(income, expense, income - expense, label),
                        selectedPeriod = period,
                        monthlyBudgetProgress = monthlyBudget,
                        annualBudgetProgress = annualBudget,
                        userName = _uiState.value.userName,
                        isLoading = false
                    )
                }
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    private suspend fun refreshSummary(period: SummaryPeriod) {
        val (start, end, _) = when (period) {
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

        transactionRepository.getTotalByType(userId, TransactionType.INCOME, start, end)
        transactionRepository.getTotalByType(userId, TransactionType.EXPENSE, start, end)


    }

    fun selectPeriod(period: SummaryPeriod) {
        _selectedPeriod.value = period
    }

    fun logout() {
        viewModelScope.launch { authManager.signOut() }
    }
}
