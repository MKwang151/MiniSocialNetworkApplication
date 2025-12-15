package com.example.minisocialnetworkapplication.ui.report

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.minisocialnetworkapplication.core.domain.model.Report
import com.example.minisocialnetworkapplication.core.domain.repository.ReportRepository
import com.example.minisocialnetworkapplication.core.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ReportUiState {
    data object Idle : ReportUiState
    data object Loading : ReportUiState
    data object Success : ReportUiState
    data class Error(val message: String) : ReportUiState
}

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val reportRepository: ReportRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Get post info from navigation args
    private val postId: String = checkNotNull(savedStateHandle["postId"])
    private val authorId: String = savedStateHandle["authorId"] ?: ""
    private val groupId: String? = savedStateHandle.get<String>("groupId")?.takeIf { it.isNotBlank() }

    private val _uiState = MutableStateFlow<ReportUiState>(ReportUiState.Idle)
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    private val _reason = MutableStateFlow("")
    val reason = _reason.asStateFlow()

    private val _description = MutableStateFlow("")
    val description = _description.asStateFlow()

    fun onReasonChange(value: String) {
        _reason.value = value
    }

    fun onDescriptionChange(value: String) {
        _description.value = value
    }

    fun submitReport() {
        val reasonVal = _reason.value.trim()
        val descriptionVal = _description.value.trim()

        if (reasonVal.isBlank()) {
            _uiState.value = ReportUiState.Error("Please enter a reason for the report")
            return
        }

        viewModelScope.launch {
            _uiState.value = ReportUiState.Loading

            val report = Report(
                postId = postId,
                authorId = authorId,
                groupId = groupId,
                reason = reasonVal,
                description = descriptionVal
            )

            when (val result = reportRepository.submitReport(report)) {
                is Result.Success -> {
                    _uiState.value = ReportUiState.Success
                }
                is Result.Error -> {
                    _uiState.value = ReportUiState.Error(result.message ?: "Failed to submit report")
                }
                else -> {}
            }
        }
    }

    fun clearError() {
        if (_uiState.value is ReportUiState.Error) {
            _uiState.value = ReportUiState.Idle
        }
    }
}
