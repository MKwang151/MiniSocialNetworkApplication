package com.example.minisocialnetworkapplication.core.data.repository

import com.example.minisocialnetworkapplication.core.data.local.AppDatabase
import com.example.minisocialnetworkapplication.core.domain.model.Comment
import com.example.minisocialnetworkapplication.core.domain.repository.CommentRepository
import com.example.minisocialnetworkapplication.core.util.Constants
import com.example.minisocialnetworkapplication.core.util.Result
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

class CommentRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val database: AppDatabase
) : CommentRepository {

    override fun getComments(postId: String): Flow<List<Comment>> = callbackFlow {
        // Early return if user is not signed in
        if (auth.currentUser == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        val listenerRegistration = firestore
            .collection(Constants.COLLECTION_POSTS)
            .document(postId)
            .collection(Constants.COLLECTION_COMMENTS)
            .orderBy(Constants.FIELD_CREATED_AT, Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (error.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        Timber.w("PERMISSION_DENIED while listening to comments - user likely logged out")
                        trySend(emptyList())
                    } else {
                        Timber.e(error, "Error listening to comments")
                        trySend(emptyList())
                    }
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val comments = snapshot.documents.mapNotNull { doc ->
                        try {
                            val rawReactions = doc.get("reactions") as? Map<String, Any> ?: emptyMap()
                            val reactions = rawReactions.mapValues { entry ->
                                (entry.value as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
                            }.filterValues { it.isNotEmpty() }
                            
                            Timber.d("Parsed reactions for ${doc.id}: $reactions")
                            
                            Comment(
                                id = doc.id,
                                postId = doc.getString(Constants.FIELD_POST_ID) ?: postId,
                                authorId = doc.getString(Constants.FIELD_AUTHOR_ID) ?: "",
                                authorName = doc.getString("authorName") ?: "Unknown",
                                authorAvatarUrl = doc.getString("authorAvatarUrl"),
                                text = doc.getString("text") ?: "",
                                createdAt = doc.getTimestamp(Constants.FIELD_CREATED_AT)
                                    ?: Timestamp.now(),
                                reactions = reactions,
                                replyToId = doc.getString("replyToId"),
                                replyToAuthorName = doc.getString("replyToAuthorName")
                            )
                        } catch (e: Exception) {
                            Timber.e(e, "Error parsing comment: ${doc.id}")
                            null
                        }
                    }
                    trySend(comments)
                }
            }

        awaitClose {
            listenerRegistration.remove()
        }
    }

    override suspend fun addComment(postId: String, text: String): Result<Comment> {
        return try {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                return Result.Error(Exception("User not authenticated"))
            }

            // Get user info
            val userDoc = firestore.collection(Constants.COLLECTION_USERS)
                .document(userId)
                .get()
                .await()

            val userName = userDoc.getString("name") ?: "Unknown"
            val userAvatarUrl = userDoc.getString("avatarUrl")

            val commentData = hashMapOf(
                Constants.FIELD_POST_ID to postId,
                Constants.FIELD_AUTHOR_ID to userId,
                "authorName" to userName,
                "authorAvatarUrl" to userAvatarUrl,
                "text" to text,
                Constants.FIELD_CREATED_AT to Timestamp.now()
            )

            // Add comment
            val docRef = firestore
                .collection(Constants.COLLECTION_POSTS)
                .document(postId)
                .collection(Constants.COLLECTION_COMMENTS)
                .add(commentData)
                .await()

            // Increment comment count in Firestore
            firestore
                .collection(Constants.COLLECTION_POSTS)
                .document(postId)
                .update(Constants.FIELD_COMMENT_COUNT, com.google.firebase.firestore.FieldValue.increment(1))
                .await()

            // Update Room cache comment count
            try {
                val currentPost = database.postDao().getPostById(postId)
                if (currentPost != null) {
                    val newCommentCount = currentPost.commentCount + 1
                    database.postDao().updateCommentCount(postId, newCommentCount)
                    Timber.d("Updated comment count in Room cache: $postId -> $newCommentCount")
                }
            } catch (e: Exception) {
                Timber.w(e, "Failed to update comment count in Room cache, will sync later")
            }
            
            // Create notification for post author (if not commenting on own post)
            try {
                val postDoc = firestore.collection(Constants.COLLECTION_POSTS)
                    .document(postId).get().await()
                val postAuthorId = postDoc.getString(Constants.FIELD_AUTHOR_ID)
                
                if (postAuthorId != null && postAuthorId != userId) {
                    val notificationId = firestore.collection("notifications").document().id
                    val commentPreview = if (text.length > 50) text.take(50) + "..." else text
                    val notificationData = hashMapOf(
                        "id" to notificationId,
                        "userId" to postAuthorId,
                        "type" to "COMMENT",
                        "title" to "New comment on your post",
                        "message" to "$userName commented: $commentPreview",
                        "data" to mapOf(
                            "postId" to postId,
                            "commentId" to docRef.id,
                            "commenterId" to userId,
                            "commenterName" to userName
                        ),
                        "read" to false,
                        "createdAt" to System.currentTimeMillis()
                    )
                    firestore.collection("notifications")
                        .document(notificationId)
                        .set(notificationData)
                        .await()
                    Timber.d("Created COMMENT notification for post author $postAuthorId")
                }
            } catch (e: Exception) {
                Timber.w(e, "Failed to create comment notification")
            }

            val comment = Comment(
                id = docRef.id,
                postId = postId,
                authorId = userId,
                authorName = userName,
                authorAvatarUrl = userAvatarUrl,
                text = text,
                createdAt = Timestamp.now()
            )

            Timber.d("Comment added successfully: ${docRef.id}")
            Result.Success(comment)

        } catch (e: Exception) {
            Timber.e(e, "Failed to add comment")
            Result.Error(e)
        }
    }

    override suspend fun deleteComment(postId: String, commentId: String): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                return Result.Error(Exception("User not authenticated"))
            }

            // Check if user is the author or post owner
            val commentDoc = firestore
                .collection(Constants.COLLECTION_POSTS)
                .document(postId)
                .collection(Constants.COLLECTION_COMMENTS)
                .document(commentId)
                .get()
                .await()

            val commentAuthorId = commentDoc.getString(Constants.FIELD_AUTHOR_ID)

            val postDoc = firestore
                .collection(Constants.COLLECTION_POSTS)
                .document(postId)
                .get()
                .await()

            val postAuthorId = postDoc.getString(Constants.FIELD_AUTHOR_ID)

            if (userId != commentAuthorId && userId != postAuthorId) {
                return Result.Error(Exception("Not authorized to delete this comment"))
            }

            // Delete comment
            firestore
                .collection(Constants.COLLECTION_POSTS)
                .document(postId)
                .collection(Constants.COLLECTION_COMMENTS)
                .document(commentId)
                .delete()
                .await()

            // Decrement comment count in Firestore
            firestore
                .collection(Constants.COLLECTION_POSTS)
                .document(postId)
                .update(Constants.FIELD_COMMENT_COUNT, com.google.firebase.firestore.FieldValue.increment(-1))
                .await()

            // Update Room cache comment count
            try {
                val currentPost = database.postDao().getPostById(postId)
                if (currentPost != null) {
                    val newCommentCount = (currentPost.commentCount - 1).coerceAtLeast(0)
                    database.postDao().updateCommentCount(postId, newCommentCount)
                    Timber.d("Updated comment count in Room cache: $postId -> $newCommentCount")
                }
            } catch (e: Exception) {
                Timber.w(e, "Failed to update comment count in Room cache, will sync later")
            }

            Timber.d("Comment deleted successfully: $commentId")
            Result.Success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to delete comment")
            Result.Error(e)
        }
    }

    override suspend fun updateComment(postId: String, commentId: String, newText: String): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                return Result.Error(Exception("User not authenticated"))
            }

            // Verify ownership - only comment author can edit
            val commentDoc = firestore
                .collection(Constants.COLLECTION_POSTS)
                .document(postId)
                .collection(Constants.COLLECTION_COMMENTS)
                .document(commentId)
                .get()
                .await()

            if (!commentDoc.exists()) {
                return Result.Error(Exception("Comment not found"))
            }

            val commentAuthorId = commentDoc.getString(Constants.FIELD_AUTHOR_ID)
            if (userId != commentAuthorId) {
                return Result.Error(Exception("Not authorized to edit this comment"))
            }

            // Update comment text in Firestore
            firestore
                .collection(Constants.COLLECTION_POSTS)
                .document(postId)
                .collection(Constants.COLLECTION_COMMENTS)
                .document(commentId)
                .update("text", newText)
                .await()

            Timber.d("Comment updated successfully: $commentId")
            Result.Success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to update comment")
            Result.Error(e)
        }
    }

    override suspend fun addReplyComment(
        postId: String,
        text: String,
        replyToId: String,
        replyToAuthorName: String
    ): Result<Comment> {
        return try {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                return Result.Error(Exception("User not authenticated"))
            }

            // Get user info
            val userDoc = firestore.collection(Constants.COLLECTION_USERS)
                .document(userId)
                .get()
                .await()

            val userName = userDoc.getString("name") ?: "Unknown"
            val userAvatarUrl = userDoc.getString("avatarUrl")

            val commentData = hashMapOf(
                Constants.FIELD_POST_ID to postId,
                Constants.FIELD_AUTHOR_ID to userId,
                "authorName" to userName,
                "authorAvatarUrl" to userAvatarUrl,
                "text" to text,
                Constants.FIELD_CREATED_AT to Timestamp.now(),
                "replyToId" to replyToId,
                "replyToAuthorName" to replyToAuthorName
            )

            val docRef = firestore
                .collection(Constants.COLLECTION_POSTS)
                .document(postId)
                .collection(Constants.COLLECTION_COMMENTS)
                .add(commentData)
                .await()

            // Increment comment count
            firestore
                .collection(Constants.COLLECTION_POSTS)
                .document(postId)
                .update(Constants.FIELD_COMMENT_COUNT, FieldValue.increment(1))
                .await()

            val comment = Comment(
                id = docRef.id,
                postId = postId,
                authorId = userId,
                authorName = userName,
                authorAvatarUrl = userAvatarUrl,
                text = text,
                createdAt = Timestamp.now(),
                replyToId = replyToId,
                replyToAuthorName = replyToAuthorName
            )

            Timber.d("Reply comment added successfully: ${docRef.id}")
            Result.Success(comment)

        } catch (e: Exception) {
            Timber.e(e, "Failed to add reply comment")
            Result.Error(e)
        }
    }

    override suspend fun addReaction(postId: String, commentId: String, emoji: String): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.Error(Exception("User not authenticated"))

            firestore
                .collection(Constants.COLLECTION_POSTS)
                .document(postId)
                .collection(Constants.COLLECTION_COMMENTS)
                .document(commentId)
                .set(
                    mapOf("reactions" to mapOf(emoji to FieldValue.arrayUnion(userId))),
                    SetOptions.merge()
                )
                .await()

            Timber.d("Added reaction $emoji to comment $commentId")
            Result.Success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to add reaction")
            Result.Error(e)
        }
    }

    override suspend fun removeReaction(postId: String, commentId: String, emoji: String): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.Error(Exception("User not authenticated"))

            // Remove user from reaction array
            firestore
                .collection(Constants.COLLECTION_POSTS)
                .document(postId)
                .collection(Constants.COLLECTION_COMMENTS)
                .document(commentId)
                .set(
                    mapOf("reactions" to mapOf(emoji to FieldValue.arrayRemove(userId))),
                    SetOptions.merge()
                )
                .await()

            // Check if array is empty and delete key
            val commentDoc = firestore
                .collection(Constants.COLLECTION_POSTS)
                .document(postId)
                .collection(Constants.COLLECTION_COMMENTS)
                .document(commentId)
                .get()
                .await()

            @Suppress("UNCHECKED_CAST")
            val reactions = commentDoc.get("reactions") as? Map<String, List<*>>
            val emojiList = reactions?.get(emoji) ?: emptyList<String>()
            
            if (emojiList.isEmpty()) {
                firestore
                    .collection(Constants.COLLECTION_POSTS)
                    .document(postId)
                    .collection(Constants.COLLECTION_COMMENTS)
                    .document(commentId)
                    .update("reactions.$emoji", FieldValue.delete())
                    .await()
            }

            Timber.d("Removed reaction $emoji from comment $commentId")
            Result.Success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to remove reaction")
            Result.Error(e)
        }
    }
}

