package com.example.minisocialnetworkapplication.ui.socialgroup

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.minisocialnetworkapplication.core.domain.model.Post
import com.example.minisocialnetworkapplication.core.domain.model.Report
import com.example.minisocialnetworkapplication.core.domain.model.ReportStatus
import com.example.minisocialnetworkapplication.core.domain.repository.GroupRepository
import com.example.minisocialnetworkapplication.core.domain.repository.ReportRepository
import com.example.minisocialnetworkapplication.core.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class GroupReportsState(
    val isLoading: Boolean = false,
    val reports: List<Report> = emptyList(),
    val hiddenPosts: List<Post> = emptyList(),
    val actionMessage: String? = null,
    val selectedTab: Int = 0 // 0 = Reports, 1 = Hidden Posts
)

@HiltViewModel
class GroupReportsViewModel @Inject constructor(
    private val reportRepository: ReportRepository,
    private val groupRepository: GroupRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val groupId: String = savedStateHandle.get<String>("groupId") ?: ""
    
    private val _state = MutableStateFlow(GroupReportsState())
    val state: StateFlow<GroupReportsState> = _state.asStateFlow()
    
    init {
        loadReports()
        loadHiddenPosts()
    }
    
    fun loadReports() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            
            when (val result = reportRepository.getReportsForGroup(groupId)) {
                is Result.Success -> {
                    // Only show pending reports
                    val pendingReports = result.data.filter { it.status == ReportStatus.PENDING }
                    _state.value = _state.value.copy(
                        isLoading = false,
                        reports = pendingReports
                    )
                }
                is Result.Error -> {
                    Timber.e(result.exception, "Failed to load reports")
                    _state.value = _state.value.copy(
                        isLoading = false,
                        actionMessage = "Failed to load reports"
                    )
                }
                else -> {}
            }
        }
    }
    
    private fun loadHiddenPosts() {
        viewModelScope.launch {
            when (val result = groupRepository.getHiddenPostsForGroup(groupId)) {
                is Result.Success -> {
                    _state.value = _state.value.copy(hiddenPosts = result.data)
                }
                is Result.Error -> {
                    Timber.e(result.exception, "Failed to load hidden posts")
                }
                else -> {}
            }
        }
    }
    
    fun selectTab(index: Int) {
        _state.value = _state.value.copy(selectedTab = index)
    }
    
    // ✅ Dismiss Report - Content is valid
    fun dismissReport(reportId: String) {
        viewModelScope.launch {
            when (val result = reportRepository.dismissReport(reportId)) {
                is Result.Success -> {
                    _state.value = _state.value.copy(
                        reports = _state.value.reports.filter { it.id != reportId },
                        actionMessage = "Report dismissed"
                    )
                }
                is Result.Error -> {
                    _state.value = _state.value.copy(actionMessage = "Failed to dismiss report")
                }
                else -> {}
            }
        }
    }
    
    // ⚠️ Hide Post - Temporarily hide (using approvalStatus = HIDDEN)
    fun hidePost(postId: String, reportId: String) {
        viewModelScope.launch {
            // First, find the report to get post info
            val report = _state.value.reports.find { it.id == reportId }
            
            when (val result = groupRepository.hidePost(postId)) {
                is Result.Success -> {
                    // Mark report as resolved
                    reportRepository.updateReportStatus(reportId, ReportStatus.RESOLVED)
                    
                    // Remove report from list
                    val updatedReports = _state.value.reports.filter { it.id != reportId }
                    
                    _state.value = _state.value.copy(
                        reports = updatedReports,
                        actionMessage = "Post hidden"
                    )
                    
                    // Refresh hidden posts from server to get the newly hidden post
                    loadHiddenPosts()
                }
                is Result.Error -> {
                    Timber.e(result.exception, "Failed to hide post")
                    _state.value = _state.value.copy(actionMessage = "Failed to hide post")
                }
                else -> {}
            }
        }
    }
    
    // 🔄 Restore Post - Un-hide (using isHidden field like AdminRepository)
    fun restorePost(postId: String) {
        viewModelScope.launch {
            when (val result = groupRepository.restorePost(postId)) {
                is Result.Success -> {
                    _state.value = _state.value.copy(
                        hiddenPosts = _state.value.hiddenPosts.filter { it.id != postId },
                        actionMessage = "Post restored"
                    )
                }
                is Result.Error -> {
                    _state.value = _state.value.copy(actionMessage = "Failed to restore post")
                }
                else -> {}
            }
        }
    }
    
    // ❌ Delete Post - Permanently delete
    fun deletePost(postId: String, reportId: String) {
        viewModelScope.launch {
            when (val result = groupRepository.deletePost(postId)) {
                is Result.Success -> {
                    reportRepository.updateReportStatus(reportId, ReportStatus.RESOLVED)
                    _state.value = _state.value.copy(
                        reports = _state.value.reports.filter { it.id != reportId },
                        actionMessage = "Post deleted"
                    )
                }
                is Result.Error -> {
                    _state.value = _state.value.copy(actionMessage = "Failed to delete post")
                }
                else -> {}
            }
        }
    }
    
    // 🚫 Kick Member from group + Delete post
    fun kickMember(memberId: String, postId: String, reportId: String) {
        viewModelScope.launch {
            // 1. Delete the post first
            val deleteResult = groupRepository.deletePost(postId)
            if (deleteResult is Result.Error) {
                Timber.e(deleteResult.exception, "Failed to delete post during kick")
            }
            
            // 2. Remove member from group
            when (val result = groupRepository.removeMember(groupId, memberId)) {
                is Result.Success -> {
                    reportRepository.updateReportStatus(reportId, ReportStatus.RESOLVED)
                    _state.value = _state.value.copy(
                        reports = _state.value.reports.filter { it.id != reportId },
                        actionMessage = "Member removed and post deleted"
                    )
                }
                is Result.Error -> {
                    _state.value = _state.value.copy(actionMessage = "Failed to remove member")
                }
                else -> {}
            }
        }
    }
    
    fun clearActionMessage() {
        _state.value = _state.value.copy(actionMessage = null)
    }
}
