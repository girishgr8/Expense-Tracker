package com.expensetracker.presentation.ui.wealth

import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.expensetracker.domain.model.InvestmentSnapshot
import com.expensetracker.domain.model.InvestmentType
import com.expensetracker.domain.model.SavingsSnapshot
import com.expensetracker.presentation.components.LocalCurrencyFormat
import com.expensetracker.presentation.components.LocalCurrencySymbol
import com.expensetracker.util.FormatUtils.formatAmountForDisplay
import java.time.format.DateTimeFormatter
import java.util.Locale

// ── Colors ────────────────────────────────────────────────────────────────────
private val HistoryBlue    = Color(0xFF3498DB)
private val HistoryGold    = Color(0xFFF0B429)
private val HistoryGreen   = Color(0xFF2ECC71)
private val HistoryRed     = Color(0xFFE74C3C)
private val HistoryPurple  = Color(0xFF9B59B6)

// ══════════════════════════════════════════════════════════════════════════════
// Savings History Screen
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun SavingsHistoryScreen(
    institutionName: String,
    onNavigateBack:  () -> Unit,
    viewModel:       WealthHistoryViewModel = hiltViewModel()
) {
    LaunchedEffect(institutionName) { viewModel.loadSavingsHistory(institutionName) }

    val uiState by viewModel.savingsHistory.collectAsState()
    val sym = LocalCurrencySymbol.current
    val fmt = LocalCurrencyFormat.current

    // Newest first for display, limit to 30 snapshots
    val newestFirst = uiState.snapshots.take(30)
    val oldestFirst = newestFirst.reversed()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            HistoryTopBar(
                title    = institutionName,
                subtitle = "Savings History",
                icon     = Icons.Default.AccountBalance,
                iconTint = HistoryBlue,
                onBack   = onNavigateBack
            )
        }
    ) { padding ->
        if (newestFirst.isEmpty() && !uiState.isLoading) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No history yet.\nAdd your first savings entry to start tracking.",
                    style     = MaterialTheme.typography.bodyLarge,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier       = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Summary hero card ─────────────────────────────────────────────
            item {
                SavingsSummaryHero(
                    snapshots = newestFirst,
                    sym       = sym,
                    fmt       = fmt
                )
            }

            // ── Multi-line chart ──────────────────────────────────────────────
            if (oldestFirst.size >= 2) {
                item {
                    SavingsLineChart(snapshots = oldestFirst, sym = sym, fmt = fmt)
                }
            }

            // ── Timeline entries ──────────────────────────────────────────────
            item {
                Text(
                    "All Entries  (${newestFirst.size})",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            itemsIndexed(newestFirst, key = { _, s -> s.id }) { idx, snap ->
                val prevSnap = newestFirst.getOrNull(idx + 1) // older entry
                SavingsTimelineEntry(
                    snap     = snap,
                    prevSnap = prevSnap,
                    isFirst  = idx == 0,
                    isLast   = idx == newestFirst.lastIndex,
                    sym      = sym,
                    fmt      = fmt
                )
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

// ── Savings hero ──────────────────────────────────────────────────────────────

@Composable
private fun SavingsSummaryHero(
    snapshots: List<SavingsSnapshot>,
    sym: String, fmt: String
) {
    val latest = snapshots.firstOrNull() ?: return
    val oldest = snapshots.lastOrNull()  ?: return
    val change = latest.total - oldest.total
    val pct    = if (oldest.total > 0) change / oldest.total * 100 else 0.0
    val isUp   = change >= 0
    val clr    = if (isUp) HistoryGreen else HistoryRed
    val dateFmt = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault())

    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF1A2744), Color(0xFF0F1629))))
            .padding(20.dp)
    ) {
        Column {
            Text(
                "Current Balance",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.55f),
                letterSpacing = 0.8.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "$sym${formatAmountForDisplay(latest.total, fmt)}",
                style      = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color      = Color.White
            )
            Spacer(Modifier.height(4.dp))
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    if (isUp) Icons.AutoMirrored.Filled.TrendingUp
                    else      Icons.AutoMirrored.Filled.TrendingDown,
                    null, Modifier.size(15.dp), tint = clr
                )
                Text(
                    "${if (isUp) "+" else ""}$sym${formatAmountForDisplay(kotlin.math.abs(change), fmt)}" +
                            "  (${"%.2f".format(pct)}%)",
                    style      = MaterialTheme.typography.bodyMedium,
                    color      = clr,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "since ${oldest.recordedOn.format(dateFmt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.4f)
                )
            }

            Spacer(Modifier.height(18.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 0.5.dp)
            Spacer(Modifier.height(14.dp))

            // Savings / FD / RD tiles
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HeroTile("Savings",   latest.savingsBalance, sym, fmt, HistoryBlue,   Modifier.weight(1f))
                HeroTile("FD",        latest.fdBalance,      sym, fmt, HistoryGold,   Modifier.weight(1f))
                HeroTile("RD",        latest.rdBalance,      sym, fmt, HistoryPurple, Modifier.weight(1f))
            }

            Spacer(Modifier.height(10.dp))
            Text(
                "Last updated: ${latest.recordedOn.format(dateFmt)}  ·  ${snapshots.size} entries",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.35f)
            )
        }
    }
}

