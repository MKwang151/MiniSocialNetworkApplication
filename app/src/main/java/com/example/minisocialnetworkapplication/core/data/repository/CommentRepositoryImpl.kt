package com.example.minisocialnetworkapplication.core.data.repository

import com.example.minisocialnetworkapplication.core.domain.model.Comment
import com.example.minisocialnetworkapplication.core.domain.repository.CommentRepository
import com.example.minisocialnetworkapplication.core.util.Constants
import com.example.minisocialnetworkapplication.core.util.Result
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

class CommentRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : CommentRepository {

    override fun getComments(postId: String): Flow<List<Comment>> = callbackFlow {
        val listenerRegistration = firestore
            .collection(Constants.COLLECTION_POSTS)
            .document(postId)
            .collection(Constants.COLLECTION_COMMENTS)
            .orderBy(Constants.FIELD_CREATED_AT, Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Error listening to comments")
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val comments = snapshot.documents.mapNotNull { doc ->
                        try {
                            Comment(
                                id = doc.id,
                                postId = doc.getString(Constants.FIELD_POST_ID) ?: postId,
                                authorId = doc.getString(Constants.FIELD_AUTHOR_ID) ?: "",
                                authorName = doc.getString("authorName") ?: "Unknown",
                                authorAvatarUrl = doc.getString("authorAvatarUrl"),
                                text = doc.getString("text") ?: "",
                                createdAt = doc.getTimestamp(Constants.FIELD_CREATED_AT)
                                    ?: Timestamp.now()
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

            // Increment comment count
            firestore
                .collection(Constants.COLLECTION_POSTS)
                .document(postId)
                .update(Constants.FIELD_COMMENT_COUNT, com.google.firebase.firestore.FieldValue.increment(1))
                .await()

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

            // Decrement comment count
            firestore
                .collection(Constants.COLLECTION_POSTS)
                .document(postId)
                .update(Constants.FIELD_COMMENT_COUNT, com.google.firebase.firestore.FieldValue.increment(-1))
                .await()

            Timber.d("Comment deleted successfully: $commentId")
            Result.Success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to delete comment")
            Result.Error(e)
        }
    }

}

