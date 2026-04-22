package com.expensetracker.domain.model

import java.time.LocalDate
import java.time.LocalDateTime

// ─── Enums ─────────────────────────────────────────────────────────────────

enum class TransactionType { EXPENSE, INCOME, TRANSFER }

enum class BudgetPeriod { MONTHLY, YEARLY }

enum class ScheduledFrequency {
    NONE,
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY;
    fun displayName(): String = when (this) {
        NONE -> "Does not repeat"
        DAILY -> "Every day"
        WEEKLY -> "Every week"
        MONTHLY -> "Every month"
        YEARLY -> "Every year"
    }
}

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
    val sortOrder: Int = 0,
    val userId: String = ""
)

/** A bank account — name, balance and color only. Payment behavior lives in PaymentMode. */
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

data class ScheduledTransaction(
    val id: Long = 0,
    val type: TransactionType,
    val amount: Double,
    val categoryId: Long,
    val paymentModeId: Long?,
    val creditCardId: Long? = null,
    val toPaymentModeId: Long? = null,
    val toCreditCardId: Long? = null,
    val note: String = "",
    val dateTime: LocalDateTime,
    val tags: List<String> = emptyList(),
    val frequency: ScheduledFrequency,
    val nextRunAt: LocalDateTime,
    val lastGeneratedAt: LocalDateTime? = null,
    val isActive: Boolean = true,
    val reminderMinutes: Long = 0,
    val userId: String = ""
)

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

enum class TagsMode { INCLUDES_ANY, INCLUDES_ALL, EXCLUDES }

data class TransactionFilter(
    val searchQuery: String = "",
    val year: Int? = null,
    val month: Int? = null,
    val categoryIds: List<Long> = emptyList(),
    val paymentModeIds: List<Long> = emptyList(),
    val tags: List<String> = emptyList(),
    val tagsMode: TagsMode = TagsMode.INCLUDES_ANY,
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

enum class DebtType {
    LENDING,    // You lent money to someone (they owe you)
    BORROWING;  // You borrowed money from someone (you owe them)

    fun displayName() = when (this) {
        LENDING   -> "Lending"
        BORROWING -> "Borrowing"
    }
}

data class Debt(
    val id: Long = 0,
    val type: DebtType,
    val personName: String,
    val amount: Double,
    val paidAmount: Double = 0.0,
    val dueDate: LocalDate? = null,
    val note: String = "",
    val createdAt: LocalDateTime,
    val isSettled: Boolean = false,
    val userId: String = ""
) {
    val remainingAmount: Double get() = (amount - paidAmount).coerceAtLeast(0.0)
    val isOverdue: Boolean get() = dueDate != null
            && !isSettled
            && dueDate.isBefore(LocalDate.now())
}

// ─── Investment type enum ─────────────────────────────────────────────────────

enum class InvestmentType {
    INDIAN_EQUITY,
    INDIAN_MUTUAL_FUND,
    US_EQUITY,
    US_MUTUAL_FUND,
    PPF,
    EPF,
    STOCKS,     // employee compensation stocks (RSU/ESOP)
    NPS,
    BITCOIN,
    GOLD,
    SILVER;

    fun displayName(): String = when (this) {
        INDIAN_EQUITY       -> "Indian Equity"
        INDIAN_MUTUAL_FUND  -> "Indian Mutual Fund"
        US_EQUITY           -> "US Equity"
        US_MUTUAL_FUND      -> "US Mutual Fund"
        PPF                 -> "Public Provident Fund (PPF)"
        EPF                 -> "Employee Provident Fund (EPF)"
        STOCKS              -> "Stocks (Compensation)"
        NPS                 -> "National Pension Scheme (NPS)"
        BITCOIN             -> "Bitcoin"
        GOLD                -> "Gold"
        SILVER              -> "Silver"
    }

    fun shortName(): String = when (this) {
        INDIAN_EQUITY      -> "Indian Equity"
        INDIAN_MUTUAL_FUND -> "Mutual Funds"
        US_EQUITY          -> "US Equity"
        US_MUTUAL_FUND     -> "US Mutual Fund"
        PPF                -> "PPF"
        EPF                -> "EPF"
        STOCKS             -> "Stocks"
        NPS                -> "NPS"
        BITCOIN            -> "Bitcoin"
        GOLD               -> "Gold"
        SILVER             -> "Silver"
    }

    /** Whether a "broker/institution name" field is needed */
    fun requiresBrokerName(): Boolean = this in setOf(INDIAN_EQUITY, US_EQUITY, INDIAN_MUTUAL_FUND, US_MUTUAL_FUND)

    /** Whether a "company/stock name" field is needed */
    fun requiresCompanyName(): Boolean = this == STOCKS
}

// ─── Savings snapshot ─────────────────────────────────────────────────────────

/**
 * One snapshot of a savings account at a point in time.
 * Every save creates a NEW row — history is preserved.
 */
data class SavingsSnapshot(
    val id:              Long      = 0,
    val institutionName: String,
    val savingsBalance:  Double    = 0.0,
    val fdBalance:       Double    = 0.0,
    val rdBalance:       Double    = 0.0,
    val recordedOn:      LocalDate,
    val userId:          String    = ""
) {
    val total: Double get() = savingsBalance + fdBalance + rdBalance
}

// ─── Investment snapshot ──────────────────────────────────────────────────────

/**
 * One snapshot of an investment position at a point in time.
 * Every save creates a NEW row — history is preserved.
 */
data class InvestmentSnapshot(
    val id:              Long           = 0,
    val type:            InvestmentType,
    val subName:         String         = "",  // broker name (equity) or company (stocks)
    val investedAmount:  Double,
    val currentAmount:   Double,
    val recordedOn:      LocalDate,
    val userId:          String         = ""
) {
    val gain:        Double  get() = currentAmount - investedAmount
    val gainPercent: Double  get() = if (investedAmount > 0)
        (gain / investedAmount) * 100.0 else 0.0
    val isGain:      Boolean get() = currentAmount >= investedAmount
}

// ─── Aggregated net-worth view model ─────────────────────────────────────────

data class NetWorthSummary(
    // Savings
    val savingsRows:      List<SavingsRow>      = emptyList(),
    val totalSavings:     Double                = 0.0,
    // Investments
    val investmentRows:   List<InvestmentRow>   = emptyList(),
    val totalInvested:    Double                = 0.0,
    val totalCurrentInv:  Double                = 0.0,
    // Net worth
    val netWorthWithoutGains: Double            = 0.0,   // savings + invested capital
    val netWorthWithGains:    Double            = 0.0    // savings + current investment value
)

data class SavingsRow(
    val institutionName: String,
    val savingsBalance:  Double,
    val fdBalance:       Double,
    val rdBalance:       Double,
    val total:           Double,
    val recordedOn:      LocalDate,
    /** Most-recent snapshot id — used for editing */
    val latestSnapshotId: Long
)

data class InvestmentRow(
    val type:            InvestmentType,
    val subName:         String,
    val invested:        Double,
    val current:         Double,
    val recordedOn:      LocalDate,
    val latestSnapshotId: Long
) {
    val gain:        Double  get() = current - invested
    val gainPercent: Double  get() = if (invested > 0) (gain / invested) * 100.0 else 0.0
    val isGain:      Boolean get() = current >= invested
}