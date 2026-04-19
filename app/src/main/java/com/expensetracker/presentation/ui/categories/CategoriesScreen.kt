package com.expensetracker.presentation.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.expensetracker.domain.model.Category
import com.expensetracker.domain.model.TransactionType
import com.expensetracker.presentation.components.CategoryIconBubble
import com.expensetracker.presentation.components.EmptyState
import androidx.core.graphics.toColorInt
import kotlin.math.roundToInt

// ─── Screen ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    onNavigateBack: () -> Unit,
    viewModel: CategoriesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val tabs = TransactionType.entries
    var showResetOrderDialog by remember { mutableStateOf(false) }

    val filteredCategories = uiState.categories
        .filter { it.transactionType == uiState.selectedTab || it.transactionType == null }
        .sortedWith(compareBy<Category> { it.sortOrder }.thenBy { it.name })
    val defaultCategory = uiState.categories.firstOrNull { it.id == uiState.defaultCategoryId }

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
                    IconButton(onClick = { showResetOrderDialog = true }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset category order")
                    }
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

            defaultCategory?.let {
                DefaultCategoryCard(category = it)
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
                ReorderableCategoryList(
                    categories = filteredCategories,
                    defaultCategoryId = uiState.defaultCategoryId,
                    modifier = Modifier.weight(1f),
                    onOrderChanged = viewModel::updateCategoryOrder,
                    onEdit = viewModel::showAddDialog,
                    onDelete = viewModel::deleteCategory
                )
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

    if (showResetOrderDialog) {
        AlertDialog(
            onDismissRequest = { showResetOrderDialog = false },
            title = { Text("Reset categories order?") },
            text = { Text("Are you sure you want to reset the categories order to alphabetic order?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetCategoryOrder()
                        showResetOrderDialog = false
                    }
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetOrderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ─── Category list item ───────────────────────────────────────────────────────

@Composable
private fun DefaultCategoryCard(category: Category) {
    Card(
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Default category",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ReorderableCategoryList(
    categories: List<Category>,
    defaultCategoryId: Long,
    modifier: Modifier = Modifier,
    onOrderChanged: (List<Category>) -> Unit,
    onEdit: (Category) -> Unit,
    onDelete: (Category) -> Unit
) {
    val scrollState = rememberScrollState()
    val orderedCategories = remember { mutableStateListOf<Category>() }
    val rowHeights = remember { mutableStateMapOf<Long, Int>() }
    var draggedCategoryId by remember { mutableStateOf<Long?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(categories.map { it.id to it.sortOrder }) {
        if (draggedCategoryId == null) {
            orderedCategories.clear()
            orderedCategories.addAll(categories)
        }
    }

    val density = LocalDensity.current
    val defaultRowHeightPx = with(density) { 76.dp.toPx() }
    val rowSpacingPx = with(density) { 8.dp.toPx() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        orderedCategories.forEach { category ->
            key(category.id) {
                val isDragging = draggedCategoryId == category.id
                CategoryItem(
                    category = category,
                    isUserDefault = category.id == defaultCategoryId,
                    modifier = Modifier
                        .zIndex(if (isDragging) 1f else 0f)
                        .offset {
                            IntOffset(
                                x = 0,
                                y = if (isDragging) dragOffset.roundToInt() else 0
                            )
                        }
                        .onGloballyPositioned { coordinates ->
                            rowHeights[category.id] = coordinates.size.height
                        },
                    dragHandleModifier = Modifier.pointerInput(category.id) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                draggedCategoryId = category.id
                                dragOffset = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragOffset += dragAmount.y

                                val currentIndex = orderedCategories.indexOfFirst { it.id == category.id }
                                if (currentIndex != -1) {
                                    val currentHeight = rowHeights[category.id]?.toFloat() ?: defaultRowHeightPx
                                    if (dragOffset > currentHeight / 2f && currentIndex < orderedCategories.lastIndex) {
                                        val next = orderedCategories[currentIndex + 1]
                                        val nextHeight = rowHeights[next.id]?.toFloat() ?: defaultRowHeightPx
                                        orderedCategories.removeAt(currentIndex)
                                        orderedCategories.add(currentIndex + 1, category)
                                        dragOffset -= nextHeight + rowSpacingPx
                                    } else if (dragOffset < -currentHeight / 2f && currentIndex > 0) {
                                        val previous = orderedCategories[currentIndex - 1]
                                        val previousHeight = rowHeights[previous.id]?.toFloat() ?: defaultRowHeightPx
                                        orderedCategories.removeAt(currentIndex)
                                        orderedCategories.add(currentIndex - 1, category)
                                        dragOffset += previousHeight + rowSpacingPx
                                    }
                                }
                            },
                            onDragEnd = {
                                val finalOrder = orderedCategories.toList()
                                draggedCategoryId = null
                                dragOffset = 0f
                                onOrderChanged(finalOrder)
                            },
                            onDragCancel = {
                                draggedCategoryId = null
                                dragOffset = 0f
                                orderedCategories.clear()
                                orderedCategories.addAll(categories)
                            }
                        )
                    },
                    onEdit = { onEdit(category) },
                    onDelete = { onDelete(category) }
                )
            }
        }
    }
}

@Composable
private fun CategoryItem(
    category: Category,
    isUserDefault: Boolean,
    modifier: Modifier = Modifier,
    dragHandleModifier: Modifier = Modifier,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        shape     = RoundedCornerShape(12.dp),
        modifier  = modifier.fillMaxWidth(),
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

            if (isUserDefault) {
                AssistChip(
                    onClick  = {},
                    label    = { Text("Default", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.height(28.dp)
                )
                Spacer(Modifier.width(8.dp))
            }

            Icon(
                Icons.Default.Menu,
                contentDescription = "Drag to reorder",
                modifier = dragHandleModifier.size(36.dp).padding(7.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!category.isDefault) {
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
