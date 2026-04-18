package com.expensetracker.presentation.ui.transactions

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.data.repository.*
import com.expensetracker.domain.model.*
import com.expensetracker.util.CsvExporter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class TransactionsUiState(
    val transactions: List<Transaction> = emptyList(),
    val filteredTransactions: List<Transaction> = emptyList(),
    val categories: List<Category> = emptyList(),
    val modes: List<PaymentMode> = emptyList(),
    val allTags: List<Tag> = emptyList(),
    val filter: TransactionFilter = TransactionFilter(),
    val isLoading: Boolean = false,
    val exportSuccess: Boolean = false
)

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val paymentModeRepository: PaymentModeRepository,
    private val tagRepository: TagRepository,
    private val authManager: AuthManager,
    private val csvExporter: CsvExporter,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    private val userId get() = authManager.userId
    private val _uiState = MutableStateFlow(TransactionsUiState())
    val uiState: StateFlow<TransactionsUiState> = _uiState.asStateFlow()
    private val transactionsFlow = MutableStateFlow<List<Transaction>>(emptyList())
    private val filterFlow = MutableStateFlow(TransactionFilter())
    private var filterJob: Job? = null

    init {
        loadData()
        observeFilteredTransactions()
    }

    private fun loadData() {
        viewModelScope.launch {
            transactionRepository.getAllTransactions(userId).collect { txns ->
                transactionsFlow.value = txns
                _uiState.update { state -> state.copy(transactions = txns) }
            }
        }
        viewModelScope.launch {
            categoryRepository.getAllCategories(userId).collect { cats ->
                _uiState.update { it.copy(categories = cats) }
            }
        }
        viewModelScope.launch {
            paymentModeRepository.getAllModes(userId).collect { modes ->
                _uiState.update { it.copy(modes = modes) }
            }
        }
        viewModelScope.launch {
            tagRepository.getAllTags(userId).collect { tags ->
                _uiState.update { it.copy(allTags = tags) }
            }
        }
    }

    private fun observeFilteredTransactions() {
        filterJob?.cancel()
        filterJob = viewModelScope.launch {
            combine(
                transactionsFlow,
                filterFlow.debounce { filter ->
                    if (filter.searchQuery.isBlank()) 0L else 150L
                }
            ) { transactions, filter ->
                transactions to filter
            }.collectLatest { (transactions, filter) ->
                val filtered = withContext(Dispatchers.Default) {
                    applyFilter(transactions, filter)
                }
                _uiState.update { state ->
                    state.copy(
                        filter = filter,
                        filteredTransactions = filtered
                    )
                }
            }
        }
    }

    fun updateFilter(filter: TransactionFilter) {
        filterFlow.value = filter
    }

    private fun applyFilter(
        transactions: List<Transaction>,
        filter: TransactionFilter
    ): List<Transaction> {
        return transactions.filter { txn ->
            val matchesQuery = filter.searchQuery.isBlank() ||
                    txn.note.contains(filter.searchQuery, ignoreCase = true) ||
                    txn.categoryName.contains(filter.searchQuery, ignoreCase = true) ||
                    txn.tags.any { it.contains(filter.searchQuery, ignoreCase = true) }

            val matchesYear  = filter.year  == null || txn.dateTime.year == filter.year
            val matchesMonth = filter.month == null || txn.dateTime.monthValue == filter.month

            val matchesCategory = filter.categoryIds.isEmpty() ||
                    txn.categoryId in filter.categoryIds

            val matchesAccount = filter.paymentModeIds.isEmpty() ||
                    txn.paymentModeId in filter.paymentModeIds

            val matchesTags = filter.tags.isEmpty() ||
                    filter.tags.any { ft -> txn.tags.any { it.contains(ft, ignoreCase = true) } }

            val matchesType = filter.transactionTypes.isEmpty() ||
                    txn.type in filter.transactionTypes

            matchesQuery && matchesYear && matchesMonth &&
                    matchesCategory && matchesAccount && matchesTags && matchesType
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch { transactionRepository.deleteTransaction(id) }
    }

    fun exportToCsv() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val transactions = _uiState.value.filteredTransactions
                val file = csvExporter.exportToCsv(transactions)
                val uri = FileProvider.getUriForFile(
                    context, "${context.packageName}.fileprovider", file
                )
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(
                    Intent.createChooser(intent, "Export Transactions")
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                _uiState.update { it.copy(isLoading = false, exportSuccess = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun clearExportSuccess() = _uiState.update { it.copy(exportSuccess = false) }
}
