package com.expensetracker.data.local.dao

import androidx.room.*
import com.expensetracker.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE userId = :userId OR userId = '' ORDER BY name ASC")
    fun getAllCategories(userId: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE (userId = :userId OR userId = '') AND (transactionType IS NULL OR transactionType = :type) ORDER BY name ASC")
    fun getCategoriesByType(userId: String, type: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: Long): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    @Query("SELECT COUNT(*) FROM categories WHERE userId = :userId OR isDefault = 1")
    suspend fun getCategoryCount(userId: String): Int
}

@Dao
interface BankAccountDao {
    @Query("SELECT * FROM bank_accounts WHERE userId = :userId ORDER BY name ASC")
    fun getAllAccounts(userId: String): Flow<List<BankAccountEntity>>

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
    fun getAllTags(userId: String): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE userId = :userId AND name LIKE '%' || :query || '%' ORDER BY name ASC LIMIT 10")
    suspend fun searchTags(userId: String, query: String): List<TagEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: TagEntity): Long

    @Delete
    suspend fun deleteTag(tag: TagEntity)
}