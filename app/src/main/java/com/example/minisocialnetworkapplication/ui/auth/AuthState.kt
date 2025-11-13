package com.example.minisocialnetworkapplication.ui.auth

import com.example.minisocialnetworkapplication.core.domain.model.User

sealed interface AuthState {
    data object Idle : AuthState
    data object Loading : AuthState
    data class Success(val user: User) : AuthState
    data class Error(val message: String) : AuthState
}

