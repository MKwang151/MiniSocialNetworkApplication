package com.example.minisocialnetworkapplication.ui.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.minisocialnetworkapplication.core.domain.model.Friend
import com.example.minisocialnetworkapplication.core.domain.usecase.user.FriendUseCase
import com.example.minisocialnetworkapplication.core.util.Result
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

sealed interface FriendRequestUiState {
    data object Loading : FriendRequestUiState
    data class Success(
        val friendRequests: List<Friend>
    ) : FriendRequestUiState
    data class Error(val message: String) : FriendRequestUiState
}

sealed interface FriendUiState {
    data object Loading : FriendUiState
    data class Success(
        val friends: List<Friend>
    ) : FriendUiState
    data class Error(val message: String) : FriendUiState
}

@HiltViewModel
class FriendViewModel @Inject constructor(
    private val friendUseCase: FriendUseCase,
    auth: FirebaseAuth
): ViewModel() {

    private val currentUserId = auth.currentUser?.uid

    private val _requestUiState = MutableStateFlow<FriendRequestUiState>(FriendRequestUiState.Loading)
    val requestUiState: StateFlow<FriendRequestUiState> = _requestUiState.asStateFlow()

    private val _friendUiState = MutableStateFlow<FriendUiState>(FriendUiState.Loading)
    val friendUiState: StateFlow<FriendUiState> = _friendUiState.asStateFlow()

    init {
        loadFriendRequests()
        loadFriends()
    }

    private fun loadFriendRequests() {
        viewModelScope.launch {
            try {
                _requestUiState.value = FriendRequestUiState.Loading

                when (val result = friendUseCase.getFriendRequests()) {
                    is Result.Success -> {
                        _requestUiState.value = FriendRequestUiState.Success(
                            friendRequests = result.data
                        )
                    }
                    is Result.Error -> {
                        _requestUiState.value = FriendRequestUiState.Error(
                            result.exception.message ?: "Failed to load friend requests"
                        )
                    }
                    is Result.Loading -> {
                        // Already in loading state
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading friend requests")
                _requestUiState.value = FriendRequestUiState.Error(
                    e.message ?: "Unknown error"
                )
            }
        }
    }

    private fun loadFriends() {
        viewModelScope.launch {
            try {
                _friendUiState.value = FriendUiState.Loading

                if (currentUserId == null) { // though this shouldn't happen, but it can
                    _friendUiState.value = FriendUiState.Error("User is not authorized")
                    return@launch
                }

                when (val result = friendUseCase.getUserFriends(currentUserId)) {
                    is Result.Success -> {
                        _friendUiState.value = FriendUiState.Success(
                            friends = result.data
                        )
                    }
                    is Result.Error -> {
                        _friendUiState.value = FriendUiState.Error(
                            result.exception.message ?: "Failed to load user's friends"
                        )
                    }
                    is Result.Loading -> {
                        // Already in loading state
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading friends")
                _friendUiState.value = FriendUiState.Error(
                    e.message ?: "Unknown error"
                )
            }
        }
    }

    fun onAcceptRequest(friendId: String) {
        viewModelScope.launch {
            try {
                when (val result = friendUseCase.acceptFriendRequest(friendId)) {
                    is Result.Success -> {
                        _requestUiState.update { state ->
                            if (state is FriendRequestUiState.Success) {
                                state.copy(
                                    friendRequests = state.friendRequests.filterNot { it.friendId == friendId }
                                )
                            } else state
                        }
                        loadFriends() // Refresh friends list
                    }
                    is Result.Error -> {
                        _requestUiState.value = FriendRequestUiState.Error(
                            result.exception.message ?: "Failed to accept friend request"
                        )
                    }
                    is Result.Loading -> {
                        // Do nothing
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error accepting friend request")
                _requestUiState.value = FriendRequestUiState.Error(
                    e.message ?: "Unknown error"
                )
            }
        }
    }

    fun onDeclineRequest(friendId: String) {
        viewModelScope.launch {
            try {
                when (val result = friendUseCase.removeFriendRequest(friendId, isSender = false)) {
                    is Result.Success -> {
                        _requestUiState.update { state ->
                            if (state is FriendRequestUiState.Success) {
                                state.copy(
                                    friendRequests = state.friendRequests.filterNot { it.friendId == friendId }
                                )
                            } else state
                        }
                    }
                    is Result.Error -> {
                        _requestUiState.value = FriendRequestUiState.Error(
                            result.exception.message ?: "Failed to decline friend request of uid=$friendId"
                        )
                    }
                    is Result.Loading -> {
                        // Do nothing
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error declining friend request of uid=$friendId")
                _requestUiState.value = FriendRequestUiState.Error(
                    e.message ?: "Unknown error"
                )
            }
        }
    }

    fun onUnfriend(friendId: String) {
        viewModelScope.launch {
            try {
                when (val result = friendUseCase.unfriend(friendId)) {
                    is Result.Success -> {
                        _friendUiState.update { state ->
                            if (state is FriendUiState.Success) {
                                state.copy(
                                    friends = state.friends.filterNot { it.friendId == friendId }
                                )
                            } else state
                        }
                    }
                    is Result.Error -> {
                        _friendUiState.value = FriendUiState.Error(
                            result.exception.message ?: "Failed to unfriend uid=$friendId"
                        )
                    }
                    is Result.Loading -> {
                        // Do nothing
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error unfriending uid=$friendId")
                _friendUiState.value = FriendUiState.Error(
                    e.message ?: "Unknown error"
                )
            }
        }
    }

    fun refresh() {
        loadFriendRequests()
        loadFriends()
    }
}
