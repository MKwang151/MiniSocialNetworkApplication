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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
    private val toggleLikeUseCase: ToggleLikeUseCase,
    private val postRepository: com.example.minisocialnetworkapplication.core.domain.repository.PostRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<FeedUiState>(FeedUiState.Idle)
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    // Optimistic like updates - track posts being liked
    private val _optimisticLikes = MutableStateFlow<Map<String, Post>>(emptyMap())

    val feedPosts: Flow<PagingData<Post>> = getFeedPagingUseCase()
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

    fun deletePost(postId: String) {
        viewModelScope.launch {
            try {
                Timber.d("Deleting post: $postId")
                when (val result = postRepository.deletePost(postId)) {
                    is Result.Success -> {
                        Timber.d("Post deleted successfully from Feed")
                        // Paging will auto-refresh
                    }
                    is Result.Error -> {
                        Timber.e("Failed to delete post: ${result.message}")
                        _uiState.value = FeedUiState.Error(
                            result.message ?: "Failed to delete post"
                        )
                    }
                    is Result.Loading -> {
                        // Should not happen
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error deleting post")
                _uiState.value = FeedUiState.Error(
                    e.message ?: "Unknown error"
                )
            }
        }
    }

    fun updatePost(postId: String, newText: String) {
        viewModelScope.launch {
            try {
                if (newText.isBlank()) {
                    Timber.w("Cannot update post with empty text")
                    _uiState.value = FeedUiState.Error("Post text cannot be empty")
                    return@launch
                }

                Timber.d("Updating post: $postId from FeedScreen")
                when (val result = postRepository.updatePost(postId, newText)) {
                    is Result.Success -> {
                        Timber.d("Post updated successfully from Feed")
                        // Post will auto-refresh through paging
                    }
                    is Result.Error -> {
                        Timber.e("Failed to update post: ${result.message}")
                        _uiState.value = FeedUiState.Error(
                            result.message ?: "Failed to update post"
                        )
                    }
                    is Result.Loading -> {
                        // Should not happen
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error updating post")
                _uiState.value = FeedUiState.Error(
                    e.message ?: "Unknown error"
                )
            }
        }
    }
}
