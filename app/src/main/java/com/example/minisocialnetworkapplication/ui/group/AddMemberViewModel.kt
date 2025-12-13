package com.example.minisocialnetworkapplication.ui.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.minisocialnetworkapplication.core.domain.model.Friend
import com.example.minisocialnetworkapplication.core.domain.repository.ConversationRepository
import com.example.minisocialnetworkapplication.core.domain.repository.FriendRepository
import com.example.minisocialnetworkapplication.core.util.Result
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddMemberViewModel @Inject constructor(
    private val friendRepository: FriendRepository,
    private val conversationRepository: ConversationRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedFriends = MutableStateFlow<Set<String>>(emptySet())
    val selectedFriends = _selectedFriends.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _friends = MutableStateFlow<List<Friend>>(emptyList())
    
    // Combining search query with friends list
    val filteredFriends: StateFlow<List<Friend>> = combine(_friends, _searchQuery) { friends, query ->
        if (query.isBlank()) {
            friends
        } else {
            friends.filter { it.friendName.contains(query, ignoreCase = true) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var currentConversationId: String? = null
    private var isCurrentUserAdmin: Boolean = false

    fun loadData(conversationId: String) {
        currentConversationId = conversationId
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            _isLoading.value = true
            
            // 1. Fetch conversation to check admin status and existing participants
            val conversationResult = conversationRepository.getConversation(conversationId)
            var participantIds = emptyList<String>()
            
            if (conversationResult is Result.Success) {
                val conversation = conversationResult.data
                isCurrentUserAdmin = conversation.adminIds.contains(userId)
                participantIds = conversation.participantIds
            }

            // 2. Fetch friends
            val friendsResult = friendRepository.getUserFriends(userId)
            if (friendsResult is Result.Success) {
                // Filter out friends who are already participants
                _friends.value = friendsResult.data.filter { !participantIds.contains(it.friendId) }
            }
            
            _isLoading.value = false
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun toggleSelection(friendId: String) {
        val currentSelected = _selectedFriends.value.toMutableSet()
        if (currentSelected.contains(friendId)) {
            currentSelected.remove(friendId)
        } else {
            currentSelected.add(friendId)
        }
        _selectedFriends.value = currentSelected
    }

    fun addMembers(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val conversationId = currentConversationId ?: return
        val targets = _selectedFriends.value.toList()
        
        if (targets.isEmpty()) return

        viewModelScope.launch {
            _isLoading.value = true
            val result = if (isCurrentUserAdmin) {
                conversationRepository.addMembers(conversationId, targets)
            } else {
                conversationRepository.createJoinRequests(conversationId, targets)
            }

            _isLoading.value = false
            when (result) {
                is Result.Success -> onSuccess()
                is Result.Error -> onError(result.exception.message ?: "Unknown error")
                else -> onError("Unknown error")
            }
        }
    }
}
