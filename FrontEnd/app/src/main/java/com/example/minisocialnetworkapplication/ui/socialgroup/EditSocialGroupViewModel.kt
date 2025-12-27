package com.example.minisocialnetworkapplication.ui.socialgroup

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.minisocialnetworkapplication.core.domain.model.Group
import com.example.minisocialnetworkapplication.core.domain.model.GroupPrivacy
import com.example.minisocialnetworkapplication.core.domain.repository.GroupRepository
import com.example.minisocialnetworkapplication.core.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class EditSocialGroupUiState {
    data object Idle : EditSocialGroupUiState()
    data object Loading : EditSocialGroupUiState()
    data class Success(val group: Group) : EditSocialGroupUiState()
    data object Updated : EditSocialGroupUiState()
    data class Error(val message: String) : EditSocialGroupUiState()
}

@HiltViewModel
class EditSocialGroupViewModel @Inject constructor(
    private val repository: GroupRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val groupId: String = checkNotNull(savedStateHandle["groupId"])
    
    private val _uiState = MutableStateFlow<EditSocialGroupUiState>(EditSocialGroupUiState.Loading)
    val uiState: StateFlow<EditSocialGroupUiState> = _uiState.asStateFlow()

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description.asStateFlow()

    private val _privacy = MutableStateFlow(GroupPrivacy.PUBLIC)
    val privacy: StateFlow<GroupPrivacy> = _privacy.asStateFlow()

    private val _avatarUri = MutableStateFlow<Uri?>(null)
    val avatarUri: StateFlow<Uri?> = _avatarUri.asStateFlow()

    private val _currentAvatarUrl = MutableStateFlow<String?>(null)
    val currentAvatarUrl: StateFlow<String?> = _currentAvatarUrl.asStateFlow()

    init {
        loadGroup()
    }

    private fun loadGroup() {
        viewModelScope.launch {
            _uiState.value = EditSocialGroupUiState.Loading
            when (val result = repository.getGroupDetails(groupId)) {
                is Result.Success -> {
                    val group = result.data
                    _name.value = group.name
                    _description.value = group.description
                    _privacy.value = group.privacy
                    _currentAvatarUrl.value = group.avatarUrl
                    _uiState.value = EditSocialGroupUiState.Success(group)
                }
                is Result.Error -> {
                    _uiState.value = EditSocialGroupUiState.Error(result.message ?: "Failed to load group")
                }
                is Result.Loading -> {
                    // Already set to Loading state before the when block
                }
            }
        }
    }

    fun onNameChange(newName: String) {
        _name.value = newName
    }

    fun onDescriptionChange(newDesc: String) {
        _description.value = newDesc
    }

    fun onPrivacyChange(newPrivacy: GroupPrivacy) {
        _privacy.value = newPrivacy
    }

    fun onAvatarChange(uri: Uri?) {
        _avatarUri.value = uri
    }

    fun updateGroup() {
        if (_name.value.isBlank()) {
            _uiState.value = EditSocialGroupUiState.Error("Group name cannot be empty")
            return
        }

        viewModelScope.launch {
            _uiState.value = EditSocialGroupUiState.Loading
            val result = repository.updateGroup(
                groupId = groupId,
                name = _name.value,
                description = _description.value,
                privacy = _privacy.value,
                avatarUri = _avatarUri.value
            )

            when (result) {
                is Result.Success -> {
                    _uiState.value = EditSocialGroupUiState.Updated
                }
                is Result.Error -> {
                    _uiState.value = EditSocialGroupUiState.Error(result.message ?: "Failed to update group")
                }
                is Result.Loading -> {
                    _uiState.value = EditSocialGroupUiState.Loading
                }
            }
        }
    }
}
