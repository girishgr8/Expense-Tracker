package com.expensetracker.presentation.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.expensetracker.domain.model.Category
import com.expensetracker.domain.model.TransactionType
import com.expensetracker.presentation.components.CategoryIconBubble
import com.expensetracker.presentation.components.EmptyState
import androidx.core.graphics.toColorInt

// ─── Screen ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    onNavigateBack: () -> Unit,
    viewModel: CategoriesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val tabs = TransactionType.entries

    val filteredCategories = uiState.categories.filter {
        it.transactionType == uiState.selectedTab || it.transactionType == null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categories", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.showAddDialog() }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Category")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.showAddDialog() }) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Type tabs
            TabRow(selectedTabIndex = tabs.indexOf(uiState.selectedTab)) {
                tabs.forEach { type ->
                    Tab(
                        selected = type == uiState.selectedTab,
                        onClick = { viewModel.setTab(type) },
                        text = {
                            Text(type.name.lowercase().replaceFirstChar { it.uppercase() })
                        }
                    )
                }
            }

            if (filteredCategories.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        icon = Icons.Default.Category,
                        title = "No categories",
                        subtitle = "Tap + to add a category"
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredCategories, key = { it.id }) { category ->
                        CategoryItem(
                            category = category,
                            onEdit   = { viewModel.showAddDialog(category) },
                            onDelete = { viewModel.deleteCategory(category) }
                        )
                    }
                }
            }
        }
    }

    if (uiState.showAddDialog) {
        AddEditCategoryDialog(
            editing   = uiState.editingCategory,
            onDismiss = viewModel::hideDialog,
            onSave    = { name, icon, colorHex, type ->
                viewModel.saveCategory(name, icon, colorHex, type)
            }
        )
    }
}

// ─── Category list item ───────────────────────────────────────────────────────

@Composable
private fun CategoryItem(
    category: Category,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        shape     = RoundedCornerShape(12.dp),
        modifier  = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ✅ Real icon from CategoryIcons map
            CategoryIconBubble(category = category, size = 44)

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    category.name,
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    category.transactionType
                        ?.name?.lowercase()?.replaceFirstChar { it.uppercase() }
                        ?: "All Types",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (category.isDefault) {
                AssistChip(
                    onClick  = {},
                    label    = { Text("Default", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.height(28.dp)
                )
            } else {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, null, Modifier.size(18.dp))
                }
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        Icons.Default.Delete, null,
                        Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title            = { Text("Delete Category") },
            text             = {
                Text("Delete \"${category.name}\"? Transactions using this category won't be deleted.")
            },
            confirmButton    = {
                TextButton(onClick = { onDelete(); showDeleteDialog = false }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton    = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// ─── Add / Edit dialog ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditCategoryDialog(
    editing: Category?,
    onDismiss: () -> Unit,
    onSave: (name: String, icon: String, colorHex: String, type: TransactionType?) -> Unit
) {
    var name         by remember { mutableStateOf(editing?.name ?: "") }
    var selectedIcon by remember { mutableStateOf(editing?.icon ?: "restaurant") }
    var colorHex     by remember { mutableStateOf(editing?.colorHex ?: "#6750A4") }
    var selectedType by remember { mutableStateOf(editing?.transactionType) }

    val colorOptions = listOf(
        "#F44336", "#E91E63", "#9C27B0", "#3F51B5", "#2196F3",
        "#009688", "#4CAF50", "#FF9800", "#FF5722", "#795548",
        "#607D8B", "#00BCD4", "#FFC107", "#8BC34A", "#1DB954"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editing != null) "Edit Category" else "Add Category") },
        text  = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ── Name ──────────────────────────────────────────────────────
                OutlinedTextField(
                    value         = name,
                    onValueChange = { name = it },
                    label         = { Text("Category Name") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                    shape         = RoundedCornerShape(12.dp),
                    leadingIcon   = {
                        // Live preview of selected icon
                        val previewColor = runCatching {
                            Color(colorHex.toColorInt())
                        }.getOrDefault(MaterialTheme.colorScheme.primary)
                        Icon(
                            imageVector  = CategoryIcons.get(selectedIcon),
                            contentDescription = null,
                            tint         = previewColor,
                            modifier     = Modifier.size(24.dp)
                        )
                    }
                )

                // ── Color picker ──────────────────────────────────────────────
                Text("Color", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(colorOptions) { hex ->
                        val c = Color(hex.toColorInt())
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(c)
                                .align(Alignment.CenterHorizontally)
                                .then(
                                    if (hex == colorHex)
                                        Modifier.border(3.dp, Color.White, CircleShape)
                                    else Modifier
                                )
                                .clickable { colorHex = hex }
                        )
                    }
                }

                // ── Icon picker ───────────────────────────────────────────────
                Text("Icon", style = MaterialTheme.typography.labelLarge)

                CategoryIcons.sections.forEach { (sectionLabel, iconPairs) ->
                    Text(
                        sectionLabel,
                        style  = MaterialTheme.typography.labelSmall,
                        color  = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    LazyVerticalGrid(
                        columns             = GridCells.Fixed(6),
                        modifier            = Modifier.heightIn(max = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        userScrollEnabled   = false
                    ) {
                        items(iconPairs, key = { it.first }) { (key, vector) ->
                            val isSelected = key == selectedIcon
                            val tintColor  = if (isSelected) {
                                runCatching {
                                    Color(colorHex.toColorInt())
                                }.getOrDefault(MaterialTheme.colorScheme.primary)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            }
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) tintColor.copy(alpha = 0.15f)
                                        else Color.Transparent
                                    )
                                    .border(
                                        width  = if (isSelected) 1.5.dp else 0.dp,
                                        color  = if (isSelected) tintColor else Color.Transparent,
                                        shape  = CircleShape
                                    )
                                    .clickable { selectedIcon = key }
                            ) {
                                Icon(
                                    imageVector        = vector,
                                    contentDescription = key,
                                    tint               = tintColor,
                                    modifier           = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }

                // ── Type selector ─────────────────────────────────────────────
                Text("Applies To", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val typeOptions = listOf(null) + TransactionType.entries
                    items(typeOptions) { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick  = { selectedType = type },
                            label    = {
                                Text(
                                    type?.name?.lowercase()?.replaceFirstChar { it.uppercase() }
                                        ?: "All"
                                )
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick  = {
                    if (name.isNotBlank())
                        onSave(name, selectedIcon, colorHex, selectedType)
                },
                enabled  = name.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}