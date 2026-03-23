package com.expensetracker.presentation.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.data.repository.AuthManager
import com.expensetracker.data.repository.UserPreferencesRepository
import com.expensetracker.util.DriveBackupScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
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
    val backupEnabled: Boolean = true,
    val lastBackupDisplay: String = "Never",
    val userName: String = "",
    val userEmail: String = "",
    val userPhotoUrl: String = ""
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
                userPreferencesRepository.themeMode,
                userPreferencesRepository.useDynamicColor,
                userPreferencesRepository.currencySymbol,
                userPreferencesRepository.currencyCode,
                userPreferencesRepository.numberFormat
            ) { theme, dynamic, symbol, code, format ->
                listOf(theme, dynamic.toString(), symbol, code, format)
            }.collect { vals ->
                _uiState.update {
                    it.copy(
                        themeMode       = vals[0],
                        useDynamicColor = vals[1].toBoolean(),
                        currencySymbol  = vals[2],
                        currencyCode    = vals[3],
                        numberFormat    = vals[4]
                    )
                }
            }
        }
        viewModelScope.launch {
            combine(
                userPreferencesRepository.backupEnabled,
                userPreferencesRepository.lastBackupTimestamp
            ) { backupOn, lastBackup ->
                val lastBackupStr = if (lastBackup == 0L) "Never"
                else LocalDateTime.ofInstant(Instant.ofEpochMilli(lastBackup), ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"))
                Pair(backupOn, lastBackupStr)
            }.collect { (backupOn, lastBackupStr) ->
                _uiState.update {
                    it.copy(backupEnabled = backupOn, lastBackupDisplay = lastBackupStr)
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

    fun setBackupEnabled(enabled: Boolean) = viewModelScope.launch {
        userPreferencesRepository.setBackupEnabled(enabled)
        if (enabled) driveBackupScheduler.scheduleMonthlyBackup()
        else driveBackupScheduler.cancelBackup()
    }

    fun triggerBackupNow() {
        driveBackupScheduler.triggerImmediateBackup()
    }

    fun logout() = viewModelScope.launch {
        authManager.signOut()
    }
}