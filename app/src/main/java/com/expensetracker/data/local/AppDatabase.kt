package com.expensetracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.expensetracker.data.local.converter.Converters
import com.expensetracker.data.local.dao.*
import com.expensetracker.data.local.entity.*

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        BankAccountEntity::class,
        PaymentModeEntity::class,
        CreditCardEntity::class,
        AttachmentEntity::class,
        BudgetEntity::class,
        TagEntity::class
    ],
    version = 3,            // bumped 2 → 3 (added credit_cards, updated transactions)
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun bankAccountDao(): BankAccountDao
    abstract fun paymentModeDao(): PaymentModeDao
    abstract fun creditCardDao(): CreditCardDao
    abstract fun attachmentDao(): AttachmentDao
    abstract fun budgetDao(): BudgetDao
    abstract fun tagDao(): TagDao

    companion object {
        const val DATABASE_NAME = "expense_tracker_db"
    }
}
