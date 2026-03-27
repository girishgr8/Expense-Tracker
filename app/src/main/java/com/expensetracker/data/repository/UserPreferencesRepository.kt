package com.expensetracker.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
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
        val CURRENCY_CODE = stringPreferencesKey("currency_code")
        val NUMBER_FORMAT = stringPreferencesKey("number_format")   // "millions" | "lakhs" | "none"
        val USE_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
        val THEME_MODE = stringPreferencesKey("theme_mode")   // "light" | "dark" | "system"
        val LAST_BACKUP_TIMESTAMP = longPreferencesKey("last_backup_timestamp")
        val DEFAULT_ACCOUNT_ID = longPreferencesKey("default_account_id")
        val IS_BACKUP_ENABLED = booleanPreferencesKey("is_backup_enabled")
        val IS_HAPTICS_ENABLED = booleanPreferencesKey("is_haptics_enabled")

    }

    val currencySymbol: Flow<String> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.CURRENCY_SYMBOL] ?: "₹" }

    val currencyCode: Flow<String> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.CURRENCY_CODE] ?: "INR" }

    val numberFormat: Flow<String> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[Keys.NUMBER_FORMAT] ?: "millions" }

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

    suspend fun setCurrencySymbol(symbol: String) {
        context.dataStore.edit { it[Keys.CURRENCY_SYMBOL] = symbol }
    }

    suspend fun setCurrencyCode(code: String) {
        context.dataStore.edit { it[Keys.CURRENCY_CODE] = code }
    }

    suspend fun setNumberFormat(format: String) {
        context.dataStore.edit { it[Keys.NUMBER_FORMAT] = format }
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
}