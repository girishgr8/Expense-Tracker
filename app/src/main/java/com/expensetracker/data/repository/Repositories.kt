package com.expensetracker.data.repository

import android.content.Context
import android.net.Uri
import com.expensetracker.data.export.CsvExporter
import com.expensetracker.data.export.PdfExporter
import com.expensetracker.data.local.dao.AttachmentDao
import com.expensetracker.data.local.dao.BalanceAdjustmentDao
import com.expensetracker.data.local.dao.BankAccountDao
import com.expensetracker.data.local.dao.BudgetDao
import com.expensetracker.data.local.dao.CategoryDao
import com.expensetracker.data.local.dao.CreditCardDao
import com.expensetracker.data.local.dao.PaymentModeDao
import com.expensetracker.data.local.dao.ScheduledTransactionDao
import com.expensetracker.data.local.dao.TagDao
import com.expensetracker.data.local.dao.TransactionDao
import com.expensetracker.data.local.entity.AttachmentEntity
import com.expensetracker.data.local.entity.TagEntity
import com.expensetracker.data.local.entity.TransactionEntity
import com.expensetracker.data.local.entity.toDomain
import com.expensetracker.data.local.entity.toEntity
import com.expensetracker.domain.model.Attachment
import com.expensetracker.domain.model.BalanceAdjustment
import com.expensetracker.domain.model.BankAccount
import com.expensetracker.domain.model.Budget
import com.expensetracker.domain.model.Category
import com.expensetracker.domain.model.CreditCard
import com.expensetracker.domain.model.ExportFilter
import com.expensetracker.domain.model.PaymentMode
import com.expensetracker.domain.model.PaymentModeType
import com.expensetracker.domain.model.ScheduledTransaction
import com.expensetracker.domain.model.Tag
import com.expensetracker.domain.model.Transaction
import com.expensetracker.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

// ─── Interfaces ───────────────────────────────────────────────────────────────

interface TransactionRepository {
    fun getAllTransactions(userId: String): Flow<List<Transaction>>
    fun getRecentTransactions(userId: String, limit: Int = 5): Flow<List<Transaction>>
    fun searchTransactions(userId: String, query: String): Flow<List<Transaction>>
    suspend fun getTransactionById(id: Long): Transaction?
    suspend fun insertTransaction(transaction: Transaction): Long
    suspend fun updateTransaction(transaction: Transaction)
    suspend fun deleteTransaction(id: Long)
    suspend fun getTotalByType(
        userId: String,
        type: TransactionType,
        start: LocalDateTime?,
        end: LocalDateTime?
    ): Double

    fun getTotalByTypeFlow(
        userId: String,
        type: TransactionType,
        start: LocalDateTime?,
        end: LocalDateTime?
    ): Flow<Double>

    suspend fun getExpenseByCategories(
        userId: String,
        categoryIds: List<Long>,
        start: LocalDateTime?,
        end: LocalDateTime?
    ): Double

    fun getExpenseByCategoriesFlow(
        userId: String,
        categoryIds: List<Long>,
        start: LocalDateTime?,
        end: LocalDateTime?
    ): Flow<Double>

    suspend fun getAllExpense(userId: String, start: LocalDateTime?, end: LocalDateTime?): Double

    suspend fun getAllTransactionsOneShot(userId: String): List<Transaction>
    suspend fun getTransactionsInRangeOneShot(
        userId: String,
        start: LocalDateTime,
        end: LocalDateTime
    ): List<Transaction>

    suspend fun getAvailableFilters(userId: String): List<ExportFilter>
}

interface CategoryRepository {
    fun getAllCategories(userId: String): Flow<List<Category>>
    fun getCategoriesByType(userId: String, type: TransactionType): Flow<List<Category>>
    suspend fun getCategoryById(id: Long): Category?
    suspend fun insertCategory(category: Category): Long
    suspend fun updateCategory(category: Category)
    suspend fun updateCategoryOrder(categories: List<Category>)
    suspend fun deleteCategory(category: Category)
    suspend fun seedDefaultCategories(userId: String)
}

