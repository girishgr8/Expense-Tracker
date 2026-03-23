package com.expensetracker.presentation.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.data.repository.AuthManager
import com.expensetracker.data.repository.CategoryRepository
import com.expensetracker.domain.model.Category
import com.expensetracker.domain.model.TransactionType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoriesUiState(
    val categories: List<Category> = emptyList(),
    val selectedTab: TransactionType = TransactionType.EXPENSE,
    val isLoading: Boolean = false,
    val showAddDialog: Boolean = false,
    val editingCategory: Category? = null
)

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
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
    }

    fun setTab(type: TransactionType) = _uiState.update { it.copy(selectedTab = type) }
    fun showAddDialog(category: Category? = null) = _uiState.update { it.copy(showAddDialog = true, editingCategory = category) }
    fun hideDialog() = _uiState.update { it.copy(showAddDialog = false, editingCategory = null) }

    fun saveCategory(name: String, icon: String, colorHex: String, type: TransactionType?) {
        viewModelScope.launch {
            val editing = _uiState.value.editingCategory
            val category = Category(
                id = editing?.id ?: 0,
                name = name, icon = icon, colorHex = colorHex,
                transactionType = type, isDefault = false, userId = userId
            )
            if (editing != null) categoryRepository.updateCategory(category)
            else categoryRepository.insertCategory(category)
            hideDialog()
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch { categoryRepository.deleteCategory(category) }
    }
}
