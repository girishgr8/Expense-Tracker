package com.expensetracker.data.local.entity

import com.expensetracker.domain.model.Attachment
import com.expensetracker.domain.model.BalanceAdjustment
import com.expensetracker.domain.model.BankAccount
import com.expensetracker.domain.model.Budget
import com.expensetracker.domain.model.Category
import com.expensetracker.domain.model.CreditCard
import com.expensetracker.domain.model.PaymentMode
import com.expensetracker.domain.model.ScheduledTransaction
import com.expensetracker.domain.model.Tag
import com.expensetracker.domain.model.Transaction
import com.google.gson.Gson
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

private val gson = Gson()

// ─── Transaction ──────────────────────────────────────────────────────────────

fun TransactionEntity.toDomain(
    categoryName: String = "",
    categoryIcon: String = "category",
    categoryColorHex: String = "#6750A4",
    paymentModeName: String = "",
    toPaymentModeName: String = "",
    attachments: List<Attachment> = emptyList()
): Transaction = Transaction(
    id = id,
    type = type,
    amount = amount,
    categoryId = categoryId,
    categoryName = categoryName,
    categoryIcon = categoryIcon,
    categoryColorHex = categoryColorHex,
    paymentModeId = paymentModeId,
    creditCardId = creditCardId,
    paymentModeName = paymentModeName,
    toPaymentModeId = toPaymentModeId,
    toCreditCardId = toCreditCardId,
    toPaymentModeName = toPaymentModeName,
    note = note,
    dateTime = LocalDateTime.ofInstant(
        Instant.ofEpochMilli(dateTimeMillis), ZoneId.systemDefault()
    ),
    tags = gson.fromJson(tags, Array<String>::class.java)?.toList() ?: emptyList(),
    attachments = attachments,
    userId = userId
)

fun Transaction.toEntity(): TransactionEntity = TransactionEntity(
    id = id,
    type = type,
    amount = amount,
    categoryId = categoryId,
    paymentModeId = paymentModeId,
    creditCardId = creditCardId,
    toPaymentModeId = toPaymentModeId,
    toCreditCardId = toCreditCardId,
    note = note,
    dateTimeMillis = dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
    tags = gson.toJson(tags),
    userId = userId
)

// ─── Category ─────────────────────────────────────────────────────────────────

fun CategoryEntity.toDomain(): Category = Category(
    id = id,
    name = name,
    icon = icon,
    colorHex = colorHex,
    transactionType = transactionType,
    isDefault = isDefault,
    userId = userId
)

fun Category.toEntity(): CategoryEntity = CategoryEntity(
    id = id,
    name = name,
    icon = icon,
    colorHex = colorHex,
    transactionType = transactionType,
    isDefault = isDefault,
    userId = userId
)

// ─── BankAccount ─────────────────────────────────────────────────────────────

fun BankAccountEntity.toDomain(): BankAccount =
    BankAccount(id = id, name = name, balance = balance, colorHex = colorHex, userId = userId)

fun BankAccount.toEntity(): BankAccountEntity =
    BankAccountEntity(id = id, name = name, balance = balance, colorHex = colorHex, userId = userId)

fun BalanceAdjustmentEntity.toDomain(): BalanceAdjustment = BalanceAdjustment(
    id = id,
    bankAccountId = bankAccountId,
    creditCardId = creditCardId,
    paymentModeId = paymentModeId,
    previousBalance = previousBalance,
    newBalance = newBalance,
    amountDelta = amountDelta,
    adjustedAt = LocalDateTime.ofInstant(
        Instant.ofEpochMilli(adjustedAtMillis), ZoneId.systemDefault()
    ),
    userId = userId
)

fun BalanceAdjustment.toEntity(): BalanceAdjustmentEntity = BalanceAdjustmentEntity(
    id = id,
    bankAccountId = bankAccountId,
    creditCardId = creditCardId,
    paymentModeId = paymentModeId,
    previousBalance = previousBalance,
    newBalance = newBalance,
    amountDelta = amountDelta,
    adjustedAtMillis = adjustedAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
    userId = userId
)

// ─── PaymentMode ──────────────────────────────────────────────────────────────

fun PaymentModeEntity.toDomain(bankAccountName: String = ""): PaymentMode = PaymentMode(
    id = id,
    bankAccountId = bankAccountId,
    bankAccountName = bankAccountName,
    type = type,
    identifier = identifier,
    userId = userId
)

