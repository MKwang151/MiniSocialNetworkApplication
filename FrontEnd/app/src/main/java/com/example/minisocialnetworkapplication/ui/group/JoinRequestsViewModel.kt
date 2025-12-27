package com.example.minisocialnetworkapplication.ui.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.minisocialnetworkapplication.core.domain.model.User
import com.example.minisocialnetworkapplication.core.domain.repository.ConversationRepository
import com.example.minisocialnetworkapplication.core.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class JoinRequestsViewModel @Inject constructor(
    private val conversationRepository: ConversationRepository
) : ViewModel() {

    private val _requests = MutableStateFlow<List<User>>(emptyList())
    val requests = _requests.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private var currentConversationId: String? = null

    fun loadRequests(conversationId: String) {
        currentConversationId = conversationId
        viewModelScope.launch {
            conversationRepository.getJoinRequests(conversationId)
                .collectLatest {
                    _requests.value = it
                }
        }
    }

    fun acceptRequest(userId: String) {
        val conversationId = currentConversationId ?: return
        viewModelScope.launch {
            conversationRepository.acceptJoinRequest(conversationId, userId)
        }
    }

    fun declineRequest(userId: String) {
        val conversationId = currentConversationId ?: return
        viewModelScope.launch {
            conversationRepository.declineJoinRequest(conversationId, userId)
        }
    }
}
