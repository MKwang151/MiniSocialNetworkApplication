package com.example.minisocialnetworkapplication.ui.socialgroup

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.minisocialnetworkapplication.core.domain.model.Group
import com.example.minisocialnetworkapplication.core.domain.repository.GroupRepository
import com.example.minisocialnetworkapplication.core.util.Result

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface GroupDetailUiState {
    data object Loading : GroupDetailUiState
    data class Success(
        val group: Group,
        val isMember: Boolean = false,
        val userRole: com.example.minisocialnetworkapplication.core.domain.model.GroupRole? = null,
        val posts: List<com.example.minisocialnetworkapplication.core.domain.model.Post> = emptyList()
    ) : GroupDetailUiState
    data class Error(val message: String) : GroupDetailUiState
}

@HiltViewModel
class GroupDetailViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    private val getCurrentUserUseCase: com.example.minisocialnetworkapplication.core.domain.usecase.auth.GetCurrentUserUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Expect "groupId" to be passed in navigation arguments
    private val groupId: String = checkNotNull(savedStateHandle["groupId"])

    private val _uiState = MutableStateFlow<GroupDetailUiState>(GroupDetailUiState.Loading)
    val uiState: StateFlow<GroupDetailUiState> = _uiState.asStateFlow()

    init {
        loadGroupDetails()
    }

    private fun loadGroupDetails() {
        viewModelScope.launch {
            _uiState.value = GroupDetailUiState.Loading
            
            val groupResult = groupRepository.getGroupDetails(groupId)
            if (groupResult is Result.Error) {
                _uiState.value = GroupDetailUiState.Error(groupResult.message ?: "Failed to load group")
                return@launch
            }
            
            val group = (groupResult as Result.Success).data
            
            // Get current user (suspend call or flow collection handled here for simplicity)
            // Ideally we observe flow, but for single check:
            val currentUser = getCurrentUserUseCase().firstOrNull()
            
            var isMember = false
            var userRole: com.example.minisocialnetworkapplication.core.domain.model.GroupRole? = null
            
            if (currentUser != null) {
                val memberResult = groupRepository.isMember(groupId, currentUser.id)
                if (memberResult is Result.Success) {
                    isMember = memberResult.data
                }
                
                // If member, fetch role
                if (isMember) {
                    val roleResult = groupRepository.getMemberRole(groupId, currentUser.id)
                    if (roleResult is Result.Success) {
                        userRole = roleResult.data
                    }
                }
            }

            // Initially set success with group, membership and role
            _uiState.value = GroupDetailUiState.Success(
                group = group,
                isMember = isMember,
                userRole = userRole
            )

            // Now load posts if public or member
            if (group.privacy == com.example.minisocialnetworkapplication.core.domain.model.GroupPrivacy.PUBLIC || isMember) {
                 loadPosts()
            }
        }
    }

    private fun loadPosts() {
        viewModelScope.launch {
            groupRepository.getGroupPosts(groupId).collect { posts ->
                val current = _uiState.value
                if (current is GroupDetailUiState.Success) {
                    _uiState.value = current.copy(posts = posts)
                }
            }
        }
    }

    fun joinGroup() {
        viewModelScope.launch {
            val result = groupRepository.joinGroup(groupId)
            if (result is Result.Success) {
                // Refresh state
                loadGroupDetails()
            } else if (result is Result.Error) {
                // Ideally show snackbar, for now just log or generic error state?
                // Keeping it simple, maybe update error message in state if critical?
            }
        }
    }

    fun leaveGroup() {
        viewModelScope.launch {
            val result = groupRepository.leaveGroup(groupId)
            if (result is Result.Success) {
                loadGroupDetails()
            }
        }
    }
}
