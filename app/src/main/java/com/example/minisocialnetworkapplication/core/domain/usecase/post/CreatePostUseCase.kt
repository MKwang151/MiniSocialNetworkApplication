package com.example.minisocialnetworkapplication.core.domain.usecase.post

import android.net.Uri
import com.example.minisocialnetworkapplication.core.domain.repository.PostRepository
import com.example.minisocialnetworkapplication.core.util.Constants
import com.example.minisocialnetworkapplication.core.util.Result
import com.example.minisocialnetworkapplication.core.util.Validator
import javax.inject.Inject

class CreatePostUseCase @Inject constructor(
    private val postRepository: PostRepository
) {
    suspend operator fun invoke(text: String, imageUris: List<Uri>): Result<Unit> {
        // Validate inputs
        if (!Validator.isValidPostText(text)) {
            return Result.Error(Exception("Post text is invalid or exceeds ${Constants.MAX_POST_TEXT_LENGTH} characters"))
        }

        if (imageUris.size > Constants.MAX_IMAGE_COUNT) {
            return Result.Error(Exception("Maximum ${Constants.MAX_IMAGE_COUNT} images allowed"))
        }

        return postRepository.createPost(text, imageUris)
    }
}

