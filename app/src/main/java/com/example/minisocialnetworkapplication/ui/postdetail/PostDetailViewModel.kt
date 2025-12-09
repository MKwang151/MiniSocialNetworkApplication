package com.example.minisocialnetworkapplication.ui.postdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.minisocialnetworkapplication.core.domain.model.Comment
import com.example.minisocialnetworkapplication.core.domain.model.Post
import com.example.minisocialnetworkapplication.core.domain.repository.PostRepository
import com.example.minisocialnetworkapplication.core.domain.usecase.comment.AddCommentUseCase
import com.example.minisocialnetworkapplication.core.domain.usecase.comment.GetCommentsUseCase
import com.example.minisocialnetworkapplication.core.domain.usecase.post.ToggleLikeUseCase
import com.example.minisocialnetworkapplication.core.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

sealed interface PostDetailUiState {
    data object Loading : PostDetailUiState
    data class Success(
        val post: Post,
        val comments: List<Comment>
    ) : PostDetailUiState
    data class Error(val message: String) : PostDetailUiState
}

@HiltViewModel
class PostDetailViewModel @Inject constructor(
    private val postRepository: PostRepository,
    private val commentRepository: com.example.minisocialnetworkapplication.core.domain.repository.CommentRepository,
    private val getCommentsUseCase: GetCommentsUseCase,
    private val addCommentUseCase: AddCommentUseCase,
    private val toggleLikeUseCase: ToggleLikeUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val postId: String = checkNotNull(savedStateHandle["postId"])

    private val _uiState = MutableStateFlow<PostDetailUiState>(PostDetailUiState.Loading)
    val uiState: StateFlow<PostDetailUiState> = _uiState.asStateFlow()

    private val _commentText = MutableStateFlow("")
    val commentText: StateFlow<String> = _commentText.asStateFlow()

    private val _isAddingComment = MutableStateFlow(false)
    val isAddingComment: StateFlow<Boolean> = _isAddingComment.asStateFlow()

    private val _deletionSuccess = MutableStateFlow(false)
    val deletionSuccess: StateFlow<Boolean> = _deletionSuccess.asStateFlow()

    // Reply mode state
    private val _replyToComment = MutableStateFlow<Comment?>(null)
    val replyToComment: StateFlow<Comment?> = _replyToComment.asStateFlow()

    // Current user ID for reactions
    val currentUserId: String? = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid

    private var currentPost: Post? = null

    init {
        loadPostAndComments()
    }

    private fun loadPostAndComments() {
        viewModelScope.launch {
            try {
                _uiState.value = PostDetailUiState.Loading

                // Load post
                when (val postResult = postRepository.getPost(postId)) {
                    is Result.Success -> {
                        currentPost = postResult.data

                        // Listen to comments
                        getCommentsUseCase(postId).collect { comments ->
                            currentPost?.let { post ->
                                _uiState.value = PostDetailUiState.Success(
                                    post = post,
                                    comments = comments
                                )
                            }
                        }
                    }
                    is Result.Error -> {
                        _uiState.value = PostDetailUiState.Error(
                            postResult.exception?.message ?: "Failed to load post"
                        )
                    }
                    is Result.Loading -> {
                        // Already in loading state
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading post detail")
                _uiState.value = PostDetailUiState.Error(
                    e.message ?: "Unknown error"
                )
            }
        }
    }

    fun updateCommentText(text: String) {
        _commentText.value = text
    }

    fun addComment() {
        val text = _commentText.value.trim()
        if (text.isEmpty()) return

        viewModelScope.launch {
            try {
                _isAddingComment.value = true

                // Check if replying to a comment
                val replyTo = _replyToComment.value
                val result = if (replyTo != null) {
                    commentRepository.addReplyComment(postId, text, replyTo.id, replyTo.authorName)
                } else {
                    addCommentUseCase(postId, text)
                }

                when (result) {
                    is Result.Success -> {
                        _commentText.value = ""
                        Timber.d("Comment added successfully")

                        // Reload post to get updated comment count
                        when (val postResult = postRepository.getPost(postId)) {
                            is Result.Success -> {
                                currentPost = postResult.data
                                val currentState = _uiState.value
                                if (currentState is PostDetailUiState.Success) {
                                    _uiState.value = currentState.copy(post = postResult.data)
                                    Timber.d("Post comment count updated in UI")
                                }
                            }
                            is Result.Error -> {
                                Timber.w("Failed to reload post after adding comment")
                            }
                            is Result.Loading -> {
                                // Ignore
                            }
                        }
                    }
                    is Result.Error -> {
                        Timber.e("Failed to add comment: ${result.exception?.message}")
                        // Could show error to user here
                    }
                    is Result.Loading -> {
                        // Already showing loading
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error adding comment")
            } finally {
                _isAddingComment.value = false
                _replyToComment.value = null // Clear reply mode after sending
            }
        }
    }

    fun setReplyToComment(comment: Comment) {
        _replyToComment.value = comment
    }

    fun clearReply() {
        _replyToComment.value = null
    }

    fun toggleCommentReaction(commentId: String, emoji: String) {
        val state = _uiState.value as? PostDetailUiState.Success ?: return
        val comment = state.comments.find { it.id == commentId } ?: return
        val userId = currentUserId ?: return

        viewModelScope.launch {
            // Check if user already reacted with this emoji
            val hasReacted = comment.reactions[emoji]?.contains(userId) == true
            
            // Check if user has any other reaction
            val existingReaction = comment.reactions.entries.find { (_, users) -> 
                users.contains(userId) 
            }?.key

            if (hasReacted) {
                // Remove existing reaction
                commentRepository.removeReaction(postId, commentId, emoji)
            } else {
                // Remove old reaction if exists (one reaction per user)
                if (existingReaction != null && existingReaction != emoji) {
                    commentRepository.removeReaction(postId, commentId, existingReaction)
                }
                // Add new reaction
                commentRepository.addReaction(postId, commentId, emoji)
            }
        }
    }

    fun toggleLike() {
        val post = currentPost ?: return

        viewModelScope.launch {
            // Optimistic update
            val updatedPost = post.copy(
                likedByMe = !post.likedByMe,
                likeCount = if (post.likedByMe) post.likeCount - 1 else post.likeCount + 1
            )
            currentPost = updatedPost

            // Update UI immediately
            val currentState = _uiState.value
            if (currentState is PostDetailUiState.Success) {
                _uiState.value = currentState.copy(post = updatedPost)
            }

            // Sync with server
            when (val result = toggleLikeUseCase(post.id)) {
                is Result.Success -> {
                    Timber.d("Like toggled successfully")
                }
                is Result.Error -> {
                    Timber.e("Failed to toggle like: ${result.exception?.message}")
                    // Rollback optimistic update
                    currentPost = post
                    if (currentState is PostDetailUiState.Success) {
                        _uiState.value = currentState.copy(post = post)
                    }
                }
                is Result.Loading -> {
                    // Already updated UI optimistically
                }
            }
        }
    }

    fun refresh() {
        loadPostAndComments()
    }
    fun deletePost() {
        viewModelScope.launch {
            try {
                Timber.d("Deleting post: $postId")
                when (val result = postRepository.deletePost(postId)) {
                    is Result.Success -> {
                        Timber.d("Post deleted successfully")
                        // Set flag to trigger navigation after deletion completes
                        _deletionSuccess.value = true
                    }
                    is Result.Error -> {
                        Timber.e("Failed to delete post: ${result.message}")
                        _uiState.value = PostDetailUiState.Error(
                            result.message ?: "Failed to delete post"
                        )
                    }
                    is Result.Loading -> {
                        // Should not happen
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error deleting post")
                _uiState.value = PostDetailUiState.Error(
                    e.message ?: "Unknown error"
                )
            }
        }
    }

    fun deleteComment(commentId: String) {
        viewModelScope.launch {
            try {
                Timber.d("Deleting comment: $commentId from post: $postId")

                // Use CommentRepository which properly updates both Firestore and Room cache
                when (val result = commentRepository.deleteComment(postId, commentId)) {
                    is Result.Success -> {
                        Timber.d("Comment deleted successfully")

                        // Optimistically update UI by removing comment from current state
                        val currentState = _uiState.value
                        if (currentState is PostDetailUiState.Success) {
                            val updatedComments = currentState.comments.filter { it.id != commentId }
                            val updatedPost = currentState.post.copy(
                                commentCount = (currentState.post.commentCount - 1).coerceAtLeast(0)
                            )
                            currentPost = updatedPost
                            _uiState.value = currentState.copy(
                                post = updatedPost,
                                comments = updatedComments
                            )
                            Timber.d("UI updated optimistically after comment deletion")
                        }

                        // Reload post from cache to ensure consistency
                        when (val postResult = postRepository.getPost(postId)) {
                            is Result.Success -> {
                                currentPost = postResult.data
                                val state = _uiState.value
                                if (state is PostDetailUiState.Success) {
                                    _uiState.value = state.copy(post = postResult.data)
                                    Timber.d("Post reloaded from cache after comment deletion")
                                }
                            }
                            is Result.Error -> {
                                Timber.w("Failed to reload post from cache")
                            }
                            is Result.Loading -> {}
                        }
                    }
                    is Result.Error -> {
                        Timber.e("Failed to delete comment: ${result.exception?.message}")
                        _uiState.value = PostDetailUiState.Error(
                            result.exception?.message ?: "Failed to delete comment"
                        )
                    }
                    is Result.Loading -> {}
                }
            } catch (e: Exception) {
                Timber.e(e, "Error deleting comment")
                _uiState.value = PostDetailUiState.Error(
                    e.message ?: "Unknown error deleting comment"
                )
            }
        }
    }

    fun updatePost(newText: String) {
        viewModelScope.launch {
            try {
                if (newText.isBlank()) {
                    Timber.w("Cannot update post with empty text")
                    return@launch
                }

                Timber.d("Updating post: $postId with new text")
                when (val result = postRepository.updatePost(postId, newText)) {
                    is Result.Success -> {
                        Timber.d("Post updated successfully")
                        // Reload post to get updated data
                        loadPostAndComments()
                    }
                    is Result.Error -> {
                        Timber.e("Failed to update post: ${result.message}")
                        _uiState.value = PostDetailUiState.Error(
                            result.message ?: "Failed to update post"
                        )
                    }
                    is Result.Loading -> {}
                }
            } catch (e: Exception) {
                Timber.e(e, "Error updating post")
                _uiState.value = PostDetailUiState.Error(
                    e.message ?: "Unknown error"
                )
            }
        }
    }

    fun updateComment(commentId: String, newText: String) {
        viewModelScope.launch {
            try {
                if (newText.isBlank()) {
                    Timber.w("Cannot update comment with empty text")
                    return@launch
                }

                Timber.d("Updating comment: $commentId with new text")

                // Use CommentRepository which properly handles authorization and updates
                when (val result = commentRepository.updateComment(postId, commentId, newText)) {
                    is Result.Success -> {
                        Timber.d("Comment updated successfully")

                        // Optimistically update UI
                        val currentState = _uiState.value
                        if (currentState is PostDetailUiState.Success) {
                            val updatedComments = currentState.comments.map { comment ->
                                if (comment.id == commentId) {
                                    comment.copy(text = newText)
                                } else {
                                    comment
                                }
                            }
                            _uiState.value = currentState.copy(comments = updatedComments)
                            Timber.d("UI updated optimistically after comment edit")
                        }
                    }
                    is Result.Error -> {
                        Timber.e("Failed to update comment: ${result.exception?.message}")
                        _uiState.value = PostDetailUiState.Error(
                            result.exception?.message ?: "Failed to update comment"
                        )
                    }
                    is Result.Loading -> {}
                }
            } catch (e: Exception) {
                Timber.e(e, "Error updating comment")
                _uiState.value = PostDetailUiState.Error(
                    e.message ?: "Unknown error updating comment"
                )
            }
        }
    }
}
