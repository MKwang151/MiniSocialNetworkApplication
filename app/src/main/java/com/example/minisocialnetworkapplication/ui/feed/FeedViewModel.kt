package com.example.minisocialnetworkapplication.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.example.minisocialnetworkapplication.core.domain.model.Post
import com.example.minisocialnetworkapplication.core.domain.usecase.post.GetFeedPagingUseCase
import com.example.minisocialnetworkapplication.core.domain.usecase.post.ToggleLikeUseCase
import com.example.minisocialnetworkapplication.core.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

sealed interface FeedUiState {
    data object Idle : FeedUiState
    data class Error(val message: String) : FeedUiState
}

@HiltViewModel
class FeedViewModel @Inject constructor(
    private val getFeedPagingUseCase: GetFeedPagingUseCase,
    private val toggleLikeUseCase: ToggleLikeUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<FeedUiState>(FeedUiState.Idle)
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    // Optimistic like updates - track posts being liked
    private val _optimisticLikes = MutableStateFlow<Map<String, Post>>(emptyMap())

    val feedPagingFlow: Flow<PagingData<Post>> = getFeedPagingUseCase()
        .cachedIn(viewModelScope)
        .combine(_optimisticLikes) { pagingData, optimisticLikes ->
            pagingData.map { post ->
                optimisticLikes[post.id] ?: post
            }
        }

    fun toggleLike(post: Post) {
        viewModelScope.launch {
            val postId = post.id
            val optimisticPost = _optimisticLikes.value[postId] ?: post
            val newLikedState = !optimisticPost.likedByMe
            val newLikeCount = if (newLikedState) optimisticPost.likeCount + 1 else optimisticPost.likeCount - 1

            // Update optimistic state immediately with new Post object
            val updatedPost = optimisticPost.copy(
                likedByMe = newLikedState,
                likeCount = newLikeCount
            )
            _optimisticLikes.value = _optimisticLikes.value + (postId to updatedPost)

            Timber.d("Optimistic like: postId=$postId, newState=$newLikedState, newCount=$newLikeCount")

            // Execute actual like toggle
            when (val result = toggleLikeUseCase(postId)) {
                is Result.Success -> {
                    // Success - remove from optimistic map, let real data show
                    val serverState = result.data
                    Timber.d("Like synced successfully: postId=$postId, state=$serverState")
                    _optimisticLikes.value = _optimisticLikes.value - postId
                }
                is Result.Error -> {
                    // Rollback optimistic update
                    _optimisticLikes.value = _optimisticLikes.value - postId
                    _uiState.value = FeedUiState.Error(result.message ?: "Failed to toggle like")
                    Timber.e("Like failed: ${result.message}")
                }
                is Result.Loading -> {
                    // Should not happen in UseCase result
                }
            }
        }
    }

    fun clearError() {
        _uiState.value = FeedUiState.Idle
    }
}

