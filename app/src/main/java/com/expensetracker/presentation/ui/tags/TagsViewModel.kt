package com.expensetracker.presentation.ui.tags

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.data.repository.AuthManager
import com.expensetracker.data.repository.TagRepository
import com.expensetracker.data.repository.TransactionRepository
import com.expensetracker.domain.model.Tag
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TagWithCount(
    val tag:   Tag,
    val count: Int    // number of transactions that include this tag
)

data class TagsUiState(
    val tags:        List<TagWithCount> = emptyList(),
    val searchQuery: String            = "",
    val isLoading:   Boolean           = true,
    val deleteTarget: Tag?             = null   // drives confirmation dialog
)

@HiltViewModel
class TagsViewModel @Inject constructor(
    private val tagRepository:         TagRepository,
    private val transactionRepository: TransactionRepository,
    private val authManager:           AuthManager
) : ViewModel() {

    private val userId get() = authManager.userId

    private val _uiState = MutableStateFlow(TagsUiState())
    val uiState: StateFlow<TagsUiState> = _uiState.asStateFlow()

    init { load() }

    private fun load() {
        viewModelScope.launch {
            combine(
                tagRepository.getAllTags(userId),
                transactionRepository.getAllTransactions(userId)
            ) { tags, txns ->
                // Count per tag using the tags JSON field on each transaction
                val countMap = mutableMapOf<String, Int>()
                txns.forEach { txn ->
                    txn.tags.forEach { t -> countMap[t] = (countMap[t] ?: 0) + 1 }
                }
                tags.map { tag ->
                    TagWithCount(tag = tag, count = countMap[tag.name] ?: 0)
                }
            }.collect { tagged ->
                _uiState.update { state ->
                    state.copy(
                        tags      = applySearch(tagged, state.searchQuery),
                        isLoading = false
                    )
                }
                // Keep the raw list for search filtering
                _allTags = tagged
            }
        }
    }

    private var _allTags: List<TagWithCount> = emptyList()

    fun setSearchQuery(q: String) {
        _uiState.update { it.copy(searchQuery = q, tags = applySearch(_allTags, q)) }
    }

    fun requestDelete(tag: Tag) = _uiState.update { it.copy(deleteTarget = tag) }
    fun cancelDelete()           = _uiState.update { it.copy(deleteTarget = null) }

    fun confirmDelete() {
        val tag = _uiState.value.deleteTarget ?: return
        _uiState.update { it.copy(deleteTarget = null) }
        viewModelScope.launch { tagRepository.deleteTag(tag) }
    }

    private fun applySearch(
        tags: List<TagWithCount>,
        query: String
    ): List<TagWithCount> =
        if (query.isBlank()) tags
        else tags.filter { it.tag.name.contains(query, ignoreCase = true) }
}