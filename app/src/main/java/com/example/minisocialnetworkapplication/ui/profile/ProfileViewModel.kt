package com.example.minisocialnetworkapplication.ui.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.minisocialnetworkapplication.core.domain.model.Post
import com.example.minisocialnetworkapplication.core.domain.model.User
import com.example.minisocialnetworkapplication.core.domain.repository.PostRepository
import com.example.minisocialnetworkapplication.core.domain.repository.UserRepository
import com.example.minisocialnetworkapplication.core.domain.usecase.post.ToggleLikeUseCase
import com.example.minisocialnetworkapplication.core.util.Result
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data class Success(val user: User, val isOwnProfile: Boolean) : ProfileUiState
    data class Error(val message: String) : ProfileUiState
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val postRepository: PostRepository,
    private val toggleLikeUseCase: ToggleLikeUseCase,
    private val auth: FirebaseAuth,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val userId: String = checkNotNull(savedStateHandle["userId"])
    private val currentUserId = auth.currentUser?.uid

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    val userPostsFlow: Flow<PagingData<Post>> = postRepository
        .getUserPosts(userId)
        .cachedIn(viewModelScope)

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            try {
                _uiState.value = ProfileUiState.Loading

                when (val result = userRepository.getUser(userId)) {
                    is Result.Success -> {
                        _uiState.value = ProfileUiState.Success(
                            user = result.data,
                            isOwnProfile = userId == currentUserId
                        )
                    }
                    is Result.Error -> {
                        _uiState.value = ProfileUiState.Error(
                            result.exception?.message ?: "Failed to load profile"
                        )
                    }
                    is Result.Loading -> {
                        // Already in loading state
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading profile")
                _uiState.value = ProfileUiState.Error(
                    e.message ?: "Unknown error"
                )
            }
        }
    }

    fun toggleLike(post: Post) {
        viewModelScope.launch {
            when (val result = toggleLikeUseCase(post.id)) {
                is Result.Success -> {
                    Timber.d("Post ${post.id} like toggled")
                }
                is Result.Error -> {
                    Timber.e("Failed to toggle like: ${result.exception?.message}")
                }
                is Result.Loading -> {
                    // Loading
                }
            }
        }
    }

    fun refresh() {
        loadProfile()
    }
}

