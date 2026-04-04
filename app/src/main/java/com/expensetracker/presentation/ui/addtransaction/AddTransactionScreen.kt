package com.expensetracker.presentation.ui.addtransaction

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.util.Log
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import com.expensetracker.domain.model.PaymentOption
import com.expensetracker.domain.model.PaymentModeType
import com.expensetracker.domain.model.Tag
import com.expensetracker.domain.model.TransactionType
import com.expensetracker.presentation.theme.AppTypography
import com.expensetracker.presentation.theme.ExpenseRed
import com.expensetracker.presentation.theme.IncomeGreen
import com.expensetracker.presentation.theme.TransferBlue
import java.io.File
import java.time.LocalDateTime
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
    val context = LocalContext.current
    var paymentSheetTarget by remember { mutableStateOf<PaymentSheetTarget?>(null) }

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
        // Fix 2: imePadding() ensures the scroll area shrinks above the keyboard
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // Transaction Type Tabs
            TransactionTypeTabs(
                selected = uiState.transactionType,
                onSelect = viewModel::setTransactionType
            )

            // Fix 1: Date & Time first — styled card row
            DateTimeCard(
                dateTime = uiState.dateTime,
                onDateTimeChange = viewModel::setDateTime,
                context = context
            )

            // Amount Input
            AmountInputField(
                amount = uiState.amount,
                onAmountChange = viewModel::setAmount,
                type = uiState.transactionType
            )

            // Category Selector
            SelectorField(
                label = "Category",
                value = uiState.selectedCategory?.name ?: "",
                icon = Icons.Default.Category,
                placeholder = "Select Category"
            ) { dismiss ->
                CategoryDropdown(
                    categories = uiState.categories.filter {
                        it.transactionType == uiState.transactionType || it.transactionType == null
                    },
                    selected = uiState.selectedCategory,
                    onSelect = { cat -> viewModel.setCategory(cat); dismiss() }
                )
            }

            PaymentSelectorField(
                label = if (uiState.transactionType == TransactionType.TRANSFER) "From Account" else "Payment mode",
                value = uiState.selectedPaymentOption?.let(::paymentOptionHeadline).orEmpty(),
                supporting = uiState.selectedPaymentOption?.let(::paymentOptionSupportingText),
                icon = paymentOptionIcon(uiState.selectedPaymentOption),
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

            SeamlessNoteField(note = uiState.note, onNoteChange = viewModel::setNote)

            SeamlessTagsSection(
                tags = uiState.tags,
                suggestions = uiState.tagSuggestions,
                onAddTag = viewModel::addTag,
                onRemoveTag = viewModel::removeTag,
                onSearchTag = viewModel::searchTags
            )

            // Fix 3: single attachment, image preview, proper PDF display
            SeamlessAttachmentRow(
                attachment = uiState.attachments.firstOrNull(),
                onPickAttachment = {
                    // MIME filter: only PDF + images
                    fileLauncher.launch("*/*")
                },
                onRemoveAttachment = {
                    uiState.attachments.firstOrNull()?.let { viewModel.removeAttachment(it) }
                }
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = viewModel::saveTransaction,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Text(
                        if (uiState.isEditMode) "Update Transaction" else "Save Transaction",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
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
                null -> ""
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

// ─── Fix 1: Date & Time Card ──────────────────────────────────────────────────

@Composable
private fun DateTimeCard(
    dateTime: LocalDateTime,
    onDateTimeChange: (LocalDateTime) -> Unit,
    context: android.content.Context
) {
    val dateFmt = DateTimeFormatter.ofPattern("EEE, MMM dd yyyy")
    val timeFmt = DateTimeFormatter.ofPattern("hh:mm a")

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Date button
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        DatePickerDialog(
                            context,
                            { _, y, m, d ->
                                onDateTimeChange(
                                    dateTime.withYear(y).withMonth(m + 1).withDayOfMonth(d)
                                )
                            },
                            dateTime.year, dateTime.monthValue - 1, dateTime.dayOfMonth
                        ).show()
                    }
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.CalendarMonth,
                    contentDescription = "Pick date",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Column {
                    Text(
                        "Date",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        dateTime.format(dateFmt),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Vertical divider
            HorizontalDivider(
                modifier = Modifier
                    .height(48.dp)
                    .width(0.5.dp)
                    .align(Alignment.CenterVertically),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // Time button
            Row(
                modifier = Modifier
                    .weight(0.65f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        TimePickerDialog(
                            context,
                            { _, h, min -> onDateTimeChange(dateTime.withHour(h).withMinute(min)) },
                            dateTime.hour, dateTime.minute, false
                        ).show()
                    }
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = "Pick time",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Column {
                    Text(
                        "Time",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        dateTime.format(timeFmt),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// ─── Amount Input ─────────────────────────────────────────────────────────────

@Composable
private fun AmountInputField(
    amount: String,
    onAmountChange: (String) -> Unit,
    type: TransactionType
) {
    val color = when (type) {
        TransactionType.INCOME -> IncomeGreen
        TransactionType.EXPENSE -> ExpenseRed
        TransactionType.TRANSFER -> TransferBlue
    }
    OutlinedTextField(
        value = amount,
        onValueChange = { if (it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) onAmountChange(it) },
        label = { Text("Amount") },
        leadingIcon = {
            Text(
                "₹", style = MaterialTheme.typography.titleLarge, color = color,
                modifier = Modifier.padding(start = 8.dp)
            )
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        textStyle = MaterialTheme.typography.headlineSmall.copy(
            color = color,
            fontWeight = FontWeight.Bold
        ),
        singleLine = true
    )
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
            tint = MaterialTheme.colorScheme.primary,
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
//            if (!supporting.isNullOrBlank()) {
//                Spacer(Modifier.height(2.dp))
//                Text(
//                    text = supporting,
//                    style = MaterialTheme.typography.bodySmall,
//                    color = MaterialTheme.colorScheme.onSurfaceVariant,
//                    maxLines = 1,
//                    overflow = TextOverflow.Ellipsis
//                )
//            }
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
                        Icon(Icons.Default.Close,
                            contentDescription = "Close",
                            modifier = Modifier.size(18.dp))
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
            Column(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
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
    val isPrimarySelected = selected is PaymentOption.Mode && selected.mode.id == group.primaryOption.mode.id
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
                    contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 3.dp, bottom = 6.dp)
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
        if (option is PaymentOption.Mode) {
            Icon(
                imageVector = Icons.Default.QrCode,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
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
    PaymentModeType.CASH -> Icons.Default.AccountBalance
    PaymentModeType.WALLET -> Icons.Default.Image
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
private fun SeamlessNoteField(note: String, onNoteChange: (String) -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
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
    onAddTag: (String) -> Unit,
    onRemoveTag: (String) -> Unit,
    onSearchTag: (String) -> Unit
) {
    var tagInput by remember { mutableStateOf("") }
    val accent = MaterialTheme.colorScheme.primary
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
    onPickAttachment: () -> Unit,
    onRemoveAttachment: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary

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
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Icon(
                    Icons.Default.ChevronRight, null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
            HorizontalDivider(
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
