package com.expensetracker.presentation.ui.addtransaction

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.data.repository.AuthManager
import com.expensetracker.data.repository.CategoryRepository
import com.expensetracker.data.repository.CreditCardRepository
import com.expensetracker.data.repository.PaymentModeRepository
import com.expensetracker.data.repository.ScheduledTransactionRepository
import com.expensetracker.data.repository.TagRepository
import com.expensetracker.domain.model.Attachment
import com.expensetracker.domain.model.Category
import com.expensetracker.domain.model.PaymentOption
import com.expensetracker.domain.model.ScheduledFrequency
import com.expensetracker.domain.model.ScheduledTransaction
import com.expensetracker.domain.model.Tag
import com.expensetracker.domain.model.TransactionType
import com.expensetracker.util.ScheduledTransactionScheduler
import com.expensetracker.worker.ScheduledTransactionWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDateTime
import javax.inject.Inject

data class AddScheduledTransactionUiState(
    val transactionType: TransactionType = TransactionType.EXPENSE,
    val amount: String = "",
    val selectedCategory: Category? = null,
    val selectedPaymentOption: PaymentOption? = null,
    val selectedToPaymentOption: PaymentOption? = null,
    val note: String = "",
    val dateTime: LocalDateTime = LocalDateTime.now(),
    val frequency: ScheduledFrequency = ScheduledFrequency.NONE,
    val categories: List<Category> = emptyList(),
    val paymentOptions: List<PaymentOption> = emptyList(),
    val tags: List<String> = emptyList(),
    val tagSuggestions: List<Tag> = emptyList(),
    val attachments: List<Attachment> = emptyList(),
    val reminderMinutes: Long = 0,
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AddScheduledTransactionViewModel @Inject constructor(
    private val scheduledTransactionRepository: ScheduledTransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val paymentModeRepository: PaymentModeRepository,
    private val creditCardRepository: CreditCardRepository,
    private val tagRepository: TagRepository,
    private val authManager: AuthManager,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddScheduledTransactionUiState())
    val uiState: StateFlow<AddScheduledTransactionUiState> = _uiState.asStateFlow()

    private val userId get() = authManager.userId

    init {
        viewModelScope.launch {
            categoryRepository.getAllCategories(userId).collect { cats ->
                _uiState.update { it.copy(categories = cats) }
            }
        }
        viewModelScope.launch {
            combine(
                paymentModeRepository.getAllModes(userId),
                creditCardRepository.getAllCards(userId)
            ) { modes, cards ->
                modes.map { PaymentOption.Mode(it) } + cards.map { PaymentOption.Card(it) }
            }.collect { options ->
                _uiState.update { it.copy(paymentOptions = options) }
            }
        }
    }

    fun setTransactionType(type: TransactionType) {
        _uiState.update {
            it.copy(
                transactionType = type,
                selectedCategory = null,
                selectedToPaymentOption = if (type == TransactionType.TRANSFER) it.selectedToPaymentOption else null
            )
        }
    }

    fun setAmount(value: String) = _uiState.update { it.copy(amount = value) }
    fun setCategory(category: Category) = _uiState.update { it.copy(selectedCategory = category) }
    fun setPaymentOption(option: PaymentOption) =
        _uiState.update { it.copy(selectedPaymentOption = option) }

    fun setToPaymentOption(option: PaymentOption) =
        _uiState.update { it.copy(selectedToPaymentOption = option) }

    fun setNote(note: String) = _uiState.update { it.copy(note = note) }
    fun setDateTime(dt: LocalDateTime) = _uiState.update { it.copy(dateTime = dt) }
    fun setFrequency(frequency: ScheduledFrequency) =
        _uiState.update { it.copy(frequency = frequency) }

    fun clearError() = _uiState.update { it.copy(error = null) }

    fun addTag(tag: String) {
        val current = _uiState.value.tags
        if (current.size < 5 && tag.isNotBlank() && !current.contains(tag)) {
            _uiState.update { it.copy(tags = current + tag.trim().lowercase()) }
            viewModelScope.launch {
                tagRepository.insertTag(Tag(name = tag.trim().lowercase(), userId = userId))
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

    fun setReminderMinutes(days: Long) {
        _uiState.update { it.copy(reminderMinutes = days) }
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

    fun saveSchedule() {
        val state = _uiState.value
        val amount = state.amount.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            _uiState.update { it.copy(error = "Please enter a valid amount") }
            return
        }
        val category = state.selectedCategory
        if (category == null) {
            _uiState.update { it.copy(error = "Please select a category") }
            return
        }
        if (state.dateTime.isBefore(LocalDateTime.now())) {
            _uiState.update { it.copy(error = "Please choose a future date and time") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val fromModeId = (state.selectedPaymentOption as? PaymentOption.Mode)?.mode?.id
                val fromCardId = (state.selectedPaymentOption as? PaymentOption.Card)?.card?.id
                val toModeId = (state.selectedToPaymentOption as? PaymentOption.Mode)?.mode?.id
                val toCardId = (state.selectedToPaymentOption as? PaymentOption.Card)?.card?.id

                val schedule = ScheduledTransaction(
                    type = state.transactionType,
                    amount = amount,
                    categoryId = category.id,
                    paymentModeId = fromModeId,
                    creditCardId = fromCardId,
                    toPaymentModeId = toModeId,
                    toCreditCardId = toCardId,
                    note = state.note,
                    dateTime = state.dateTime,
                    frequency = state.frequency,
                    nextRunAt = state.dateTime,
                    reminderMinutes = uiState.value.reminderMinutes,
                    userId = userId
                )
                val savedId = scheduledTransactionRepository.insertScheduledTransaction(schedule)
                val savedSchedule = schedule.copy(id = savedId)
                ScheduledTransactionWorker.scheduleReminder(context, savedSchedule)
                ScheduledTransactionScheduler.schedule(context, savedId, state.dateTime)
                _uiState.update { it.copy(isSaved = true, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = e.message ?: "Failed to save schedule",
                        isLoading = false
                    )
                }
            }
        }
    }
}
