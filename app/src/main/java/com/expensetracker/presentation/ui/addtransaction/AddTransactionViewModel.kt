package com.expensetracker.presentation.ui.addtransaction

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.data.repository.AuthManager
import com.expensetracker.data.repository.CategoryRepository
import com.expensetracker.data.repository.CreditCardRepository
import com.expensetracker.data.repository.PaymentModeRepository
import com.expensetracker.data.repository.TagRepository
import com.expensetracker.data.repository.TransactionRepository
import com.expensetracker.data.repository.UserPreferencesRepository
import com.expensetracker.domain.model.Attachment
import com.expensetracker.domain.model.Category
import com.expensetracker.domain.model.PaymentOption
import com.expensetracker.domain.model.Tag
import com.expensetracker.domain.model.Transaction
import com.expensetracker.domain.model.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDateTime
import javax.inject.Inject

data class AddTransactionUiState(
    val transactionType: TransactionType = TransactionType.EXPENSE,
    val amount: String = "",
    val selectedCategory: Category? = null,
    val selectedPaymentOption: PaymentOption? = null,       // Mode or CreditCard
    val selectedToPaymentOption: PaymentOption? = null,     // For transfers
    val note: String = "",
    val dateTime: LocalDateTime = LocalDateTime.now(),
    val tags: List<String> = emptyList(),
    val attachments: List<Attachment> = emptyList(),
    val categories: List<Category> = emptyList(),
    val paymentOptions: List<PaymentOption> = emptyList(),  // All modes + all cards
    val tagSuggestions: List<Tag> = emptyList(),
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null,
    val isEditMode: Boolean = false
)

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val paymentModeRepository: PaymentModeRepository,
    private val creditCardRepository: CreditCardRepository,
    private val tagRepository: TagRepository,
    private val authManager: AuthManager,
    private val userPreferencesRepository: UserPreferencesRepository,
    @param:ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val transactionId: Long = savedStateHandle["transactionId"] ?: -1L
    private val _uiState = MutableStateFlow(AddTransactionUiState())
    val uiState: StateFlow<AddTransactionUiState> = _uiState.asStateFlow()

    private val userId get() = authManager.userId

    init {
        loadInitialData()
        if (transactionId > 0) loadTransaction(transactionId)
        else applyDefaults()
    }

    private fun applyDefaults() {
        viewModelScope.launch {
            val defaultCatId = userPreferencesRepository.defaultCategoryId.first()
            val defaultModeId = userPreferencesRepository.defaultPaymentModeId.first()
            // Wait for categories/modes to be loaded, then apply
            if (defaultCatId > 0 || defaultModeId > 0) {
                // Collect once to get the first emission
                val cats = _uiState.value.categories
                val modes = _uiState.value.paymentOptions
                val cat = cats.find { it.id == defaultCatId }
                val mode = modes.find { it.id == defaultModeId }
                _uiState.update {
                    it.copy(
                        selectedCategory = cat ?: it.selectedCategory,
                        selectedPaymentOption = mode ?: it.selectedPaymentOption
                    )
                }
                // If not loaded yet, wait for the next emission
                if ((defaultCatId > 0 && cat == null) || (defaultModeId > 0 && mode == null)) {
                    kotlinx.coroutines.delay(300)
                    val cats2 = _uiState.value.categories
                    val modes2 = _uiState.value.paymentOptions
                    _uiState.update {
                        it.copy(
                            selectedCategory = if (defaultCatId > 0) cats2.find { c -> c.id == defaultCatId }
                                ?: it.selectedCategory else it.selectedCategory,
                            selectedPaymentOption = if (defaultModeId > 0) modes2.find { m -> m.id == defaultModeId }
                                ?: it.selectedPaymentOption else it.selectedPaymentOption
                        )
                    }
                }
            }
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            categoryRepository.getAllCategories(userId).collect { cats ->
                _uiState.update { it.copy(categories = cats) }
            }
        }
        // Combine PaymentModes and CreditCards into a unified PaymentOption list
        viewModelScope.launch {
            combine(
                paymentModeRepository.getAllModes(userId),
                creditCardRepository.getAllCards(userId)
            ) { modes, cards ->
                val modeOptions = modes.map { PaymentOption.Mode(it) }
                val cardOptions = cards.map { PaymentOption.Card(it) }
                modeOptions + cardOptions
            }.collect { options ->
                _uiState.update { it.copy(paymentOptions = options) }
            }
        }
    }

    private fun loadTransaction(id: Long) {
        viewModelScope.launch {
            transactionRepository.getTransactionById(id)?.let { txn ->
                _uiState.update { state ->
                    // Resolve the "from" PaymentOption
                    val fromOption = when {
                        txn.paymentModeId != null ->
                            state.paymentOptions.filterIsInstance<PaymentOption.Mode>()
                                .find { it.mode.id == txn.paymentModeId }

                        txn.creditCardId != null ->
                            state.paymentOptions.filterIsInstance<PaymentOption.Card>()
                                .find { it.card.id == txn.creditCardId }

                        else -> null
                    }
                    // Resolve the "to" PaymentOption for transfers
                    val toOption = when {
                        txn.toPaymentModeId != null ->
                            state.paymentOptions.filterIsInstance<PaymentOption.Mode>()
                                .find { it.mode.id == txn.toPaymentModeId }

                        txn.toCreditCardId != null ->
                            state.paymentOptions.filterIsInstance<PaymentOption.Card>()
                                .find { it.card.id == txn.toCreditCardId }

                        else -> null
                    }
                    state.copy(
                        isEditMode = true,
                        transactionType = txn.type,
                        amount = txn.amount.toString(),
                        selectedCategory = state.categories.find { it.id == txn.categoryId },
                        selectedPaymentOption = fromOption,
                        selectedToPaymentOption = toOption,
                        note = txn.note,
                        dateTime = txn.dateTime,
                        tags = txn.tags,
                        attachments = txn.attachments
                    )
                }
            }
        }
    }

    fun setTransactionType(type: TransactionType) {
        _uiState.update { it.copy(transactionType = type, selectedCategory = null) }
    }

    fun setAmount(value: String) = _uiState.update { it.copy(amount = value) }
    fun setCategory(category: Category) = _uiState.update { it.copy(selectedCategory = category) }
    fun setPaymentOption(option: PaymentOption) =
        _uiState.update { it.copy(selectedPaymentOption = option) }

    fun setToPaymentOption(option: PaymentOption) =
        _uiState.update { it.copy(selectedToPaymentOption = option) }

    fun setNote(note: String) = _uiState.update { it.copy(note = note) }
    fun setDateTime(dt: LocalDateTime) = _uiState.update { it.copy(dateTime = dt) }

    fun addTag(tag: String) {
        val current = _uiState.value.tags
        if (current.size < 5 && tag.isNotBlank() && !current.contains(tag)) {
            _uiState.update { it.copy(tags = current + tag.trim().lowercase()) }
            viewModelScope.launch {
                tagRepository.insertTag(Tag(name = tag.trim().lowercase(), userId = userId))
                tagRepository.getAllTags(userId).collect { tag -> Log.d("Expense Tracker", "tag=$tag") }
            }
        }
    }

    fun removeTag(tag: String) = _uiState.update { it.copy(tags = it.tags - tag) }

    fun searchTags(query: String) {
        viewModelScope.launch {
            val suggestions = tagRepository.searchTags(userId, query)
            _uiState.update { it.copy(tagSuggestions = suggestions) }
        }
    }

    fun addAttachment(uri: Uri) {
        viewModelScope.launch {
            try {
                val att = saveAttachmentFromUri(uri)
                // Replace any existing attachment — only one allowed
                _uiState.update { it.copy(attachments = listOf(att)) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to add attachment") }
            }
        }
    }

    private fun saveAttachmentFromUri(uri: Uri): Attachment {
        val cr = context.contentResolver
        val mimeType = cr.getType(uri) ?: "application/octet-stream"

        // Only PDF and common image formats allowed
        val allowed = setOf("application/pdf", "image/jpeg", "image/jpg", "image/png")
        if (mimeType !in allowed) {
            throw IllegalArgumentException(
                "Only PDF, JPEG and PNG files are supported. Got: $mimeType"
            )
        }

        // Derive a clean file name from the content URI
        val rawName = uri.lastPathSegment ?: "attachment"
        val fileName = rawName.substringAfterLast('/').substringAfterLast(':')
            .ifBlank { "attachment_${System.currentTimeMillis()}" }

        val dir = File(context.filesDir, "attachments").also { it.mkdirs() }
        val dest = File(dir, "${System.currentTimeMillis()}_$fileName")
        cr.openInputStream(uri)?.use { it.copyTo(dest.outputStream()) }

        return Attachment(
            transactionId = 0,
            fileName = fileName,
            filePath = dest.absolutePath,
            mimeType = mimeType,
            fileSizeBytes = dest.length()
        )
    }

    fun removeAttachment(attachment: Attachment) =
        _uiState.update { it.copy(attachments = it.attachments - attachment) }

    fun saveTransaction() {
        val state = _uiState.value
        val amount = state.amount.toDoubleOrNull()

        if (amount == null || amount <= 0) {
            _uiState.update { it.copy(error = "Please enter a valid amount") }
            return
        }
        if (state.selectedCategory == null) {
            _uiState.update { it.copy(error = "Please select a category") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                // Resolve from-payment fields from the selected PaymentOption
                val fromModeId = (state.selectedPaymentOption as? PaymentOption.Mode)?.mode?.id
                val fromCardId = (state.selectedPaymentOption as? PaymentOption.Card)?.card?.id
                val fromLabel = state.selectedPaymentOption?.displayLabel ?: ""

                // Resolve to-payment fields for transfers
                val toModeId = (state.selectedToPaymentOption as? PaymentOption.Mode)?.mode?.id
                val toCardId = (state.selectedToPaymentOption as? PaymentOption.Card)?.card?.id
                val toLabel = state.selectedToPaymentOption?.displayLabel ?: ""

                val transaction = Transaction(
                    id = if (state.isEditMode) transactionId else 0,
                    type = state.transactionType,
                    amount = amount,
                    categoryId = state.selectedCategory.id,
                    categoryName = state.selectedCategory.name,
                    categoryIcon = state.selectedCategory.icon,
                    categoryColorHex = state.selectedCategory.colorHex,
                    paymentModeId = fromModeId,
                    creditCardId = fromCardId,
                    paymentModeName = fromLabel,
                    toPaymentModeId = toModeId,
                    toCreditCardId = toCardId,
                    toPaymentModeName = toLabel,
                    note = state.note,
                    dateTime = state.dateTime,
                    tags = state.tags,
                    attachments = state.attachments,
                    userId = userId
                )
                if (state.isEditMode) transactionRepository.updateTransaction(transaction)
                else transactionRepository.insertTransaction(transaction)
                _uiState.update { it.copy(isSaved = true, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}