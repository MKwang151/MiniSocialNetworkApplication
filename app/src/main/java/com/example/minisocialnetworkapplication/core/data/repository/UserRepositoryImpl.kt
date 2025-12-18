package com.example.minisocialnetworkapplication.core.data.repository

import com.example.minisocialnetworkapplication.core.domain.model.User
import com.example.minisocialnetworkapplication.core.domain.repository.UserRepository
import com.example.minisocialnetworkapplication.core.util.Constants
import com.example.minisocialnetworkapplication.core.util.Result
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import android.content.Context
import android.net.Uri
import com.example.minisocialnetworkapplication.core.util.ImageCompressor
import java.util.UUID

class UserRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val storage: FirebaseStorage
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
                ?: return Result.Error(Exception("Failed to parse user data"))

            Result.Success(user)

        } catch (e: Exception) {
            Timber.e(e, "Failed to get user: $userId")
            Result.Error(e)
        }
    }

    override suspend fun getCurrentUser(): Result<User> {
        return try {
            val userId = auth.currentUser?.uid
                    ?: return Result.Error(Exception("User not authenticated"))

            getUser(userId)

        } catch (e: Exception) {
            Timber.e(e, "Failed to get current user")
            Result.Error(e)
        }
    }

    override suspend fun updateUser(user: User): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.Error(Exception("User not authenticated"))

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
        // Redundant: Now handled by Firebase Cloud Function (syncUserProfile)
        // for better reliability and performance.
        Timber.d("Profile sync for userId=$userId will be handled by Cloud Functions")
        return Result.Success(Unit)
    }

    override suspend fun uploadAvatar(userId: String, imageUri: Uri): Result<String> {
        return try {
            // Compress image
            val compressedFile = ImageCompressor.compressImage(context, imageUri)
                ?: return Result.Error(Exception("Failed to compress image"))

            // Generate filename
            val filename = "${UUID.randomUUID()}.jpg"
            val storageRef = storage.reference
                .child("${Constants.STORAGE_AVATARS_PATH}/$userId/$filename")

            // Upload to Firebase Storage
            storageRef.putFile(Uri.fromFile(compressedFile)).await()

            // Get download URL
            val downloadUrl = storageRef.downloadUrl.await().toString()

            // Cleanup local compressed file
            compressedFile.delete()

            Result.Success(downloadUrl)

        } catch (e: Exception) {
            Timber.e(e, "Failed to upload avatar")
            Result.Error(e)
        }
    }

    override suspend fun updateAvatar(avatarUrl: String): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid
                ?: return Result.Error(Exception("User not authenticated"))

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

            val queryLower = query.lowercase().trim()
            val currentUserId = auth.currentUser?.uid

            // Firestore doesn't support case-insensitive search or OR queries well
            // So we fetch all users and filter client-side
            // For production, consider using Algolia or ElasticSearch
            val snapshot = firestore
                .collection(Constants.COLLECTION_USERS)
                .limit(100) // Limit to prevent loading too many users
                .get()
                .await()

            val users = snapshot.documents.mapNotNull { doc ->
                doc.toObject(User::class.java)
            }.filter { user ->
                // Exclude current user from results
                user.id != currentUserId &&
                // Match by name or email (case-insensitive, contains)
                (user.name.lowercase().contains(queryLower) ||
                 user.email.lowercase().contains(queryLower))
            }.take(20) // Limit results

            Result.Success(users)

        } catch (e: Exception) {
            Timber.e(e, "Failed to search users")
            Result.Error(e)
        }
    }

    override suspend fun updatePresence(isOnline: Boolean): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                return Result.Error(Exception("User not authenticated"))
            }

            val updates = mapOf(
                "isOnline" to isOnline,
                "lastActive" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )

            firestore
                .collection(Constants.COLLECTION_USERS)
                .document(userId)
                .update(updates)
                .await()

            Timber.d("Presence updated: isOnline=$isOnline for user $userId")
            Result.Success(Unit)

        } catch (e: Exception) {
            Timber.e(e, "Failed to update presence")
            Result.Error(e)
        }
    }
    
    override fun observeUser(userId: String): kotlinx.coroutines.flow.Flow<User?> = 
        kotlinx.coroutines.flow.callbackFlow {
            val listener = firestore.collection(Constants.COLLECTION_USERS)
                .document(userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Timber.e(error, "Failed to observe user: $userId")
                        return@addSnapshotListener
                    }
                    
                    val user = snapshot?.toObject(User::class.java)
                    Timber.d("observeUser: $userId isOnline=${user?.isOnline}")
                    trySend(user)
                }
            
            awaitClose { listener.remove() }
        }
}
