package com.expensetracker.presentation.ui.wealth

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.expensetracker.domain.model.InvestmentRow
import com.expensetracker.domain.model.NetWorthSummary
import com.expensetracker.domain.model.SavingsRow
import com.expensetracker.presentation.components.LocalCurrencyFormat
import com.expensetracker.presentation.components.LocalCurrencySymbol
import com.expensetracker.util.FormatUtils.formatAmountForDisplay
import java.time.format.DateTimeFormatter
import java.util.Locale

// ── Theme colors ──────────────────────────────────────────────────────────────
private val WealthGold = Color(0xFFF0B429)
private val WealthGoldLight = Color(0xFFFFF3CD)
private val WealthGreen = Color(0xFF2ECC71)
private val WealthGreenDark = Color(0xFF1B7A45)
private val WealthRed = Color(0xFFE74C3C)
private val WealthBlue = Color(0xFF3498DB)
private val WealthPurple = Color(0xFF9B59B6)
private val WealthSurface = Color(0xFF1C2333)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WealthScreen(
    onNavigateBack: () -> Unit,
    onAddSavings: () -> Unit,
    onAddInvestment: () -> Unit,
    onViewSavingsHistory: (SavingsRow) -> Unit,
    onViewInvestmentHistory: (InvestmentRow) -> Unit,
    onEditSavings: (SavingsRow) -> Unit,
    onEditInvestment: (InvestmentRow) -> Unit,
    viewModel: WealthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val sym = LocalCurrencySymbol.current
    val fmt = LocalCurrencyFormat.current
    val s = uiState.summary
    var showAddMenu by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Box(
                        Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, "Back",
                            Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(Modifier.width(4.dp))
                Text(
                    "Net Worth", style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── Net worth hero card ───────────────────────────────────────
                item {
                    NetWorthHeroCard(sym = sym, fmt = fmt, summary = s)
                }

                // ── Savings section ───────────────────────────────────────────
                item {
                    SectionHeader(
                        title = "Savings",
                        icon = Icons.Default.AccountBalance,
                        iconTint = WealthBlue,
                        total = sym + formatAmountForDisplay(s.totalSavings, fmt)
                    )
                }
                if (s.savingsRows.isEmpty()) {
                    item {
                        EmptyHint(
                            text = "No savings added yet. Tap + to add your bank balances.",
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                } else {
                    items(s.savingsRows, key = { it.latestSnapshotId }) { row ->
                        SavingsCard(
                            row = row,
                            sym = sym,
                            fmt = fmt,
                            onView = { onViewSavingsHistory(row) },
                            onEdit = { onEditSavings(row) },
                            onDelete = { viewModel.deleteSavings(row.institutionName) }
                        )
                    }
                    item {
                        SavingsTotalRow(sym = sym, fmt = fmt, summary = s)
                    }
                }

                // ── Investments section ───────────────────────────────────────
                item {
                    SectionHeader(
                        title = "Investments",
                        icon = Icons.AutoMirrored.Filled.ShowChart,
                        iconTint = WealthGold,
                        total = sym + formatAmountForDisplay(s.totalCurrentInv, fmt)
                    )
                }
                if (s.investmentRows.isEmpty()) {
                    item {
                        EmptyHint(
                            text = "No investments added yet. Tap + to track your portfolio.",
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                } else {
                    items(s.investmentRows, key = { it.latestSnapshotId }) { row ->
                        InvestmentCard(
                            row = row,
                            sym = sym,
                            fmt = fmt,
                            onView = { onViewInvestmentHistory(row) },
                            onEdit = { onEditInvestment(row) },
                            onDelete = { viewModel.deleteInvestment(row.type, row.subName) }
                        )
                    }
                    item {
                        InvestmentTotalRow(sym = sym, fmt = fmt, summary = s)
                    }
                }

                // ── Wealth trend chart tabs ──────────────────────────────────
                item { Spacer(Modifier.height(8.dp)) }
                item {
                    WealthChartSection(
                        summary = s,
                        sym = sym,
                        fmt = fmt
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }

            // ── Dismiss overlay — rendered FIRST (lower z-order than FAB) ──────
            // Must be BEFORE the FAB box so it doesn't intercept FAB menu button taps
            if (showAddMenu) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures { showAddMenu = false }
                        }
                )
            }

            // ── FAB — rendered LAST so it sits on top of the dismiss overlay ─
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 16.dp, bottom = 4.dp)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    if (showAddMenu) {
                        AddMenuButton("Add Investment", WealthGold) {
                            showAddMenu = false
                            onAddInvestment()
                        }
                        Spacer(Modifier.height(8.dp))
                        AddMenuButton("Add Savings", WealthBlue) {
                            showAddMenu = false
                            onAddSavings()
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                    FloatingActionButton(
                        onClick = { showAddMenu = !showAddMenu },
                        shape = CircleShape,
                        containerColor = WealthGold,
                        contentColor = Color.Black,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(Icons.Default.Add, "Add", Modifier.size(28.dp))
                    }
                }
            }
        }
    }
}

// ─── Net Worth Hero Card ──────────────────────────────────────────────────────

@Composable
private fun NetWorthHeroCard(sym: String, fmt: String, summary: NetWorthSummary) {
    val gain = summary.netWorthWithGains - summary.netWorthWithoutGains
    val gainPct = if (summary.netWorthWithoutGains > 0)
        (gain / summary.netWorthWithoutGains) * 100.0 else 0.0
    val isGain = gain >= 0
    val gainColor = if (isGain) WealthGreen else WealthRed
    val dateFmt = DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.getDefault())

    Card(
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF1A2744), Color(0xFF0F1629))
                    ),
                    RoundedCornerShape(24.dp)
                )
                .padding(24.dp)
        ) {
            Column {
                Text(
                    "Total Net Worth",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.6f),
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "$sym${formatAmountForDisplay(summary.netWorthWithGains, fmt)}",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        if (isGain) Icons.AutoMirrored.Filled.TrendingUp
                        else Icons.AutoMirrored.Filled.TrendingDown,
                        null, Modifier.size(16.dp), tint = gainColor
                    )
                    Text(
                        "${if (isGain) "+" else ""}$sym${formatAmountForDisplay(gain, fmt)} " +
                                "(${if (isGain) "+" else ""}${"%.2f".format(gainPct)}%)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = gainColor, fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "total gains",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.4f)
                    )
                }

                Spacer(Modifier.height(20.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 0.5.dp)
                Spacer(Modifier.height(16.dp))

                // Two mini tiles: without gains / savings total
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MiniTile(
                        label = "Invested Capital",
                        value = "$sym${formatAmountForDisplay(summary.netWorthWithoutGains, fmt)}",
                        color = WealthBlue,
                        modifier = Modifier.weight(1f)
                    )
                    MiniTile(
                        label = "Investment Value",
                        value = "$sym${formatAmountForDisplay(summary.totalCurrentInv, fmt)}",
                        color = gainColor,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniTile(label: String, value: String, color: Color, modifier: Modifier) {
    Box(
        modifier = modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Column {
            Text(
                label, style = MaterialTheme.typography.labelSmall,
                color = color.copy(alpha = 0.8f)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                value, style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold, color = color
            )
        }
    }
}

// ─── Section header ───────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    total: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                Modifier
                    .size(32.dp)
                    .background(iconTint.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, Modifier.size(18.dp), tint = iconTint)
            }
            Text(
                title, style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            total, style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold, color = iconTint
        )
    }
}

