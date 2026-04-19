package com.expensetracker.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.expensetracker.domain.model.BudgetPeriod
import com.expensetracker.domain.model.PaymentModeType
import com.expensetracker.domain.model.ScheduledFrequency
import com.expensetracker.domain.model.TransactionType

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["userId", "dateTimeMillis"]),
        Index(value = ["userId", "type", "dateTimeMillis"]),
        Index(value = ["userId", "categoryId", "dateTimeMillis"]),
        Index(value = ["userId", "paymentModeId", "dateTimeMillis"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: TransactionType,
    val amount: Double,
    val categoryId: Long,
    val paymentModeId: Long?,       // references payment_modes.id (nullable)
    val creditCardId: Long?,        // references credit_cards.id (nullable)
    val toPaymentModeId: Long?,     // for transfers
    val toCreditCardId: Long?,      // for transfers via credit card
    val note: String,
    val dateTimeMillis: Long,
    val tags: String,               // JSON array
    val userId: String
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val icon: String,
    val colorHex: String,
    val transactionType: TransactionType?,
    val isDefault: Boolean,
    @ColumnInfo(defaultValue = "0") val sortOrder: Int = 0,
    val userId: String
)

/** Top-level bank account — only name, balance, colour. */
@Entity(tableName = "bank_accounts")
data class BankAccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val balance: Double,
    val colorHex: String,
    val userId: String
)

@Entity(
    tableName = "balance_adjustments",
    foreignKeys = [ForeignKey(
        entity = BankAccountEntity::class,
        parentColumns = ["id"],
        childColumns = ["bankAccountId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("bankAccountId"), Index("creditCardId"), Index("paymentModeId"), Index("userId")]
)
data class BalanceAdjustmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bankAccountId: Long?,
    val creditCardId: Long?,
    val paymentModeId: Long?,
    val previousBalance: Double,
    val newBalance: Double,
    val amountDelta: Double,
    val adjustedAtMillis: Long,
    val userId: String
)

/**
 * A payment mode linked to a bank account (nullable for CASH/WALLET/standalone).
 * Credit cards are stored separately in [CreditCardEntity].
 * Foreign key to bank_accounts with CASCADE delete.
 */
@Entity(
    tableName = "payment_modes",
    foreignKeys = [ForeignKey(
        entity = BankAccountEntity::class,
        parentColumns = ["id"],
        childColumns = ["bankAccountId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("bankAccountId")]
)
data class PaymentModeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bankAccountId: Long?,       // null = standalone (Cash, Wallet, Other)
    val type: PaymentModeType,
    val identifier: String,         // UPI ID / last-4 / wallet name
    val userId: String
)

/**
 * A standalone credit card.
 * Not tied to any bank account — issued by any bank or NBFC.
 */
@Entity(tableName = "credit_cards")
data class CreditCardEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val availableLimit: Double,
    val totalLimit: Double,
    val billingCycleDate: Int,      // day of month (1-31)
    val paymentDueDate: Int,        // day of month (1-31)
    val colorHex: String,
    val userId: String
)

@Entity(
    tableName = "attachments",
    foreignKeys = [ForeignKey(
        entity = TransactionEntity::class,
        parentColumns = ["id"],
        childColumns = ["transactionId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("transactionId")]
)
data class AttachmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transactionId: Long,
    val fileName: String,
    val filePath: String,
    val mimeType: String,
    val fileSizeBytes: Long
)

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val totalLimit: Double,
    val period: BudgetPeriod,
    val year: Int,
    val month: Int?,
    val applicableCategoryIds: String,  // JSON array
    val categoryLimits: String = "{}",  // JSON object {categoryId: limit}
    val userId: String
)

@Entity(tableName = "tags", indices = [Index(value = ["name", "userId"], unique = true)])
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val userId: String
)

@Entity(
    tableName = "scheduled_transactions",
    indices = [
        Index(value = ["userId", "nextRunAtMillis"]),
        Index(value = ["isActive", "nextRunAtMillis"])
    ]
)
data class ScheduledTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: TransactionType,
    val amount: Double,
    val categoryId: Long,
    val paymentModeId: Long?,
    val creditCardId: Long?,
    val toPaymentModeId: Long?,
    val toCreditCardId: Long?,
    val note: String,
    val dateTimeMillis: Long,
    val tags: String,
    val frequency: ScheduledFrequency,
    val nextRunAtMillis: Long,
    val lastGeneratedAtMillis: Long?,
    val isActive: Boolean,
    val reminderMinutes: Long = 0,
    val userId: String
)
