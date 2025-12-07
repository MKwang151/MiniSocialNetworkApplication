package com.example.minisocialnetworkapplication.ui.chat

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

sealed class StartChatUiState {
    data object Loading : StartChatUiState()
    data class Success(val conversationId: String) : StartChatUiState()
    data class Error(val message: String) : StartChatUiState()
}

@HiltViewModel
class StartChatViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val conversationRepository: ConversationRepository
) : ViewModel() {

    private val userId: String = savedStateHandle.get<String>("userId") ?: ""

    private val _uiState = MutableStateFlow<StartChatUiState>(StartChatUiState.Loading)
    val uiState: StateFlow<StartChatUiState> = _uiState.asStateFlow()

    init {
        if (userId.isNotBlank()) {
            createOrGetConversation()
        } else {
            _uiState.value = StartChatUiState.Error("Invalid user ID")
        }
    }

    private fun createOrGetConversation() {
        viewModelScope.launch {
            when (val result = conversationRepository.getOrCreateDirectConversation(userId)) {
                is Result.Success -> {
                    _uiState.value = StartChatUiState.Success(result.data.id)
                }
                is Result.Error -> {
                    _uiState.value = StartChatUiState.Error(result.message ?: "Failed to start conversation")
                }
                is Result.Loading -> {
                    _uiState.value = StartChatUiState.Loading
                }
            }
        }
    }
}
