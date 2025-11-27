package com.example.minisocialnetworkapplication.core.data.repository

import com.example.minisocialnetworkapplication.core.domain.model.User
import com.example.minisocialnetworkapplication.core.domain.repository.UserRepository
import com.example.minisocialnetworkapplication.core.util.Constants
import com.example.minisocialnetworkapplication.core.util.Result
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : UserRepository {

    override suspend fun getUser(userId: String): Result<User> {
        return try {
            val document = firestore
                .collection(Constants.COLLECTION_USERS)
                .document(userId)
                .get()
                .await()

            if (!document.exists()) {
                return Result.Error(Exception("User not found"))
            }

            val user = document.toObject(User::class.java)
            if (user == null) {
                return Result.Error(Exception("Failed to parse user data"))
            }

            Result.Success(user)

        } catch (e: Exception) {
            Timber.e(e, "Failed to get user: $userId")
            Result.Error(e)
        }
    }

    override suspend fun getCurrentUser(): Result<User> {
        return try {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                return Result.Error(Exception("User not authenticated"))
            }

            getUser(userId)

        } catch (e: Exception) {
            Timber.e(e, "Failed to get current user")
            Result.Error(e)
        }
    }

    override suspend fun updateUser(user: User): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                return Result.Error(Exception("User not authenticated"))
            }

            if (userId != user.id) {
                return Result.Error(Exception("Cannot update other user's profile"))
            }

            firestore
                .collection(Constants.COLLECTION_USERS)
                .document(userId)
                .set(user)
                .await()

            Timber.d("User updated successfully: $userId")
            Result.Success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to update user")
            Result.Error(e)
        }
    }

    override suspend fun updateUserInPostsAndComments(userId: String, newName: String): Result<Unit> {
        return try {
            Timber.d("Updating user info in posts and comments: userId=$userId, newName=$newName")

            // Update author name in all posts by this user
            val postsQuery = firestore.collection(Constants.COLLECTION_POSTS)
                .whereEqualTo(Constants.FIELD_AUTHOR_ID, userId)
                .get()
                .await()

            val postBatch = firestore.batch()
            var postsUpdated = 0

            postsQuery.documents.forEach { postDoc ->
                postBatch.update(postDoc.reference, Constants.FIELD_AUTHOR_NAME, newName)
                postsUpdated++
            }

            if (postsUpdated > 0) {
                postBatch.commit().await()
                Timber.d("Updated $postsUpdated posts with new author name")
            }

            // Update author name in all comments by this user
            // Need to query all posts first, then their comments
            val allPostsSnapshot = firestore.collection(Constants.COLLECTION_POSTS)
                .get()
                .await()

            var commentsUpdated = 0

            allPostsSnapshot.documents.forEach { postDoc ->
                val commentsQuery = postDoc.reference
                    .collection(Constants.COLLECTION_COMMENTS)
                    .whereEqualTo(Constants.FIELD_AUTHOR_ID, userId)
                    .get()
                    .await()

                if (commentsQuery.documents.isNotEmpty()) {
                    val commentBatch = firestore.batch()
                    commentsQuery.documents.forEach { commentDoc ->
                        commentBatch.update(commentDoc.reference, Constants.FIELD_AUTHOR_NAME, newName)
                        commentsUpdated++
                    }
                    commentBatch.commit().await()
                }
            }

            if (commentsUpdated > 0) {
                Timber.d("Updated $commentsUpdated comments with new author name")
            }

            Timber.d("Successfully updated user info in posts and comments")
            Result.Success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to update user in posts and comments")
            Result.Error(e)
        }
    }

    override suspend fun updateAvatar(avatarUrl: String): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                return Result.Error(Exception("User not authenticated"))
            }

            firestore
                .collection(Constants.COLLECTION_USERS)
                .document(userId)
                .update("avatarUrl", avatarUrl)
                .await()

            Timber.d("Avatar updated successfully: $userId")
            Result.Success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to update avatar")
            Result.Error(e)
        }
    }

    override suspend fun searchUsers(query: String): Result<List<User>> {
        return try {
            if (query.isBlank()) {
                return Result.Success(emptyList())
            }

            val snapshot = firestore
                .collection(Constants.COLLECTION_USERS)
                .orderBy("name")
                .startAt(query)
                .endAt(query + "\uf8ff")
                .limit(20)
                .get()
                .await()

            val users = snapshot.documents.mapNotNull { doc ->
                doc.toObject(User::class.java)
            }

            Result.Success(users)

        } catch (e: Exception) {
            Timber.e(e, "Failed to search users")
            Result.Error(e)
        }
    }
}
