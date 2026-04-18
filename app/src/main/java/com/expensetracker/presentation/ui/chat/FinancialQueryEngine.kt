package com.expensetracker.presentation.ui.chat

import androidx.lifecycle.viewModelScope
import com.expensetracker.data.repository.AuthManager
import com.expensetracker.data.repository.BalanceAdjustmentRepository
import com.expensetracker.data.repository.BankAccountRepository
import com.expensetracker.data.repository.CategoryRepository
import com.expensetracker.data.repository.PaymentModeRepository
import com.expensetracker.data.repository.TransactionRepository
import com.expensetracker.domain.model.Transaction
import com.expensetracker.domain.model.TransactionType
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * Executes AI-requested tool calls against the real local database.
 *
 * Each function receives a [JSONObject] of arguments from the Claude API and
 * returns a plain-text or JSON string result that goes back to the API as a
 * tool_result message.
 */
@Singleton
class FinancialQueryEngine @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val paymentModeRepository: PaymentModeRepository,
    private val bankAccountRepository: BankAccountRepository,
    private val balanceAdjustmentRepository: BalanceAdjustmentRepository,
    private val authManager: AuthManager
) {
    private val userId get() = authManager.userId
    private val dateFmt = DateTimeFormatter.ofPattern("dd MMM yyyy")
    private val dtFmt   = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")

    // ── Tool dispatch ─────────────────────────────────────────────────────────

    suspend fun execute(toolName: String, input: JSONObject): String = when (toolName) {
        "get_summary"              -> getSummary(input)
        "get_transactions"         -> getTransactions(input)
        "summarise_by_category"    -> summariseByCategory(input)
        "summarise_by_payment_mode"-> summariseByPaymentMode(input)
        "get_account_balances"     -> getAccountBalances(input)
        "get_top_expenses"         -> getTopExpenses(input)
        "get_spending_trend"       -> getSpendingTrend(input)
        "get_budget_status"        -> getBudgetStatus(input)
        else -> """{"error":"Unknown tool $toolName"}"""
    }

    // ── Tool: overall income / expense / net summary ──────────────────────────

    private suspend fun getSummary(args: JSONObject): String {
        val txns = filteredTransactions(args)
        val income   = txns.filter { it.type == TransactionType.INCOME  }.sumOf { it.amount }
        val expense  = txns.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        val transfer = txns.filter { it.type == TransactionType.TRANSFER}.sumOf { it.amount }
        return JSONObject().apply {
            put("total_transactions", txns.size)
            put("total_income",   fmt(income))
            put("total_expense",  fmt(expense))
            put("total_transfer", fmt(transfer))
            put("net_balance",    fmt(income - expense))
            put("period",         periodLabel(args))
        }.toString()
    }

    // ── Tool: list transactions with optional filters ─────────────────────────

    private suspend fun getTransactions(args: JSONObject): String {
        val txns  = filteredTransactions(args).take(50) // cap to avoid huge responses
        val arr   = JSONArray()
        txns.forEach { t ->
            arr.put(JSONObject().apply {
                put("date",         t.dateTime.format(dateFmt))
                put("type",         t.type.name.lowercase())
                put("amount",       fmt(t.amount))
                put("category",     t.categoryName)
                put("payment_mode", t.paymentModeName)
                put("note",         t.note)
                if (t.tags.isNotEmpty()) put("tags", t.tags.joinToString(", "))
            })
        }
        return JSONObject().apply {
            put("count", txns.size)
            put("transactions", arr)
            put("period", periodLabel(args))
        }.toString()
    }

    // ── Tool: group + sum by category ─────────────────────────────────────────

    private suspend fun summariseByCategory(args: JSONObject): String {
        val txns      = filteredTransactions(args)
        val typeStr   = args.optString("transaction_type", "all").lowercase()
        val filtered  = when (typeStr) {
            "income"   -> txns.filter { it.type == TransactionType.INCOME  }
            "expense"  -> txns.filter { it.type == TransactionType.EXPENSE }
            "transfer" -> txns.filter { it.type == TransactionType.TRANSFER}
            else       -> txns
        }
        val grouped = filtered
            .groupBy { it.categoryName.ifEmpty { "Uncategorised" } }
            .map { (cat, list) ->
                JSONObject().apply {
                    put("category", cat)
                    put("count",  list.size)
                    put("total",  fmt(list.sumOf { it.amount }))
                }
            }
            .sortedByDescending { it.getString("total").replace(",", "").toDoubleOrNull() ?: 0.0 }
        return JSONObject().apply {
            put("summary", JSONArray(grouped))
            put("period", periodLabel(args))
        }.toString()
    }

    // ── Tool: group + sum by payment mode ─────────────────────────────────────

    private suspend fun summariseByPaymentMode(args: JSONObject): String {
        val txns     = filteredTransactions(args)
        val typeStr  = args.optString("transaction_type", "expense").lowercase()
        val filtered = when (typeStr) {
            "income"   -> txns.filter { it.type == TransactionType.INCOME  }
            "transfer" -> txns.filter { it.type == TransactionType.TRANSFER}
            else       -> txns.filter { it.type == TransactionType.EXPENSE }
        }
        val grouped = filtered
            .groupBy { it.paymentModeName.ifEmpty { "Unknown" } }
            .map { (mode, list) ->
                JSONObject().apply {
                    put("payment_mode", mode)
                    put("count", list.size)
                    put("total", fmt(list.sumOf { it.amount }))
                }
            }
            .sortedByDescending { it.getString("total").replace(",", "").toDoubleOrNull() ?: 0.0 }
        return JSONObject().apply {
            put("summary", JSONArray(grouped))
            put("period", periodLabel(args))
        }.toString()
    }

    // ── Tool: account balances ────────────────────────────────────────────────

    private suspend fun getAccountBalances(args: JSONObject): String {
        val accounts = bankAccountRepository.getAllAccounts(userId).first()
        val arr      = JSONArray()
        accounts.forEach { acc ->
            arr.put(JSONObject().apply {
                put("account", acc.name)
                put("balance", fmt(acc.balance))
            })
        }
//        val modes = paymentModeRepository.getAllModes(userId).first()
//        val modesArr = JSONArray()

//        modes.filter { it.bankAccountId == null }.forEach { mode ->
//            run {
//                combine(
//                transactionRepository.getAllTransactions(userId),
//                balanceAdjustmentRepository.getAllAdjustments(userId)
//            ) { txns, adjustments ->
//                    val income =
//                        txns.filter { it.paymentModeId == mode.id && it.type == TransactionType.INCOME }
//                            .sumOf { it.amount }
//                    val expense =
//                        txns.filter { it.paymentModeId == mode.id && it.type == TransactionType.EXPENSE }
//                            .sumOf { it.amount }
//                    val manualAdjustments = adjustments
//                        .filter { it.paymentModeId == mode.id }
//                        .sumOf { it.amountDelta }
//                    mode.id to (income - expense + manualAdjustments)
//                }
//            }.collect { balances ->
//
//                modesArr.put(JSONObject().apply {
//                    put("mode", mode.displayLabel)
//                    put("balance", fmt(mode.bankAccountId .?: 0.0))
//                })
//            }
//        }
        return JSONObject().apply {
            put("bank_accounts",   arr)
            put("total_bank_balance",
                fmt(accounts.sumOf { it.balance }))
//            put("standalone_modes", modesArr)
        }.toString()
    }

    // ── Tool: top N expenses ──────────────────────────────────────────────────

    private suspend fun getTopExpenses(args: JSONObject): String {
        val n    = args.optInt("limit", 10)
        val txns = filteredTransactions(args)
            .filter { it.type == TransactionType.EXPENSE }
            .sortedByDescending { it.amount }
            .take(n)
        val arr = JSONArray()
        txns.forEach { t ->
            arr.put(JSONObject().apply {
                put("date",         t.dateTime.format(dateFmt))
                put("amount",       fmt(t.amount))
                put("category",     t.categoryName)
                put("payment_mode", t.paymentModeName)
                put("note",         t.note)
            })
        }
        return JSONObject().apply {
            put("top_expenses", arr)
            put("period", periodLabel(args))
        }.toString()
    }

    // ── Tool: monthly spending trend ──────────────────────────────────────────

    private suspend fun getSpendingTrend(args: JSONObject): String {
        val months = args.optInt("months", 6).coerceIn(1, 24)
        val txns   = transactionRepository.getAllTransactionsOneShot(userId)
        val cutoff = LocalDate.now().minusMonths(months.toLong()).withDayOfMonth(1)
        val fmt2   = DateTimeFormatter.ofPattern("MMM yyyy")

        val grouped = txns
            .filter { it.type == TransactionType.EXPENSE }
            .filter { !it.dateTime.toLocalDate().isBefore(cutoff) }
            .groupBy { it.dateTime.toLocalDate().withDayOfMonth(1) }
            .map { (month, list) ->
                JSONObject().apply {
                    put("month",   month.format(fmt2))
                    put("expense", fmt(list.sumOf { it.amount }))
                    put("count",   list.size)
                }
            }
            .sortedBy { it.getString("month") }
        return JSONObject().apply {
            put("monthly_trend", JSONArray(grouped))
            put("months_analysed", months)
        }.toString()
    }

    // ── Tool: budget status ───────────────────────────────────────────────────

    private suspend fun getBudgetStatus(args: JSONObject): String {
        // Just returns a note — budget data is available via BudgetRepository
        // but injecting it here keeps this engine focused. The VM can add it.
        return """{"note":"Budget data is available. Ask about specific budget periods."}"""
    }

    // ── Shared helpers ────────────────────────────────────────────────────────

    private suspend fun filteredTransactions(args: JSONObject): List<Transaction> {
        val all = transactionRepository.getAllTransactionsOneShot(userId)
        var result = all

        // Filter by category name (fuzzy match)
        val catName = args.optString("category", "")
        if (catName.isNotBlank()) {
            val cats = categoryRepository.getAllCategories(userId).first()
            val matchedIds = cats
                .filter { it.name.contains(catName, ignoreCase = true) }
                .map { it.id }
            result = result.filter { it.categoryId in matchedIds }
        }

        // Filter by payment mode name
        val modeName = args.optString("payment_mode", "")
        if (modeName.isNotBlank()) {
            result = result.filter {
                it.paymentModeName.contains(modeName, ignoreCase = true)
            }
        }

        // Filter by transaction type
        val typeStr = args.optString("transaction_type", "")
        if (typeStr.isNotBlank() && typeStr != "all") {
            val type = when (typeStr.lowercase()) {
                "income"   -> TransactionType.INCOME
                "expense"  -> TransactionType.EXPENSE
                "transfer" -> TransactionType.TRANSFER
                else       -> null
            }
            if (type != null) result = result.filter { it.type == type }
        }

        // Filter by year
        val year = args.optInt("year", 0)
        if (year > 0) result = result.filter { it.dateTime.year == year }

        // Filter by month (1-12)
        val month = args.optInt("month", 0)
        if (month in 1..12) result = result.filter { it.dateTime.monthValue == month }

        // Filter by date range
        val fromStr = args.optString("from_date", "")
        val toStr   = args.optString("to_date",   "")
        val parseFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        if (fromStr.isNotBlank()) {
            runCatching {
                val from = LocalDate.parse(fromStr, parseFmt).atStartOfDay()
                result = result.filter { !it.dateTime.isBefore(from) }
            }
        }
        if (toStr.isNotBlank()) {
            runCatching {
                val to = LocalDate.parse(toStr, parseFmt).atTime(23, 59, 59)
                result = result.filter { !it.dateTime.isAfter(to) }
            }
        }

        // Filter by tags
        val tagFilter = args.optString("tag", "")
        if (tagFilter.isNotBlank()) {
            result = result.filter { txn ->
                txn.tags.any { it.contains(tagFilter, ignoreCase = true) }
            }
        }

        return result
    }

    private fun periodLabel(args: JSONObject): String {
        val year  = args.optInt("year", 0)
        val month = args.optInt("month", 0)
        val from  = args.optString("from_date", "")
        val to    = args.optString("to_date", "")
        val cat   = args.optString("category", "")
        return when {
            from.isNotBlank() && to.isNotBlank() -> "$from to $to"
            year > 0 && month in 1..12 ->
                "${DateTimeFormatter.ofPattern("MMMM").format(
                    LocalDateTime.of(year, month, 1, 0, 0))} $year"
            year > 0    -> "Year $year"
            month in 1..12 -> "Month $month"
            cat.isNotBlank() -> "Category: $cat (all time)"
            else        -> "All time"
        }
    }

    private fun fmt(d: Double): String = "%,.2f".format(d)
}