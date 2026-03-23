package com.expensetracker.presentation.ui.addtransaction

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.expensetracker.domain.model.*
import com.expensetracker.presentation.components.TagChip
import com.expensetracker.presentation.theme.*
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
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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

            TransactionTypeTabs(
                selected = uiState.transactionType,
                onSelect = viewModel::setTransactionType
            )

            AmountInputField(
                amount = uiState.amount,
                onAmountChange = viewModel::setAmount,
                type = uiState.transactionType
            )

            SelectorField(
                label = "Category",
                value = uiState.selectedCategory?.name ?: "",
                icon = Icons.Default.Category,
                placeholder = "Select Category",
                onClick = {}
            ) {
                CategoryDropdown(
                    categories = uiState.categories.filter {
                        it.transactionType == uiState.transactionType || it.transactionType == null
                    },
                    selected = uiState.selectedCategory,
                    onSelect = viewModel::setCategory
                )
            }

            SelectorField(
                label = if (uiState.transactionType == TransactionType.TRANSFER)
                    "From Account" else "Payment Account",
                value = uiState.selectedPaymentOption?.displayLabel ?: "",
                icon = Icons.Default.AccountBalance,
                placeholder = "Select Account",
                onClick = {}
            ) {
                PaymentOptionDropdown(
                    options = uiState.paymentOptions,
                    selected = uiState.selectedPaymentOption,
                    onSelect = viewModel::setPaymentOption
                )
            }

            if (uiState.transactionType == TransactionType.TRANSFER) {
                SelectorField(
                    label = "To Account",
                    value = uiState.selectedToPaymentOption?.displayLabel ?: "",
                    icon = Icons.Default.AccountBalance,
                    placeholder = "Select Destination Account",
                    onClick = {}
                ) {
                    PaymentOptionDropdown(
                        options = uiState.paymentOptions.filter {
                            it.id != uiState.selectedPaymentOption?.id
                        },
                        selected = uiState.selectedToPaymentOption,
                        onSelect = viewModel::setToPaymentOption
                    )
                }
            }

            DateTimePickerRow(
                dateTime = uiState.dateTime,
                onDateTimeChange = viewModel::setDateTime,
                context = context
            )

            OutlinedTextField(
                value = uiState.note,
                onValueChange = viewModel::setNote,
                label = { Text("Note (optional)") },
                leadingIcon = { Icon(Icons.Default.Notes, null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                maxLines = 3
            )

            TagsSection(
                tags = uiState.tags,
                suggestions = uiState.tagSuggestions,
                onAddTag = viewModel::addTag,
                onRemoveTag = viewModel::removeTag,
                onSearchTag = viewModel::searchTags
            )

            AttachmentsSection(
                attachments = uiState.attachments,
                onAddAttachment = { fileLauncher.launch("*/*") },
                onRemoveAttachment = viewModel::removeAttachment
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = viewModel::saveTransaction,
                modifier = Modifier.fillMaxWidth().height(56.dp),
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
                isSelected && type == TransactionType.EXPENSE  -> ExpenseRed
                isSelected && type == TransactionType.INCOME   -> IncomeGreen
                isSelected                                     -> TransferBlue
                else                                           -> Color.Transparent
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

@Composable
private fun AmountInputField(
    amount: String,
    onAmountChange: (String) -> Unit,
    type: TransactionType
) {
    val color = when (type) {
        TransactionType.INCOME   -> IncomeGreen
        TransactionType.EXPENSE  -> ExpenseRed
        TransactionType.TRANSFER -> TransferBlue
    }
    OutlinedTextField(
        value = amount,
        onValueChange = {
            if (it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) onAmountChange(it)
        },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorField(
    label: String,
    value: String,
    icon: ImageVector,
    placeholder: String,
    onClick: () -> Unit,
    dropdownContent: @Composable ColumnScope.() -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
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
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
            shape = RoundedCornerShape(12.dp)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            dropdownContent()
        }
    }
}

@Composable
private fun ColumnScope.CategoryDropdown(
    categories: List<Category>,
    selected: Category?,
    onSelect: (Category) -> Unit
) {
    categories.forEach { cat ->
        val iconColor = runCatching {
            Color(android.graphics.Color.parseColor(cat.colorHex))
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

    // ── Payment Modes group ───────────────────────────────────────────────────
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

    // ── Credit Cards group ────────────────────────────────────────────────────
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

@Composable
private fun TagsSection(
    tags: List<String>,
    suggestions: List<Tag>,
    onAddTag: (String) -> Unit,
    onRemoveTag: (String) -> Unit,
    onSearchTag: (String) -> Unit
) {
    var tagInput by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Tags (max 5)", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = tagInput,
                onValueChange = { tagInput = it; onSearchTag(it) },
                placeholder = { Text("#tag") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            IconButton(
                onClick = {
                    if (tagInput.isNotBlank()) { onAddTag(tagInput); tagInput = "" }
                },
                enabled = tags.size < 5 && tagInput.isNotBlank()
            ) {
                Icon(Icons.Default.Add, "Add Tag")
            }
        }
        if (suggestions.isNotEmpty() && tagInput.isNotBlank()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(suggestions) { tag ->
                    SuggestionChip(
                        onClick = { onAddTag(tag.name); tagInput = "" },
                        label = { Text("#${tag.name}") }
                    )
                }
            }
        }
        if (tags.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(tags) { tag ->
                    TagChip(tag = tag, onRemove = { onRemoveTag(tag) })
                }
            }
        }
    }
}

@Composable
private fun AttachmentsSection(
    attachments: List<Attachment>,
    onAddAttachment: () -> Unit,
    onRemoveAttachment: (Attachment) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Attachments (max 5)", style = MaterialTheme.typography.labelLarge)
            if (attachments.size < 5) {
                TextButton(onClick = onAddAttachment) {
                    Icon(Icons.Default.AttachFile, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add")
                }
            }
        }
        attachments.forEach { att ->
            AttachmentItem(attachment = att, onRemove = { onRemoveAttachment(att) })
        }
    }
}

@Composable
private fun AttachmentItem(attachment: Attachment, onRemove: () -> Unit) {
    val icon = when {
        attachment.mimeType.contains("pdf")   -> Icons.Default.PictureAsPdf
        attachment.mimeType.contains("image") -> Icons.Default.Image
        else                                   -> Icons.Default.Description
    }
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
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
    bytes >= 1_024     -> "%.1f KB".format(bytes / 1_024f)
    else               -> "$bytes B"
}