package com.expensetracker.presentation.ui.wealth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.expensetracker.domain.model.InvestmentRow
import com.expensetracker.domain.model.InvestmentType
import com.expensetracker.domain.model.SavingsRow

private val AccentGold  = Color(0xFFF0B429)
private val AccentBlue  = Color(0xFF3498DB)
private val AccentGreen = Color(0xFF2ECC71)

// ─── Add / Edit Savings Screen ────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSavingsScreen(
    existing:       SavingsRow? = null,
    onNavigateBack: () -> Unit,
    onSaved:        () -> Unit,
    viewModel:      WealthViewModel = hiltViewModel()
) {
    val formState  by viewModel.savingsForm.collectAsState()
    val snackState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.initSavingsForm(existing) }
    LaunchedEffect(formState.isSaved) { if (formState.isSaved) onSaved() }
    LaunchedEffect(formState.error) {
        formState.error?.let {
            snackState.showSnackbar(it)
            viewModel.clearSavingsError()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
        ) {
            WealthTopBar(
                title = if (existing != null) "Edit Savings" else "Add Savings",
                onBack = onNavigateBack
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Spacer(Modifier.height(4.dp))

                WealthFormCard(
                    title = "Bank / Institution",
                    accentColor = AccentBlue
                ) {
                    WealthTextField(
                        value         = formState.institutionName,
                        onValueChange = viewModel::setSavingsInstitution,
                        placeholder   = "e.g. HDFC Bank, IOB Bank",
                        leadingIcon   = {
                            Icon(Icons.Default.AccountBalance, null,
                                tint = AccentBlue, modifier = Modifier.size(20.dp))
                        },
                        enabled = existing == null   // institution name locked on edit
                    )
                }

                WealthFormCard(title = "Balances", accentColor = AccentBlue) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        WealthAmountField(
                            label         = "Savings Balance",
                            value         = formState.savingsBalance,
                            onValueChange = viewModel::setSavingsBalance,
                            accentColor   = AccentBlue
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            thickness = 0.5.dp
                        )
                        WealthAmountField(
                            label         = "FD Balance",
                            value         = formState.fdBalance,
                            onValueChange = viewModel::setFdBalance,
                            accentColor   = AccentBlue
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            thickness = 0.5.dp
                        )
                        WealthAmountField(
                            label         = "RD Balance",
                            value         = formState.rdBalance,
                            onValueChange = viewModel::setRdBalance,
                            accentColor   = AccentBlue
                        )
                    }
                }

                Spacer(Modifier.height(100.dp))
            }
        }

        SnackbarHost(
            hostState = snackState,
            modifier  = Modifier.align(Alignment.BottomCenter).navigationBarsPadding()
        )

        SaveButton(
            label    = "Save Savings",
            color    = AccentBlue,
            modifier = Modifier.align(Alignment.BottomCenter),
            onClick  = viewModel::saveSavings
        )
    }
}