interface BankAccountRepository {
    fun getAllAccounts(userId: String): Flow<List<BankAccount>>
    suspend fun getAccountById(id: Long): BankAccount?
    suspend fun insertAccount(account: BankAccount): Long
    suspend fun updateAccount(account: BankAccount)
    suspend fun deleteAccount(account: BankAccount)
}

interface BalanceAdjustmentRepository {
    fun getAdjustmentsForAccount(bankAccountId: Long, userId: String): Flow<List<BalanceAdjustment>>
    fun getAdjustmentsForCreditCard(creditCardId: Long, userId: String): Flow<List<BalanceAdjustment>>
    fun getAdjustmentsForPaymentMode(paymentModeId: Long, userId: String): Flow<List<BalanceAdjustment>>
    fun getAllAdjustments(userId: String): Flow<List<BalanceAdjustment>>
    suspend fun insertAdjustment(adjustment: BalanceAdjustment): Long
}

interface PaymentModeRepository {
    fun getAllModes(userId: String): Flow<List<PaymentMode>>
    fun getModesForAccount(bankAccountId: Long, userId: String): Flow<List<PaymentMode>>
    suspend fun getModeById(id: Long): PaymentMode?
    suspend fun insertMode(mode: PaymentMode): Long
    suspend fun updateMode(mode: PaymentMode)
    suspend fun deleteMode(mode: PaymentMode)
    suspend fun seedDefaultModes(userId: String)
}

interface CreditCardRepository {
    fun getAllCards(userId: String): Flow<List<CreditCard>>
    suspend fun getCardById(id: Long): CreditCard?
    suspend fun insertCard(card: CreditCard): Long
    suspend fun updateCard(card: CreditCard)
    suspend fun deleteCard(card: CreditCard)
}

interface AttachmentRepository {
    fun getAttachmentsForTransaction(transactionId: Long): Flow<List<Attachment>>
    suspend fun insertAttachment(attachment: Attachment): Long
    suspend fun insertAttachments(attachments: List<Attachment>)
    suspend fun deleteAttachment(attachment: Attachment)
    suspend fun deleteAttachmentsForTransaction(transactionId: Long)
}

interface BudgetRepository {
    fun getAllBudgets(userId: String): Flow<List<Budget>>
    suspend fun getBudgetById(id: Long): Budget?
    suspend fun getBudgetForPeriod(userId: String, year: Int, month: Int?): Budget?
    suspend fun insertBudget(budget: Budget): Long
    suspend fun updateBudget(budget: Budget)
    suspend fun deleteBudget(budget: Budget)
}

interface TagRepository {
    suspend fun getAllTagsOneShot(userId: String): List<Tag>
    fun getAllTags(userId: String): Flow<List<Tag>>
    suspend fun searchTags(userId: String, query: String): List<Tag>
    suspend fun insertTag(tag: Tag): Long
    suspend fun deleteTag(tag: Tag)
}

interface ScheduledTransactionRepository {
    fun getAllScheduledTransactions(userId: String): Flow<List<ScheduledTransaction>>
    suspend fun getScheduledTransactionById(id: Long): ScheduledTransaction?
    suspend fun insertScheduledTransaction(schedule: ScheduledTransaction): Long
    suspend fun updateScheduledTransaction(schedule: ScheduledTransaction)
    suspend fun deleteScheduledTransaction(schedule: ScheduledTransaction)
}

interface ExportRepository {
    suspend fun exportTransactions(
        context: Context,
        userId: String,
        userName: String,
        userEmail: String,
        filter: ExportFilter,
        isPdf: Boolean
    ): Uri?
}

