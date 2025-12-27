package com.example.minisocialnetworkapplication.core.domain.usecase.comment

import com.example.minisocialnetworkapplication.core.domain.model.Comment
import com.example.minisocialnetworkapplication.core.domain.repository.CommentRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCommentsUseCase @Inject constructor(
    private val commentRepository: CommentRepository
) {
    operator fun invoke(postId: String): Flow<List<Comment>> {
        return commentRepository.getComments(postId)
    }
}

