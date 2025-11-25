package com.example.minisocialnetworkapplication.core.util

import com.example.minisocialnetworkapplication.core.data.local.AppDatabase
import com.example.minisocialnetworkapplication.core.data.local.PostEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

/**
 * Utility class để đồng bộ hóa Room cache từ Firebase Firestore
 * Sử dụng khi cần cập nhật dữ liệu local sau khi có thay đổi quan trọng trên Firebase
 */
class CacheSyncUtil @Inject constructor(
    private val database: AppDatabase,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {

    /**
     * Đồng bộ tất cả posts từ Firebase về Room cache
     * Xóa cache cũ và tải lại dữ liệu mới nhất
     *
     * @param limit Số lượng posts tối đa cần tải (mặc định 50)
     * @return Result.Success nếu thành công, Result.Error nếu thất bại
     */
    suspend fun syncPostsFromFirebase(limit: Int = 50): Result<Int> {
        return try {
            val userId = auth.currentUser?.uid
            Timber.d("Starting cache sync from Firebase, limit=$limit")

            // Bước 1: Xóa toàn bộ cache và remote keys
            database.postDao().clearAll()
            database.remoteKeysDao().clearAll()
            Timber.d("Cleared existing cache")

            // Bước 2: Lấy posts mới nhất từ Firebase
            val postsSnapshot = firestore.collection(Constants.COLLECTION_POSTS)
                .orderBy(Constants.FIELD_CREATED_AT, com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            if (postsSnapshot.isEmpty) {
                Timber.d("No posts found in Firebase")
                return Result.Success(0)
            }

            // Bước 3: Lấy danh sách postIds để check likes
            val postIds = postsSnapshot.documents.map { it.id }
            val likedPostIds = if (userId != null) {
                getLikedPostIds(userId, postIds)
            } else {
                emptySet()
            }

            // Bước 4: Convert Firebase documents thành PostEntity
            val postEntities = mutableListOf<PostEntity>()

            postsSnapshot.documents.forEach { doc ->
                try {
                    val postId = doc.id
                    val authorId = doc.getString(Constants.FIELD_AUTHOR_ID) ?: return@forEach
                    val authorName = doc.getString(Constants.FIELD_AUTHOR_NAME) ?: "Unknown"
                    val authorAvatarUrl = doc.getString("authorAvatarUrl")
                    val text = doc.getString("text") ?: ""
                    @Suppress("UNCHECKED_CAST")
                    val mediaUrls = doc.get("mediaUrls") as? List<String> ?: emptyList()
                    val likeCount = doc.getLong(Constants.FIELD_LIKE_COUNT)?.toInt() ?: 0
                    val commentCount = doc.getLong(Constants.FIELD_COMMENT_COUNT)?.toInt() ?: 0
                    val createdAt = doc.getTimestamp(Constants.FIELD_CREATED_AT)?.toDate()?.time
                        ?: System.currentTimeMillis()
                    val likedByMe = likedPostIds.contains(postId)

                    val postEntity = PostEntity(
                        id = postId,
                        authorId = authorId,
                        authorName = authorName,
                        authorAvatarUrl = authorAvatarUrl,
                        text = text,
                        mediaUrls = mediaUrls.joinToString(","),
                        likeCount = likeCount,
                        commentCount = commentCount,
                        likedByMe = likedByMe,
                        createdAt = createdAt,
                        isSyncPending = false
                    )

                    postEntities.add(postEntity)
                } catch (e: Exception) {
                    Timber.e(e, "Error converting post document: ${doc.id}")
                }
            }

            // Bước 5: Insert vào Room database
            if (postEntities.isNotEmpty()) {
                database.postDao().insertAll(postEntities)
                Timber.d("Synced ${postEntities.size} posts to Room cache")
            }

            Result.Success(postEntities.size)

        } catch (e: Exception) {
            Timber.e(e, "Failed to sync posts from Firebase")
            Result.Error(e)
        }
    }

    /**
     * Chỉ làm mới lại thông tin author (name, avatar) trong các posts đã có trong cache
     * Hữu ích khi user cập nhật profile, không cần tải lại toàn bộ posts
     *
     * @param userId ID của user cần cập nhật thông tin
     */
    suspend fun refreshAuthorInfoInCache(userId: String): Result<Int> {
        return try {
            Timber.d("Refreshing author info in cache for userId=$userId")

            // Lấy thông tin user mới nhất từ Firebase
            val userDoc = firestore.collection(Constants.COLLECTION_USERS)
                .document(userId)
                .get()
                .await()

            if (!userDoc.exists()) {
                return Result.Error(Exception("User not found"))
            }

            val newName = userDoc.getString("name") ?: "Unknown"
            val newAvatarUrl = userDoc.getString("avatarUrl")

            // Cập nhật toàn bộ posts của user này trong Room cache
            val updatedCount = database.postDao().updateAuthorInfo(userId, newName, newAvatarUrl)

            Timber.d("Updated author info for $updatedCount posts in cache")
            Result.Success(updatedCount)

        } catch (e: Exception) {
            Timber.e(e, "Failed to refresh author info in cache")
            Result.Error(e)
        }
    }

    /**
     * Xóa một post khỏi cache
     * Dùng khi post bị xóa trên Firebase
     */
    suspend fun removePostFromCache(postId: String): Result<Unit> {
        return try {
            database.postDao().deletePost(postId)
            Timber.d("Removed post $postId from cache")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to remove post from cache")
            Result.Error(e)
        }
    }

    /**
     * Xóa toàn bộ cache
     * Buộc app tải lại dữ liệu từ Firebase ở lần fetch tiếp theo
     */
    suspend fun clearAllCache(): Result<Unit> {
        return try {
            database.postDao().clearAll()
            database.remoteKeysDao().clearAll()
            Timber.d("Cleared all cache")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear cache")
            Result.Error(e)
        }
    }

    /**
     * Lấy danh sách các postId mà user đã like
     * Helper function để xác định trạng thái likedByMe
     */
    private suspend fun getLikedPostIds(userId: String, postIds: List<String>): Set<String> {
        return try {
            if (postIds.isEmpty()) return emptySet()

            // Query likes collection để tìm các post mà user này đã like
            val likesSnapshot = firestore.collection(Constants.COLLECTION_LIKES)
                .whereEqualTo(Constants.FIELD_USER_ID, userId)
                .whereIn(Constants.FIELD_POST_ID, postIds.take(10)) // Firestore giới hạn 10 items cho whereIn
                .get()
                .await()

            likesSnapshot.documents
                .mapNotNull { it.getString(Constants.FIELD_POST_ID) }
                .toSet()

        } catch (e: Exception) {
            Timber.e(e, "Failed to get liked post ids")
            emptySet()
        }
    }

    /**
     * Đồng bộ một post cụ thể từ Firebase về cache
     * Hữu ích khi cần cập nhật một post sau khi edit hoặc có thay đổi
     */
    suspend fun syncSinglePost(postId: String): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid
            Timber.d("Syncing single post: $postId")

            val postDoc = firestore.collection(Constants.COLLECTION_POSTS)
                .document(postId)
                .get()
                .await()

            if (!postDoc.exists()) {
                // Post không tồn tại trên Firebase, xóa khỏi cache
                database.postDao().deletePost(postId)
                Timber.d("Post $postId not found on Firebase, removed from cache")
                return Result.Success(Unit)
            }

            // Convert thành PostEntity
            val authorId = postDoc.getString(Constants.FIELD_AUTHOR_ID) ?: ""
            val authorName = postDoc.getString(Constants.FIELD_AUTHOR_NAME) ?: "Unknown"
            val authorAvatarUrl = postDoc.getString("authorAvatarUrl")
            val text = postDoc.getString("text") ?: ""
            @Suppress("UNCHECKED_CAST")
            val mediaUrls = postDoc.get("mediaUrls") as? List<String> ?: emptyList()
            val likeCount = postDoc.getLong(Constants.FIELD_LIKE_COUNT)?.toInt() ?: 0
            val commentCount = postDoc.getLong(Constants.FIELD_COMMENT_COUNT)?.toInt() ?: 0
            val createdAt = postDoc.getTimestamp(Constants.FIELD_CREATED_AT)?.toDate()?.time
                ?: System.currentTimeMillis()

            // Check like status
            val likedByMe = if (userId != null) {
                val likeId = "${userId}_$postId"
                val likeDoc = firestore.collection(Constants.COLLECTION_LIKES)
                    .document(likeId)
                    .get()
                    .await()
                likeDoc.exists()
            } else {
                false
            }

            val postEntity = PostEntity(
                id = postId,
                authorId = authorId,
                authorName = authorName,
                authorAvatarUrl = authorAvatarUrl,
                text = text,
                mediaUrls = mediaUrls.joinToString(","),
                likeCount = likeCount,
                commentCount = commentCount,
                likedByMe = likedByMe,
                createdAt = createdAt,
                isSyncPending = false
            )

            // Insert hoặc update trong Room
            database.postDao().insertAll(listOf(postEntity))
            Timber.d("Synced post $postId to cache")

            Result.Success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to sync single post")
            Result.Error(e)
        }
    }
}

