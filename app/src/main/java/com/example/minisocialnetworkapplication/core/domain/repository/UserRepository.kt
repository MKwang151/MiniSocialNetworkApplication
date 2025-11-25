package com.example.minisocialnetworkapplication.core.domain.repository

import com.example.minisocialnetworkapplication.core.domain.model.User
import com.example.minisocialnetworkapplication.core.util.Result

interface UserRepository {

    /**
     * Get user by ID
     */
    suspend fun getUser(userId: String): Result<User>

    /**
     * Get current logged in user
     */
    suspend fun getCurrentUser(): Result<User>

    /**
     * Update user data
     */
    suspend fun updateUser(user: User): Result<Unit>

    /**
     * Update user info in all posts and comments
     * Called after profile update to sync author names
     */
    suspend fun updateUserInPostsAndComments(userId: String, newName: String): Result<Unit>

    /**
     * Update user avatar
     */
    suspend fun updateAvatar(avatarUrl: String): Result<Unit>

    /**
     * Search users by name
     */
    suspend fun searchUsers(query: String): Result<List<User>>
}


