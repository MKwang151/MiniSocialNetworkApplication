package com.example.minisocialnetworkapplication.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.minisocialnetworkapplication.core.domain.model.User
import com.example.minisocialnetworkapplication.core.domain.repository.AdminRepository
import com.example.minisocialnetworkapplication.core.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface UserManagementUiState {
    data object Loading : UserManagementUiState
    data class Success(val users: List<User>) : UserManagementUiState
    data class Error(val message: String) : UserManagementUiState
}

@HiltViewModel
class UserManagementViewModel @Inject constructor(
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UserManagementUiState>(UserManagementUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        loadUsers()
    }

    private fun loadUsers() {
        viewModelScope.launch {
            adminRepository.getAllUsers().collectLatest { result ->
                when (result) {
                    is Result.Success -> {
                        _uiState.value = UserManagementUiState.Success(result.data)
                    }
                    is Result.Error -> {
                        _uiState.value = UserManagementUiState.Error(result.message ?: "Failed to load users")
                    }
                    else -> {}
                }
            }
        }
    }

    fun banUser(userId: String) {
        viewModelScope.launch {
            adminRepository.banUser(userId)
        }
    }

    fun unbanUser(userId: String) {
        viewModelScope.launch {
            adminRepository.unbanUser(userId)
        }
    }
}