// ─── Implementations ──────────────────────────────────────────────────────────

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val paymentModeDao: PaymentModeDao,
    private val creditCardDao: CreditCardDao,
    private val bankAccountDao: BankAccountDao,
    private val attachmentDao: AttachmentDao
) : TransactionRepository {

    private fun toEpochMilli(dt: LocalDateTime?) =
        dt?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()

    override fun getAllTransactions(userId: String): Flow<List<Transaction>> =
        transactionDao.getAllTransactions(userId).map { entities ->
            enrichTransactions(entities, userId = userId, includeAttachments = false)
        }

    override fun getRecentTransactions(userId: String, limit: Int): Flow<List<Transaction>> =
        transactionDao.getRecentTransactionsFlow(userId, limit).map { entities ->
            enrichTransactions(entities, userId = userId, includeAttachments = false)
        }

    override fun searchTransactions(userId: String, query: String): Flow<List<Transaction>> =
        transactionDao.searchTransactions(userId, query).map { entities ->
            enrichTransactions(entities, userId = userId, includeAttachments = false)
        }

    private suspend fun TransactionEntity.enriched(): Transaction {
        val category = categoryDao.getCategoryById(categoryId)
        val attachments =
            attachmentDao.getAttachmentsForTransactionOneShot(id).map { it.toDomain() }

        // Resolve the "from" payment label — either a PaymentMode or CreditCard
        val fromLabel = when {
            paymentModeId != null -> {
                val mode = paymentModeDao.getModeById(paymentModeId)
                val bankName =
                    mode?.bankAccountId?.let { bankAccountDao.getAccountById(it)?.name } ?: ""
                mode?.toDomain(bankName)?.displayLabel ?: ""
            }

            creditCardId != null -> {
                creditCardDao.getCardById(creditCardId)?.toDomain()?.displayLabel ?: ""
            }

            else -> ""
        }

        // Resolve the "to" payment label for transfers
        val toLabel = when {
            toPaymentModeId != null -> {
                val mode = paymentModeDao.getModeById(toPaymentModeId)
                val bankName =
                    mode?.bankAccountId?.let { bankAccountDao.getAccountById(it)?.name } ?: ""
                mode?.toDomain(bankName)?.displayLabel ?: ""
            }

            toCreditCardId != null -> {
                creditCardDao.getCardById(toCreditCardId)?.toDomain()?.displayLabel ?: ""
            }

            else -> ""
        }

        return toDomain(
            categoryName = category?.name ?: "",
            categoryIcon = category?.icon ?: "category",
            categoryColorHex = category?.colorHex ?: "#6750A4",
            paymentModeName = fromLabel,
            toPaymentModeName = toLabel,
            attachments = attachments
        )
    }

    override suspend fun getTransactionById(id: Long): Transaction? =
        transactionDao.getTransactionById(id)?.enriched()

    private suspend fun enrichTransactions(
        entities: List<TransactionEntity>,
        userId: String,
        includeAttachments: Boolean
    ): List<Transaction> {
        if (entities.isEmpty()) return emptyList()

        val categoriesById = categoryDao.getAllCategoriesOneShot(userId)
            .associateBy { it.id }
        val bankAccountsById = bankAccountDao.getAllAccountsOneShot(userId)
            .associateBy { it.id }
        val paymentModesById = paymentModeDao.getAllModesOneShot(userId)
            .associateBy { it.id }
        val creditCardsById = creditCardDao.getAllCardsOneShot(userId)
            .associateBy { it.id }
        val attachmentsByTxnId = if (includeAttachments) {
            attachmentDao.getAttachmentsForTransactions(entities.map { it.id })
                .groupBy { it.transactionId }
        } else {
            emptyMap<Long, List<AttachmentEntity>>()
        }

        return entities.map { entity ->
            val category = categoriesById[entity.categoryId]
            val fromLabel = when {
                entity.paymentModeId != null -> {
                    val mode = paymentModesById[entity.paymentModeId]
                    val bankName = mode?.bankAccountId?.let { bankAccountsById[it]?.name } ?: ""
                    mode?.toDomain(bankName)?.displayLabel ?: ""
                }
                entity.creditCardId != null -> {
                    creditCardsById[entity.creditCardId]?.toDomain()?.displayLabel ?: ""
                }
                else -> ""
            }
            val toLabel = when {
                entity.toPaymentModeId != null -> {
                    val mode = paymentModesById[entity.toPaymentModeId]
                    val bankName = mode?.bankAccountId?.let { bankAccountsById[it]?.name } ?: ""
                    mode?.toDomain(bankName)?.displayLabel ?: ""
                }
                entity.toCreditCardId != null -> {
                    creditCardsById[entity.toCreditCardId]?.toDomain()?.displayLabel ?: ""
                }
                else -> ""
            }

            entity.toDomain(
                categoryName = category?.name ?: "",
                categoryIcon = category?.icon ?: "category",
                categoryColorHex = category?.colorHex ?: "#6750A4",
                paymentModeName = fromLabel,
                toPaymentModeName = toLabel,
                attachments = attachmentsByTxnId[entity.id]?.map { it.toDomain() } ?: emptyList()
            )
        }
    }

    override suspend fun insertTransaction(transaction: Transaction): Long {
        val id = transactionDao.insertTransaction(transaction.toEntity())
        if (transaction.attachments.isNotEmpty())
            attachmentDao.insertAttachments(
                transaction.attachments.map { it.copy(transactionId = id).toEntity() }
            )
        // Adjust balances for the new transaction
        applyBalanceEffect(transaction, delta = +1)
        return id
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        // Reverse the old transaction's effect first, then apply the new one
        transactionDao.getTransactionById(transaction.id)?.enriched()?.let { old ->
            applyBalanceEffect(old, delta = -1)
        }
        transactionDao.updateTransaction(transaction.toEntity())
        attachmentDao.deleteAttachmentsForTransaction(transaction.id)
        if (transaction.attachments.isNotEmpty())
            attachmentDao.insertAttachments(transaction.attachments.map { it.toEntity() })
        applyBalanceEffect(transaction, delta = +1)
    }

    override suspend fun deleteTransaction(id: Long) {
        // Reverse the deleted transaction's effect on balances
        transactionDao.getTransactionById(id)?.enriched()?.let { txn ->
            applyBalanceEffect(txn, delta = -1)
        }
        transactionDao.deleteTransactionById(id)
    }

    /**
     * Adjusts bank account balance and credit card available limit based on a transaction.
     * [delta] = +1 when adding the effect (insert), -1 when reversing it (delete/old update).
     *
     * Logic:
     *  - EXPENSE via PaymentMode linked to bank → decrease bank balance
     *  - EXPENSE via CreditCard → decrease card's availableLimit (used credit goes up)
     *  - INCOME  via PaymentMode linked to bank → increase bank balance
     *  - INCOME  via CreditCard → increase availableLimit (credit payment received)
     *  - TRANSFER from PaymentMode → decrease "from" bank balance
     *  - TRANSFER to   PaymentMode → increase "to"   bank balance
     */
    private suspend fun applyBalanceEffect(txn: Transaction, delta: Int) {
        val amount = txn.amount * delta

        when (txn.type) {
            TransactionType.EXPENSE -> {
                txn.paymentModeId?.let { modeId ->
                    adjustBankBalance(modeId, -amount)
                }
                txn.creditCardId?.let { cardId ->
                    adjustCardAvailableLimit(cardId, -amount)
                }
            }

            TransactionType.INCOME -> {
                txn.paymentModeId?.let { modeId ->
                    adjustBankBalance(modeId, +amount)
                }
                txn.creditCardId?.let { cardId ->
                    // Income to credit card = card payment received, limit restored
                    adjustCardAvailableLimit(cardId, +amount)
                }
            }

            TransactionType.TRANSFER -> {
                // Deduct from source
                txn.paymentModeId?.let { modeId -> adjustBankBalance(modeId, -amount) }
                txn.creditCardId?.let { cardId -> adjustCardAvailableLimit(cardId, -amount) }
                // Add to destination
                txn.toPaymentModeId?.let { modeId -> adjustBankBalance(modeId, +amount) }
                txn.toCreditCardId?.let { cardId -> adjustCardAvailableLimit(cardId, +amount) }
            }
        }
    }

    private suspend fun adjustBankBalance(paymentModeId: Long, delta: Double) {
        val mode = paymentModeDao.getModeById(paymentModeId) ?: return
        val bankAccountId = mode.bankAccountId ?: return
        val account = bankAccountDao.getAccountById(bankAccountId) ?: return
        bankAccountDao.updateAccount(account.copy(balance = account.balance + delta))
    }

    private suspend fun adjustCardAvailableLimit(creditCardId: Long, delta: Double) {
        val card = creditCardDao.getCardById(creditCardId) ?: return
        val newLimit = (card.availableLimit + delta).coerceAtLeast(0.0)
        creditCardDao.updateCard(card.copy(availableLimit = newLimit))
    }

    override suspend fun getTotalByType(
        userId: String, type: TransactionType,
        start: LocalDateTime?, end: LocalDateTime?
    ): Double = transactionDao.getTotalByType(
        userId, type.name, toEpochMilli(start), toEpochMilli(end)
    )

    override fun getTotalByTypeFlow(
        userId: String, type: TransactionType,
        start: LocalDateTime?, end: LocalDateTime?
    ): Flow<Double> = transactionDao.getTotalByTypeFlow(
        userId, type.name, toEpochMilli(start), toEpochMilli(end)
    )

    override fun getExpenseByCategoriesFlow(
        userId: String, categoryIds: List<Long>,
        start: LocalDateTime?, end: LocalDateTime?
    ): Flow<Double> = if (categoryIds.isEmpty())
        transactionDao.getAllExpenseFlow(userId, toEpochMilli(start), toEpochMilli(end))
    else
        transactionDao.getExpenseByCategoriesFlow(
            userId, categoryIds, toEpochMilli(start), toEpochMilli(end)
        )

    override suspend fun getExpenseByCategories(
        userId: String, categoryIds: List<Long>,
        start: LocalDateTime?, end: LocalDateTime?
    ): Double = if (categoryIds.isEmpty())
        transactionDao.getAllExpense(userId, toEpochMilli(start), toEpochMilli(end))
    else
        transactionDao.getExpenseByCategories(
            userId, categoryIds, toEpochMilli(start), toEpochMilli(end)
        )

    override suspend fun getAllExpense(
        userId: String, start: LocalDateTime?, end: LocalDateTime?
    ): Double = transactionDao.getAllExpense(userId, toEpochMilli(start), toEpochMilli(end))

    override suspend fun getAllTransactionsOneShot(userId: String): List<Transaction> =
        enrichTransactions(
            transactionDao.getAllTransactionsOneShot(userId),
            userId = userId,
            includeAttachments = false
        )

    override suspend fun getTransactionsInRangeOneShot(
        userId: String,
        start: LocalDateTime,
        end: LocalDateTime
    ): List<Transaction> = enrichTransactions(
        transactionDao.getTransactionsInRangeOneShot(
            userId = userId,
            startMillis = toEpochMilli(start) ?: 0L,
            endMillis = toEpochMilli(end) ?: 0L
        ),
        userId = userId,
        includeAttachments = false
    )

    override suspend fun getAvailableFilters(userId: String): List<ExportFilter> {
        val result = transactionDao.getAvailableYearMonths(userId)

        val filters = mutableListOf<ExportFilter>()

        // Always include All Time
        filters.add(ExportFilter.AllTime)

        // Group by year
        val grouped = result.groupBy { it.year }

        grouped.forEach { (year, months) ->

            val yearInt = year.toInt()

            // Add year filter
            filters.add(ExportFilter.Year(yearInt))

            // Add months under that year
            months.forEach {
                filters.add(
                    ExportFilter.Month(
                        year = yearInt,
                        month = it.month.toInt()
                    )
                )
            }
        }

        return filters
    }
}

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao
) : CategoryRepository {
    override fun getAllCategories(userId: String): Flow<List<Category>> =
        categoryDao.getAllCategories(userId).map { it.map { e -> e.toDomain() } }

    override fun getCategoriesByType(userId: String, type: TransactionType): Flow<List<Category>> =
        categoryDao.getCategoriesByType(userId, type.name).map { it.map { e -> e.toDomain() } }

    override suspend fun getCategoryById(id: Long): Category? =
        categoryDao.getCategoryById(id)?.toDomain()

    override suspend fun insertCategory(category: Category): Long =
        categoryDao.insertCategory(category.toEntity())

    override suspend fun updateCategory(category: Category) =
        categoryDao.updateCategory(category.toEntity())

    override suspend fun updateCategoryOrder(categories: List<Category>) =
        categoryDao.updateCategories(categories.map { it.toEntity() })

    override suspend fun deleteCategory(category: Category) =
        categoryDao.deleteCategory(category.toEntity())

    override suspend fun seedDefaultCategories(userId: String) {
        if (categoryDao.getCategoryCount(userId) > 0) return
        categoryDao.insertCategories(DefaultCategories.list.map { it.toEntity() })
    }
}