// ── Savings multi-line chart ──────────────────────────────────────────────────

@Composable
private fun SavingsLineChart(snapshots: List<SavingsSnapshot>, sym: String, fmt: String) {
    val animProg by animateFloatAsState(
        targetValue   = 1f,
        animationSpec = tween(1000, easing = EaseOutCubic),
        label         = "savings_chart"
    )
    val dateFmt = DateTimeFormatter.ofPattern("dd MMM", Locale.getDefault())

    // Tap state — which data point index is selected
    var tappedIndex by remember { mutableStateOf<Int?>(null) }

    Card(
        shape  = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Balance Over Time", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                ChartLegendDot("Total",   HistoryBlue)
                ChartLegendDot("Savings", HistoryGreen)
                ChartLegendDot("FD",      HistoryGold)
                ChartLegendDot("RD",      HistoryPurple)
            }
            Spacer(Modifier.height(8.dp))

            val allValues = snapshots.flatMap {
                listOf(it.total, it.savingsBalance, it.fdBalance, it.rdBalance)
            }
            val maxVal = allValues.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
            val minVal = allValues.minOrNull() ?: 0.0
            val range  = (maxVal - minVal).coerceAtLeast(1.0)

            // Y-axis label formatter — compact (K / L / Cr)
            fun yLabel(v: Double): String = when {
                v >= 10_000_000 -> "${"%.1f".format(v / 10_000_000)}Cr"
                v >= 100_000    -> "${"%.1f".format(v / 100_000)}L"
                v >= 1_000      -> "${"%.0f".format(v / 1_000)}K"
                else            -> "%.0f".format(v)
            }
            val ySteps = 4  // number of Y-axis gridlines
            val padL   = 52.dp  // left padding for Y labels

            Box(Modifier.fillMaxWidth()) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .pointerInput(snapshots) {
                            detectTapGestures { offset ->
                                val w    = size.width.toFloat()
                                val padLpx = padL.toPx()
                                val plotW  = w - padLpx
                                val step   = if (snapshots.size > 1)
                                    plotW / (snapshots.size - 1) else plotW
                                val idx = ((offset.x - padLpx) / step).toInt()
                                    .coerceIn(0, snapshots.lastIndex)
                                tappedIndex = if (tappedIndex == idx) null else idx
                            }
                        }
                ) {
                    val w      = size.width
                    val h      = size.height
                    val padLpx = padL.toPx()
                    val padT   = 16f; val padB = 28f
                    val plotH  = h - padT - padB
                    val plotW  = w - padLpx
                    val step   = if (snapshots.size > 1)
                        plotW / (snapshots.size - 1) else plotW

                    fun yFor(v: Double) =
                        padT + plotH * (1f - ((v - minVal) / range * animProg).toFloat())

                    // Y-axis gridlines + labels
                    repeat(ySteps + 1) { i ->
                        val fraction = i.toFloat() / ySteps
                        val yVal     = minVal + range * (1f - fraction)
                        val yPos     = padT + plotH * fraction

                        // Gridline
                        drawLine(
                            color       = Color.White.copy(alpha = 0.06f),
                            start       = Offset(padLpx, yPos),
                            end         = Offset(w, yPos),
                            strokeWidth = 1f,
                            pathEffect  = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                        )
                        // Y label
                        drawContext.canvas.nativeCanvas.apply {
                            val p = android.graphics.Paint().apply {
                                color       = android.graphics.Color.argb(160, 180, 180, 180)
                                textSize    = 20f
                                textAlign   = android.graphics.Paint.Align.RIGHT
                                isAntiAlias = true
                            }
                            drawText(yLabel(yVal), padLpx - 6f, yPos + 7f, p)
                        }
                    }

                    // Y-axis vertical line
                    drawLine(
                        color = Color.White.copy(alpha = 0.15f),
                        start = Offset(padLpx, padT),
                        end   = Offset(padLpx, h - padB),
                        strokeWidth = 1f
                    )

                    // Series lines + dots
                    data class Series(val values: List<Double>, val color: Color)
                    listOf(
                        Series(snapshots.map { it.total },          HistoryBlue),
                        Series(snapshots.map { it.savingsBalance }, HistoryGreen),
                        Series(snapshots.map { it.fdBalance },      HistoryGold),
                        Series(snapshots.map { it.rdBalance },      HistoryPurple)
                    ).forEach { series ->
                        if (series.values.any { it > 0 }) {
                            val path = Path()
                            series.values.forEachIndexed { i, v ->
                                val x = padLpx + i * step; val y = yFor(v)
                                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            }
                            drawPath(path, series.color,
                                style = Stroke(2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                            series.values.forEachIndexed { i, v ->
                                val isTapped = tappedIndex == i
                                drawCircle(series.color, if (isTapped) 7f else 4f,
                                    Offset(padLpx + i * step, yFor(v)))
                                drawCircle(Color.Black.copy(if (isTapped) 0.8f else 0.5f), 2f,
                                    Offset(padLpx + i * step, yFor(v)))
                            }
                        }
                    }

                    // Tapped vertical guide line
                    tappedIndex?.let { ti ->
                        val x = padLpx + ti * step
                        drawLine(Color.White.copy(0.25f), Offset(x, padT), Offset(x, h - padB),
                            strokeWidth = 1.5f,
                            pathEffect  = PathEffect.dashPathEffect(floatArrayOf(5f, 4f)))
                    }

                    // X-axis date labels (show all if ≤8, else first/last only)
                    val showAll = snapshots.size <= 8
                    snapshots.forEachIndexed { i, snap ->
                        val showLabel = showAll || i == 0 || i == snapshots.lastIndex
                                || (snapshots.size <= 15 && i % 3 == 0)
                        if (!showLabel) return@forEachIndexed
                        drawContext.canvas.nativeCanvas.apply {
                            val p = android.graphics.Paint().apply {
                                color       = android.graphics.Color.argb(120, 200, 200, 200)
                                textSize    = 20f
                                textAlign   = android.graphics.Paint.Align.CENTER
                                isAntiAlias = true
                            }
                            drawText(snap.recordedOn.format(dateFmt),
                                padLpx + i * step, h, p)
                        }
                    }
                }

                // Tapped tooltip overlay — shows values for the selected point
                tappedIndex?.let { ti ->
                    val snap = snapshots[ti]
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                snap.recordedOn.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault())),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                TooltipChip("Total",   sym + formatAmountForDisplay(snap.total,          fmt), HistoryBlue)
                                TooltipChip("Savings", sym + formatAmountForDisplay(snap.savingsBalance, fmt), HistoryGreen)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                TooltipChip("FD", sym + formatAmountForDisplay(snap.fdBalance, fmt), HistoryGold)
                                TooltipChip("RD", sym + formatAmountForDisplay(snap.rdBalance, fmt), HistoryPurple)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Savings timeline entry ────────────────────────────────────────────────────

@Composable
private fun SavingsTimelineEntry(
    snap:     SavingsSnapshot,
    prevSnap: SavingsSnapshot?,       // older entry (null if this is the oldest)
    isFirst:  Boolean,
    isLast:   Boolean,
    sym:      String,
    fmt:      String
) {
    val dateFmt   = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault())
    val totalDiff = prevSnap?.let { snap.total - it.total }
    val isUp      = (totalDiff ?: 0.0) >= 0
    val diffColor = if (totalDiff == null) MaterialTheme.colorScheme.onSurfaceVariant
    else if (isUp) HistoryGreen else HistoryRed

    // ── Entry card ──────────────────────────────────────────────────────────────
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isFirst) HistoryBlue.copy(alpha = 0.08f)
                else         MaterialTheme.colorScheme.surfaceContainer
            )
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Date + total
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.Top
            ) {
                Column {
                    Text(
                        snap.recordedOn.format(dateFmt),
                        style      = MaterialTheme.typography.labelMedium,
                        color      = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isFirst) {
                        Text(
                            "Latest",
                            style      = MaterialTheme.typography.labelSmall,
                            color      = HistoryBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "$sym${formatAmountForDisplay(snap.total, fmt)}",
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color      = if (isFirst) HistoryBlue
                        else MaterialTheme.colorScheme.onSurface
                    )
                    if (totalDiff != null) {
                        Text(
                            "${if (isUp) "+" else ""}$sym${
                                formatAmountForDisplay(kotlin.math.abs(totalDiff), fmt)
                            }",
                            style      = MaterialTheme.typography.labelSmall,
                            color      = diffColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            HorizontalDivider(
                color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                thickness = 0.5.dp
            )

            // Savings / FD / RD breakdown
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                BreakdownMini(
                    label  = "Savings",
                    value  = "$sym${formatAmountForDisplay(snap.savingsBalance, fmt)}",
                    color  = HistoryGreen,
                    change = prevSnap?.let { snap.savingsBalance - it.savingsBalance },
                    sym    = sym,
                    fmt    = fmt,
                    modifier = Modifier.weight(1f)
                )
                BreakdownMini(
                    label  = "FD",
                    value  = "$sym${formatAmountForDisplay(snap.fdBalance, fmt)}",
                    color  = HistoryGold,
                    change = prevSnap?.let { snap.fdBalance - it.fdBalance },
                    sym    = sym,
                    fmt    = fmt,
                    modifier = Modifier.weight(1f)
                )
                BreakdownMini(
                    label  = "RD",
                    value  = "$sym${formatAmountForDisplay(snap.rdBalance, fmt)}",
                    color  = HistoryPurple,
                    change = prevSnap?.let { snap.rdBalance - it.rdBalance },
                    sym    = sym,
                    fmt    = fmt,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Investment History Screen
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun InvestmentHistoryScreen(
    type:           InvestmentType,
    subName:        String,
    onNavigateBack: () -> Unit,
    viewModel:      WealthHistoryViewModel = hiltViewModel()
) {
    LaunchedEffect(type, subName) { viewModel.loadInvestmentHistory(type, subName) }

    val uiState by viewModel.investmentHistory.collectAsState()
    val sym = LocalCurrencySymbol.current
    val fmt = LocalCurrencyFormat.current

    val newestFirst = uiState.snapshots.take(30)
    val oldestFirst = newestFirst.reversed()

    val displayTitle = subName.ifEmpty { type.shortName() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            HistoryTopBar(
                title    = displayTitle,
                subtitle = "${type.shortName()} History",
                icon     = Icons.AutoMirrored.Filled.ShowChart,
                iconTint = HistoryGold,
                onBack   = onNavigateBack
            )
        }
    ) { padding ->
        if (newestFirst.isEmpty() && !uiState.isLoading) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No history yet.\nAdd your first investment entry to start tracking.",
                    style     = MaterialTheme.typography.bodyLarge,
                    color     = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier       = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                InvestmentSummaryHero(snapshots = newestFirst, sym = sym, fmt = fmt)
            }

            if (oldestFirst.size >= 2) {
                item {
                    InvestmentLineChart(snapshots = oldestFirst, sym = sym, fmt = fmt)
                }
            }

            item {
                Text(
                    "All Entries  (${newestFirst.size})",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            itemsIndexed(newestFirst, key = { _, s -> s.id }) { idx, snap ->
                val prevSnap = newestFirst.getOrNull(idx + 1)
                InvestmentTimelineEntry(
                    snap     = snap,
                    prevSnap = prevSnap,
                    isFirst  = idx == 0,
                    isLast   = idx == newestFirst.lastIndex,
                    sym      = sym,
                    fmt      = fmt
                )
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

// ── Investment hero ───────────────────────────────────────────────────────────

@Composable
private fun InvestmentSummaryHero(
    snapshots: List<InvestmentSnapshot>,
    sym: String, fmt: String
) {
    val latest  = snapshots.firstOrNull() ?: return
    val oldest  = snapshots.lastOrNull()  ?: return
    val gainNow = latest.currentAmount - latest.investedAmount
    val gainPct = if (latest.investedAmount > 0) gainNow / latest.investedAmount * 100 else 0.0
    val isGain  = gainNow >= 0
    val gainClr = if (isGain) HistoryGreen else HistoryRed

    val currentChange = latest.currentAmount - oldest.currentAmount
    val currentPct    = if (oldest.currentAmount > 0) currentChange / oldest.currentAmount * 100 else 0.0
    val isUp          = currentChange >= 0
    val dateFmt       = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault())

    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF1C1A0F), Color(0xFF0F1629))))
            .padding(20.dp)
    ) {
        Column {
            Text(
                "Current Value",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.55f),
                letterSpacing = 0.8.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "$sym${formatAmountForDisplay(latest.currentAmount, fmt)}",
                style      = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color      = Color.White
            )
            Spacer(Modifier.height(4.dp))
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    if (isGain) Icons.AutoMirrored.Filled.TrendingUp
                    else        Icons.AutoMirrored.Filled.TrendingDown,
                    null, Modifier.size(15.dp), tint = gainClr
                )
                Text(
                    "${if (isGain) "+" else ""}$sym${
                        formatAmountForDisplay(kotlin.math.abs(gainNow), fmt)
                    }  (${"%.2f".format(gainPct)}%)",
                    style      = MaterialTheme.typography.bodyMedium,
                    color      = gainClr,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "total gain",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.4f)
                )
            }

            Spacer(Modifier.height(18.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 0.5.dp)
            Spacer(Modifier.height(14.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HeroTile("Invested",       latest.investedAmount, sym, fmt, HistoryBlue,   Modifier.weight(1f))
                HeroTile("Current",        latest.currentAmount,  sym, fmt, gainClr,       Modifier.weight(1f))
                HeroTile("Gain/Loss",      kotlin.math.abs(gainNow), sym, fmt,
                    if (isGain) HistoryGreen else HistoryRed, Modifier.weight(1f))
            }

            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Last updated: ${latest.recordedOn.format(dateFmt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.35f)
                )
                Text(
                    "${snapshots.size} entries",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.35f)
                )
            }
        }
    }
}

