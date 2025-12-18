package com.example.minisocialnetworkapplication.ui.profile

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.minisocialnetworkapplication.core.domain.repository.UserRepository
import com.example.minisocialnetworkapplication.core.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

sealed interface EditProfileUiState {
    data object Idle : EditProfileUiState
    data object Loading : EditProfileUiState
    data object Success : EditProfileUiState
    data class Error(val message: String) : EditProfileUiState
}

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val postRepository: com.example.minisocialnetworkapplication.core.domain.repository.PostRepository,
    private val cacheSyncUtil: com.example.minisocialnetworkapplication.core.util.CacheSyncUtil,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val userId: String = checkNotNull(savedStateHandle["userId"])

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _bio = MutableStateFlow("")
    val bio: StateFlow<String> = _bio.asStateFlow()

    private val _avatarUrl = MutableStateFlow<String?>(null)
    val avatarUrl: StateFlow<String?> = _avatarUrl.asStateFlow()

    private val _uiState = MutableStateFlow<EditProfileUiState>(EditProfileUiState.Idle)
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            _uiState.value = EditProfileUiState.Loading
            when (val result = userRepository.getUser(userId)) {
                is Result.Success -> {
                    _name.value = result.data.name
                    _bio.value = result.data.bio ?: ""
                    _avatarUrl.value = result.data.avatarUrl
                    _uiState.value = EditProfileUiState.Idle
                }
                is Result.Error -> {
                    _uiState.value = EditProfileUiState.Error(
                        result.message ?: "Failed to load profile"
                    )
                }
                is Result.Loading -> {
                    // Already loading
                }
            }
        }
    }

    fun updateName(newName: String) {
        _name.value = newName
    }

    fun updateBio(newBio: String) {
        _bio.value = newBio
    }

    fun updateAvatar(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = EditProfileUiState.Loading
            when (val result = userRepository.uploadAvatar(userId, uri)) {
                is Result.Success -> {
                    val newAvatarUrl = result.data
                    when (val updateResult = userRepository.updateAvatar(newAvatarUrl)) {
                        is Result.Success -> {
                            _avatarUrl.value = newAvatarUrl
                            _uiState.value = EditProfileUiState.Idle
                            Timber.d("Avatar updated successfully: $newAvatarUrl")
                        }
                        is Result.Error -> {
                            _uiState.value = EditProfileUiState.Error(
                                updateResult.message ?: "Failed to update avatar URL"
                            )
                        }
                        is Result.Loading -> {}
                    }
                }
                is Result.Error -> {
                    _uiState.value = EditProfileUiState.Error(
                        result.message ?: "Failed to upload avatar"
                    )
                }
                is Result.Loading -> {}
            }
        }
    }

    fun saveProfile() {
        if (_name.value.isBlank()) {
            _uiState.value = EditProfileUiState.Error("Name cannot be empty")
            return
        }

        viewModelScope.launch {
            try {
                _uiState.value = EditProfileUiState.Loading

                // Get current user first
                val currentUserResult = userRepository.getUser(userId)
                if (currentUserResult !is Result.Success) {
                    _uiState.value = EditProfileUiState.Error("Failed to load user data")
                    return@launch
                }

                val currentUser = currentUserResult.data

                // Create updated user object
                val updatedUser = currentUser.copy(
                    name = _name.value.trim(),
                    bio = _bio.value.trim().ifBlank { null }
                )

                // Update profile in Firestore
                when (val result = userRepository.updateUser(updatedUser)) {
                    is Result.Success -> {
                        Timber.d("Profile updated successfully")

                        // Update user name in all posts and comments in Firestore
                        val updateResult = userRepository.updateUserInPostsAndComments(
                            userId = userId,
                            newName = updatedUser.name
                        )

                        when (updateResult) {
                            is Result.Success -> {
                                Timber.d("Updated user info in posts and comments on Firestore")
                            }
                            is Result.Error -> {
                                Timber.w("Failed to update user in posts/comments: ${updateResult.message}")
                            }
                            is Result.Loading -> {}
                        }

                        // CRITICAL: Sync cache from Firebase để cập nhật author info trong Room
                        // Sử dụng refreshAuthorInfoInCache để chỉ cập nhật thông tin author (nhanh hơn)
                        Timber.d("Starting cache sync after profile update")
                        val syncResult = cacheSyncUtil.refreshAuthorInfoInCache(userId)

                        when (syncResult) {
                            is Result.Success -> {
                                Timber.d("Cache synced successfully: ${syncResult.data} posts updated")
                                // Invalidate paging source để trigger refresh UI
                                postRepository.invalidatePagingSource()
                            }
                            is Result.Error -> {
                                Timber.w("Cache sync failed: ${syncResult.message}, falling back to clear cache")
                                // Fallback: Clear cache nếu sync thất bại
                                postRepository.clearPostsCache()
                                postRepository.invalidatePagingSource()
                            }
                            is Result.Loading -> {}
                        }

                        _uiState.value = EditProfileUiState.Success
                    }
                    is Result.Error -> {
                        Timber.e("Failed to update profile: ${result.message}")
                        _uiState.value = EditProfileUiState.Error(
                            result.message ?: "Failed to update profile"
                        )
                    }
                    is Result.Loading -> {
                        // Already loading
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error updating profile")
                _uiState.value = EditProfileUiState.Error(
                    e.message ?: "Unknown error"
                )
            }
        }
    }

    fun clearError() {
        if (_uiState.value is EditProfileUiState.Error) {
            _uiState.value = EditProfileUiState.Idle
        }
    }
}

