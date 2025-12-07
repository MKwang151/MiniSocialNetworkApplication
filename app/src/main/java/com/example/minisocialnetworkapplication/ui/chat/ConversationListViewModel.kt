package com.example.minisocialnetworkapplication.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.minisocialnetworkapplication.core.domain.model.Conversation
import com.example.minisocialnetworkapplication.core.domain.model.User
import com.example.minisocialnetworkapplication.core.domain.repository.ConversationRepository
import com.example.minisocialnetworkapplication.core.domain.repository.UserRepository
import com.example.minisocialnetworkapplication.core.domain.usecase.chat.GetConversationsUseCase
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConversationListUiState(
    val conversations: List<ConversationWithUser> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val searchQuery: String = ""
)

data class ConversationWithUser(
    val conversation: Conversation,
    val otherUser: User? = null // For direct conversations, the other participant
)

@HiltViewModel
class ConversationListViewModel @Inject constructor(
    private val getConversationsUseCase: GetConversationsUseCase,
    private val conversationRepository: ConversationRepository,
    private val userRepository: UserRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConversationListUiState())
    val uiState: StateFlow<ConversationListUiState> = _uiState.asStateFlow()

    val currentUserId: String? get() = auth.currentUser?.uid

    init {
        loadConversations()
    }

    private fun loadConversations() {
        viewModelScope.launch {
            getConversationsUseCase()
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load conversations"
                    )
                }
                .collect { conversations ->
                    // Enrich conversations with user data for direct chats
                    val enrichedConversations = conversations.map { conversation ->
                        val otherUser = if (conversation.type == com.example.minisocialnetworkapplication.core.domain.model.ConversationType.DIRECT) {
                            val otherUserId = conversation.participantIds.find { it != currentUserId }
                            otherUserId?.let { id ->
                                when (val result = userRepository.getUser(id)) {
                                    is com.example.minisocialnetworkapplication.core.util.Result.Success -> result.data
                                    else -> null
                                }
                            }
                        } else null

                        ConversationWithUser(conversation, otherUser)
                    }

                    _uiState.value = _uiState.value.copy(
                        conversations = enrichedConversations,
                        isLoading = false,
                        error = null
                    )
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        
        if (query.isBlank()) {
            loadConversations()
        } else {
            viewModelScope.launch {
                conversationRepository.searchConversations(query)
                    .collect { conversations ->
                        val enrichedConversations = conversations.map { ConversationWithUser(it, null) }
                        _uiState.value = _uiState.value.copy(conversations = enrichedConversations)
                    }
            }
        }
    }

    fun pinConversation(conversationId: String, isPinned: Boolean) {
        viewModelScope.launch {
            conversationRepository.updateConversation(conversationId, isPinned = isPinned)
        }
    }

    fun muteConversation(conversationId: String, isMuted: Boolean) {
        viewModelScope.launch {
            conversationRepository.updateConversation(conversationId, isMuted = isMuted)
        }
    }

    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            conversationRepository.deleteConversation(conversationId)
        }
    }

    fun markAsRead(conversationId: String) {
        viewModelScope.launch {
            conversationRepository.markConversationAsRead(conversationId)
        }
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        loadConversations()
    }
}