// ── Investment dual-line chart (invested vs current) ─────────────────────────

@Composable
private fun InvestmentLineChart(
    snapshots: List<InvestmentSnapshot>,
    sym: String, fmt: String
) {
    val animProg by animateFloatAsState(
        targetValue   = 1f,
        animationSpec = tween(1000, easing = EaseOutCubic),
        label         = "inv_chart"
    )
    val dateFmt = DateTimeFormatter.ofPattern("dd MMM", Locale.getDefault())
    var tappedIndex by remember { mutableStateOf<Int?>(null) }

    Card(
        shape  = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Invested vs Current Value", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                ChartLegendDot("Invested", HistoryBlue)
                ChartLegendDot("Current",  HistoryGold)
            }
            Spacer(Modifier.height(8.dp))

            val allV  = snapshots.flatMap { listOf(it.investedAmount, it.currentAmount) }
            val maxV  = allV.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
            val minV  = allV.minOrNull() ?: 0.0
            val range = (maxV - minV).coerceAtLeast(1.0)

            fun yLabel(v: Double): String = when {
                v >= 10_000_000 -> "${"%.1f".format(v / 10_000_000)}Cr"
                v >= 100_000    -> "${"%.1f".format(v / 100_000)}L"
                v >= 1_000      -> "${"%.0f".format(v / 1_000)}K"
                else            -> "%.0f".format(v)
            }
            val ySteps = 4
            val padL   = 52.dp

            Box(Modifier.fillMaxWidth()) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .pointerInput(snapshots) {
                            detectTapGestures { offset ->
                                val padLpx = padL.toPx()
                                val plotW  = size.width - padLpx
                                val step   = if (snapshots.size > 1) plotW / (snapshots.size - 1) else plotW
                                val idx    = ((offset.x - padLpx) / step).toInt()
                                    .coerceIn(0, snapshots.lastIndex)
                                tappedIndex = if (tappedIndex == idx) null else idx
                            }
                        }
                ) {
                    val w = size.width; val h = size.height
                    val padLpx = padL.toPx()
                    val padT = 16f; val padB = 28f
                    val plotH = h - padT - padB
                    val plotW = w - padLpx
                    val step  = if (snapshots.size > 1) plotW / (snapshots.size - 1) else plotW

                    fun yFor(v: Double) =
                        padT + plotH * (1f - ((v - minV) / range * animProg).toFloat())

                    // Gridlines + Y labels
                    repeat(ySteps + 1) { i ->
                        val fraction = i.toFloat() / ySteps
                        val yVal     = minV + range * (1f - fraction)
                        val yPos     = padT + plotH * fraction
                        drawLine(Color.White.copy(0.06f), Offset(padLpx, yPos), Offset(w, yPos),
                            1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))
                        drawContext.canvas.nativeCanvas.apply {
                            val p = android.graphics.Paint().apply {
                                color = android.graphics.Color.argb(160, 180, 180, 180)
                                textSize = 20f
                                textAlign = android.graphics.Paint.Align.RIGHT
                                isAntiAlias = true
                            }
                            drawText(yLabel(yVal), padLpx - 6f, yPos + 7f, p)
                        }
                    }

                    // Y-axis line
                    drawLine(Color.White.copy(0.15f), Offset(padLpx, padT), Offset(padLpx, h - padB), 1f)

                    // Gradient fill under current-value line
                    val currentPath = Path()
                    snapshots.forEachIndexed { i, s ->
                        val x = padLpx + i * step; val y = yFor(s.currentAmount)
                        if (i == 0) currentPath.moveTo(x, y) else currentPath.lineTo(x, y)
                    }
                    val fillPath = Path().apply {
                        addPath(currentPath)
                        lineTo(padLpx + (snapshots.size - 1) * step, h - padB)
                        lineTo(padLpx, h - padB); close()
                    }
                    drawPath(fillPath, Brush.verticalGradient(
                        listOf(HistoryGold.copy(0.18f), Color.Transparent)))

                    // Lines + dots
                    listOf(
                        Pair(snapshots.map { it.investedAmount }, HistoryBlue),
                        Pair(snapshots.map { it.currentAmount },  HistoryGold)
                    ).forEach { (values, color) ->
                        val linePath = Path()
                        values.forEachIndexed { i, v ->
                            val x = padLpx + i * step; val y = yFor(v)
                            if (i == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
                        }
                        drawPath(linePath, color, style = Stroke(2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                        values.forEachIndexed { i, v ->
                            val isTapped = tappedIndex == i
                            drawCircle(color, if (isTapped) 7f else 4f, Offset(padLpx + i * step, yFor(v)))
                            drawCircle(Color.Black.copy(if (isTapped) 0.8f else 0.5f), 2f, Offset(padLpx + i * step, yFor(v)))
                        }
                    }

                    // Tap guide line
                    tappedIndex?.let { ti ->
                        val x = padLpx + ti * step
                        drawLine(Color.White.copy(0.25f), Offset(x, padT), Offset(x, h - padB),
                            1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 4f)))
                    }

                    // X labels
                    val showAll = snapshots.size <= 8
                    snapshots.forEachIndexed { i, snap ->
                        if (!showAll && i != 0 && i != snapshots.lastIndex
                            && !(snapshots.size <= 15 && i % 3 == 0)) return@forEachIndexed
                        drawContext.canvas.nativeCanvas.apply {
                            val p = android.graphics.Paint().apply {
                                color = android.graphics.Color.argb(120, 200, 200, 200)
                                textSize = 20f; textAlign = android.graphics.Paint.Align.CENTER
                                isAntiAlias = true
                            }
                            drawText(snap.recordedOn.format(dateFmt), padLpx + i * step, h, p)
                        }
                    }
                }

                // Tap tooltip
                tappedIndex?.let { ti ->
                    val snap = snapshots[ti]
                    val gainNow = snap.currentAmount - snap.investedAmount
                    val gainPct = if (snap.investedAmount > 0) gainNow / snap.investedAmount * 100 else 0.0
                    val gainClr = if (gainNow >= 0) HistoryGreen else HistoryRed
                    Box(
                        Modifier.fillMaxWidth().padding(top = 4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(0.95f))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                snap.recordedOn.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault())),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                TooltipChip("Invested", sym + formatAmountForDisplay(snap.investedAmount, fmt), HistoryBlue)
                                TooltipChip("Current",  sym + formatAmountForDisplay(snap.currentAmount,  fmt), HistoryGold)
                            }
                            TooltipChip(
                                label = if (gainNow >= 0) "Gain" else "Loss",
                                value = "${if (gainNow >= 0) "+" else ""}${sym}${
                                    formatAmountForDisplay(kotlin.math.abs(gainNow), fmt)
                                } (${"%.2f".format(gainPct)}%)",
                                color = gainClr
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Investment timeline entry ─────────────────────────────────────────────────

