package com.expensetracker.presentation.ui.transactions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.expensetracker.domain.model.ScheduledFrequency
import com.expensetracker.domain.model.ScheduledTransaction
import com.expensetracker.domain.model.TransactionType
import com.expensetracker.presentation.components.CategoryIconBubble
import com.expensetracker.presentation.components.EmptyState
import com.expensetracker.presentation.components.LocalCurrencyFormat
import com.expensetracker.presentation.components.LocalCurrencySymbol
import com.expensetracker.presentation.theme.ExpenseRed
import com.expensetracker.presentation.theme.IncomeGreen
import com.expensetracker.presentation.theme.TransferBlue
import com.expensetracker.util.FormatUtils.formatAmountForDisplay
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduledTransactionsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAdd: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    viewModel: ScheduledTransactionsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showFilterSheet by remember { mutableStateOf(false) }
    val currencySymbol = LocalCurrencySymbol.current
    val currencyFormat = LocalCurrencyFormat.current

    val hasFilters = uiState.statusFilter != ScheduleStatusFilter.ALL ||
            uiState.typeFilter != null || uiState.frequencyFilter != null

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAdd,
                containerColor = MaterialTheme.colorScheme.onBackground,
                contentColor = MaterialTheme.colorScheme.background
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Schedule")
            }
        },
        topBar = {
            Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        onClick = onNavigateBack
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        "Scheduled",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    CircularButton(
                        icon = Icons.Default.FilterList,
                        contentDescription = "Filter",
                        onClick = { showFilterSheet = true }
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            // ── Active filter chips ───────────────────────────────────────────
            AnimatedVisibility(
                visible = hasFilters,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = true,
                            onClick = viewModel::clearFilters,
                            label = { Text("Clear All") },
                            trailingIcon = {
                                Icon(Icons.Default.Close, null, Modifier.size(14.dp))
                            }
                        )
                    }
                    if (uiState.statusFilter != ScheduleStatusFilter.ALL) {
                        item {
                            FilterChip(
                                selected = true, onClick = {},
                                label = {
                                    Text(
                                        uiState.statusFilter.name.lowercase()
                                            .replaceFirstChar { it.uppercase() })
                                }
                            )
                        }
                    }
                    uiState.typeFilter?.let { it ->
                        item {
                            FilterChip(
                                selected = true, onClick = {},
                                label = {
                                    Text(
                                        it.name.lowercase()
                                            .replaceFirstChar { it.uppercase() })
                                }
                            )
                        }
                    }
                    uiState.frequencyFilter?.let {
                        item {
                            FilterChip(
                                selected = true, onClick = {},
                                label = { Text(it.displayName()) }
                            )
                        }
                    }
                }
            }

            if (uiState.filteredSchedules.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        icon = Icons.Default.Schedule,
                        title = "No scheduled transactions",
                        subtitle = if (hasFilters) "Try clearing the filters"
                        else "Tap + to set one up"
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 16.dp, end = 16.dp,
                        top = 12.dp, bottom = 100.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.filteredSchedules, key = { it.id }) { schedule ->
                        val category = viewModel.categoryFor(schedule)
                        ScheduledTransactionCard(
                            schedule = schedule,
                            categoryIcon = category?.icon ?: "category",
                            categoryColor = category?.colorHex ?: "#6750A4",
                            currencySymbol = currencySymbol,
                            currencyFormat = currencyFormat,
                            onTap = { viewModel.selectSchedule(schedule.id) },
                            onToggleActive = { viewModel.toggleActive(schedule) }
                        )
                    }
                }
            }
        }
    }

    // ── Filter bottom sheet ───────────────────────────────────────────────────
    if (showFilterSheet) {
        ScheduleFilterSheet(
            statusFilter = uiState.statusFilter,
            typeFilter = uiState.typeFilter,
            frequencyFilter = uiState.frequencyFilter,
            onStatusChange = viewModel::setStatusFilter,
            onTypeChange = viewModel::setTypeFilter,
            onFrequencyChange = viewModel::setFrequencyFilter,
            onReset = { viewModel.clearFilters(); showFilterSheet = false },
            onDismiss = { showFilterSheet = false }
        )
    }

    // ── Schedule detail / action bottom sheet ────────────────────────────────
    uiState.selectedScheduleId?.let { selectedId ->
        val schedule = uiState.schedules.find { it.id == selectedId }
        if (schedule != null) {
            val category = viewModel.categoryFor(schedule)
            val mode = viewModel.modeFor(schedule)
            ScheduleDetailSheet(
                schedule = schedule,
                categoryName = category?.name ?: "—",
                categoryIcon = category?.icon ?: "category",
                categoryColor = category?.colorHex ?: "#6750A4",
                modeName = mode?.displayLabel ?: "—",
                currencySymbol = currencySymbol,
                currencyFormat = currencyFormat,
                onEdit = {
                    viewModel.clearSelection()
                    onNavigateToEdit(schedule.id)
                },
                onToggleActive = {
                    viewModel.toggleActive(schedule)
                    viewModel.clearSelection()
                },
                onDelete = { viewModel.deleteSchedule(schedule) },
                onDismiss = viewModel::clearSelection
            )
        }
    }
}