// ─── Add / Edit Investment Screen ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddInvestmentScreen(
    existing:       InvestmentRow? = null,
    onNavigateBack: () -> Unit,
    onSaved:        () -> Unit,
    viewModel:      WealthViewModel = hiltViewModel()
) {
    val formState  by viewModel.investmentForm.collectAsState()
    val snackState = remember { SnackbarHostState() }
    var showTypeDropdown by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.initInvestmentForm(existing) }
    LaunchedEffect(formState.isSaved) { if (formState.isSaved) onSaved() }
    LaunchedEffect(formState.error) {
        formState.error?.let {
            snackState.showSnackbar(it)
            viewModel.clearInvestmentError()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
        ) {
            WealthTopBar(
                title = if (existing != null) "Edit Investment" else "Add Investment",
                onBack = onNavigateBack
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Spacer(Modifier.height(4.dp))

                // ── Investment type picker ─────────────────────────────────────
                WealthFormCard(title = "Investment Type", accentColor = AccentGold) {
                    Box {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .then(
                                    if (existing == null) Modifier.clickable { showTypeDropdown = true }
                                    else Modifier
                                )
                                .padding(horizontal = 14.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ShowChart, null,
                                    tint = AccentGold, modifier = Modifier.size(20.dp))
                                Text(
                                    formState.type.displayName(),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            if (existing == null) {
                                Icon(Icons.Default.ExpandMore, null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        DropdownMenu(
                            expanded = showTypeDropdown,
                            onDismissRequest = { showTypeDropdown = false }
                        ) {
                            InvestmentType.entries.forEach { type ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            if (type == formState.type) {
                                                Icon(Icons.Default.Check, null,
                                                    Modifier.size(16.dp), tint = AccentGold)
                                            } else {
                                                Spacer(Modifier.size(16.dp))
                                            }
                                            Text(type.displayName())
                                        }
                                    },
                                    onClick = {
                                        viewModel.setInvestmentType(type)
                                        showTypeDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                // ── Broker / company name (conditional) ───────────────────────
                if (formState.type.requiresBrokerName()) {
                    // Title and placeholder differ between equity (broker) and MF (fund house)
                    val isMutualFund = formState.type == InvestmentType.INDIAN_MUTUAL_FUND
                            || formState.type == InvestmentType.US_MUTUAL_FUND
                    val brokerTitle       = if (isMutualFund) "Fund House / Platform" else "Broker Name"
                    val brokerPlaceholder = if (isMutualFund)
                        "e.g. Mirae Asset, Zerodha(Coin)"
                    else
                        "e.g. Zerodha, Groww, Angel One"

                    WealthFormCard(title = brokerTitle, accentColor = AccentGold) {
                        WealthTextField(
                            value         = formState.subName,
                            onValueChange = viewModel::setInvestmentSubName,
                            placeholder   = brokerPlaceholder,
                            leadingIcon   = {
                                Icon(Icons.Default.Person, null,
                                    tint = AccentGold, modifier = Modifier.size(20.dp))
                            }
                        )
                    }
                }

                if (formState.type.requiresCompanyName()) {
                    WealthFormCard(title = "Company / Stock Name", accentColor = AccentGold) {
                        WealthTextField(
                            value         = formState.subName,
                            onValueChange = viewModel::setInvestmentSubName,
                            placeholder   = "e.g. Infosys, Google, Qualcomm",
                            leadingIcon   = {
                                Icon(Icons.Default.Business, null,
                                    tint = AccentGold, modifier = Modifier.size(20.dp))
                            }
                        )
                    }
                }

                // ── Amounts ────────────────────────────────────────────────────
                WealthFormCard(title = "Portfolio Values", accentColor = AccentGold) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        WealthAmountField(
                            label         = "Total Invested Amount",
                            value         = formState.investedAmount,
                            onValueChange = viewModel::setInvestedAmount,
                            accentColor   = AccentBlue,
                            hint          = "Capital you've put in so far"
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            thickness = 0.5.dp
                        )
                        WealthAmountField(
                            label         = "Current Market Value",
                            value         = formState.currentAmount,
                            onValueChange = viewModel::setCurrentAmount,
                            accentColor   = AccentGreen,
                            hint          = "Today's total portfolio value"
                        )
                    }
                }

                // ── Live gain preview ──────────────────────────────────────────
                val invested = formState.investedAmount.toDoubleOrNull() ?: 0.0
                val current  = formState.currentAmount.toDoubleOrNull() ?: 0.0
                if (invested > 0 && current > 0) {
                    val gain    = current - invested
                    val gainPct = (gain / invested) * 100
                    val isGain  = gain >= 0
                    val gainColor = if (isGain) AccentGreen else Color(0xFFE74C3C)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                gainColor.copy(alpha = 0.08f), RoundedCornerShape(12.dp)
                            )
                            .border(0.5.dp, gainColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Text(
                                "Unrealised ${if (isGain) "Gain" else "Loss"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = gainColor
                            )
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "${if (isGain) "+" else ""}₹${"%.2f".format(gain)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = gainColor
                                )
                                Text(
                                    "${if (isGain) "+" else ""}${"%.2f".format(gainPct)}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = gainColor
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(100.dp))
            }
        }

        SnackbarHost(
            hostState = snackState,
            modifier  = Modifier.align(Alignment.BottomCenter).navigationBarsPadding()
        )

        SaveButton(
            label   = "Save Investment",
            color   = AccentGold,
            modifier = Modifier.align(Alignment.BottomCenter),
            onClick  = viewModel::saveInvestment
        )
    }
}

// ─── Shared form composables ──────────────────────────────────────────────────

@Composable
private fun WealthTopBar(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Box(
                Modifier.size(38.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.width(4.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun WealthFormCard(
    title:       String,
    accentColor: Color,
    content:     @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(4.dp).background(accentColor, CircleShape))
                Text(title, style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold, color = accentColor)
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun WealthTextField(
    value:         String,
    onValueChange: (String) -> Unit,
    placeholder:   String,
    leadingIcon:   @Composable (() -> Unit)? = null,
    enabled:       Boolean = true
) {
    OutlinedTextField(
        value         = value,
        onValueChange = onValueChange,
        placeholder   = { Text(placeholder,
            color = MaterialTheme.colorScheme.onSurfaceVariant) },
        leadingIcon   = leadingIcon,
        enabled       = enabled,
        singleLine    = true,
        modifier      = Modifier.fillMaxWidth(),
        shape         = RoundedCornerShape(12.dp),
        colors        = OutlinedTextFieldDefaults.colors(
            focusedContainerColor   = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            disabledContainerColor  = MaterialTheme.colorScheme.surfaceVariant,
            focusedBorderColor      = Color.Transparent,
            unfocusedBorderColor    = Color.Transparent,
            disabledBorderColor     = Color.Transparent
        )
    )
}

@Composable
private fun WealthAmountField(
    label:         String,
    value:         String,
    onValueChange: (String) -> Unit,
    accentColor:   Color,
    hint:          String = ""
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall,
            color = accentColor, fontWeight = FontWeight.Medium)
        if (hint.isNotEmpty()) {
            Text(hint, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        OutlinedTextField(
            value         = value,
            onValueChange = onValueChange,
            placeholder   = { Text("0.00",
                color = MaterialTheme.colorScheme.onSurfaceVariant) },
            prefix        = { Text("₹ ", fontWeight = FontWeight.Medium) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine    = true,
            modifier      = Modifier.fillMaxWidth(),
            shape         = RoundedCornerShape(12.dp),
            colors        = OutlinedTextFieldDefaults.colors(
                focusedContainerColor   = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                focusedBorderColor      = accentColor.copy(alpha = 0.5f),
                unfocusedBorderColor    = Color.Transparent
            )
        )
    }
}

@Composable
private fun SaveButton(
    label:    String,
    color:    Color,
    modifier: Modifier,
    onClick:  () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 20.dp)
    ) {
        Button(
            onClick   = onClick,
            modifier  = Modifier.fillMaxWidth().height(56.dp),
            shape     = RoundedCornerShape(28.dp),
            colors    = ButtonDefaults.buttonColors(
                containerColor = color,
                contentColor   = Color.Black
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Text(label, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}