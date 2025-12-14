package com.example.minisocialnetworkapplication.ui.socialgroup

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.minisocialnetworkapplication.core.domain.model.Friend
import com.example.minisocialnetworkapplication.core.domain.repository.FriendRepository
import com.example.minisocialnetworkapplication.core.domain.repository.GroupRepository
import com.example.minisocialnetworkapplication.core.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupInviteViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    private val friendRepository: FriendRepository,
    private val getCurrentUserUseCase: com.example.minisocialnetworkapplication.core.domain.usecase.auth.GetCurrentUserUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val groupId: String = checkNotNull(savedStateHandle["groupId"])

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _friends = MutableStateFlow<List<Friend>>(emptyList())

    private val _selectedFriendIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedFriendIds: StateFlow<Set<String>> = _selectedFriendIds.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    // Filtered friends based on search query
    val filteredFriends: StateFlow<List<Friend>> = combine(
        _friends,
        _searchQuery
    ) { friends, query ->
        if (query.isBlank()) friends
        else friends.filter { it.friendName.contains(query, ignoreCase = true) }
    }.let { flow ->
        MutableStateFlow<List<Friend>>(emptyList()).also { state ->
            viewModelScope.launch {
                flow.collect { state.value = it }
            }
        }
    }

    init {
        loadFriends()
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun toggleFriendSelection(friendId: String) {
        val current = _selectedFriendIds.value.toMutableSet()
        if (current.contains(friendId)) {
            current.remove(friendId)
        } else {
            current.add(friendId)
        }
        _selectedFriendIds.value = current
    }

    private fun loadFriends() {
        viewModelScope.launch {
            _isLoading.value = true
            val currentUser = getCurrentUserUseCase().firstOrNull()
            if (currentUser == null) {
                _isLoading.value = false
                return@launch
            }

            val result = friendRepository.getUserFriends(currentUser.uid)
            if (result is Result.Success) {
                // Filter out users who are already group members
                val memberResult = groupRepository.getGroupMembers(groupId)
                val memberIds = if (memberResult is Result.Success) {
                    memberResult.data.map { it.userId }
                } else emptyList()

                _friends.value = result.data.filter { !memberIds.contains(it.friendId) }
            }
            _isLoading.value = false
        }
    }

    fun sendInvitations(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val selectedIds = _selectedFriendIds.value.toList()
        if (selectedIds.isEmpty()) return

        viewModelScope.launch {
            _isSending.value = true
            val result = groupRepository.sendInvitations(groupId, selectedIds)
            _isSending.value = false

            when (result) {
                is Result.Success -> onSuccess()
                is Result.Error -> onError(result.message ?: "Failed to send invitations")
                else -> {}
            }
        }
    }
}
