package com.example.minisocialnetworkapplication.core.domain.usecase.auth

import com.example.minisocialnetworkapplication.core.domain.model.User
import com.example.minisocialnetworkapplication.core.domain.repository.AuthRepository
import com.example.minisocialnetworkapplication.core.util.Result
import com.example.minisocialnetworkapplication.core.util.Validator
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): Result<User> {
        // Validate inputs
        Validator.getEmailError(email)?.let {
            return Result.Error(Exception(it))
        }

        if (password.isBlank()) {
            return Result.Error(Exception("Password is required"))
        }

        return authRepository.login(email, password)
    }
}

