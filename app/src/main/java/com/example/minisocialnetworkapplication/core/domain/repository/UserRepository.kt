package com.example.minisocialnetworkapplication.core.domain.repository

import com.example.minisocialnetworkapplication.core.domain.model.User
import com.example.minisocialnetworkapplication.core.util.Result

interface UserRepository {

    /**
     * Get user by ID
     */
    suspend fun getUser(userId: String): Result<User>

    /**
     * Update user profile
     */
    suspend fun updateProfile(
        name: String? = null,
        bio: String? = null,
        avatarUrl: String? = null
    ): Result<User>
}

