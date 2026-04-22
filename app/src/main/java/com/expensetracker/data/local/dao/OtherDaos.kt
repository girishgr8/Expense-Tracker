package com.expensetracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.expensetracker.data.local.entity.AttachmentEntity
import com.expensetracker.data.local.entity.BalanceAdjustmentEntity
import com.expensetracker.data.local.entity.BankAccountEntity
import com.expensetracker.data.local.entity.BudgetEntity
import com.expensetracker.data.local.entity.CategoryEntity
import com.expensetracker.data.local.entity.CreditCardEntity
import com.expensetracker.data.local.entity.DebtEntity
import com.expensetracker.data.local.entity.InvestmentSnapshotEntity
import com.expensetracker.data.local.entity.PaymentModeEntity
import com.expensetracker.data.local.entity.SavingsSnapshotEntity
import com.expensetracker.data.local.entity.ScheduledTransactionEntity
import com.expensetracker.data.local.entity.TagEntity
import com.expensetracker.domain.model.DebtType
import com.expensetracker.domain.model.InvestmentType
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE userId = :userId OR userId = '' ORDER BY sortOrder ASC, name ASC")
    fun getAllCategories(userId: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE userId = :userId OR userId = '' ORDER BY sortOrder ASC, name ASC")
    suspend fun getAllCategoriesOneShot(userId: String): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE (userId = :userId OR userId = '') AND (transactionType IS NULL OR transactionType = :type) ORDER BY sortOrder ASC, name ASC")
    fun getCategoriesByType(userId: String, type: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: Long): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Update
    suspend fun updateCategories(categories: List<CategoryEntity>)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    @Query("SELECT COUNT(*) FROM categories WHERE userId = :userId OR isDefault = 1")
    suspend fun getCategoryCount(userId: String): Int
}

@Dao
interface BankAccountDao {
    @Query("SELECT * FROM bank_accounts WHERE userId = :userId ORDER BY name ASC")
    fun getAllAccounts(userId: String): Flow<List<BankAccountEntity>>

    @Query("SELECT * FROM bank_accounts WHERE userId = :userId")
    suspend fun getAllAccountsOneShot(userId: String): List<BankAccountEntity>

    @Query("SELECT * FROM bank_accounts WHERE id = :id")
    suspend fun getAccountById(id: Long): BankAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: BankAccountEntity): Long

    @Update
    suspend fun updateAccount(account: BankAccountEntity)

    @Delete
    suspend fun deleteAccount(account: BankAccountEntity)
}

@Dao
interface BalanceAdjustmentDao {
    @Query(
        "SELECT * FROM balance_adjustments " +
                "WHERE bankAccountId = :bankAccountId AND userId = :userId " +
                "ORDER BY adjustedAtMillis DESC, id DESC"
    )
    fun getAdjustmentsForAccount(
        bankAccountId: Long,
        userId: String
    ): Flow<List<BalanceAdjustmentEntity>>

    @Query(
        "SELECT * FROM balance_adjustments " +
                "WHERE creditCardId = :creditCardId AND userId = :userId " +
                "ORDER BY adjustedAtMillis DESC, id DESC"
    )
    fun getAdjustmentsForCreditCard(
        creditCardId: Long,
        userId: String
    ): Flow<List<BalanceAdjustmentEntity>>

    @Query(
        "SELECT * FROM balance_adjustments " +
                "WHERE paymentModeId = :paymentModeId AND userId = :userId " +
                "ORDER BY adjustedAtMillis DESC, id DESC"
    )
    fun getAdjustmentsForPaymentMode(
        paymentModeId: Long,
        userId: String
    ): Flow<List<BalanceAdjustmentEntity>>

    @Query(
        "SELECT * FROM balance_adjustments " +
                "WHERE userId = :userId ORDER BY adjustedAtMillis DESC, id DESC"
    )
    fun getAllAdjustments(userId: String): Flow<List<BalanceAdjustmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdjustment(adjustment: BalanceAdjustmentEntity): Long
}

@Dao
interface PaymentModeDao {
    @Query("SELECT * FROM payment_modes WHERE userId = :userId ORDER BY type ASC")
    fun getAllModes(userId: String): Flow<List<PaymentModeEntity>>

