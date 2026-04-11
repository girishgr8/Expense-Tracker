package com.expensetracker.presentation.ui.analysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.data.repository.AuthManager
import com.expensetracker.data.repository.TransactionRepository
import com.expensetracker.data.repository.UserPreferencesRepository
import com.expensetracker.domain.model.Category
import com.expensetracker.domain.model.Transaction
import com.expensetracker.domain.model.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject

enum class AnalysisPeriod { WEEK, MONTH, YEAR, CUSTOM }

data class CategorySpend(
    val category: Category,
    val amount: Double,
    val percentage: Float,
    val colorHex: String,
    val previousAmount: Double = 0.0,
    val changePercent: Float? = null
)

data class DailyPoint(
    val label: String,
    val amount: Double,
    val cumulative: Double
)

/** Spending/income/transfer totals grouped by payment mode display name. */
data class PaymentModeSpend(
    val modeName: String,
    val expense: Double,
    val income: Double,
    val transfer: Double
)

data class AnalysisStats(
    val avgExpensePerDay: Double   = 0.0,
    val avgExpensePerTxn: Double   = 0.0,
    val avgIncomePerDay: Double    = 0.0,
    val avgIncomePerTxn: Double    = 0.0
)

data class AnalysisUiState(
    val period: AnalysisPeriod             = AnalysisPeriod.MONTH,
    val trendsViewType: TransactionType       = TransactionType.EXPENSE,
    val categoriesViewType: TransactionType   = TransactionType.EXPENSE,
    val paymentViewType: TransactionType      = TransactionType.EXPENSE,
    val periodLabel: String                = "",
    val transactionCount: Int              = 0,
    val totalExpense: Double               = 0.0,
    val totalIncome: Double                = 0.0,
    val netBalance: Double                 = 0.0,
    val expenseChangePercent: Float?       = null,
    val incomeChangePercent: Float?        = null,
    val categoryBreakdown: List<CategorySpend>    = emptyList(),
    val dailyPoints: List<DailyPoint>             = emptyList(),
    val comparisonPoints: List<DailyPoint>        = emptyList(),
    val dayWisePoints: List<DailyPoint>           = emptyList(),
    val comparisonDayWisePoints: List<DailyPoint> = emptyList(),
    val paymentModeBreakdown: List<PaymentModeSpend> = emptyList(),
    val stats: AnalysisStats               = AnalysisStats(),
    val customStart: LocalDate?            = null,
    val customEnd: LocalDate?              = null,
    val compareEnabled: Boolean            = false,
    val comparePeriodLabel: String?        = null,
    val compareMonth: YearMonth?           = null,
    val compareYear: Int?                  = null,
    val currencySymbol: String             = "₹",
    val isLoading: Boolean                 = false
)

