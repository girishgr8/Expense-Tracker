package com.expensetracker.presentation.ui.addtransaction

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.hilt.navigation.compose.hiltViewModel
import com.expensetracker.domain.model.TransactionType

private val AccentOrange = Color(0xFFFF9F2E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToAccounts: () -> Unit,
    viewModel: AddTransactionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var paymentSheetTarget by remember { mutableStateOf<PaymentSheetTarget?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showCalculator by remember { mutableStateOf(false) }
    var showCategorySheet by remember { mutableStateOf(false) }

    // Accepts only PDF and images
    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { viewModel.addAttachment(it) } }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onNavigateBack()
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    val fieldAccent = remember(uiState.selectedCategory) {
        uiState.selectedCategory?.let { category ->
            runCatching { Color(category.colorHex.toColorInt()) }.getOrDefault(AccentOrange)
        } ?: AccentOrange
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (uiState.isEditMode) "Edit Transaction" else "Add Transaction",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = viewModel::saveTransaction,
                        enabled = !uiState.isLoading
                    ) {
                        Text(
                            "Save",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
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
                accentColor = fieldAccent,
                onDateClick = { showDatePicker = true },
                onTimeClick = { showTimePicker = true }
            )

            // Transaction Amount Entry Row
            AmountEntryRow(
                amount = uiState.amount,
                accentColor = fieldAccent,
                onAmountChange = viewModel::setAmount,
                onOpenCalculator = { showCalculator = true }
            )

            CategorySelectionRow(
                category = uiState.selectedCategory,
                accentColor = fieldAccent,
                onClick = { showCategorySheet = true }
            )

            PaymentSelectorField(
                label = if (uiState.transactionType == TransactionType.TRANSFER) "From Account" else "Payment mode",
                value = uiState.selectedPaymentOption?.let(::paymentOptionHeadline).orEmpty(),
                supporting = uiState.selectedPaymentOption?.let(::paymentOptionSupportingText),
                icon = paymentOptionIcon(uiState.selectedPaymentOption),
                accentColor = fieldAccent,
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
                    accentColor = fieldAccent,
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
                accentColor = fieldAccent,
                onNoteChange = viewModel::setNote
            )

            SeamlessTagsSection(
                tags = uiState.tags,
                suggestions = uiState.tagSuggestions,
                accentColor = fieldAccent,
                onAddTag = viewModel::addTag,
                onRemoveTag = viewModel::removeTag,
                onSearchTag = viewModel::searchTags
            )

            SeamlessAttachmentRow(
                attachment = uiState.attachments.firstOrNull(),
                accentColor = fieldAccent,
                onPickAttachment = {
                    // MIME filter: only PDF + images
                    fileLauncher.launch("*/*")
                },
                onRemoveAttachment = {
                    uiState.attachments.firstOrNull()?.let { viewModel.removeAttachment(it) }
                }
            )
        }
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
}

// ─── Selector Field ───────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorField(
    label: String,
    value: String,
    icon: ImageVector,
    placeholder: String,
    dropdownContent: @Composable ColumnScope.(dismiss: () -> Unit) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = value, onValueChange = {}, readOnly = true,
            label = { Text(label) },
            leadingIcon = { Icon(icon, null) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            placeholder = { Text(placeholder) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            shape = RoundedCornerShape(12.dp)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            dropdownContent { expanded = false }
        }
    }
}

