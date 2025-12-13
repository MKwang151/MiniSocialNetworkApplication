package com.example.minisocialnetworkapplication.core.domain.repository

import com.example.minisocialnetworkapplication.core.domain.model.Friend
import com.example.minisocialnetworkapplication.core.domain.model.FriendStatus
import com.example.minisocialnetworkapplication.core.util.Result

interface FriendRepository {

    /**
    * Get friends by uid
    */
    suspend fun getUserFriends(userId: String): Result<List<Friend>>

    /**
     * Get current user's friend requests
     */
    suspend fun getFriendRequests(): Result<List<Friend>>

    /**
     * Get current user's friend requests count (real-time)
     */
    fun getFriendRequestsCount(): kotlinx.coroutines.flow.Flow<Int>

    /**
     * Send friend request to uid=friendId
     */
    suspend fun sendFriendRequest(friendId: String): Result<Unit>

    /**
     * Accept friend request
     */
    suspend fun acceptFriendRequest(friendId: String): Result<Unit>

    /**
     * Remove friend request to uid=friendId
     */
    suspend fun removeFriendRequest(friendId: String, isSender: Boolean): Result<Unit>

    /**
     * Remove friend for current user
     */
    suspend fun unfriend(friendId: String): Result<Unit>

    /**
     * Check current user friend status with {friendId} (Pending/Added)
     */
    suspend fun getFriendStatus(friendId: String): Result<FriendStatus>
}