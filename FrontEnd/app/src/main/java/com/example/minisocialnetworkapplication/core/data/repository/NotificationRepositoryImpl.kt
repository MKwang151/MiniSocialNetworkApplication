package com.example.minisocialnetworkapplication.core.data.repository

import com.example.minisocialnetworkapplication.core.domain.model.Notification
import com.example.minisocialnetworkapplication.core.domain.repository.NotificationRepository
import com.example.minisocialnetworkapplication.core.util.Result
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

class NotificationRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : NotificationRepository {
    
    override fun getNotifications(userId: String): Flow<List<Notification>> = callbackFlow {
        // Early return if user is not signed in
        if (auth.currentUser == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        val listener = firestore.collection("notifications")
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                // Check if still signed in
                if (auth.currentUser == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                if (error != null) {
                    if (error.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        Timber.w("Permission denied while listening to notifications - user likely logged out")
                        trySend(emptyList())
                    } else {
                        Timber.e(error, "Error listening to notifications")
                        trySend(emptyList())
                    }
                    return@addSnapshotListener
                }
                
                val notifications = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(Notification::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        Timber.e(e, "Error parsing notification ${doc.id}")
                        null
                    }
                } ?: emptyList()
                Timber.d("Loaded ${notifications.size} notifications for user $userId")
                trySend(notifications)
            }
        awaitClose { listener.remove() }
    }
    
    override suspend fun markAsRead(notificationId: String): Result<Unit> {
        return try {
            firestore.collection("notifications").document(notificationId)
                .update("read", true)  // Firebase uses "read" field, not "isRead"
                .await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error marking notification as read")
            Result.Error(e)
        }
    }

    override suspend fun markAllAsRead(userId: String): Result<Unit> {
        return try {
            val unreadNotifications = firestore.collection("notifications")
                .whereEqualTo("userId", userId)
                .whereEqualTo("read", false)
                .get()
                .await()
            
            if (unreadNotifications.isEmpty) {
                return Result.Success(Unit)
            }
            
            val batch = firestore.batch()
            for (document in unreadNotifications.documents) {
                batch.update(document.reference, "read", true)
            }
            
            batch.commit().await()
            Timber.d("Marked ${unreadNotifications.size()} notifications as read for user $userId")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error marking all notifications as read")
            Result.Error(e)
        }
    }
    
    override suspend fun deleteNotification(notificationId: String): Result<Unit> {
        return try {
            firestore.collection("notifications").document(notificationId)
                .delete()
                .await()
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error deleting notification")
            Result.Error(e)
        }
    }
    
    override fun getUnreadCount(userId: String): Flow<Int> = callbackFlow {
        // Early return if user is not signed in
        if (auth.currentUser == null) {
            trySend(0)
            close()
            return@callbackFlow
        }
        
        val listener = firestore.collection("notifications")
            .whereEqualTo("userId", userId)
            .whereEqualTo("read", false)  // Firebase uses "read" field, not "isRead"
            .addSnapshotListener { snapshot, error ->
                // Check if still signed in
                if (auth.currentUser == null) {
                    trySend(0)
                    return@addSnapshotListener
                }
                
                if (error != null) {
                    if (error.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        Timber.w("Permission denied while listening to unread count - user likely logged out")
                        trySend(0)
                    } else {
                        Timber.e(error, "Error listening to unread count")
                        trySend(0)
                    }
                    return@addSnapshotListener
                }
                
                trySend(snapshot?.size() ?: 0)
            }
        awaitClose { listener.remove() }
    }
    
    override suspend fun createNotification(notification: com.example.minisocialnetworkapplication.core.domain.model.Notification): Result<Unit> {
        return try {
            val notificationId = firestore.collection("notifications").document().id
            
            val notificationData = hashMapOf(
                "id" to notificationId,
                "userId" to notification.userId,
                "type" to notification.type.name,
                "title" to notification.title,
                "message" to notification.message,
                "data" to notification.data,
                "read" to false,
                "createdAt" to System.currentTimeMillis()
            )
            
            firestore.collection("notifications")
                .document(notificationId)
                .set(notificationData)
                .await()
            
            Timber.d("Created notification for user ${notification.userId}: ${notification.title}")
            Result.Success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to create notification")
            Result.Error(e)
        }
    }
}
