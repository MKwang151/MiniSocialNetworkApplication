package com.example.minisocialnetworkapplication.ui.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
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
    private val postRepository: com.example.minisocialnetworkapplication.core.domain.repository.PostRepository,
    private val getCurrentUserUseCase: com.example.minisocialnetworkapplication.core.domain.usecase.auth.GetCurrentUserUseCase,
    private val cacheSyncUtil: com.example.minisocialnetworkapplication.core.util.CacheSyncUtil,
    private val groupRepository: com.example.minisocialnetworkapplication.core.domain.repository.GroupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<FeedUiState>(FeedUiState.Idle)
    val uiState: StateFlow<FeedUiState> = _uiState.asStateFlow()

    // Optimistic like updates - track posts being liked
    private val _optimisticLikes = MutableStateFlow<Map<String, Post>>(emptyMap())
    
    // Track user's group memberships for filtering private group posts
    private val _userGroupIds = MutableStateFlow<Set<String>>(emptySet())
    private var currentUserId: String? = null

    val feedPosts: Flow<PagingData<Post>> = getFeedPagingUseCase()
        .cachedIn(viewModelScope)
        .combine(_optimisticLikes) { pagingData, optimisticLikes ->
            pagingData.map { post ->
                optimisticLikes[post.id] ?: post
            }
        }
        .combine(_userGroupIds) { pagingData, userGroupIds ->
            // Filter out posts that should not be shown
            pagingData.filter { post ->
                // Filter 1: Hide posts with PENDING or HIDDEN approval status
                val isApproved = post.approvalStatus == com.example.minisocialnetworkapplication.core.domain.model.PostApprovalStatus.APPROVED
                
                // Filter 2: Hide explicitly hidden posts
                val notHidden = !post.isHidden
                
                // Filter 3: For group posts, only show if user is member (for private groups)
                // We can't check privacy here easily, so we trust the RemoteMediator to have filtered properly
                // However, as an extra safety check: if post has groupId and user is not member, hide it
                // (This handles edge cases where cache wasn't properly cleaned)
                val groupAccessible = if (post.groupId != null) {
                    userGroupIds.contains(post.groupId) || post.authorId == currentUserId
                } else {
                    true // Non-group posts are always visible
                }
                
                isApproved && notHidden && groupAccessible
            }
        }

    init {
        loadUserGroupMemberships()
    }
    
    private fun loadUserGroupMemberships() {
        viewModelScope.launch {
            // Get current user ID using the sync method
            val userId = getCurrentUserUseCase.getUserId()
            if (userId != null) {
                currentUserId = userId
                Timber.d("Loaded current user ID: $currentUserId")
                
                // Get user's group memberships
                groupRepository.getGroupsForUser(userId).collect { groups ->
                    _userGroupIds.value = groups.map { it.id }.toSet()
                    Timber.d("Loaded ${groups.size} group memberships for filtering")
                }
            } else {
                Timber.w("Current user ID is null, cannot load group memberships")
            }
        }
    }

    fun syncCache() {
        viewModelScope.launch {
            Timber.d("Starting background cache sync from FeedViewModel")
            when (val result = cacheSyncUtil.syncPostsFromFirebase(limit = 50)) {
                is Result.Success -> {
                    Timber.d("Cache sync successful: ${result.data} posts updated")
                    // If sync updated the database, the PagingSource should reflect this 
                    // on next load or if we explicitly invalidate it.
                    // For now, syncPostsFromFirebase already clears and re-inserts.
                }
                is Result.Error -> {
                    Timber.e("Cache sync failed: ${result.message}")
                }
                else -> {}
            }
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
            _optimisticLikes.value += (postId to updatedPost)

            Timber.d("Optimistic like: postId=$postId, newState=$newLikedState, newCount=$newLikeCount")

            // Execute actual like toggle
            when (val result = toggleLikeUseCase(postId)) {
                is Result.Success -> {
                    // Success - remove from optimistic map, let real data show
                    val serverState = result.data
                    Timber.d("Like synced successfully: postId=$postId, state=$serverState")
                    _optimisticLikes.value -= postId
                }
                is Result.Error -> {
                    // Rollback optimistic update
                    _optimisticLikes.value -= postId
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
