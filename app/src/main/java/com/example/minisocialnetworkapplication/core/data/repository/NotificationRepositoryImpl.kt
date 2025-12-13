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
        val listener = firestore.collection("notifications")
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Error listening to notifications")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val notifications = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Notification::class.java)?.copy(id = doc.id)
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
        val listener = firestore.collection("notifications")
            .whereEqualTo("userId", userId)
            .whereEqualTo("read", false)  // Firebase uses "read" field, not "isRead"
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Timber.e(error, "Error listening to unread count")
                    trySend(0)
                    return@addSnapshotListener
                }
                
                trySend(snapshot?.size() ?: 0)
            }
        awaitClose { listener.remove() }
    }
}
