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
     * Called after profile update to sync author names and avatars
     */
    suspend fun updateUserInPostsAndComments(userId: String, newName: String, newAvatarUrl: String?): Result<Unit>

    /**
     * Upload user avatar to Storage and return download URL
     */
    suspend fun uploadAvatar(userId: String, imageUri: android.net.Uri): Result<String>

    /**
     * Update user avatar URL in Firestore
     */
    suspend fun updateAvatar(avatarUrl: String): Result<Unit>

    /**
     * Search users by name
     */
    suspend fun searchUsers(query: String): Result<List<User>>
    
    /**
     * Update user online presence
     */
    suspend fun updatePresence(isOnline: Boolean): Result<Unit>
    
    /**
     * Observe user status real-time (for online/offline indicator)
     */
    fun observeUser(userId: String): kotlinx.coroutines.flow.Flow<User?>
}


