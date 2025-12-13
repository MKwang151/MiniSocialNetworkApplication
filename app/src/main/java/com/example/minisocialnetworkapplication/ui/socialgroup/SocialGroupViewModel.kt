package com.example.minisocialnetworkapplication.ui.socialgroup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.minisocialnetworkapplication.core.domain.model.Group
import com.example.minisocialnetworkapplication.core.domain.repository.GroupRepository

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SocialGroupUiState {
    data object Loading : SocialGroupUiState
    data class Success(
        val myGroups: List<Group> = emptyList(),
        val discoverGroups: List<Group> = emptyList(),
        val managedGroups: List<Group> = emptyList() // TODO: Filter admin roles
    ) : SocialGroupUiState
    data class Error(val message: String) : SocialGroupUiState
}

@HiltViewModel
class SocialGroupViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    private val getCurrentUserUseCase: com.example.minisocialnetworkapplication.core.domain.usecase.auth.GetCurrentUserUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<SocialGroupUiState>(SocialGroupUiState.Loading)
    val uiState: StateFlow<SocialGroupUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            // Collect user flow
            getCurrentUserUseCase().collect { user ->
                 if (user == null) {
                    _uiState.value = SocialGroupUiState.Error("User not logged in")
                    return@collect
                }
                
                // Once we have user, load groups
                 // 1. My Groups
                launch {
                    groupRepository.getGroupsForUser(user.id).collect { groups ->
                        updateState { currentState ->
                            currentState.copy(myGroups = groups, managedGroups = groups.filter { it.ownerId == user.id })
                        }
                    }
                }

                // 2. Discover (All groups for now)
                launch {
                    groupRepository.getAllGroups().collect { groups ->
                        updateState { currentState ->
                            currentState.copy(discoverGroups = groups)
                        }
                    }
                }
            }
        }
    }
    
    private fun updateState(update: (SocialGroupUiState.Success) -> SocialGroupUiState.Success) {
        val current = _uiState.value
        if (current is SocialGroupUiState.Success) {
            _uiState.value = update(current)
        } else {
            // Initialize with empty success then update
            _uiState.value = update(SocialGroupUiState.Success())
        }
    }
}
