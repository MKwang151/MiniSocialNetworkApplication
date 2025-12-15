package com.example.minisocialnetworkapplication.ui.socialgroup

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.minisocialnetworkapplication.core.domain.model.GroupMember
import com.example.minisocialnetworkapplication.core.domain.model.GroupRole
import com.example.minisocialnetworkapplication.core.domain.repository.GroupRepository
import com.example.minisocialnetworkapplication.core.domain.repository.UserRepository
import com.example.minisocialnetworkapplication.core.util.Result
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MemberWithInfo(
    val member: GroupMember,
    val displayName: String = "",
    val avatarUrl: String? = null
)

data class GroupMembersState(
    val members: List<MemberWithInfo> = emptyList(),
    val filteredMembers: List<MemberWithInfo> = emptyList(),
    val searchQuery: String = "",
    val currentUserRole: GroupRole? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val actionMessage: String? = null
)

@HiltViewModel
class GroupMembersViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    private val userRepository: UserRepository,
    private val auth: FirebaseAuth,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val groupId: String = savedStateHandle.get<String>("groupId") ?: ""
    
    private val _state = MutableStateFlow(GroupMembersState())
    val state: StateFlow<GroupMembersState> = _state.asStateFlow()
    
    init {
        loadMembers()
        loadCurrentUserRole()
    }
    
    private fun loadCurrentUserRole() {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            when (val result = groupRepository.getMemberRole(groupId, userId)) {
                is Result.Success -> {
                    _state.value = _state.value.copy(currentUserRole = result.data)
                }
                is Result.Error -> {}
                else -> {}
            }
        }
    }
    
    private fun loadMembers() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            
            when (val result = groupRepository.getGroupMembers(groupId)) {
                is Result.Success -> {
                    // Fetch user info for each member
                    val membersWithInfo = result.data.map { member ->
                        val userResult = userRepository.getUser(member.userId)
                        val user = (userResult as? Result.Success<*>)?.data as? com.example.minisocialnetworkapplication.core.domain.model.User
                        MemberWithInfo(
                            member = member,
                            displayName = user?.name ?: "Unknown",
                            avatarUrl = user?.avatarUrl
                        )
                    }.sortedWith(compareByDescending<MemberWithInfo> { 
                        it.member.role == GroupRole.CREATOR 
                    }.thenByDescending { 
                        it.member.role == GroupRole.ADMIN 
                    })
                    
                    _state.value = _state.value.copy(
                        members = membersWithInfo,
                        filteredMembers = membersWithInfo,
                        isLoading = false
                    )
                }
                is Result.Error -> {
                    _state.value = _state.value.copy(
                        error = result.exception.message,
                        isLoading = false
                    )
                }
                else -> {}
            }
        }
    }
    
    fun onSearchQueryChange(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
        val filtered = if (query.isBlank()) {
            _state.value.members
        } else {
            _state.value.members.filter { 
                it.displayName.contains(query, ignoreCase = true) 
            }
        }
        _state.value = _state.value.copy(filteredMembers = filtered)
    }
    
    fun makeAdmin(userId: String) {
        viewModelScope.launch {
            when (val result = groupRepository.makeAdmin(groupId, userId)) {
                is Result.Success -> {
                    _state.value = _state.value.copy(actionMessage = "User promoted to admin")
                    loadMembers() // Reload to show updated role
                }
                is Result.Error -> {
                    _state.value = _state.value.copy(actionMessage = "Failed: ${result.exception.message}")
                }
                else -> {}
            }
        }
    }
    
    fun dismissAdmin(userId: String) {
        viewModelScope.launch {
            when (val result = groupRepository.dismissAdmin(groupId, userId)) {
                is Result.Success -> {
                    _state.value = _state.value.copy(actionMessage = "Admin dismissed")
                    loadMembers() // Reload to show updated role
                }
                is Result.Error -> {
                    _state.value = _state.value.copy(actionMessage = "Failed: ${result.exception.message}")
                }
                else -> {}
            }
        }
    }
    
    fun removeMember(userId: String) {
        viewModelScope.launch {
            when (val result = groupRepository.removeMember(groupId, userId)) {
                is Result.Success -> {
                    _state.value = _state.value.copy(actionMessage = "Member removed")
                    loadMembers() // Reload list
                }
                is Result.Error -> {
                    _state.value = _state.value.copy(actionMessage = "Failed: ${result.exception.message}")
                }
                else -> {}
            }
        }
    }
    
    fun clearActionMessage() {
        _state.value = _state.value.copy(actionMessage = null)
    }
}
