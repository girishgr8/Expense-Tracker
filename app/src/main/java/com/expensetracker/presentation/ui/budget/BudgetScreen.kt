package com.expensetracker.presentation.ui.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.data.repository.AuthManager
import com.expensetracker.data.repository.BudgetRepository
import com.expensetracker.data.repository.CategoryRepository
import com.expensetracker.data.repository.TransactionRepository
import com.expensetracker.domain.model.Budget
import com.expensetracker.domain.model.BudgetPeriod
import com.expensetracker.domain.model.BudgetProgress
import com.expensetracker.domain.model.Category
import com.expensetracker.domain.model.TransactionType
import com.expensetracker.presentation.components.CategoryIconBubble
import com.expensetracker.presentation.components.EmptyState
import com.expensetracker.presentation.theme.ExpenseRed
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.Locale
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
                _uiState.update {
                    it.copy(categories = cats.filter { c ->
                        c.transactionType == TransactionType.EXPENSE || c.transactionType == null
                    })
                }
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
            Pair(
                LocalDateTime.of(budget.year, 1, 1, 0, 0),
                LocalDateTime.of(budget.year, 12, 31, 23, 59, 59)
            )
        }
    }

    fun showDialog(budget: Budget? = null) =
        _uiState.update { it.copy(showDialog = true, editingBudget = budget) }

    fun hideDialog() =
        _uiState.update { it.copy(showDialog = false, editingBudget = null) }

    fun saveBudget(
        name: String, limit: Double, period: BudgetPeriod,
        year: Int, month: Int?, categoryIds: List<Long>
    ) {
        viewModelScope.launch {
            val editing = _uiState.value.editingBudget
            val budget = Budget(
                id = editing?.id ?: 0, name = name, totalLimit = limit,
                period = period, year = year, month = month,
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

// ─── Budget List Screen ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddBudget: (() -> Unit)? = null,
    viewModel: BudgetViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Budget", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onNavigateToAddBudget?.invoke() ?: viewModel.showDialog() },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Set Budget") }
            )
        }
    ) { padding ->
        if (uiState.budgets.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(padding), contentAlignment = Alignment.Center
            ) {
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
                    BudgetProgressCard(
                        progress = progress,
                        onEdit = {
                            onNavigateToAddBudget?.invoke() ?: viewModel.showDialog(progress.budget)
                        },
                        onDelete = { viewModel.deleteBudget(progress.budget) }
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    if (uiState.showDialog) {
        AddBudgetScreen(
            onNavigateBack = viewModel::hideDialog,
            editingBudget = uiState.editingBudget,
            viewModel = viewModel
        )
    }
}

// ─── Add / Edit Budget — full-screen composable ───────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBudgetScreen(
    onNavigateBack: () -> Unit,
    editingBudget: Budget? = null,
    viewModel: BudgetViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val categories = uiState.categories

    val now = YearMonth.now()
    val monthNames = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )
    val monthShort = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )

    var isMonthly by remember { mutableStateOf(editingBudget?.period != BudgetPeriod.YEARLY) }
    var selectedYear by remember { mutableIntStateOf(editingBudget?.year ?: now.year) }
    var selectedMonth by remember { mutableIntStateOf(editingBudget?.month ?: now.monthValue) }
    var limitInput by remember {
        mutableStateOf(editingBudget?.totalLimit?.let {
            if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString()
        } ?: "")
    }

    // All expense categories included by default; track the excluded ones
    var excludedIds by remember(categories) {
        mutableStateOf(
            if (editingBudget != null)
                (categories.map { it.id }.toSet() - editingBudget.applicableCategoryIds.toSet())
            else
                emptySet()
        )
    }

    // Bottom sheet to pick included categories
    var showCategorySheet by remember { mutableStateOf(false) }

    val includedCategories = categories.filter { it.id !in excludedIds }

    // Pages of 4 for the horizontal pager preview
    val categoryPages = categories.chunked(4)
    val pagerState = rememberPagerState(pageCount = { categoryPages.size.coerceAtLeast(1) })

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (editingBudget != null) "Edit budget" else "Add budget",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Box(
                            Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", Modifier.size(18.dp))
                        }
                    }
                },
                actions = {
                    if (editingBudget != null) {
                        IconButton(onClick = {
                            viewModel.deleteBudget(editingBudget)
                            onNavigateBack()
                        }) {
                            Box(
                                Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Delete, "Delete",
                                    Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    } else {
                        IconButton(onClick = {}) {
                            Box(
                                Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Help, "Help", Modifier.size(18.dp))
                            }
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                }
            )
        },
        bottomBar = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Button(
                    onClick = {
                        val limit = limitInput.toDoubleOrNull() ?: return@Button
                        val includedIds = categories
                            .filter { it.id !in excludedIds }
                            .map { it.id }
                        val budgetName = if (isMonthly)
                            "${monthNames[selectedMonth - 1]} $selectedYear Budget"
                        else "$selectedYear Budget"
                        viewModel.saveBudget(
                            name = budgetName,
                            limit = limit,
                            period = if (isMonthly) BudgetPeriod.MONTHLY else BudgetPeriod.YEARLY,
                            year = selectedYear,
                            month = if (isMonthly) selectedMonth else null,
                            categoryIds = includedIds
                        )
                        onNavigateBack()
                    },
                    enabled = limitInput.toDoubleOrNull() != null && limitInput.isNotBlank(),
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    )
                ) {
                    Text(
                        "Next",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Monthly / Yearly pill toggle ──────────────────────────────────
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                BudgetPeriodToggle(
                    isMonthly = isMonthly,
                    onMonthly = { isMonthly = true },
                    onYearly = { isMonthly = false }
                )
            }

            // ── "Budget for" card ─────────────────────────────────────────────
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Budget for",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    // Date display row
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.CalendarMonth, null,
                                Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                if (isMonthly) "${monthNames[selectedMonth - 1]} $selectedYear"
                                else "$selectedYear",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Box(
                            Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.ContentCopy, null, Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Month strip (Monthly only)
                    if (isMonthly) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 2.dp)
                        ) {
                            items(monthShort.size) { idx ->
                                val m = idx + 1
                                val sel = m == selectedMonth
                                Box(
                                    Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (sel) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surface
                                        )
                                        .clickable { selectedMonth = m }
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        monthShort[idx],
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                        color = if (sel) Color.White
                                        else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    // Year strip
                    val years = (now.year - 2..now.year + 2).toList()
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 2.dp)
                    ) {
                        items(years) { y ->
                            val sel = y == selectedYear
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (sel) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surface
                                    )
                                    .clickable { selectedYear = y }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "$y",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                    color = if (sel) Color.White
                                    else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // ── Budget limit card ─────────────────────────────────────────────
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "What's your total budget limit?",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.CurrencyRupee, null, Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        BudgetLimitField(
                            value = limitInput,
                            onChange = { limitInput = it },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // ── Included categories card ──────────────────────────────────────
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Header row
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { showCategorySheet = true },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Included Categories",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "All categories included in your budget",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.Default.ChevronRight, null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(Modifier.height(1.dp))

                    if (categories.isEmpty()) {
                        Text(
                            "No categories found",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        // HorizontalPager — 4 items per page, swipeable
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxWidth()
                        ) { page ->
                            val pageCats = categoryPages.getOrElse(page) { emptyList() }
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                pageCats.forEach { cat ->
                                    val included = cat.id !in excludedIds
                                    CategoryCircleItem(
                                        category = cat,
                                        included = included,
                                        onClick = { showCategorySheet = true },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                // Fill empty slots so layout stays aligned
                                repeat(4 - pageCats.size) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }

                        // Dot indicators
                        if (categoryPages.size > 1) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                repeat(categoryPages.size) { idx ->
                                    val selected = pagerState.currentPage == idx
                                    Box(
                                        Modifier
                                            .padding(horizontal = 4.dp)
                                            .size(if (selected) 10.dp else 7.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (selected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                                    .copy(alpha = 0.3f)
                                            )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }

    // ── Category selection bottom sheet (image 3) ─────────────────────────────
    if (showCategorySheet) {
        CategorySelectionSheet(
            categories = categories,
            excludedIds = excludedIds,
            onDismiss = { showCategorySheet = false },
            onConfirm = { newExcluded ->
                excludedIds = newExcluded
                showCategorySheet = false
            }
        )
    }
}

// ─── Period toggle pill ───────────────────────────────────────────────────────

@Composable
private fun BudgetPeriodToggle(
    isMonthly: Boolean,
    onMonthly: () -> Unit,
    onYearly: () -> Unit
) {
    Box(
        Modifier
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(Modifier.padding(4.dp)) {
            PeriodPill(label = "Monthly", selected = isMonthly, onClick = onMonthly)
            PeriodPill(label = "Yearly", selected = !isMonthly, onClick = onYearly)
        }
    }
}

@Composable
private fun PeriodPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent
            )
            .clickable { onClick() }
            .padding(horizontal = 32.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.surface
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ─── Circle category item in pager ───────────────────────────────────────────

@Composable
private fun CategoryCircleItem(
    category: Category,
    included: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(Modifier.alpha(if (included) 1f else 0.25f)) {
            CategoryIconBubble(category = category, size = 50)
        }
        Text(
            category.name,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
                .copy(alpha = if (included) 1f else 0.25f),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ─── Category selection bottom sheet (image 3) ───────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategorySelectionSheet(
    categories: List<Category>,
    excludedIds: Set<Long>,
    onDismiss: () -> Unit,
    onConfirm: (Set<Long>) -> Unit
) {
    var localExcluded by remember { mutableStateOf(excludedIds) }
    var searchQuery by remember { mutableStateOf("") }

    val filtered = categories.filter {
        it.name.contains(searchQuery, ignoreCase = true)
    }
    val allSelected = localExcluded.isEmpty()
    val selectedCount = categories.size - localExcluded.size

    ModalBottomSheet(
        onDismissRequest = { onConfirm(localExcluded) },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .navigationBarsPadding()
        ) {
            // ── Sheet header ──────────────────────────────────────────────────
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp, top = 20.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Select Included Categories",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { onConfirm(localExcluded) }) {
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Close, "Close", Modifier.size(16.dp))
                    }
                }
            }

            // ── Search ────────────────────────────────────────────────────────
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Search, null, Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { inner ->
                        if (searchQuery.isEmpty()) Text(
                            "Search",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        inner()
                    }
                )
                if (searchQuery.isNotEmpty()) {
                    Icon(
                        Icons.Default.Close, null, Modifier
                            .size(16.dp)
                            .clickable { searchQuery = "" },
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Selected count + Select all ───────────────────────────────────
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "$selectedCount categories selected",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    Modifier.clickable {
                        localExcluded = if (allSelected) categories.map { it.id }.toSet()
                        else emptySet()
                    },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        Modifier
                            .size(22.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(
                                if (allSelected) Color.White
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .border(
                                1.5.dp,
                                if (allSelected) Color.White
                                else MaterialTheme.colorScheme.outline,
                                RoundedCornerShape(5.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (allSelected) {
                            Icon(
                                Icons.Default.Check, null, Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Text(
                        "Select all",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            HorizontalDivider(
                Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )

            // ── Category list ─────────────────────────────────────────────────
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered, key = { it.id }) { cat ->
                    val included = cat.id !in localExcluded
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .clickable {
                                localExcluded = if (included)
                                    localExcluded + cat.id
                                else
                                    localExcluded - cat.id
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        CategoryIconBubble(category = cat, size = 40)
                        Text(
                            cat.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        // Checkbox
                        Box(
                            Modifier
                                .size(22.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(
                                    if (included) Color.White
                                    else MaterialTheme.colorScheme.surface
                                )
                                .border(
                                    1.5.dp,
                                    if (included) Color.White
                                    else MaterialTheme.colorScheme.outline,
                                    RoundedCornerShape(5.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (included) {
                                Icon(
                                    Icons.Default.Check, null, Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // ── Done button ───────────────────────────────────────────────────
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Button(
                    onClick = { onConfirm(localExcluded) },
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .wrapContentWidth()
                        .align(Alignment.BottomEnd)
                        .height(40.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White, contentColor = Color.Black
                    )
                ) {
                    Text(
                        "Done",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ─── Borderless limit input ───────────────────────────────────────────────────

@Composable
private fun BudgetLimitField(value: String, onChange: (String) -> Unit, modifier: Modifier) {
    BasicTextField(
        value = value,
        onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) onChange(it) },
        modifier = modifier.padding(vertical = 12.dp),
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onSurface
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { inner ->
            if (value.isEmpty()) Text(
                "Enter your total budget limit",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            inner()
        }
    )
}

// ─── Budget progress card (list screen) ──────────────────────────────────────

@Composable
private fun BudgetProgressCard(
    progress: BudgetProgress, onEdit: () -> Unit, onDelete: () -> Unit
) {
    var showDelete by remember { mutableStateOf(false) }
    val pct = progress.percentage.coerceIn(0f, 100f)
    val color = when {
        pct >= 90 -> ExpenseRed
        pct >= 70 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }
    val monthNames = listOf(
        "", "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )

    Card(
        Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(
                        progress.budget.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    val periodLabel = if (progress.budget.period == BudgetPeriod.MONTHLY &&
                        progress.budget.month != null
                    )
                        "${monthNames.getOrElse(progress.budget.month) { "" }} ${progress.budget.year}"
                    else "${progress.budget.year}"
                    Text(
                        periodLabel, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, null, Modifier.size(16.dp))
                    }
                    IconButton(onClick = { showDelete = true }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Delete, null, Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "Spent: ₹${String.format(Locale.getDefault(), "%,.0f", progress.spent)}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Limit: ₹${
                        String.format(
                            Locale.getDefault(),
                            "%,.0f",
                            progress.budget.totalLimit
                        )
                    }",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { pct / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp)),
                color = color,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "${pct.toInt()}% used",
                    style = MaterialTheme.typography.labelSmall, color = color
                )
                Text(
                    "₹${
                        String.format(
                            Locale.getDefault(),
                            "%,.0f",
                            (progress.budget.totalLimit - progress.spent).coerceAtLeast(0.0)
                        )
                    } left",
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
            dismissButton = {
                TextButton(onClick = { showDelete = false }) { Text("Cancel") }
            }
        )
    }
}