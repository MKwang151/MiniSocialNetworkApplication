package com.example.minisocialnetworkapplication.core.domain.usecase.post

import com.example.minisocialnetworkapplication.core.domain.repository.PostRepository
import com.example.minisocialnetworkapplication.core.util.Result
import javax.inject.Inject

class ToggleLikeUseCase @Inject constructor(
    private val postRepository: PostRepository
) {
    suspend operator fun invoke(postId: String): Result<Boolean> {
        return postRepository.toggleLike(postId)
    }
}

