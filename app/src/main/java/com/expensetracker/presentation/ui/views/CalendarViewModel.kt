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
import java.time.YearMonth
import javax.inject.Inject

data class DayData(
    val date:             LocalDate,
    val totalExpense:     Double,
    val totalIncome:      Double,
    val transactionCount: Int
)

data class CalendarUiState(
    val yearMonth:        YearMonth              = YearMonth.now(),
    val dayDataMap:       Map<LocalDate, DayData> = emptyMap(),
    val monthTransactionCount: Int               = 0,
    val isLoading:        Boolean                = true
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val authManager:           AuthManager
) : ViewModel() {

    private val userId get() = authManager.userId

    private val _uiState  = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init { loadMonth(_uiState.value.yearMonth) }

    fun previousMonth() = loadMonth(_uiState.value.yearMonth.minusMonths(1))
    fun nextMonth()     = loadMonth(_uiState.value.yearMonth.plusMonths(1))

    private fun loadMonth(ym: YearMonth) {
        _uiState.update { it.copy(yearMonth = ym, isLoading = true) }
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val start = ym.atDay(1).atStartOfDay()
            val end   = ym.atEndOfMonth().atTime(23, 59, 59)

            val txns = withContext(Dispatchers.IO) {
                // Use getAllTransactionsOneShot and filter — works without range method
                transactionRepository.getAllTransactionsOneShot(userId).filter { txn ->
                    !txn.dateTime.isBefore(start) && !txn.dateTime.isAfter(end)
                }
            }

            val dayMap = buildDayMap(txns)
            _uiState.update {
                it.copy(
                    yearMonth             = ym,
                    dayDataMap            = dayMap,
                    monthTransactionCount = txns.size,
                    isLoading             = false
                )
            }
        }
    }

    private fun buildDayMap(txns: List<Transaction>): Map<LocalDate, DayData> {
        val grouped = txns.groupBy { it.dateTime.toLocalDate() }
        return grouped.mapValues { (date, list) ->
            DayData(
                date             = date,
                totalExpense     = list.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount },
                totalIncome      = list.filter { it.type == TransactionType.INCOME  }.sumOf { it.amount },
                transactionCount = list.size
            )
        }
    }
}