package com.expensetracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.expensetracker.data.local.entity.TransactionEntity
import com.expensetracker.domain.model.YearMonthTuple
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY dateTimeMillis DESC")
    fun getAllTransactions(userId: String): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT * FROM transactions 
        WHERE userId = :userId 
        AND (:year IS NULL OR strftime('%Y', dateTimeMillis/1000, 'unixepoch') = :year)
        AND (:month IS NULL OR strftime('%m', dateTimeMillis/1000, 'unixepoch') = :month)
        AND (:type IS NULL OR type = :type)
        ORDER BY dateTimeMillis DESC
    """
    )
    fun getTransactionsFiltered(
        userId: String, year: String? = null, month: String? = null, type: String? = null
    ): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT * FROM transactions 
        WHERE userId = :userId 
        AND (note LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%')
        ORDER BY dateTimeMillis DESC
    """
    )
    fun searchTransactions(userId: String, query: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Query(
        """
        SELECT * FROM transactions 
        WHERE userId = :userId 
        ORDER BY dateTimeMillis DESC 
        LIMIT :limit
    """
    )
    fun getRecentTransactionsFlow(userId: String, limit: Int = 5): Flow<List<TransactionEntity>>

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0) FROM transactions 
        WHERE userId = :userId 
        AND type = :type
        AND (:startMillis IS NULL OR dateTimeMillis >= :startMillis)
        AND (:endMillis IS NULL OR dateTimeMillis <= :endMillis)
    """
    )
    suspend fun getTotalByType(
        userId: String, type: String, startMillis: Long? = null, endMillis: Long? = null
    ): Double

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0) FROM transactions 
        WHERE userId = :userId 
        AND type = :type
        AND (:startMillis IS NULL OR dateTimeMillis >= :startMillis)
        AND (:endMillis IS NULL OR dateTimeMillis <= :endMillis)
    """
    )
    fun getTotalByTypeFlow(
        userId: String, type: String, startMillis: Long? = null, endMillis: Long? = null
    ): Flow<Double>

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0) FROM transactions 
        WHERE userId = :userId 
        AND type = 'EXPENSE'
        AND categoryId IN (:categoryIds)
        AND (:startMillis IS NULL OR dateTimeMillis >= :startMillis)
        AND (:endMillis IS NULL OR dateTimeMillis <= :endMillis)
    """
    )
    suspend fun getExpenseByCategories(
        userId: String, categoryIds: List<Long>, startMillis: Long? = null, endMillis: Long? = null
    ): Double

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0) FROM transactions 
        WHERE userId = :userId 
        AND type = 'EXPENSE'
        AND categoryId IN (:categoryIds)
        AND (:startMillis IS NULL OR dateTimeMillis >= :startMillis)
        AND (:endMillis IS NULL OR dateTimeMillis <= :endMillis)
    """
    )
    fun getExpenseByCategoriesFlow(
        userId: String, categoryIds: List<Long>, startMillis: Long? = null, endMillis: Long? = null
    ): Flow<Double>

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0) FROM transactions 
        WHERE userId = :userId 
        AND type = 'EXPENSE'
        AND (:startMillis IS NULL OR dateTimeMillis >= :startMillis)
        AND (:endMillis IS NULL OR dateTimeMillis <= :endMillis)
    """
    )
    suspend fun getAllExpense(
        userId: String, startMillis: Long? = null, endMillis: Long? = null
    ): Double

    @Query(
        """
        SELECT COALESCE(SUM(amount), 0) FROM transactions 
        WHERE userId = :userId 
        AND type = 'EXPENSE'
        AND (:startMillis IS NULL OR dateTimeMillis >= :startMillis)
        AND (:endMillis IS NULL OR dateTimeMillis <= :endMillis)
    """
    )
    fun getAllExpenseFlow(
        userId: String, startMillis: Long? = null, endMillis: Long? = null
    ): Flow<Double>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("SELECT * FROM transactions WHERE userId = :userId ORDER BY dateTimeMillis DESC")
    suspend fun getAllTransactionsOneShot(userId: String): List<TransactionEntity>

    @Query(
        """
        SELECT * FROM transactions
        WHERE userId = :userId
        AND dateTimeMillis BETWEEN :startMillis AND :endMillis
        ORDER BY dateTimeMillis DESC
        """
    )
    suspend fun getTransactionsInRangeOneShot(
        userId: String,
        startMillis: Long,
        endMillis: Long
    ): List<TransactionEntity>

    @Query(
        """
    SELECT strftime('%Y', dateTimeMillis / 1000, 'unixepoch') AS year,
           strftime('%m', dateTimeMillis / 1000, 'unixepoch') AS month
    FROM transactions
    WHERE userId = :userId
    GROUP BY year, month
    ORDER BY year DESC, month DESC
    """
    )
    suspend fun getAvailableYearMonths(userId: String): List<YearMonthTuple>
}
