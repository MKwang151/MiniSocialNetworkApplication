package com.example.minisocialnetworkapplication.ui.socialgroup

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.minisocialnetworkapplication.core.domain.model.JoinRequest
import com.example.minisocialnetworkapplication.core.domain.repository.GroupRepository
import com.example.minisocialnetworkapplication.core.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface JoinRequestsUiState {
    data object Loading : JoinRequestsUiState
    data class Success(val requests: List<JoinRequest>) : JoinRequestsUiState
    data class Error(val message: String) : JoinRequestsUiState
}

@HiltViewModel
class JoinRequestsViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val groupId: String = checkNotNull(savedStateHandle["groupId"])

    private val _uiState = MutableStateFlow<JoinRequestsUiState>(JoinRequestsUiState.Loading)
    val uiState: StateFlow<JoinRequestsUiState> = _uiState.asStateFlow()

    private val _actionResult = MutableStateFlow<String?>(null)
    val actionResult: StateFlow<String?> = _actionResult.asStateFlow()

    init {
        loadJoinRequests()
    }

    private fun loadJoinRequests() {
        viewModelScope.launch {
            groupRepository.getJoinRequestsForGroup(groupId)
                .catch { e ->
                    _uiState.value = JoinRequestsUiState.Error(e.message ?: "Failed to load requests")
                }
                .collect { requests ->
                    _uiState.value = JoinRequestsUiState.Success(requests)
                }
        }
    }

    fun approveRequest(requestId: String) {
        viewModelScope.launch {
            when (val result = groupRepository.approveJoinRequest(requestId)) {
                is Result.Success -> {
                    _actionResult.value = "Request approved"
                }
                is Result.Error -> {
                    _actionResult.value = "Error: ${result.message}"
                }
                else -> {}
            }
        }
    }

    fun rejectRequest(requestId: String) {
        viewModelScope.launch {
            when (val result = groupRepository.rejectJoinRequest(requestId)) {
                is Result.Success -> {
                    _actionResult.value = "Request rejected"
                }
                is Result.Error -> {
                    _actionResult.value = "Error: ${result.message}"
                }
                else -> {}
            }
        }
    }

    fun clearActionResult() {
        _actionResult.value = null
    }
}
