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
                            // Observe user real-time for online status updates
                            observeOtherUser(id)
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
    
    private fun observeOtherUser(userId: String) {
        viewModelScope.launch {
            userRepository.observeUser(userId).collect { user ->
                _uiState.value = _uiState.value.copy(otherUser = user)
            }
        }
    }

    private fun loadMessages() {
        viewModelScope.launch {
            var lastMessageCount = 0
            
            getMessagesUseCase(conversationId)
                .catch { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load messages"
                    )
                }
                .collect { messages ->
                    val currentCount = messages.size
                    timber.log.Timber.d("loadMessages: received $currentCount messages, lastCount=$lastMessageCount")
                    
                    _uiState.value = _uiState.value.copy(
                        messages = messages,
                        isLoading = false,
                        error = null
                    )
                    
                    // Mark as read whenever new messages arrive (count changed means new message or initial load)
                    // BUT skip if latest message is from current user (sendMessage already handles lastReadSequenceId)
                    if (currentCount != lastMessageCount) {
                        val latestMessage = messages.firstOrNull()
                        val isFromCurrentUser = latestMessage?.senderId == currentUserId
                        timber.log.Timber.d("loadMessages: message count changed $lastMessageCount -> $currentCount, latestSenderId=${latestMessage?.senderId}, isFromCurrentUser=$isFromCurrentUser")
                        lastMessageCount = currentCount
                        
                        // Only call markAsRead if the message is from someone else
                        if (!isFromCurrentUser) {
                            markAsRead()
                        }
                    }
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
        timber.log.Timber.d("markAsRead: called for conv=$conversationId")
        viewModelScope.launch {
            markMessagesAsReadUseCase(conversationId)
        }
    }
    
    /**
     * Public function to mark all messages as read - called when leaving the screen
     * Uses NonCancellable to ensure the write completes even when ViewModel is destroyed
     */
    fun markAllAsRead() {
        timber.log.Timber.d("markAllAsRead: called on dispose for conv=$conversationId")
        viewModelScope.launch(kotlinx.coroutines.NonCancellable) {
            markMessagesAsReadUseCase(conversationId)
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        // Cancel any pending typing job and clear typing status immediately
        typingJob?.cancel()
        setTypingStatus(false)

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSending = true)

            val replyToId = _uiState.value.replyToMessage?.id

            when (val result = sendMessageUseCase.sendText(conversationId, text, replyToId)) {
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
        timber.log.Timber.d("revokeMessage: called with messageId=$messageId, conversationId=$conversationId")
        viewModelScope.launch {
            when (val result = messageRepository.revokeMessage(conversationId, messageId)) {
                is Result.Error -> {
                    timber.log.Timber.e("revokeMessage: FAILED - ${result.message}")
                    _uiState.value = _uiState.value.copy(error = result.message)
                }
                is Result.Success -> { 
                    timber.log.Timber.d("revokeMessage: SUCCESS")
                }
                is Result.Loading -> { /* ignore */ }
            }
        }
    }

    fun pinMessage(messageId: String) {
        viewModelScope.launch {
            timber.log.Timber.d("ViewModel: pinMessage called for $messageId in $conversationId")
            when (val result = messageRepository.pinMessage(conversationId, messageId)) {
                is Result.Error -> {
                    timber.log.Timber.e("ViewModel: pinMessage failed - ${result.message}")
                    _uiState.value = _uiState.value.copy(error = result.message)
                }
                is Result.Success -> {
                    timber.log.Timber.d("ViewModel: pinMessage success, reloading conversation")
                    // Refresh conversation to get updated pinnedMessageIds
                    refreshConversation()
                }
                is Result.Loading -> { /* ignore */ }
            }
        }
    }

    fun unpinMessage(messageId: String) {
        viewModelScope.launch {
            timber.log.Timber.d("ViewModel: unpinMessage called for $messageId in $conversationId")
            when (val result = messageRepository.unpinMessage(conversationId, messageId)) {
                is Result.Error -> {
                    timber.log.Timber.e("ViewModel: unpinMessage failed - ${result.message}")
                    _uiState.value = _uiState.value.copy(error = result.message)
                }
                is Result.Success -> {
                    timber.log.Timber.d("ViewModel: unpinMessage success, reloading conversation")
                    // Refresh conversation to get updated pinnedMessageIds
                    refreshConversation()
                }
                is Result.Loading -> { /* ignore */ }
            }
        }
    }
    
    private suspend fun refreshConversation() {
        when (val result = conversationRepository.getConversation(conversationId)) {
            is Result.Success -> {
                timber.log.Timber.d("refreshConversation: pinnedMessageIds = ${result.data.pinnedMessageIds}")
                _uiState.value = _uiState.value.copy(conversation = result.data)
            }
            is Result.Error -> {
                timber.log.Timber.e("refreshConversation failed: ${result.message}")
            }
            is Result.Loading -> { /* ignore */ }
        }
    }

    // Reaction functions
    fun addReaction(messageId: String, emoji: String) {
        viewModelScope.launch {
            timber.log.Timber.d("Adding reaction $emoji to message $messageId")
            when (val result = messageRepository.addReaction(conversationId, messageId, emoji)) {
                is Result.Error -> {
                    timber.log.Timber.e("addReaction failed: ${result.message}")
                    _uiState.value = _uiState.value.copy(error = result.message)
                }
                is Result.Success -> {
                    timber.log.Timber.d("Reaction $emoji added successfully")
                }
                is Result.Loading -> { /* ignore */ }
            }
        }
    }

    fun removeReaction(messageId: String, emoji: String) {
        viewModelScope.launch {
            timber.log.Timber.d("Removing reaction $emoji from message $messageId")
            when (val result = messageRepository.removeReaction(conversationId, messageId, emoji)) {
                is Result.Error -> {
                    timber.log.Timber.e("removeReaction failed: ${result.message}")
                    _uiState.value = _uiState.value.copy(error = result.message)
                }
                is Result.Success -> {
                    timber.log.Timber.d("Reaction $emoji removed successfully")
                }
                is Result.Loading -> { /* ignore */ }
            }
        }
    }

    fun toggleReaction(messageId: String, emoji: String) {
        viewModelScope.launch {
            val currentUserId = auth.currentUser?.uid
            timber.log.Timber.d("REACTION: ========================================")
            timber.log.Timber.d("REACTION: toggleReaction called")
            timber.log.Timber.d("REACTION: messageId=$messageId, emoji=$emoji")
            timber.log.Timber.d("REACTION: currentUserId=$currentUserId")
            
            if (currentUserId == null) {
                timber.log.Timber.e("REACTION: ERROR - No current user!")
                return@launch
            }
            
            // Find the message
            val message = _uiState.value.messages.find { it.id == messageId }
            if (message == null) {
                timber.log.Timber.e("REACTION: ERROR - Message not found!")
                return@launch
            }
            
            // Log current reactions state
            timber.log.Timber.d("REACTION: Current reactions on message: ${message.reactions}")
            
            val usersWithThisEmoji = message.reactions[emoji] ?: emptyList()
            timber.log.Timber.d("REACTION: Users with $emoji: $usersWithThisEmoji")
            timber.log.Timber.d("REACTION: Current user in list: ${usersWithThisEmoji.contains(currentUserId)}")
            
            if (usersWithThisEmoji.contains(currentUserId)) {
                // User clicked same emoji - remove it (toggle off)
                timber.log.Timber.d("REACTION: ACTION = REMOVE (same emoji clicked)")
                val result = messageRepository.removeReaction(conversationId, messageId, emoji)
                timber.log.Timber.d("REACTION: Remove result: $result")
            } else {
                // Find if user has any existing reaction on this message
                var existingReactionEmoji: String? = null
                message.reactions.forEach { (emojiKey, userIds) ->
                    timber.log.Timber.d("REACTION: Checking $emojiKey -> $userIds")
                    if (userIds.contains(currentUserId)) {
                        timber.log.Timber.d("REACTION: Found existing reaction: $emojiKey")
                        existingReactionEmoji = emojiKey
                    }
                }
                
                // If user already has a reaction, remove it first
                if (existingReactionEmoji != null) {
                    timber.log.Timber.d("REACTION: ACTION = REMOVE OLD ($existingReactionEmoji)")
                    val removeResult = messageRepository.removeReaction(conversationId, messageId, existingReactionEmoji!!)
                    timber.log.Timber.d("REACTION: Remove old result: $removeResult")
                }
                
                // Then add new reaction
                timber.log.Timber.d("REACTION: ACTION = ADD NEW ($emoji)")
                val addResult = messageRepository.addReaction(conversationId, messageId, emoji)
                timber.log.Timber.d("REACTION: Add result: $addResult")
            }
            timber.log.Timber.d("REACTION: ========================================")
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
