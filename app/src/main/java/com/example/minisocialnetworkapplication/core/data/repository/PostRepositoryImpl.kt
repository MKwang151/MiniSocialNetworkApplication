package com.example.minisocialnetworkapplication.core.data.repository

import android.content.Context
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
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject

class PostRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val workManager: WorkManager
) : PostRepository {

    // Track current paging sources for invalidation
    private var currentPagingSource: PagingSource<*, *>? = null

    /**
     * Copy image from content URI to app cache directory
     * This ensures the image is accessible when WorkManager runs
     */
    private fun copyImageToCache(uri: Uri): File? {
        return try {
            Timber.d("Starting to copy image: $uri")

            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                Timber.e("Cannot open input stream for URI: $uri")
                return null
            }

            val cacheFile = File(
                context.cacheDir,
                "upload_${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg"
            )

            var bytesCopied = 0L
            FileOutputStream(cacheFile).use { outputStream ->
                val buffer = ByteArray(8192)
                var bytes = inputStream.read(buffer)
                while (bytes >= 0) {
                    outputStream.write(buffer, 0, bytes)
                    bytesCopied += bytes
                    bytes = inputStream.read(buffer)
                }
            }
            inputStream.close()

            Timber.d("Copied image to cache: ${cacheFile.absolutePath}, size: ${cacheFile.length()} bytes (copied: $bytesCopied)")

            if (!cacheFile.exists() || cacheFile.length() == 0L) {
                Timber.e("Cache file is empty or doesn't exist")
                return null
            }

            cacheFile
        } catch (e: SecurityException) {
            Timber.e(e, "Security exception copying image: $uri")
            null
        } catch (e: OutOfMemoryError) {
            Timber.e(e, "Out of memory copying image: $uri")
            null
        } catch (e: Exception) {
            Timber.e(e, "Failed to copy image to cache: $uri")
            null
        }
    }

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
            pagingSourceFactory = {
                database.postDao().getPagingSource().also {
                    currentPagingSource = it
                }
            }
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

            Timber.d("Creating post with ${imageUris.size} images")

            // Copy images to cache directory first
            // This ensures WorkManager can access them later
            val cachedImagePaths = mutableListOf<String>()
            for (uri in imageUris) {
                val cachedFile = copyImageToCache(uri)
                if (cachedFile != null) {
                    cachedImagePaths.add(cachedFile.absolutePath)
                } else {
                    Timber.w("Failed to cache image: $uri, skipping")
                }
            }

            if (imageUris.isNotEmpty() && cachedImagePaths.isEmpty()) {
                return Result.Error(Exception("Failed to prepare images for upload"))
            }

            Timber.d("Cached ${cachedImagePaths.size} images to: ${cachedImagePaths.joinToString()}")

            // Generate post ID
            val postId = UUID.randomUUID().toString()

            // Get user info from Firestore
            val userDoc = firestore.collection(Constants.COLLECTION_USERS)
                .document(userId)
                .get()
                .await()
            val userName = userDoc.getString("name") ?: "Unknown"
            val userAvatarUrl = userDoc.getString("avatarUrl")

            // Create temporary post entity and insert to Room immediately
            // This makes the post appear in Feed right away
            val tempPost = com.example.minisocialnetworkapplication.core.data.local.PostEntity(
                id = postId,
                authorId = userId,
                authorName = userName,
                authorAvatarUrl = userAvatarUrl,
                text = text,
                mediaUrls = cachedImagePaths.joinToString(","),
                likeCount = 0,
                commentCount = 0,
                likedByMe = false,
                createdAt = System.currentTimeMillis(),
                isSyncPending = true
            )
            database.postDao().insertAll(listOf(tempPost))
            Timber.d("Inserted temporary post to Room: $postId")

            // Create WorkManager request with unique ID
            val workData = workDataOf(
                "POST_ID" to postId,
                "TEXT" to text,
                "IMAGE_URIS" to cachedImagePaths.toTypedArray(),
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
                "${Constants.WORK_UPLOAD_POST}_$postId",
                ExistingWorkPolicy.KEEP,
                uploadWorkRequest
            )

            Timber.d("Post upload enqueued with ID: $postId")
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

            Timber.d("Toggle like for post $postId: current=$isLiked, new=$newLikedState")

            // Update Firestore using transaction for atomicity
            val postRef = firestore.collection(Constants.COLLECTION_POSTS).document(postId)

            try {
                firestore.runTransaction { transaction ->
                    // Read post to verify it exists
                    val postSnapshot = transaction.get(postRef)
                    if (!postSnapshot.exists()) {
                        throw Exception("Post not found")
                    }

                    if (newLikedState) {
                        // Add like
                        transaction.set(likeRef, hashMapOf(
                            Constants.FIELD_USER_ID to userId,
                            Constants.FIELD_POST_ID to postId,
                            Constants.FIELD_CREATED_AT to com.google.firebase.Timestamp.now()
                        ))
                        // Increment like count
                        transaction.update(postRef, Constants.FIELD_LIKE_COUNT,
                            com.google.firebase.firestore.FieldValue.increment(1))
                    } else {
                        // Remove like
                        transaction.delete(likeRef)
                        // Decrement like count (but not below 0)
                        val currentCount = postSnapshot.getLong(Constants.FIELD_LIKE_COUNT) ?: 0
                        if (currentCount > 0) {
                            transaction.update(postRef, Constants.FIELD_LIKE_COUNT,
                                com.google.firebase.firestore.FieldValue.increment(-1))
                        }
                    }
                }.await()

                Timber.d("Firestore transaction completed successfully")

                // Get updated like count from Firestore
                val updatedPostSnapshot = postRef.get().await()
                val newLikeCount = updatedPostSnapshot.getLong(Constants.FIELD_LIKE_COUNT)?.toInt() ?: 0

                // Update Room cache with both like status and count
                database.postDao().updateLike(postId, newLikedState, newLikeCount)

                Timber.d("Post $postId like toggled to $newLikedState, count=$newLikeCount")
                Result.Success(newLikedState)
            } catch (e: Exception) {
                Timber.e(e, "Transaction failed for toggle like")
                throw e
            }
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

            if (!postDoc.exists()) {
                return Result.Error(Exception("Post not found"))
            }

            val authorId = postDoc.getString(Constants.FIELD_AUTHOR_ID)
            if (authorId != userId) {
                return Result.Error(Exception("Not authorized to delete this post"))
            }

            Timber.d("Deleting post $postId with all its comments and likes")

            // Step 1: Delete all comments in this post
            try {
                val commentsSnapshot = firestore.collection(Constants.COLLECTION_POSTS)
                    .document(postId)
                    .collection(Constants.COLLECTION_COMMENTS)
                    .get()
                    .await()

                if (commentsSnapshot.documents.isNotEmpty()) {
                    val commentDeleteBatch = firestore.batch()
                    commentsSnapshot.documents.forEach { commentDoc ->
                        commentDeleteBatch.delete(commentDoc.reference)
                    }
                    commentDeleteBatch.commit().await()
                    Timber.d("Deleted ${commentsSnapshot.documents.size} comments for post $postId")
                } else {
                    Timber.d("No comments to delete for post $postId")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error deleting comments, continuing with post deletion")
                // Continue even if comments deletion fails
            }

            // Step 2: Delete all likes for this post
            try {
                // Query likes collection where postId matches
                val likesSnapshot = firestore.collection(Constants.COLLECTION_LIKES)
                    .whereEqualTo(Constants.FIELD_POST_ID, postId)
                    .get()
                    .await()

                if (likesSnapshot.documents.isNotEmpty()) {
                    val likeDeleteBatch = firestore.batch()
                    likesSnapshot.documents.forEach { likeDoc ->
                        likeDeleteBatch.delete(likeDoc.reference)
                    }
                    likeDeleteBatch.commit().await()
                    Timber.d("Deleted ${likesSnapshot.documents.size} likes for post $postId")
                } else {
                    Timber.d("No likes to delete for post $postId")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error deleting likes, continuing with post deletion")
                // Continue even if likes deletion fails
            }

            // Step 3: Delete the post itself from Firestore (CRITICAL)
            firestore.collection(Constants.COLLECTION_POSTS)
                .document(postId)
                .delete()
                .await()
            Timber.d("Deleted post document $postId from Firestore")

            // Step 4: Delete from Room cache (CRITICAL - must succeed)
            database.postDao().deletePost(postId)
            Timber.d("Deleted post $postId from Room cache")

            Timber.d("Post $postId deleted successfully with all related data")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete post: ${e.message}")
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

    override suspend fun clearPostsCache(): Result<Unit> {
        return try {
            Timber.d("Clearing posts cache from Room database")

            // CRITICAL: Clear both posts AND remote keys
            // Without clearing remote keys, Paging 3 thinks data still exists
            // and won't trigger RemoteMediator to reload from Firestore
            database.postDao().clearAll()
            database.remoteKeysDao().clearAll()

            Timber.d("Posts cache and remote keys cleared successfully")

            // Invalidate paging source to trigger immediate refresh
            invalidatePagingSource()

            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear posts cache")
            Result.Error(e)
        }
    }

    override fun invalidatePagingSource() {
        Timber.d("Invalidating paging source to trigger refresh")
        currentPagingSource?.invalidate()
    }
}