@Singleton
class BankAccountRepositoryImpl @Inject constructor(
    private val dao: BankAccountDao
) : BankAccountRepository {
    override fun getAllAccounts(userId: String): Flow<List<BankAccount>> =
        dao.getAllAccounts(userId).map { it.map { e -> e.toDomain() } }

    override suspend fun getAccountById(id: Long): BankAccount? =
        dao.getAccountById(id)?.toDomain()

    override suspend fun insertAccount(account: BankAccount): Long =
        dao.insertAccount(account.toEntity())

    override suspend fun updateAccount(account: BankAccount) =
        dao.updateAccount(account.toEntity())

    override suspend fun deleteAccount(account: BankAccount) =
        dao.deleteAccount(account.toEntity())
}

@Singleton
class BalanceAdjustmentRepositoryImpl @Inject constructor(
    private val dao: BalanceAdjustmentDao
) : BalanceAdjustmentRepository {
    override fun getAdjustmentsForAccount(
        bankAccountId: Long,
        userId: String
    ): Flow<List<BalanceAdjustment>> =
        dao.getAdjustmentsForAccount(bankAccountId, userId).map { list ->
            list.map { it.toDomain() }
        }

    override fun getAdjustmentsForCreditCard(
        creditCardId: Long,
        userId: String
    ): Flow<List<BalanceAdjustment>> =
        dao.getAdjustmentsForCreditCard(creditCardId, userId).map { list ->
            list.map { it.toDomain() }
        }

    override fun getAdjustmentsForPaymentMode(
        paymentModeId: Long,
        userId: String
    ): Flow<List<BalanceAdjustment>> =
        dao.getAdjustmentsForPaymentMode(paymentModeId, userId).map { list ->
            list.map { it.toDomain() }
        }

    override fun getAllAdjustments(userId: String): Flow<List<BalanceAdjustment>> =
        dao.getAllAdjustments(userId).map { list ->
            list.map { it.toDomain() }
        }

    override suspend fun insertAdjustment(adjustment: BalanceAdjustment): Long =
        dao.insertAdjustment(adjustment.toEntity())
}

