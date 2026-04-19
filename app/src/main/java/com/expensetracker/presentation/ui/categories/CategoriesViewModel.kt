package com.expensetracker.presentation.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.data.repository.AuthManager
import com.expensetracker.data.repository.CategoryRepository
import com.expensetracker.data.repository.UserPreferencesRepository
import com.expensetracker.domain.model.Category
import com.expensetracker.domain.model.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoriesUiState(
    val categories: List<Category> = emptyList(),
    val selectedTab: TransactionType = TransactionType.EXPENSE,
    val defaultCategoryId: Long = -1L,
    val isLoading: Boolean = false,
    val showAddDialog: Boolean = false,
    val editingCategory: Category? = null
)

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val authManager: AuthManager
) : ViewModel() {

    private val userId get() = authManager.userId
    private val _uiState = MutableStateFlow(CategoriesUiState())
    val uiState: StateFlow<CategoriesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            categoryRepository.getAllCategories(userId).collect { cats ->
                _uiState.update { it.copy(categories = cats) }
            }
        }
        viewModelScope.launch {
            userPreferencesRepository.defaultCategoryId.collect { defaultCategoryId ->
                _uiState.update { it.copy(defaultCategoryId = defaultCategoryId) }
            }
        }
    }

    fun setTab(type: TransactionType) = _uiState.update { it.copy(selectedTab = type) }
    fun showAddDialog(category: Category? = null) = _uiState.update { it.copy(showAddDialog = true, editingCategory = category) }
    fun hideDialog() = _uiState.update { it.copy(showAddDialog = false, editingCategory = null) }

    fun saveCategory(name: String, icon: String, colorHex: String, type: TransactionType?) {
        viewModelScope.launch {
            val editing = _uiState.value.editingCategory
            val nextSortOrder = (_uiState.value.categories.maxOfOrNull { it.sortOrder } ?: -1) + 1
            val category = Category(
                id = editing?.id ?: 0,
                name = name, icon = icon, colorHex = colorHex,
                transactionType = type,
                isDefault = editing?.isDefault ?: false,
                sortOrder = editing?.sortOrder ?: nextSortOrder,
                userId = editing?.userId ?: userId
            )
            if (editing != null) categoryRepository.updateCategory(category)
            else categoryRepository.insertCategory(category)
            hideDialog()
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch { categoryRepository.deleteCategory(category) }
    }

    fun updateCategoryOrder(orderedVisibleCategories: List<Category>) {
        if (orderedVisibleCategories.isEmpty()) return

        val selectedTab = _uiState.value.selectedTab
        val normalizedCategories = _uiState.value.categories
            .sortedWith(compareBy<Category> { it.sortOrder }.thenBy { it.name })
            .mapIndexed { index, category -> category.copy(sortOrder = index) }
        val visibleSortSlots = normalizedCategories
            .filter { it.transactionType == selectedTab || it.transactionType == null }
            .map { it.sortOrder }
        val orderedVisibleById = orderedVisibleCategories
            .mapIndexedNotNull { index, category ->
                visibleSortSlots.getOrNull(index)?.let { sortOrder -> category.id to sortOrder }
            }
            .toMap()
        val reordered = normalizedCategories.map { category ->
            val visibleOrder = orderedVisibleById[category.id]
            if (visibleOrder == null) {
                category
            } else {
                category.copy(sortOrder = visibleOrder)
            }
        }.sortedWith(compareBy<Category> { it.sortOrder }.thenBy { it.name })
            .mapIndexed { index, category -> category.copy(sortOrder = index) }

        viewModelScope.launch {
            categoryRepository.updateCategoryOrder(reordered)
        }
    }

    fun resetCategoryOrder() {
        val selectedTab = _uiState.value.selectedTab
        val normalizedCategories = _uiState.value.categories
            .sortedWith(compareBy<Category> { it.sortOrder }.thenBy { it.name })
            .mapIndexed { index, category -> category.copy(sortOrder = index) }
        val visibleCategories = normalizedCategories
            .filter { it.transactionType == selectedTab || it.transactionType == null }
        val visibleSortSlots = visibleCategories.map { it.sortOrder }
        val alphabetizedVisible = visibleCategories
            .sortedBy { it.name.lowercase() }
        val visibleSortOrderById = alphabetizedVisible
            .mapIndexedNotNull { index, category ->
                visibleSortSlots.getOrNull(index)?.let { sortOrder -> category.id to sortOrder }
            }
            .toMap()
        val reordered = normalizedCategories.map { category ->
            val visibleOrder = visibleSortOrderById[category.id]
            if (visibleOrder == null) {
                category
            } else {
                category.copy(sortOrder = visibleOrder)
            }
        }.sortedWith(compareBy<Category> { it.sortOrder }.thenBy { it.name })
            .mapIndexed { index, category -> category.copy(sortOrder = index) }

        viewModelScope.launch {
            categoryRepository.updateCategoryOrder(reordered)
        }
    }

    fun moveCategory(category: Category, direction: Int) {
        val orderedCategories = _uiState.value.categories
            .sortedWith(compareBy<Category> { it.sortOrder }.thenBy { it.name })
            .mapIndexed { index, cat -> cat.copy(sortOrder = index) }
        val visible = orderedCategories
            .filter { it.transactionType == _uiState.value.selectedTab || it.transactionType == null }
        val fromIndex = visible.indexOfFirst { it.id == category.id }
        if (fromIndex == -1) return
        val toIndex = fromIndex + direction
        if (toIndex !in visible.indices) return

        val fromCategory = visible[fromIndex]
        val toCategory = visible[toIndex]
        val reordered = orderedCategories.map { current ->
            when (current.id) {
                fromCategory.id -> current.copy(sortOrder = toCategory.sortOrder)
                toCategory.id -> current.copy(sortOrder = fromCategory.sortOrder)
                else -> current
            }
        }

        viewModelScope.launch {
            categoryRepository.updateCategoryOrder(reordered)
        }
    }
}
