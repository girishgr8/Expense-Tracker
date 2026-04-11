package com.expensetracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.expensetracker.data.local.converter.Converters
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
import com.expensetracker.data.local.entity.BalanceAdjustmentEntity
import com.expensetracker.data.local.entity.BankAccountEntity
import com.expensetracker.data.local.entity.BudgetEntity
import com.expensetracker.data.local.entity.CategoryEntity
import com.expensetracker.data.local.entity.CreditCardEntity
import com.expensetracker.data.local.entity.PaymentModeEntity
import com.expensetracker.data.local.entity.ScheduledTransactionEntity
import com.expensetracker.data.local.entity.TagEntity
import com.expensetracker.data.local.entity.TransactionEntity

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        BankAccountEntity::class,
        BalanceAdjustmentEntity::class,
        PaymentModeEntity::class,
        CreditCardEntity::class,
        AttachmentEntity::class,
        BudgetEntity::class,
        TagEntity::class,
        ScheduledTransactionEntity::class
    ],
    version = 8,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun bankAccountDao(): BankAccountDao
    abstract fun balanceAdjustmentDao(): BalanceAdjustmentDao
    abstract fun paymentModeDao(): PaymentModeDao
    abstract fun creditCardDao(): CreditCardDao
    abstract fun attachmentDao(): AttachmentDao
    abstract fun budgetDao(): BudgetDao
    abstract fun tagDao(): TagDao
    abstract fun scheduledTransactionDao(): ScheduledTransactionDao

    companion object {
        const val DATABASE_NAME = "expense_tracker_db"
    }
}