@Singleton
class PaymentModeRepositoryImpl @Inject constructor(
    private val modeDao: PaymentModeDao,
    private val bankAccountDao: BankAccountDao
) : PaymentModeRepository {
    override fun getAllModes(userId: String): Flow<List<PaymentMode>> =
        modeDao.getAllModes(userId).map { list ->
            list.map { entity ->
                val bankName = entity.bankAccountId
                    ?.let { bankAccountDao.getAccountById(it)?.name } ?: ""
                entity.toDomain(bankName)
            }
        }

    override fun getModesForAccount(bankAccountId: Long, userId: String): Flow<List<PaymentMode>> =
        modeDao.getModesForAccount(bankAccountId, userId).map { list ->
            val bankName = bankAccountDao.getAccountById(bankAccountId)?.name ?: ""
            list.map { it.toDomain(bankName) }
        }

    override suspend fun getModeById(id: Long): PaymentMode? =
        modeDao.getModeById(id)?.let { entity ->
            val bankName = entity.bankAccountId
                ?.let { bankAccountDao.getAccountById(it)?.name } ?: ""
            entity.toDomain(bankName)
        }

    override suspend fun insertMode(mode: PaymentMode): Long = modeDao.insertMode(mode.toEntity())
    override suspend fun updateMode(mode: PaymentMode) = modeDao.updateMode(mode.toEntity())
    override suspend fun deleteMode(mode: PaymentMode) = modeDao.deleteMode(mode.toEntity())

    /** Seeds Cash as a default standalone payment mode if no modes exist yet. */
    override suspend fun seedDefaultModes(userId: String) {
        val existing = modeDao.getAllModesOneShot(userId)
        if (existing.isEmpty()) {
            modeDao.insertMode(
                com.expensetracker.data.local.entity.PaymentModeEntity(
                    bankAccountId = null,
                    type = PaymentModeType.CASH,
                    identifier = "",
                    userId = userId
                )
            )
        }
    }
}

