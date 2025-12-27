package com.example.minisocialnetworkapplication.core.domain.usecase.auth

import com.example.minisocialnetworkapplication.core.domain.model.User
import com.example.minisocialnetworkapplication.core.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCurrentUserUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): Flow<User?> {
        return authRepository.getCurrentUser()
    }

    fun getUserId(): String? {
        return authRepository.getCurrentUserId()
    }

    fun isAuthenticated(): Boolean {
        return authRepository.isAuthenticated()
    }
}

