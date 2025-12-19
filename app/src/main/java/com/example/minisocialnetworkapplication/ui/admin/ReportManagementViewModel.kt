package com.example.minisocialnetworkapplication.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.minisocialnetworkapplication.core.domain.model.Report
import com.example.minisocialnetworkapplication.core.domain.model.ReportStatus
import com.example.minisocialnetworkapplication.core.domain.repository.AdminRepository
import com.example.minisocialnetworkapplication.core.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ReportManagementUiState {
    data object Loading : ReportManagementUiState
    data class Success(val reports: List<Report>) : ReportManagementUiState
    data class Error(val message: String) : ReportManagementUiState
}

@HiltViewModel
class ReportManagementViewModel @Inject constructor(
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _statusFilter = MutableStateFlow<ReportStatus?>(ReportStatus.PENDING)
    val statusFilter = _statusFilter.asStateFlow()

    private val _allReports = MutableStateFlow<List<Report>>(emptyList())
    private val _uiState = MutableStateFlow<ReportManagementUiState>(ReportManagementUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        loadReports()
        observeFilters()
    }

    private fun loadReports() {
        viewModelScope.launch {
            adminRepository.getAllReports().collectLatest { result ->
                when (result) {
                    is Result.Success -> {
                        _allReports.value = result.data
                        applyFilters()
                    }
                    is Result.Error -> {
                        _uiState.value = ReportManagementUiState.Error(result.message ?: "Failed to load reports")
                    }
                    else -> {}
                }
            }
        }
    }

    private fun observeFilters() {
        viewModelScope.launch {
            searchQuery.collectLatest { applyFilters() }
        }
        viewModelScope.launch {
            statusFilter.collectLatest { applyFilters() }
        }
    }

    private fun applyFilters() {
        val query = _searchQuery.value.lowercase()
        val status = _statusFilter.value
        
        val filtered = _allReports.value.filter { report ->
            val matchesQuery = report.reporterName.lowercase().contains(query) || 
                              report.reason.lowercase().contains(query)
            val matchesStatus = status == null || report.status == status
            matchesQuery && matchesStatus
        }
        _uiState.value = ReportManagementUiState.Success(filtered)
    }

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onStatusFilterChange(newStatus: ReportStatus?) {
        _statusFilter.value = newStatus
    }

    fun resolveReport(reportId: String, action: String) {
        viewModelScope.launch {
            adminRepository.resolveReport(reportId, action)
        }
    }

    fun banUser(userId: String) {
        viewModelScope.launch {
            adminRepository.banUser(userId)
        }
    }

    fun hidePost(postId: String) {
        viewModelScope.launch {
            adminRepository.hidePost(postId)
        }
    }
}
