package com.example.minisocialnetworkapplication.ui.group

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.minisocialnetworkapplication.core.domain.model.User
import com.example.minisocialnetworkapplication.core.domain.repository.AuthRepository
import com.example.minisocialnetworkapplication.core.domain.repository.ConversationRepository
import com.example.minisocialnetworkapplication.core.domain.repository.UserRepository
import com.example.minisocialnetworkapplication.core.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.minisocialnetworkapplication.core.domain.model.Conversation

data class MemberUiModel(
    val user: User,
    val isAdmin: Boolean,
    val isCreator: Boolean // Optionally track creator if needed, but admin is main role
)

@HiltViewModel
class ChatMembersViewModel @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val conversationId: String = checkNotNull(savedStateHandle["conversationId"])
    
    private val _members = MutableStateFlow<List<MemberUiModel>>(emptyList())
    val members = _members.asStateFlow()

    private val _currentUserIsAdmin = MutableStateFlow(false)
    val currentUserIsAdmin = _currentUserIsAdmin.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    init {
        loadMembers()
    }

    fun loadMembers() {
        viewModelScope.launch {
            _isLoading.value = true
            val currentUserId = authRepository.getCurrentUserId()

            when (val result = conversationRepository.getConversation(conversationId)) {
                is Result.Success -> {
                    val conversation = result.data
                    val adminIds = conversation.adminIds ?: emptyList() // Assuming added to model or fetched
                    // Note: Conversation model might need adminIds field if not present. 
                    // Checked Conversation model previously - need to verify if adminIds is in Domain model.
                    // If not, need to add it. Assuming it is or I need to fetch it.
                    
                    // Actually, let's check Conversation model first.
                    // If adminIds is missing in Domain model, I must add it.
                    
                    fetchUsers(conversation.participantIds, adminIds, curUserId = currentUserId)
                }
                is Result.Error -> {
                    _error.value = "Failed to load conversation: ${result.message}"
                }
                is Result.Loading -> {}
            }
            _isLoading.value = false
        }
    }

    private suspend fun fetchUsers(participantIds: List<String>, adminIds: List<String>, curUserId: String?) {
        val userList = mutableListOf<MemberUiModel>()
        
        // Check if current user is admin
        _currentUserIsAdmin.value = adminIds.contains(curUserId)

        for (id in participantIds) {
            when (val userResult = userRepository.getUser(id)) {
                is Result.Success -> {
                    val user = userResult.data
                    val isAdmin = adminIds.contains(id)
                    // First admin in list is usually creator (simplified logic) or check explicit creatorId if exists
                    val isCreator = adminIds.firstOrNull() == id 
                    
                    userList.add(MemberUiModel(user, isAdmin, isCreator))
                }
                else -> {}
            }
        }
        _members.value = userList.sortedByDescending { it.isAdmin }
    }

    fun promoteToAdmin(userId: String) {
        viewModelScope.launch {
            when (val result = conversationRepository.addAdmin(conversationId, userId)) {
                is Result.Success -> loadMembers()
                is Result.Error -> _error.value = "Failed to promote: ${result.message}"
                else -> {}
            }
        }
    }

    fun demoteAdmin(userId: String) {
        viewModelScope.launch {
            when (val result = conversationRepository.removeAdmin(conversationId, userId)) {
                is Result.Success -> loadMembers()
                is Result.Error -> _error.value = "Failed to demote: ${result.message}"
                else -> {}
            }
        }
    }

    fun removeMember(userId: String) {
        viewModelScope.launch {
             when (val result = conversationRepository.removeParticipant(conversationId, userId)) {
                is Result.Success -> loadMembers()
                is Result.Error -> _error.value = "Failed to remove member: ${result.message}"
                else -> {}
            }
        }
    }
}
