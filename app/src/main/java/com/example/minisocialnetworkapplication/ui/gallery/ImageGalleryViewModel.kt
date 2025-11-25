package com.example.minisocialnetworkapplication.ui.gallery

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.minisocialnetworkapplication.core.domain.repository.PostRepository
import com.example.minisocialnetworkapplication.core.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

sealed interface ImageGalleryUiState {
    data object Loading : ImageGalleryUiState
    data class Success(val imageUrls: List<String>) : ImageGalleryUiState
    data class Error(val message: String) : ImageGalleryUiState
}

@HiltViewModel
class ImageGalleryViewModel @Inject constructor(
    private val postRepository: PostRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val postId: String = checkNotNull(savedStateHandle["postId"])
    val initialIndex: Int = savedStateHandle.get<String>("initialIndex")?.toIntOrNull() ?: 0

    private val _uiState = MutableStateFlow<ImageGalleryUiState>(ImageGalleryUiState.Loading)
    val uiState: StateFlow<ImageGalleryUiState> = _uiState.asStateFlow()

    init {
        loadImages()
    }

    private fun loadImages() {
        viewModelScope.launch {
            try {
                when (val result = postRepository.getPost(postId)) {
                    is Result.Success -> {
                        val imageUrls = result.data.mediaUrls
                        if (imageUrls.isEmpty()) {
                            _uiState.value = ImageGalleryUiState.Error("No images found")
                        } else {
                            _uiState.value = ImageGalleryUiState.Success(imageUrls)
                        }
                    }
                    is Result.Error -> {
                        _uiState.value = ImageGalleryUiState.Error(
                            result.message ?: "Failed to load images"
                        )
                    }
                    is Result.Loading -> {
                        // Already loading
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading images")
                _uiState.value = ImageGalleryUiState.Error(
                    e.message ?: "Unknown error"
                )
            }
        }
    }
}