// ─── Scheduled transaction card (list item) ───────────────────────────────────

@Composable
private fun ScheduledTransactionCard(
    schedule: ScheduledTransaction,
    categoryIcon: String,
    categoryColor: String,
    currencySymbol: String,
    currencyFormat: String,
    onTap: () -> Unit,
    onToggleActive: () -> Unit
) {
    val amountColor = when (schedule.type) {
        TransactionType.INCOME -> IncomeGreen
        TransactionType.EXPENSE -> ExpenseRed
        TransactionType.TRANSFER -> TransferBlue
    }
    val amountPrefix = when (schedule.type) {
        TransactionType.INCOME -> "+"
        TransactionType.EXPENSE -> "-"
        TransactionType.TRANSFER -> ""
    }
    val dtFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault())
    val nextLabel = "Next: ${schedule.nextRunAt.format(dtFormatter)}"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTap() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .alpha(if (schedule.isActive) 1f else 0.5f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category icon
            CategoryIconBubble(
                iconKey = categoryIcon,
                colorHex = categoryColor,
                size = 44
            )
            Spacer(Modifier.width(12.dp))

            // Center: note + frequency + next run
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = schedule.note.ifEmpty { "Scheduled ${schedule.type.name.lowercase()}" },
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (!schedule.isActive) TextDecoration.LineThrough else null
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Repeat, null,
                        Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        schedule.frequency.displayName(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = if (schedule.isActive) nextLabel else "Inactive",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (schedule.isActive) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
            }

            Spacer(Modifier.width(8.dp))

            // Right: amount + toggle switch
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$amountPrefix$currencySymbol${
                        formatAmountForDisplay(abs(schedule.amount), currencyFormat)
                    }",
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = amountColor
                )
                Spacer(Modifier.height(4.dp))
                Switch(
                    checked = schedule.isActive,
                    onCheckedChange = { onToggleActive() },
                    modifier = Modifier.size(width = 44.dp, height = 24.dp)
                )
            }
        }
    }
}

