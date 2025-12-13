package com.example.minisocialnetworkapplication.ui.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.minisocialnetworkapplication.core.domain.model.Friend
import com.example.minisocialnetworkapplication.core.domain.repository.AuthRepository
import com.example.minisocialnetworkapplication.core.domain.repository.FriendRepository
import com.example.minisocialnetworkapplication.core.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SelectParticipantsUiState {
    data object Loading : SelectParticipantsUiState()
    data class Success(val friends: List<Friend>, val selectedIds: Set<String>) : SelectParticipantsUiState()
    data class Error(val message: String) : SelectParticipantsUiState()
}

@HiltViewModel
class SelectParticipantsViewModel @Inject constructor(
    private val friendRepository: FriendRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _friends = MutableStateFlow<List<Friend>>(emptyList())
    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
    private val _isLoading = MutableStateFlow(true)
    private val _error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SelectParticipantsUiState> = combine(
        _friends,
        _selectedIds,
        _searchQuery,
        _isLoading,
        _error
    ) { friends, selectedIds, query, isLoading, error ->
        when {
            isLoading -> SelectParticipantsUiState.Loading
            error != null -> SelectParticipantsUiState.Error(error)
            else -> {
                val filtered = if (query.isBlank()) {
                    friends
                } else {
                    friends.filter { it.friendName.contains(query, ignoreCase = true) }
                }
                SelectParticipantsUiState.Success(filtered, selectedIds)
            }
            // Add else branch to cover any other cases conceptually, though sealed state is covered by explicit checks if we do when(state)
            // But here we are mapping combinators.
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SelectParticipantsUiState.Loading
    )

    init {
        loadFriends()
    }

    private fun loadFriends() {
        viewModelScope.launch {
            _isLoading.value = true
            val currentUserId = authRepository.getCurrentUserId()
            if (currentUserId == null) {
                _error.value = "User not logged in"
                _isLoading.value = false
                return@launch
            }

            when (val result = friendRepository.getUserFriends(currentUserId)) {
                is Result.Success -> {
                    _friends.value = result.data
                    _error.value = null
                }
                is Result.Error -> {
                    _error.value = result.message
                }
                is Result.Loading -> { }
            }
            _isLoading.value = false
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun toggleSelection(friendId: String) {
        val current = _selectedIds.value.toMutableSet()
        if (current.contains(friendId)) {
            current.remove(friendId)
        } else {
            current.add(friendId)
        }
        _selectedIds.value = current
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
    }
}
