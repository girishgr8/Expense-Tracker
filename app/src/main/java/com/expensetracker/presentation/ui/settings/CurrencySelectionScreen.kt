package com.expensetracker.presentation.ui.settings

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class CurrencyInfo(
    val code: String,
    val name: String,
    val symbol: String,
    val flag: String           // emoji flag
)

val ALL_CURRENCIES = listOf(
    CurrencyInfo("USD", "United States Dollar", "$", "🇺🇸"),
    CurrencyInfo("INR", "Indian Rupee", "₹", "🇮🇳"),
    CurrencyInfo("EUR", "Euro", "€", "🇪🇺"),
    CurrencyInfo("JPY", "Japanese Yen", "¥", "🇯🇵"),
    CurrencyInfo("GBP", "British Pound", "£", "🇬🇧"),
    CurrencyInfo("AUD", "Australian Dollar", "A$", "🇦🇺"),
    CurrencyInfo("CAD", "Canadian Dollar", "C$", "🇨🇦"),
    CurrencyInfo("CHF", "Swiss Franc", "Fr", "🇨🇭"),
    CurrencyInfo("CNY", "Chinese Yuan", "¥", "🇨🇳"),
    CurrencyInfo("SEK", "Swedish Krona", "kr", "🇸🇪"),
    CurrencyInfo("NZD", "New Zealand Dollar", "NZ$", "🇳🇿"),
    CurrencyInfo("MXN", "Mexican Peso", "$", "🇲🇽"),
    CurrencyInfo("SGD", "Singapore Dollar", "S$", "🇸🇬"),
    CurrencyInfo("HKD", "Hong Kong Dollar", "HK$", "🇭🇰"),
    CurrencyInfo("NOK", "Norwegian Krone", "kr", "🇳🇴"),
    CurrencyInfo("KRW", "South Korean Won", "₩", "🇰🇷"),
    CurrencyInfo("TRY", "Turkish Lira", "₺", "🇹🇷"),
    CurrencyInfo("RUB", "Russian Ruble", "₽", "🇷🇺"),
    CurrencyInfo("BRL", "Brazilian Real", "R$", "🇧🇷"),
    CurrencyInfo("ZAR", "South African Rand", "R", "🇿🇦"),
    CurrencyInfo("AED", "UAE Dirham", "د.إ", "🇦🇪"),
    CurrencyInfo("SAR", "Saudi Riyal", "﷼", "🇸🇦"),
    CurrencyInfo("THB", "Thai Baht", "฿", "🇹🇭"),
    CurrencyInfo("MYR", "Malaysian Ringgit", "RM", "🇲🇾"),
    CurrencyInfo("IDR", "Indonesian Rupiah", "Rp", "🇮🇩"),
    CurrencyInfo("PHP", "Philippine Peso", "₱", "🇵🇭"),
    CurrencyInfo("PKR", "Pakistani Rupee", "₨", "🇵🇰"),
    CurrencyInfo("BDT", "Bangladeshi Taka", "৳", "🇧🇩"),
    CurrencyInfo("NPR", "Nepalese Rupee", "₨", "🇳🇵"),
    CurrencyInfo("LKR", "Sri Lankan Rupee", "₨", "🇱🇰"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencySelectionScreen(
    currentCode: String,
    currentFormat: String,
    onSave: (code: String, symbol: String, format: String) -> Unit,
    onNavigateBack: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCode by remember { mutableStateOf(currentCode) }
    var selectedFormat by remember { mutableStateOf(currentFormat) }

    val filtered = remember(searchQuery) {
        if (searchQuery.isBlank()) ALL_CURRENCIES
        else ALL_CURRENCIES.filter {
            it.code.contains(searchQuery, ignoreCase = true) ||
                    it.name.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Currency", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Button(
                    onClick = {
                        val info = ALL_CURRENCIES.find { it.code == selectedCode }
                            ?: ALL_CURRENCIES[0]
                        onSave(selectedCode, info.symbol, selectedFormat)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search currencies") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, null)
                        }
                    }
                } else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered, key = { it.code }) { currency ->
                    val isSelected = currency.code == selectedCode
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected)
                                MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(if (isSelected) 0.dp else 1.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedCode = currency.code }
                    ) {
                        Column(Modifier.fillMaxWidth()) {
                            // Currency row
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Flag emoji in circle
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(currency.flag, fontSize = 22.sp)
                                }
                                Spacer(Modifier.width(14.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "${currency.code} (${currency.symbol})",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected)
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        currency.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isSelected)
                                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedCode = currency.code },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }

                            // Number format subsection (only for selected INR)
                            if (isSelected && currency.code == "INR") {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                                    Text(
                                        "Format",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    listOf(
                                        Triple("millions", "Millions Format", "1,000,000"),
                                        Triple("lakhs", "Lakhs Format", "10,00,000"),
                                        Triple("none", "No Format", "1000000"),
                                    ).forEach { (key, label, example) ->
                                        Row(
                                            Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { selectedFormat = key }
                                                .padding(vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = selectedFormat == key,
                                                onClick = { selectedFormat = key }
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                "$label ($example)",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (selectedFormat == key)
                                                    FontWeight.SemiBold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}