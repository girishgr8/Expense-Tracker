package com.expensetracker.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferencesRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private object Keys {
        val CURRENCY_SYMBOL = stringPreferencesKey("currency_symbol")
        val USER_DISPLAY_NAME = stringPreferencesKey("user_display_name")
        val CURRENCY_CODE = stringPreferencesKey("currency_code")
        val CURRENCY_FORMAT = stringPreferencesKey("currency_format")   // "millions" | "lakhs" | "none"
        val USE_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
        val THEME_MODE = stringPreferencesKey("theme_mode")   // "light" | "dark" | "system"
        val LAST_BACKUP_TIMESTAMP = longPreferencesKey("last_backup_timestamp")
        val DEFAULT_ACCOUNT_ID = longPreferencesKey("default_account_id")
        val IS_BACKUP_ENABLED = booleanPreferencesKey("is_backup_enabled")
        val IS_HAPTICS_ENABLED = booleanPreferencesKey("is_haptics_enabled")
        val DAILY_REMINDER_HOUR = intPreferencesKey("daily_reminder_hour")
        val DAILY_REMINDER_MINUTE = intPreferencesKey("daily_reminder_minute")
        val IS_DAILY_REMINDER_ENABLED = booleanPreferencesKey("is_daily_reminder_enabled")
        val IS_BUDGET_ALERT_ENABLED = booleanPreferencesKey("is_budget_alert_enabled")
        val DEFAULT_CATEGORY_ID     = longPreferencesKey("default_category_id")
        val DEFAULT_PAYMENT_MODE_ID = longPreferencesKey("default_payment_mode_id")
        val FIRST_DAY_OF_MONTH      = intPreferencesKey("first_day_of_month")
        val DECIMAL_FORMAT          = stringPreferencesKey("decimal_format")  // "default"|"none"|"one"|"two"
    }

    val currencySymbol: Flow<String> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.CURRENCY_SYMBOL] ?: "₹" }

    val userDisplayName: Flow<String> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.USER_DISPLAY_NAME] ?: "" }

    val currencyCode: Flow<String> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.CURRENCY_CODE] ?: "INR" }

    val currencyFormat: Flow<String> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.CURRENCY_FORMAT] ?: "millions" }

    val useDynamicColor: Flow<Boolean> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.USE_DYNAMIC_COLOR] ?: true }

    val themeMode: Flow<String> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.THEME_MODE] ?: "system" }

    val lastBackupTimestamp: Flow<Long> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.LAST_BACKUP_TIMESTAMP] ?: 0L }

    val isBackupEnabled: Flow<Boolean> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.IS_BACKUP_ENABLED] ?: true }

    val isHapticsEnabled: Flow<Boolean> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.IS_HAPTICS_ENABLED] ?: true }

    val reminderHour: Flow<Int> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.DAILY_REMINDER_HOUR] ?: 19 } // default 7 PM

    val reminderMinute: Flow<Int> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.DAILY_REMINDER_MINUTE] ?: 0 }

    val isReminderEnabled: Flow<Boolean> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.IS_DAILY_REMINDER_ENABLED] ?: true }

    val isBudgetAlertEnabled: Flow<Boolean> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.IS_BUDGET_ALERT_ENABLED] ?: true }

    val defaultCategoryId: Flow<Long> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.DEFAULT_CATEGORY_ID] ?: -1L }

    val defaultPaymentModeId: Flow<Long> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.DEFAULT_PAYMENT_MODE_ID] ?: -1L }

    val decimalFormat: Flow<String> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.DECIMAL_FORMAT] ?: "default" }

    val firstDayOfMonth: Flow<Int> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.FIRST_DAY_OF_MONTH] ?: 1 }

    suspend fun setCurrencySymbol(symbol: String) {
        context.dataStore.edit { it[Keys.CURRENCY_SYMBOL] = symbol }
    }

    suspend fun setUserDisplayName(name: String) {
        context.dataStore.edit { it[Keys.USER_DISPLAY_NAME] = name }
    }

    suspend fun setCurrencyCode(code: String) {
        context.dataStore.edit { it[Keys.CURRENCY_CODE] = code }
    }

    suspend fun setCurrencyFormat(format: String) {
        context.dataStore.edit { it[Keys.CURRENCY_FORMAT] = format }
    }

    suspend fun setUseDynamicColor(use: Boolean) {
        context.dataStore.edit { it[Keys.USE_DYNAMIC_COLOR] = use }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode }
    }

    suspend fun setLastBackupTimestamp(ts: Long) {
        context.dataStore.edit { it[Keys.LAST_BACKUP_TIMESTAMP] = ts }
    }

    suspend fun setIsBackupEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.IS_BACKUP_ENABLED] = enabled }
    }

    suspend fun setIsHapticsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.IS_HAPTICS_ENABLED] = enabled }
    }

    suspend fun setDailyReminderTime(hour: Int, minute: Int) {
        context.dataStore.edit {
            it[Keys.DAILY_REMINDER_HOUR] = hour
            it[Keys.DAILY_REMINDER_MINUTE] = minute
        }
    }

    suspend fun setDailyReminderEnabled(enabled: Boolean) {
        context.dataStore.edit {
            it[Keys.IS_DAILY_REMINDER_ENABLED] = enabled
        }
    }

    suspend fun setIsBudgetAlertEnabled(enabled: Boolean) {
        context.dataStore.edit {
            it[Keys.IS_BUDGET_ALERT_ENABLED] = enabled
        }
    }

    suspend fun setDefaultCategoryId(id: Long) {
        context.dataStore.edit { it[Keys.DEFAULT_CATEGORY_ID] = id }
    }

    suspend fun setDefaultPaymentModeId(id: Long) {
        context.dataStore.edit { it[Keys.DEFAULT_PAYMENT_MODE_ID] = id }
    }

    suspend fun setDecimalFormat(format: String) {
        context.dataStore.edit { it[Keys.DECIMAL_FORMAT] = format }
    }

    suspend fun setFirstDayOfMonth(day: Int) {
        context.dataStore.edit { it[Keys.FIRST_DAY_OF_MONTH] = day }
    }
}