// ─── Savings card ─────────────────────────────────────────────────────────────

@Composable
private fun SavingsCard(
    row: SavingsRow,
    sym: String,
    fmt: String,
    onView: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val dateFmt = DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.getDefault())

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .animateContentSize()
    ) {
        Column(Modifier.padding(16.dp)) {
            // Header row
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .background(WealthBlue.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.AccountBalance, null,
                            Modifier.size(20.dp), tint = WealthBlue
                        )
                    }
                    Column {
                        Text(
                            row.institutionName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            row.recordedOn.format(dateFmt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "$sym${formatAmountForDisplay(row.total, fmt)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold, color = WealthBlue
                    )
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        null, Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Expanded detail
            if (expanded) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    thickness = 0.5.dp
                )
                Spacer(Modifier.height(10.dp))
                WealthDetailRow(
                    "Savings Balance",
                    "$sym${formatAmountForDisplay(row.savingsBalance, fmt)}"
                )
                WealthDetailRow(
                    "FD Balance",
                    "$sym${formatAmountForDisplay(row.fdBalance, fmt)}"
                )
                WealthDetailRow(
                    "RD Balance",
                    "$sym${formatAmountForDisplay(row.rdBalance, fmt)}"
                )
                Spacer(Modifier.height(12.dp))
                // Action buttons
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallActionButton("View", WealthGreen, Modifier.weight(1f), onView)
                    SmallActionButton("Edit", WealthBlue, Modifier.weight(1f), onEdit)
                    SmallActionButton("Delete", WealthRed, Modifier.weight(1f), onDelete)
                }
            }
        }
    }
}

