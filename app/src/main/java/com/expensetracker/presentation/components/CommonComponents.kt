package com.expensetracker.presentation.components

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.expensetracker.R
import com.expensetracker.domain.model.Transaction
import com.expensetracker.domain.model.TransactionType
import com.expensetracker.presentation.theme.CardGradientEnd
import com.expensetracker.presentation.theme.CardGradientStart
import com.expensetracker.presentation.theme.ExpenseRed
import com.expensetracker.presentation.theme.IncomeGreen
import com.expensetracker.presentation.theme.TransferBlue
import com.expensetracker.util.FormatUtils.formatAmountForDisplay

// ─── Amount Display ───────────────────────────────────────────────────────────


// ─── Decimal Format CompositionLocal ─────────────────────────────────────────
/** Provides the user's chosen decimal format preference throughout the UI tree. */
val LocalDecimalFormat = compositionLocalOf { "default" }
val LocalCurrencySymbol = compositionLocalOf { "₹" }
val LocalCurrencyFormat = compositionLocalOf { "millions" }

// ─── Amount formatting helper ─────────────────────────────────────────────────
/** Formats an amount without trailing .00 — e.g. 10.0 → "10", 10.5 → "10.50" */
internal fun fmtAmt(amount: Double, decimalFmt: String = "default"): String {
    return formatAmountForDisplay(amount, currencyFormat = "millions", decimalFmt = decimalFmt)
}

@Composable
fun AmountText(
    amount: Double,
    type: TransactionType,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleMedium
) {
    val symbol = LocalCurrencySymbol.current
    val currencyFormat = LocalCurrencyFormat.current
    val color = when (type) {
        TransactionType.INCOME -> IncomeGreen
        TransactionType.EXPENSE -> ExpenseRed
        TransactionType.TRANSFER -> TransferBlue
    }
    val prefix = when (type) {
        TransactionType.INCOME -> "+"
        TransactionType.EXPENSE -> "-"
        TransactionType.TRANSFER -> "↔"
    }
    Text(
        text = "$prefix$symbol${formatAmountForDisplay(amount, currencyFormat)}",
        color = color,
        style = style,
        modifier = modifier
    )
}

// ─── Transaction List Item ────────────────────────────────────────────────────

@Composable
fun TransactionListItem(
    transaction: Transaction, onClick: () -> Unit, modifier: Modifier = Modifier
) {
    // Title: note if written, otherwise fall back to category name
    val title = transaction.note.ifEmpty { transaction.categoryName.ifEmpty { "Uncategorized" } }

    // Subtitle: category name (shown only when note is the title, so user still sees category)
    val subtitle = if (transaction.note.isNotEmpty()) transaction.categoryName else null

    // Date shown below the amount — e.g. "Mar 19"
    val dateLabel = transaction.dateTime.format(
        java.time.format.DateTimeFormatter.ofPattern("MMM dd")
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category icon bubble using the category's own color
            CategoryIconBubble(
                iconKey = transaction.categoryIcon.ifEmpty { "category" },
                colorHex = transaction.categoryColorHex.ifEmpty { "#6750A4" })

            Spacer(Modifier.width(12.dp))

            // Left column — title + optional category subtitle
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            // Right column — amount on top, date below
            Column(horizontalAlignment = Alignment.End) {
                AmountText(
                    amount = transaction.amount,
                    type = transaction.type,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = dateLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun CategoryIconBubble(
    iconKey: String, colorHex: String, size: Int = 44
) {
    val color =
        runCatching { Color(colorHex.toColorInt()) }.getOrDefault(MaterialTheme.colorScheme.primary)

    val imageVector = com.expensetracker.presentation.ui.categories.CategoryIcons.get(iconKey)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.3f), CircleShape)
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size((size * 0.5f).dp)
        )
    }
}

/** Convenience overload that accepts a [Category] domain object directly. */
@Composable
fun CategoryIconBubble(
    category: com.expensetracker.domain.model.Category, size: Int = 44
) {
    CategoryIconBubble(
        iconKey = category.icon, colorHex = category.colorHex, size = size
    )
}


// ─── Section Header ───────────────────────────────────────────────────────────

