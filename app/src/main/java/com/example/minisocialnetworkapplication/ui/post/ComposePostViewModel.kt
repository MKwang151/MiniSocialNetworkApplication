package com.example.minisocialnetworkapplication.ui.post

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.minisocialnetworkapplication.core.domain.model.GroupRole
import com.example.minisocialnetworkapplication.core.domain.model.PostApprovalStatus
import com.example.minisocialnetworkapplication.core.domain.repository.GroupRepository
import com.example.minisocialnetworkapplication.core.domain.usecase.post.CreatePostUseCase
import com.example.minisocialnetworkapplication.core.util.Constants
import com.example.minisocialnetworkapplication.core.util.Result
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

sealed interface ComposePostUiState {
    data object Idle : ComposePostUiState
    data class Uploading(val message: String = "Preparing images...") : ComposePostUiState
    data object Success : ComposePostUiState
    data class Error(val message: String) : ComposePostUiState
}

@HiltViewModel
class ComposePostViewModel @Inject constructor(
    private val createPostUseCase: CreatePostUseCase,
    private val groupRepository: GroupRepository,
    private val auth: FirebaseAuth,
    savedStateHandle: androidx.lifecycle.SavedStateHandle
) : ViewModel() {

    // Treat empty string as null for personal posts
    private val groupId: String? = savedStateHandle.get<String>("groupId")?.takeIf { it.isNotBlank() }

    private val _uiState = MutableStateFlow<ComposePostUiState>(ComposePostUiState.Idle)
    val uiState: StateFlow<ComposePostUiState> = _uiState.asStateFlow()

    private val _selectedImages = MutableStateFlow<List<Uri>>(emptyList())
    val selectedImages: StateFlow<List<Uri>> = _selectedImages.asStateFlow()

    private val _postText = MutableStateFlow("")
    val postText: StateFlow<String> = _postText.asStateFlow()
    
    // Will post require approval message
    private val _requiresApproval = MutableStateFlow(false)
    val requiresApproval: StateFlow<Boolean> = _requiresApproval.asStateFlow()
    
    init {
        // Check if this group requires post approval
        if (groupId != null) {
            checkPostApprovalRequired()
        }
    }
    
    private fun checkPostApprovalRequired() {
        viewModelScope.launch {
            val currentUserId = auth.currentUser?.uid ?: return@launch
            
            // Get group details
            val groupResult = groupRepository.getGroupDetails(groupId!!)
            if (groupResult is Result.Success) {
                val group = groupResult.data
                if (group.requirePostApproval) {
                    // Check user role - admins and creators don't need approval
                    val roleResult = groupRepository.getMemberRole(groupId, currentUserId)
                    if (roleResult is Result.Success) {
                        val role = roleResult.data
                        _requiresApproval.value = role != GroupRole.ADMIN && role != GroupRole.CREATOR
                    }
                }
            }
        }
    }

    fun updatePostText(text: String) {
        if (text.length <= Constants.MAX_POST_TEXT_LENGTH) {
            _postText.value = text
        }
    }

    fun addImages(uris: List<Uri>) {
        val currentImages = _selectedImages.value
        val totalImages = currentImages.size + uris.size

        if (totalImages > Constants.MAX_IMAGE_COUNT) {
            _uiState.value = ComposePostUiState.Error(
                "You can only add up to ${Constants.MAX_IMAGE_COUNT} images"
            )
            return
        }

        _selectedImages.value = currentImages + uris
        Timber.d("Added ${uris.size} images, total: ${_selectedImages.value.size}")
    }

    fun addImageFromCamera(uri: Uri) {
        val currentImages = _selectedImages.value
        if (currentImages.size >= Constants.MAX_IMAGE_COUNT) {
            _uiState.value = ComposePostUiState.Error(
                "You can only add up to ${Constants.MAX_IMAGE_COUNT} images"
            )
            return
        }

        _selectedImages.value = currentImages + uri
        Timber.d("Added camera image, total: ${_selectedImages.value.size}")
    }

    fun removeImage(uri: Uri) {
        _selectedImages.value = _selectedImages.value - uri
        Timber.d("Removed image, total: ${_selectedImages.value.size}")
    }

    fun createPost() {
        viewModelScope.launch {
            try {
                val text = _postText.value.trim()
                val images = _selectedImages.value

                // Validation
                if (text.isBlank() && images.isEmpty()) {
                    _uiState.value = ComposePostUiState.Error("Post cannot be empty")
                    return@launch
                }

                if (text.length > Constants.MAX_POST_TEXT_LENGTH) {
                    _uiState.value = ComposePostUiState.Error(
                        "Post text cannot exceed ${Constants.MAX_POST_TEXT_LENGTH} characters"
                    )
                    return@launch
                }

                val progressMessage = if (images.isEmpty()) {
                    "Creating post..."
                } else {
                    "Creating post with ${images.size} image${if (images.size > 1) "s" else ""}..."
                }
                _uiState.value = ComposePostUiState.Uploading(progressMessage)
                
                // Determine approval status
                val approvalStatus = if (_requiresApproval.value) {
                    PostApprovalStatus.PENDING
                } else {
                    PostApprovalStatus.APPROVED
                }
                
                Timber.d("Creating post: text=${text.take(50)}, images=${images.size}, approvalStatus=$approvalStatus")

                // Use IO dispatcher for potentially heavy I/O operations (copying images)
                val result = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    createPostUseCase(text, images, groupId, approvalStatus)
                }

                when (result) {
                    is Result.Success -> {
                        Timber.d("Post created successfully")
                        _uiState.value = ComposePostUiState.Success
                        clearPost()
                    }
                    is Result.Error -> {
                        Timber.e("Failed to create post: ${result.message}")
                        _uiState.value = ComposePostUiState.Error(
                            result.message ?: "Failed to create post"
                        )
                    }
                    is Result.Loading -> {
                        // Should not happen
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Unexpected error creating post")
                _uiState.value = ComposePostUiState.Error(
                    "Failed to create post: ${e.message}"
                )
            }
        }
    }

    fun clearPost() {
        _postText.value = ""
        _selectedImages.value = emptyList()
    }

    fun clearError() {
        if (_uiState.value is ComposePostUiState.Error) {
            _uiState.value = ComposePostUiState.Idle
        }
    }

    fun showError(message: String) {
        _uiState.value = ComposePostUiState.Error(message)
    }
}

