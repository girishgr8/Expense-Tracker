package com.expensetracker.presentation.ui.transactions

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.data.repository.AuthManager
import com.expensetracker.data.repository.CategoryRepository
import com.expensetracker.data.repository.PaymentModeRepository
import com.expensetracker.data.repository.ScheduledTransactionRepository
import com.expensetracker.domain.model.Category
import com.expensetracker.domain.model.PaymentMode
import com.expensetracker.domain.model.ScheduledFrequency
import com.expensetracker.domain.model.ScheduledTransaction
import com.expensetracker.domain.model.TransactionType
import com.expensetracker.util.ReminderNotificationScheduler
import com.expensetracker.util.ScheduledTransactionScheduler
import com.expensetracker.worker.ScheduledTransactionWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ScheduleStatusFilter { ALL, ACTIVE, INACTIVE }

data class ScheduledTransactionsUiState(
    val schedules: List<ScheduledTransaction> = emptyList(),
    val filteredSchedules: List<ScheduledTransaction> = emptyList(),
    val categories: List<Category> = emptyList(),
    val modes: List<PaymentMode> = emptyList(),
    val searchQuery: String = "",
    val statusFilter: ScheduleStatusFilter = ScheduleStatusFilter.ALL,
    val typeFilter: TransactionType? = null,
    val frequencyFilter: ScheduledFrequency? = null,
    val isLoading: Boolean = false,
    // ID of the schedule the user tapped — drives the detail/edit bottom sheet
    val selectedScheduleId: Long? = null
)

@HiltViewModel
class ScheduledTransactionsViewModel @Inject constructor(
    private val scheduledTransactionRepository: ScheduledTransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val paymentModeRepository: PaymentModeRepository,
    private val authManager: AuthManager,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    private val userId get() = authManager.userId

    private val _uiState = MutableStateFlow(ScheduledTransactionsUiState())
    val uiState: StateFlow<ScheduledTransactionsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                scheduledTransactionRepository.getAllScheduledTransactions(userId),
                categoryRepository.getAllCategories(userId),
                paymentModeRepository.getAllModes(userId)
            ) { schedules, cats, modes -> Triple(schedules, cats, modes) }
                .collect { (schedules, cats, modes) ->
                    _uiState.update { state ->
                        state.copy(
                            schedules = schedules,
                            categories = cats,
                            modes = modes,
                            filteredSchedules = applyFilters(
                                schedules,
                                state.searchQuery,
                                state.statusFilter,
                                state.typeFilter,
                                state.frequencyFilter
                            )
                        )
                    }
                }
        }
    }

    // ── Filter helpers ────────────────────────────────────────────────────────

    fun setSearchQuery(query: String) = updateFilters(searchQuery = query)
    fun setStatusFilter(f: ScheduleStatusFilter) = updateFilters(statusFilter = f)
    fun setTypeFilter(f: TransactionType?) = updateFilters(typeFilter = f)
    fun setFrequencyFilter(f: ScheduledFrequency?) = updateFilters(frequencyFilter = f)

    private fun updateFilters(
        searchQuery: String = _uiState.value.searchQuery,
        statusFilter: ScheduleStatusFilter = _uiState.value.statusFilter,
        typeFilter: TransactionType? = _uiState.value.typeFilter,
        frequencyFilter: ScheduledFrequency? = _uiState.value.frequencyFilter
    ) {
        _uiState.update { state ->
            state.copy(
                searchQuery = searchQuery,
                statusFilter = statusFilter,
                typeFilter = typeFilter,
                frequencyFilter = frequencyFilter,
                filteredSchedules = applyFilters(
                    state.schedules, searchQuery, statusFilter, typeFilter, frequencyFilter
                )
            )
        }
    }

    private fun applyFilters(
        schedules: List<ScheduledTransaction>,
        searchQuery: String,
        statusFilter: ScheduleStatusFilter,
        typeFilter: TransactionType?,
        frequencyFilter: ScheduledFrequency?
    ): List<ScheduledTransaction> = schedules.filter { s ->
        val matchesSearch = searchQuery.isBlank() ||
                s.note.contains(searchQuery, ignoreCase = true)

        val matchesStatus = when (statusFilter) {
            ScheduleStatusFilter.ALL -> true
            ScheduleStatusFilter.ACTIVE -> s.isActive
            ScheduleStatusFilter.INACTIVE -> !s.isActive
        }

        val matchesType = typeFilter == null || s.type == typeFilter
        val matchesFrequency = frequencyFilter == null || s.frequency == frequencyFilter

        matchesSearch && matchesStatus && matchesType && matchesFrequency
    }

    fun clearFilters() {
        _uiState.update { state ->
            state.copy(
                searchQuery = "",
                statusFilter = ScheduleStatusFilter.ALL,
                typeFilter = null,
                frequencyFilter = null,
                filteredSchedules = state.schedules
            )
        }
    }

    // ── Selection ─────────────────────────────────────────────────────────────

    fun selectSchedule(id: Long) = _uiState.update { it.copy(selectedScheduleId = id) }
    fun clearSelection() = _uiState.update { it.copy(selectedScheduleId = null) }

    val selectedSchedule
        get() =
            _uiState.value.schedules.find { it.id == _uiState.value.selectedScheduleId }

    // ── Mutations ─────────────────────────────────────────────────────────────

    fun toggleActive(schedule: ScheduledTransaction) {
        viewModelScope.launch {
            val updated = schedule.copy(isActive = !schedule.isActive)
            scheduledTransactionRepository.updateScheduledTransaction(updated)
            if (updated.isActive) {
                // Re-schedule the next run + reminder
                ScheduledTransactionScheduler.schedule(
                    context, updated.id, updated.nextRunAt
                )
                ScheduledTransactionWorker.scheduleReminder(context, updated)
            } else {
                ScheduledTransactionScheduler.cancel(context, updated.id)
                ReminderNotificationScheduler.cancel(context, updated.id)
            }
        }
    }

    fun deleteSchedule(schedule: ScheduledTransaction) {
        viewModelScope.launch {
            scheduledTransactionRepository.deleteScheduledTransaction(schedule)
            ScheduledTransactionScheduler.cancel(context, schedule.id)
            ReminderNotificationScheduler.cancel(context, schedule.id)
            clearSelection()
        }
    }

    // ── Category / Mode lookup helpers used by the UI ─────────────────────────

    fun categoryFor(schedule: ScheduledTransaction): Category? =
        _uiState.value.categories.find { it.id == schedule.categoryId }

    fun modeFor(schedule: ScheduledTransaction): PaymentMode? =
        _uiState.value.modes.find { it.id == schedule.paymentModeId }
}