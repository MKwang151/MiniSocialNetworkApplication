package com.example.minisocialnetworkapplication.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.minisocialnetworkapplication.core.domain.repository.AdminRepository
import com.example.minisocialnetworkapplication.core.domain.repository.AdminStats
import com.example.minisocialnetworkapplication.core.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AdminDashboardUiState {
    data object Loading : AdminDashboardUiState
    data class Success(val stats: AdminStats) : AdminDashboardUiState
    data class Error(val message: String) : AdminDashboardUiState
}

@HiltViewModel
class AdminDashboardViewModel @Inject constructor(
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AdminDashboardUiState>(AdminDashboardUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        refreshStats()
    }

    fun refreshStats() {
        viewModelScope.launch {
            _uiState.value = AdminDashboardUiState.Loading
            when (val result = adminRepository.getDashboardStats()) {
                is Result.Success -> {
                    _uiState.value = AdminDashboardUiState.Success(result.data)
                }
                is Result.Error -> {
                    _uiState.value = AdminDashboardUiState.Error(result.message ?: "Failed to fetch stats")
                }
                else -> {}
            }
        }
    }
}
