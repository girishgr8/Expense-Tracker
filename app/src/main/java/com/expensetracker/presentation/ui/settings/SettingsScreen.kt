package com.expensetracker.presentation.ui.settings

import android.app.TimePickerDialog
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Upload
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.expensetracker.domain.model.ExportFormat
import com.expensetracker.presentation.components.AppBottomBar
import com.expensetracker.presentation.ui.export.ExportFormatBottomSheet
import com.expensetracker.presentation.ui.export.ExportOptionsBottomSheet
import com.expensetracker.presentation.ui.export.ExportSuccessBottomSheet
import com.expensetracker.presentation.ui.export.ExportViewModel
import com.expensetracker.util.HapticManager
import com.expensetracker.util.LocalHapticManager
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToHome: () -> Unit = {},
    onNavigateToAnalysis: () -> Unit = {},
    onNavigateToAccounts: () -> Unit = {},
    onNavigateToAddTransaction: () -> Unit = {},
    onNavigateToCurrency: () -> Unit = {},
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

    LaunchedEffect(Unit) {
        exportViewModel.loadFilters()
    }

    if (showTimePicker) {
        val context = LocalContext.current

        DisposableEffect(Unit) {
            val timePickerDialog = TimePickerDialog(
                context,
                { _, hour, minute ->
                    settingsViewModel.setBudgetReminderTime(hour, minute)
                },
                uiState.dailyReminderHour,
                uiState.dailyReminderMinute,
                false
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
            }
        )
    }

    if (showFormatSheet) {
        ExportFormatBottomSheet(
            onDismiss = { showFormatSheet = false },
            onFormatSelected = { format ->
                exportViewModel.export(
                    context,
                    uiState.userName,
                    uiState.userEmail,
                    format == ExportFormat.PDF
                )
                showFormatSheet = false
            }
        )
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

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                AppBottomBar(
                    currentRoute = "settings",
                    onHome     = onNavigateToHome,
                    onAnalysis = onNavigateToAnalysis,
                    onAccounts = onNavigateToAccounts,
                    onSettings = {},
                    onAddTransaction = onNavigateToAddTransaction,
                )
            },
            topBar = {
                TopAppBar(
                    title = { Text("Settings", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // User Profile Card
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (uiState.userPhotoUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = uiState.userPhotoUrl,
                                    contentDescription = "Profile photo",
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        uiState.userName.firstOrNull()?.toString() ?: "U",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(Modifier.width(16.dp))
                            Column {
                                Text(
                                    uiState.userName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    uiState.userEmail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }

                // Appearance Section
                item { SettingsSectionHeader("Appearance") }

                item {
                    SettingsCard {
                        // Currency
                        SettingsItem(
                            icon = Icons.Default.CurrencyRupee,
                            title = "Currency",
                            subtitle = "${uiState.currencyCode} • ${uiState.currencySymbol}"
                        ) {
                            IconButton(onClick = onNavigateToCurrency) {
                                Icon(Icons.Default.ChevronRight, null)
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                        // Theme mode
                        SettingsItem(
                            icon = Icons.Default.DarkMode,
                            title = "Theme",
                            subtitle = uiState.themeMode.replaceFirstChar { it.uppercase() }
                        ) {
                            var expanded by remember { mutableStateOf(false) }
                            Box {
                                TextButton(onClick = { expanded = true }) {
                                    Text(uiState.themeMode.replaceFirstChar { it.uppercase() })
                                }
                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }) {
                                    listOf("system", "light", "dark").forEach { mode ->
                                        DropdownMenuItem(
                                            text = { Text(mode.replaceFirstChar { it.uppercase() }) },
                                            onClick = {
                                                settingsViewModel.setThemeMode(mode); expanded =
                                                false
                                            },
                                            trailingIcon = if (mode == uiState.themeMode) {
                                                { Icon(Icons.Default.Check, null) }
                                            } else null
                                        )
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

                        // Haptics
                        SettingsItem(
                            icon = Icons.Default.TouchApp,
                            title = "Haptics",
                            subtitle = "Subtle taps when you interact with the app"
                        ) {
                            Switch(
                                checked = uiState.isHapticsEnabled,
                                onCheckedChange = settingsViewModel::setIsHapticsEnabled
                            )
                        }
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

//                    SettingsItem(
//                        icon = Icons.Default.Sync,
//                        title = "Daily Reminder",
//                        subtitle = "Get notified to log expenses"
//                    ) {
//                        Switch(
//                            checked = uiState.isDailyReminderEnabled,
//                            onCheckedChange = { isEnabled ->
//                                if (isEnabled) {
//                                    hapticManager.perform(HapticFeedbackType.ToggleOn)
//                                } else {
//                                    hapticManager.perform(HapticFeedbackType.ToggleOff)
//                                }
//                                viewModel.setIsDailyReminderEnabled(isEnabled)
//                            }
//                        )
//                    }
//
//                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
//
//                    SettingsItem(
//                        icon = Icons.Default.Schedule,
//                        title = "Reminder Time",
//                        subtitle = String.format(
//                            Locale.getDefault(),
//                            "%02d:%02d",
//                            uiState.dailyReminderHour,
//                            uiState.dailyReminderMinute
//                        )
//                    ) {
//                        TextButton(
//                            onClick = { timePickerDialog.show() },
//                            enabled = uiState.isDailyReminderEnabled
//                        ) {
//                            Text("Change")
//                        }
//                    }

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
                                }
                            )
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
                        ) { }
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

            if (exportResult.isSuccess && exportResult.uri != null) {
                ExportSuccessBottomSheet(
                    uri = exportResult.uri!!,
                    onDismiss = { exportViewModel.resetExportState() }
                )
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
                    }
                ) {
                    Text("Sign Out", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("Cancel") }
            }
        )
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
                onClick = { onTimeClick() }
            )
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
                text = "Daily Reminder",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (isEnabled) "Reminder me daily at $time" else "Disabled",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = isEnabled,
            onCheckedChange = { isEnabled ->
                if (isEnabled) {
                    hapticManager.perform(HapticFeedbackType.ToggleOn)
                } else {
                    hapticManager.perform(HapticFeedbackType.ToggleOff)
                }
                onToggle(isEnabled)
            }
        )
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