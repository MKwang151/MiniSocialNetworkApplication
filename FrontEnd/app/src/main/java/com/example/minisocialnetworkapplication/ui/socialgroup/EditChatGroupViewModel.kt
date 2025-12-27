package com.example.minisocialnetworkapplication.ui.socialgroup

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.minisocialnetworkapplication.core.domain.repository.ConversationRepository
import com.example.minisocialnetworkapplication.core.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class EditChatGroupUiState {
    data object Idle : EditChatGroupUiState()
    data object Loading : EditChatGroupUiState()
    data object Updated : EditChatGroupUiState()
    data class Error(val message: String) : EditChatGroupUiState()
}

@HiltViewModel
class EditChatGroupViewModel @Inject constructor(
    private val repository: ConversationRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val conversationId: String = checkNotNull(savedStateHandle["conversationId"])
    
    private val _uiState = MutableStateFlow<EditChatGroupUiState>(EditChatGroupUiState.Loading)
    val uiState: StateFlow<EditChatGroupUiState> = _uiState.asStateFlow()

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _avatarUri = MutableStateFlow<Uri?>(null)
    val avatarUri: StateFlow<Uri?> = _avatarUri.asStateFlow()

    private val _currentAvatarUrl = MutableStateFlow<String?>(null)
    val currentAvatarUrl: StateFlow<String?> = _currentAvatarUrl.asStateFlow()

    init {
        loadConversation()
    }

    private fun loadConversation() {
        viewModelScope.launch {
            _uiState.value = EditChatGroupUiState.Loading
            when (val result = repository.getConversation(conversationId)) {
                is Result.Success -> {
                    val conversation = result.data
                    _name.value = conversation.name ?: ""
                    _currentAvatarUrl.value = conversation.avatarUrl
                    _uiState.value = EditChatGroupUiState.Idle
                }
                is Result.Error -> {
                    _uiState.value = EditChatGroupUiState.Error(result.exception.message ?: "Failed to load group")
                }
                else -> {}
            }
        }
    }

    fun onNameChange(newName: String) {
        _name.value = newName
    }

    fun onAvatarChange(uri: Uri?) {
        _avatarUri.value = uri
    }

    fun updateGroup() {
        if (_name.value.isBlank()) {
            _uiState.value = EditChatGroupUiState.Error("Group name cannot be empty")
            return
        }

        viewModelScope.launch {
            _uiState.value = EditChatGroupUiState.Loading
            
            var finalAvatarUrl = _currentAvatarUrl.value
            
            // Upload new avatar if selected
            _avatarUri.value?.let { uri ->
                when (val uploadResult = repository.uploadGroupAvatar(uri)) {
                    is Result.Success -> finalAvatarUrl = uploadResult.data
                    is Result.Error -> {
                        _uiState.value = EditChatGroupUiState.Error("Failed to upload avatar")
                        return@launch
                    }
                    else -> {}
                }
            }

            val result = repository.updateGroupMetadata(
                conversationId = conversationId,
                name = _name.value,
                avatarUrl = finalAvatarUrl
            )

            when (result) {
                is Result.Success -> {
                    _uiState.value = EditChatGroupUiState.Updated
                }
                is Result.Error -> {
                    _uiState.value = EditChatGroupUiState.Error(result.exception.message ?: "Failed to update group")
                }
                else -> {}
            }
        }
    }
}
