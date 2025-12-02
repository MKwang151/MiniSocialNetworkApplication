package com.example.minisocialnetworkapplication.core.domain.repository

import com.example.minisocialnetworkapplication.core.domain.model.Friend
import com.example.minisocialnetworkapplication.core.util.Result

interface FriendRepository {

    /**
    * Get friends by uid
    */
    suspend fun getUserFriends(userId: String): Result<List<Friend>>

    /**
     * Add friend for current user
     */
    suspend fun addFriend(friendId: String): Result<Unit>

    /**
     * Remove friend for current user
     */
    suspend fun removeFriend(friendId: String): Result<Unit>

    /**
     * Check current user friend status with {friendId}
     */
    suspend fun isFriend(friendId: String): Result<Boolean>

    /**
     * Update current user friend profile (name, avatarUrl) when they edit their profile
     * This method should be called using a Worker
     */
    suspend fun updateFriendProfile(): Result<Unit>
}