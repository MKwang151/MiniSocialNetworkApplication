package com.example.minisocialnetworkapplication.ui.socialgroup

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.minisocialnetworkapplication.core.domain.model.Group
import com.example.minisocialnetworkapplication.core.domain.repository.GroupRepository
import com.example.minisocialnetworkapplication.core.util.Result
import com.example.minisocialnetworkapplication.ui.auth.AuthViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface GroupDetailUiState {
    data object Loading : GroupDetailUiState
    data class Success(
        val group: Group,
        val isMember: Boolean = false,
        val posts: List<com.example.minisocialnetworkapplication.core.domain.model.Post> = emptyList()
    ) : GroupDetailUiState
    data class Error(val message: String) : GroupDetailUiState
}

@HiltViewModel
class GroupDetailViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    private val authViewModel: AuthViewModel,
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
            val currentUser = authViewModel.currentUser.value
            
            var isMember = false
            if (currentUser != null) {
                val memberResult = groupRepository.isMember(groupId, currentUser.id)
                if (memberResult is Result.Success) {
                    isMember = memberResult.data
                }
            }

            // Initially set success with group and membership
            _uiState.value = GroupDetailUiState.Success(group = group, isMember = isMember)

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
