package com.expensetracker.presentation.ui.wealth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.data.repository.AuthManager
import com.expensetracker.data.repository.WealthRepository
import com.expensetracker.domain.model.InvestmentRow
import com.expensetracker.domain.model.InvestmentSnapshot
import com.expensetracker.domain.model.InvestmentType
import com.expensetracker.domain.model.NetWorthSummary
import com.expensetracker.domain.model.SavingsRow
import com.expensetracker.domain.model.SavingsSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class WealthUiState(
    val summary: NetWorthSummary = NetWorthSummary(),
    val isLoading: Boolean = true
)

// ── Add / Edit Savings form state ─────────────────────────────────────────────
data class SavingsFormState(
    val institutionName: String = "",
    val originalInstitutionName: String = "",   // tracks pre-edit name for cleanup
    val savingsBalance: String = "0",
    val fdBalance: String = "0",
    val rdBalance: String = "0",
    val isEditMode: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)

// ── Add / Edit Investment form state ─────────────────────────────────────────
data class InvestmentFormState(
    val type: InvestmentType = InvestmentType.INDIAN_MUTUAL_FUND,
    val originalType: InvestmentType = InvestmentType.INDIAN_MUTUAL_FUND, // pre-edit
    val subName: String = "",
    val originalSubName: String = "",  // pre-edit
    val investedAmount: String = "0",
    val currentAmount: String = "0",
    val isEditMode: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class WealthViewModel @Inject constructor(
    private val wealthRepository: WealthRepository,
    private val authManager: AuthManager
) : ViewModel() {

    private val userId get() = authManager.userId

    private val _uiState = MutableStateFlow(WealthUiState())
    val uiState: StateFlow<WealthUiState> = _uiState.asStateFlow()

    private val _savingsForm = MutableStateFlow(SavingsFormState())
    val savingsForm: StateFlow<SavingsFormState> = _savingsForm.asStateFlow()

    private val _investmentForm = MutableStateFlow(InvestmentFormState())
    val investmentForm: StateFlow<InvestmentFormState> = _investmentForm.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            wealthRepository.getNetWorthSummary(userId).collect { summary ->
                _uiState.update { it.copy(summary = summary, isLoading = false) }
            }
        }
    }

    // ── Savings form ──────────────────────────────────────────────────────────

    fun initSavingsForm(existing: SavingsRow? = null) {
        _savingsForm.value = if (existing != null) SavingsFormState(
            institutionName = existing.institutionName,
            originalInstitutionName = existing.institutionName,  // locked for cleanup
            savingsBalance = existing.savingsBalance.toString(),
            fdBalance = existing.fdBalance.toString(),
            rdBalance = existing.rdBalance.toString(),
            isEditMode = true
        ) else SavingsFormState()
    }

    fun setSavingsInstitution(v: String) = _savingsForm.update { it.copy(institutionName = v) }
    fun setSavingsBalance(v: String) = _savingsForm.update { it.copy(savingsBalance = v) }
    fun setFdBalance(v: String) = _savingsForm.update { it.copy(fdBalance = v) }
    fun setRdBalance(v: String) = _savingsForm.update { it.copy(rdBalance = v) }
    fun clearSavingsError() = _savingsForm.update { it.copy(error = null) }

    fun saveSavings() {
        val s = _savingsForm.value
        if (s.institutionName.isBlank()) {
            _savingsForm.update { it.copy(error = "Institution name is required") }; return
        }
        val savings = s.savingsBalance.toDoubleOrNull() ?: 0.0
        val fd = s.fdBalance.toDoubleOrNull() ?: 0.0
        val rd = s.rdBalance.toDoubleOrNull() ?: 0.0

        viewModelScope.launch {
            // In edit mode: if the institution name changed, delete all snapshots
            // under the OLD name so it doesn't appear as a phantom second entry.
            // History is intentionally wiped when the name changes (rename = new entity).
            if (s.isEditMode && s.originalInstitutionName.isNotBlank()
                && s.originalInstitutionName != s.institutionName.trim()
            ) {
                wealthRepository.deleteSavingsInstitution(userId, s.originalInstitutionName)
            }
            wealthRepository.saveSavingsSnapshot(
                SavingsSnapshot(
                    institutionName = s.institutionName.trim(),
                    savingsBalance = savings,
                    fdBalance = fd,
                    rdBalance = rd,
                    recordedOn = LocalDate.now(),
                    userId = userId
                )
            )
            _savingsForm.update { it.copy(isSaved = true) }
        }
    }

    fun deleteSavings(institution: String) {
        viewModelScope.launch {
            wealthRepository.deleteSavingsInstitution(userId, institution)
        }
    }

    // ── Investment form ───────────────────────────────────────────────────────

    fun initInvestmentForm(existing: InvestmentRow? = null) {
        _investmentForm.value = if (existing != null) InvestmentFormState(
            type = existing.type,
            originalType = existing.type,      // locked for cleanup
            subName = existing.subName,
            originalSubName = existing.subName,   // locked for cleanup
            investedAmount = existing.invested.toString(),
            currentAmount = existing.current.toString(),
            isEditMode = true
        ) else InvestmentFormState()
    }

    fun setInvestmentType(v: InvestmentType) =
        _investmentForm.update { it.copy(type = v, subName = "") }

    fun setInvestmentSubName(v: String) = _investmentForm.update { it.copy(subName = v) }
    fun setInvestedAmount(v: String) = _investmentForm.update { it.copy(investedAmount = v) }
    fun setCurrentAmount(v: String) = _investmentForm.update { it.copy(currentAmount = v) }
    fun clearInvestmentError() = _investmentForm.update { it.copy(error = null) }

    fun saveInvestment() {
        val s = _investmentForm.value
        val invested = s.investedAmount.toDoubleOrNull()
        val current = s.currentAmount.toDoubleOrNull()

        val isMutualFund = s.type == InvestmentType.INDIAN_MUTUAL_FUND
                || s.type == InvestmentType.US_MUTUAL_FUND
        if (s.type.requiresBrokerName() && s.subName.isBlank()) {
            _investmentForm.update {
                it.copy(
                    error = if (isMutualFund) "Fund house / platform name is required"
                    else "Broker name is required"
                )
            }; return
        }
        if (s.type.requiresCompanyName() && s.subName.isBlank()) {
            _investmentForm.update { it.copy(error = "Company name is required") }; return
        }
        if (invested == null || invested < 0) {
            _investmentForm.update { it.copy(error = "Enter a valid invested amount") }; return
        }
        if (current == null || current < 0) {
            _investmentForm.update { it.copy(error = "Enter a valid current amount") }; return
        }
        viewModelScope.launch {
            // In edit mode: if type OR subName changed, delete all snapshots for
            // the old (type, subName) key to prevent phantom duplicate entries.
            if (s.isEditMode) {
                val typeChanged = s.originalType != s.type
                val subNameChanged = s.originalSubName != s.subName.trim()
                if (typeChanged || subNameChanged) {
                    wealthRepository.deleteInvestmentPosition(
                        userId = userId,
                        type = s.originalType,
                        subName = s.originalSubName
                    )
                }
            }
            wealthRepository.saveInvestmentSnapshot(
                InvestmentSnapshot(
                    type = s.type,
                    subName = s.subName.trim(),
                    investedAmount = invested,
                    currentAmount = current,
                    recordedOn = LocalDate.now(),
                    userId = userId
                )
            )
            _investmentForm.update { it.copy(isSaved = true) }
        }
    }

    fun deleteInvestment(type: InvestmentType, subName: String) {
        viewModelScope.launch {
            wealthRepository.deleteInvestmentPosition(userId, type, subName)
        }
    }
}