    @Query("SELECT * FROM payment_modes WHERE bankAccountId = :bankAccountId AND userId = :userId")
    fun getModesForAccount(bankAccountId: Long, userId: String): Flow<List<PaymentModeEntity>>

    @Query("SELECT * FROM payment_modes WHERE id = :id")
    suspend fun getModeById(id: Long): PaymentModeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMode(mode: PaymentModeEntity): Long

    @Update
    suspend fun updateMode(mode: PaymentModeEntity)

    @Delete
    suspend fun deleteMode(mode: PaymentModeEntity)

    @Query("DELETE FROM payment_modes WHERE bankAccountId = :bankAccountId")
    suspend fun deleteModesForAccount(bankAccountId: Long)

    @Query("SELECT * FROM payment_modes WHERE userId = :userId")
    suspend fun getAllModesOneShot(userId: String): List<PaymentModeEntity>
}

@Dao
interface CreditCardDao {
    @Query("SELECT * FROM credit_cards WHERE userId = :userId ORDER BY name ASC")
    fun getAllCards(userId: String): Flow<List<CreditCardEntity>>

    @Query("SELECT * FROM credit_cards WHERE userId = :userId")
    suspend fun getAllCardsOneShot(userId: String): List<CreditCardEntity>

    @Query("SELECT * FROM credit_cards WHERE id = :id")
    suspend fun getCardById(id: Long): CreditCardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: CreditCardEntity): Long

    @Update
    suspend fun updateCard(card: CreditCardEntity)

    @Delete
    suspend fun deleteCard(card: CreditCardEntity)
}

@Dao
interface AttachmentDao {
    @Query("SELECT * FROM attachments WHERE transactionId = :transactionId")
    fun getAttachmentsForTransaction(transactionId: Long): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM attachments WHERE transactionId = :transactionId")
    suspend fun getAttachmentsForTransactionOneShot(transactionId: Long): List<AttachmentEntity>

    @Query("SELECT * FROM attachments WHERE transactionId IN (:transactionIds)")
    suspend fun getAttachmentsForTransactions(transactionIds: List<Long>): List<AttachmentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachment(attachment: AttachmentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachments(attachments: List<AttachmentEntity>)

    @Delete
    suspend fun deleteAttachment(attachment: AttachmentEntity)

    @Query("DELETE FROM attachments WHERE transactionId = :transactionId")
    suspend fun deleteAttachmentsForTransaction(transactionId: Long)
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE userId = :userId ORDER BY year DESC, month DESC")
    fun getAllBudgets(userId: String): Flow<List<BudgetEntity>>

    @Query("SELECT * FROM budgets WHERE userId = :userId AND year = :year AND (month = :month OR month IS NULL)")
    suspend fun getBudgetForPeriod(userId: String, year: Int, month: Int?): BudgetEntity?

    @Query("SELECT * FROM budgets WHERE id = :id")
    suspend fun getBudgetById(id: Long): BudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity): Long

    @Update
    suspend fun updateBudget(budget: BudgetEntity)

    @Delete
    suspend fun deleteBudget(budget: BudgetEntity)
}

@Dao
interface TagDao {
    @Query("SELECT * FROM tags WHERE userId = :userId ORDER BY name ASC")
    suspend fun getAllTagsOneShot(userId: String): List<TagEntity>

    @Query("SELECT * FROM tags WHERE userId = :userId ORDER BY name ASC")
    fun getAllTags(userId: String): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE userId = :userId AND name LIKE '%' || :query || '%' ORDER BY name ASC LIMIT 10")
    suspend fun searchTags(userId: String, query: String): List<TagEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: TagEntity): Long

    @Delete
    suspend fun deleteTag(tag: TagEntity)
}

@Dao
interface ScheduledTransactionDao {
    @Query(
        "SELECT * FROM scheduled_transactions WHERE userId = :userId " +
            "ORDER BY nextRunAtMillis ASC, id ASC"
    )
    fun getAllScheduledTransactions(userId: String): Flow<List<ScheduledTransactionEntity>>

    @Query("SELECT * FROM scheduled_transactions WHERE id = :id")
    suspend fun getScheduledTransactionById(id: Long): ScheduledTransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduledTransaction(schedule: ScheduledTransactionEntity): Long

    @Update
    suspend fun updateScheduledTransaction(schedule: ScheduledTransactionEntity)

