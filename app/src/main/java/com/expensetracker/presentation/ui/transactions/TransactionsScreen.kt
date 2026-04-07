package com.expensetracker.presentation.ui.transactions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.expensetracker.domain.model.Category
import com.expensetracker.domain.model.PaymentMode
import com.expensetracker.domain.model.Tag
import com.expensetracker.domain.model.Transaction
import com.expensetracker.domain.model.TransactionFilter
import com.expensetracker.domain.model.TransactionType
import com.expensetracker.presentation.components.CategoryIconBubble
import com.expensetracker.presentation.components.EmptyState
import com.expensetracker.presentation.components.LoadingOverlay
import com.expensetracker.presentation.components.fmtAmt
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    onNavigateToAdd: () -> Unit = {},
    viewModel: TransactionsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showFilterSheet by remember { mutableStateOf(false) }
    var selectedTransaction by remember { mutableStateOf<Transaction?>(null) }
    var sortDescending by rememberSaveable { mutableStateOf(true) }
    val snackbarHostState = remember { SnackbarHostState() }

    val sortedTransactions by remember(uiState.filteredTransactions, sortDescending) {
        derivedStateOf {
            if (sortDescending) {
                uiState.filteredTransactions.sortedByDescending { it.dateTime }
            } else {
                uiState.filteredTransactions.sortedBy { it.dateTime }
            }
        }
    }
    val groupedTransactions by remember(sortedTransactions) {
        derivedStateOf { sortedTransactions.groupBy { it.dateTime.toLocalDate() }.toList() }
    }

    LaunchedEffect(uiState.exportSuccess) {
        if (uiState.exportSuccess) {
            snackbarHostState.showSnackbar("Exported successfully!")
            viewModel.clearExportSuccess()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAdd,
                containerColor = MaterialTheme.colorScheme.onBackground,
                contentColor = MaterialTheme.colorScheme.background
            ) {
                Text("+", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Light)
            }
        },
        topBar = {
            Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularActionButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        onClick = onNavigateBack
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        "Transactions",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    CircularActionButton(
                        icon = Icons.Default.Upload,
                        contentDescription = "Export",
                        onClick = viewModel::exportToCsv
                    )
                    Spacer(Modifier.width(10.dp))
                    CircularActionButton(
                        icon = Icons.Default.FilterList,
                        contentDescription = "Open Filters",
                        onClick = { showFilterSheet = true }
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            SearchAndActionRow(
                query = uiState.filter.searchQuery,
                onQueryChange = { query ->
                    viewModel.updateFilter(uiState.filter.copy(searchQuery = query))
                },
                onFilterClick = { showFilterSheet = true },
                onSortClick = { sortDescending = !sortDescending },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            )

            ActiveFilterChips(
                filter = uiState.filter,
                categories = uiState.categories,
                modes = uiState.modes,
                onClearFilter = { viewModel.updateFilter(TransactionFilter()) }
            )

            if (sortedTransactions.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        icon = Icons.Default.SearchOff,
                        title = "No transactions found",
                        subtitle = "Try adjusting filters or search"
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(groupedTransactions, key = { it.first.toString() }) { (date, transactions) ->
                        TransactionDayCard(
                            date = date,
                            transactions = transactions,
                            onTransactionClick = { selectedTransaction = it }
                        )
                    }
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
private fun SearchAndActionRow(
    query: String,
    onQueryChange: (String) -> Unit,
    onFilterClick: () -> Unit,
    onSortClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SearchBar(
            query = query,
            onQueryChange = onQueryChange,
            modifier = Modifier.weight(1f)
        )
        CircularActionButton(
            icon = Icons.Default.FilterList,
            contentDescription = "Filter",
            onClick = onFilterClick
        )
        CircularActionButton(
            icon = Icons.AutoMirrored.Filled.Sort,
            contentDescription = "Sort",
            onClick = onSortClick
        )
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var localQuery by rememberSaveable { mutableStateOf(query) }

    LaunchedEffect(query) {
        if (query != localQuery) localQuery = query
    }

    OutlinedTextField(
        value = localQuery,
        onValueChange = {
            localQuery = it
            onQueryChange(it)
        },
        placeholder = { Text("Search transactions") },
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
        shape = RoundedCornerShape(24.dp),
        singleLine = true,
        textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.ContentOrLtr),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            focusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f),
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
        )
    )
}

