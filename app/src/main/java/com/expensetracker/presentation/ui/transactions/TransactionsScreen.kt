package com.expensetracker.presentation.ui.transactions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.expensetracker.domain.model.Category
import com.expensetracker.domain.model.PaymentMode
import com.expensetracker.domain.model.Tag
import com.expensetracker.domain.model.Transaction
import com.expensetracker.domain.model.TransactionFilter
import com.expensetracker.domain.model.TransactionType
import com.expensetracker.presentation.components.EmptyState
import com.expensetracker.presentation.components.LoadingOverlay
import com.expensetracker.presentation.components.TransactionListItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    viewModel: TransactionsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showFilterSheet by remember { mutableStateOf(false) }
    var selectedTransaction by remember { mutableStateOf<Transaction?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.exportSuccess) {
        if (uiState.exportSuccess) {
            snackbarHostState.showSnackbar("Exported successfully!")
            viewModel.clearExportSuccess()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Transactions", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showFilterSheet = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filter")
                    }
                    IconButton(onClick = viewModel::exportToCsv) {
                        Icon(Icons.Default.Upload, contentDescription = "Export CSV")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            SearchBar(
                query = uiState.filter.searchQuery,
                onQueryChange = { query ->
                    viewModel.updateFilter(uiState.filter.copy(searchQuery = query))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            ActiveFilterChips(
                filter = uiState.filter,
                categories = uiState.categories,
                modes = uiState.modes,
                onClearFilter = { viewModel.updateFilter(TransactionFilter()) }
            )

            val totalExpense = uiState.filteredTransactions
                .filter { it.type == TransactionType.EXPENSE }
                .sumOf { it.amount }
            val totalIncome = uiState.filteredTransactions
                .filter { it.type == TransactionType.INCOME }
                .sumOf { it.amount }
            MiniSummaryRow(
                count = uiState.filteredTransactions.size,
                totalExpense = totalExpense,
                totalIncome = totalIncome
            )

            if (uiState.filteredTransactions.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        icon = Icons.Default.SearchOff,
                        title = "No transactions found",
                        subtitle = "Try adjusting filters or search"
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.filteredTransactions, key = { it.id }) { txn ->
                        var showDeleteDialog by remember { mutableStateOf(false) }
                        SwipeToDismissBox(
                            state = rememberSwipeToDismissBoxState(
                                confirmValueChange = { state ->
                                    if (state == SwipeToDismissBoxValue.EndToStart) {
                                        showDeleteDialog = true
                                    }
                                    false
                                }
                            ),
                            backgroundContent = {
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .padding(end = 16.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(
                                        Icons.Default.Delete, null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        ) {
                            TransactionListItem(
                                transaction = txn,
                                onClick = { selectedTransaction = txn }
                            )
                        }

                        if (showDeleteDialog) {
                            AlertDialog(
                                onDismissRequest = { showDeleteDialog = false },
                                title = { Text("Delete Transaction") },
                                text = { Text("Are you sure you want to delete this transaction?") },
                                confirmButton = {
                                    TextButton(onClick = {
                                        viewModel.deleteTransaction(txn.id)
                                        showDeleteDialog = false
                                    }) {
                                        Text("Delete", color = MaterialTheme.colorScheme.error)
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showDeleteDialog = false }) {
                                        Text("Cancel")
                                    }
                                }
                            )
                        }
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }

    if (showFilterSheet) {
        FilterBottomSheet(
            filter = uiState.filter,
            categories = uiState.categories,
            modes = uiState.modes,
            allTags = uiState.allTags,
            onApply = { viewModel.updateFilter(it) },
            onDismiss = { showFilterSheet = false }
        )
    }

    selectedTransaction?.let { txn ->
        TransactionDetailSheet(
            transaction = txn,
            onEdit = { onNavigateToEdit(txn.id); selectedTransaction = null },
            onDelete = { viewModel.deleteTransaction(txn.id); selectedTransaction = null },
            onDismiss = { selectedTransaction = null }
        )
    }

    if (uiState.isLoading) LoadingOverlay(true)
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var localQuery by rememberSaveable { mutableStateOf(query) }

    LaunchedEffect(query) {
        if (query != localQuery) {
            localQuery = query
        }
    }

    OutlinedTextField(
        value = localQuery,
        onValueChange = {
            localQuery = it
            onQueryChange(it)
        },
        placeholder = { Text("Search transactions...") },
        leadingIcon = { Icon(Icons.Default.Search, null) },
        trailingIcon = if (localQuery.isNotEmpty()) {
            {
                IconButton(onClick = {
                    localQuery = ""
                    onQueryChange("")
                }) {
                    Icon(Icons.Default.Close, null)
                }
            }
        } else null,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.ContentOrLtr)
    )
}

@Composable
private fun ActiveFilterChips(
    filter: TransactionFilter,
    categories: List<Category>,
    modes: List<PaymentMode>,
    onClearFilter: () -> Unit
) {
    val hasFilters = filter.year != null || filter.month != null ||
            filter.categoryIds.isNotEmpty() || filter.paymentModeIds.isNotEmpty() ||
            filter.tags.isNotEmpty() || filter.transactionTypes.isNotEmpty()

    AnimatedVisibility(
        visible = hasFilters,
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = true,
                    onClick = onClearFilter,
                    label = { Text("Clear All") },
                    trailingIcon = { Icon(Icons.Default.Close, null, Modifier.size(14.dp)) }
                )
            }
            filter.year?.let {
                item { FilterChip(selected = true, onClick = {}, label = { Text("Year: $it") }) }
            }
            filter.month?.let {
                item { FilterChip(selected = true, onClick = {}, label = { Text("Month: $it") }) }
            }
        }
    }
}

