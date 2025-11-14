package com.example.minisocialnetworkapplication.core.data.repository

import android.net.Uri
import androidx.paging.*
import androidx.work.*
import com.example.minisocialnetworkapplication.core.data.local.AppDatabase
import com.example.minisocialnetworkapplication.core.data.remote.PostRemoteMediator
import com.example.minisocialnetworkapplication.core.domain.model.Post
import com.example.minisocialnetworkapplication.core.domain.repository.PostRepository
import com.example.minisocialnetworkapplication.core.util.Constants
import com.example.minisocialnetworkapplication.core.util.Result
import com.example.minisocialnetworkapplication.core.worker.UploadPostWorker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

class PostRepositoryImpl @Inject constructor(
    private val database: AppDatabase,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val workManager: WorkManager
) : PostRepository {

    @OptIn(ExperimentalPagingApi::class)
    override fun getFeedPaging(): Flow<PagingData<Post>> {
        return Pager(
            config = PagingConfig(
                pageSize = Constants.PAGE_SIZE,
                prefetchDistance = Constants.PREFETCH_DISTANCE,
                enablePlaceholders = false,
                initialLoadSize = Constants.INITIAL_LOAD_SIZE
            ),
            remoteMediator = PostRemoteMediator(
                database = database,
                firestore = firestore,
                auth = auth
            ),
            pagingSourceFactory = { database.postDao().getPagingSource() }
        ).flow.map { pagingData ->
            pagingData.map { it.toPost() }
        }
    }

    @OptIn(ExperimentalPagingApi::class)
    override fun getUserPosts(userId: String): Flow<PagingData<Post>> {
        return Pager(
            config = PagingConfig(
                pageSize = Constants.PAGE_SIZE,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { database.postDao().getUserPostsPagingSource(userId) }
        ).flow.map { pagingData ->
            pagingData.map { it.toPost() }
        }
    }

    override suspend fun createPost(text: String, imageUris: List<Uri>): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                return Result.Error(Exception("User not authenticated"))
            }

            // Create WorkManager request with unique ID
            val workId = UUID.randomUUID()
            val workData = workDataOf(
                "TEXT" to text,
                "IMAGE_URIS" to imageUris.map { it.toString() }.toTypedArray(),
                "USER_ID" to userId
            )

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val uploadWorkRequest = OneTimeWorkRequestBuilder<UploadPostWorker>()
                .setInputData(workData)
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    java.util.concurrent.TimeUnit.MILLISECONDS
                )
                .addTag(Constants.WORK_TAG_UPLOAD)
                .build()

            workManager.enqueueUniqueWork(
                "${Constants.WORK_UPLOAD_POST}_$workId",
                ExistingWorkPolicy.KEEP,
                uploadWorkRequest
            )

            Timber.d("Post upload enqueued with ID: $workId")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to enqueue post upload")
            Result.Error(e)
        }
    }

    override suspend fun toggleLike(postId: String): Result<Boolean> {
        return try {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                return Result.Error(Exception("User not authenticated"))
            }

            // Check current like status
            val likeId = "${userId}_$postId"
            val likeRef = firestore.collection(Constants.COLLECTION_LIKES).document(likeId)
            val likeDoc = likeRef.get().await()

            val isLiked = likeDoc.exists()
            val newLikedState = !isLiked

            // Get current like count from Firestore
            val postRef = firestore.collection(Constants.COLLECTION_POSTS).document(postId)
            val postSnapshot = postRef.get().await()
            val currentLikeCount = postSnapshot.getLong(Constants.FIELD_LIKE_COUNT)?.toInt() ?: 0
            val newLikeCount = if (newLikedState) currentLikeCount + 1 else (currentLikeCount - 1).coerceAtLeast(0)

            // Update Firestore
            firestore.runTransaction { transaction ->
                if (newLikedState) {
                    // Add like
                    transaction.set(likeRef, hashMapOf(
                        Constants.FIELD_USER_ID to userId,
                        Constants.FIELD_POST_ID to postId,
                        Constants.FIELD_CREATED_AT to com.google.firebase.Timestamp.now()
                    ))
                } else {
                    // Remove like
                    transaction.delete(likeRef)
                }

                // Update post like count
                transaction.update(postRef, Constants.FIELD_LIKE_COUNT, newLikeCount)
            }.await()

            // Update Room cache with both like status and count
            database.postDao().updateLike(postId, newLikedState, newLikeCount)

            Timber.d("Post $postId like toggled to $newLikedState, count=$newLikeCount")
            Result.Success(newLikedState)
        } catch (e: Exception) {
            Timber.e(e, "Failed to toggle like")
            Result.Error(e)
        }
    }

    override suspend fun getPost(postId: String): Result<Post> {
        return try {
            val userId = auth.currentUser?.uid

            // Try to get from cache first
            val cachedPost = database.postDao().getPostById(postId)
            if (cachedPost != null) {
                return Result.Success(cachedPost.toPost())
            }

            // Fetch from Firestore
            val postDoc = firestore.collection(Constants.COLLECTION_POSTS)
                .document(postId)
                .get()
                .await()

            if (!postDoc.exists()) {
                return Result.Error(Exception("Post not found"))
            }

            val authorId = postDoc.getString(Constants.FIELD_AUTHOR_ID) ?: ""
            val authorName = postDoc.getString("authorName") ?: ""
            val authorAvatarUrl = postDoc.getString("authorAvatarUrl")
            val text = postDoc.getString("text") ?: ""
            @Suppress("UNCHECKED_CAST")
            val mediaUrls = postDoc.get("mediaUrls") as? List<String> ?: emptyList()
            val likeCount = postDoc.getLong(Constants.FIELD_LIKE_COUNT)?.toInt() ?: 0
            val commentCount = postDoc.getLong(Constants.FIELD_COMMENT_COUNT)?.toInt() ?: 0
            val createdAt = postDoc.getTimestamp(Constants.FIELD_CREATED_AT) ?: com.google.firebase.Timestamp.now()

            // Check if liked by current user
            val likedByMe = if (userId != null) {
                isPostLikedByCurrentUser(postId)
            } else {
                false
            }

            val post = Post(
                id = postId,
                authorId = authorId,
                authorName = authorName,
                authorAvatarUrl = authorAvatarUrl,
                text = text,
                mediaUrls = mediaUrls,
                likeCount = likeCount,
                commentCount = commentCount,
                likedByMe = likedByMe,
                createdAt = createdAt
            )

            Result.Success(post)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get post")
            Result.Error(e)
        }
    }

    override suspend fun deletePost(postId: String): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                return Result.Error(Exception("User not authenticated"))
            }

            // Verify ownership
            val postDoc = firestore.collection(Constants.COLLECTION_POSTS)
                .document(postId)
                .get()
                .await()

            val authorId = postDoc.getString(Constants.FIELD_AUTHOR_ID)
            if (authorId != userId) {
                return Result.Error(Exception("Not authorized to delete this post"))
            }

            // Delete from Firestore
            firestore.collection(Constants.COLLECTION_POSTS)
                .document(postId)
                .delete()
                .await()

            // Delete from Room cache
            database.postDao().deletePost(postId)

            Timber.d("Post $postId deleted")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete post")
            Result.Error(e)
        }
    }

    override suspend fun isPostLikedByCurrentUser(postId: String): Boolean {
        return try {
            val userId = auth.currentUser?.uid ?: return false
            val likeId = "${userId}_$postId"

            val likeDoc = firestore.collection(Constants.COLLECTION_LIKES)
                .document(likeId)
                .get()
                .await()

            likeDoc.exists()
        } catch (e: Exception) {
            Timber.e(e, "Failed to check like status")
            false
        }
    }
}

