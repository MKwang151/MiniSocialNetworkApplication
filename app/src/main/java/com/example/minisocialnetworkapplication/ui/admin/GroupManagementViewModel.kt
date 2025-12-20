package com.example.minisocialnetworkapplication.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.minisocialnetworkapplication.core.domain.model.Group
import com.example.minisocialnetworkapplication.core.domain.repository.AdminRepository
import com.example.minisocialnetworkapplication.core.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface GroupManagementUiState {
    data object Loading : GroupManagementUiState
    data class Success(val groups: List<Group>) : GroupManagementUiState
    data class Error(val message: String) : GroupManagementUiState
}

@HiltViewModel
class GroupManagementViewModel @Inject constructor(
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _allGroups = MutableStateFlow<List<Group>>(emptyList())
    private val _uiState = MutableStateFlow<GroupManagementUiState>(GroupManagementUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        loadGroups()
        observeFilters()
    }

    private fun loadGroups() {
        viewModelScope.launch {
            adminRepository.getAllGroups().collectLatest { result ->
                when (result) {
                    is Result.Success -> {
                        _allGroups.value = result.data
                        applyFilter()
                    }
                    is Result.Error -> {
                        _uiState.value = GroupManagementUiState.Error(result.message ?: "Failed to load groups")
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
            _allGroups.value
        } else {
            _allGroups.value.filter { group ->
                group.name.lowercase().contains(query) || 
                group.description.lowercase().contains(query)
            }
        }
        _uiState.value = GroupManagementUiState.Success(filtered)
    }

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun banGroup(groupId: String) {
        viewModelScope.launch {
            adminRepository.banGroup(groupId)
        }
    }

    fun unbanGroup(groupId: String) {
        viewModelScope.launch {
            adminRepository.unbanGroup(groupId)
        }
    }
}
