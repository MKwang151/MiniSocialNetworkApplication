package com.example.minisocialnetworkapplication.ui.socialgroup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.minisocialnetworkapplication.core.domain.model.GroupPrivacy
import com.example.minisocialnetworkapplication.core.domain.model.GroupPostingPermission
import com.example.minisocialnetworkapplication.core.domain.repository.GroupRepository
import com.example.minisocialnetworkapplication.core.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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
    private val groupRepository: GroupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CreateGroupUiState>(CreateGroupUiState.Idle)
    val uiState: StateFlow<CreateGroupUiState> = _uiState.asStateFlow()

    private val _name = MutableStateFlow("")
    val name = _name.asStateFlow()

    private val _description = MutableStateFlow("")
    val description = _description.asStateFlow()
    
    // Privacy: Public by default
    private val _privacy = MutableStateFlow(GroupPrivacy.PUBLIC)
    val privacy = _privacy.asStateFlow()

    // TODO: Add these to Repository createGroup signature if needed, or update afterwards.
    // For now, let's assume createGroup only takes basics, and we'll update the rest later 
    // OR we update the repository signature right now.
    // The plan says "Inputs + Settings toggles". 
    // I should update Repository to accept these settings.
    
    fun onNameChange(value: String) { _name.value = value }
    fun onDescriptionChange(value: String) { _description.value = value }
    fun onPrivacyChange(value: GroupPrivacy) { _privacy.value = value }

    fun createGroup() {
        val nameVal = _name.value
        val descVal = _description.value
        val privacyVal = _privacy.value

        if (nameVal.isBlank()) {
            _uiState.value = CreateGroupUiState.Error("Name cannot be empty")
            return
        }

        viewModelScope.launch {
            _uiState.value = CreateGroupUiState.Loading
            val result = groupRepository.createGroup(nameVal, descVal, privacyVal)
            when (result) {
                is Result.Success -> {
                    _uiState.value = CreateGroupUiState.Success(result.data)
                }
                is Result.Error -> {
                    _uiState.value = CreateGroupUiState.Error(result.message)
                }
            }
        }
    }
}