@Singleton
class CreditCardRepositoryImpl @Inject constructor(
    private val dao: CreditCardDao
) : CreditCardRepository {
    override fun getAllCards(userId: String): Flow<List<CreditCard>> =
        dao.getAllCards(userId).map { it.map { e -> e.toDomain() } }

    override suspend fun getCardById(id: Long): CreditCard? =
        dao.getCardById(id)?.toDomain()

    override suspend fun insertCard(card: CreditCard): Long =
        dao.insertCard(card.toEntity())

    override suspend fun updateCard(card: CreditCard) =
        dao.updateCard(card.toEntity())

    override suspend fun deleteCard(card: CreditCard) =
        dao.deleteCard(card.toEntity())
}

@Singleton
class AttachmentRepositoryImpl @Inject constructor(
    private val dao: AttachmentDao
) : AttachmentRepository {
    override fun getAttachmentsForTransaction(transactionId: Long): Flow<List<Attachment>> =
        dao.getAttachmentsForTransaction(transactionId).map { it.map { e -> e.toDomain() } }

    override suspend fun insertAttachment(attachment: Attachment): Long =
        dao.insertAttachment(attachment.toEntity())

    override suspend fun insertAttachments(attachments: List<Attachment>) =
        dao.insertAttachments(attachments.map { it.toEntity() })

    override suspend fun deleteAttachment(attachment: Attachment) =
        dao.deleteAttachment(attachment.toEntity())

    override suspend fun deleteAttachmentsForTransaction(transactionId: Long) =
        dao.deleteAttachmentsForTransaction(transactionId)
}