// ─── Savings total row ────────────────────────────────────────────────────────

@Composable
private fun SavingsTotalRow(sym: String, fmt: String, summary: NetWorthSummary) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(WealthBlue.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            "Total Savings", style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            "$sym${formatAmountForDisplay(summary.totalSavings, fmt)}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold, color = WealthBlue
        )
    }
}

// ─── Investment card ──────────────────────────────────────────────────────────

@Composable
private fun InvestmentCard(
    row: InvestmentRow,
    sym: String,
    fmt: String,
    onView: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val dateFmt = DateTimeFormatter.ofPattern("dd-MMM-yyyy", Locale.getDefault())
    val gainColor = if (row.isGain) WealthGreen else WealthRed
    val gainIcon = if (row.isGain) Icons.Default.KeyboardArrowUp
    else Icons.Default.KeyboardArrowDown

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .animateContentSize()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .background(WealthGold.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ShowChart, null,
                            Modifier.size(20.dp), tint = WealthGold
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            row.subName.ifEmpty { row.type.shortName() },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            row.type.shortName(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "$sym${formatAmountForDisplay(row.current, fmt)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(gainIcon, null, Modifier.size(14.dp), tint = gainColor)
                        Text(
                            "${"%.1f".format(kotlin.math.abs(row.gainPercent))}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = gainColor, fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            null, Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (expanded) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    thickness = 0.5.dp
                )
                Spacer(Modifier.height(10.dp))
                WealthDetailRow("Invested", "$sym${formatAmountForDisplay(row.invested, fmt)}")
                WealthDetailRow("Current Value", "$sym${formatAmountForDisplay(row.current, fmt)}")
                WealthDetailRow(
                    "Gain / Loss",
                    "${if (row.isGain) "+" else ""}$sym${
                        formatAmountForDisplay(
                            kotlin.math.abs(row.gain), fmt
                        )
                    }",
                    valueColor = gainColor
                )
                WealthDetailRow(
                    "Return",
                    "${if (row.isGain) "+" else ""}${"%.2f".format(row.gainPercent)}%",
                    valueColor = gainColor
                )
                WealthDetailRow("Last Updated", row.recordedOn.format(dateFmt))
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallActionButton("View", WealthGreen, Modifier.weight(1f), onView)
                    SmallActionButton("Edit", WealthGold, Modifier.weight(1f), onEdit)
                    SmallActionButton("Delete", WealthRed, Modifier.weight(1f), onDelete)
                }
            }
        }
    }
}

// ─── Investment total row ─────────────────────────────────────────────────────

