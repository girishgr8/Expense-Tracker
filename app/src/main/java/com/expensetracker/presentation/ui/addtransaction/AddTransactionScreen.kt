package com.expensetracker.presentation.ui.addtransaction

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import androidx.hilt.navigation.compose.hiltViewModel
import com.expensetracker.domain.model.Attachment
import com.expensetracker.domain.model.Category
import com.expensetracker.domain.model.PaymentOption
import com.expensetracker.domain.model.Tag
import com.expensetracker.domain.model.TransactionType
import com.expensetracker.presentation.theme.ExpenseRed
import com.expensetracker.presentation.theme.IncomeGreen
import com.expensetracker.presentation.theme.TransferBlue
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
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
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

            // Amount Input
            AmountInputField(
                amount = uiState.amount,
                onAmountChange = viewModel::setAmount,
                type = uiState.transactionType
            )

            // Category Selector — closes on selection
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
                    onSelect = { cat ->
                        viewModel.setCategory(cat)
                        dismiss()        // ← auto-close
                    }
                )
            }

            // Payment Account Selector — closes on selection
            SelectorField(
                label = if (uiState.transactionType == TransactionType.TRANSFER) "From Account" else "Payment Account",
                value = uiState.selectedPaymentOption?.displayLabel ?: "",
                icon = Icons.Default.AccountBalance,
                placeholder = "Select Account"
            ) { dismiss ->
                PaymentOptionDropdown(
                    options = uiState.paymentOptions,
                    selected = uiState.selectedPaymentOption,
                    onSelect = { opt ->
                        viewModel.setPaymentOption(opt)
                        dismiss()        // ← auto-close
                    }
                )
            }

            // To Account (Transfer only) — closes on selection
            if (uiState.transactionType == TransactionType.TRANSFER) {
                SelectorField(
                    label = "To Account",
                    value = uiState.selectedToPaymentOption?.displayLabel ?: "",
                    icon = Icons.Default.AccountBalance,
                    placeholder = "Select Destination Account"
                ) { dismiss ->
                    PaymentOptionDropdown(
                        options = uiState.paymentOptions.filter { it.id != uiState.selectedPaymentOption?.id },
                        selected = uiState.selectedToPaymentOption,
                        onSelect = { opt ->
                            viewModel.setToPaymentOption(opt)
                            dismiss()    // ← auto-close
                        }
                    )
                }
            }

            // Date & Time Picker
            DateTimePickerRow(
                dateTime = uiState.dateTime,
                onDateTimeChange = viewModel::setDateTime,
                context = context
            )

            // ── Other Details section ─────────────────────────────────────────
            Spacer(Modifier.height(4.dp))
            Text(
                "Other details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Seamless Note field
            SeamlessNoteField(
                note = uiState.note,
                onNoteChange = viewModel::setNote
            )

            // Seamless Tags field
            SeamlessTagsSection(
                tags = uiState.tags,
                suggestions = uiState.tagSuggestions,
                onAddTag = viewModel::addTag,
                onRemoveTag = viewModel::removeTag,
                onSearchTag = viewModel::searchTags
            )

            // Attachments row
            SeamlessAttachmentRow(
                attachments = uiState.attachments,
                onAddAttachment = { fileLauncher.launch("*/*") },
                onRemoveAttachment = viewModel::removeAttachment
            )

            Spacer(Modifier.height(24.dp))

            // Save button
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
}

// ─── Transaction Type Tabs ────────────────────────────────────────────────────