@Composable
fun SectionHeader(
    title: String,
    action: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold
        )
        if (action != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(text = action, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

// ─── Gradient Summary Card ────────────────────────────────────────────────────

@Composable
fun GradientSummaryCard(
    income: Double, expense: Double, balance: Double, label: String, modifier: Modifier = Modifier
) {
    val symbol = LocalCurrencySymbol.current
    val currencyFormat = LocalCurrencyFormat.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                brush = Brush.linearGradient(listOf(CardGradientStart, CardGradientEnd))
            )
            .padding(24.dp)
    ) {
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.8f)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "$symbol${formatAmountForDisplay(balance, currencyFormat)}",
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Net Balance",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth()) {
                SummaryPill(
                    label = "Income",
                    amount = income,
                    symbol = symbol,
                    currencyFormat = currencyFormat,
                    color = IncomeGreen,
                    Modifier.weight(1f)
                )
                Spacer(Modifier.width(12.dp))
                SummaryPill(
                    label = "Expense",
                    amount = expense,
                    symbol = symbol,
                    currencyFormat = currencyFormat,
                    color = ExpenseRed,
                    Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SummaryPill(
    label: String,
    amount: Double,
    symbol: String,
    currencyFormat: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.15f))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(0.8f)
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = "$symbol${
                    formatAmountForDisplay(
                        amount,
                        currencyFormat,
                        decimalFmt = "none"
                    )
                }",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ─── Tag Chip ─────────────────────────────────────────────────────────────────

@Composable
fun TagChip(
    tag: String, onRemove: (() -> Unit)? = null, modifier: Modifier
) {
    InputChip(
        selected = false,
        onClick = {},
        label = { Text("#$tag", style = MaterialTheme.typography.labelSmall) },
        trailingIcon = if (onRemove != null) {
            {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove tag",
                    modifier = Modifier
                        .size(14.dp)
                        .clickable { onRemove() })
            }
        } else null,
        modifier = modifier)
}

// ─── Empty State ──────────────────────────────────────────────────────────────

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (action != null) {
            Spacer(Modifier.height(24.dp))
            action()
        }
    }
}

// ─── Loading Overlay ──────────────────────────────────────────────────────────

@Composable
fun LoadingOverlay(isLoading: Boolean) {
    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

// ─── Period Selector Chips ────────────────────────────────────────────────────

@Composable
fun PeriodSelectorChips(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(option, style = MaterialTheme.typography.labelMedium) })
        }
    }
}

// ─── Shared App Bottom Navigation Bar ────────────────────────────────────────

/**
 * Persistent bottom bar shown on all main screens.
 * [currentRoute] is matched against the four tab routes to highlight the active item.
 * A centered FAB sits on top of the bar — tap it to add a transaction.
 */
@Composable
fun AppBottomBar(
    currentRoute: String,
    onHome: () -> Unit,
    onAnalysis: () -> Unit,
    onAccounts: () -> Unit,
    onSettings: () -> Unit,
    onAddTransaction: () -> Unit = {}
) {
    Box {
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
            tonalElevation = 4.dp
        ) {
            // Shared active/inactive colors for all items
            val navItemColors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f),
                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f),
                indicatorColor = MaterialTheme.colorScheme.surfaceContainer
            )

            // Home
            NavigationBarItem(
                selected = currentRoute == "dashboard",
                onClick = onHome,
                icon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_home),
                        contentDescription = "Home",
                        modifier = Modifier.size(28.dp)
                    )
                },
                label = {
                    Text(
                        "Home",
                        fontWeight = if (currentRoute == "dashboard") FontWeight.Bold
                        else FontWeight.Normal
                    )
                },
                colors = navItemColors
            )
            // Analysis
            NavigationBarItem(
                selected = currentRoute == "analysis",
                onClick = onAnalysis,
                icon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_analysis),
                        "Analysis",
                        modifier = Modifier.size(28.dp)
                    )
                },
                label = {
                    Text(
                        "Analysis",
                        fontWeight = if (currentRoute == "analysis") FontWeight.Bold
                        else FontWeight.Normal
                    )
                },
                colors = navItemColors
            )
            // Center slot — invisible placeholder to keep equal spacing
            NavigationBarItem(
                selected = false,
                onClick = {},
                icon = { Spacer(Modifier.size(56.dp)) },
                label = { Text("") },
                enabled = false,
                colors = navItemColors
            )
            // Accounts
            NavigationBarItem(
                selected = currentRoute == "accounts",
                onClick = onAccounts,
                icon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_bank_account),
                        "Accounts",
                        modifier = Modifier.size(28.dp)
                    )
                },
                label = {
                    Text(
                        "Accounts",
                        fontWeight = if (currentRoute == "accounts") FontWeight.Bold
                        else FontWeight.Normal
                    )
                },
                colors = navItemColors
            )
            // Settings
            NavigationBarItem(
                selected = currentRoute == "settings",
                onClick = onSettings,
                icon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_more_horiz),
                        "More",
                        modifier = Modifier.size(28.dp)
                    )
                },
                label = {
                    Text(
                        "More",
                        fontWeight = if (currentRoute == "settings") FontWeight.Bold
                        else FontWeight.Normal
                    )
                },
                colors = navItemColors
            )
        }

        // FAB centered on top of the bar
        FloatingActionButton(
            onClick = onAddTransaction,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-28).dp),
            containerColor = Color.White,
            contentColor = Color.Black,
            shape = CircleShape
        ) {
            Icon(
                Icons.Default.Add, contentDescription = "Add Transaction",
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