@Composable
private fun InvestmentTotalRow(sym: String, fmt: String, summary: NetWorthSummary) {
    val gain = summary.totalCurrentInv - summary.totalInvested
    val gainColor = if (gain >= 0) WealthGreen else WealthRed

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(WealthGold.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "Invested", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "$sym${formatAmountForDisplay(summary.totalInvested, fmt)}",
                style = MaterialTheme.typography.bodySmall
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "Current Value", style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "$sym${formatAmountForDisplay(summary.totalCurrentInv, fmt)}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold, color = WealthGold
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "Total Gain/Loss", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "${if (gain >= 0) "+" else ""}$sym${
                    formatAmountForDisplay(
                        kotlin.math.abs(gain), fmt
                    )
                }",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium, color = gainColor
            )
        }
    }
}

// ─── Shared small helpers ─────────────────────────────────────────────────────

@Composable
private fun WealthDetailRow(
    label: String, value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value, style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium, color = valueColor
        )
    }
}

@Composable
private fun SmallActionButton(
    label: String,
    color: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.12f))
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label, style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold, color = color
        )
    }
}

@Composable
private fun AddMenuButton(label: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            label, style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold, color = color
        )
    }
}

@Composable
private fun EmptyHint(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        Text(
            text, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

// ─── Wealth Chart Section (tabs: Overview / Savings / Investments) ─────────────

@Composable
private fun WealthChartSection(
    summary: NetWorthSummary,
    sym: String,
    fmt: String
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Overview", "Savings", "Investments")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // ── Section label ─────────────────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                Modifier
                    .size(32.dp)
                    .background(WealthPurple.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.TrendingUp, null,
                    Modifier.size(18.dp), tint = WealthPurple
                )
            }
            Text(
                "Portfolio Breakdown",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(14.dp))

        // ── Tab row ───────────────────────────────────────────────────────────
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                tabs.forEachIndexed { idx, label ->
                    val active = idx == selectedTab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(11.dp))
                            .background(
                                if (active) MaterialTheme.colorScheme.onSurface
                                else Color.Transparent
                            )
                            .clickable { selectedTab = idx }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                            color = if (active) MaterialTheme.colorScheme.surface
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Content per tab ───────────────────────────────────────────────────
        when (selectedTab) {
            0 -> OverviewChartTab(summary = summary, sym = sym, fmt = fmt)
            1 -> SavingsBreakdownTab(summary = summary, sym = sym, fmt = fmt)
            2 -> InvestmentsBreakdownTab(summary = summary, sym = sym, fmt = fmt)
        }
    }
}

// ─── Tab 0: Overview — donut + summary rows ───────────────────────────────────

