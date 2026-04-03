package com.expensetracker.presentation.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.data.repository.AuthManager
import com.expensetracker.data.repository.CategoryRepository
import com.expensetracker.data.repository.PaymentModeRepository
import com.expensetracker.data.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authManager: AuthManager,
    private val categoryRepository: CategoryRepository,
    private val paymentModeRepository: PaymentModeRepository,
    userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val isLoggedIn: StateFlow<Boolean> = authManager.currentUser
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, authManager.isLoggedIn())

    /** "system" | "light" | "dark" */
    val themeMode: StateFlow<String> = userPreferencesRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, "system")

    val useDynamicColor: StateFlow<Boolean> = userPreferencesRepository.useDynamicColor
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val currencySymbol: StateFlow<String> = userPreferencesRepository.currencySymbol
        .stateIn(viewModelScope, SharingStarted.Eagerly, "₹")

    val currencyCode: StateFlow<String> = userPreferencesRepository.currencyCode
        .stateIn(viewModelScope, SharingStarted.Eagerly, "INR")

    val currencyFormat: StateFlow<String> = userPreferencesRepository.currencyFormat
        .stateIn(viewModelScope, SharingStarted.Eagerly, "millions")

    val decimalFormat: StateFlow<String> = userPreferencesRepository.decimalFormat
        .stateIn(viewModelScope, SharingStarted.Eagerly, "default")

    val isBackupEnabled: StateFlow<Boolean> = userPreferencesRepository.isBackupEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val isHapticsEnabled: StateFlow<Boolean> = userPreferencesRepository.isHapticsEnabled
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    init {
        viewModelScope.launch {
            val userId = authManager.userId
            categoryRepository.seedDefaultCategories(userId)
            paymentModeRepository.seedDefaultModes(userId)
        }
    }
}