@HiltViewModel
class AnalysisViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val authManager: AuthManager
) : ViewModel() {

    private val userId get() = authManager.userId

    private val _period      = MutableStateFlow(AnalysisPeriod.MONTH)
    private val _trendsViewType     = MutableStateFlow(TransactionType.EXPENSE)
    private val _categoriesViewType = MutableStateFlow(TransactionType.EXPENSE)
    private val _paymentViewType    = MutableStateFlow(TransactionType.EXPENSE)
    private val _customStart = MutableStateFlow<LocalDate?>(null)
    private val _customEnd   = MutableStateFlow<LocalDate?>(null)
    private val _compareEnabled = MutableStateFlow(false)
    private val _compareMonth = MutableStateFlow(YearMonth.now().minusMonths(1))
    private val _compareYear = MutableStateFlow(LocalDate.now().year - 1)

    private var anchorWeek  = LocalDate.now().with(DayOfWeek.MONDAY)
    private var anchorMonth = YearMonth.now()
    private var anchorYear  = LocalDate.now().year
    private var refreshJob: Job? = null

    private val _uiState = MutableStateFlow(AnalysisUiState())
    val uiState: StateFlow<AnalysisUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            userPreferencesRepository.currencySymbol.collect { sym ->
                _uiState.update { it.copy(currencySymbol = sym) }
            }
        }
        viewModelScope.launch {
            combine(
                _period,          // args[0]: AnalysisPeriod
                _customStart,     // args[1]: LocalDate?
                _customEnd,       // args[2]: LocalDate?
                _compareEnabled,  // args[3]: Boolean
                _compareMonth,    // args[4]: YearMonth
                _compareYear      // args[5]: Int
            ) { args: Array<Any?> ->
                args
            }.collect { refresh() }
        }
    }

    fun setPeriod(p: AnalysisPeriod) { _period.value = p }
    fun setTrendsViewType(t: TransactionType) {
        _trendsViewType.value = t
        refresh()
    }

    fun setCategoriesViewType(t: TransactionType) {
        _categoriesViewType.value = t
        refresh()
    }

    fun setPaymentViewType(t: TransactionType) {
        _paymentViewType.value = t
        // payment breakdown is precomputed for all types, just re-filter in UI
        _uiState.update { it.copy(paymentViewType = t) }
    }
    fun setCustomRange(start: LocalDate, end: LocalDate) {
        _customStart.value = start
        _customEnd.value   = end
        _period.value      = AnalysisPeriod.CUSTOM
    }
    fun setCompareEnabled(enabled: Boolean) {
        _compareEnabled.value = enabled
        if (enabled) {
            when (_period.value) {
                AnalysisPeriod.MONTH -> if (_compareMonth.value == anchorMonth) {
                    _compareMonth.value = anchorMonth.minusMonths(1)
                }
                AnalysisPeriod.YEAR -> if (_compareYear.value == anchorYear) {
                    _compareYear.value = anchorYear - 1
                }
                else -> Unit
            }
        }
    }

    fun setCompareMonth(compareMonth: YearMonth) {
        _compareMonth.value = compareMonth
    }

    fun setCompareYear(compareYear: Int) {
        _compareYear.value = compareYear
    }

    fun previousPeriod() {
        when (_period.value) {
            AnalysisPeriod.WEEK  -> anchorWeek  = anchorWeek.minusWeeks(1)
            AnalysisPeriod.MONTH -> anchorMonth = anchorMonth.minusMonths(1)
            AnalysisPeriod.YEAR  -> anchorYear--
            else -> return
        }
        refresh()
    }

    fun nextPeriod() {
        when (_period.value) {
            AnalysisPeriod.WEEK  -> anchorWeek  = anchorWeek.plusWeeks(1)
            AnalysisPeriod.MONTH -> anchorMonth = anchorMonth.plusMonths(1)
            AnalysisPeriod.YEAR  -> anchorYear++
            else -> return
        }
        refresh()
    }

    private fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = withContext(Dispatchers.IO) { compute() }
            // Preserve independently-toggled fields that don't trigger a recompute
            _uiState.value = result.copy(
                currencySymbol  = _uiState.value.currencySymbol,
                paymentViewType = _uiState.value.paymentViewType
            )
        }
    }

    private suspend fun compute(): AnalysisUiState {
        val (start, end, label) = periodRange()
        val inRange = if (start != null && end != null) {
            transactionRepository.getTransactionsInRangeOneShot(
                userId = userId,
                start = start.atStartOfDay(),
                end = end.atTime(23, 59, 59)
            )
        } else {
            transactionRepository.getAllTransactionsOneShot(userId)
        }
        val (previousStart, previousEnd) = previousPeriodRange(start, end)
        val previousRange = if (previousStart != null && previousEnd != null) {
            transactionRepository.getTransactionsInRangeOneShot(
                userId = userId,
                start = previousStart.atStartOfDay(),
                end = previousEnd.atTime(23, 59, 59)
            )
        } else {
            emptyList()
        }

        val expense = inRange.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val income  = inRange.filter { it.type == TransactionType.INCOME  }.sumOf { it.amount }
        val previousExpense = previousRange.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val previousIncome  = previousRange.filter { it.type == TransactionType.INCOME  }.sumOf { it.amount }
        val compareSelection = comparisonRange(start, end)
        val comparisonRangeTxns = if (_compareEnabled.value && compareSelection != null) {
            transactionRepository.getTransactionsInRangeOneShot(
                userId = userId,
                start = compareSelection.first.atStartOfDay(),
                end = compareSelection.second.atTime(23, 59, 59)
            )
        } else {
            emptyList()
        }

        val breakdown      = buildBreakdown(inRange, previousRange, _categoriesViewType.value)
        val daily          = buildCumulativePoints(inRange, _trendsViewType.value, start, end, _period.value)
        val comparisonPoints = if (_compareEnabled.value && compareSelection != null) {
            buildCumulativePoints(
                comparisonRangeTxns,
                _trendsViewType.value,
                compareSelection.first,
                compareSelection.second,
                _period.value
            )
        } else {
            emptyList()
        }
        val dayWisePoints = if (_period.value == AnalysisPeriod.MONTH) {
            buildDayWisePoints(inRange, _trendsViewType.value, start, end)
        } else {
            emptyList()
        }
        val comparisonDayWisePoints = if (_compareEnabled.value && compareSelection != null && _period.value == AnalysisPeriod.MONTH) {
            buildDayWisePoints(
                comparisonRangeTxns,
                _trendsViewType.value,
                compareSelection.first,
                compareSelection.second
            )
        } else {
            emptyList()
        }
        val paymentModes   = buildPaymentModeBreakdown(inRange)
        val stats          = buildStats(inRange, start, end)

        return AnalysisUiState(
            period                = _period.value,
            trendsViewType        = _trendsViewType.value,
            categoriesViewType    = _categoriesViewType.value,
            paymentViewType       = _paymentViewType.value,
            periodLabel           = label,
            transactionCount      = inRange.size,
            totalExpense          = expense,
            totalIncome           = income,
            netBalance            = income - expense,
            expenseChangePercent  = computeChangePercent(expense, previousExpense),
            incomeChangePercent   = computeChangePercent(income, previousIncome),
            categoryBreakdown     = breakdown,
            dailyPoints           = daily,
            comparisonPoints      = comparisonPoints,
            dayWisePoints         = dayWisePoints,
            comparisonDayWisePoints = comparisonDayWisePoints,
            paymentModeBreakdown  = paymentModes,
            stats                 = stats,
            customStart           = _customStart.value,
            customEnd             = _customEnd.value,
            compareEnabled        = _compareEnabled.value && compareSelection != null,
            comparePeriodLabel    = compareSelection?.third,
            compareMonth          = _compareMonth.value,
            compareYear           = _compareYear.value,
            isLoading             = false
        )
    }

    private fun periodRange(): Triple<LocalDate?, LocalDate?, String> = when (_period.value) {
        AnalysisPeriod.WEEK -> {
            val end = anchorWeek.plusDays(6)
            Triple(
                anchorWeek, end,
                "${anchorWeek.format(DateTimeFormatter.ofPattern("MMM d"))} – " +
                        end.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
            )
        }
        AnalysisPeriod.MONTH -> Triple(
            anchorMonth.atDay(1),
            anchorMonth.atEndOfMonth(),
            anchorMonth.atDay(1).format(DateTimeFormatter.ofPattern("MMMM yyyy"))
        )
        AnalysisPeriod.YEAR -> Triple(
            LocalDate.of(anchorYear, 1, 1),
            LocalDate.of(anchorYear, 12, 31),
            "Year $anchorYear"
        )
        AnalysisPeriod.CUSTOM -> {
            val s = _customStart.value ?: LocalDate.now().withDayOfMonth(1)
            val e = _customEnd.value   ?: LocalDate.now()
            val fmt = DateTimeFormatter.ofPattern("MMM d, yyyy")
            Triple(s, e, "${s.format(fmt)} – ${e.format(fmt)}")
        }
    }

    private fun previousPeriodRange(
        start: LocalDate?,
        end: LocalDate?
    ): Pair<LocalDate?, LocalDate?> {
        if (start == null || end == null) return null to null
        return when (_period.value) {
            AnalysisPeriod.WEEK -> start.minusWeeks(1) to end.minusWeeks(1)
            AnalysisPeriod.MONTH -> {
                val prevMonth = YearMonth.from(start).minusMonths(1)
                prevMonth.atDay(1) to prevMonth.atEndOfMonth()
            }
            AnalysisPeriod.YEAR -> {
                val prevYear = start.year - 1
                LocalDate.of(prevYear, 1, 1) to LocalDate.of(prevYear, 12, 31)
            }
            AnalysisPeriod.CUSTOM -> {
                val daySpan = ChronoUnit.DAYS.between(start, end).coerceAtLeast(0)
                val previousEnd = start.minusDays(1)
                val previousStart = previousEnd.minusDays(daySpan)
                previousStart to previousEnd
            }
        }
    }

    private fun comparisonRange(
        currentStart: LocalDate?,
        currentEnd: LocalDate?
    ): Triple<LocalDate, LocalDate, String>? {
        if (!_compareEnabled.value) return null
        return when (_period.value) {
            AnalysisPeriod.MONTH -> {
                var compareMonth = _compareMonth.value
                if (compareMonth == anchorMonth) {
                    compareMonth = anchorMonth.minusMonths(1)
                }
                Triple(
                    compareMonth.atDay(1),
                    compareMonth.atEndOfMonth(),
                    compareMonth.atDay(1).format(DateTimeFormatter.ofPattern("MMMM yyyy"))
                )
            }
            AnalysisPeriod.YEAR -> {
                var compareYear = _compareYear.value
                if (compareYear == anchorYear) {
                    compareYear = anchorYear - 1
                }
                Triple(
                    LocalDate.of(compareYear, 1, 1),
                    LocalDate.of(compareYear, 12, 31),
                    "Year $compareYear"
                )
            }
            else -> null
        }
    }

    private fun buildBreakdown(
        txns: List<Transaction>,
        previousTxns: List<Transaction>,
        type: TransactionType
    ): List<CategorySpend> {
        val filtered = txns.filter { it.type == type }
        val total    = filtered.sumOf { it.amount }.takeIf { it > 0 } ?: return emptyList()
        val previousByCategory = previousTxns
            .filter { it.type == type }
            .groupBy { it.categoryId }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
        return filtered
            .groupBy { it.categoryId }
            .map { (catId, list) ->
                val first  = list.first()
                val amount = list.sumOf { it.amount }
                val previousAmount = previousByCategory[catId] ?: 0.0
                CategorySpend(
                    category = Category(
                        id              = catId,
                        name            = first.categoryName.ifEmpty { "Unknown" },
                        icon            = first.categoryIcon,
                        colorHex        = first.categoryColorHex.ifEmpty { "#6750A4" },
                        transactionType = type
                    ),
                    amount     = amount,
                    percentage = (amount / total * 100).toFloat(),
                    colorHex   = first.categoryColorHex.ifEmpty { "#6750A4" },
                    previousAmount = previousAmount,
                    changePercent = computeChangePercent(amount, previousAmount)
                )
            }
            .sortedByDescending { it.amount }
    }

    private fun computeChangePercent(current: Double, previous: Double): Float? {
        if (previous <= 0.0) return null
        return (((current - previous) / previous) * 100.0).toFloat()
    }

    private fun buildPaymentModeBreakdown(txns: List<Transaction>): List<PaymentModeSpend> {
        // Group by paymentModeName (strip balance parenthetical for cleaner labels)
        val grouped = txns
            .filter { it.paymentModeName.isNotEmpty() }
            .groupBy { it.paymentModeName.replace(Regex("""\s*\([^)]*available\)"""), "").trim() }

        return grouped.map { (name, list) ->
            PaymentModeSpend(
                modeName = name,
                expense  = list.filter { it.type == TransactionType.EXPENSE  }.sumOf { it.amount },
                income   = list.filter { it.type == TransactionType.INCOME   }.sumOf { it.amount },
                transfer = list.filter { it.type == TransactionType.TRANSFER }.sumOf { it.amount }
            )
        }.sortedByDescending { it.expense + it.income + it.transfer }
    }

    private fun buildStats(
        txns: List<Transaction>,
        start: LocalDate?,
        end: LocalDate?
    ): AnalysisStats {
        val days = if (start != null && end != null)
            ChronoUnit.DAYS.between(start, end).toInt().coerceAtLeast(1)
        else 1

        val expenses = txns.filter { it.type == TransactionType.EXPENSE }
        val incomes  = txns.filter { it.type == TransactionType.INCOME  }

        val totalExp = expenses.sumOf { it.amount }
        val totalInc = incomes.sumOf { it.amount }

        return AnalysisStats(
            avgExpensePerDay = totalExp / days,
            avgExpensePerTxn = if (expenses.isNotEmpty()) totalExp / expenses.size else 0.0,
            avgIncomePerDay  = totalInc / days,
            avgIncomePerTxn  = if (incomes.isNotEmpty()) totalInc / incomes.size else 0.0
        )
    }

    private fun buildCumulativePoints(
        txns: List<Transaction>,
        type: TransactionType,
        start: LocalDate?,
        end: LocalDate?,
        period: AnalysisPeriod
    ): List<DailyPoint> {
        if (start == null || end == null) return emptyList()
        if (end.isBefore(start)) return emptyList()

        val filtered = txns.filter { it.type == type }

        if (period == AnalysisPeriod.YEAR) {
            var cum = 0.0
            return (1..12).map { m ->
                val ms  = LocalDate.of(start.year, m, 1)
                val me  = YearMonth.of(start.year, m).atEndOfMonth()
                val amt = filtered
                    .filter { t -> val d = t.dateTime.toLocalDate(); !d.isBefore(ms) && !d.isAfter(me) }
                    .sumOf { it.amount }
                cum += amt
                DailyPoint(ms.format(DateTimeFormatter.ofPattern("MMM")), amt, cum)
            }
        }

        val totalDays = ChronoUnit.DAYS.between(start, end).toInt().coerceIn(0, 365)
        val useDailyPoints = period != AnalysisPeriod.CUSTOM || totalDays <= 62

        if (useDailyPoints) {
            val byDate = filtered
                .groupBy { it.dateTime.toLocalDate() }
                .mapValues { (_, list) -> list.sumOf { it.amount } }
            val fmt = when (period) {
                AnalysisPeriod.WEEK -> DateTimeFormatter.ofPattern("EEE")
                else                -> DateTimeFormatter.ofPattern("d")
            }
            var cum = 0.0
            return (0..totalDays).map { offset ->
                val day = start.plusDays(offset.toLong())
                val amt = byDate[day] ?: 0.0
                cum += amt
                DailyPoint(day.format(fmt), amt, cum)
            }
        }

        val totalWeeks = (totalDays / 7).coerceIn(1, 53)
        val weekFmt    = DateTimeFormatter.ofPattern("MMM d")
        val byDate     = filtered
            .groupBy { it.dateTime.toLocalDate() }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
        var cum = 0.0
        return (0 until totalWeeks).map { weekIdx ->
            val wStart = start.plusWeeks(weekIdx.toLong())
            val wEnd   = minOf(wStart.plusDays(6), end)
            val wDays  = ChronoUnit.DAYS.between(wStart, wEnd).toInt().coerceIn(0, 6)
            val amt    = (0..wDays).sumOf { d -> byDate[wStart.plusDays(d.toLong())] ?: 0.0 }
            cum += amt
            DailyPoint(wStart.format(weekFmt), amt, cum)
        }
    }

    private fun buildDayWisePoints(
        txns: List<Transaction>,
        type: TransactionType,
        start: LocalDate?,
        end: LocalDate?
    ): List<DailyPoint> {
        if (start == null || end == null || end.isBefore(start)) return emptyList()
        val filtered = txns.filter { it.type == type }
        val byDate = filtered
            .groupBy { it.dateTime.toLocalDate() }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
        val totalDays = ChronoUnit.DAYS.between(start, end).toInt().coerceIn(0, 31)
        return (0..totalDays).map { offset ->
            val day = start.plusDays(offset.toLong())
            val amt = byDate[day] ?: 0.0
            DailyPoint(day.format(DateTimeFormatter.ofPattern("d")), amt, amt)
        }
    }
}
