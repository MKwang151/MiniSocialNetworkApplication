package com.example.minisocialnetworkapplication.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.minisocialnetworkapplication.core.domain.usecase.auth.GetCurrentUserUseCase
import com.example.minisocialnetworkapplication.core.domain.usecase.auth.LoginUseCase
import com.example.minisocialnetworkapplication.core.domain.usecase.auth.LogoutUseCase
import com.example.minisocialnetworkapplication.core.domain.usecase.auth.RegisterUseCase
import com.example.minisocialnetworkapplication.core.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val registerUseCase: RegisterUseCase,
    private val loginUseCase: LoginUseCase,
    private val logoutUseCase: LogoutUseCase,
    getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    val currentUser = getCurrentUserUseCase().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
    )

    fun register(email: String, password: String, name: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            when (val result = registerUseCase(email, password, name)) {
                is Result.Success -> _authState.value = AuthState.Success(result.data)
                is Result.Error -> _authState.value = AuthState.Error(result.message ?: "Registration failed")
                else -> {}
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            when (val result = loginUseCase(email, password)) {
                is Result.Success -> _authState.value = AuthState.Success(result.data)
                is Result.Error -> _authState.value = AuthState.Error(result.message ?: "Login failed")
                else -> {}
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            logoutUseCase()
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }
}

