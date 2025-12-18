package com.example.minisocialnetworkapplication.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.minisocialnetworkapplication.core.domain.model.Notification
import com.example.minisocialnetworkapplication.core.domain.repository.GroupRepository
import com.example.minisocialnetworkapplication.core.domain.repository.NotificationRepository
import com.example.minisocialnetworkapplication.core.domain.usecase.auth.GetCurrentUserUseCase
import com.example.minisocialnetworkapplication.core.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class NotificationsUiState {
    data object Loading : NotificationsUiState()
    data class Success(
        val notifications: List<Notification>,
        val unreadCount: Int
    ) : NotificationsUiState()
    data class Error(val message: String) : NotificationsUiState()
}

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val groupRepository: GroupRepository,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<NotificationsUiState>(NotificationsUiState.Loading)
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    init {
        loadNotifications()
    }

    private fun loadNotifications() {
        viewModelScope.launch {
            getCurrentUserUseCase().collect { user ->
                if (user == null) {
                    _uiState.value = NotificationsUiState.Error("User not logged in")
                    return@collect
                }

                // Mark all as read when loading notifications screen
                notificationRepository.markAllAsRead(user.id)

                // Load notifications
                launch {
                    notificationRepository.getNotifications(user.id).collect { notifications ->
                        val unreadCount = notifications.count { !it.isRead }
                        
                        _uiState.value = NotificationsUiState.Success(
                            notifications = notifications,
                            unreadCount = unreadCount
                        )
                    }
                }
            }
        }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            notificationRepository.markAsRead(notificationId)
        }
    }

    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            notificationRepository.deleteNotification(notificationId)
        }
    }

    fun acceptInvitation(invitationId: String, notificationId: String) {
        viewModelScope.launch {
            when (val result = groupRepository.respondToInvitation(invitationId, accept = true)) {
                is Result.Success -> {
                    // Delete notification after accepting
                    deleteNotification(notificationId)
                }
                is Result.Error -> {
                    _uiState.value = NotificationsUiState.Error(
                        result.exception.message ?: "Failed to accept invitation"
                    )
                }
                is Result.Loading -> { /* Ignore loading state */ }
            }
        }
    }

    fun declineInvitation(invitationId: String, notificationId: String) {
        viewModelScope.launch {
            when (val result = groupRepository.respondToInvitation(invitationId, accept = false)) {
                is Result.Success -> {
                    // Delete notification after declining
                    deleteNotification(notificationId)
                }
                is Result.Error -> {
                    _uiState.value = NotificationsUiState.Error(
                        result.exception.message ?: "Failed to decline invitation"
                    )
                }
                is Result.Loading -> { /* Ignore loading state */ }
            }
        }
    }
}
