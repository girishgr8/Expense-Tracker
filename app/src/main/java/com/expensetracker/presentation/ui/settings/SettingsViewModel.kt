package com.expensetracker.presentation.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.data.repository.AuthManager
import com.expensetracker.data.repository.UserPreferencesRepository
import com.expensetracker.util.DriveBackupScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class SettingsUiState(
    val themeMode: String = "system",
    val useDynamicColor: Boolean = true,
    val currencySymbol: String = "₹",
    val currencyCode: String = "INR",
    val numberFormat: String = "millions",
    val isHapticsEnabled: Boolean = true,
    val isBackupEnabled: Boolean = true,
    val lastBackupDisplay: String = "Never",
    val userName: String = "",
    val userEmail: String = "",
    val userPhotoUrl: String = "",
    val isDailyReminderEnabled: Boolean = true,
    val dailyReminderHour: Int = 19,
    val dailyReminderMinute: Int = 0,
    val isBudgetAlertEnabled: Boolean = true
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val authManager: AuthManager,
    private val driveBackupScheduler: DriveBackupScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
        loadUserInfo()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            combine(
                userPreferencesRepository.themeMode, // args[0]: String
                userPreferencesRepository.useDynamicColor,    // args[1]: Boolean
                userPreferencesRepository.currencySymbol,     // args[2]: String
                userPreferencesRepository.currencyCode,       // args[3]: String
                userPreferencesRepository.numberFormat,       // args[4]: String
                userPreferencesRepository.isHapticsEnabled,   // args[5]: Boolean
            ) { args: Array<Any?> ->
                args
            }.collect { args ->
                _uiState.update {
                    it.copy(
                        themeMode = args[0] as String,
                        useDynamicColor = args[1] as Boolean,
                        currencySymbol = args[2] as String,
                        currencyCode = args[3] as String,
                        numberFormat = args[4] as String,
                        isHapticsEnabled = args[5] as Boolean
                    )
                }
            }
        }

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

    fun setCurrency(code: String, symbol: String, format: String) = viewModelScope.launch {
        userPreferencesRepository.setCurrencyCode(code)
        userPreferencesRepository.setCurrencySymbol(symbol)
        userPreferencesRepository.setNumberFormat(format)
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

    fun triggerBackupNow() {
        driveBackupScheduler.triggerImmediateBackup()
    }

    fun logout() = viewModelScope.launch {
        authManager.signOut()
    }
}