@Composable
private fun TransactionTypeTabs(
    selected: TransactionType,
    onSelect: (TransactionType) -> Unit
) {
    val types = TransactionType.entries.toTypedArray()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        types.forEach { type ->
            val isSelected = type == selected
            val containerColor = when {
                isSelected && type == TransactionType.EXPENSE -> ExpenseRed
                isSelected && type == TransactionType.INCOME -> IncomeGreen
                isSelected -> TransferBlue
                else -> Color.Transparent
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(containerColor, RoundedCornerShape(8.dp))
                    .clickable { onSelect(type) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = type.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) Color.White
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
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
                "₹",
                style = MaterialTheme.typography.titleLarge,
                color = color,
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

// ─── Selector Field (category / payment account) ─────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorField(
    label: String,
    value: String,
    icon: ImageVector,
    placeholder: String,
    // dropdownContent now receives a `dismiss` lambda so items can close the menu
    dropdownContent: @Composable ColumnScope.(dismiss: () -> Unit) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            leadingIcon = { Icon(icon, null) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            placeholder = { Text(placeholder) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            shape = RoundedCornerShape(12.dp)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            // Pass dismiss = { expanded = false } so item clicks can close the menu
            dropdownContent { expanded = false }
        }
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
            Color(cat.colorHex.toColorInt().toLong() or 0xFF000000L)
        }.getOrDefault(Color.Gray)

        DropdownMenuItem(
            text = { Text(cat.name) },
            leadingIcon = {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.15f))
                ) {
                    Icon(
                        imageVector = com.expensetracker.presentation.ui.categories
                            .CategoryIcons.get(cat.icon),
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(18.dp)
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
private fun ColumnScope.PaymentOptionDropdown(
    options: List<PaymentOption>,
    selected: PaymentOption?,
    onSelect: (PaymentOption) -> Unit
) {
    if (options.isEmpty()) {
        DropdownMenuItem(
            text = { Text("No payment options added") },
            onClick = {},
            enabled = false
        )
        return
    }

    val modes = options.filterIsInstance<PaymentOption.Mode>()
    val cards = options.filterIsInstance<PaymentOption.Card>()

    if (modes.isNotEmpty()) {
        DropdownMenuItem(
            text = {
                Text(
                    "Payment Modes",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            onClick = {},
            enabled = false
        )
        modes.forEach { option ->
            val isSelected = selected is PaymentOption.Mode &&
                    selected.mode.id == option.mode.id
            DropdownMenuItem(
                text = { Text(option.displayLabel) },
                leadingIcon = {
                    Icon(
                        Icons.Default.AccountBalance,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = if (isSelected) {
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
                    "Credit Cards",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            onClick = {},
            enabled = false
        )
        cards.forEach { option ->
            val isSelected = selected is PaymentOption.Card &&
                    selected.card.id == option.card.id
            DropdownMenuItem(
                text = { Text(option.displayLabel) },
                leadingIcon = {
                    Icon(
                        Icons.Default.CreditCard,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                trailingIcon = if (isSelected) {
                    { Icon(Icons.Default.Check, null) }
                } else null,
                onClick = { onSelect(option) }
            )
        }
    }
}

// ─── Date & Time Picker ───────────────────────────────────────────────────────

@Composable
private fun DateTimePickerRow(
    dateTime: LocalDateTime,
    onDateTimeChange: (LocalDateTime) -> Unit,
    context: android.content.Context
) {
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
    val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = {
                DatePickerDialog(
                    context,
                    { _, year, month, day ->
                        onDateTimeChange(
                            dateTime.withYear(year).withMonth(month + 1).withDayOfMonth(day)
                        )
                    },
                    dateTime.year, dateTime.monthValue - 1, dateTime.dayOfMonth
                ).show()
            },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.CalendarToday, null, Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(dateTime.format(dateFormatter), style = MaterialTheme.typography.bodySmall)
        }
        OutlinedButton(
            onClick = {
                TimePickerDialog(
                    context,
                    { _, hour, minute ->
                        onDateTimeChange(dateTime.withHour(hour).withMinute(minute))
                    },
                    dateTime.hour, dateTime.minute, false
                ).show()
            },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.AccessTime, null, Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(dateTime.format(timeFormatter), style = MaterialTheme.typography.bodySmall)
        }
    }
}

// ─── Seamless Note Field ──────────────────────────────────────────────────────

@Composable
private fun SeamlessNoteField(
    note: String,
    onNoteChange: (String) -> Unit
) {
    val accentColor = MaterialTheme.colorScheme.primary
    val textColor = MaterialTheme.colorScheme.onSurface
    val hintColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // Leading icon — matches the screenshot's three-line list icon
        Icon(
            Icons.AutoMirrored.Filled.Notes,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier
                .padding(top = 14.dp)
                .size(22.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            BasicTextField(
                value = note,
                onValueChange = onNoteChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = textColor),
                cursorBrush = SolidColor(accentColor),
                maxLines = 5,
                decorationBox = { inner ->
                    if (note.isEmpty()) {
                        Text(
                            "Write a note",
                            style = MaterialTheme.typography.bodyLarge,
                            color = hintColor
                        )
                    }
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
    val accentColor = MaterialTheme.colorScheme.primary
    val textColor = MaterialTheme.colorScheme.onSurface
    val hintColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // Leading # icon
        Icon(
            Icons.Default.Tag,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier
                .padding(top = 14.dp)
                .size(22.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            // Input row
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = tagInput,
                    onValueChange = { tagInput = it; onSearchTag(it) },
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 12.dp),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = textColor),
                    cursorBrush = SolidColor(accentColor),
                    singleLine = true,
                    decorationBox = { inner ->
                        if (tagInput.isEmpty()) {
                            Text(
                                "Add tags",
                                style = MaterialTheme.typography.bodyLarge,
                                color = hintColor
                            )
                        }
                        inner()
                    }
                )
                // Add tag on Enter key / button tap
                if (tagInput.isNotBlank() && tags.size < 5) {
                    IconButton(
                        onClick = {
                            onAddTag(tagInput.trimStart('#'))
                            tagInput = ""
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add Tag",
                            modifier = Modifier.size(18.dp),
                            tint = accentColor
                        )
                    }
                }
            }
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                thickness = 0.5.dp
            )

            // Suggestions row
            if (suggestions.isNotEmpty() && tagInput.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(suggestions) { tag ->
                        SuggestionChip(
                            onClick = { onAddTag(tag.name); tagInput = "" },
                            label = { Text("#${tag.name}") }
                        )
                    }
                }
            }

            // Applied tag chips — pill style matching screenshot
            if (tags.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(tags) { tag ->
                        PillTagChip(
                            tag = tag,
                            onRemove = { onRemoveTag(tag) }
                        )
                    }
                }
            }
        }
    }
}

// ─── Pill Tag Chip (matches screenshot style) ─────────────────────────────────

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
        // Small filled circle — matches screenshot's black dot before tag name
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
        )
        Text(
            text = tag,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Icon(
            Icons.Default.Close,
            contentDescription = "Remove $tag",
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

// ─── Seamless Attachment Row ──────────────────────────────────────────────────

@Composable
private fun SeamlessAttachmentRow(
    attachments: List<Attachment>,
    onAddAttachment: () -> Unit,
    onRemoveAttachment: (Attachment) -> Unit
) {
    val accentColor = MaterialTheme.colorScheme.primary

    Column {
        // Tap-able row matching screenshot's "Add attachment >" style
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { if (attachments.size < 5) onAddAttachment() }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.AttachFile,
                contentDescription = null,
                tint = accentColor,
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
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            thickness = 0.5.dp
        )

        // Existing attachments
        if (attachments.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            attachments.forEach { att ->
                AttachmentItem(
                    attachment = att,
                    onRemove = { onRemoveAttachment(att) }
                )
            }
        }
    }
}

// ─── Attachment Item ──────────────────────────────────────────────────────────

@Composable
private fun AttachmentItem(attachment: Attachment, onRemove: () -> Unit) {
    val icon = when {
        attachment.mimeType.contains("pdf") -> Icons.Default.PictureAsPdf
        attachment.mimeType.contains("image") -> Icons.Default.Image
        else -> Icons.Default.Description
    }
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, Modifier.size(24.dp))
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    attachment.fileName,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    formatFileSize(attachment.fileSizeBytes),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Close, null, Modifier.size(16.dp))
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String = when {
    bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576f)
    bytes >= 1024 -> "%.1f KB".format(bytes / 1024f)
    else -> "$bytes B"
}