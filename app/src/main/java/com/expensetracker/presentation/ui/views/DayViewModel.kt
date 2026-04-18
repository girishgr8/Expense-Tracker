package com.expensetracker.presentation.ui.views

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.data.repository.AuthManager
import com.expensetracker.data.repository.TransactionRepository
import com.expensetracker.domain.model.Transaction
import com.expensetracker.domain.model.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject

data class DayViewUiState(
    val date: LocalDate = LocalDate.now(),
    val transactions: List<Transaction> = emptyList(),
    val totalExpense: Double = 0.0,
    val totalIncome: Double = 0.0,
    val isLoading: Boolean = true
)

@HiltViewModel
class DayViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val authManager: AuthManager
) : ViewModel() {

    private val userId get() = authManager.userId

    private val _uiState = MutableStateFlow(DayViewUiState())
    val uiState: StateFlow<DayViewUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    fun loadDay(date: LocalDate) {
        _uiState.update { it.copy(date = date, isLoading = true) }
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val start = date.atStartOfDay()
            val end = date.atTime(23, 59, 59)

            val txns = withContext(Dispatchers.IO) {
                transactionRepository.getAllTransactionsOneShot(userId).filter { txn ->
                    !txn.dateTime.isBefore(start) && !txn.dateTime.isAfter(end)
                }.sortedByDescending { it.dateTime }   // newest first, matching screenshot
            }

            _uiState.update {
                it.copy(
                    date = date,
                    transactions = txns,
                    totalExpense = txns.filter { t -> t.type == TransactionType.EXPENSE }
                        .sumOf { t -> t.amount },
                    totalIncome = txns.filter { t -> t.type == TransactionType.INCOME }
                        .sumOf { t -> t.amount },
                    isLoading = false
                )
            }
        }
    }

    fun previousDay() = loadDay(_uiState.value.date.minusDays(1))
    fun nextDay() = loadDay(_uiState.value.date.plusDays(1))
}