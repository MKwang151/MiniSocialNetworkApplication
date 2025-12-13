package com.example.minisocialnetworkapplication.ui.socialgroup

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.minisocialnetworkapplication.core.domain.model.GroupPrivacy
import com.example.minisocialnetworkapplication.core.domain.repository.FriendRepository
import com.example.minisocialnetworkapplication.core.domain.repository.GroupRepository
import com.example.minisocialnetworkapplication.core.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface CreateGroupUiState {
    data object Idle : CreateGroupUiState
    data object Loading : CreateGroupUiState
    data class Success(val groupId: String) : CreateGroupUiState
    data class Error(val message: String) : CreateGroupUiState
}

@HiltViewModel
class CreateSocialGroupViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    private val friendRepository: FriendRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CreateGroupUiState>(CreateGroupUiState.Idle)
    val uiState: StateFlow<CreateGroupUiState> = _uiState.asStateFlow()

    // Step management
    private val _currentStep = MutableStateFlow(0)
    val currentStep = _currentStep.asStateFlow()

    // Step 1: Basic Info
    private val _name = MutableStateFlow("")
    val name = _name.asStateFlow()

    private val _description = MutableStateFlow("")
    val description = _description.asStateFlow()
    
    private val _privacy = MutableStateFlow(GroupPrivacy.PUBLIC)
    val privacy = _privacy.asStateFlow()
    
    private val _avatarUri = MutableStateFlow<Uri?>(null)
    val avatarUri = _avatarUri.asStateFlow()

    // Step 2: Friend Selection
    private val _friends = MutableStateFlow<List<com.example.minisocialnetworkapplication.core.domain.model.User>>(emptyList())
    val friends = _friends.asStateFlow()
    
    private val _selectedFriendIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedFriendIds = _selectedFriendIds.asStateFlow()

    init {
        loadFriends()
    }

    private fun loadFriends() {
        viewModelScope.launch {
            friendRepository.getFriends().collect { friendsList ->
                _friends.value = friendsList
            }
        }
    }

    fun onNameChange(value: String) { _name.value = value }
    fun onDescriptionChange(value: String) { _description.value = value }
    fun onPrivacyChange(value: GroupPrivacy) { _privacy.value = value }
    fun onAvatarSelected(uri: Uri?) { _avatarUri.value = uri }
    
    fun toggleFriendSelection(friendId: String) {
        _selectedFriendIds.value = if (friendId in _selectedFriendIds.value) {
            _selectedFriendIds.value - friendId
        } else {
            _selectedFriendIds.value + friendId
        }
    }

    fun nextStep() {
        if (_currentStep.value == 0) {
            // Validate Step 1
            if (_name.value.isBlank()) {
                _uiState.value = CreateGroupUiState.Error("Name cannot be empty")
                return
            }
            _currentStep.value = 1
            _uiState.value = CreateGroupUiState.Idle // Reset error
        }
    }

    fun previousStep() {
        if (_currentStep.value > 0) {
            _currentStep.value--
            _uiState.value = CreateGroupUiState.Idle
        }
    }

    fun createGroup() {
        val nameVal = _name.value
        val descVal = _description.value
        val privacyVal = _privacy.value
        val selectedFriends = _selectedFriendIds.value.toList()

        if (nameVal.isBlank()) {
            _uiState.value = CreateGroupUiState.Error("Name cannot be empty")
            return
        }

        viewModelScope.launch {
            _uiState.value = CreateGroupUiState.Loading
            
            // Create the group
            val result = groupRepository.createGroup(nameVal, descVal, privacyVal)
            when (result) {
                is Result.Success -> {
                    val groupId = result.data
                    
                    // Send invitations if any friends selected
                    if (selectedFriends.isNotEmpty()) {
                        groupRepository.sendInvitations(groupId, selectedFriends)
                    }
                    
                    _uiState.value = CreateGroupUiState.Success(groupId)
                }
                is Result.Error -> {
                    _uiState.value = CreateGroupUiState.Error(result.message ?: "Unknown error")
                }
                is Result.Loading -> {
                    _uiState.value = CreateGroupUiState.Loading
                }
            }
        }
    }
}