@Singleton
class BudgetRepositoryImpl @Inject constructor(
    private val dao: BudgetDao
) : BudgetRepository {
    override fun getAllBudgets(userId: String): Flow<List<Budget>> =
        dao.getAllBudgets(userId).map { it.map { e -> e.toDomain() } }

    override suspend fun getBudgetById(id: Long): Budget? =
        dao.getBudgetById(id)?.toDomain()

    override suspend fun getBudgetForPeriod(userId: String, year: Int, month: Int?): Budget? =
        dao.getBudgetForPeriod(userId, year, month)?.toDomain()

    override suspend fun insertBudget(budget: Budget): Long = dao.insertBudget(budget.toEntity())
    override suspend fun updateBudget(budget: Budget) = dao.updateBudget(budget.toEntity())
    override suspend fun deleteBudget(budget: Budget) = dao.deleteBudget(budget.toEntity())
}

@Singleton
class TagRepositoryImpl @Inject constructor(
    private val tagDao: TagDao
) : TagRepository {
    private suspend fun enrichTags(
        entities: List<TagEntity>,
        userId: String,
    ): List<Tag> {
        if (entities.isEmpty()) return emptyList()

        return entities.map { entity ->
            val name = entity.name
            val userId = userId

            entity.toDomain(
                name = name,
                userId = userId
            )
        }
    }

    override suspend fun getAllTagsOneShot(userId: String): List<Tag> =
        enrichTags(
            tagDao.getAllTagsOneShot(userId),
            userId = userId,
        )


    override fun getAllTags(userId: String): Flow<List<Tag>> =
        tagDao.getAllTags(userId).map { it.map { e -> e.toDomain(e.name, e.userId) } }

    override suspend fun searchTags(userId: String, query: String): List<Tag> =
        tagDao.searchTags(userId, query).map { it.toDomain(it.name, it.userId) }

    override suspend fun insertTag(tag: Tag): Long = tagDao.insertTag(tag.toEntity())
    override suspend fun deleteTag(tag: Tag) = tagDao.deleteTag(tag.toEntity())
}

