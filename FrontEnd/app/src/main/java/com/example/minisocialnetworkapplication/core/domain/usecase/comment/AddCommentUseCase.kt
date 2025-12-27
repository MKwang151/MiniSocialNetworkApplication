package com.example.minisocialnetworkapplication.core.domain.usecase.comment

import com.example.minisocialnetworkapplication.core.domain.model.Comment
import com.example.minisocialnetworkapplication.core.domain.repository.CommentRepository
import com.example.minisocialnetworkapplication.core.util.Constants
import com.example.minisocialnetworkapplication.core.util.Result
import com.example.minisocialnetworkapplication.core.util.Validator
import javax.inject.Inject

class AddCommentUseCase @Inject constructor(
    private val commentRepository: CommentRepository
) {
    suspend operator fun invoke(postId: String, text: String): Result<Comment> {
        // Validate input
        if (!Validator.isValidCommentText(text)) {
            return Result.Error(Exception("Comment text is invalid or exceeds ${Constants.MAX_COMMENT_TEXT_LENGTH} characters"))
        }

        return commentRepository.addComment(postId, text)
    }
}