    @Delete
    suspend fun deleteScheduledTransaction(schedule: ScheduledTransactionEntity)
}

@Dao
interface DebtDao {
    @Query("SELECT * FROM debts WHERE userId = :userId ORDER BY createdAtMillis DESC")
    fun getAllDebts(userId: String): Flow<List<DebtEntity>>

    @Query("SELECT * FROM debts WHERE userId = :userId AND type = :type ORDER BY createdAtMillis DESC")
    fun getDebtsByType(userId: String, type: DebtType): Flow<List<DebtEntity>>

    @Query("SELECT * FROM debts WHERE id = :id")
    suspend fun getDebtById(id: Long): DebtEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebt(debt: DebtEntity): Long

    @Update
    suspend fun updateDebt(debt: DebtEntity)

    @Delete
    suspend fun deleteDebt(debt: DebtEntity)
}

@Dao
interface SavingsSnapshotDao {

    /**
     * Latest snapshot per institution.
     * Uses MAX(id) as tiebreaker when two snapshots share the same timestamp,
     * guaranteeing exactly ONE row per institutionName — no duplicate keys.
     */
    @Query("""
        SELECT * FROM savings_snapshots
        WHERE userId = :userId
          AND id = (
              SELECT MAX(s2.id) FROM savings_snapshots s2
              WHERE s2.institutionName = savings_snapshots.institutionName
                AND s2.userId = :userId
          )
        ORDER BY institutionName ASC
    """)
    fun getLatestSnapshotsPerInstitution(userId: String): Flow<List<SavingsSnapshotEntity>>

    /** Full history for one institution, newest first */
    @Query("""
        SELECT * FROM savings_snapshots
        WHERE userId = :userId AND institutionName = :institutionName
        ORDER BY recordedOnMillis DESC
    """)
    fun getHistoryForInstitution(
        userId: String,
        institutionName: String
    ): Flow<List<SavingsSnapshotEntity>>

    /** All snapshots for the user, newest first — used for history graphs */
    @Query("SELECT * FROM savings_snapshots WHERE userId = :userId ORDER BY recordedOnMillis DESC")
    fun getAllSnapshots(userId: String): Flow<List<SavingsSnapshotEntity>>

    /** INSERT only — never update */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(snapshot: SavingsSnapshotEntity): Long

    /** Delete all snapshots for an institution (soft-delete / cleanup) */
    @Query("DELETE FROM savings_snapshots WHERE userId = :userId AND institutionName = :institutionName")
    suspend fun deleteForInstitution(userId: String, institutionName: String)
}

@Dao
interface InvestmentSnapshotDao {

    /**
     * Latest snapshot per (type, subName).
     * MAX(id) as tiebreaker — guaranteed exactly ONE row per position,
     * preventing duplicate LazyColumn keys.
     */
    @Query("""
        SELECT * FROM investment_snapshots
        WHERE userId = :userId
          AND id = (
              SELECT MAX(i2.id) FROM investment_snapshots i2
              WHERE i2.type = investment_snapshots.type
                AND i2.subName = investment_snapshots.subName
                AND i2.userId = :userId
          )
        ORDER BY type ASC, subName ASC
    """)
    fun getLatestSnapshotsPerPosition(userId: String): Flow<List<InvestmentSnapshotEntity>>

    /** Full history for one position, newest first */
    @Query("""
        SELECT * FROM investment_snapshots
        WHERE userId = :userId AND type = :type AND subName = :subName
        ORDER BY recordedOnMillis DESC
    """)
    fun getHistoryForPosition(
        userId: String,
        type:    InvestmentType,
        subName: String
    ): Flow<List<InvestmentSnapshotEntity>>

    /** All snapshots newest first */
    @Query("SELECT * FROM investment_snapshots WHERE userId = :userId ORDER BY recordedOnMillis DESC")
    fun getAllSnapshots(userId: String): Flow<List<InvestmentSnapshotEntity>>

    /** INSERT only — never update */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(snapshot: InvestmentSnapshotEntity): Long

    /** Delete all snapshots for a position */
    @Query("""
        DELETE FROM investment_snapshots
        WHERE userId = :userId AND type = :type AND subName = :subName
    """)
    suspend fun deleteForPosition(userId: String, type: InvestmentType, subName: String)
}