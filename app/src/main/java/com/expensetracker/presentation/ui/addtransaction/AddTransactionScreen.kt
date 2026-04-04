package com.expensetracker.presentation.ui.addtransaction

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.expensetracker.R
import com.expensetracker.domain.model.Attachment
import com.expensetracker.domain.model.Category
import com.expensetracker.domain.model.PaymentModeType
import com.expensetracker.domain.model.PaymentOption
import com.expensetracker.domain.model.Tag
import com.expensetracker.domain.model.TransactionType
import com.expensetracker.presentation.components.CategoryIconBubble
import com.expensetracker.presentation.theme.ExpenseRed
import com.expensetracker.presentation.theme.IncomeGreen
import com.expensetracker.presentation.theme.TransferBlue
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

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

            TransactionDateTimeRow(
                dateTime = uiState.dateTime,
                accentColor = fieldAccent,
                onDateClick = { showDatePicker = true },
                onTimeClick = { showTimePicker = true }
            )

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

            // Fix 3: single attachment, image preview, proper PDF display
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

// ─── Transaction Type Tabs ────────────────────────────────────────────────────

@Composable
private fun TransactionTypeTabs(selected: TransactionType, onSelect: (TransactionType) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        TransactionType.entries.forEach { type ->
            val isSelected = type == selected
            val bg = when {
                isSelected && type == TransactionType.EXPENSE -> ExpenseRed
                isSelected && type == TransactionType.INCOME -> IncomeGreen
                isSelected -> TransferBlue
                else -> Color.Transparent
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(bg, RoundedCornerShape(8.dp))
                    .clickable { onSelect(type) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    type.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) Color.White
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

// ─── Transaction Header Fields ────────────────────────────────────────────────

private val AccentOrange = Color(0xFFFF9F2E)

@Composable
private fun TransactionDateTimeRow(
    dateTime: LocalDateTime,
    accentColor: Color,
    onDateClick: () -> Unit,
    onTimeClick: () -> Unit
) {
    val dateFmt = DateTimeFormatter.ofPattern("dd MMM yyyy")
    val timeFmt = DateTimeFormatter.ofPattern("hh:mm a")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        DateTimeInlineField(
            icon = Icons.Default.CalendarMonth,
            value = dateTime.format(dateFmt),
            accentColor = accentColor,
            onClick = onDateClick
        )
        DateTimeInlineField(
            icon = Icons.Default.Schedule,
            value = dateTime.format(timeFmt).uppercase(),
            accentColor = accentColor,
            onClick = onTimeClick
        )
    }
}

@Composable
private fun DateTimeInlineField(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    value: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "Date/Time Field",
            tint = accentColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontSize = 16.sp,
                lineHeight = 20.sp
            ),
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AmountEntryRow(
    amount: String,
    accentColor: Color,
    onAmountChange: (String) -> Unit,
    onOpenCalculator: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "₹",
            style = MaterialTheme.typography.headlineLarge.copy(fontSize = 28.sp),
            color = accentColor,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Amount",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(2.dp))
            BasicTextField(
                value = amount,
                onValueChange = {
                    if (it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) onAmountChange(it)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Normal
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (amount.isBlank()) {
                            Text(
                                text = "0",
                                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 22.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                            )
                        }
                        inner()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.width(16.dp))
        IconButton(
            onClick = onOpenCalculator,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                .padding(0.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_calculator_logo),
                contentDescription = "Calculator",
                modifier = Modifier.size(28.dp),
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            )
//            Icon(
//                imageVector = Icons.Default.Calculate,
//                contentDescription = "Calculator",
//                tint = MaterialTheme.colorScheme.onSurfaceVariant,
//                modifier = Modifier.size(24.dp)
//            )
        }
    }
}

@Composable
private fun CategorySelectionRow(
    category: Category?,
    accentColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (category != null) {
            Icon(
                imageVector = com.expensetracker.presentation.ui.categories.CategoryIcons.get(category.icon),
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(26.dp)
            )
        } else {
            Icon(
                imageVector = Icons.Default.Category,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Category",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = category?.name ?: "Select category",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (category != null) FontWeight.Medium else FontWeight.Normal,
                color = if (category != null) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(12.dp))
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
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

private enum class PaymentSheetTarget { FROM, TO }

@Composable
private fun PaymentSelectorField(
    label: String,
    value: String,
    supporting: String?,
    icon: ImageVector,
    accentColor: Color,
    placeholder: String,
    onClick: () -> Unit
) {
    val hasValue = value.isNotBlank()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (hasValue) value else placeholder,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (hasValue) FontWeight.Medium else FontWeight.Normal,
                color = if (hasValue) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (hasValue && !supporting.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = supporting,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ─── Category Dropdown ────────────────────────────────────────────────────────

@Composable
private fun ColumnScope.CategoryDropdown(
    categories: List<Category>,
    selected: Category?,
    onSelect: (Category) -> Unit
) {
    categories.forEach { cat ->
        val iconColor = runCatching {
            Color(cat.colorHex.toColorInt())
        }.getOrDefault(Color.Gray)
        DropdownMenuItem(
            text = { Text(cat.name) },
            leadingIcon = {
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        com.expensetracker.presentation.ui.categories.CategoryIcons.get(cat.icon),
                        null, tint = iconColor, modifier = Modifier.size(18.dp)
                    )
                }
            },
            trailingIcon = if (cat.id == selected?.id) {
                { Icon(Icons.Default.Check, null) }
            } else null,
            onClick = { onSelect(cat) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionDatePickerDialog(
    dateTime: LocalDateTime,
    onConfirm: (java.time.LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val zoneId = remember { ZoneId.systemDefault() }
    val initialMillis = remember(dateTime) {
        dateTime.atZone(zoneId).toInstant().toEpochMilli()
    }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onConfirm(
                            Instant.ofEpochMilli(millis)
                                .atZone(zoneId)
                                .toLocalDate()
                        )
                    }
                }
            ) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } }
    ) {
        DatePicker(state = datePickerState, showModeToggle = true)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionTimePickerDialog(
    dateTime: LocalDateTime,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var showTextInput by remember { mutableStateOf(false) }
    val pickerState = rememberTimePickerState(
        initialHour = dateTime.hour,
        initialMinute = dateTime.minute,
        is24Hour = false
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "SELECT TIME",
                style = MaterialTheme.typography.labelLarge,
                letterSpacing = 2.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (showTextInput) {
                    TimeInput(state = pickerState)
                } else {
                    TimePicker(state = pickerState)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(pickerState.hour, pickerState.minute) }) {
                Text("OK")
            }
        },
        dismissButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { showTextInput = !showTextInput }) {
                    Icon(
                        imageVector = Icons.Default.Keyboard,
                        contentDescription = "Toggle input mode"
                    )
                }
                TextButton(onClick = onDismiss) { Text("CANCEL") }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AmountCalculatorSheet(
    initialAmount: String,
    onDone: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var expression by remember(initialAmount) {
        mutableStateOf(initialAmount.takeIf { it.isNotBlank() } ?: "0")
    }
    val evaluated = remember(expression) { evaluateCalculatorExpression(expression) }
    val evaluatedText = evaluated
        ?.let(::formatAmountValue)
        ?.takeIf { it != expression.ifBlank { "0" } }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CalculatorHeaderButton(text = "Cancel", onClick = onDismiss)
                CalculatorHeaderButton(
                    text = "Done",
                    onClick = {
                        evaluated?.let { onDone(formatAmountValue(it)) }
                    },
                    enabled = evaluated != null
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.45f))
                    .padding(horizontal = 24.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = expression.ifBlank { "0" },
                    style = MaterialTheme.typography.headlineMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (evaluatedText != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = evaluatedText,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(460.dp)
            ) {
                Row(Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .weight(3f)
                            .fillMaxHeight()
                    ) {
                        CalculatorRow(
                            "C", "⌫", "%"
                        ) { input ->
                            expression = reduceCalculatorInput(expression, input)
                        }
                        CalculatorRow("7", "8", "9") { input ->
                            expression = reduceCalculatorInput(expression, input)
                        }
                        CalculatorRow("4", "5", "6") { input ->
                            expression = reduceCalculatorInput(expression, input)
                        }
                        CalculatorRow("1", "2", "3") { input ->
                            expression = reduceCalculatorInput(expression, input)
                        }
                        CalculatorRow("00", "0", ".") { input ->
                            expression = reduceCalculatorInput(expression, input)
                        }
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        listOf("÷", "×", "-", "+", "=").forEach { op ->
                            CalculatorOperationButton(
                                label = op,
                                modifier = Modifier.weight(1f)
                            ) {
                                expression = reduceCalculatorInput(expression, op)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CalculatorHeaderButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(24.dp),
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 10.dp)
    ) { Text(text, fontWeight = FontWeight.Bold) }
}

@Composable
private fun ColumnScope.CalculatorRow(
    vararg labels: String,
    onTap: (String) -> Unit
) {
    Row(
        modifier = Modifier.weight(1f),
        horizontalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        labels.forEach { label ->
            CalculatorKeyButton(
                label = label,
                modifier = Modifier.weight(1f),
                onClick = { onTap(label) }
            )
        }
    }
}

@Composable
private fun CalculatorKeyButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        when (label) {
            "⌫" -> Icon(
                Icons.AutoMirrored.Filled.Backspace,
                contentDescription = "Backspace",
                modifier = Modifier.size(28.dp)
            )

            else -> Text(
                label,
                style = MaterialTheme.typography.displaySmall.copy(fontSize = 28.sp),
                fontWeight = FontWeight.Normal
            )
        }
    }
}

@Composable
private fun CalculatorOperationButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                if (label == "=") MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
                else MaterialTheme.colorScheme.background
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Normal
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryPickerSheet(
    categories: List<Category>,
    selected: Category?,
    onSelect: (Category) -> Unit,
    onManageCategories: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var gridMode by remember { mutableStateOf(true) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.6f)
                .navigationBarsPadding()
                .padding(horizontal = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Select Category",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(start = 10.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GridListToggle(
                        gridMode = gridMode,
                        onGrid = { gridMode = true },
                        onList = { gridMode = false }
                    )
                    Spacer(Modifier.width(4.dp))
                    CircularIconButton(Icons.Default.Edit, "Manage categories", onManageCategories)
                    CircularIconButton(Icons.Default.Close, "Close", onDismiss)
                }
            }

            if (gridMode) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(categories.size) { index ->
                        val category = categories[index]
                        CategoryGridItem(
                            category = category,
                            selected = selected?.id == category.id,
                            onClick = { onSelect(category) }
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    items(categories) { category ->
                        CategoryListItem(
                            category = category,
                            selected = selected?.id == category.id,
                            onClick = { onSelect(category) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GridListToggle(
    gridMode: Boolean,
    onGrid: () -> Unit,
    onList: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        TogglePillButton(
            selected = gridMode,
            icon = Icons.Default.GridView,
            onClick = onGrid
        )
        TogglePillButton(
            selected = !gridMode,
            icon = Icons.AutoMirrored.Filled.ViewList,
            onClick = onList
        )
    }
}

@Composable
private fun TogglePillButton(
    selected: Boolean,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) MaterialTheme.colorScheme.background else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CircularIconButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun CategoryGridItem(
    category: Category,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(
                    if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    else Color.Transparent
                )
                .padding(4.dp)
        ) {
            CategoryIconBubble(category = category, size = 40)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = category.name,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CategoryListItem(
    category: Category,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CategoryIconBubble(category = category, size = 40)
        Spacer(Modifier.width(14.dp))
        Text(
            text = category.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private fun reduceCalculatorInput(expression: String, input: String): String = when (input) {
    "C" -> "0"
    "⌫" -> expression.dropLast(1).ifBlank { "0" }
    "=" -> evaluateCalculatorExpression(expression)?.let(::formatAmountValue) ?: expression
    "%" -> applyPercentToExpression(expression)
    "+", "-", "×", "÷" -> appendOperator(expression, input)
    "." -> appendDecimal(expression)
    else -> appendDigit(expression, input)
}

private fun appendDigit(expression: String, digit: String): String {
    if (expression == "0") {
        return digit.trimStart('0').ifBlank { "0" }
    }
    if (expression.takeLastWhile { it.isDigit() }.length >= 12) return expression
    return expression + digit
}

private fun appendDecimal(expression: String): String {
    val tail = expression.takeLastWhile { it.isDigit() || it == '.' }
    return if (tail.contains('.')) expression else "$expression."
}

private fun appendOperator(expression: String, operator: String): String {
    val trimmed = expression.trim()
    if (trimmed.isBlank()) return "0"
    return if (trimmed.last() in listOf('+', '-', '×', '÷')) {
        trimmed.dropLast(1) + operator
    } else {
        "$trimmed$operator"
    }
}

private fun applyPercentToExpression(expression: String): String {
    val match = Regex("(\\d+(?:\\.\\d+)?)$").find(expression) ?: return expression
    val number = match.value.toDoubleOrNull() ?: return expression
    val replacement = formatAmountValue(number / 100.0)
    return expression.replaceRange(match.range, replacement)
}

private fun evaluateCalculatorExpression(expression: String): Double? {
    val normalized = expression.replace('×', '*').replace('÷', '/').trim()
    if (normalized.isBlank()) return null
    return runCatching {
        val values = ArrayDeque<Double>()
        val ops = ArrayDeque<Char>()
        var i = 0
        while (i < normalized.length) {
            val c = normalized[i]
            when {
                c == ' ' -> i++
                c.isDigit() || c == '.' -> {
                    val start = i
                    while (i < normalized.length &&
                        (normalized[i].isDigit() || normalized[i] == '.')
                    ) {
                        i++
                    }
                    values.addLast(normalized.substring(start, i).toDouble())
                    continue
                }

                c in charArrayOf('+', '-', '*', '/') -> {
                    while (ops.isNotEmpty() && precedence(ops.last()) >= precedence(c)) {
                        applyTopOperation(values, ops.removeLast())
                    }
                    ops.addLast(c)
                }

                else -> return null
            }
            i++
        }
        while (ops.isNotEmpty()) {
            applyTopOperation(values, ops.removeLast())
        }
        values.singleOrNull()
    }.getOrNull()
}

private fun precedence(op: Char): Int = when (op) {
    '*', '/' -> 2
    '+', '-' -> 1
    else -> 0
}

private fun applyTopOperation(values: ArrayDeque<Double>, op: Char) {
    if (values.size < 2) return
    val right = values.removeLast()
    val left = values.removeLast()
    val result = when (op) {
        '+' -> left + right
        '-' -> left - right
        '*' -> left * right
        '/' -> if (right == 0.0) 0.0 else left / right
        else -> left
    }
    values.addLast(result)
}

private fun formatAmountValue(amount: Double): String {
    val rounded = kotlin.math.round(amount * 100) / 100
    return if (rounded % 1.0 == 0.0) rounded.toLong().toString()
    else "%,.2f".format(rounded).replace(",", "")
}

// ─── Payment Option Dropdown ──────────────────────────────────────────────────

@Composable
private fun PaymentOptionDropdown(
    options: List<PaymentOption>,
    selected: PaymentOption?,
    onSelect: (PaymentOption) -> Unit
) {
    if (options.isEmpty()) {
        DropdownMenuItem(text = { Text("No payment options added") }, onClick = {}, enabled = false)
        return
    }
    val modes = options.filterIsInstance<PaymentOption.Mode>()
    val cards = options.filterIsInstance<PaymentOption.Card>()
    if (modes.isNotEmpty()) {
        DropdownMenuItem(
            text = {
                Text(
                    "Payment Modes", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            onClick = {}, enabled = false
        )
        modes.forEach { option ->
            DropdownMenuItem(
                text = { Text(option.displayLabel) },
                leadingIcon = {
                    Icon(
                        Icons.Default.AccountBalance, null, Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = if (selected is PaymentOption.Mode && selected.mode.id == option.mode.id) {
                    { Icon(Icons.Default.Check, null) }
                } else null,
                onClick = { onSelect(option) }
            )
        }
    }
    if (cards.isNotEmpty()) {
        HorizontalDivider()
        DropdownMenuItem(
            text = {
                Text(
                    "Credit Cards", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            onClick = {}, enabled = false
        )
        cards.forEach { option ->
            DropdownMenuItem(
                text = { Text(option.displayLabel) },
                leadingIcon = {
                    Icon(
                        Icons.Default.CreditCard, null, Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                trailingIcon = if (selected is PaymentOption.Card && selected.card.id == option.card.id) {
                    { Icon(Icons.Default.Check, null) }
                } else null,
                onClick = { onSelect(option) }
            )
        }
    }
}

private data class PaymentOptionSheetSection(
    val title: String,
    val icon: ImageVector,
    val groups: List<PaymentOptionSheetGroup>
)

private sealed interface PaymentOptionSheetGroup {
    data class BankAccountGroup(
        val bankName: String,
        val primaryOption: PaymentOption.Mode,
        val modes: List<PaymentOption.Mode>
    ) : PaymentOptionSheetGroup

    data class SimpleOptions(
        val options: List<PaymentOption>
    ) : PaymentOptionSheetGroup
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaymentOptionSheet(
    title: String,
    options: List<PaymentOption>,
    selected: PaymentOption?,
    onSelect: (PaymentOption) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sections = remember(options) { buildPaymentOptionSections(options) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 720.dp)
                .padding(bottom = 20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 16.dp, top = 15.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (sections.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No payment modes available yet",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 15.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    sections.forEach { section ->
                        PaymentOptionSectionCard(
                            section = section,
                            selected = selected,
                            onSelect = onSelect
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentOptionSectionCard(
    section: PaymentOptionSheetSection,
    selected: PaymentOption?,
    onSelect: (PaymentOption) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = section.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = section.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp)
            ) {
                section.groups.forEachIndexed { index, group ->
                    when (group) {
                        is PaymentOptionSheetGroup.BankAccountGroup -> BankAccountOptionGroup(
                            group = group,
                            selected = selected,
                            onSelect = onSelect
                        )

                        is PaymentOptionSheetGroup.SimpleOptions -> group.options.forEachIndexed { optionIndex, option ->
                            PaymentOptionRow(
                                option = option,
                                selected = selected?.id == option.id &&
                                        selected::class == option::class,
                                onClick = { onSelect(option) }
                            )
                            if (optionIndex < group.options.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 10.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    thickness = 0.6.dp
                                )
                            }
                        }
                    }
                    if (index < section.groups.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 10.dp, end = 5.dp, top = 2.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            thickness = 0.6.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BankAccountOptionGroup(
    group: PaymentOptionSheetGroup.BankAccountGroup,
    selected: PaymentOption?,
    onSelect: (PaymentOption) -> Unit
) {
    val isPrimarySelected =
        selected is PaymentOption.Mode && selected.mode.id == group.primaryOption.mode.id
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelect(group.primaryOption) }
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isPrimarySelected,
                onClick = null,
                modifier = Modifier.scale(0.7f),
                colors = RadioButtonDefaults.colors(
                    selectedColor = MaterialTheme.colorScheme.primary,
                    unselectedColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = group.bankName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.weight(1f)
            )
            group.primaryOption.mode.identifier
                .takeIf { it.isNotBlank() }
                ?.let {
                    Text(
                        text = maskIdentifier(it),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
        }

        if (group.modes.isNotEmpty()) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Linked payment modes",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 10.dp, bottom = 4.dp)
            )
            group.modes.forEachIndexed { index, option ->
                PaymentOptionRow(
                    option = option,
                    selected = selected is PaymentOption.Mode && selected.mode.id == option.mode.id,
                    onClick = { onSelect(option) },
                    contentPadding = PaddingValues(
                        start = 10.dp,
                        end = 10.dp,
                        top = 3.dp,
                        bottom = 6.dp
                    )
                )
            }
        }
    }
}

@Composable
private fun PaymentOptionRow(
    option: PaymentOption,
    selected: Boolean,
    onClick: () -> Unit,
    contentPadding: PaddingValues = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            modifier = Modifier.scale(0.7f),
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary,
                unselectedColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )
        )
        Spacer(Modifier.width(4.dp))
        if (option is PaymentOption.Mode && shouldShowSheetLeadingIcon(option.mode.type)) {
            PaymentModeSheetIcon(
                type = option.mode.type,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = paymentOptionHeadline(option),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
//            paymentOptionSupportingText(option)?.takeIf { it.isNotBlank() }?.let { supporting ->
//                Text(
//                    text = supporting,
//                    style = MaterialTheme.typography.bodySmall,
//                    color = MaterialTheme.colorScheme.onSurfaceVariant,
//                    maxLines = 1,
//                    overflow = TextOverflow.Ellipsis
//                )
//            }
        }
        paymentOptionTrailingText(option)?.let { trailing ->
            Spacer(Modifier.width(12.dp))
            Text(
                text = trailing,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun shouldShowSheetLeadingIcon(type: PaymentModeType): Boolean = when (type) {
    PaymentModeType.CASH, PaymentModeType.WALLET -> false
    else -> true
}

@Composable
private fun PaymentModeSheetIcon(
    type: PaymentModeType,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    when (type) {
        PaymentModeType.UPI -> Image(
            painter = painterResource(id = R.drawable.ic_upi_logo),
            contentDescription = "UPI",
            modifier = modifier
        )

        PaymentModeType.DEBIT_CARD -> Image(
            painter = painterResource(id = R.drawable.ic_payment_card_logo),
            contentDescription = "Debit Card",
            modifier = modifier
        )

        PaymentModeType.WALLET -> Image(
            painter = painterResource(id = R.drawable.ic_wallet_logo),
            contentDescription = "Wallet",
            modifier = modifier
        )

        else -> {
            val icon = when (type) {
                PaymentModeType.NET_BANKING -> Icons.Default.AccountBalance
                PaymentModeType.CHEQUE -> Icons.Default.EditNote
                PaymentModeType.CASH -> Icons.Default.Payments
                else -> Icons.Default.Payment
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = modifier
            )
        }
    }
}

private fun buildPaymentOptionSections(options: List<PaymentOption>): List<PaymentOptionSheetSection> {
    val modeOptions = options.filterIsInstance<PaymentOption.Mode>()
    val cardOptions = options.filterIsInstance<PaymentOption.Card>()

    val bankGroups = modeOptions
        .filter { it.mode.bankAccountId != null }
        .groupBy { it.mode.bankAccountId to it.mode.bankAccountName }
        .values
        .map { groupedModes ->
            val primary = groupedModes.firstOrNull { it.mode.type == PaymentModeType.NET_BANKING }
                ?: groupedModes.first()
            PaymentOptionSheetGroup.BankAccountGroup(
                bankName = groupedModes.first().mode.bankAccountName.ifBlank { "Bank Account" },
                primaryOption = primary,
                modes = groupedModes
                    .filterNot { samePaymentOption(it, primary) }
                    .sortedBy { paymentOptionHeadline(it) }
            )
        }
        .sortedBy { it.bankName.lowercase() }

    val standaloneGroups = modeOptions
        .filter { it.mode.bankAccountId == null }
        .groupBy { standaloneSectionTitle(it.mode.type) }
        .toSortedMap()
        .map { (title, groupedModes) ->
            PaymentOptionSheetSection(
                title = title,
                icon = standaloneSectionIcon(groupedModes.first().mode.type),
                groups = listOf(
                    PaymentOptionSheetGroup.SimpleOptions(
                        groupedModes.sortedBy { paymentOptionHeadline(it) }
                    )
                )
            )
        }

    val sections = mutableListOf<PaymentOptionSheetSection>()
    if (bankGroups.isNotEmpty()) {
        sections += PaymentOptionSheetSection(
            title = "Bank Accounts",
            icon = Icons.Default.AccountBalance,
            groups = bankGroups
        )
    }
    if (cardOptions.isNotEmpty()) {
        sections += PaymentOptionSheetSection(
            title = "Credit Cards",
            icon = Icons.Default.CreditCard,
            groups = listOf(
                PaymentOptionSheetGroup.SimpleOptions(
                    cardOptions.sortedBy { paymentOptionHeadline(it) }
                )
            )
        )
    }
    sections += standaloneGroups
    return sections
}

private fun paymentOptionIcon(option: PaymentOption?): ImageVector = when (option) {
    is PaymentOption.Card -> Icons.Default.CreditCard
    is PaymentOption.Mode -> standaloneSectionIcon(option.mode.type)
    null -> Icons.Default.AccountBalance
}

private fun standaloneSectionIcon(type: PaymentModeType): ImageVector = when (type) {
    PaymentModeType.DEBIT_CARD -> Icons.Default.CreditCard
    PaymentModeType.UPI -> Icons.Default.SwapHoriz
    PaymentModeType.NET_BANKING -> Icons.Default.AccountBalance
    PaymentModeType.CHEQUE -> Icons.Default.Edit
    PaymentModeType.CASH -> Icons.Default.Payments
    PaymentModeType.WALLET -> Icons.Default.Wallet
    PaymentModeType.OTHER -> Icons.Default.AccountBalance
}

private fun standaloneSectionTitle(type: PaymentModeType): String = when (type) {
    PaymentModeType.CASH -> "Cash"
    PaymentModeType.WALLET -> "Wallets"
    PaymentModeType.OTHER -> "Other payment modes"
    else -> "Payment Modes"
}

private fun paymentOptionHeadline(option: PaymentOption): String = when (option) {
    is PaymentOption.Card -> option.card.name
    is PaymentOption.Mode -> when {
        option.mode.bankAccountId != null &&
                option.mode.type == PaymentModeType.NET_BANKING &&
                option.mode.bankAccountName.isNotBlank() -> option.mode.bankAccountName

        option.mode.identifier.isNotBlank() -> option.mode.identifier
        option.mode.bankAccountName.isNotBlank() -> option.mode.bankAccountName
        else -> option.mode.type.displayName()
    }
}

private fun paymentOptionSupportingText(option: PaymentOption): String? = when (option) {
    is PaymentOption.Card -> "Credit card"
    is PaymentOption.Mode -> buildString {
        if (option.mode.bankAccountName.isNotBlank() &&
            option.mode.identifier.isNotBlank() &&
            option.mode.identifier != option.mode.bankAccountName
        ) {
            append(option.mode.bankAccountName)
            append(" • ")
        }
        append(option.mode.type.displayName())
    }.takeIf { it.isNotBlank() }
}

private fun paymentOptionTrailingText(option: PaymentOption): String? = when (option) {
    is PaymentOption.Card -> null
    is PaymentOption.Mode -> option.mode.identifier
        .takeIf { it.isNotBlank() && it.any(Char::isDigit) }
        ?.let(::maskIdentifier)
}

private fun samePaymentOption(left: PaymentOption?, right: PaymentOption?): Boolean {
    if (left == null || right == null) return false
    return left.id == right.id && left::class == right::class
}

private fun maskIdentifier(value: String): String {
    val digits = value.filter(Char::isDigit)
    if (digits.length >= 4) return "*".repeat(digits.length - 4) + digits.takeLast(4)
    return value
}

// ─── Seamless Note Field ──────────────────────────────────────────────────────

@Composable
private fun SeamlessNoteField(
    note: String,
    accentColor: Color,
    onNoteChange: (String) -> Unit
) {
    val accent = accentColor
    val onSurface = MaterialTheme.colorScheme.onSurface
    val hint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Icon(
            Icons.AutoMirrored.Filled.Notes, null, tint = accent,
            modifier = Modifier
                .padding(top = 14.dp)
                .size(22.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            BasicTextField(
                value = note, onValueChange = onNoteChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = onSurface),
                cursorBrush = SolidColor(accent),
                maxLines = 5,
                decorationBox = { inner ->
                    if (note.isEmpty()) Text(
                        "Write a note",
                        style = MaterialTheme.typography.bodyLarge, color = hint
                    )
                    inner()
                }
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                thickness = 0.5.dp
            )
        }
    }
}

// ─── Seamless Tags Section ────────────────────────────────────────────────────

@Composable
private fun SeamlessTagsSection(
    tags: List<String>,
    suggestions: List<Tag>,
    accentColor: Color,
    onAddTag: (String) -> Unit,
    onRemoveTag: (String) -> Unit,
    onSearchTag: (String) -> Unit
) {
    var tagInput by remember { mutableStateOf("") }
    val accent = accentColor
    val onSurface = MaterialTheme.colorScheme.onSurface
    val hint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Icon(
            Icons.Default.Tag, null, tint = accent,
            modifier = Modifier
                .padding(top = 14.dp)
                .size(22.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                BasicTextField(
                    value = tagInput,
                    onValueChange = { tagInput = it; onSearchTag(it) },
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 12.dp),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = onSurface),
                    cursorBrush = SolidColor(accent),
                    singleLine = true,
                    decorationBox = { inner ->
                        if (tagInput.isEmpty()) Text(
                            "Add tags",
                            style = MaterialTheme.typography.bodyLarge, color = hint
                        )
                        inner()
                    }
                )
                if (tagInput.isNotBlank() && tags.size < 5) {
                    IconButton(
                        onClick = { onAddTag(tagInput.trimStart('#')); tagInput = "" },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Add, "Add Tag",
                            Modifier.size(18.dp), tint = accent
                        )
                    }
                }
            }
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                thickness = 0.5.dp
            )
            if (suggestions.isNotEmpty() && tagInput.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(suggestions) { tag ->
                        SuggestionChip(
                            onClick = { onAddTag(tag.name); tagInput = "" },
                            label = { Text("#${tag.name}") })
                    }
                }
            }
            if (tags.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(tags) { tag ->
                        PillTagChip(tag = tag, onRemove = { onRemoveTag(tag) })
                    }
                }
            }
        }
    }
}

@Composable
private fun PillTagChip(tag: String, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onRemove() }
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
        )
        Text(
            tag, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Icon(
            Icons.Default.Close, "Remove $tag",
            Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

// ─── Fix 3: Seamless Attachment Row — single file, image preview, PDF card ───

@Composable
private fun SeamlessAttachmentRow(
    attachment: Attachment?,
    accentColor: Color,
    onPickAttachment: () -> Unit,
    onRemoveAttachment: () -> Unit
) {
    val accent = accentColor

    Column {
        // Tap row — shown always if no attachment yet, or if we want to replace
        if (attachment == null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPickAttachment() }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.AttachFile, null, tint = accent,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(16.dp))
                Text(
                    "Add attachment",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
                Icon(
                    Icons.Default.ChevronRight, null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
            HorizontalDivider(
                modifier = Modifier.padding(start = 38.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                thickness = 0.5.dp
            )
        } else {
            Spacer(Modifier.height(4.dp))
            val isImage = attachment.mimeType.startsWith("image/")
            val isPdf = attachment.mimeType == "application/pdf"

            if (isImage) {
                // Full-width image preview card with remove + replace buttons
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Box(Modifier.fillMaxWidth()) {
                        AsyncImage(
                            model = File(attachment.filePath),
                            contentDescription = attachment.fileName,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp, max = 240.dp)
                                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        // Remove button overlaid top-right
                        IconButton(
                            onClick = onRemoveAttachment,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                Icons.Default.Close, "Remove",
                                Modifier.size(16.dp), tint = Color.White
                            )
                        }
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Image, null,
                            Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            attachment.fileName,
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            formatFileSize(attachment.fileSizeBytes),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                // Replace link
                TextButton(
                    onClick = onPickAttachment,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(Icons.Default.SwapHoriz, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Replace", style = MaterialTheme.typography.labelMedium)
                }
            }

            if (isPdf) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // PDF logo box
                        Box(
                            Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFB71C1C).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.PictureAsPdf, null,
                                Modifier.size(28.dp), tint = Color(0xFFB71C1C)
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                attachment.fileName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2, overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                formatFileSize(attachment.fileSizeBytes),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            IconButton(
                                onClick = onRemoveAttachment,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close, "Remove",
                                    Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                            TextButton(
                                onClick = onPickAttachment,
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text("Replace", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String = when {
    bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576f)
    bytes >= 1024 -> "%.1f KB".format(bytes / 1024f)
    else -> "$bytes B"
}