fun PaymentMode.toEntity(): PaymentModeEntity = PaymentModeEntity(
    id = id, bankAccountId = bankAccountId, type = type, identifier = identifier, userId = userId
)

// ─── CreditCard ───────────────────────────────────────────────────────────────

fun CreditCardEntity.toDomain(): CreditCard = CreditCard(
    id = id,
    name = name,
    availableLimit = availableLimit,
    totalLimit = totalLimit,
    billingCycleDate = billingCycleDate,
    paymentDueDate = paymentDueDate,
    colorHex = colorHex,
    userId = userId
)

fun CreditCard.toEntity(): CreditCardEntity = CreditCardEntity(
    id = id,
    name = name,
    availableLimit = availableLimit,
    totalLimit = totalLimit,
    billingCycleDate = billingCycleDate,
    paymentDueDate = paymentDueDate,
    colorHex = colorHex,
    userId = userId
)

// ─── Attachment ───────────────────────────────────────────────────────────────

fun AttachmentEntity.toDomain(): Attachment = Attachment(
    id = id,
    transactionId = transactionId,
    fileName = fileName,
    filePath = filePath,
    mimeType = mimeType,
    fileSizeBytes = fileSizeBytes
)

fun Attachment.toEntity(): AttachmentEntity = AttachmentEntity(
    id = id,
    transactionId = transactionId,
    fileName = fileName,
    filePath = filePath,
    mimeType = mimeType,
    fileSizeBytes = fileSizeBytes
)

// ─── Budget ───────────────────────────────────────────────────────────────────

fun BudgetEntity.toDomain(): Budget = Budget(
    id = id,
    name = name,
    totalLimit = totalLimit,
    period = period,
    year = year,
    month = month,
    applicableCategoryIds = gson.fromJson(
        applicableCategoryIds, Array<Long>::class.java
    )?.toList() ?: emptyList(),
    categoryLimits = runCatching {
        @Suppress("UNCHECKED_CAST")
        (gson.fromJson(
            categoryLimits,
            Map::class.java
        ) as? Map<String, Double>)?.mapKeys { it.key.toLong() } ?: emptyMap()
    }.getOrDefault(emptyMap()),
    userId = userId
)

fun Budget.toEntity(): BudgetEntity = BudgetEntity(
    id = id,
    name = name,
    totalLimit = totalLimit,
    period = period,
    year = year,
    month = month,
    applicableCategoryIds = gson.toJson(applicableCategoryIds),
    categoryLimits = gson.toJson(categoryLimits),
    userId = userId
)

// ─── Tag ──────────────────────────────────────────────────────────────────────

fun TagEntity.toDomain(): Tag = Tag(id = id, name = name, userId = userId)
fun Tag.toEntity(): TagEntity = TagEntity(id = id, name = name, userId = userId)

fun ScheduledTransactionEntity.toDomain(): ScheduledTransaction = ScheduledTransaction(
    id = id,
    type = type,
    amount = amount,
    categoryId = categoryId,
    paymentModeId = paymentModeId,
    creditCardId = creditCardId,
    toPaymentModeId = toPaymentModeId,
    toCreditCardId = toCreditCardId,
    note = note,
    dateTime = LocalDateTime.ofInstant(
        Instant.ofEpochMilli(dateTimeMillis), ZoneId.systemDefault()
    ),
    tags = gson.fromJson(tags, Array<String>::class.java)?.toList() ?: emptyList(),
    frequency = frequency,
    nextRunAt = LocalDateTime.ofInstant(
        Instant.ofEpochMilli(nextRunAtMillis), ZoneId.systemDefault()
    ),
    lastGeneratedAt = lastGeneratedAtMillis?.let {
        LocalDateTime.ofInstant(Instant.ofEpochMilli(it), ZoneId.systemDefault())
    },
    isActive = isActive,
    userId = userId
)

fun ScheduledTransaction.toEntity(): ScheduledTransactionEntity = ScheduledTransactionEntity(
    id = id,
    type = type,
    amount = amount,
    categoryId = categoryId,
    paymentModeId = paymentModeId,
    creditCardId = creditCardId,
    toPaymentModeId = toPaymentModeId,
    toCreditCardId = toCreditCardId,
    note = note,
    dateTimeMillis = dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
    tags = gson.toJson(tags),
    frequency = frequency,
    nextRunAtMillis = nextRunAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
    lastGeneratedAtMillis = lastGeneratedAt?.atZone(ZoneId.systemDefault())?.toInstant()
        ?.toEpochMilli(),
    isActive = isActive,
    userId = userId
)
