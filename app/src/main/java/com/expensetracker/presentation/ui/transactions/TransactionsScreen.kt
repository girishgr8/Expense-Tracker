package com.expensetracker.presentation.ui.transactions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.expensetracker.domain.model.Category
import com.expensetracker.domain.model.PaymentMode
import com.expensetracker.domain.model.Tag
import com.expensetracker.domain.model.Transaction
import com.expensetracker.domain.model.TransactionFilter
import com.expensetracker.domain.model.TransactionType
import com.expensetracker.presentation.components.CategoryIconBubble
import com.expensetracker.presentation.components.EmptyState
import com.expensetracker.presentation.components.LoadingOverlay
import com.expensetracker.presentation.components.LocalCurrencyFormat
import com.expensetracker.presentation.components.LocalCurrencySymbol
import com.expensetracker.presentation.theme.ExpenseRed
import com.expensetracker.domain.model.TagsMode
import androidx.compose.material.icons.filled.CheckCircle
import com.expensetracker.presentation.theme.IncomeGreen
import com.expensetracker.presentation.theme.TransferBlue
import com.expensetracker.util.FormatUtils.formatAmountForDisplay
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
    val currencySymbol = LocalCurrencySymbol.current
    val currencyFormat = LocalCurrencyFormat.current

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
                Text(
                    "+",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Light
                )
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
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 6.dp,
                        bottom = 96.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(
                        groupedTransactions,
                        key = { it.first.toString() }) { (date, transactions) ->
                        TransactionDayCard(
                            date = date,
                            transactions = transactions,
                            currencySymbol = currencySymbol,
                            currencyFormat = currencyFormat,
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
    currencySymbol: String,
    currencyFormat: String,
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
                var netAmountColor = MaterialTheme.colorScheme.onSurface
                if (netAmount < 0) netAmountColor = ExpenseRed
                else if (netAmount > 0) netAmountColor = IncomeGreen
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
                    color = netAmountColor
                )
            }

            Spacer(Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                transactions.forEachIndexed { index, transaction ->
                    TransactionDayRow(
                        transaction = transaction,
                        currencySymbol = currencySymbol,
                        currencyFormat = currencyFormat,
                        onClick = { onTransactionClick(transaction) }
                    )
                    if (index < transactions.size - 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 0.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                                .copy(alpha = 0.5f),
                            thickness = 0.5.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionDayRow(
    transaction: Transaction,
    currencySymbol: String,
    currencyFormat: String,
    onClick: () -> Unit
) {
    val title = transaction.note.ifEmpty { transaction.categoryName.ifEmpty { "Uncategorized" } }
    val subtitle =
        transaction.paymentModeName.ifEmpty { transaction.categoryName.ifEmpty { "Payment mode" } }
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
            val txnAmtColor = when (transaction.type) {
                TransactionType.INCOME -> IncomeGreen
                TransactionType.EXPENSE -> ExpenseRed
                TransactionType.TRANSFER -> TransferBlue
            }
            Text(
                text = "$currencySymbol${
                    formatAmountForDisplay(
                        abs(transaction.amount),
                        currencyFormat
                    )
                }",
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                fontWeight = FontWeight.SemiBold,
                color = txnAmtColor
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

@Composable
private fun dayTotalLabel(amount: Double): String {
    val currencySymbol = LocalCurrencySymbol.current
    val currencyFormat = LocalCurrencyFormat.current
    val prefix = when {
        amount > 0 -> "+"
        amount < 0 -> "-"
        else -> ""
    }
    return prefix + currencySymbol + formatAmountForDisplay(abs(amount), currencyFormat)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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

    // Combined year+month chips — years full (2026), recent months abbreviated (Apr 26)
    val currentYear = LocalDate.now().year
    val currentMonth = LocalDate.now().monthValue
    val years = (currentYear downTo currentYear - 5).toList()
    val shortMonthNames = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )

    // Date chips: 3 year chips (current → current-2) + 24 month chips back from current month
    // Ordered: years first, then months newest→oldest
    data class DateChip(val year: Int, val month: Int?, val label: String)

    val yearChips = (currentYear downTo currentYear - 2).map { DateChip(it, null, "$it") }
    val monthChips = run {
        val chips = mutableListOf<DateChip>()
        var y = currentYear; var m = currentMonth
        // 24 months back = covers ~2 full years of month chips
        repeat(24) {
            chips += DateChip(y, m,
                "${shortMonthNames[m - 1]} ${(y % 100).toString().padStart(2, '0')}")
            m--; if (m == 0) { m = 12; y-- }
        }
        chips
    }
    val allDateChips = yearChips + monthChips
    val showMoreDate = allDateChips.size > 8
    var expandDate by remember { mutableStateOf(false) }
    val visibleDate = if (expandDate) allDateChips else allDateChips.take(8)

    // Category type sub-filter (All / Spending / Income / Transfer)
    val typeLabels = listOf("All", "Spending", "Income", "Transfer")
    var catTypeTab by remember { mutableStateOf("All") }
    val visibleCategories = categories.filter { cat ->
        when (catTypeTab) {
            "Spending" -> cat.transactionType == TransactionType.EXPENSE
            "Income" -> cat.transactionType == TransactionType.INCOME
            "Transfer" -> cat.transactionType == null  // transfers typically uncategorized
            else -> true
        }
    }
    val showMoreCat = visibleCategories.size > 8
    var expandCat by remember { mutableStateOf(false) }
    val visibleCat = if (expandCat) visibleCategories else visibleCategories.take(8)

    val showMoreMode = modes.size > 7
    var expandMode by remember { mutableStateOf(false) }
    val visibleModes = if (expandMode) modes else modes.take(7)

    val showMoreTags = allTags.size > 7
    var expandTags by remember { mutableStateOf(false) }
    val visibleTags = if (expandTags) allTags else allTags.take(7)

    // Tags match mode — synced from existing filter value
    var tagsMode by remember { mutableStateOf(localFilter.tagsMode) }
    var showTagMatchMenu by remember { mutableStateOf(false) }
    // Keep legacy tagsMatchAny alias for brevity
    val tagsMatchAny = tagsMode == TagsMode.INCLUDES_ANY

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
        ) {
            Spacer(Modifier.height(12.dp))

            // ── Header row ────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Filter",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                // Reset pill button
                OutlinedButton(
                    onClick = {
                        localFilter = TransactionFilter()
                        catTypeTab = "All"
                    },
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.Refresh, null, Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Reset", style = MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.width(8.dp))
                // Close circle button
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                ) {
                    Icon(Icons.Default.Close, "Close", Modifier.size(16.dp))
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Year / Month section ──────────────────────────────────────────
            SectionHeader(
                label = "Year/month",
                trailingLabel = "Date range",
                onTrailingClick = {}   // date-range mode reserved for future
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                visibleDate.forEach { chip ->
                    // A year chip is "selected" if explicitly chosen, OR if it is the
                    // current year and no date filter has been applied yet (default highlight)
                    val isSelected = if (chip.month == null) {
                        if (localFilter.year == null)
                            chip.year == currentYear   // default: current year highlighted
                        else
                            localFilter.year == chip.year && localFilter.month == null
                    } else {
                        localFilter.year == chip.year && localFilter.month == chip.month
                    }
                    CompactFilterChip(
                        label = chip.label,
                        selected = isSelected,
                        onClick = {
                            localFilter = if (isSelected)
                                localFilter.copy(year = null, month = null)
                            else
                                localFilter.copy(year = chip.year, month = chip.month)
                        }
                    )
                }
                if (showMoreDate) {
                    OverflowChip(expanded = expandDate) { expandDate = !expandDate }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Category section ──────────────────────────────────────────────
            SectionHeader(
                label = "Category",
                isAllSelected = visibleCategories.isNotEmpty() &&
                        localFilter.categoryIds.containsAll(visibleCategories.map { it.id }),
                onSelectAll = {
                    val allIds = visibleCategories.map { it.id }.toSet()
                    localFilter = if (localFilter.categoryIds.containsAll(allIds.toList()))
                        localFilter.copy(categoryIds = emptyList())
                    else
                        localFilter.copy(categoryIds = localFilter.categoryIds + allIds)
                }
            )
            Spacer(Modifier.height(8.dp))

            // Category type sub-tabs (All / Spending / Income / Transfer)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(50)
                    )
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                typeLabels.forEach { tab ->
                    val sel = tab == catTypeTab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (sel) MaterialTheme.colorScheme.onSurface
                                else androidx.compose.ui.graphics.Color.Transparent,
                                RoundedCornerShape(50)
                            )
                            .clickable { catTypeTab = tab }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            tab,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                            color = if (sel) MaterialTheme.colorScheme.surface
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                visibleCat.forEach { cat ->
                    // Color by category transaction type
                    val catAccent = when (cat.transactionType) {
                        TransactionType.EXPENSE  -> ExpenseRed
                        TransactionType.INCOME   -> IncomeGreen
                        TransactionType.TRANSFER -> TransferBlue
                        null                     -> TransferBlue
                    }
                    CategoryFilterChip(
                        label    = cat.name,
                        selected = cat.id in localFilter.categoryIds,
                        accent   = catAccent,
                        onClick  = {
                            localFilter = if (cat.id in localFilter.categoryIds)
                                localFilter.copy(categoryIds = localFilter.categoryIds - cat.id)
                            else
                                localFilter.copy(categoryIds = localFilter.categoryIds + cat.id)
                        }
                    )
                }
                if (showMoreCat) {
                    OverflowChip(expanded = expandCat) { expandCat = !expandCat }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Payment mode section ──────────────────────────────────────────
            SectionHeader(
                label = "Payment mode",
                isAllSelected = modes.isNotEmpty() &&
                        localFilter.paymentModeIds.containsAll(modes.map { it.id }),
                onSelectAll = {
                    val allIds = modes.map { it.id }.toSet()
                    localFilter = if (localFilter.paymentModeIds.containsAll(allIds.toList()))
                        localFilter.copy(paymentModeIds = emptyList())
                    else
                        localFilter.copy(paymentModeIds = localFilter.paymentModeIds + allIds)
                }
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                visibleModes.forEach { mode ->
                    CompactFilterChip(
                        label = mode.displayLabel,
                        selected = mode.id in localFilter.paymentModeIds,
                        onClick = {
                            localFilter = if (mode.id in localFilter.paymentModeIds)
                                localFilter.copy(paymentModeIds = localFilter.paymentModeIds - mode.id)
                            else
                                localFilter.copy(paymentModeIds = localFilter.paymentModeIds + mode.id)
                        }
                    )
                }
                if (showMoreMode) {
                    OverflowChip(expanded = expandMode) { expandMode = !expandMode }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Tags section ──────────────────────────────────────────────────
            if (allTags.isNotEmpty()) {
                SectionHeader(
                    label = "Tags",
                    isAllSelected = localFilter.tags.containsAll(allTags.map { it.name }),
                    trailingContent = {
                        // Mode dropdown with 3 options matching screenshot
                        Box {
                            Row(
                                modifier = Modifier
                                    .clickable { showTagMatchMenu = true }
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    when (tagsMode) {
                                        TagsMode.INCLUDES_ANY -> "Includes any"
                                        TagsMode.INCLUDES_ALL -> "Includes all"
                                        TagsMode.EXCLUDES     -> "Excludes"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Icon(
                                    Icons.Default.KeyboardArrowDown,
                                    null, Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            DropdownMenu(
                                expanded = showTagMatchMenu,
                                onDismissRequest = { showTagMatchMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text("Includes any",
                                                style = MaterialTheme.typography.bodyMedium)
                                            Text("Shows txns having any selected tags",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    },
                                    onClick = {
                                        tagsMode = TagsMode.INCLUDES_ANY
                                        localFilter = localFilter.copy(tagsMode = TagsMode.INCLUDES_ANY)
                                        showTagMatchMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text("Includes all",
                                                style = MaterialTheme.typography.bodyMedium)
                                            Text("Shows txns having all the selected tags",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    },
                                    onClick = {
                                        tagsMode = TagsMode.INCLUDES_ALL
                                        localFilter = localFilter.copy(tagsMode = TagsMode.INCLUDES_ALL)
                                        showTagMatchMenu = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text("Excludes",
                                                style = MaterialTheme.typography.bodyMedium)
                                            Text("Shows txns not having the selected tags",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    },
                                    onClick = {
                                        tagsMode = TagsMode.EXCLUDES
                                        localFilter = localFilter.copy(tagsMode = TagsMode.EXCLUDES)
                                        showTagMatchMenu = false
                                    }
                                )
                            }
                        }
                    },
                    onSelectAll = {
                        val allTagNames = allTags.map { it.name }.toSet()
                        localFilter = if (localFilter.tags.containsAll(allTagNames))
                            localFilter.copy(tags = emptyList())
                        else
                            localFilter.copy(tags = localFilter.tags + allTagNames)
                    }
                )
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    visibleTags.forEach { tag ->
                        TagFilterChip(
                            name     = tag.name,
                            selected = tag.name in localFilter.tags,
                            onClick  = {
                                localFilter = if (tag.name in localFilter.tags)
                                    localFilter.copy(tags = localFilter.tags - tag.name)
                                else
                                    localFilter.copy(tags = localFilter.tags + tag.name)
                            }
                        )
                    }
                    if (showMoreTags) {
                        OverflowChip(expanded = expandTags) { expandTags = !expandTags }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // ── Apply button ──────────────────────────────────────────────────
            Button(
                onClick = { onApply(localFilter); onDismiss() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Apply Filters", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ── Filter sheet helpers ──────────────────────────────────────────────────────

@Composable
private fun SectionHeader(
    label: String,
    trailingLabel: String? = null,
    trailingContent: @Composable (() -> Unit)? = null,
    onTrailingClick: (() -> Unit)? = null,
    onSelectAll: (() -> Unit)? = null,
    isAllSelected: Boolean = false    // drives the filled/empty circle icon
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        if (trailingContent != null) {
            trailingContent()
            Spacer(Modifier.width(8.dp))
        }
        if (trailingLabel != null) {
            Text(
                trailingLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = if (onTrailingClick != null)
                    Modifier.clickable { onTrailingClick() } else Modifier
            )
            Spacer(Modifier.width(12.dp))
        }
        if (onSelectAll != null) {
            Row(
                modifier = Modifier.clickable { onSelectAll() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Empty circle when nothing selected, filled check when all selected
                Icon(
                    if (isAllSelected) Icons.Default.CheckCircle
                    else Icons.Default.RadioButtonUnchecked,
                    null,
                    Modifier.size(16.dp),
                    tint = if (isAllSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Select all",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isAllSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isAllSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun CompactFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(
                color = if (selected) MaterialTheme.colorScheme.secondaryContainer
                else androidx.compose.ui.graphics.Color.Transparent,
                shape = RoundedCornerShape(50)
            )
            .then(
                if (!selected) Modifier.border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(50)
                ) else Modifier
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
            else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ── Category chip: colored border/text based on category type ────────────────
@Composable
private fun CategoryFilterChip(
    label:    String,
    selected: Boolean,
    accent:   androidx.compose.ui.graphics.Color,
    onClick:  () -> Unit
) {
    val borderColor = accent.copy(alpha = if (selected) 1f else 0.55f)
    val bgColor     = if (selected) accent.copy(alpha = 0.18f)
    else androidx.compose.ui.graphics.Color.Transparent
    val textColor   = if (selected) accent else accent.copy(alpha = 0.85f)
    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(50))
            .border(1.dp, borderColor, RoundedCornerShape(50))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style      = MaterialTheme.typography.bodySmall,
            color      = textColor,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines   = 1,
            overflow   = TextOverflow.Ellipsis
        )
    }
}

// ── Tag chip: outlined pill, teal # prefix, exactly matching screenshot ───────
@Composable
private fun TagFilterChip(
    name:     String,
    selected: Boolean,
    onClick:  () -> Unit
) {
    // Teal color for the # symbol (matching screenshot)
    val hashColor = androidx.compose.ui.graphics.Color(0xFF26C6DA)
    val bgColor   = if (selected) MaterialTheme.colorScheme.surfaceVariant
    else androidx.compose.ui.graphics.Color.Transparent
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = if (selected) 0.8f else 0.4f)

    Row(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(50))
            .border(1.dp, borderColor, RoundedCornerShape(50))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            "#",
            style      = MaterialTheme.typography.bodySmall,
            color      = hashColor,
            fontWeight = FontWeight.Bold
        )
        Text(
            name,
            style      = MaterialTheme.typography.bodySmall,
            color      = if (selected) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            maxLines   = 1,
            overflow   = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun OverflowChip(expanded: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(50)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            if (expanded) "‹ less" else "…",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}