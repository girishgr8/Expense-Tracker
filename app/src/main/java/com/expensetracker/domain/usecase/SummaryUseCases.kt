package com.expensetracker.domain.usecase

import com.expensetracker.data.repository.BudgetRepository
import com.expensetracker.data.repository.TransactionRepository
import com.expensetracker.domain.model.BudgetProgress
import com.expensetracker.domain.model.MonthlySummary
import com.expensetracker.domain.model.TransactionType
import com.expensetracker.util.endOfMonth
import com.expensetracker.util.startOfMonth
import java.time.YearMonth
import javax.inject.Inject

// ─── GetMonthlySummaryUseCase ─────────────────────────────────────────────────

class GetMonthlySummaryUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(
        userId: String,
        yearMonth: YearMonth
    ): MonthlySummary {
        val start = yearMonth.startOfMonth()
        val end   = yearMonth.endOfMonth()

        val income  = transactionRepository.getTotalByType(userId, TransactionType.INCOME,  start, end)
        val expense = transactionRepository.getTotalByType(userId, TransactionType.EXPENSE, start, end)

        return MonthlySummary(
            totalIncome  = income,
            totalExpense = expense,
            netBalance   = income - expense,
            label        = "${yearMonth.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${yearMonth.year}"
        )
    }
}

// ─── GetYearlySummaryUseCase ──────────────────────────────────────────────────

class GetYearlySummaryUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(userId: String, year: Int): MonthlySummary {
        val start = java.time.LocalDateTime.of(year, 1,  1,  0,  0,  0)
        val end   = java.time.LocalDateTime.of(year, 12, 31, 23, 59, 59)

        val income  = transactionRepository.getTotalByType(userId, TransactionType.INCOME,  start, end)
        val expense = transactionRepository.getTotalByType(userId, TransactionType.EXPENSE, start, end)

        return MonthlySummary(
            totalIncome  = income,
            totalExpense = expense,
            netBalance   = income - expense,
            label        = "Year $year"
        )
    }
}

// ─── GetAllTimeSummaryUseCase ─────────────────────────────────────────────────

class GetAllTimeSummaryUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(userId: String): MonthlySummary {
        val income  = transactionRepository.getTotalByType(userId, TransactionType.INCOME,  null, null)
        val expense = transactionRepository.getTotalByType(userId, TransactionType.EXPENSE, null, null)

        return MonthlySummary(
            totalIncome  = income,
            totalExpense = expense,
            netBalance   = income - expense,
            label        = "All Time"
        )
    }
}

// ─── GetBudgetProgressUseCase ─────────────────────────────────────────────────

class GetBudgetProgressUseCase @Inject constructor(
    private val budgetRepository: BudgetRepository,
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(userId: String, yearMonth: YearMonth): BudgetProgress? {
        val budget = budgetRepository.getBudgetForPeriod(
            userId, yearMonth.year, yearMonth.monthValue
        ) ?: return null

        val start = yearMonth.startOfMonth()
        val end   = yearMonth.endOfMonth()

        val spent = if (budget.applicableCategoryIds.isEmpty()) {
            transactionRepository.getAllExpense(userId, start, end)
        } else {
            transactionRepository.getExpenseByCategories(
                userId, budget.applicableCategoryIds, start, end
            )
        }

        return BudgetProgress(budget = budget, spent = spent)
    }
}
