package com.example.minisocialnetworkapplication.ui.socialgroup

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.minisocialnetworkapplication.core.domain.model.Group
import com.example.minisocialnetworkapplication.core.domain.model.Post
import com.example.minisocialnetworkapplication.core.domain.repository.GroupRepository
import com.example.minisocialnetworkapplication.core.domain.repository.PostRepository
import com.example.minisocialnetworkapplication.core.domain.usecase.post.ToggleLikeUseCase
import com.example.minisocialnetworkapplication.core.util.Result

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

sealed interface GroupDetailUiState {
    data object Loading : GroupDetailUiState
    data class Success(
        val group: Group,
        val isMember: Boolean = false,
        val userRole: com.example.minisocialnetworkapplication.core.domain.model.GroupRole? = null,
        val posts: List<Post> = emptyList(),
        val currentUserId: String? = null
    ) : GroupDetailUiState
    data class Error(val message: String) : GroupDetailUiState
}

@HiltViewModel
class GroupDetailViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    private val postRepository: PostRepository,
    private val toggleLikeUseCase: ToggleLikeUseCase,
    private val getCurrentUserUseCase: com.example.minisocialnetworkapplication.core.domain.usecase.auth.GetCurrentUserUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val groupId: String = checkNotNull(savedStateHandle["groupId"])

    private val _uiState = MutableStateFlow<GroupDetailUiState>(GroupDetailUiState.Loading)
    val uiState: StateFlow<GroupDetailUiState> = _uiState.asStateFlow()

    // Track optimistic like updates
    private val _optimisticLikes = MutableStateFlow<Map<String, Post>>(emptyMap())

    // Error message for snackbar
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    // Success message for snackbar (e.g., join request submitted)
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private var currentUserId: String? = null

    init {
        loadGroupDetails()
    }

    fun clearError() {
        _errorMessage.value = null
    }
    
    fun clearSuccessMessage() {
        _successMessage.value = null
    }

    // ========== Post Actions ==========

    fun toggleLike(post: Post) {
        viewModelScope.launch {
            val postId = post.id
            val optimisticPost = _optimisticLikes.value[postId] ?: post
            val newLikedState = !optimisticPost.likedByMe
            val newLikeCount = if (newLikedState) optimisticPost.likeCount + 1 else optimisticPost.likeCount - 1

            // Optimistic update
            val updatedPost = optimisticPost.copy(
                likedByMe = newLikedState,
                likeCount = newLikeCount
            )
            _optimisticLikes.value += (postId to updatedPost)

            // Update UI immediately
            updatePostInState(updatedPost)

            // Sync with server
            when (val result = toggleLikeUseCase(postId)) {
                is Result.Success -> {
                    Timber.d("Like synced: postId=$postId")
                    _optimisticLikes.value -= postId
                }
                is Result.Error -> {
                    // Rollback
                    _optimisticLikes.value -= postId
                    updatePostInState(post) // Revert to original
                    _errorMessage.value = result.message ?: "Failed to toggle like"
                }
                else -> {}
            }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            when (val result = postRepository.deletePost(postId)) {
                is Result.Success -> {
                    Timber.d("Post deleted: $postId")
                    // Remove from local state
                    val current = _uiState.value
                    if (current is GroupDetailUiState.Success) {
                        _uiState.value = current.copy(
                            posts = current.posts.filter { it.id != postId }
                        )
                    }
                }
                is Result.Error -> {
                    _errorMessage.value = result.message ?: "Failed to delete post"
                }
                else -> {}
            }
        }
    }

    fun updatePost(postId: String, newText: String) {
        viewModelScope.launch {
            if (newText.isBlank()) {
                _errorMessage.value = "Post text cannot be empty"
                return@launch
            }

            when (val result = postRepository.updatePost(postId, newText)) {
                is Result.Success -> {
                    Timber.d("Post updated: $postId")
                    // Update in local state
                    val current = _uiState.value
                    if (current is GroupDetailUiState.Success) {
                        _uiState.value = current.copy(
                            posts = current.posts.map {
                                if (it.id == postId) it.copy(text = newText) else it
                            }
                        )
                    }
                }
                is Result.Error -> {
                    _errorMessage.value = result.message ?: "Failed to update post"
                }
                else -> {}
            }
        }
    }

    private fun updatePostInState(updatedPost: Post) {
        val current = _uiState.value
        if (current is GroupDetailUiState.Success) {
            _uiState.value = current.copy(
                posts = current.posts.map { 
                    if (it.id == updatedPost.id) updatedPost else it 
                }
            )
        }
    }

    // ========== Group Actions ==========

    private fun loadGroupDetails() {
        viewModelScope.launch {
            _uiState.value = GroupDetailUiState.Loading
            
            val groupResult = groupRepository.getGroupDetails(groupId)
            if (groupResult is Result.Error) {
                _uiState.value = GroupDetailUiState.Error(groupResult.message ?: "Failed to load group")
                return@launch
            }
            
            val group = (groupResult as Result.Success).data
            
            val currentUser = getCurrentUserUseCase().firstOrNull()
            currentUserId = currentUser?.uid
            
            var isMember = false
            var userRole: com.example.minisocialnetworkapplication.core.domain.model.GroupRole? = null
            
            if (currentUser != null) {
                val memberResult = groupRepository.isMember(groupId, currentUser.id)
                if (memberResult is Result.Success) {
                    isMember = memberResult.data
                }
                
                if (isMember) {
                    val roleResult = groupRepository.getMemberRole(groupId, currentUser.id)
                    if (roleResult is Result.Success) {
                        userRole = roleResult.data
                    }
                }
            }

            _uiState.value = GroupDetailUiState.Success(
                group = group,
                isMember = isMember,
                userRole = userRole,
                currentUserId = currentUserId
            )

            if (group.privacy == com.example.minisocialnetworkapplication.core.domain.model.GroupPrivacy.PUBLIC || isMember) {
                 loadPosts()
            }
        }
    }

    private fun loadPosts() {
        viewModelScope.launch {
            groupRepository.getGroupPosts(groupId).collect { posts ->
                val current = _uiState.value
                if (current is GroupDetailUiState.Success) {
                    // Apply any optimistic updates
                    val updatedPosts = posts.map { post ->
                        _optimisticLikes.value[post.id] ?: post
                    }
                    _uiState.value = current.copy(posts = updatedPosts)
                }
            }
        }
    }

    fun joinGroup() {
        viewModelScope.launch {
            // Get current group state to check if it's private
            val currentState = _uiState.value
            val isPrivateGroup = if (currentState is GroupDetailUiState.Success) {
                currentState.group.privacy == com.example.minisocialnetworkapplication.core.domain.model.GroupPrivacy.PRIVATE
            } else {
                false
            }
            
            Timber.d("joinGroup called, isPrivateGroup=$isPrivateGroup")
            
            val result = groupRepository.joinGroup(groupId)
            if (result is Result.Success) {
                Timber.d("joinGroup success, isPrivateGroup=$isPrivateGroup")
                if (isPrivateGroup) {
                    // For private groups, join creates a request - show pending message
                    _successMessage.value = "Your join request has been submitted and is awaiting admin approval."
                    Timber.d("Set successMessage for private group join request")
                }
                loadGroupDetails()
            } else if (result is Result.Error) {
                Timber.e("joinGroup failed: ${result.message}")
                _errorMessage.value = result.message ?: "Failed to join group"
            }
        }
    }

    
    fun togglePostApproval(enabled: Boolean) {
        viewModelScope.launch {
            val result = groupRepository.togglePostApproval(groupId, enabled)
            if (result is Result.Success) {
                loadGroupDetails() // Reload to reflect change
            } else if (result is Result.Error) {
                _errorMessage.value = result.message ?: "Failed to toggle post approval"
            }
        }
    }

    fun deleteGroup(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val result = groupRepository.deleteGroup(groupId)
            if (result is Result.Success) {
                onSuccess()
            } else if (result is Result.Error) {
                _errorMessage.value = result.message ?: "Failed to delete group"
            }
        }
    }

    fun leaveGroup(onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            val result = groupRepository.leaveGroup(groupId)
            if (result is Result.Success) {
                Timber.d("Left group $groupId successfully, reloading UI")
                // Reload group details to update UI to non-member state
                loadGroupDetails()
                onSuccess()
            } else if (result is Result.Error) {
                _errorMessage.value = result.message ?: "Failed to leave group"
            }
        }
    }
}
