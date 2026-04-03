package com.expensetracker.presentation.ui.budget

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.toColorInt
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
import com.expensetracker.presentation.theme.IncomeGreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.min

// ─── ViewModel ────────────────────────────────────────────────────────────────

data class BudgetCategoryBreakdown(
    val category: Category,
    val spent: Double,
    val budgetLimit: Double? = null
)

data class BudgetUiState(
    val budgets: List<BudgetProgress> = emptyList(),
    val categories: List<Category> = emptyList(),
    val showDialog: Boolean = false,
    val editingBudget: Budget? = null,
    val selectedBudgetAnalysis: BudgetProgress? = null,
    val analysisBreakdown: List<BudgetCategoryBreakdown> = emptyList(),
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

    fun loadBudgetById(budgetId: Long) {
        if (budgetId <= 0L) {
            _uiState.update { it.copy(editingBudget = null) }
            return
        }
        viewModelScope.launch {
            val budget = budgetRepository.getBudgetById(budgetId)
            _uiState.update { it.copy(editingBudget = budget) }
        }
    }

    fun clearEditingBudget() {
        _uiState.update { it.copy(editingBudget = null) }
    }

    fun saveBudget(
        name: String, limit: Double, period: BudgetPeriod,
        year: Int, month: Int?, categoryIds: List<Long>,
        categoryLimits: Map<Long, Double> = emptyMap()
    ) {
        viewModelScope.launch {
            val editing = _uiState.value.editingBudget
            val budget = Budget(
                id = editing?.id ?: 0, name = name, totalLimit = limit,
                period = period, year = year, month = month,
                applicableCategoryIds = categoryIds,
                categoryLimits = categoryLimits, userId = userId
            )
            if (editing != null) budgetRepository.updateBudget(budget)
            else budgetRepository.insertBudget(budget)
            hideDialog()
        }
    }

    fun deleteBudget(budget: Budget) {
        viewModelScope.launch { budgetRepository.deleteBudget(budget) }
    }

    fun openBudgetAnalysis(progress: BudgetProgress) {
        viewModelScope.launch {
            val budget = progress.budget
            val categories = _uiState.value.categories
            val includedCategories = categories.filter { it.id in budget.applicableCategoryIds }
            val (start, end) = getBudgetDateRange(budget)
            val txns = transactionRepository.getAllTransactionsOneShot(userId)
            val breakdown = includedCategories.map { category ->
                val spent = txns
                    .filter { it.type == TransactionType.EXPENSE }
                    .filter { it.categoryId == category.id }
                    .filter { !it.dateTime.isBefore(start) && !it.dateTime.isAfter(end) }
                    .sumOf { it.amount }

                BudgetCategoryBreakdown(
                    category = category,
                    spent = spent,
                    budgetLimit = budget.categoryLimits[category.id]
                )
            }.sortedByDescending { it.spent }

            _uiState.update {
                it.copy(
                    selectedBudgetAnalysis = progress,
                    analysisBreakdown = breakdown
                )
            }
        }
    }

    fun closeBudgetAnalysis() {
        _uiState.update {
            it.copy(
                selectedBudgetAnalysis = null,
                analysisBreakdown = emptyList()
            )
        }
    }
}