@Composable
private fun OverviewChartTab(summary: NetWorthSummary, sym: String, fmt: String) {
    val gain = summary.netWorthWithGains - summary.netWorthWithoutGains
    val isGain = gain >= 0
    val gainColor = if (isGain) WealthGreen else WealthRed
    val total = summary.netWorthWithGains

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Allocation donut chart
        if (total > 0) {
            val savingsShare = (summary.totalSavings / total).toFloat()
            val investShare = (summary.totalCurrentInv / total).toFloat()
            DonutChart(
                segments = listOf(
                    DonutSegment("Savings", savingsShare, WealthBlue),
                    DonutSegment("Investments", investShare, WealthGold)
                ),
                centerLabel = "$sym${formatAmountForDisplay(total, fmt)}",
                centerSubLabel = "Net Worth",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
        }

        // Summary rows
        OverviewRow(
            "Savings",
            "$sym${formatAmountForDisplay(summary.totalSavings, fmt)}",
            WealthBlue
        )
        OverviewRow(
            "Invested Capital",
            "$sym${formatAmountForDisplay(summary.totalInvested, fmt)}",
            WealthBlue.copy(alpha = 0.7f)
        )
        OverviewRow(
            "Investment Value",
            "$sym${formatAmountForDisplay(summary.totalCurrentInv, fmt)}",
            WealthGold
        )
        OverviewRow(
            label = "Total Gain / Loss",
            value = "${if (isGain) "+" else ""}$sym${
                formatAmountForDisplay(
                    kotlin.math.abs(gain),
                    fmt
                )
            }",
            color = gainColor,
            isBold = true
        )
        Spacer(Modifier.height(4.dp))
        // Net worth without / with gains pill row
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NetWorthPill(
                label = "Without Gains",
                value = "$sym${formatAmountForDisplay(summary.netWorthWithoutGains, fmt)}",
                color = WealthBlue,
                modifier = Modifier.weight(1f)
            )
            NetWorthPill(
                label = "With Gains",
                value = "$sym${formatAmountForDisplay(summary.netWorthWithGains, fmt)}",
                color = gainColor,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ─── Tab 1: Savings breakdown ─────────────────────────────────────────────────

@Composable
private fun SavingsBreakdownTab(summary: NetWorthSummary, sym: String, fmt: String) {
    if (summary.savingsRows.isEmpty()) {
        EmptyHint("No savings data yet.", Modifier.fillMaxWidth())
        return
    }
    val total = summary.totalSavings.coerceAtLeast(1.0)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Horizontal bar chart
        HorizontalStackedBar(
            segments = summary.savingsRows.mapIndexed { i, row ->
                StackedSegment(
                    row.institutionName, (row.total / total).toFloat(),
                    barColors[i % barColors.size]
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
        )
        // Per-institution rows
        summary.savingsRows.forEachIndexed { i, row ->
            val color = barColors[i % barColors.size]
            SavingsBreakdownRow(
                institution = row.institutionName,
                savings = row.savingsBalance,
                fd = row.fdBalance,
                rd = row.rdBalance,
                total = row.total,
                pct = if (total > 0) row.total / total * 100.0 else 0.0,
                color = color,
                sym = sym,
                fmt = fmt
            )
        }
    }
}

// ─── Tab 2: Investments breakdown ─────────────────────────────────────────────

@Composable
private fun InvestmentsBreakdownTab(summary: NetWorthSummary, sym: String, fmt: String) {
    if (summary.investmentRows.isEmpty()) {
        EmptyHint("No investment data yet.", Modifier.fillMaxWidth())
        return
    }
    val totalCurrent = summary.totalCurrentInv.coerceAtLeast(1.0)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Horizontal stacked bar
        HorizontalStackedBar(
            segments = summary.investmentRows.mapIndexed { i, row ->
                StackedSegment(
                    label = row.type.shortName(),
                    fraction = (row.current / totalCurrent).toFloat(),
                    color = barColors[i % barColors.size]
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
        )
        // Per-investment rows with gain indicator
        summary.investmentRows.forEachIndexed { i, row ->
            val color = barColors[i % barColors.size]
            val gainColor = if (row.isGain) WealthGreen else WealthRed
            InvestmentBreakdownRow(
                label = row.subName.ifEmpty { row.type.shortName() },
                typeLabel = row.type.shortName(),
                invested = row.invested,
                current = row.current,
                gainPct = row.gainPercent,
                pct = row.current / totalCurrent * 100.0,
                color = color,
                gainColor = gainColor,
                sym = sym,
                fmt = fmt
            )
        }
    }
}

// ─── Donut chart ──────────────────────────────────────────────────────────────

private data class DonutSegment(val label: String, val fraction: Float, val color: Color)

@Composable
private fun DonutChart(
    segments: List<DonutSegment>,
    centerLabel: String,
    centerSubLabel: String,
    modifier: Modifier = Modifier
) {
    val animProg by animateFloatAsState(1f, tween(900, easing = EaseOutCubic), label = "donut")

    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val diameter = size.minDimension * 0.85f
            val stroke = diameter * 0.16f
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)
            var startAngle = -90f

            segments.forEach { seg ->
                val sweep = seg.fraction * 360f * animProg
                drawArc(
                    color = seg.color,
                    startAngle = startAngle,
                    sweepAngle = sweep - 1.5f,    // 1.5° gap between segments
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(
                        width = stroke,
                        cap = androidx.compose.ui.graphics.StrokeCap.Butt
                    )
                )
                startAngle += sweep
            }
        }

        // Centre text
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                centerLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp
            )
            Text(
                centerSubLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Legend — bottom-right pills
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            segments.forEach { seg ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .background(seg.color, CircleShape)
                    )
                    Text(
                        seg.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ─── Horizontal stacked bar ───────────────────────────────────────────────────

private data class StackedSegment(val label: String, val fraction: Float, val color: Color)

@Composable
private fun HorizontalStackedBar(segments: List<StackedSegment>, modifier: Modifier) {
    val animProg by animateFloatAsState(1f, tween(800, easing = EaseOutCubic), label = "bar")
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            var x = 0f
            segments.forEach { seg ->
                val w = seg.fraction * size.width * animProg
                drawRect(color = seg.color, topLeft = Offset(x, 0f), size = Size(w, size.height))
                x += w
            }
        }
    }
}

// ─── Colour palette for charts ────────────────────────────────────────────────

private val barColors = listOf(
    Color(0xFF3498DB), Color(0xFFF0B429), Color(0xFF2ECC71),
    Color(0xFF9B59B6), Color(0xFFE74C3C), Color(0xFF1ABC9C),
    Color(0xFFE67E22), Color(0xFFF39C12), Color(0xFF8E44AD),
    Color(0xFF2980B9), Color(0xFF27AE60)
)

// ─── Breakdown row helpers ────────────────────────────────────────────────────

@Composable
private fun OverviewRow(
    label: String, value: String, color: Color, isBold: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.06f), RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
        Text(
            value, style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold,
            color = color
        )
    }
}

