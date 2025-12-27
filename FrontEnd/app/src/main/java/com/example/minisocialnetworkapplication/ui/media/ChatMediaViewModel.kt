package com.example.minisocialnetworkapplication.ui.media

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.minisocialnetworkapplication.core.domain.model.Message
import com.example.minisocialnetworkapplication.core.domain.repository.MessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface ChatMediaUiState {
    data object Loading : ChatMediaUiState
    data class Success(val messages: List<Message>) : ChatMediaUiState
    data class Error(val message: String) : ChatMediaUiState
}

@HiltViewModel
class ChatMediaViewModel @Inject constructor(
    private val messageRepository: MessageRepository
) : ViewModel() {

    fun getMediaMessages(conversationId: String): StateFlow<ChatMediaUiState> {
        return messageRepository.getMediaMessages(conversationId)
            .map { messages ->
                if (messages.isEmpty()) {
                    ChatMediaUiState.Success(emptyList())
                } else {
                    ChatMediaUiState.Success(messages)
                } as ChatMediaUiState
            }
            .catch { e ->
                emit(ChatMediaUiState.Error(e.message ?: "Failed to load media"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = ChatMediaUiState.Loading
            )
    }
}
