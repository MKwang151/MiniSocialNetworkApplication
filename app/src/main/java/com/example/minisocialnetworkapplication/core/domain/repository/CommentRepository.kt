package com.example.minisocialnetworkapplication.core.domain.repository

import com.example.minisocialnetworkapplication.core.domain.model.Comment
import com.example.minisocialnetworkapplication.core.util.Result
import kotlinx.coroutines.flow.Flow

interface CommentRepository {

    /**
     * Get comments for a post (realtime)
     */
    fun getComments(postId: String): Flow<List<Comment>>

    /**
     * Add a comment to a post
     */
    suspend fun addComment(postId: String, text: String): Result<Comment>

    /**
     * Delete a comment
     */
    suspend fun deleteComment(postId: String, commentId: String): Result<Unit>
}

