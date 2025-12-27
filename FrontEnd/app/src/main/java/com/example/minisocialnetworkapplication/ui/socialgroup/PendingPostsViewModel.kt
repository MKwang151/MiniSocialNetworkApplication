package com.example.minisocialnetworkapplication.ui.socialgroup

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.minisocialnetworkapplication.core.domain.model.Post
import com.example.minisocialnetworkapplication.core.domain.repository.GroupRepository
import com.example.minisocialnetworkapplication.core.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PendingPostsState(
    val posts: List<Post> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val actionMessage: String? = null
)

@HiltViewModel
class PendingPostsViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val groupId: String = savedStateHandle.get<String>("groupId") ?: ""
    
    private val _state = MutableStateFlow(PendingPostsState())
    val state: StateFlow<PendingPostsState> = _state.asStateFlow()
    
    init {
        loadPendingPosts()
    }
    
    private fun loadPendingPosts() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            
            groupRepository.getPendingPosts(groupId)
                .catch { e ->
                    _state.value = _state.value.copy(
                        error = e.message,
                        isLoading = false
                    )
                }
                .collect { posts ->
                    _state.value = _state.value.copy(
                        posts = posts,
                        isLoading = false
                    )
                }
        }
    }
    
    fun approvePost(postId: String) {
        viewModelScope.launch {
            when (val result = groupRepository.approvePost(postId)) {
                is Result.Success -> {
                    _state.value = _state.value.copy(actionMessage = "Post approved")
                }
                is Result.Error -> {
                    _state.value = _state.value.copy(actionMessage = "Failed: ${result.exception.message}")
                }
                else -> {}
            }
        }
    }
    
    fun rejectPost(postId: String, reason: String? = null) {
        viewModelScope.launch {
            when (val result = groupRepository.rejectPost(postId, reason)) {
                is Result.Success -> {
                    _state.value = _state.value.copy(actionMessage = "Post rejected")
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
