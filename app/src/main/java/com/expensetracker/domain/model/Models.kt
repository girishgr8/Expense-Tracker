package com.expensetracker.domain.model

import java.time.LocalDateTime

// ─── Enums ─────────────────────────────────────────────────────────────────

enum class TransactionType { EXPENSE, INCOME, TRANSFER }

enum class BudgetPeriod { MONTHLY, YEARLY }

/**
 * Payment mode types that can be linked to a BankAccount, or standalone.
 * CREDIT_CARD is intentionally excluded — credit cards are a first-class
 * entity ([CreditCard]) with their own fields (limit, billing cycle, etc.).
 */
enum class PaymentModeType {
    DEBIT_CARD,
    UPI,
    NET_BANKING,
    CHEQUE,
    CASH,
    WALLET,
    OTHER;

    fun displayName(): String = when (this) {
        DEBIT_CARD -> "Debit Card"
        UPI -> "UPI"
        NET_BANKING -> "Net Banking"
        CHEQUE -> "Cheque"
        CASH -> "Cash"
        WALLET -> "Wallet"
        OTHER -> "Other"
    }

    /** True if this mode must be linked to a BankAccount. */
    fun requiresBankAccount(): Boolean = this in setOf(
        DEBIT_CARD, UPI, NET_BANKING, CHEQUE
    )
}

enum class ExportFormat {
    CSV,
    PDF;

    fun displayName(): String = when (this) {
        CSV -> "CSV"
        PDF -> "PDF"
    }
}

// ─── Core Domain Models ─────────────────────────────────────────────────────

data class Transaction(
    val id: Long = 0,
    val type: TransactionType,
    val amount: Double,
    val categoryId: Long,
    val categoryName: String = "",
    val categoryIcon: String = "category",
    val categoryColorHex: String = "#6750A4",
    val paymentModeId: Long?,           // references PaymentMode (nullable)
    val creditCardId: Long? = null,     // references CreditCard (nullable)
    val paymentModeName: String = "",   // display label for either mode or card
    val toPaymentModeId: Long? = null,
    val toCreditCardId: Long? = null,
    val toPaymentModeName: String = "",
    val note: String = "",
    val dateTime: LocalDateTime,
    val tags: List<String> = emptyList(),
    val attachments: List<Attachment> = emptyList(),
    val userId: String = ""
)

data class Category(
    val id: Long = 0,
    val name: String,
    val icon: String,
    val colorHex: String,
    val transactionType: TransactionType?,
    val isDefault: Boolean = false,
    val userId: String = ""
)

/** A bank account — name, balance and colour only. Payment behaviour lives in PaymentMode. */
data class BankAccount(
    val id: Long = 0,
    val name: String,
    val balance: Double = 0.0,
    val colorHex: String = "#6750A4",
    val userId: String = ""
)

data class BalanceAdjustment(
    val id: Long = 0,
    val bankAccountId: Long? = null,
    val creditCardId: Long? = null,
    val paymentModeId: Long? = null,
    val previousBalance: Double,
    val newBalance: Double,
    val amountDelta: Double,
    val adjustedAt: LocalDateTime,
    val userId: String = ""
)

/**
 * A payment mode linked to a [BankAccount] (or standalone for CASH/WALLET/OTHER).
 * Credit cards are NOT represented here — use [CreditCard] instead.
 */
data class PaymentMode(
    val id: Long = 0,
    val bankAccountId: Long?,
    val bankAccountName: String = "",
    val type: PaymentModeType,
    val identifier: String = "",
    val userId: String = ""
) {
    val displayLabel: String
        get() = buildString {
            if (bankAccountName.isNotEmpty()) {
                append(bankAccountName); append(" – ")
            }
            append(type.displayName())
            if (identifier.isNotEmpty()) {
                append(" ("); append(identifier); append(")")
            }
        }
}

/**
 * A standalone credit card — issued by any bank or NBFC.
 * Not tied to a [BankAccount].
 *
 * @param name             e.g. "HDFC Regalia", "Axis Magnus"
 * @param availableLimit   current spendable limit (updated by user)
 * @param totalLimit       total sanctioned credit limit
 * @param billingCycleDate day of month on which the billing cycle resets (1-31)
 * @param paymentDueDate   day of month by which the bill must be paid (1-31)
 * @param colorHex         card accent colour for UI
 */
data class CreditCard(
    val id: Long = 0,
    val name: String,
    val availableLimit: Double = 0.0,
    val totalLimit: Double = 0.0,
    val billingCycleDate: Int = 1,
    val paymentDueDate: Int = 15,
    val colorHex: String = "#EA4335",
    val userId: String = ""
) {
    val displayLabel: String
        get() = name
}

/**
 * A unified payment option shown in transaction dropdowns.
 * Wraps either a [PaymentMode] or a [CreditCard].
 */
sealed class PaymentOption {
    abstract val id: Long
    abstract val displayLabel: String

    data class Mode(val mode: PaymentMode) : PaymentOption() {
        override val id get() = mode.id
        override val displayLabel get() = mode.displayLabel
    }

    data class Card(val card: CreditCard) : PaymentOption() {
        override val id get() = card.id
        override val displayLabel get() = card.displayLabel
    }
}

data class Attachment(
    val id: Long = 0,
    val transactionId: Long,
    val fileName: String,
    val filePath: String,
    val mimeType: String,
    val fileSizeBytes: Long
)

data class Budget(
    val id: Long = 0,
    val name: String,
    val totalLimit: Double,
    val period: BudgetPeriod,
    val year: Int,
    val month: Int? = null,
    val applicableCategoryIds: List<Long> = emptyList(),
    val categoryLimits: Map<Long, Double> = emptyMap(),
    val userId: String = ""
)

data class Tag(val id: Long = 0, val name: String, val userId: String = "")

// ─── UI / Summary Models ─────────────────────────────────────────────────────

data class MonthlySummary(
    val totalIncome: Double,
    val totalExpense: Double,
    val netBalance: Double,
    val label: String
)

data class BudgetProgress(
    val budget: Budget,
    val spent: Double,
    val percentage: Float = if (budget.totalLimit > 0)
        (spent / budget.totalLimit * 100).toFloat() else 0f
)

data class TransactionFilter(
    val searchQuery: String = "",
    val year: Int? = null,
    val month: Int? = null,
    val categoryIds: List<Long> = emptyList(),
    val paymentModeIds: List<Long> = emptyList(),
    val tags: List<String> = emptyList(),
    val transactionTypes: List<TransactionType> = emptyList()
)

sealed class ExportFilter(val displayName: String) {
    object AllTime : ExportFilter("All time")
    data class Year(val year: Int) : ExportFilter(year.toString())
    data class Month(val year: Int, val month: Int) :
        ExportFilter("${java.time.Month.of(month)} $year")
}

data class YearMonthTuple(
    val year: String,
    val month: String
)
