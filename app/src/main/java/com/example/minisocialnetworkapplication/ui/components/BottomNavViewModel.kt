package com.example.minisocialnetworkapplication.ui.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.minisocialnetworkapplication.core.domain.repository.ConversationRepository
import com.example.minisocialnetworkapplication.core.domain.repository.FriendRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class BottomNavViewModel @Inject constructor(
    conversationRepository: ConversationRepository,
    friendRepository: FriendRepository
) : ViewModel() {

    val unreadMessageCount: StateFlow<Int> = conversationRepository.getConversations()
        .map { conversations ->
            conversations.sumOf { it.unreadCount }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val friendRequestCount: StateFlow<Int> = friendRepository.getFriendRequestsCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )
}
