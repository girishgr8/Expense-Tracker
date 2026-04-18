package com.expensetracker.presentation.ui.settings

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.data.repository.AuthManager
import com.expensetracker.data.repository.CategoryRepository
import com.expensetracker.data.repository.CreditCardRepository
import com.expensetracker.data.repository.PaymentModeRepository
import com.expensetracker.data.repository.TagRepository
import com.expensetracker.data.repository.TransactionRepository
import com.expensetracker.data.repository.UserPreferencesRepository
import com.expensetracker.domain.model.Category
import com.expensetracker.domain.model.CreditCard
import com.expensetracker.domain.model.PaymentMode
import com.expensetracker.domain.model.Tag
import com.expensetracker.domain.model.Transaction
import com.expensetracker.domain.model.TransactionType
import com.expensetracker.util.DriveBackupScheduler
import com.opencsv.CSVReader
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.InputStreamReader
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatterBuilder
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

data class SettingsUiState(
    // User Details
    val userName: String = "",
    val userEmail: String = "",
    val userPhotoUrl: String = "",

    // Appearance
    val themeMode: String = "system",
    val useDynamicColor: Boolean = true,
    val decimalFormat: String = "default",

    // Notifications
    val isDailyReminderEnabled: Boolean = true,
    val dailyReminderHour: Int = 19,
    val dailyReminderMinute: Int = 0,
    val isBudgetAlertEnabled: Boolean = true,

    // Preferences
    val currencySymbol: String = "₹",
    val currencyCode: String = "INR",
    val currencyFormat: String = "millions",
    val defaultCategoryId: Long = -1L,
    val defaultPaymentModeId: Long = -1L,
    val firstDayOfMonth: Int = 1,
    val categories: List<Category> = emptyList(),
    val paymentModes: List<PaymentMode> = emptyList(),
    val isHapticsEnabled: Boolean = true,

    // Backup, Restore & Export
    val isBackupEnabled: Boolean = true,
    val lastBackupDisplay: String = "Never",
    val isCheckingBackup: Boolean = false,
    val isRestoring: Boolean = false,
    val restoreError: String? = null,
    val restoreSuccess: Boolean = false,
    val isImportingTransactions: Boolean = false,
    val importTransactionsMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val authManager: AuthManager,
    private val driveBackupScheduler: DriveBackupScheduler,
    private val categoryRepository: CategoryRepository,
    private val paymentModeRepository: PaymentModeRepository,
    private val creditCardRepository: CreditCardRepository,
    private val transactionRepository: TransactionRepository,
    private val tagRepository: TagRepository,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        // Settings: User Info
        loadUserInfo()

        // Settings: Appearance
        loadAppearanceSettings()

        // Settings: Preferences
        loadPreferencesSettings()

        // Settings: Notifications
        loadNotificationsSettings()

        // Backup, Restore & Export
        loadBackupRestoreExportSettings()
    }

    private fun loadUserInfo() {
        viewModelScope.launch {
            authManager.currentUser.collect { user ->
                _uiState.update {
                    it.copy(
                        userName = user?.displayName ?: "",
                        userEmail = user?.email ?: "",
                        userPhotoUrl = user?.photoUrl?.toString() ?: ""
                    )
                }
            }
        }
    }

    private fun loadAppearanceSettings(){
        viewModelScope.launch {
            combine(
                userPreferencesRepository.themeMode, // args[0]: String
                userPreferencesRepository.useDynamicColor,    // args[1]: Boolean
                userPreferencesRepository.decimalFormat       // args[2]: String
            ) { args: Array<Any?> ->
                args
            }.collect { args ->
                _uiState.update {
                    it.copy(
                        themeMode = args[0] as String,
                        useDynamicColor = args[1] as Boolean,
                        decimalFormat = args[2] as String
                    )
                }
            }
        }
    }

    private fun loadPreferencesSettings() {
        viewModelScope.launch {
            combine(
                userPreferencesRepository.currencySymbol,       // args[0]: String
                userPreferencesRepository.currencyCode,                  // args[1]: String
                userPreferencesRepository.currencyFormat,                // args[2]: String
                userPreferencesRepository.defaultPaymentModeId,          // args[3]: Long
                userPreferencesRepository.defaultCategoryId,             // args[4]: Long
                userPreferencesRepository.firstDayOfMonth,               // args[5]: Int
                userPreferencesRepository.isHapticsEnabled,              // args[6]: Boolean
            ) { args: Array<Any?> ->
                args
            }.collect { args ->
                _uiState.update {
                    it.copy(
                        currencySymbol = args[0] as String,
                        currencyCode = args[1] as String,
                        currencyFormat = args[2] as String,
                        defaultPaymentModeId = args[3] as Long,
                        defaultCategoryId = args[4] as Long,
                        firstDayOfMonth = args[5] as Int,
                        isHapticsEnabled = args[6] as Boolean
                    )
                }
            }
        }

        // Load: Expense Categories
        viewModelScope.launch {
            categoryRepository.getAllCategories(authManager.userId).collect { cats ->
                _uiState.update {
                    it.copy(categories = cats.filter { c ->
                        c.transactionType == TransactionType.EXPENSE || c.transactionType == null
                    })
                }
            }
        }

        // Load: Payment Modes
        viewModelScope.launch {
            paymentModeRepository.getAllModes(authManager.userId).collect { modes ->
                _uiState.update { it.copy(paymentModes = modes) }
            }
        }
    }

    private fun loadNotificationsSettings() {
        viewModelScope.launch {
            combine(
                userPreferencesRepository.isReminderEnabled,   // args[0]: Boolean
                userPreferencesRepository.reminderHour,                 // args[1]: Integer
                userPreferencesRepository.reminderMinute,               // args[2]: Integer
                userPreferencesRepository.isBudgetAlertEnabled,         // args[3]: Boolean
            ) { args: Array<Any?> ->
                args
            }.collect { args ->
                _uiState.update {
                    it.copy(
                        isDailyReminderEnabled       = args[0] as Boolean,
                        dailyReminderHour            = args[1] as Int,
                        dailyReminderMinute          = args[2] as Int,
                        isBudgetAlertEnabled         = args[3] as Boolean,
                    )
                }
            }
        }
    }

    private fun loadBackupRestoreExportSettings() {
        viewModelScope.launch {
            combine(
                userPreferencesRepository.isBackupEnabled,
                userPreferencesRepository.lastBackupTimestamp
            ) { backupOn, lastBackup ->
                val lastBackupStr = if (lastBackup == 0L) "Never"
                else LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(lastBackup),
                    ZoneId.systemDefault()
                )
                    .format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"))
                Pair(backupOn, lastBackupStr)
            }.collect { (backupOn, lastBackupStr) ->
                _uiState.update {
                    it.copy(isBackupEnabled = backupOn, lastBackupDisplay = lastBackupStr)
                }
            }
        }
    }

    fun setCurrency(code: String, symbol: String, format: String) = viewModelScope.launch {
        userPreferencesRepository.setCurrencyCode(code)
        userPreferencesRepository.setCurrencySymbol(symbol)
        userPreferencesRepository.setCurrencyFormat(format)
    }

    fun setThemeMode(mode: String) = viewModelScope.launch {
        userPreferencesRepository.setThemeMode(mode)
    }

    fun setDynamicColor(use: Boolean) = viewModelScope.launch {
        userPreferencesRepository.setUseDynamicColor(use)
    }

    fun setCurrencySymbol(symbol: String) = viewModelScope.launch {
        userPreferencesRepository.setCurrencySymbol(symbol)
    }

    fun setIsBackupEnabled(enabled: Boolean) = viewModelScope.launch {
        userPreferencesRepository.setIsBackupEnabled(enabled)
        if (enabled) driveBackupScheduler.scheduleMonthlyBackup()
        else driveBackupScheduler.cancelBackup()
    }

    fun setIsHapticsEnabled(enabled: Boolean) = viewModelScope.launch {
        userPreferencesRepository.setIsHapticsEnabled(enabled)
    }

    fun setIsDailyReminderEnabled(enabled: Boolean) = viewModelScope.launch {
        userPreferencesRepository.setDailyReminderEnabled(enabled)

        if (enabled) {
            val hour = userPreferencesRepository.reminderHour.first()
            val minute = userPreferencesRepository.reminderMinute.first()

            driveBackupScheduler.scheduleDailyReminder(hour, minute)
        } else {
            driveBackupScheduler.cancelDailyReminder()
        }
    }

    fun setBudgetReminderTime(hour: Int, minute: Int) = viewModelScope.launch {
        userPreferencesRepository.setDailyReminderTime(hour, minute)
        driveBackupScheduler.scheduleDailyReminder(hour, minute)
    }

    fun setIsBudgetAlertEnabled(enabled: Boolean) = viewModelScope.launch {
        userPreferencesRepository.setIsBudgetAlertEnabled(enabled)
    }

    fun triggerBackupNow() = viewModelScope.launch {
        driveBackupScheduler.triggerImmediateBackup()
    }

    fun setDefaultCategoryId(id: Long) = viewModelScope.launch {
        userPreferencesRepository.setDefaultCategoryId(id)
    }

    fun setDefaultPaymentModeId(id: Long) = viewModelScope.launch {
        userPreferencesRepository.setDefaultPaymentModeId(id)
    }

    fun setDecimalFormat(format: String) = viewModelScope.launch {
        userPreferencesRepository.setDecimalFormat(format)
    }

    fun setFirstDayOfMonth(day: Int) = viewModelScope.launch {
        userPreferencesRepository.setFirstDayOfMonth(day)
    }

    fun logout() = viewModelScope.launch {
        authManager.signOut()
    }

    fun importTransactionsFromCsv(uri: Uri) = viewModelScope.launch {
        _uiState.update { it.copy(isImportingTransactions = true, importTransactionsMessage = null) }
        runCatching {
            val userId = authManager.userId
            val categories = categoryRepository.getAllCategories(userId).first()
            val paymentModes = paymentModeRepository.getAllModes(userId).first()
            val creditCards = creditCardRepository.getAllCards(userId).first()
            val tags = tagRepository.getAllTags(userId).first()
            val imported = parseTransactionsFromCsv(uri, userId, categories, paymentModes, creditCards, tags)
            imported.forEach { transactionRepository.insertTransaction(it) }
            imported.size
        }.onSuccess { count ->
            _uiState.update {
                it.copy(
                    isImportingTransactions = false,
                    importTransactionsMessage = if (count == 1) {
                        "Imported 1 transaction"
                    } else {
                        "Imported $count transactions"
                    }
                )
            }
        }.onFailure { error ->
            _uiState.update {
                it.copy(
                    isImportingTransactions = false,
                    importTransactionsMessage = error.message ?: "Failed to import transactions"
                )
            }
        }
    }

    fun clearImportTransactionsMessage() {
        _uiState.update { it.copy(importTransactionsMessage = null) }
    }

    private fun parseTransactionsFromCsv(
        uri: Uri,
        userId: String,
        categories: List<Category>,
        paymentModes: List<PaymentMode>,
        creditCards: List<CreditCard>,
        tags: List<Tag>
    ): List<Transaction> {
        val rows = mutableListOf<Transaction>()
        val formatter = DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("yyyy-MM-dd H:mm")
            .toFormatter(Locale.getDefault())

        val availableTags = tagRepository.getAllTags(userId)

        context.contentResolver.openInputStream(uri)?.use { input ->
            CSVReader(InputStreamReader(input)).use { reader ->
                val header = reader.readNext()?.map { it.trim() }.orEmpty()
                require(header.isNotEmpty()) { "CSV header is missing" }
                val index = header.withIndex().associate { it.value.lowercase() to it.index }

                fun cell(row: Array<String>, name: String): String =
                    index[name.lowercase()]?.let { row.getOrNull(it) }?.trim().orEmpty()

                generateSequence { reader.readNext() }.forEach { row ->
                    if (row.all { it.isBlank() }) return@forEach

                    val type = parseTransactionType(cell(row, "type")) ?: return@forEach
                    val category = resolveCategory(
                        requestedName = cell(row, "Category"),
                        type = type,
                        categories = categories
                    ) ?: return@forEach

                    val amount = cell(row, "Amount").replace(",", "").toDoubleOrNull() ?: return@forEach
                    val dateTime = LocalDateTime.parse(cell(row, "Date"), formatter)

                    val fromMatch = resolvePaymentMatch(cell(row, "Payment mode"), paymentModes, creditCards)
                    val toMatch = resolvePaymentMatch(cell(row, "To payment mode"), paymentModes, creditCards)
                    val tags = cell(row, "Tags")
                        .split(Regex("\\s+"))
                        .map { it.trim().trimStart('#').lowercase() }
                        .filter { it.isNotBlank() }

                    tags.forEach { tag ->

                        Tag(name = tag.trim().lowercase(), userId = userId)

                    }

                    rows += Transaction(
                        type = type,
                        amount = amount,
                        categoryId = category.id,
                        categoryName = category.name,
                        categoryIcon = category.icon,
                        categoryColorHex = category.colorHex,
                        paymentModeId = fromMatch.mode?.id,
                        creditCardId = fromMatch.card?.id,
                        paymentModeName = fromMatch.label,
                        toPaymentModeId = toMatch.mode?.id,
                        toCreditCardId = toMatch.card?.id,
                        toPaymentModeName = toMatch.label,
                        note = cell(row, "Note"),
                        dateTime = dateTime,
                        tags = tags,
                        attachments = emptyList(),
                        userId = userId
                    )
                }
            }
        } ?: error("Unable to open selected CSV file")

        return rows
    }

    private fun parseTransactionType(raw: String): TransactionType? = when (raw.trim().uppercase()) {
        "EXPENSE" -> TransactionType.EXPENSE
        "INCOME" -> TransactionType.INCOME
        "TRANSFER" -> TransactionType.TRANSFER
        else -> null
    }

    private fun resolveCategory(
        requestedName: String,
        type: TransactionType,
        categories: List<Category>
    ): Category? {
        val normalized = requestedName.normalizeLookup()
        return categories.firstOrNull {
            it.name.normalizeLookup() == normalized &&
                    (it.transactionType == type || it.transactionType == null)
        } ?: categories.firstOrNull {
            it.name.normalizeLookup() == "others" &&
                    (it.transactionType == type || it.transactionType == null)
        }
    }

    private data class PaymentImportMatch(
        val mode: PaymentMode? = null,
        val card: com.expensetracker.domain.model.CreditCard? = null,
        val label: String = ""
    )

    private fun resolvePaymentMatch(
        raw: String,
        paymentModes: List<PaymentMode>,
        creditCards: List<com.expensetracker.domain.model.CreditCard>
    ): PaymentImportMatch {
        val normalized = raw.normalizeLookup()
        if (normalized.isBlank()) return PaymentImportMatch()

        paymentModes.firstOrNull { mode ->
            listOf(
                mode.displayLabel,
                mode.bankAccountName,
                mode.identifier,
                mode.type.displayName()
            ).any { it.normalizeLookup() == normalized }
        }?.let { return PaymentImportMatch(mode = it, label = it.displayLabel) }

        creditCards.firstOrNull { card ->
            card.name.normalizeLookup() == normalized || card.displayLabel.normalizeLookup() == normalized
        }?.let { return PaymentImportMatch(card = it, label = it.displayLabel) }

        return PaymentImportMatch(label = raw.trim())
    }

    private fun String.normalizeLookup(): String =
        trim().lowercase().replace(Regex("\\s+"), " ")
}
