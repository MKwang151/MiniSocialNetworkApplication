package com.example.minisocialnetworkapplication.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.minisocialnetworkapplication.core.domain.model.Message
import com.example.minisocialnetworkapplication.core.domain.repository.MessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface MessageSearchUiState {
    data object Idle : MessageSearchUiState
    data object Loading : MessageSearchUiState
    data class Success(val messages: List<Message>) : MessageSearchUiState
    data class Error(val message: String) : MessageSearchUiState
}

@HiltViewModel
class MessageSearchViewModel @Inject constructor(
    private val messageRepository: MessageRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    @OptIn(FlowPreview::class)
    fun searchMessages(conversationId: String): StateFlow<MessageSearchUiState> {
        return _searchQuery
            .debounce(300)
            .flatMapLatest { query ->
                if (query.isBlank()) {
                    flowOf(MessageSearchUiState.Idle)
                } else {
                    try {
                        messageRepository.searchMessages(conversationId, query)
                            .combine(flowOf(query)) { messages, _ ->
                                if (messages.isEmpty()) {
                                    MessageSearchUiState.Success(emptyList())
                                } else {
                                    // Robust Deduplication Logic
                                    val deduplicated = messages
                                        .groupBy { it.content to it.senderId } // Group by content and sender
                                        .flatMap { (_, msgs) ->
                                            if (msgs.size == 1) return@flatMap msgs
                                            
                                            // Sort by timestamp to find close messages
                                            msgs.sortedBy { it.timestamp.seconds }
                                                .fold(mutableListOf<Message>()) { acc, msg ->
                                                    val last = acc.lastOrNull()
                                                    if (last != null && kotlin.math.abs(msg.timestamp.seconds - last.timestamp.seconds) < 60) {
                                                        // It's a duplicate (within 60 seconds)
                                                        // Keep the one that looks like a Firestore ID (shorter, usually 20 chars vs 36 uuid)
                                                        // or if simple heuristic: if `msg` is "better", replace `last`.
                                                        if (msg.id.length < 32 && last.id.length >= 32) {
                                                            acc.removeAt(acc.lastIndex)
                                                            acc.add(msg)
                                                        } else if (last.id.length < 32) {
                                                            // Keep last, ignore msg
                                                        } else {
                                                            // Both seem like UUIDs or both Real, just keep last one (arbitrary) or msg?
                                                            // Let's keep the one closer to "persistence" (maybe msg is newer being parsed?)
                                                            // Default keep existing (acc)
                                                        }
                                                    } else {
                                                        acc.add(msg)
                                                    }
                                                    acc
                                                }
                                        }
                                        .sortedByDescending { it.timestamp }
                                    
                                    MessageSearchUiState.Success(deduplicated)
                                }
                            }
                            .distinctUntilChanged()
                    } catch (e: Exception) {
                        flowOf(MessageSearchUiState.Error(e.message ?: "Search failed"))
                    }
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = MessageSearchUiState.Idle
            )
    }

    fun onQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }
}
