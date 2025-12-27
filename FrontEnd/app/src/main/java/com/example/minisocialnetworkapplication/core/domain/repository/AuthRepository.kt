package com.example.minisocialnetworkapplication.core.domain.repository

import com.example.minisocialnetworkapplication.core.domain.model.User
import com.example.minisocialnetworkapplication.core.util.Result
import kotlinx.coroutines.flow.Flow

interface AuthRepository {

    /**
     * Register a new user with email and password
     */
    suspend fun register(email: String, password: String, name: String): Result<User>

    /**
     * Login with email and password
     */
    suspend fun login(email: String, password: String): Result<User>

    /**
     * Logout current user
     */
    suspend fun logout(): Result<Unit>

    /**
     * Get current user as Flow (reactive)
     */
    fun getCurrentUser(): Flow<User?>

    /**
     * Get current user ID
     */
    fun getCurrentUserId(): String?

    /**
     * Check if user is authenticated
     */
    fun isAuthenticated(): Boolean

    /**
     * Reset password
     */
    suspend fun resetPassword(email: String): Result<Unit>

    /**
     * Update FCM token
     */
    suspend fun updateFcmToken(token: String): Result<Unit>
}

