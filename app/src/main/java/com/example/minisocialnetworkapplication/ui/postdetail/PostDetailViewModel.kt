package com.example.minisocialnetworkapplication.ui.postdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.minisocialnetworkapplication.core.domain.model.Comment
import com.example.minisocialnetworkapplication.core.domain.model.Post
import com.example.minisocialnetworkapplication.core.domain.usecase.comment.AddCommentUseCase
import com.example.minisocialnetworkapplication.core.domain.usecase.comment.GetCommentsUseCase
import com.example.minisocialnetworkapplication.core.domain.usecase.post.ToggleLikeUseCase
import com.example.minisocialnetworkapplication.core.domain.repository.PostRepository
import com.example.minisocialnetworkapplication.core.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
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

                when (val result = addCommentUseCase(postId, text)) {
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
                // Call repository to delete comment
                // For now, we'll use Firestore directly since we don't have a repository method
                val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                val currentUserId = auth.currentUser?.uid

                if (currentUserId == null) {
                    Timber.e("User not authenticated")
                    return@launch
                }

                // Get comment to check ownership
                val commentRef = firestore.collection("posts")
                    .document(postId)
                    .collection("comments")
                    .document(commentId)

                val commentDoc = commentRef.get().await()

                if (!commentDoc.exists()) {
                    Timber.e("Comment not found")
                    return@launch
                }

                val commentAuthorId = commentDoc.getString("authorId")
                val postAuthorId = currentPost?.authorId

                // Check if user can delete: must be comment owner OR post owner
                if (commentAuthorId != currentUserId && postAuthorId != currentUserId) {
                    Timber.e("Not authorized to delete this comment")
                    return@launch
                }

                // Delete comment from Firestore
                commentRef.delete().await()
                Timber.d("Comment deleted successfully")

                // Update post comment count
                val postRef = firestore.collection("posts").document(postId)
                postRef.update(
                    "commentCount",
                    com.google.firebase.firestore.FieldValue.increment(-1)
                ).await()
                Timber.d("Post comment count decremented")

                // Reload post to update UI
                when (val postResult = postRepository.getPost(postId)) {
                    is Result.Success -> {
                        currentPost = postResult.data
                        val currentState = _uiState.value
                        if (currentState is PostDetailUiState.Success) {
                            _uiState.value = currentState.copy(post = postResult.data)
                        }
                    }
                    is Result.Error -> {
                        Timber.w("Failed to reload post after deleting comment")
                    }
                    is Result.Loading -> {}
                }
            } catch (e: Exception) {
                Timber.e(e, "Error deleting comment")
            }
        }
    }
}
