package com.example.minisocialnetworkapplication.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.minisocialnetworkapplication.core.domain.model.Report
import com.example.minisocialnetworkapplication.core.domain.model.ReportStatus
import com.example.minisocialnetworkapplication.core.domain.repository.AdminRepository
import com.example.minisocialnetworkapplication.core.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ReportManagementUiState {
    data object Loading : ReportManagementUiState
    data class Success(val reports: List<Report>) : ReportManagementUiState
    data class Error(val message: String) : ReportManagementUiState
}

data class WarningDialogState(
    val reportId: String,
    val targetUserId: String,
    val type: String, // POST, USER, GROUP
    val targetId: String? = null,
    val groupId: String? = null
)

@HiltViewModel
class ReportManagementViewModel @Inject constructor(
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _statusFilter = MutableStateFlow<ReportStatus?>(ReportStatus.PENDING)
    val statusFilter = _statusFilter.asStateFlow()

    private val _warningDialogState = MutableStateFlow<WarningDialogState?>(null)
    val warningDialogState = _warningDialogState.asStateFlow()

    // Single source of truth for UI state using combine
    val uiState: StateFlow<ReportManagementUiState> = combine(
        adminRepository.getAllReports(),
        _searchQuery,
        _statusFilter
    ) { result, query, status ->
        when (result) {
            is Result.Success -> {
                val lowercaseQuery = query.lowercase()
                val filtered = result.data.filter { report ->
                    val matchesQuery = report.reporterName.lowercase().contains(lowercaseQuery) ||
                                       report.reason.lowercase().contains(lowercaseQuery)
                    val matchesStatus = status == null || report.status == status
                    matchesQuery && matchesStatus
                }
                ReportManagementUiState.Success(filtered)
            }
            is Result.Error -> {
                ReportManagementUiState.Error(result.message ?: "Failed to load reports")
            }
            is Result.Loading -> ReportManagementUiState.Loading
            else -> ReportManagementUiState.Loading
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ReportManagementUiState.Loading
    )

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onStatusFilterChange(newStatus: ReportStatus?) {
        _statusFilter.value = newStatus
    }

    // Common Actions
    fun dismissReport(reportId: String) {
        viewModelScope.launch {
            adminRepository.resolveReport(reportId, "DISMISSED")
        }
    }

    fun openWarningDialog(report: Report) {
        _warningDialogState.value = WarningDialogState(
            reportId = report.id,
            targetUserId = report.authorId,
            type = report.targetType,
            targetId = report.targetId,
            groupId = report.groupId
        )
    }

    fun closeWarningDialog() {
        _warningDialogState.value = null
    }

    fun sendWarning(content: String) {
        val state = _warningDialogState.value ?: return
        viewModelScope.launch {
            val result = adminRepository.sendWarning(
                userId = state.targetUserId,
                content = content,
                type = state.type,
                targetId = state.targetId,
                groupId = state.groupId
            )
            if (result is Result.Success) {
                adminRepository.resolveReport(state.reportId, "RESOLVED")
                closeWarningDialog()
            }
        }
    }

    // User Actions
    fun banUserAndResolve(reportId: String, userId: String) {
        viewModelScope.launch {
            val result = adminRepository.banUser(userId)
            if (result is Result.Success) {
                adminRepository.resolveReport(reportId, "RESOLVED")
            }
        }
    }

    // Post Actions
    fun hidePostAndResolve(reportId: String, postId: String) {
        viewModelScope.launch {
            val result = adminRepository.hidePost(postId)
            if (result is Result.Success) {
                adminRepository.resolveReport(reportId, "RESOLVED")
            }
        }
    }

    fun deletePostAndResolve(reportId: String, postId: String) {
        viewModelScope.launch {
            val result = adminRepository.deletePost(postId)
            if (result is Result.Success) {
                adminRepository.resolveReport(reportId, "RESOLVED")
            }
        }
    }

    // Group Actions
    fun banGroupAndResolve(reportId: String, groupId: String) {
        viewModelScope.launch {
            val result = adminRepository.banGroup(groupId)
            if (result is Result.Success) {
                adminRepository.resolveReport(reportId, "RESOLVED")
            }
        }
    }

    fun resolveManually(reportId: String) {
        viewModelScope.launch {
            adminRepository.resolveReport(reportId, "RESOLVED")
        }
    }
}
