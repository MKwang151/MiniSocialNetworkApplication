package com.example.minisocialnetworkapplication.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.minisocialnetworkapplication.core.domain.model.Conversation
import com.example.minisocialnetworkapplication.core.domain.model.ConversationType
import com.example.minisocialnetworkapplication.core.domain.model.User
import com.example.minisocialnetworkapplication.core.domain.repository.ConversationRepository
import com.example.minisocialnetworkapplication.core.domain.repository.UserRepository
import com.example.minisocialnetworkapplication.core.util.Result
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

sealed interface ChatSettingsUiState {
    data object Loading : ChatSettingsUiState
    data class Success(
        val conversation: Conversation,
        val otherUser: User? = null,
        val isAdmin: Boolean = false,
        val isCreator: Boolean = false
    ) : ChatSettingsUiState
    data class Error(val message: String) : ChatSettingsUiState
}

@HiltViewModel
class ChatSettingsViewModel @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val userRepository: UserRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChatSettingsUiState>(ChatSettingsUiState.Loading)
    val uiState: StateFlow<ChatSettingsUiState> = _uiState.asStateFlow()

    private val currentUserId = auth.currentUser?.uid
    
    // Track if deletion is in progress
    private val _isDeleting = MutableStateFlow(false)
    val isDeleting: StateFlow<Boolean> = _isDeleting.asStateFlow()

    fun loadConversation(conversationId: String) {
        viewModelScope.launch {
            _uiState.value = ChatSettingsUiState.Loading
            if (currentUserId == null) {
                _uiState.value = ChatSettingsUiState.Error("User not authenticated")
                return@launch
            }

            try {
                when (val result = conversationRepository.getConversation(conversationId)) {
                    is Result.Success -> {
                        val conversation = result.data
                        val otherUser = if (conversation.type == ConversationType.DIRECT) {
                            val otherUserId = conversation.participantIds.firstOrNull { it != currentUserId }
                            if (otherUserId != null) {
                                when(val userResult = userRepository.getUser(otherUserId)) {
                                    is Result.Success -> userResult.data
                                    else -> null
                                }
                            } else null
                        } else null
                        
                        _uiState.value = ChatSettingsUiState.Success(
                            conversation = conversation,
                            otherUser = otherUser,
                            isAdmin = conversation.adminIds.contains(currentUserId),
                            isCreator = conversation.creatorId == currentUserId
                        )
                    }
                    is Result.Error -> {
                        _uiState.value = ChatSettingsUiState.Error(result.exception.message ?: "Failed to load conversation")
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading conversation settings")
                _uiState.value = ChatSettingsUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
    
    fun toggleMute(conversationId: String, currentMuteState: Boolean) {
        viewModelScope.launch {
            try {
                val newState = !currentMuteState
                when (conversationRepository.updateConversation(conversationId, isMuted = newState)) {
                    is Result.Success -> {
                        val currentState = _uiState.value
                        if (currentState is ChatSettingsUiState.Success) {
                            _uiState.value = currentState.copy(
                                conversation = currentState.conversation.copy(isMuted = newState)
                            )
                        }
                    }
                    is Result.Error -> {
                        // handle error
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                Timber.e(e, "Error toggling mute")
            }
        }
    }

    fun deleteConversation(conversationId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isDeleting.value = true
            try {
               when(val result = conversationRepository.hideConversationForUser(conversationId)) {
                   is Result.Success -> onSuccess()
                   is Result.Error -> {
                       Timber.e(result.exception, "Error deleting conversation")
                   }
                   else -> {}
               }
            } catch (e: Exception) {
                Timber.e(e, "Error deleting conversation")
            } finally {
                _isDeleting.value = false
            }
        }
    }
    
    fun leaveGroup(conversationId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isDeleting.value = true
            try {
                when(val result = conversationRepository.leaveConversation(conversationId)) {
                    is Result.Success -> onSuccess()
                    is Result.Error -> {
                        Timber.e(result.exception, "Error leaving group")
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                Timber.e(e, "Error leaving group")
            } finally {
                _isDeleting.value = false
            }
        }
    }
    
    fun deleteGroupPermanent(conversationId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isDeleting.value = true
            try {
                when(val result = conversationRepository.deleteConversationPermanent(conversationId)) {
                    is Result.Success -> onSuccess()
                    is Result.Error -> {
                        Timber.e(result.exception, "Error deleting group permanently")
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                Timber.e(e, "Error deleting group permanently")
            } finally {
                _isDeleting.value = false
            }
        }
    }
}
