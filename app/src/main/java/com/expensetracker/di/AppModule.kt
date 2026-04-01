package com.expensetracker.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.expensetracker.data.local.AppDatabase
import com.expensetracker.data.local.dao.AttachmentDao
import com.expensetracker.data.local.dao.BankAccountDao
import com.expensetracker.data.local.dao.BudgetDao
import com.expensetracker.data.local.dao.CategoryDao
import com.expensetracker.data.local.dao.CreditCardDao
import com.expensetracker.data.local.dao.PaymentModeDao
import com.expensetracker.data.local.dao.TagDao
import com.expensetracker.data.local.dao.TransactionDao
import com.expensetracker.data.repository.AttachmentRepository
import com.expensetracker.data.repository.AttachmentRepositoryImpl
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

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
            .addMigrations(MIGRATION_3_4)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideTransactionDao(db: AppDatabase): TransactionDao = db.transactionDao()

    @Provides
    fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()

    @Provides
    fun provideBankAccountDao(db: AppDatabase): BankAccountDao = db.bankAccountDao()

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
        transactionRepository: TransactionRepository
    ): ExportRepository {
        return ExportRepositoryImpl(transactionRepository)
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