@Composable
private fun MiniSummaryRow(count: Int, totalExpense: Double, totalIncome: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "$count transactions",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            "+₹${String.format("%,.0f", totalIncome)}",
            style = MaterialTheme.typography.labelMedium,
            color = com.expensetracker.presentation.theme.IncomeGreen
        )
        Text(
            "-₹${String.format("%,.0f", totalExpense)}",
            style = MaterialTheme.typography.labelMedium,
            color = com.expensetracker.presentation.theme.ExpenseRed
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterBottomSheet(
    filter: TransactionFilter,
    categories: List<Category>,
    modes: List<PaymentMode>,
    allTags: List<Tag>,
    onApply: (TransactionFilter) -> Unit,
    onDismiss: () -> Unit
) {
    var localFilter by remember { mutableStateOf(filter) }
    val months = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )
    val currentYear = java.time.LocalDate.now().year
    val years = (currentYear downTo currentYear - 5).toList()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
        ) {
            Text(
                "Filter Transactions",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))

            // Year
            Text("Year", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = localFilter.year == null,
                        onClick = { localFilter = localFilter.copy(year = null) },
                        label = { Text("All") }
                    )
                }
                items(years) { year ->
                    FilterChip(
                        selected = localFilter.year == year,
                        onClick = {
                            localFilter = localFilter.copy(
                                year = if (localFilter.year == year) null else year
                            )
                        },
                        label = { Text("$year") }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Month
            Text("Month", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = localFilter.month == null,
                        onClick = { localFilter = localFilter.copy(month = null) },
                        label = { Text("All") }
                    )
                }
                items(months.mapIndexed { i, m -> Pair(i + 1, m) }) { (idx, mon) ->
                    FilterChip(
                        selected = localFilter.month == idx,
                        onClick = {
                            localFilter = localFilter.copy(
                                month = if (localFilter.month == idx) null else idx
                            )
                        },
                        label = { Text(mon) }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Transaction Type
            Text("Type", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // entries replaces deprecated values()
                TransactionType.entries.forEach { type ->
                    FilterChip(
                        selected = type in localFilter.transactionTypes,
                        onClick = {
                            localFilter = if (type in localFilter.transactionTypes)
                                localFilter.copy(
                                    transactionTypes = localFilter.transactionTypes - type
                                )
                            else localFilter.copy(
                                transactionTypes = localFilter.transactionTypes + type
                            )
                        },
                        label = {
                            Text(type.name.lowercase().replaceFirstChar { it.uppercase() })
                        }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Category
            Text("Category", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { cat ->
                    FilterChip(
                        selected = cat.id in localFilter.categoryIds,
                        onClick = {
                            localFilter = if (cat.id in localFilter.categoryIds)
                                localFilter.copy(categoryIds = localFilter.categoryIds - cat.id)
                            else localFilter.copy(categoryIds = localFilter.categoryIds + cat.id)
                        },
                        label = { Text(cat.name) }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Payment Mode
            Text("Payment Mode", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(modes) { mode ->
                    FilterChip(
                        selected = mode.id in localFilter.paymentModeIds,
                        onClick = {
                            localFilter = if (mode.id in localFilter.paymentModeIds)
                                localFilter.copy(
                                    paymentModeIds = localFilter.paymentModeIds - mode.id
                                )
                            else localFilter.copy(
                                paymentModeIds = localFilter.paymentModeIds + mode.id
                            )
                        },
                        label = { Text(mode.displayLabel) }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = {
                        localFilter = TransactionFilter()
                        onApply(TransactionFilter())
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Reset") }
                Button(
                    onClick = { onApply(localFilter); onDismiss() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Apply Filters") }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
