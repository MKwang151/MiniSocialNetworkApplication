package com.example.minisocialnetworkapplication.ui.searchuser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.minisocialnetworkapplication.core.domain.model.User
import com.example.minisocialnetworkapplication.core.domain.usecase.search.GetSearchResultUseCase
import com.example.minisocialnetworkapplication.core.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

sealed interface SearchUiState {
    data object Idle : SearchUiState
    data class Error(val message: String) : SearchUiState
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val getSearchResultUseCase: GetSearchResultUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    private val _searchResults = MutableStateFlow<List<User>>(emptyList())
    val searchResults: StateFlow<List<User>> = _searchResults.asStateFlow()

    init {
        debounceSearch()
    }

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
    }

    @OptIn(FlowPreview::class)
    private fun debounceSearch() {
        viewModelScope.launch {
            _query
                .debounce(300) // wait 300ms after user stops typing
                .filter { it.isNotEmpty() }
                .distinctUntilChanged()
                .collect { query ->
                    search(query)
                }
        }
    }

    fun search(query: String) {
        viewModelScope.launch {
            when (val result = getSearchResultUseCase.searchUser(query)) {
                is Result.Success -> {
                    result.getOrNull()?.let {
                        _searchResults.value = it
                    }
                }
                is Result.Error -> {
                    _uiState.value = SearchUiState.Error(message = result.message ?: "Failed to fetch search results")
                    Timber.e("Search failed: ${result.message}")
                }
                else -> {}
            }
        }
    }

    fun clearError() {
        _uiState.value = SearchUiState.Idle
    }
}