@Composable
private fun InvestmentTimelineEntry(
    snap:     InvestmentSnapshot,
    prevSnap: InvestmentSnapshot?,
    isFirst:  Boolean,
    isLast:   Boolean,
    sym:      String,
    fmt:      String
) {
    val dateFmt  = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault())
    val gainNow  = snap.currentAmount - snap.investedAmount
    val gainPct  = if (snap.investedAmount > 0) gainNow / snap.investedAmount * 100 else 0.0
    val isGain   = gainNow >= 0
    val gainClr  = if (isGain) HistoryGreen else HistoryRed

    val curDiff  = prevSnap?.let { snap.currentAmount - it.currentAmount }
    val isUp     = (curDiff ?: 0.0) >= 0

    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isFirst) HistoryGold.copy(alpha = 0.07f)
                else         MaterialTheme.colorScheme.surfaceContainer
            )
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Date + current value
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.Top
            ) {
                Column {
                    Text(
                        snap.recordedOn.format(dateFmt),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isFirst) {
                        Text(
                            "Latest",
                            style      = MaterialTheme.typography.labelSmall,
                            color      = HistoryGold,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "$sym${formatAmountForDisplay(snap.currentAmount, fmt)}",
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color      = if (isFirst) HistoryGold
                        else MaterialTheme.colorScheme.onSurface
                    )
                    if (curDiff != null) {
                        Text(
                            "${if (isUp) "+" else ""}$sym${
                                formatAmountForDisplay(kotlin.math.abs(curDiff), fmt)
                            }",
                            style      = MaterialTheme.typography.labelSmall,
                            color      = if (isUp) HistoryGreen else HistoryRed,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            HorizontalDivider(
                color     = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                thickness = 0.5.dp
            )

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                BreakdownMini(
                    label    = "Invested",
                    value    = "$sym${formatAmountForDisplay(snap.investedAmount, fmt)}",
                    color    = HistoryBlue,
                    change   = prevSnap?.let { snap.investedAmount - it.investedAmount },
                    sym      = sym,
                    fmt      = fmt,
                    modifier = Modifier.weight(1f)
                )
                BreakdownMini(
                    label    = "Current",
                    value    = "$sym${formatAmountForDisplay(snap.currentAmount, fmt)}",
                    color    = HistoryGold,
                    change   = curDiff,
                    sym      = sym,
                    fmt      = fmt,
                    modifier = Modifier.weight(1f)
                )
                // Gain tile
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(gainClr.copy(alpha = 0.10f))
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(
                        if (isGain) "Gain" else "Loss",
                        style  = MaterialTheme.typography.labelSmall,
                        color  = gainClr,
                        fontSize = 9.sp
                    )
                    Text(
                        "${"%.2f".format(gainPct)}%",
                        style      = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color      = gainClr,
                        fontSize   = 11.sp
                    )
                }
            }
        }
    }
}