@Composable
private fun NetWorthPill(label: String, value: String, color: Color, modifier: Modifier) {
    Column(
        modifier = modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = color.copy(alpha = 0.8f))
        Text(
            value, style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold, color = color
        )
    }
}

@Composable
private fun SavingsBreakdownRow(
    institution: String, savings: Double, fd: Double, rd: Double,
    total: Double, pct: Double, color: Color, sym: String, fmt: String
) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .background(color, CircleShape)
                    )
                    Text(
                        institution, style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "$sym${formatAmountForDisplay(total, fmt)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold, color = color
                    )
                    Text(
                        "${"%.1f".format(pct)}% of savings",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            // Progress bar showing this institution's share
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth((pct / 100).toFloat().coerceIn(0f, 1f))
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(color)
                )
            }
            // Savings / FD / RD mini row
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniBreakdownChip(
                    "Savings",
                    "$sym${formatAmountForDisplay(savings, fmt)}",
                    Modifier.weight(1f)
                )
                MiniBreakdownChip(
                    "FD",
                    "$sym${formatAmountForDisplay(fd, fmt)}",
                    Modifier.weight(1f)
                )
                MiniBreakdownChip(
                    "RD",
                    "$sym${formatAmountForDisplay(rd, fmt)}",
                    Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun InvestmentBreakdownRow(
    label: String, typeLabel: String, invested: Double, current: Double,
    gainPct: Double, pct: Double, color: Color, gainColor: Color,
    sym: String, fmt: String
) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .background(color, CircleShape)
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            label, style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            typeLabel, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "$sym${formatAmountForDisplay(current, fmt)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${if (gainPct >= 0) "+" else ""}${"%.2f".format(gainPct)}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = gainColor, fontWeight = FontWeight.Medium
                    )
                }
            }
            // Invested vs current mini bar
            val investedFrac =
                if (current > 0) (invested / current).toFloat().coerceIn(0f, 1f) else 0f
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color.copy(alpha = 0.25f))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(investedFrac)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(color)
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MiniBreakdownChip(
                    "Invested",
                    "$sym${formatAmountForDisplay(invested, fmt)}",
                    Modifier.weight(1f)
                )
                MiniBreakdownChip(
                    "Current",
                    "$sym${formatAmountForDisplay(current, fmt)}",
                    Modifier.weight(1f)
                )
                MiniBreakdownChip("Share", "${"%.1f".format(pct)}%", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MiniBreakdownChip(label: String, value: String, modifier: Modifier) {
    Column(
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(
            label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp
        )
        Text(
            value, style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold, maxLines = 1,
            overflow = TextOverflow.Ellipsis, fontSize = 10.sp
        )
    }
}