@Composable
private fun CircularActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(36.dp)
            .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape)
    ) {
        Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(24.dp))
    }
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
            filter.categoryIds.takeIf { it.isNotEmpty() }?.let { ids ->
                val categoryLabel = categories.filter { it.id in ids }.joinToString { it.name }
                item { FilterChip(selected = true, onClick = {}, label = { Text(categoryLabel) }) }
            }
            filter.paymentModeIds.takeIf { it.isNotEmpty() }?.let { ids ->
                val paymentLabel = modes.filter { it.id in ids }.joinToString { it.displayLabel }
                item { FilterChip(selected = true, onClick = {}, label = { Text(paymentLabel) }) }
            }
        }
    }
}

@Composable
private fun TransactionDayCard(
    date: LocalDate,
    transactions: List<Transaction>,
    onTransactionClick: (Transaction) -> Unit
) {
    val netAmount = transactions.sumOf { txn ->
        when (txn.type) {
            TransactionType.INCOME -> txn.amount
            TransactionType.EXPENSE -> -txn.amount
            TransactionType.TRANSFER -> 0.0
        }
    }
    val dateLabel = when (date) {
        LocalDate.now() -> "Today"
        LocalDate.now().minusDays(1) -> "Yesterday"
        else -> date.format(DateTimeFormatter.ofPattern("dd MMMM", Locale.getDefault()))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateLabel,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = dayTotalLabel(netAmount),
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                transactions.forEach { transaction ->
                    TransactionDayRow(
                        transaction = transaction,
                        onClick = { onTransactionClick(transaction) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionDayRow(
    transaction: Transaction,
    onClick: () -> Unit
) {
    val title = transaction.note.ifEmpty { transaction.categoryName.ifEmpty { "Uncategorized" } }
    val subtitle = transaction.paymentModeName.ifEmpty { transaction.categoryName.ifEmpty { "Payment mode" } }
    val timeLabel = transaction.dateTime.format(
        DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault())
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryIconBubble(
            iconKey = transaction.categoryIcon.ifEmpty { "category" },
            colorHex = transaction.categoryColorHex.ifEmpty { "#6750A4" },
            size = 40
        )
        Spacer(Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "₹${fmtAmt(abs(transaction.amount))}",
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = timeLabel,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}

private fun dayTotalLabel(amount: Double): String {
    val prefix = when {
        amount > 0 -> "+"
        amount < 0 -> "-"
        else -> ""
    }
    return prefix + "₹" + fmtAmt(abs(amount))
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
    val currentYear = LocalDate.now().year
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
                items(months.mapIndexed { i, m -> i + 1 to m }) { (idx, mon) ->
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

            Text("Type", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TransactionType.entries.forEach { type ->
                    FilterChip(
                        selected = type in localFilter.transactionTypes,
                        onClick = {
                            localFilter = if (type in localFilter.transactionTypes) {
                                localFilter.copy(
                                    transactionTypes = localFilter.transactionTypes - type
                                )
                            } else {
                                localFilter.copy(
                                    transactionTypes = localFilter.transactionTypes + type
                                )
                            }
                        },
                        label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Text("Category", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { cat ->
                    FilterChip(
                        selected = cat.id in localFilter.categoryIds,
                        onClick = {
                            localFilter = if (cat.id in localFilter.categoryIds) {
                                localFilter.copy(categoryIds = localFilter.categoryIds - cat.id)
                            } else {
                                localFilter.copy(categoryIds = localFilter.categoryIds + cat.id)
                            }
                        },
                        label = { Text(cat.name) }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Text("Payment Mode", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(modes) { mode ->
                    FilterChip(
                        selected = mode.id in localFilter.paymentModeIds,
                        onClick = {
                            localFilter = if (mode.id in localFilter.paymentModeIds) {
                                localFilter.copy(paymentModeIds = localFilter.paymentModeIds - mode.id)
                            } else {
                                localFilter.copy(paymentModeIds = localFilter.paymentModeIds + mode.id)
                            }
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
                ) {
                    Text("Reset")
                }
                Button(
                    onClick = { onApply(localFilter); onDismiss() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Apply Filters")
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
