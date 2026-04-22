package com.expensetracker.presentation.ui.debt

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.hilt.navigation.compose.hiltViewModel
import com.expensetracker.domain.model.Debt
import com.expensetracker.domain.model.DebtType
import com.expensetracker.presentation.components.LocalCurrencyFormat
import com.expensetracker.presentation.components.LocalCurrencySymbol
import com.expensetracker.util.FormatUtils.formatAmountForDisplay
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtsScreen(
    onNavigateBack: () -> Unit,
    onAddLending: () -> Unit,
    onAddBorrowing: () -> Unit,
    viewModel: DebtsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val sym = LocalCurrencySymbol.current
    val fmt = LocalCurrencyFormat.current

    Box(modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Top bar ───────────────────────────────────────────────────────
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
                    "Debts",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = {}) {
                    Box(
                        Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.HelpOutline, "Help", Modifier.size(20.dp))
                    }
                }
            }

            // ── Tab row: All / Lending / Borrowing ───────────────────────────
            DebtTabRow(
                selected = uiState.activeTab,
                onSelect = viewModel::setTab
            )

            Spacer(Modifier.height(8.dp))

            // ── Content ───────────────────────────────────────────────────────
            AnimatedContent(
                targetState = uiState.visibleDebts.isEmpty(),
                label = "debt_content"
            ) { isEmpty ->
                if (isEmpty) {
                    DebtEmptyState(
                        tab = uiState.activeTab,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 16.dp, vertical = 8.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(uiState.visibleDebts, key = { it.id }) { debt ->
                            DebtCard(
                                debt = debt,
                                currencySymbol = sym,
                                currencyFormat = fmt,
                                onSettle = { viewModel.settleDebt(debt) },
                                onDelete = { viewModel.deleteDebt(debt) }
                            )
                        }
                        item { Spacer(Modifier.height(100.dp)) }
                    }
                }
            }
        }

        // ── Pinned bottom CTA ─────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Button(
                onClick = when (uiState.activeTab) {
                    DebtTabFilter.BORROWING -> onAddBorrowing
                    else -> onAddLending
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Text(
                    text = when (uiState.activeTab) {
                        DebtTabFilter.ALL -> "Add Your First Debt"
                        DebtTabFilter.LENDING ->
                            if (uiState.visibleDebts.isEmpty()) "Add a Lending Entry"
                            else "Add Lending Entry"

                        DebtTabFilter.BORROWING ->
                            if (uiState.visibleDebts.isEmpty()) "Add a Borrowing Entry"
                            else "Add Borrowing Entry"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

// ─── Tab Row ──────────────────────────────────────────────────────────────────

@Composable
private fun DebtTabRow(
    selected: DebtTabFilter,
    onSelect: (DebtTabFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        listOf(
            DebtTabFilter.ALL to "All",
            DebtTabFilter.LENDING to "Lending",
            DebtTabFilter.BORROWING to "Borrowing"
        ).forEach { (tab, label) ->
            val active = selected == tab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (active) MaterialTheme.colorScheme.onSurface
                        else Color.Transparent
                    )
                    .then(
                        Modifier.then(
                            Modifier.clickable { onSelect(tab) }
                        )
                    )
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (active) MaterialTheme.colorScheme.surface
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ─── Empty state with illustration ────────────────────────────────────────────

@Composable
private fun DebtEmptyState(tab: DebtTabFilter, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.SpaceBetween) {
        // Illustration — hands exchanging money
        HandsIllustration(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
        )

        // Text content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.height(24.dp))
            Text(
                text = when (tab) {
                    DebtTabFilter.ALL -> "Track Your Debts\nEffortlessly"
                    DebtTabFilter.LENDING -> "No Lending\nActivity Yet"
                    DebtTabFilter.BORROWING -> "No Borrowing\nActivity Yet"
                },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 36.sp
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = when (tab) {
                    DebtTabFilter.ALL ->
                        "Managing money lent or borrowed has never been easier. " +
                                "Keep track of who owes you or what you owe, all in one place."

                    DebtTabFilter.LENDING ->
                        "You haven't lent any money yet. Once you do, you can track " +
                                "it here to stay on top of your repayments."

                    DebtTabFilter.BORROWING ->
                        "You haven't borrowed any money yet. Once you do, you can " +
                                "track it here to stay on top of your repayments."
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp
            )
            Spacer(Modifier.height(20.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Transparent)
                    .clickable {}
            ) {
                Text(
                    "Learn more",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForwardIos,
                    null,
                    Modifier.size(14.dp)
                )
            }
        }

        // Bottom spacer so the pinned button doesn't overlap
        Spacer(Modifier.height(100.dp))
    }
}

// ─── Hands illustration (Canvas-drawn vector) ─────────────────────────────────

@Composable
private fun HandsIllustration(modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f

        // Background: slightly lighter dark rectangle (the gradient bg in the image)
        drawRect(
            color = Color(0xFF1A1F2C),
            topLeft = androidx.compose.ui.geometry.Offset(0f, cy * 0.4f),
            size = androidx.compose.ui.geometry.Size(w, h - cy * 0.4f)
        )

        val paint = android.graphics.Paint().apply { isAntiAlias = true }
        val c = drawContext.canvas.nativeCanvas

        // ── Left hand (receiving) ─────────────────────────────────────────────
        paint.color = "#E8A87C".toColorInt()     // skin tone
        // Sleeve (dark blue)
        paint.color = "#1E4D8C".toColorInt()
        c.drawRoundRect(
            0f, cy - 20f, cx - 40f, cy + 60f, 12f, 12f, paint
        )
        // White cuff
        paint.color = android.graphics.Color.WHITE
        c.drawRoundRect(cx - 100f, cy - 10f, cx - 40f, cy + 30f, 8f, 8f, paint)
        // Hand
        paint.color = "#E8A87C".toColorInt()
        c.drawRoundRect(cx - 160f, cy - 30f, cx + 40f, cy + 50f, 30f, 30f, paint)
        // Fingers
        val fingerW = 26f
        for (i in 0..3) {
            c.drawRoundRect(
                cx - 130f + i * 32f, cy - 75f,
                cx - 130f + i * 32f + fingerW, cy - 10f,
                12f, 12f, paint
            )
        }

        // ── Cash stack (green) ────────────────────────────────────────────────
        paint.color = "#2E7D32".toColorInt()
        c.drawRoundRect(cx - 120f, cy - 50f, cx + 80f, cy + 10f, 8f, 8f, paint)
        paint.color = "#388E3C".toColorInt()
        c.drawRoundRect(cx - 115f, cy - 55f, cx + 85f, cy + 5f, 8f, 8f, paint)
        paint.color = "#43A047".toColorInt()
        c.drawRoundRect(cx - 110f, cy - 60f, cx + 90f, cy, 8f, 8f, paint)
        // Dollar sign on cash
        paint.color = "#1B5E20".toColorInt()
        paint.textSize = 28f; paint.textAlign = android.graphics.Paint.Align.CENTER
        paint.isFakeBoldText = true
        c.drawText("$", cx - 10f, cy - 28f, paint)
        paint.textAlign = android.graphics.Paint.Align.LEFT
        paint.isFakeBoldText = false

        // ── Right hand (giving) ───────────────────────────────────────────────
        // Sleeve
        paint.color = "#1E4D8C".toColorInt()
        c.drawRoundRect(cx + 60f, cy - 20f, w, cy + 60f, 12f, 12f, paint)
        // White cuff
        paint.color = android.graphics.Color.WHITE
        c.drawRoundRect(cx + 60f, cy - 10f, cx + 130f, cy + 30f, 8f, 8f, paint)
        // Hand
        paint.color = "#E8A87C".toColorInt()
        c.drawRoundRect(cx + 10f, cy - 30f, cx + 200f, cy + 50f, 30f, 30f, paint)
        // Fingers (pointing left)
        for (i in 0..3) {
            c.drawRoundRect(
                cx + 20f + i * 32f, cy - 75f,
                cx + 20f + i * 32f + fingerW, cy - 10f,
                12f, 12f, paint
            )
        }
    }
}

// ─── Debt card (for list view when debts exist) ───────────────────────────────

@Composable
private fun DebtCard(
    debt: Debt,
    currencySymbol: String,
    currencyFormat: String,
    onSettle: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFmt = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault())
    val typeColor = if (debt.type == DebtType.LENDING)
        com.expensetracker.presentation.theme.IncomeGreen
    else
        com.expensetracker.presentation.theme.ExpenseRed

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        debt.personName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        debt.type.displayName(),
                        style = MaterialTheme.typography.bodySmall,
                        color = typeColor
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "$currencySymbol${
                            formatAmountForDisplay(
                                debt.remainingAmount,
                                currencyFormat
                            )
                        }",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = typeColor
                    )
                    if (debt.isSettled) {
                        Text(
                            "Settled",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (debt.isOverdue) {
                        Text(
                            "Overdue",
                            style = MaterialTheme.typography.labelSmall,
                            color = com.expensetracker.presentation.theme.ExpenseRed
                        )
                    }
                }
            }

            debt.dueDate?.let {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Due: ${it.format(dateFmt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (debt.isOverdue)
                        com.expensetracker.presentation.theme.ExpenseRed
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (debt.note.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    debt.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}