@Singleton
class ScheduledTransactionRepositoryImpl @Inject constructor(
    private val dao: ScheduledTransactionDao
) : ScheduledTransactionRepository {
    override fun getAllScheduledTransactions(userId: String): Flow<List<ScheduledTransaction>> =
        dao.getAllScheduledTransactions(userId).map { list -> list.map { it.toDomain() } }

    override suspend fun getScheduledTransactionById(id: Long): ScheduledTransaction? =
        dao.getScheduledTransactionById(id)?.toDomain()

    override suspend fun insertScheduledTransaction(schedule: ScheduledTransaction): Long =
        dao.insertScheduledTransaction(schedule.toEntity())

    override suspend fun updateScheduledTransaction(schedule: ScheduledTransaction) =
        dao.updateScheduledTransaction(schedule.toEntity())

    override suspend fun deleteScheduledTransaction(schedule: ScheduledTransaction) =
        dao.deleteScheduledTransaction(schedule.toEntity())
}

@Singleton
class ExportRepositoryImpl @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ExportRepository {

    override suspend fun exportTransactions(
        context: Context,
        userId: String,
        userName: String,
        userEmail: String,
        filter: ExportFilter,
        isPdf: Boolean
    ): Uri? {
        // 1️⃣ Fetch all transactions (already enriched in your repo)
        val allTransactions = transactionRepository
            .getAllTransactionsOneShot(userId)

        // 2️⃣ Apply filter
        val filteredTransactions = applyFilter(allTransactions, filter)

        // 3️⃣ Sort (important for clean export)
        val sortedTransactions = filteredTransactions.sortedByDescending {
            it.dateTime
        }
        val currencySymbol = userPreferencesRepository.currencySymbol.first()
        val currencyFormat = userPreferencesRepository.currencyFormat.first()

        val uri = if (isPdf) {
            PdfExporter.generate(
                context,
                sortedTransactions,
                userName,
                userEmail,
                filter.displayName,
                currencySymbol,
                currencyFormat
            )
        } else {
            CsvExporter.generate(context, sortedTransactions)
        }
        return uri
    }

    // ─────────────────────────────────────────────────────────────
    // FILTER LOGIC (CLEAN + CENTRALIZED)
    // ─────────────────────────────────────────────────────────────

    private fun applyFilter(
        transactions: List<Transaction>,
        filter: ExportFilter
    ): List<Transaction> {

        return when (filter) {

            is ExportFilter.AllTime -> transactions

            is ExportFilter.Year -> {
                transactions.filter {
                    it.dateTime.year == filter.year
                }
            }

            is ExportFilter.Month -> {
                transactions.filter {
                    it.dateTime.year == filter.year &&
                            it.dateTime.monthValue == filter.month
                }
            }
        }
    }
}