// ─── Budget List Screen ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddBudget: ((Long) -> Unit)? = null,
    viewModel: BudgetViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var isMonthly by remember { mutableStateOf(true) }
    val now = YearMonth.now()

    val filteredBudgets = uiState.budgets.filter { p ->
        if (isMonthly) p.budget.period == BudgetPeriod.MONTHLY
        else p.budget.period == BudgetPeriod.YEARLY
    }
    val activeBudget = filteredBudgets.find { p ->
        if (isMonthly) p.budget.year == now.year && p.budget.month == now.monthValue
        else p.budget.year == now.year
    }
    val upcomingBudget = filteredBudgets.find { p ->
        val budgetYM = if (isMonthly && p.budget.month != null)
            YearMonth.of(p.budget.year, p.budget.month)
        else YearMonth.of(p.budget.year, 1)
        budgetYM.isAfter(now)
    }
    val pastBudgets = filteredBudgets.filter { p ->
        val budgetYM = if (isMonthly && p.budget.month != null)
            YearMonth.of(p.budget.year, p.budget.month)
        else YearMonth.of(p.budget.year, 1)
        budgetYM.isBefore(now)
    }.sortedByDescending { it.budget.year * 100 + (it.budget.month ?: 0) }

    uiState.selectedBudgetAnalysis?.let { selected ->
        BudgetAnalysisScreen(
            progress = selected,
            breakdown = uiState.analysisBreakdown,
            onNavigateBack = viewModel::closeBudgetAnalysis,
            onEdit = {
                viewModel.closeBudgetAnalysis()
                if (onNavigateToAddBudget != null) {
                    onNavigateToAddBudget(selected.budget.id)
                } else {
                    viewModel.showDialog(selected.budget)
                }
            }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Budgets", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Box(
                            Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, Modifier.size(18.dp))
                        }
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Box(
                            Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Help, null, Modifier.size(18.dp))
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToAddBudget?.invoke(-1L) ?: viewModel.showDialog() },
                shape = CircleShape,
                containerColor = Color.White,
                contentColor = Color.Black
            ) {
                Icon(Icons.Default.Add, null, Modifier.size(28.dp))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                BudgetPeriodToggle(
                    isMonthly = isMonthly,
                    onMonthly = { isMonthly = true },
                    onYearly = { isMonthly = false }
                )
            }

            if (activeBudget != null) {
                item {
                    SectionLabel("Active")
                    Spacer(Modifier.height(8.dp))
                    ActiveBudgetCard(
                        progress = activeBudget,
                        onOpen = { viewModel.openBudgetAnalysis(activeBudget) },
                        onEdit = {
                            if (onNavigateToAddBudget != null)
                                onNavigateToAddBudget(activeBudget.budget.id)
                            else
                                viewModel.showDialog(activeBudget.budget)
                        }
                    )
                }
            }

            item {
                SectionLabel("Upcoming")
                Spacer(Modifier.height(8.dp))
                if (upcomingBudget != null) {
                    UpcomingBudgetCard(
                        progress = upcomingBudget,
                        onEdit = {
                            if (onNavigateToAddBudget != null)
                                onNavigateToAddBudget(upcomingBudget.budget.id)
                            else
                                viewModel.showDialog(upcomingBudget.budget)
                        }
                    )
                } else {
                    PlanAheadCard(
                        onAddBudget = {
                            onNavigateToAddBudget?.invoke(-1L) ?: viewModel.showDialog()
                        }
                    )
                }
            }

            if (pastBudgets.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(4.dp))
                    SectionLabel("Past Budgets")
                    Spacer(Modifier.height(2.dp))
                }
                items(pastBudgets, key = { it.budget.id }) { progress ->
                    PastBudgetCard(
                        progress = progress,
                        onOpen = { viewModel.openBudgetAnalysis(progress) },
                        onDelete = { viewModel.deleteBudget(progress.budget) }
                    )
                }
            }

            if (activeBudget == null && pastBudgets.isEmpty()) {
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyState(
                            icon = Icons.Default.PieChart,
                            title = "No budgets yet",
                            subtitle = "Tap + to set your first budget"
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
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

// ─── Section label ────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 2.dp)
    )
}

// ─── Active budget card with arc gauge ───────────────────────────────────────