// ─── Schedule detail / action bottom sheet ────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleDetailSheet(
    schedule: ScheduledTransaction,
    categoryName: String,
    categoryIcon: String,
    categoryColor: String,
    modeName: String,
    currencySymbol: String,
    currencyFormat: String,
    onEdit: () -> Unit,
    onToggleActive: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val dtFmt = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.getDefault())
    val amountColor = when (schedule.type) {
        TransactionType.INCOME -> IncomeGreen
        TransactionType.EXPENSE -> ExpenseRed
        TransactionType.TRANSFER -> TransferBlue
    }
    val amountPrefix = when (schedule.type) {
        TransactionType.INCOME -> "+"
        TransactionType.EXPENSE -> "-"
        TransactionType.TRANSFER -> ""
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // ── Header ────────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: icon + title/category — takes all remaining space
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CategoryIconBubble(
                        iconKey = categoryIcon,
                        colorHex = categoryColor,
                        size = 48
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            schedule.note.ifEmpty {
                                "Scheduled ${schedule.type.name.lowercase()}"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            categoryName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                // Right: amount — fixed width, never wraps
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "$amountPrefix$currencySymbol${
                        formatAmountForDisplay(abs(schedule.amount), currencyFormat)
                    }",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = amountColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(Modifier.height(12.dp))

            // ── Detail rows ───────────────────────────────────────────────────
            DetailRow(label = "Frequency", value = schedule.frequency.displayName())
            DetailRow(label = "Next run", value = schedule.nextRunAt.format(dtFmt))
            DetailRow(label = "Payment mode", value = modeName)
            if (schedule.note.isNotEmpty()) {
                DetailRow(label = "Note", value = schedule.note)
            }
            if (schedule.reminderMinutes > 0L) {
                DetailRow(
                    label = "Reminder",
                    value = reminderLabel(schedule.reminderMinutes),
                    icon = Icons.Default.NotificationsNone
                )
            }
            schedule.lastGeneratedAt?.let {
                DetailRow(label = "Last run", value = it.format(dtFmt))
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(Modifier.height(12.dp))

            // ── Status toggle ─────────────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        if (schedule.isActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                        null,
                        tint = if (schedule.isActive) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        if (schedule.isActive) "Active — tap to pause" else "Paused — tap to resume",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Switch(
                    checked = schedule.isActive,
                    onCheckedChange = { onToggleActive() }
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Action buttons ────────────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Delete, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Delete")
                }
                Button(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Edit, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Edit")
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Schedule?") },
            text = {
                Text(
                    "This will permanently delete the scheduled transaction" +
                            if (schedule.note.isNotEmpty()) " \"${schedule.note}\"" else "" +
                                    ". Past transactions already generated will not be removed."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { showDeleteDialog = false; onDelete() }
                ) {
                    Text(
                        "Delete", color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// ─── Filter bottom sheet ──────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleFilterSheet(
    statusFilter: ScheduleStatusFilter,
    typeFilter: TransactionType?,
    frequencyFilter: ScheduledFrequency?,
    onStatusChange: (ScheduleStatusFilter) -> Unit,
    onTypeChange: (TransactionType?) -> Unit,
    onFrequencyChange: (ScheduledFrequency?) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .navigationBarsPadding()
        ) {
            Text(
                "Filter Schedules",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))

            // Status
            Text("Status", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ScheduleStatusFilter.entries.forEach { s ->
                    FilterChip(
                        selected = s == statusFilter,
                        onClick = { onStatusChange(s) },
                        label = { Text(s.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Type
            Text("Type", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = typeFilter == null, onClick = { onTypeChange(null) },
                        label = { Text("All") }
                    )
                }
                items(TransactionType.entries) { t ->
                    FilterChip(
                        selected = t == typeFilter,
                        onClick = { onTypeChange(if (t == typeFilter) null else t) },
                        label = { Text(t.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Frequency
            Text("Frequency", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = frequencyFilter == null,
                        onClick = { onFrequencyChange(null) },
                        label = { Text("All") }
                    )
                }
                items(ScheduledFrequency.entries) { f ->
                    FilterChip(
                        selected = f == frequencyFilter,
                        onClick = { onFrequencyChange(if (f == frequencyFilter) null else f) },
                        label = { Text(f.displayName()) }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onReset,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Reset") }
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("Done") }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ─── Shared small helpers ─────────────────────────────────────────────────────

@Composable
private fun CircularButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(36.dp)
            .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape)
    ) {
        Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    icon: ImageVector? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Icon(
                    icon, null, Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 24.dp)
        )
    }
}

private fun reminderLabel(minutes: Long): String = when {
    minutes <= 0L -> "None"
    minutes < 60L -> "$minutes minute${if (minutes == 1L) "" else "s"} before"
    minutes < 1440L -> {
        val h = minutes / 60; "$h hour${if (h == 1L) "" else "s"} before"
    }

    minutes == 1440L -> "1 day before"
    else -> {
        val d = minutes / 1440; "$d day${if (d == 1L) "" else "s"} before"
    }
}