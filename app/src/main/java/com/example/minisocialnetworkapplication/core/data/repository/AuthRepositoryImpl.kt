package com.example.minisocialnetworkapplication.core.data.repository

import com.example.minisocialnetworkapplication.core.data.local.ConversationDao
import com.example.minisocialnetworkapplication.core.data.local.MessageDao
import com.example.minisocialnetworkapplication.core.data.local.ParticipantDao
import com.example.minisocialnetworkapplication.core.domain.model.User
import com.example.minisocialnetworkapplication.core.domain.repository.AuthRepository
import com.example.minisocialnetworkapplication.core.util.Constants
import com.example.minisocialnetworkapplication.core.util.Result
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val participantDao: ParticipantDao
) : AuthRepository {

    override suspend fun register(email: String, password: String, name: String): Result<User> {
        return try {
            // Create user in Firebase Auth
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user
                ?: return Result.Error(Exception("User creation failed"))

            // Create user profile in Firestore
            val user = User(
                uid = firebaseUser.uid,
                name = name,
                email = email,
                avatarUrl = null,
                bio = null,
                createdAt = Timestamp.now()
            )

            firestore.collection(Constants.COLLECTION_USERS)
                .document(firebaseUser.uid)
                .set(user)
                .await()

            Timber.d("User registered successfully: ${user.uid}")
            Result.Success(user)
        } catch (e: Exception) {
            Timber.e(e, "Registration failed")
            Result.Error(e, "Registration failed: ${e.message}")
        }
    }

    override suspend fun login(email: String, password: String): Result<User> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user
                ?: return Result.Error(Exception("Login failed"))

            // Set user as online immediately after login
            firestore.collection(Constants.COLLECTION_USERS)
                .document(firebaseUser.uid)
                .update(
                    mapOf(
                        "isOnline" to true,
                        "lastActive" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            
            Timber.d("User ${firebaseUser.uid} set as online")

            // Fetch user profile from Firestore
            val userDoc = firestore.collection(Constants.COLLECTION_USERS)
                .document(firebaseUser.uid)
                .get()
                .await()

            val user = userDoc.toObject(User::class.java)
                ?: return Result.Error(Exception("User profile not found"))

            Timber.d("User logged in successfully: ${user.uid}")
            Result.Success(user)
        } catch (e: Exception) {
            Timber.e(e, "Login failed")
            Result.Error(e, "Login failed: ${e.message}")
        }
    }

    override suspend fun logout(): Result<Unit> {
        return try {
            val userId = auth.currentUser?.uid
            
            // Set user as offline before signing out
            if (userId != null) {
                firestore.collection(Constants.COLLECTION_USERS)
                    .document(userId)
                    .update(
                        mapOf(
                            "isOnline" to false,
                            "lastActive" to FieldValue.serverTimestamp()
                        )
                    )
                    .await()
                Timber.d("User $userId set as offline")
            }
            
            // Clear local chat data to prevent data mixing between accounts
            conversationDao.clearAll()
            messageDao.clearAll()
            participantDao.clearAll()
            Timber.d("Local chat data cleared")
            
            auth.signOut()
            Timber.d("User logged out successfully")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Logout failed")
            Result.Error(e)
        }
    }

    override fun getCurrentUser(): Flow<User?> = callbackFlow {
        // Track Firestore listener so we can clean it up when auth state changes
        var userDocListener: com.google.firebase.firestore.ListenerRegistration? = null
        
        val listener = FirebaseAuth.AuthStateListener { auth ->
            val firebaseUser = auth.currentUser
            
            // Clean up previous Firestore listener when auth state changes
            userDocListener?.remove()
            userDocListener = null
            
            if (firebaseUser != null) {
                userDocListener = firestore.collection(Constants.COLLECTION_USERS)
                    .document(firebaseUser.uid)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            // Don't crash on PERMISSION_DENIED (happens during logout)
                            if (error.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                                Timber.w("PERMISSION_DENIED while fetching user - user likely logged out")
                                trySend(null)
                            } else {
                                Timber.e(error, "Error fetching user")
                                trySend(null)
                            }
                            return@addSnapshotListener
                        }
                        val user = snapshot?.toObject(User::class.java)
                        trySend(user)
                    }
            } else {
                trySend(null)
            }
        }

        auth.addAuthStateListener(listener)
        awaitClose { 
            // Clean up both listeners
            userDocListener?.remove()
            auth.removeAuthStateListener(listener) 
        }
    }

    override fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    override fun isAuthenticated(): Boolean {
        return auth.currentUser != null
    }

    override suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Password reset failed")
            Result.Error(e)
        }
    }

    override suspend fun updateFcmToken(token: String): Result<Unit> {
        return try {
            val userId = getCurrentUserId()
                ?: return Result.Error(Exception("User not logged in"))

            firestore.collection(Constants.COLLECTION_USERS)
                .document(userId)
                .update("fcmToken", token)
                .await()

            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to update FCM token")
            Result.Error(e)
        }
    }
}

