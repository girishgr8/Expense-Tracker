package com.expensetracker.presentation.ui.settings

import android.app.TimePickerDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CalendarViewDay
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ModeEdit
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import com.expensetracker.domain.model.ExportFormat
import com.expensetracker.presentation.components.AppBottomBar
import com.expensetracker.presentation.ui.export.ExportFormatBottomSheet
import com.expensetracker.presentation.ui.export.ExportOptionsBottomSheet
import com.expensetracker.presentation.ui.export.ExportSuccessBottomSheet
import com.expensetracker.presentation.ui.export.ExportViewModel
import com.expensetracker.util.HapticManager
import com.expensetracker.util.LocalHapticManager
import java.time.LocalDate
import java.util.Locale

// ─── Quick Actions ────────────────────────────────────────────────────────────

@Composable
private fun QuickActionsGrid(
    onDayView: (LocalDate) -> Unit,
    onCalendarView: () -> Unit,
    onCustomView: () -> Unit,
) {
    val actions = listOf(
        QuickAction(
            Icons.Default.CalendarViewDay, "Day",
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        ) { onDayView(LocalDate.now()) },
        QuickAction(
            Icons.Default.CalendarMonth, "Calendar",
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer, onCalendarView
        ),
        QuickAction(
            Icons.Default.Tune, "Custom",
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer, onCustomView
        ),
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        actions.forEach { action ->
            QuickActionTile(action = action, modifier = Modifier.weight(1f))
        }
    }
}

private data class QuickAction(
    val icon: ImageVector,
    val label: String,
    val containerColor: Color,
    val contentColor: Color,
    val onClick: () -> Unit
)