// ── Shared composables ────────────────────────────────────────────────────────

@Composable
private fun HistoryTopBar(
    title:    String,
    subtitle: String,
    icon:     androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    onBack:   () -> Unit
) {
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
        Box(
            Modifier.size(36.dp)
                .background(iconTint.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, Modifier.size(20.dp), tint = iconTint)
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun HeroTile(
    label: String, amount: Double, sym: String, fmt: String,
    color: Color, modifier: Modifier
) {
    Column(
        modifier = modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = color.copy(0.8f))
        Spacer(Modifier.height(3.dp))
        Text(
            "$sym${formatAmountForDisplay(amount, fmt)}",
            style      = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color      = color,
            maxLines   = 1,
            overflow   = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ChartLegendDot(label: String, color: Color) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(Modifier.size(8.dp).background(color, CircleShape))
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun BreakdownMini(
    label:    String,
    value:    String,
    color:    Color,
    change:   Double?,
    sym:      String,
    fmt:      String,
    modifier: Modifier
) {
    val isUp = (change ?: 0.0) >= 0
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.08f))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = color, fontSize = 9.sp)
        Text(value, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
            maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 11.sp)
        if (change != null && kotlin.math.abs(change) > 0.001) {
            Text(
                "${if (isUp) "▲" else "▼"} $sym${
                    formatAmountForDisplay(kotlin.math.abs(change), fmt)}",
                style      = MaterialTheme.typography.labelSmall,
                color      = if (isUp) HistoryGreen else HistoryRed,
                fontSize   = 9.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun TooltipChip(label: String, value: String, color: Color) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(Modifier.size(7.dp).background(color, CircleShape))
        Text(
            "$label: $value",
            style      = MaterialTheme.typography.labelSmall,
            color      = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}