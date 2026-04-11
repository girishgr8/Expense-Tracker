package com.expensetracker.presentation.ui.addtransaction

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.hilt.navigation.compose.hiltViewModel
import com.expensetracker.domain.model.ScheduledFrequency
import com.expensetracker.domain.model.TransactionType
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val ScheduledAccentOrange = Color(0xFFFF9F1C)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddScheduledTransactionScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToAccounts: () -> Unit,
    viewModel: AddScheduledTransactionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var paymentSheetTarget by remember { mutableStateOf<PaymentSheetTarget?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showFrequencySheet by remember { mutableStateOf(false) }
    var showCategorySheet by remember { mutableStateOf(false) }
    var showCalculator by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Accepts only PDF and images
    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { viewModel.addAttachment(it) } }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onNavigateBack()
    }

    val accentColor = remember(uiState.selectedCategory) {
        uiState.selectedCategory?.let {
            runCatching { Color(it.colorHex.toColorInt()) }.getOrDefault(ScheduledAccentOrange)
        } ?: ScheduledAccentOrange
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text("Add scheduled txn", fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = viewModel::saveSchedule, enabled = !uiState.isLoading) {
                        Text("Save", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Transaction Type Tabs
            TransactionTypeTabs(
                selected = uiState.transactionType,
                onSelect = viewModel::setTransactionType
            )

            // Transaction Date Time Row
            TransactionDateTimeRow(
                dateTime = uiState.dateTime,
                accentColor = accentColor,
                onDateClick = { showDatePicker = true },
                onTimeClick = { showTimePicker = true }
            )

            ScheduleFrequencyRow(
                value = uiState.frequency.displayName(),
                accentColor = accentColor,
                supporting = frequencySupportingText(uiState.frequency, uiState.dateTime),
                onClick = { showFrequencySheet = true }
            )

            // Transaction Amount Entry Row
            AmountEntryRow(
                amount = uiState.amount,
                accentColor = accentColor,
                onAmountChange = viewModel::setAmount,
                onOpenCalculator = { showCalculator = true }
            )

            // Transaction Category Selection Row
            CategorySelectionRow(
                category = uiState.selectedCategory,
                accentColor = accentColor,
                onClick = { showCategorySheet = true }
            )

            PaymentSelectorField(
                label = if (uiState.transactionType == TransactionType.TRANSFER) "From Account" else "Payment mode",
                value = uiState.selectedPaymentOption?.let(::paymentOptionHeadline).orEmpty(),
                supporting = uiState.selectedPaymentOption?.let(::paymentOptionSupportingText),
                icon = paymentOptionIcon(uiState.selectedPaymentOption),
                accentColor = accentColor,
                placeholder = "Select payment mode",
                onClick = { paymentSheetTarget = PaymentSheetTarget.FROM }
            )

            // To Account (Transfer only)
            if (uiState.transactionType == TransactionType.TRANSFER) {
                PaymentSelectorField(
                    label = "To Account",
                    value = uiState.selectedToPaymentOption?.let(::paymentOptionHeadline).orEmpty(),
                    supporting = uiState.selectedToPaymentOption?.let(::paymentOptionSupportingText),
                    icon = paymentOptionIcon(uiState.selectedToPaymentOption),
                    accentColor = accentColor,
                    placeholder = "Select destination account",
                    onClick = { paymentSheetTarget = PaymentSheetTarget.TO }
                )
            }

            // ── Other Details ─────────────────────────────────────────────────
            Spacer(Modifier.height(4.dp))
            Text(
                "Other details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            SeamlessNoteField(
                note = uiState.note,
                accentColor = accentColor,
                onNoteChange = viewModel::setNote
            )

            SeamlessTagsSection(
                tags = uiState.tags,
                suggestions = uiState.tagSuggestions,
                accentColor = accentColor,
                onAddTag = viewModel::addTag,
                onRemoveTag = viewModel::removeTag,
                onSearchTag = viewModel::searchTags
            )

            SeamlessAttachmentRow(
                attachment = uiState.attachments.firstOrNull(),
                accentColor = accentColor,
                onPickAttachment = {
                    // MIME filter: only PDF + images
                    fileLauncher.launch("*/*")
                },
                onRemoveAttachment = {
                    uiState.attachments.firstOrNull()?.let { viewModel.removeAttachment(it) }
                }
            )

            Spacer(Modifier.size(12.dp))
        }
    }

    if (showDatePicker) {
        TransactionDatePickerDialog(
            dateTime = uiState.dateTime,
            onConfirm = {
                viewModel.setDateTime(
                    uiState.dateTime
                        .withYear(it.year)
                        .withMonth(it.monthValue)
                        .withDayOfMonth(it.dayOfMonth)
                )
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }

    if (showTimePicker) {
        TransactionTimePickerDialog(
            dateTime = uiState.dateTime,
            onConfirm = { hour, minute ->
                viewModel.setDateTime(uiState.dateTime.withHour(hour).withMinute(minute))
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }

    if (showFrequencySheet) {
        FrequencyPickerSheet(
            selected = uiState.frequency,
            onSelect = {
                viewModel.setFrequency(it)
                showFrequencySheet = false
            },
            onDismiss = { showFrequencySheet = false }
        )
    }

    if (showCalculator) {
        AmountCalculatorSheet(
            initialAmount = uiState.amount,
            onDone = {
                viewModel.setAmount(it)
                showCalculator = false
            },
            onDismiss = { showCalculator = false }
        )
    }

    if (showCategorySheet) {
        CategoryPickerSheet(
            categories = uiState.categories.filter {
                it.transactionType == uiState.transactionType || it.transactionType == null
            },
            selected = uiState.selectedCategory,
            onSelect = {
                viewModel.setCategory(it)
                showCategorySheet = false
            },
            onManageCategories = {
                showCategorySheet = false
                onNavigateToCategories()
            },
            onDismiss = { showCategorySheet = false }
        )
    }

    val sheetOptions = when (paymentSheetTarget) {
        PaymentSheetTarget.FROM -> uiState.paymentOptions
        PaymentSheetTarget.TO -> uiState.paymentOptions.filterNot {
            samePaymentOption(it, uiState.selectedPaymentOption)
        }

        null -> emptyList()
    }

    val selectedSheetOption = when (paymentSheetTarget) {
        PaymentSheetTarget.FROM -> uiState.selectedPaymentOption
        PaymentSheetTarget.TO -> uiState.selectedToPaymentOption
        null -> null
    }

    if (paymentSheetTarget != null) {
        PaymentOptionSheet(
            title = when (paymentSheetTarget) {
                PaymentSheetTarget.FROM -> if (uiState.transactionType == TransactionType.TRANSFER) {
                    "Select From Account"
                } else {
                    "Select Payment Mode"
                }

                PaymentSheetTarget.TO -> "Select Destination Account"
                else -> {
                    ""
                }
            },
            options = sheetOptions,
            selected = selectedSheetOption,
            onSelect = { option ->
                when (paymentSheetTarget) {
                    PaymentSheetTarget.FROM -> viewModel.setPaymentOption(option)
                    PaymentSheetTarget.TO -> viewModel.setToPaymentOption(option)
                    null -> Unit
                }
                paymentSheetTarget = null
            },
            onDismiss = { paymentSheetTarget = null }
        )
    }
}

@Composable
private fun ScheduleFrequencyRow(
    value: String,
    accentColor: Color,
    supporting: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Repeat, null, tint = accentColor, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Frequency",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                color = if (value.startsWith("Select")) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface
            )
            supporting.takeIf { it.isNotBlank() }?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SelectionRow(
    label: String,
    value: String,
    accentColor: Color,
    leading: @Composable () -> Unit,
    onClick: () -> Unit,
    supporting: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
            leading()
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            if (label.isNotEmpty()) {
                Text(
                    label,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                color = if (value.startsWith("Select")) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface
            )
            supporting?.takeIf { it.isNotBlank() }?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FrequencyPickerSheet(
    selected: ScheduledFrequency,
    onSelect: (ScheduledFrequency) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Set frequency...",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(
                    onClick = onDismiss,
                ) { Icon(Icons.Default.Close, "Close") }
            }
            ScheduledFrequency.entries.forEach { frequency ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(frequency) }
                        .padding(vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(frequency.displayName(), style = MaterialTheme.typography.bodyLarge)
                    if (frequency == selected) {
                        Text(
                            "Selected",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(Icons.Default.ChevronRight, null)
                    }
                }
            }
            Spacer(Modifier.size(12.dp))
        }
    }
}

private fun frequencySupportingText(
    frequency: ScheduledFrequency,
    dateTime: LocalDateTime
): String = when (frequency) {
    ScheduledFrequency.NONE -> "Runs once on ${dateTime.format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"))}"
    ScheduledFrequency.DAILY -> "Starts ${dateTime.format(DateTimeFormatter.ofPattern("dd MMM"))} and repeats daily at ${
        dateTime.format(
            DateTimeFormatter.ofPattern("hh:mm a")
        )
    }"

    ScheduledFrequency.WEEKLY -> "Repeats every ${
        dateTime.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
    } at ${dateTime.format(DateTimeFormatter.ofPattern("hh:mm a"))}"

    ScheduledFrequency.MONTHLY -> "Repeats on day ${dateTime.dayOfMonth} of every month at ${
        dateTime.format(
            DateTimeFormatter.ofPattern("hh:mm a")
        )
    }"

    ScheduledFrequency.YEARLY -> "Repeats every year on ${
        dateTime.format(
            DateTimeFormatter.ofPattern(
                "dd MMM"
            )
        )
    } at ${dateTime.format(DateTimeFormatter.ofPattern("hh:mm a"))}"
}
