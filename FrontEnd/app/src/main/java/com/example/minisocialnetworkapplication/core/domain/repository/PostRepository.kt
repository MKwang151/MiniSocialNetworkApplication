package com.example.minisocialnetworkapplication.core.domain.repository

import android.net.Uri
import androidx.paging.PagingData
import com.example.minisocialnetworkapplication.core.domain.model.Post
import com.example.minisocialnetworkapplication.core.util.Result
import kotlinx.coroutines.flow.Flow

interface PostRepository {

    /**
     * Get paginated feed posts
     */
    fun getFeedPaging(): Flow<PagingData<Post>>

    /**
     * Get posts by user ID
     */
    fun getUserPosts(userId: String): Flow<PagingData<Post>>

    /**
     * Create a new post (enqueues WorkManager task)
     */
    suspend fun createPost(
        text: String, 
        imageUris: List<Uri>, 
        groupId: String? = null, 
        approvalStatus: com.example.minisocialnetworkapplication.core.domain.model.PostApprovalStatus = com.example.minisocialnetworkapplication.core.domain.model.PostApprovalStatus.APPROVED
    ): Result<Unit>

    /**
     * Toggle like on a post (optimistic update)
     */
    suspend fun toggleLike(postId: String): Result<Boolean>

    /**
     * Get a single post by ID
     */
    suspend fun getPost(postId: String): Result<Post>

    /**
     * Get a single post by ID as a realtime Flow
     * Use this when you need to observe changes to a post (e.g., commentCount updates)
     */
    fun getPostFlow(postId: String): Flow<Post?>

    /**
     * Delete a post
     */
    suspend fun deletePost(postId: String): Result<Unit>

    /**
     * Update post text
     */
    suspend fun updatePost(postId: String, newText: String): Result<Unit>

    /**
     * Check if current user liked a post
     */
    suspend fun isPostLikedByCurrentUser(postId: String): Boolean

    /**
     * Clear posts cache (Room database)
     * Use after profile update to force refresh from Firestore
     */
    suspend fun clearPostsCache(): Result<Unit>

    /**
     * Invalidate paging source to trigger refresh
     * Call after clearing cache to force UI reload
     */
    fun invalidatePagingSource()

    /**
     * Update post with uploaded image URLs
     * Used by background worker after images are uploaded
     */
    suspend fun updatePostImages(postId: String, mediaUrls: List<String>): Result<Unit>
}
