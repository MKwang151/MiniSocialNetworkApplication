package com.example.minisocialnetworkapplication.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.minisocialnetworkapplication.core.domain.model.Post
import com.example.minisocialnetworkapplication.core.domain.repository.AdminRepository
import com.example.minisocialnetworkapplication.core.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ContentModerationUiState {
    data object Loading : ContentModerationUiState
    data class Success(val posts: List<Post>) : ContentModerationUiState
    data class Error(val message: String) : ContentModerationUiState
}

@HiltViewModel
class ContentModerationViewModel @Inject constructor(
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _allPosts = MutableStateFlow<List<Post>>(emptyList())
    private val _uiState = MutableStateFlow<ContentModerationUiState>(ContentModerationUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        loadPosts()
        observeFilters()
    }

    private fun loadPosts() {
        viewModelScope.launch {
            adminRepository.getAllPosts().collectLatest { result ->
                when (result) {
                    is Result.Success -> {
                        _allPosts.value = result.data
                        applyFilter()
                    }
                    is Result.Error -> {
                        _uiState.value = ContentModerationUiState.Error(result.message ?: "Failed to load posts")
                    }
                    else -> {}
                }
            }
        }
    }

    private fun observeFilters() {
        viewModelScope.launch {
            searchQuery.collectLatest { applyFilter() }
        }
    }

    private fun applyFilter() {
        val query = _searchQuery.value.lowercase()
        val filtered = if (query.isEmpty()) {
            _allPosts.value
        } else {
            _allPosts.value.filter { post ->
                post.authorName.lowercase().contains(query) || 
                post.text.lowercase().contains(query)
            }
        }
        _uiState.value = ContentModerationUiState.Success(filtered)
    }

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun hidePost(postId: String) {
        viewModelScope.launch {
            adminRepository.hidePost(postId)
        }
    }

    fun restorePost(postId: String) {
        viewModelScope.launch {
            adminRepository.restorePost(postId)
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            adminRepository.deletePost(postId)
        }
    }
}
