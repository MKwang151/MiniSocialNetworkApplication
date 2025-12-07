package com.example.minisocialnetworkapplication.ui.chat

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.minisocialnetworkapplication.core.domain.model.Conversation
import com.example.minisocialnetworkapplication.core.domain.model.ConversationType
import com.example.minisocialnetworkapplication.core.domain.model.Message
import com.example.minisocialnetworkapplication.core.domain.model.MessageType
import com.example.minisocialnetworkapplication.core.domain.model.User
import com.example.minisocialnetworkapplication.core.domain.repository.ConversationRepository
import com.example.minisocialnetworkapplication.core.domain.repository.MessageRepository
import com.example.minisocialnetworkapplication.core.domain.repository.UserRepository
import com.example.minisocialnetworkapplication.core.domain.usecase.chat.GetMessagesUseCase
import com.example.minisocialnetworkapplication.core.domain.usecase.chat.MarkMessagesAsReadUseCase
import com.example.minisocialnetworkapplication.core.domain.usecase.chat.SendMessageUseCase
import com.example.minisocialnetworkapplication.core.util.Result
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatDetailUiState(
    val conversation: Conversation? = null,
    val messages: List<Message> = emptyList(),
    val otherUser: User? = null,
    val typingUsers: List<String> = emptyList(),
    val isLoading: Boolean = true,
    val isSending: Boolean = false,
    val error: String? = null,
    val replyToMessage: Message? = null
)

@HiltViewModel
class ChatDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getMessagesUseCase: GetMessagesUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
    private val markMessagesAsReadUseCase: MarkMessagesAsReadUseCase,
    private val conversationRepository: ConversationRepository,
    private val messageRepository: MessageRepository,
    private val userRepository: UserRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val conversationId: String = savedStateHandle.get<String>("conversationId") ?: ""

    private val _uiState = MutableStateFlow(ChatDetailUiState())
    val uiState: StateFlow<ChatDetailUiState> = _uiState.asStateFlow()

    val currentUserId: String? get() = auth.currentUser?.uid

    private var typingJob: Job? = null

    init {
        if (conversationId.isNotBlank()) {
            loadConversation()
            loadMessages()
            observeTypingStatus()
            markAsRead()
        }
    }

    private fun loadConversation() {
        viewModelScope.launch {
            when (val result = conversationRepository.getConversation(conversationId)) {
                is Result.Success -> {
                    val conversation = result.data
                    _uiState.value = _uiState.value.copy(conversation = conversation)

                    if (conversation.type == ConversationType.DIRECT) {
                        val otherUserId = conversation.participantIds.find { it != currentUserId }
                        otherUserId?.let { id ->
                            when (val userResult = userRepository.getUser(id)) {
                                is Result.Success -> {
                                    _uiState.value = _uiState.value.copy(otherUser = userResult.data)
                                }
                                is Result.Error -> { /* ignore */ }
                                is Result.Loading -> { /* ignore */ }
                            }
                        }
                    }
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(error = result.message)
                }
                is Result.Loading -> { /* ignore */ }
            }
        }
    }

    private fun loadMessages() {
        viewModelScope.launch {
            getMessagesUseCase(conversationId)
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load messages"
                    )
                }
                .collect { messages ->
                    _uiState.value = _uiState.value.copy(
                        messages = messages,
                        isLoading = false,
                        error = null
                    )
                }
        }
    }

    private fun observeTypingStatus() {
        viewModelScope.launch {
            messageRepository.observeTypingStatus(conversationId)
                .collect { typingUserIds ->
                    timber.log.Timber.d("observeTypingStatus: received typingUserIds=$typingUserIds for conv=$conversationId")
                    _uiState.value = _uiState.value.copy(typingUsers = typingUserIds)
                }
        }
    }

    private fun markAsRead() {
        viewModelScope.launch {
            markMessagesAsReadUseCase(conversationId)
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSending = true)

            val replyToId = _uiState.value.replyToMessage?.id

            when (val result = sendMessageUseCase.sendText(conversationId, text, replyToId)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isSending = false,
                        replyToMessage = null
                    )
                    setTypingStatus(false)
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isSending = false,
                        error = result.message
                    )
                }
                is Result.Loading -> { /* ignore */ }
            }
        }
    }

    fun sendMediaMessage(uris: List<Uri>, type: MessageType = MessageType.IMAGE, caption: String? = null) {
        if (uris.isEmpty()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSending = true)

            val replyToId = _uiState.value.replyToMessage?.id

            when (val result = sendMessageUseCase.sendMedia(conversationId, type, uris, caption, replyToId)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isSending = false,
                        replyToMessage = null
                    )
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isSending = false,
                        error = result.message
                    )
                }
                is Result.Loading -> { /* ignore */ }
            }
        }
    }

    fun onTextChanged(text: String) {
        typingJob?.cancel()
        typingJob = viewModelScope.launch {
            if (text.isNotBlank()) {
                setTypingStatus(true)
                delay(3000)
                setTypingStatus(false)
            } else {
                setTypingStatus(false)
            }
        }
    }

    private fun setTypingStatus(isTyping: Boolean) {
        timber.log.Timber.d("setTypingStatus: isTyping=$isTyping, conversationId=$conversationId")
        viewModelScope.launch {
            messageRepository.setTypingStatus(conversationId, isTyping)
        }
    }

    fun setReplyToMessage(message: Message?) {
        _uiState.value = _uiState.value.copy(replyToMessage = message)
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            messageRepository.deleteMessageLocal(conversationId, messageId)
        }
    }

    fun revokeMessage(messageId: String) {
        viewModelScope.launch {
            when (val result = messageRepository.revokeMessage(conversationId, messageId)) {
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(error = result.message)
                }
                is Result.Success -> { /* success */ }
                is Result.Loading -> { /* ignore */ }
            }
        }
    }

    fun addReaction(messageId: String, emoji: String) {
        viewModelScope.launch {
            messageRepository.addReaction(conversationId, messageId, emoji)
        }
    }

    fun removeReaction(messageId: String, emoji: String) {
        viewModelScope.launch {
            messageRepository.removeReaction(conversationId, messageId, emoji)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            messageRepository.setTypingStatus(conversationId, false)
        }
    }
}