@Composable
private fun UserProfileCard(
    userName: String,
    userEmail: String,
    userPhotoUrl: String,
    lastBackupDisplay: String,
    onBackupNow: () -> Unit,
    onNameChange: (String) -> Unit
) {
    var showEditNameDialog by remember { mutableStateOf(false) }
    var editedName by remember(userName) { mutableStateOf(userName) }
    val displayName = userName.ifBlank { "User" }
    val initial = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "U"
    val backupText = if (lastBackupDisplay == "Never") {
        "Last backup: Never"
    } else {
        "Last backup: Automatic G-Drive backup on $lastBackupDisplay."
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 15.dp, end = 15.dp, top = 10.dp, bottom = 5.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (userPhotoUrl.isNotEmpty()) {
                    AsyncImage(
                        model = userPhotoUrl,
                        contentDescription = "Profile photo",
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF039BE5)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            initial,
                            style = MaterialTheme.typography.headlineLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(Modifier.width(16.dp))

                Column(Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            displayName,
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 19.sp),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable {
                                    editedName = displayName
                                    showEditNameDialog = true
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Default.ModeEdit,
                                contentDescription = "Edit name",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                    Text(
                        userEmail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Light,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.width(14.dp))

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = "Account security",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(top = 10.dp, bottom = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    backupText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Light
                )
                Spacer(Modifier.width(16.dp))
                TextButton(onClick = onBackupNow) {
                    Text(
                        "Backup now",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }

    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text("Edit name") },
            text = {
                OutlinedTextField(
                    value = editedName,
                    onValueChange = { editedName = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    enabled = editedName.isNotBlank(),
                    onClick = {
                        onNameChange(editedName)
                        showEditNameDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private data class SettingsShortcut(
    val icon: ImageVector,
    val title: String,
    val iconColor: Color,
    val onClick: () -> Unit
)

@Composable
private fun SettingsShortcutGrid(
    onTransactions: () -> Unit,
    onScheduledTransactions: () -> Unit,
    onBudgets: () -> Unit,
    onCategories: () -> Unit,
    onTags: () -> Unit,
    onDebts: () -> Unit
) {
    val shortcuts = listOf(
        SettingsShortcut(Icons.Default.CalendarViewDay, "Transactions", Color(0xFF2196F3), onTransactions),
        SettingsShortcut(Icons.Default.CalendarMonth, "Scheduled Txns", Color(0xFF00BCD4), onScheduledTransactions),
        SettingsShortcut(Icons.Default.Wallet, "Budgets", Color(0xFFFF4081), onBudgets),
        SettingsShortcut(Icons.Default.Category, "Categories", Color(0xFF00E676), onCategories),
        SettingsShortcut(Icons.Default.Numbers, "Tags", Color(0xFF4DD0E1), onTags),
        SettingsShortcut(Icons.Default.Sync, "Debts", Color(0xFFFFB300), onDebts)
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        shortcuts.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowItems.forEach { shortcut ->
                    SettingsShortcutCard(
                        shortcut = shortcut,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowItems.size == 1) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SettingsShortcutCard(
    shortcut: SettingsShortcut,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(60.dp)
            .clickable { shortcut.onClick() },
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = shortcut.icon,
                contentDescription = shortcut.title,
                tint = shortcut.iconColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = shortcut.title,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun QuickActionTile(action: QuickAction, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(action.containerColor)
            .clickable { action.onClick() }
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            action.icon, contentDescription = action.label,
            tint = action.contentColor,
            modifier = Modifier.size(24.dp)
        )
        Text(
            action.label,
            style = MaterialTheme.typography.labelSmall,
            color = action.contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToHome: () -> Unit = {},
    onNavigateToAnalysis: () -> Unit = {},
    onNavigateToAccounts: () -> Unit = {},
    onNavigateToAddTransaction: () -> Unit = {},
    onNavigateToTransactions: () -> Unit = {},
    onNavigateToScheduledTransactions: () -> Unit = {},
    onNavigateToBudgets: () -> Unit = {},
    onNavigateToCategories: () -> Unit = {},
    onNavigateToTags: () -> Unit = {},
    onNavigateToDebts: () -> Unit = {},
    onNavigateToCurrency: () -> Unit = {},
    onNavigateToCalendarView: () -> Unit = {},
    onNavigateToDayView: (LocalDate) -> Unit = {},
    onLogout: () -> Unit,
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    exportViewModel: ExportViewModel = hiltViewModel()
) {
    val uiState by settingsViewModel.uiState.collectAsState()
    val exportResult by exportViewModel.exportResult.collectAsState()
    val filters by exportViewModel.filters.collectAsState()
    val context = LocalContext.current
    val hapticManager = LocalHapticManager.current
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showFormatSheet by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> uri?.let(settingsViewModel::importTransactionsFromCsv) }

    LaunchedEffect(Unit) {
        exportViewModel.loadFilters()
    }

    LaunchedEffect(uiState.importTransactionsMessage) {
        uiState.importTransactionsMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            settingsViewModel.clearImportTransactionsMessage()
        }
    }

    if (showTimePicker) {
        val context = LocalContext.current

        DisposableEffect(Unit) {
            val timePickerDialog = TimePickerDialog(
                context, { _, hour, minute ->
                    settingsViewModel.setBudgetReminderTime(hour, minute)
                }, uiState.dailyReminderHour, uiState.dailyReminderMinute, false
            )

            timePickerDialog.setOnDismissListener {
                showTimePicker = false
            }

            timePickerDialog.show()

            onDispose { timePickerDialog.dismiss() }
        }
    }

    if (showFilterSheet) {
        ExportOptionsBottomSheet(
            options = filters,
            onDismiss = { showFilterSheet = false },
            onOptionSelected = { filter ->
                exportViewModel.setFilter(filter)
                showFilterSheet = false
                showFormatSheet = true
            })
    }

    if (showFormatSheet) {
        ExportFormatBottomSheet(
            onDismiss = { showFormatSheet = false },
            onFormatSelected = { format ->
                exportViewModel.export(
                    context, uiState.userName, uiState.userEmail, format == ExportFormat.PDF
                )
                showFormatSheet = false
            })
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                AppBottomBar(
                    currentRoute = "settings",
                    onHome = onNavigateToHome,
                    onAnalysis = onNavigateToAnalysis,
                    onAccounts = onNavigateToAccounts,
                    onSettings = {},
                    onAddTransaction = onNavigateToAddTransaction
                )
            }, topBar = {
                TopAppBar(
                    title = { Text("Settings", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    })
            }) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // User Profile Card
                item {
                    UserProfileCard(
                        userName = uiState.userName,
                        userEmail = uiState.userEmail,
                        userPhotoUrl = uiState.userPhotoUrl,
                        lastBackupDisplay = uiState.lastBackupDisplay,
                        onBackupNow = settingsViewModel::triggerBackupNow,
                        onNameChange = settingsViewModel::updateUserName
                    )
                }

                item {
                    Spacer(Modifier.height(6.dp))
                    SettingsShortcutGrid(
                        onTransactions = onNavigateToTransactions,
                        onScheduledTransactions = onNavigateToScheduledTransactions,
                        onBudgets = onNavigateToBudgets,
                        onCategories = onNavigateToCategories,
                        onTags = onNavigateToTags,
                        onDebts = onNavigateToDebts
                    )
                }

                // Views Section
                item { SettingsSectionHeader("Views") }

                item {
                    QuickActionsGrid(
                        onDayView = onNavigateToDayView,
                        onCalendarView = onNavigateToCalendarView,
                        onCustomView = onNavigateToCalendarView
                    )
                }

                // Appearance Section
                item { SettingsSectionHeader("Appearance") }

                item {
                    // Decimal Format
                    var showDecimalSheet by remember { mutableStateOf(false) }

                    SettingsCard {
                        // Theme mode
                        SettingsItem(
                            icon = Icons.Default.DarkMode,
                            title = "Theme",
                            subtitle = uiState.themeMode.replaceFirstChar { it.uppercase() }) {
                            var expanded by remember { mutableStateOf(false) }
                            Box {
                                TextButton(onClick = { expanded = true }) {
                                    Text(uiState.themeMode.replaceFirstChar { it.uppercase() })
                                }
                                DropdownMenu(
                                    expanded = expanded, onDismissRequest = { expanded = false }) {
                                    listOf("system", "light", "dark").forEach { mode ->
                                        DropdownMenuItem(
                                            text = { Text(mode.replaceFirstChar { it.uppercase() }) },
                                            onClick = {
                                                settingsViewModel.setThemeMode(mode); expanded =
                                                false
                                            },
                                            trailingIcon = if (mode == uiState.themeMode) {
                                                { Icon(Icons.Default.Check, null) }
                                            } else null)
                                    }
                                }
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        // Dynamic color
                        SettingsItem(
                            icon = Icons.Default.Palette,
                            title = "Dynamic Color",
                            subtitle = "Use Material You colors from wallpaper"
                        ) {
                            Switch(
                                checked = uiState.useDynamicColor,
                                onCheckedChange = settingsViewModel::setDynamicColor
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        // Decimal Format
                        val decimalLabel = when (uiState.decimalFormat) {
                            "none" -> "No Decimal Places"
                            "one" -> "1 Decimal Place"
                            "two" -> "2 Decimal Places"
                            else -> "Default"
                        }
                        SettingsItem(
                            icon = Icons.Default.Numbers,
                            title = "Decimal Format",
                            subtitle = decimalLabel
                        ) {
                            IconButton(onClick = { showDecimalSheet = true }) {
                                Icon(Icons.Default.ChevronRight, null)
                            }
                        }

                        // ── Decimal Format picker sheet ─────────────────────
                        if (showDecimalSheet) {
                            DecimalFormatSheet(current = uiState.decimalFormat, onSelect = { fmt ->
                                settingsViewModel.setDecimalFormat(fmt)
                                showDecimalSheet = false
                            }, onDismiss = { showDecimalSheet = false })
                        }
                    }
                }

                item { SettingsSectionHeader("Preferences") }

                item {
                    var showFirstDaySheet by remember { mutableStateOf(false) }
                    var showCategorySheet by remember { mutableStateOf(false) }
                    var showPaymentModeSheet by remember { mutableStateOf(false) }

                    val defaultCategory =
                        uiState.categories.find { it.id == uiState.defaultCategoryId }
                    val defaultMode =
                        uiState.paymentModes.find { it.id == uiState.defaultPaymentModeId }

                    SettingsCard {
                        // Currency & Format
                        SettingsItem(
                            icon = Icons.Default.CurrencyRupee,
                            title = "Currency & Format",
                            subtitle = "${uiState.currencyCode} (${uiState.currencySymbol})"
                        ) {
                            IconButton(onClick = onNavigateToCurrency) {
                                Icon(Icons.Default.ChevronRight, null)
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        // Default Payment Mode
                        SettingsItem(
                            icon = Icons.Default.AccountBalanceWallet,
                            title = "Default Payment Mode",
                            subtitle = defaultMode?.displayLabel ?: "None selected"
                        ) {
                            IconButton(onClick = { showPaymentModeSheet = true }) {
                                Icon(Icons.Default.ChevronRight, null)
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        // Default Category
                        SettingsItem(
                            icon = Icons.Default.Category,
                            title = "Default Category",
                            subtitle = defaultCategory?.name ?: "None selected"
                        ) {
                            IconButton(onClick = { showCategorySheet = true }) {
                                Icon(Icons.Default.ChevronRight, null)
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        // First Day of Month
                        val ordinal = when (uiState.firstDayOfMonth) {
                            1 -> "1st"; 2 -> "2nd"; 3 -> "3rd"
                            else -> "${uiState.firstDayOfMonth}th"
                        }
                        SettingsItem(
                            icon = Icons.Default.CalendarToday,
                            title = "First Day of the Month",
                            subtitle = ordinal
                        ) {
                            IconButton(onClick = { showFirstDaySheet = true }) {
                                Icon(Icons.Default.ChevronRight, null)
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        // Haptics
                        SettingsItem(
                            icon = Icons.Default.TouchApp,
                            title = "Haptics",
                            subtitle = "Subtle taps when you interact with the app"
                        ) {
                            Switch(
                                checked = uiState.isHapticsEnabled,
                                onCheckedChange = settingsViewModel::setIsHapticsEnabled,
                                modifier = Modifier
                            )
                        }
                    }

                    // ── First Day of Month bottom sheet ───────────────────────
                    if (showFirstDaySheet) {
                        FirstDayOfMonthSheet(current = uiState.firstDayOfMonth, onSelect = { day ->
                            settingsViewModel.setFirstDayOfMonth(day)
                            showFirstDaySheet = false
                        }, onDismiss = { showFirstDaySheet = false })
                    }

                    // ── Default Category picker sheet ─────────────────────────
                    if (showCategorySheet) {
                        SelectionSheet(
                            title = "Select Default Category",
                            items = uiState.categories,
                            selectedId = uiState.defaultCategoryId,
                            itemLabel = { it.name },
                            onSelect = { cat ->
                                settingsViewModel.setDefaultCategoryId(cat.id)
                                showCategorySheet = false
                            },
                            onClear = {
                                settingsViewModel.setDefaultCategoryId(-1L)
                                showCategorySheet = false
                            },
                            onDismiss = { showCategorySheet = false })
                    }

                    // ── Default Payment Mode picker sheet ─────────────────────
                    if (showPaymentModeSheet) {
                        SelectionSheet(
                            title = "Select Default Payment Mode",
                            items = uiState.paymentModes,
                            selectedId = uiState.defaultPaymentModeId,
                            itemLabel = { it.displayLabel },
                            onSelect = { mode ->
                                settingsViewModel.setDefaultPaymentModeId(mode.id)
                                showPaymentModeSheet = false
                            },
                            onClear = {
                                settingsViewModel.setDefaultPaymentModeId(-1L)
                                showPaymentModeSheet = false
                            },
                            onDismiss = { showPaymentModeSheet = false })
                    }
                }

                item { SettingsSectionHeader("Notifications") }

                item {
                    SettingsCard {
                        DailyReminderSettingItem(
                            isEnabled = uiState.isDailyReminderEnabled,
                            time = String.format(
                                Locale.getDefault(),
                                "%02d:%02d",
                                uiState.dailyReminderHour,
                                uiState.dailyReminderMinute
                            ),
                            onToggle = settingsViewModel::setIsDailyReminderEnabled,
                            onTimeClick = { showTimePicker = true },
                            hapticManager = hapticManager
                        )

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        val budgetAlertIcon = if (uiState.isBudgetAlertEnabled) {
                            Icons.Default.NotificationsActive
                        } else {
                            Icons.Default.NotificationsOff
                        }

                        SettingsItem(
                            icon = budgetAlertIcon,
                            title = "Budget Alerts",
                            subtitle = "Notify me when I go off-track with my budget"
                        ) {
                            Switch(
                                checked = uiState.isBudgetAlertEnabled,
                                onCheckedChange = { isEnabled ->
                                    if (isEnabled) {
                                        hapticManager.perform(HapticFeedbackType.ToggleOn)
                                    } else {
                                        hapticManager.perform(HapticFeedbackType.ToggleOff)
                                    }
                                    settingsViewModel.setIsBudgetAlertEnabled(isEnabled)
                                })
                        }
                    }
                }

                // Data Section
                item { SettingsSectionHeader("Backup, Restore & Export") }

                item {
                    SettingsCard {
                        SettingsItem(
                            icon = Icons.Default.CloudUpload,
                            title = "Auto Google Drive Backup",
                            subtitle = "Back up monthly to Google Drive"
                        ) {
                            Switch(
                                checked = uiState.isBackupEnabled,
                                onCheckedChange = settingsViewModel::setIsBackupEnabled
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        SettingsItem(
                            icon = Icons.Default.Backup,
                            title = "Back Up Now",
                            subtitle = "Last backup: ${uiState.lastBackupDisplay}"
                        ) {
                            IconButton(onClick = settingsViewModel::triggerBackupNow) {
                                Icon(Icons.Default.Sync, contentDescription = "Backup Now")
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        SettingsItem(
                            icon = Icons.Default.Upload,
                            title = "Export Transactions",
                            subtitle = "Export data to a spreadsheet(.csv) or a PDF",
                        ) {
                            IconButton(onClick = { showFilterSheet = true }) {
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = "Export Transactions"
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        SettingsItem(
                            icon = Icons.Default.Download,
                            title = "Import Transactions",
                            subtitle = "Import data from a spreadsheet(.csv)"
                        ) {
                            if (uiState.isImportingTransactions) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                            } else {
                                IconButton(onClick = { importLauncher.launch("text/*") }) {
                                    Icon(
                                        Icons.Default.ChevronRight,
                                        contentDescription = "Import Transactions"
                                    )
                                }
                            }
                        }
                    }
                }

                // Account Section
                item { SettingsSectionHeader("Account") }

                item {
                    SettingsCard {
                        SettingsItem(
                            icon = Icons.AutoMirrored.Filled.Logout,
                            title = "Sign Out",
                            subtitle = "Sign out of your Google account",
                            titleColor = MaterialTheme.colorScheme.error
                        ) {
                            IconButton(onClick = { showLogoutDialog = true }) {
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = "Sign Out",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(32.dp)) }
            }
        }
        if (exportResult.isSuccess && exportResult.uri != null) {
            ExportSuccessBottomSheet(
                uri = exportResult.uri!!, onDismiss = { exportViewModel.resetExportState() })
        }

        if (exportViewModel.isExporting) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Sign Out") },
            text = { Text("Are you sure you want to sign out? Your data will remain on this device.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        settingsViewModel.logout()
                        onLogout()
                    }) {
                    Text("Sign Out", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Cancel") }
            })
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth(), content = content)
    }
}

@Composable
fun DailyReminderSettingItem(
    isEnabled: Boolean,
    time: String,
    hapticManager: HapticManager,
    onToggle: (Boolean) -> Unit,
    onTimeClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = isEnabled, // 🚨 only clickable if enabled
                onClick = { onTimeClick() })
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        val dailyReminderIcon = if (isEnabled) {
            Icons.Default.NotificationsActive
        } else {
            Icons.Default.NotificationsOff
        }

        // Icon
        Icon(
            imageVector = dailyReminderIcon,
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {

            Text(
                text = "Daily Reminder", style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (isEnabled) "Reminder me daily at $time" else "Disabled",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = isEnabled, onCheckedChange = { isEnabled ->
                if (isEnabled) {
                    hapticManager.perform(HapticFeedbackType.ToggleOn)
                } else {
                    hapticManager.perform(HapticFeedbackType.ToggleOff)
                }
                onToggle(isEnabled)
            })
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    action: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = titleColor)
            Spacer(Modifier.height(2.dp))
            if (subtitle.isNotEmpty()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        action()
    }
}

// ─── First Day of Month Bottom Sheet (matching image 2) ──────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FirstDayOfMonthSheet(
    current: Int, onSelect: (Int) -> Unit, onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp, top = 20.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Select First Day of Month",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Check, "Close", Modifier.size(16.dp))
                    }
                }
            }

            HorizontalDivider(
                Modifier.padding(horizontal = 0.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )

            // Day list (1–31 max default for all months)
            LazyColumn(modifier = Modifier.weight(1f)) {
                items((1..31).toList()) { day ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(day) }
                            .padding(horizontal = 20.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            "$day",
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (day == current) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurface
                        )
                        if (day == current) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    HorizontalDivider(
                        Modifier.padding(horizontal = 20.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                        thickness = 0.5.dp
                    )
                }
            }
        }
    }
}

// ─── Generic selection sheet (category / payment mode) ───────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T : Any> SelectionSheet(
    title: String,
    items: List<T>,
    selectedId: Long,
    itemLabel: (T) -> String,
    onSelect: (T) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Get id reflectively — works for Category and PaymentMode which both have Long id
    fun getId(item: T): Long = try {
        item::class.java.getDeclaredField("id").also { it.isAccessible = true }.get(item) as Long
    } catch (e: Exception) {
        -1L
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp, top = 20.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Check, "Close", Modifier.size(16.dp))
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // "None" option
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { onClear() }
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "None",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (selectedId == -1L) {
                    Icon(
                        Icons.Default.Check,
                        null,
                        Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            HorizontalDivider(
                Modifier.padding(horizontal = 20.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                thickness = 0.5.dp
            )

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(items) { item ->
                    val id = getId(item)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(item) }
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            itemLabel(item),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )
                        if (id == selectedId) {
                            Icon(
                                Icons.Default.Check,
                                null,
                                Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    HorizontalDivider(
                        Modifier.padding(horizontal = 20.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                        thickness = 0.5.dp
                    )
                }
            }
        }
    }
}

// ─── Decimal Format Bottom Sheet (matching image) ────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DecimalFormatSheet(
    current: String, onSelect: (String) -> Unit, onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    data class FormatOption(val key: String, val label: String, val example: String)

    val options = listOf(
        FormatOption("default", "Default (Optimized for readability)", "100 or 100.50"),
        FormatOption("none", "No Decimal Places (Example: 100)", "100"),
        FormatOption("one", "1 Decimal Place (Example: 100.0)", "100.0"),
        FormatOption("two", "2 Decimal Places (Example: 100.00)", "100.00"),
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            // Header row
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp, top = 20.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Select Decimal Format",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Check, "Close", Modifier.size(16.dp))
                    }
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )

            options.forEach { option ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(option.key) }
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        option.label,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (option.key == current) FontWeight.Bold
                        else FontWeight.Normal,
                        modifier = Modifier.weight(1f)
                    )
                    if (option.key == current) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                HorizontalDivider(
                    Modifier.padding(horizontal = 20.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f),
                    thickness = 0.5.dp
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
