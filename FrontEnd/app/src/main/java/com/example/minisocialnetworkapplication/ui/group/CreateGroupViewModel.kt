package com.example.minisocialnetworkapplication.ui.group

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.minisocialnetworkapplication.core.domain.model.User
import com.example.minisocialnetworkapplication.core.domain.repository.ConversationRepository
import com.example.minisocialnetworkapplication.core.domain.repository.UserRepository
import com.example.minisocialnetworkapplication.core.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class CreateGroupUiState {
    data object Loading : CreateGroupUiState()
    data class Success(
        val participantUsers: List<User>,
        val isCreating: Boolean = false,
        val createdConversationId: String? = null
    ) : CreateGroupUiState()
    data class Error(val message: String) : CreateGroupUiState()
}

@HiltViewModel
class CreateGroupViewModel @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val userRepository: UserRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val participantIdsString: String = savedStateHandle["participantIds"] ?: ""
    private val participantIds = participantIdsString.split(",").filter { it.isNotEmpty() }

    private val _participantUsers = MutableStateFlow<List<User>>(emptyList())
    private val _isCreating = MutableStateFlow(false)
    private val _createdConversationId = MutableStateFlow<String?>(null)
    private val _error = MutableStateFlow<String?>(null)
    private val _isLoading = MutableStateFlow(true)

    val uiState: StateFlow<CreateGroupUiState> = combine(
        _participantUsers,
        _isCreating,
        _createdConversationId,
        _isLoading,
        _error
    ) { users, isCreating, createdId, isLoading, error ->
        when {
            isLoading -> CreateGroupUiState.Loading
            error != null -> CreateGroupUiState.Error(error)
            else -> CreateGroupUiState.Success(users, isCreating, createdId)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CreateGroupUiState.Loading
    )

    init {
        loadParticipants()
    }

    private fun loadParticipants() {
        viewModelScope.launch {
            _isLoading.value = true
            val users = mutableListOf<User>()
            // In a real app, we might have a bulk fetch API. 
            // Here we fetch individually (or maybe UserRepository has getUserList?). 
            // Checked outline: getUser(uid) exists.
            
            for (id in participantIds) {
                when (val result = userRepository.getUser(id)) {
                    is Result.Success -> users.add(result.data)
                    is Result.Error -> { /* Ignore or handle missing user */ }
                    is Result.Loading -> { }
                }
            }
            _participantUsers.value = users
            _isLoading.value = false
        }
    }
    
    fun removeParticipant(userId: String) {
        // Technically we should update participantIds but since we just use it for display and creation...
        // Wait, if user removes a participant, we should update the list to be created.
        val current = _participantUsers.value.toMutableList()
        current.removeAll { it.uid == userId }
        _participantUsers.value = current
    }

    fun createGroup(name: String, imageUri: Uri?) {
        if (name.isBlank()) {
            _error.value = "Group name cannot be empty"
            return
        }
        
        val finalParticipants = _participantUsers.value.map { it.uid }
        if (finalParticipants.size < 2) {
             _error.value = "Group must have at least 2 other members"
             return
        }

        viewModelScope.launch {
            _isCreating.value = true
            _error.value = null // Clear previous errors
            
            var avatarUrl: String? = null
            
            if (imageUri != null) {
                when (val uploadResult = conversationRepository.uploadGroupAvatar(imageUri)) {
                    is Result.Success -> {
                        avatarUrl = uploadResult.data
                    }
                    is Result.Error -> {
                        _error.value = "Failed to upload avatar: ${uploadResult.message}"
                        _isCreating.value = false
                        return@launch
                    }
                    is Result.Loading -> { /* Should not happen for upload here */ }
                }
            }
            
            when (val result = conversationRepository.createGroupConversation(
                name = name,
                participantIds = finalParticipants,
                avatarUrl = avatarUrl
            )) {
                is Result.Success -> {
                    _createdConversationId.value = result.data.id
                }
                is Result.Error -> {
                    _error.value = result.message
                    _isCreating.value = false
                }
                is Result.Loading -> {
                     _isCreating.value = true
                }
            }
        }
    }
    
    fun clearError() {
        _error.value = null
    }
}
