package com.expensetracker.presentation.ui.budget

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.data.repository.AuthManager
import com.expensetracker.data.repository.BudgetRepository
import com.expensetracker.data.repository.CategoryRepository
import com.expensetracker.data.repository.TransactionRepository
import com.expensetracker.domain.model.*
import com.expensetracker.presentation.components.EmptyState
import com.expensetracker.presentation.theme.ExpenseRed
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.YearMonth
import javax.inject.Inject

// ─── ViewModel ────────────────────────────────────────────────────────────────

data class BudgetUiState(
    val budgets: List<BudgetProgress> = emptyList(),
    val categories: List<Category> = emptyList(),
    val showDialog: Boolean = false,
    val editingBudget: Budget? = null,
    val isLoading: Boolean = false
)

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val authManager: AuthManager
) : ViewModel() {

    private val userId get() = authManager.userId
    private val _uiState = MutableStateFlow(BudgetUiState())
    val uiState: StateFlow<BudgetUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            categoryRepository.getAllCategories(userId).collect { cats ->
                _uiState.update { it.copy(categories = cats.filter { c -> c.transactionType == TransactionType.EXPENSE || c.transactionType == null }) }
            }
        }
        viewModelScope.launch {
            budgetRepository.getAllBudgets(userId).collect { budgets ->
                val progressList = budgets.map { budget ->
                    val (start, end) = getBudgetDateRange(budget)
                    val spent = transactionRepository.getExpenseByCategories(
                        userId, budget.applicableCategoryIds, start, end
                    )
                    BudgetProgress(budget, spent)
                }
                _uiState.update { it.copy(budgets = progressList) }
            }
        }
    }

    private fun getBudgetDateRange(budget: Budget): Pair<LocalDateTime, LocalDateTime> {
        return if (budget.period == BudgetPeriod.MONTHLY && budget.month != null) {
            val ym = YearMonth.of(budget.year, budget.month)
            Pair(ym.atDay(1).atStartOfDay(), ym.atEndOfMonth().atTime(23, 59, 59))
        } else {
            Pair(LocalDateTime.of(budget.year, 1, 1, 0, 0), LocalDateTime.of(budget.year, 12, 31, 23, 59, 59))
        }
    }

    fun showDialog(budget: Budget? = null) = _uiState.update { it.copy(showDialog = true, editingBudget = budget) }
    fun hideDialog() = _uiState.update { it.copy(showDialog = false, editingBudget = null) }

    fun saveBudget(name: String, limit: Double, period: BudgetPeriod, year: Int, month: Int?, categoryIds: List<Long>) {
        viewModelScope.launch {
            val editing = _uiState.value.editingBudget
            val budget = Budget(
                id = editing?.id ?: 0,
                name = name, totalLimit = limit, period = period,
                year = year, month = month,
                applicableCategoryIds = categoryIds, userId = userId
            )
            if (editing != null) budgetRepository.updateBudget(budget)
            else budgetRepository.insertBudget(budget)
            hideDialog()
        }
    }

    fun deleteBudget(budget: Budget) {
        viewModelScope.launch { budgetRepository.deleteBudget(budget) }
    }
}

// ─── Screen ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    onNavigateBack: () -> Unit,
    viewModel: BudgetViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Budget", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, null) }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.showDialog() },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Set Budget") }
            )
        }
    ) { padding ->
        if (uiState.budgets.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyState(
                    icon = Icons.Default.PieChart,
                    title = "No budgets set",
                    subtitle = "Set a monthly or yearly budget to track your spending"
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.budgets, key = { it.budget.id }) { progress ->
                    BudgetProgressCard(progress = progress,
                        onEdit = { viewModel.showDialog(progress.budget) },
                        onDelete = { viewModel.deleteBudget(progress.budget) })
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    if (uiState.showDialog) {
        BudgetDialog(
            editing = uiState.editingBudget,
            categories = uiState.categories,
            onDismiss = viewModel::hideDialog,
            onSave = { name, limit, period, year, month, categoryIds ->
                viewModel.saveBudget(name, limit, period, year, month, categoryIds)
            }
        )
    }
}

@Composable
private fun BudgetProgressCard(
    progress: BudgetProgress,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDelete by remember { mutableStateOf(false) }
    val pct = progress.percentage.coerceIn(0f, 100f)
    val color = when {
        pct >= 90 -> ExpenseRed
        pct >= 70 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(progress.budget.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(
                        "${progress.budget.period.name.lowercase().replaceFirstChar { it.uppercase() }} • ${progress.budget.year}" +
                                if (progress.budget.month != null) " / ${progress.budget.month}" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, null, Modifier.size(16.dp))
                    }
                    IconButton(onClick = { showDelete = true }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Spent: ₹${String.format("%,.0f", progress.spent)}", style = MaterialTheme.typography.bodySmall)
                Text("Limit: ₹${String.format("%,.0f", progress.budget.totalLimit)}", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { pct / 100f },
                modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
                color = color,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${pct.toInt()}% used", style = MaterialTheme.typography.labelSmall, color = color)
                Text(
                    "₹${String.format("%,.0f", (progress.budget.totalLimit - progress.spent).coerceAtLeast(0.0))} left",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Delete Budget") },
            text = { Text("Remove \"${progress.budget.name}\"?") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDelete = false }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Cancel") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BudgetDialog(
    editing: Budget?,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (String, Double, BudgetPeriod, Int, Int?, List<Long>) -> Unit
) {
    val currentYear = java.time.LocalDate.now().year
    var name by remember { mutableStateOf(editing?.name ?: "") }
    var limit by remember { mutableStateOf(editing?.totalLimit?.toString() ?: "") }
    var period by remember { mutableStateOf(editing?.period ?: BudgetPeriod.MONTHLY) }
    var year by remember { mutableStateOf(editing?.year ?: currentYear) }
    var month by remember { mutableStateOf(editing?.month) }
    var selectedCategoryIds by remember { mutableStateOf(editing?.applicableCategoryIds ?: emptyList()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editing != null) "Edit Budget" else "Set Budget") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Budget Name") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = limit, onValueChange = { limit = it },
                    label = { Text("Total Limit (₹)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal)
                )

                Text("Period", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BudgetPeriod.values().forEach { p ->
                        FilterChip(selected = period == p, onClick = { period = p },
                            label = { Text(p.name.lowercase().replaceFirstChar { it.uppercase() }) })
                    }
                }

                if (period == BudgetPeriod.MONTHLY) {
                    Text("Month (1-12)", style = MaterialTheme.typography.labelLarge)
                    OutlinedTextField(
                        value = month?.toString() ?: "",
                        onValueChange = { month = it.toIntOrNull()?.coerceIn(1, 12) },
                        label = { Text("Month") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number)
                    )
                }

                Text("Categories (empty = all expense)", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = cat.id in selectedCategoryIds,
                            onClick = {
                                selectedCategoryIds = if (cat.id in selectedCategoryIds)
                                    selectedCategoryIds - cat.id
                                else selectedCategoryIds + cat.id
                            },
                            label = { Text(cat.name, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val l = limit.toDoubleOrNull() ?: return@Button
                    if (name.isNotBlank()) onSave(name, l, period, year, if (period == BudgetPeriod.MONTHLY) month else null, selectedCategoryIds)
                },
                enabled = name.isNotBlank() && limit.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
