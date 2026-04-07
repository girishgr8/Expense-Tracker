package com.expensetracker.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.expensetracker.data.local.AppDatabase
import com.expensetracker.data.local.dao.AttachmentDao
import com.expensetracker.data.local.dao.BalanceAdjustmentDao
import com.expensetracker.data.local.dao.BankAccountDao
import com.expensetracker.data.local.dao.BudgetDao
import com.expensetracker.data.local.dao.CategoryDao
import com.expensetracker.data.local.dao.CreditCardDao
import com.expensetracker.data.local.dao.PaymentModeDao
import com.expensetracker.data.local.dao.TagDao
import com.expensetracker.data.local.dao.TransactionDao
import com.expensetracker.data.repository.AttachmentRepository
import com.expensetracker.data.repository.AttachmentRepositoryImpl
import com.expensetracker.data.repository.BalanceAdjustmentRepository
import com.expensetracker.data.repository.BalanceAdjustmentRepositoryImpl
import com.expensetracker.data.repository.BankAccountRepository
import com.expensetracker.data.repository.BankAccountRepositoryImpl
import com.expensetracker.data.repository.BudgetRepository
import com.expensetracker.data.repository.BudgetRepositoryImpl
import com.expensetracker.data.repository.CategoryRepository
import com.expensetracker.data.repository.CategoryRepositoryImpl
import com.expensetracker.data.repository.CreditCardRepository
import com.expensetracker.data.repository.CreditCardRepositoryImpl
import com.expensetracker.data.repository.ExportRepository
import com.expensetracker.data.repository.ExportRepositoryImpl
import com.expensetracker.data.repository.PaymentModeRepository
import com.expensetracker.data.repository.PaymentModeRepositoryImpl
import com.expensetracker.data.repository.TagRepository
import com.expensetracker.data.repository.TagRepositoryImpl
import com.expensetracker.data.repository.TransactionRepository
import com.expensetracker.data.repository.TransactionRepositoryImpl
import com.expensetracker.data.repository.UserPreferencesRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE budgets ADD COLUMN categoryLimits TEXT NOT NULL DEFAULT '{}'")
        }
    }

    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `balance_adjustments` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `bankAccountId` INTEGER NOT NULL,
                    `previousBalance` REAL NOT NULL,
                    `newBalance` REAL NOT NULL,
                    `amountDelta` REAL NOT NULL,
                    `adjustedAtMillis` INTEGER NOT NULL,
                    `userId` TEXT NOT NULL,
                    FOREIGN KEY(`bankAccountId`) REFERENCES `bank_accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_balance_adjustments_bankAccountId` " +
                        "ON `balance_adjustments` (`bankAccountId`)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_balance_adjustments_userId` " +
                        "ON `balance_adjustments` (`userId`)"
            )
        }
    }

    private val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `balance_adjustments_new` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `bankAccountId` INTEGER,
                    `creditCardId` INTEGER,
                    `paymentModeId` INTEGER,
                    `previousBalance` REAL NOT NULL,
                    `newBalance` REAL NOT NULL,
                    `amountDelta` REAL NOT NULL,
                    `adjustedAtMillis` INTEGER NOT NULL,
                    `userId` TEXT NOT NULL,
                    FOREIGN KEY(`bankAccountId`) REFERENCES `bank_accounts`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                INSERT INTO `balance_adjustments_new` (
                    `id`, `bankAccountId`, `creditCardId`, `paymentModeId`,
                    `previousBalance`, `newBalance`, `amountDelta`, `adjustedAtMillis`, `userId`
                )
                SELECT
                    `id`, `bankAccountId`, NULL, NULL,
                    `previousBalance`, `newBalance`, `amountDelta`, `adjustedAtMillis`, `userId`
                FROM `balance_adjustments`
                """.trimIndent()
            )
            db.execSQL("DROP TABLE `balance_adjustments`")
            db.execSQL("ALTER TABLE `balance_adjustments_new` RENAME TO `balance_adjustments`")
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_balance_adjustments_bankAccountId` " +
                    "ON `balance_adjustments` (`bankAccountId`)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_balance_adjustments_userId` " +
                    "ON `balance_adjustments` (`userId`)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_balance_adjustments_creditCardId` " +
                    "ON `balance_adjustments` (`creditCardId`)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_balance_adjustments_paymentModeId` " +
                    "ON `balance_adjustments` (`paymentModeId`)"
            )
        }
    }

    private val MIGRATION_6_7 = object : Migration(6, 7) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_transactions_userId_dateTimeMillis` " +
                    "ON `transactions` (`userId`, `dateTimeMillis`)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_transactions_userId_type_dateTimeMillis` " +
                    "ON `transactions` (`userId`, `type`, `dateTimeMillis`)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_transactions_userId_categoryId_dateTimeMillis` " +
                    "ON `transactions` (`userId`, `categoryId`, `dateTimeMillis`)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_transactions_userId_paymentModeId_dateTimeMillis` " +
                    "ON `transactions` (`userId`, `paymentModeId`, `dateTimeMillis`)"
            )
        }
    }

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
            .addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideTransactionDao(db: AppDatabase): TransactionDao = db.transactionDao()

    @Provides
    fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideBankAccountDao(db: AppDatabase): BankAccountDao = db.bankAccountDao()

    @Provides
    fun provideBalanceAdjustmentDao(db: AppDatabase): BalanceAdjustmentDao =
        db.balanceAdjustmentDao()

    @Provides
    fun providePaymentModeDao(db: AppDatabase): PaymentModeDao = db.paymentModeDao()

    @Provides
    fun provideCreditCardDao(db: AppDatabase): CreditCardDao = db.creditCardDao()

    @Provides
    fun provideAttachmentDao(db: AppDatabase): AttachmentDao = db.attachmentDao()

    @Provides
    fun provideBudgetDao(db: AppDatabase): BudgetDao = db.budgetDao()

    @Provides
    fun provideTagDao(db: AppDatabase): TagDao = db.tagDao()
}

@Module
@InstallIn(SingletonComponent::class)
object ExportModule {

    @Provides
    @Singleton
    fun provideExportRepository(
        transactionRepository: TransactionRepository,
        userPreferencesRepository: UserPreferencesRepository
    ): ExportRepository {
        return ExportRepositoryImpl(transactionRepository, userPreferencesRepository)
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(impl: TransactionRepositoryImpl): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindBankAccountRepository(impl: BankAccountRepositoryImpl): BankAccountRepository

    @Binds
    @Singleton
    abstract fun bindBalanceAdjustmentRepository(
        impl: BalanceAdjustmentRepositoryImpl
    ): BalanceAdjustmentRepository

    @Binds
    @Singleton
    abstract fun bindPaymentModeRepository(impl: PaymentModeRepositoryImpl): PaymentModeRepository

    @Binds
    @Singleton
    abstract fun bindCreditCardRepository(impl: CreditCardRepositoryImpl): CreditCardRepository

    @Binds
    @Singleton
    abstract fun bindAttachmentRepository(impl: AttachmentRepositoryImpl): AttachmentRepository

    @Binds
    @Singleton
    abstract fun bindBudgetRepository(impl: BudgetRepositoryImpl): BudgetRepository

    @Binds
    @Singleton
    abstract fun bindTagRepository(impl: TagRepositoryImpl): TagRepository
}