@Composable
private fun ActiveBudgetCard(
    progress: BudgetProgress,
    onOpen: () -> Unit,
    onEdit: () -> Unit
) {
    val monthNames = listOf(
        "", "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )
    val shortMonth = listOf(
        "", "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )

    val pct = progress.percentage.coerceIn(0f, 100f)
    val spent = progress.spent
    val limit = progress.budget.totalLimit
    val isExceeded = (limit - spent) < 0
    val remaining = abs((limit - spent))

    val arcColor = when {
        pct >= 90 -> ExpenseRed
        pct >= 70 -> MaterialTheme.colorScheme.tertiary
        else -> IncomeGreen
    }
    val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)

    val today = LocalDate.now()
    val daysInMonth = today.lengthOfMonth()
    val daysRemaining = (daysInMonth - today.dayOfMonth + 1).coerceAtLeast(1)
    val safePerDay = remaining / daysRemaining

    val title = if (progress.budget.period == BudgetPeriod.MONTHLY && progress.budget.month != null)
        "${monthNames.getOrElse(progress.budget.month) { "" }} ${progress.budget.year}"
    else "${progress.budget.year}"

    val safeMonth =
        if (progress.budget.period == BudgetPeriod.MONTHLY && progress.budget.month != null)
            shortMonth.getOrElse(progress.budget.month) { "" }
        else ""

    Card(
        Modifier
            .fillMaxWidth()
            .clickable { onOpen() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                title, style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                contentAlignment = Alignment.Center
            ) {
                val sweepDeg = 180f * (pct / 100f)
                Canvas(
                    Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                ) {
                    val strokeWidth = 20.dp.toPx()
                    val diameter = min(size.width, size.height * 2f) - strokeWidth
                    val topLeft = Offset(
                        x = (size.width - diameter) / 2f,
                        y = size.height - diameter / 2f - strokeWidth / 2f
                    )
                    val arcSize = Size(diameter, diameter)
                    drawArc(
                        color = trackColor,
                        startAngle = 180f, sweepAngle = 180f,
                        useCenter = false, topLeft = topLeft, size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    if (sweepDeg > 0f) {
                        drawArc(
                            color = arcColor,
                            startAngle = 180f, sweepAngle = sweepDeg,
                            useCenter = false, topLeft = topLeft, size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 48.dp)
                ) {

                    Text(
                        text = if (isExceeded) "EXCEEDED" else "REMAINING",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isExceeded) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 2.sp
                    )

                    Text(
                        text = "₹${String.format(Locale.getDefault(), "%,.0f", remaining)}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isExceeded) ExpenseRed else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(
                        "Spent", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "₹${String.format(Locale.getDefault(), "%,.2f", spent)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "Limit", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(
                            Icons.Default.Edit, null,
                            Modifier
                                .size(12.dp)
                                .clickable { onEdit() },
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        "₹${String.format(Locale.getDefault(), "%,.0f", limit)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(Modifier.height(10.dp))

            if (isExceeded) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Limit crossed.",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = arcColor
                    )
                    Text(
                        " Avoid all non-essential spending.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Safe to spend: ", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "₹${String.format(Locale.getDefault(), "%,.0f", safePerDay)}/day",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = arcColor
                    )
                    Text(
                        if (safeMonth.isNotEmpty()) " for rest of $safeMonth." else " remaining.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ─── Upcoming budget card ─────────────────────────────────────────────────────

@Composable
private fun UpcomingBudgetCard(progress: BudgetProgress, onEdit: () -> Unit) {
    val monthNames = listOf(
        "", "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )
    val title = if (progress.budget.period == BudgetPeriod.MONTHLY && progress.budget.month != null)
        "${monthNames.getOrElse(progress.budget.month) { "" }} ${progress.budget.year}"
    else "${progress.budget.year}"

    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    title, style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Budget: ₹${
                        String.format(
                            Locale.getDefault(),
                            "%,.0f",
                            progress.budget.totalLimit
                        )
                    }",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.Edit, null, Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ─── Plan Ahead card ─────────────────────────────────────────────────────────

@Composable
private fun PlanAheadCard(onAddBudget: () -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("\uD83D\uDCC5", fontSize = 24.sp)
                Column {
                    Text(
                        "Plan Ahead", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Plan your budget for the next month and stay on top of your finances.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = onAddBudget,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                )
            ) {
                Text(
                    "Add Budget", fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

// ─── Past budget compact card ─────────────────────────────────────────────────

@Composable
private fun PastBudgetCard(
    progress: BudgetProgress,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    val monthNames = listOf(
        "", "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    val pct = progress.percentage.coerceIn(0f, 100f)
    val spent = progress.spent
    val limit = progress.budget.totalLimit
    val saved = (limit - spent).coerceAtLeast(0.0)

    val title = if (progress.budget.period == BudgetPeriod.MONTHLY && progress.budget.month != null)
        "${monthNames.getOrElse(progress.budget.month) { "" }} ${progress.budget.year}"
    else "${progress.budget.year}"

    val progressColor = Color(0xFF00C896) // exact green tone from UI

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {

            // ─── TOP ROW ─────────────────────────────────────
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                // LEFT SIDE
                Column {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        "Budget: ₹${
                            String.format(
                                Locale.getDefault(),
                                "%,.0f",
                                limit
                            )
                        }",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // RIGHT SIDE
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        "₹${String.format(Locale.getDefault(), "%,.0f", saved)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        "Saved",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        "${pct.toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = progressColor
                    )
                }
            }

            Spacer(Modifier.height(2.dp))

            // ─── PROGRESS BAR ───────────────────────────────
            LinearProgressIndicator(
                progress = { pct / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50)),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BudgetAnalysisScreen(
    progress: BudgetProgress,
    breakdown: List<BudgetCategoryBreakdown>,
    onNavigateBack: () -> Unit,
    onEdit: () -> Unit
) {
    BackHandler(onBack = onNavigateBack)

    val budget = progress.budget
    val title = if (budget.period == BudgetPeriod.MONTHLY && budget.month != null) {
        "${
            java.time.Month.of(budget.month)
                .getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault())
        } ${budget.year}"
    } else {
        budget.year.toString()
    }
    val spent = progress.spent
    val limit = budget.totalLimit
    val remaining = abs((limit - spent))
    val budgetedCategories = breakdown.filter { (it.budgetLimit ?: 0.0) > 0.0 }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Budget Analysis", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Box(
                            Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, Modifier.size(18.dp))
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onEdit,
                shape = CircleShape,
                containerColor = Color.White,
                contentColor = Color.Black
            ) {
                Icon(Icons.Default.Edit, null)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                BudgetSummaryCard(
                    title = title,
                    spent = spent,
                    limit = limit,
                    remaining = remaining
                )
            }

            if (budgetedCategories.isNotEmpty()) {
                item {
                    BudgetAnalysisHeader(
                        title = "Budgeted categories",
                        countLabel = "${budgetedCategories.size} categor${if (budgetedCategories.size == 1) "y" else "ies"}"
                    )
                }
                items(budgetedCategories, key = { it.category.id }) { item ->
                    BudgetedCategoryCard(item = item)
                }
            }

            if (breakdown.isNotEmpty()) {
                item {
                    BudgetAnalysisHeader(
                        title = "Included categories",
                        countLabel = "${breakdown.size} categor${if (breakdown.size == 1) "y" else "ies"}"
                    )
                }
                item {
                    IncludedCategoriesCard(breakdown = breakdown)
                }
            }

            item { Spacer(Modifier.height(88.dp)) }
        }
    }
}

@Composable
private fun BudgetAnalysisHeader(title: String, countLabel: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(
            countLabel,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BudgetSummaryCard(
    title: String,
    spent: Double,
    limit: Double,
    remaining: Double
) {
    val pct = if (limit > 0) (spent * 100 / limit).toFloat().coerceIn(0f, 100f) else 0f
    val isExceeded = (limit - spent) < 0
    val arcColor = when {
        pct >= 90 -> ExpenseRed
        pct >= 70 -> MaterialTheme.colorScheme.tertiary
        else -> IncomeGreen
    }

    val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                title, style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                contentAlignment = Alignment.Center
            ) {
                val sweepDeg = 180f * (pct / 100f)
                Canvas(
                    Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                ) {
                    val strokeWidth = 20.dp.toPx()
                    val diameter = min(size.width, size.height * 2f) - strokeWidth
                    val topLeft = Offset(
                        x = (size.width - diameter) / 2f,
                        y = size.height - diameter / 2f - strokeWidth / 2f
                    )
                    val arcSize = Size(diameter, diameter)
                    drawArc(
                        color = trackColor,
                        startAngle = 180f, sweepAngle = 180f,
                        useCenter = false, topLeft = topLeft, size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    if (sweepDeg > 0f) {
                        drawArc(
                            color = arcColor,
                            startAngle = 180f, sweepAngle = sweepDeg,
                            useCenter = false, topLeft = topLeft, size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 48.dp)
                ) {
                    Text(
                        text = if (isExceeded) "EXCEEDED" else "REMAINING",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isExceeded) ExpenseRed else MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 2.sp
                    )
                    Text(
                        "₹${String.format(Locale.getDefault(), "%,.0f", remaining)}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(
                        "Spent", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "₹${String.format(Locale.getDefault(), "%,.2f", spent)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "Limit", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        "₹${String.format(Locale.getDefault(), "%,.0f", limit)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun BudgetedCategoryCard(item: BudgetCategoryBreakdown) {
    val budgetLimit = item.budgetLimit ?: return
    val progress =
        if (budgetLimit > 0) (item.spent / budgetLimit).toFloat().coerceAtLeast(0f) else 0f
    val progressColor = when {
        progress >= 1f -> ExpenseRed
        progress >= 0.85f -> MaterialTheme.colorScheme.tertiary
        else -> IncomeGreen
    }
    val delta = budgetLimit - item.spent

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    CategoryIconBubble(category = item.category, size = 40)
                    Column {
                        Text(
                            item.category.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Budget: ₹${formatBudgetAmount(budgetLimit)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "₹${formatBudgetAmount(item.spent)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Spent",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = progressColor
                )
            }

            LinearProgressIndicator(
                progress = { progress.coerceAtMost(1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(999.dp)),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
            )

            Text(
                if (delta >= 0) "You saved ₹${formatBudgetAmount(delta)}."
                else "You exceeded by ₹${formatBudgetAmount(-delta)}.",
                style = MaterialTheme.typography.bodySmall,
                color = if (delta >= 0) IncomeGreen else ExpenseRed,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun IncludedCategoriesCard(breakdown: List<BudgetCategoryBreakdown>) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            CategorySpendDonutChart(breakdown = breakdown)
            breakdown.forEach { item ->
                IncludedCategoryRow(item = item)
            }
        }
    }
}

@Composable
private fun CategorySpendDonutChart(breakdown: List<BudgetCategoryBreakdown>) {
    val totalSpent = breakdown.sumOf { it.spent }
    val positiveBreakdown = breakdown.filter { it.spent > 0 }
    val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)

    Box(
        Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(180.dp)) {
            val strokeWidth = 36.dp.toPx()
            var startAngle = -90f
            positiveBreakdown.forEach { item ->
                val sweep = if (totalSpent > 0) ((item.spent / totalSpent) * 360f).toFloat() else 0f
                drawArc(
                    color = budgetChartColor(item.category.colorHex),
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                )
                startAngle += sweep
            }
            if (positiveBreakdown.isEmpty()) {
                drawArc(
                    color = trackColor,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                )
            }
        }
    }
}

@Composable
private fun IncludedCategoryRow(item: BudgetCategoryBreakdown) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CategoryIconBubble(category = item.category, size = 36)
        Text(
            item.category.name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            "₹${formatBudgetAmount(item.spent)}",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

private fun budgetChartColor(colorHex: String): Color =
    runCatching { Color(colorHex.toColorInt()) }.getOrElse { Color(0xFF5E8E8E) }

// ─── Add / Edit Budget — full-screen composable ───────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBudgetScreen(
    onNavigateBack: () -> Unit,
    budgetId: Long = -1L,         // -1 = new budget; >0 = editing existing
    editingBudget: Budget? = null, // used only in inline dialog path
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

    // Load the budget to edit:
    //  - nav path:    budgetId > 0 → fetch from DB via LaunchedEffect
    //  - dialog path: editingBudget passed directly → store in VM state
    androidx.compose.runtime.LaunchedEffect(budgetId, editingBudget?.id) {
        when {
            budgetId > 0L -> viewModel.loadBudgetById(budgetId)
            editingBudget != null -> viewModel.showDialog(editingBudget)
            else -> viewModel.clearEditingBudget()
        }
    }

    // Single source of truth: VM state (populated by either path above)
    val budget = uiState.editingBudget

    // ── Form state — keyed on budget?.id so values reload when a different budget loads ──
    var isMonthly by remember(budget?.id) {
        mutableStateOf(budget?.period != BudgetPeriod.YEARLY)
    }
    var selectedYear by remember(budget?.id) {
        mutableIntStateOf(budget?.year ?: now.year)
    }
    var selectedMonth by remember(budget?.id) {
        mutableIntStateOf(budget?.month ?: now.monthValue)
    }
    var limitInput by remember(budget?.id) {
        mutableStateOf(
            budget?.totalLimit?.let {
                if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString()
            } ?: ""
        )
    }
    var categoryLimits by remember(budget?.id) {
        mutableStateOf(budget?.categoryLimits ?: emptyMap())
    }
    // Excluded IDs: keyed on budget?.id — when budget loads (async), this re-initializes
    // with the correct excluded set. categories.size ensures we wait until loaded.
    var excludedIds by remember(budget?.id, categories.size) {
        mutableStateOf(
            if (budget != null && categories.isNotEmpty())
                categories.map { it.id }.toSet() - budget.applicableCategoryIds.toSet()
            else
                emptySet()
        )
    }

    var showCategorySheet by remember { mutableStateOf(false) }
    var showCopyBudgetSheet by remember { mutableStateOf(false) }
    val includedCategories = categories.filter { it.id !in excludedIds }
    val categoryPages = categories.chunked(4)
    val pagerState = rememberPagerState(pageCount = { categoryPages.size.coerceAtLeast(1) })
    val copyableBudgets = remember(
        uiState.budgets,
        isMonthly,
        selectedYear,
        selectedMonth,
        budget?.id
    ) {
        uiState.budgets
            .map { it.budget }
            .filter { candidate ->
                val samePeriodType = if (isMonthly) {
                    candidate.period == BudgetPeriod.MONTHLY
                } else {
                    candidate.period == BudgetPeriod.YEARLY
                }
                val sameTargetPeriod = if (isMonthly) {
                    candidate.year == selectedYear && candidate.month == selectedMonth
                } else {
                    candidate.year == selectedYear
                }
                samePeriodType && !sameTargetPeriod && candidate.id != budget?.id
            }
            .sortedWith(
                compareByDescending<Budget> { it.year }
                    .thenByDescending { it.month ?: 0 }
            )
    }

    // Clean up VM state when this screen exits
    DisposableEffect(Unit) {
        onDispose { viewModel.clearEditingBudget() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (budget != null) "Edit budget" else "Add budget",
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
                    if (budget != null) {
                        IconButton(onClick = {
                            viewModel.deleteBudget(budget)
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
                var showCategoryLimits by remember { mutableStateOf(false) }

                Button(
                    onClick = { showCategoryLimits = true },
                    enabled = limitInput.toDoubleOrNull() != null && limitInput.isNotBlank(),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    )
                ) {
                    Text(
                        "Next", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (showCategoryLimits) {
                    val limit = limitInput.toDoubleOrNull() ?: 0.0
                    val includedIds = includedCategories.map { it.id }
                    val budgetName = if (isMonthly)
                        "${monthNames[selectedMonth - 1]} $selectedYear Budget"
                    else "$selectedYear Budget"
                    SetCategoryLimitsScreen(
                        totalLimit = limit,
                        includedCategories = includedCategories,
                        existingLimits = categoryLimits.filterKeys { it in includedIds },
                        budgetPeriod = if (isMonthly) BudgetPeriod.MONTHLY else BudgetPeriod.YEARLY,
                        selectedYear = selectedYear,
                        selectedMonth = if (isMonthly) selectedMonth else null,
                        onBack = { showCategoryLimits = false },
                        onSave = { catLimits ->
                            val filteredCategoryLimits = catLimits.filterKeys { it in includedIds }
                            categoryLimits = filteredCategoryLimits
                            viewModel.saveBudget(
                                name = budgetName,
                                limit = limit,
                                period = if (isMonthly) BudgetPeriod.MONTHLY else BudgetPeriod.YEARLY,
                                year = selectedYear,
                                month = if (isMonthly) selectedMonth else null,
                                categoryIds = includedIds,
                                categoryLimits = filteredCategoryLimits
                            )
                            onNavigateBack()
                        }
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
                        "Budget for", style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    var showMonthPicker by remember { mutableStateOf(false) }

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
                                .clickable { showMonthPicker = true }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.CalendarMonth, null, Modifier.size(20.dp),
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
                                .clickable(enabled = copyableBudgets.isNotEmpty()) {
                                    showCopyBudgetSheet = true
                                }
                                .background(MaterialTheme.colorScheme.surface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.ContentCopy, null, Modifier.size(20.dp),
                                tint = if (copyableBudgets.isNotEmpty()) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                                }
                            )
                        }
                    }

                    if (showMonthPicker) {
                        MonthYearPickerDialog(
                            isMonthly = isMonthly,
                            selectedMonth = selectedMonth,
                            selectedYear = selectedYear,
                            monthShort = monthShort,
                            onMonthSelect = { m -> selectedMonth = m },
                            onYearChange = { y -> selectedYear = y },
                            onDismiss = { showMonthPicker = false }
                        )
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
                            value = limitInput, onChange = { limitInput = it },
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
                            val includedCount = categories.size - excludedIds.size
                            val subtitleText = when {
                                excludedIds.isEmpty() -> "All categories included in your budget"
                                includedCount == 0 -> "No categories included"
                                else -> "$includedCount of ${categories.size} categories included"
                            }
                            Text(
                                subtitleText, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.Default.ChevronRight, null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (categories.isEmpty()) {
                        Text(
                            "No categories found", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
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
                                repeat(4 - pageCats.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
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

    if (showCopyBudgetSheet) {
        CopyBudgetSheet(
            budgets = copyableBudgets,
            isMonthly = isMonthly,
            onDismiss = { showCopyBudgetSheet = false },
            onSelect = { selectedBudget ->
                limitInput = selectedBudget.totalLimit.let {
                    if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString()
                }
                excludedIds =
                    categories.map { it.id }.toSet() - selectedBudget.applicableCategoryIds.toSet()
                categoryLimits = selectedBudget.categoryLimits
                showCopyBudgetSheet = false
            }
        )
    }
}


// ─── Set Category Limits Screen (Step 2) ─────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SetCategoryLimitsScreen(
    totalLimit: Double,
    includedCategories: List<Category>,
    existingLimits: Map<Long, Double>,
    budgetPeriod: BudgetPeriod,
    selectedYear: Int,
    selectedMonth: Int?,
    onBack: () -> Unit,
    onSave: (Map<Long, Double>) -> Unit
) {
    // Map of categoryId → limit string ("" means no limit)
    val limitInputs = remember(includedCategories.map { it.id }, existingLimits.hashCode()) {
        mutableStateMapOf<Long, String>().also { map ->
            includedCategories.forEach { cat ->
                val existing = existingLimits[cat.id]
                map[cat.id] = if (existing != null && existing > 0)
                    existing.toLong().toString() else ""
            }
        }
    }

    // Compute allocated and remaining in real-time
    val allocatedTotal by remember {
        derivedStateOf {
            limitInputs.values.sumOf { it.toDoubleOrNull() ?: 0.0 }
        }
    }
    val remaining by remember { derivedStateOf { totalLimit - allocatedTotal } }

    fun fmtAmt(d: Double): String {
        val l = d.toLong()
        return if (d == l.toDouble()) "%,d".format(l) else "%,.1f".format(d)
    }

    // Full-screen overlay using Dialog so it slides over AddBudgetScreen
    Dialog(
        onDismissRequest = onBack,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        Text("Add budget", fontWeight = FontWeight.Bold)
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Box(
                                Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    "Back",
                                    Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    actions = {
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
                            // Convert inputs → map, 0 means no limit
                            val result = limitInputs.mapValues { it.value.toDoubleOrNull() ?: 0.0 }
                            onSave(result)
                        },
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
                            "Save",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ── Header ────────────────────────────────────────────────────
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Set category-wise limits",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "(optional)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            "Set limits on categories within your budget, if you want",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // ── Total Budget / Remaining summary ──────────────────────────
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    "Total Budget",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "₹${fmtAmt(totalLimit)}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "Remaining",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "₹${fmtAmt(remaining.coerceAtLeast(0.0))}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (remaining < 0) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // ── Count + sort row ──────────────────────────────────────────
                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${includedCategories.size} categories included",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.FilterList, null, Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Sort by",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // ── Category limit rows ───────────────────────────────────────
                items(includedCategories, key = { it.id }) { cat ->
                    CategoryLimitRow(
                        category = cat,
                        limitInput = limitInputs[cat.id] ?: "",
                        onLimitChange = { v -> limitInputs[cat.id] = v },
                        budgetPeriod = budgetPeriod,
                        selectedYear = selectedYear,
                        selectedMonth = selectedMonth
                    )
                }

                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

// ─── Individual category limit row ────────────────────────────────────────────

@Composable
private fun CategoryLimitRow(
    category: Category,
    limitInput: String,
    onLimitChange: (String) -> Unit,
    budgetPeriod: BudgetPeriod,
    selectedYear: Int,
    selectedMonth: Int?
) {
    // We could show actual spend here — but we'd need ViewModel access.
    // For now show the row UI matching the image; the spend hint is shown below.
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column {
            // Top row: icon + name + limit input
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CategoryIconBubble(category = category, size = 44)

                Text(
                    category.name,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Limit input chip — shows "No Limit" when empty
                Box(
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .width(110.dp)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (limitInput.isEmpty()) {
                        // Tap to start entering
                        BasicTextField(
                            value = "",
                            onValueChange = { if (it.isNotEmpty()) onLimitChange(it) },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            decorationBox = { inner ->
                                Text(
                                    "No Limit",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        textAlign = TextAlign.Center
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                inner()
                            }
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                "₹", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            BasicTextField(
                                value = limitInput,
                                onValueChange = { v ->
                                    if (v.all { it.isDigit() || it == '.' }) onLimitChange(v)
                                    if (v.isEmpty()) onLimitChange("")
                                },
                                modifier = Modifier.weight(1f),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                            )
                        }
                    }
                }
            }

            // Divider + spend hint row
            HorizontalDivider(
                Modifier.padding(horizontal = 14.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                thickness = 0.5.dp
            )
            Row(
                Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.TrendingUp, null, Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Text(
                    buildSpendHint(budgetPeriod, selectedYear, selectedMonth),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

private fun buildSpendHint(period: BudgetPeriod, year: Int, month: Int?): String {
    return if (period == BudgetPeriod.MONTHLY && month != null) {
        val monthNames = listOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
        val periodLabel = "${monthNames.getOrElse(month - 1) { "" }} $year"
        "Track your spending for $periodLabel here."
    } else {
        "Track your annual spending for $year here."
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CopyBudgetSheet(
    budgets: List<Budget>,
    isMonthly: Boolean,
    onDismiss: () -> Unit,
    onSelect: (Budget) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp, top = 20.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (isMonthly) "Copy monthly budget" else "Copy yearly budget",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Choose an existing budget to prefill this form",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
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

            if (budgets.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (isMonthly) "No previous monthly budgets found"
                        else "No previous yearly budgets found",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(budgets, key = { it.id }) { budget ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(budget) },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            )
                        ) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        budgetPeriodLabel(budget),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "${budget.applicableCategoryIds.size} categories included",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    "₹${formatBudgetAmount(budget.totalLimit)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    item { Spacer(Modifier.height(12.dp)) }
                }
            }
        }
    }
}

private fun budgetPeriodLabel(budget: Budget): String {
    return if (budget.period == BudgetPeriod.MONTHLY && budget.month != null) {
        "${budget.monthName()} ${budget.year}"
    } else {
        budget.year.toString()
    }
}

private fun Budget.monthName(): String =
    java.time.Month.of(month ?: 1)
        .getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault())

private fun formatBudgetAmount(amount: Double): String {
    val asLong = amount.toLong()
    return if (amount == asLong.toDouble()) {
        "%,d".format(asLong)
    } else {
        "%,.2f".format(amount)
    }
}

// ─── Month / Year Picker Dialog ────────────────────────────────────

@Composable
private fun MonthYearPickerDialog(
    isMonthly: Boolean,
    selectedMonth: Int,
    selectedYear: Int,
    monthShort: List<String>,
    onMonthSelect: (Int) -> Unit,
    onYearChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val minYear = 2001
    val maxYear = 2108

    // Local state for the temporary selection before the dialog closes
    var tempSelectedMonth by remember { mutableIntStateOf(selectedMonth) }
    var tempSelectedYear by remember { mutableIntStateOf(selectedYear) }

    // Local year for monthly mode browsing
    var browseYear by remember { mutableIntStateOf(selectedYear) }

    // State for yearly mode pagination
    var startYear by remember {
        mutableIntStateOf((selectedYear - (selectedYear % 3)).coerceIn(minYear, maxYear - 11))
    }

    // Helper to close with a tiny delay so the user sees the selection
    val scope = rememberCoroutineScope()
    val handleSelection = {
        scope.launch {
            delay(50) // Brief pause to show the "active" state
            onDismiss()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Column(Modifier.padding(24.dp)) {

                // ── Header + Navigation ──────────────────────────────────
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (isMonthly) "$browseYear" else "$startYear - ${
                            minOf(
                                startYear + 11,
                                maxYear
                            )
                        }",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = {
                                if (isMonthly) {
                                    if (browseYear > minYear) browseYear--
                                } else {
                                    if (startYear > minYear) startYear -= 12
                                }
                            },
                            enabled = if (isMonthly) browseYear > minYear else startYear > minYear,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.ChevronLeft, "Previous", Modifier.size(24.dp))
                        }
                        IconButton(
                            onClick = {
                                if (isMonthly) {
                                    if (browseYear < maxYear) browseYear++
                                } else {
                                    if (startYear + 12 <= maxYear) startYear += 12
                                }
                            },
                            enabled = if (isMonthly) browseYear < maxYear else startYear + 11 < maxYear,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.ChevronRight, "Next", Modifier.size(24.dp))
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                if (isMonthly) {
                    // ── Month Grid ───────────────────────────────────
                    monthShort.chunked(3).forEachIndexed { rowIdx, rowMonths ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowMonths.forEachIndexed { colIdx, name ->
                                val m = rowIdx * 3 + colIdx + 1
                                // Check if this specific box is the one currently "active"
                                val isSelected =
                                    m == tempSelectedMonth && browseYear == tempSelectedYear

                                Box(
                                    Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) Color.White else Color.Transparent)
                                        .clickable {
                                            tempSelectedMonth = m
                                            tempSelectedYear = browseYear
                                            onMonthSelect(m)
                                            onYearChange(browseYear)
                                            handleSelection()
                                        }
                                        .padding(vertical = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                } else {
                    // ── Year Grid ─────────────────────────────────────────
                    (startYear..minOf(startYear + 11, maxYear)).toList().chunked(3)
                        .forEach { rowYears ->
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowYears.forEach { year ->
                                    val isSelected = year == tempSelectedYear
                                    Box(
                                        Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) Color.White else Color.Transparent)
                                            .clickable {
                                                tempSelectedYear = year
                                                onYearChange(year)
                                                handleSelection()
                                            }
                                            .padding(vertical = 16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "$year",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                // Fill empty spaces if the last row isn't full (e.g., reaching 2070)
                                if (rowYears.size < 3) {
                                    repeat(3 - rowYears.size) { Spacer(Modifier.weight(1f)) }
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                        }
                }
            }
        }
    }
}

// ─── Period toggle pill ───────────────────────────────────────────────────────

@Composable
private fun BudgetPeriodToggle(
    isMonthly: Boolean,
    onMonthly: () -> Unit,
    onYearly: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        Modifier
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(Modifier.padding(4.dp)) {
            PeriodPill(
                label = "Monthly",
                selected = isMonthly,
                onClick = onMonthly,
                modifier = Modifier.weight(1f)
            )
            PeriodPill(
                label = "Yearly",
                selected = !isMonthly,
                onClick = onYearly,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PeriodPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent
            )
            .clickable { onClick() }
            .padding(vertical = 10.dp),
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
