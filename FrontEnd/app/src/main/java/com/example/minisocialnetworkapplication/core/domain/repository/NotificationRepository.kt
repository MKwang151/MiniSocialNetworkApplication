package com.example.minisocialnetworkapplication.core.domain.repository

import com.example.minisocialnetworkapplication.core.domain.model.Notification
import com.example.minisocialnetworkapplication.core.util.Result
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    fun getNotifications(userId: String): Flow<List<Notification>>
    suspend fun markAsRead(notificationId: String): Result<Unit>
    suspend fun markAllAsRead(userId: String): Result<Unit>
    suspend fun deleteNotification(notificationId: String): Result<Unit>
    fun getUnreadCount(userId: String): Flow<Int>
    suspend fun createNotification(notification: Notification): Result<Unit>
}
