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
        val allPosts: List<com.example.minisocialnetworkapplication.core.domain.model.Post> = emptyList(),
        val discoverGroups: List<Group> = emptyList(),
        val managedGroups: List<Group> = emptyList()
    ) : SocialGroupUiState
    data class Error(val message: String) : SocialGroupUiState
}

@HiltViewModel
class SocialGroupViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    private val getCurrentUserUseCase: com.example.minisocialnetworkapplication.core.domain.usecase.auth.GetCurrentUserUseCase,
    private val toggleLikeUseCase: com.example.minisocialnetworkapplication.core.domain.usecase.post.ToggleLikeUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<SocialGroupUiState>(SocialGroupUiState.Loading)
    val uiState: StateFlow<SocialGroupUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            // Store all groups temporarily for filtering
            var allGroupsCache: List<Group> = emptyList()
            
            // Collect user flow
            getCurrentUserUseCase().collect { user ->
                 if (user == null) {
                    _uiState.value = SocialGroupUiState.Error("User not logged in")
                    return@collect
                }
                
                // Once we have user, load data for all tabs
                
                // 1. Your Groups - Groups user has joined
                launch {
                    groupRepository.getGroupsForUser(user.id).collect { groups ->
                        updateState { currentState ->
                            // Recompute discover when myGroups changes
                            val myGroupIds = groups.map { it.id }.toSet()
                            val filteredDiscover = allGroupsCache.filter { it.id !in myGroupIds }
                            currentState.copy(
                                myGroups = groups,
                                discoverGroups = filteredDiscover
                            )
                        }
                    }
                }
                
                // 2. Posts - All posts from groups user joined
                launch {
                    groupRepository.getAllPostsFromUserGroups(user.id).collect { posts ->
                        updateState { currentState ->
                            currentState.copy(allPosts = posts)
                        }
                    }
                }

                // 3. Discover - All groups, filtered to exclude joined ones
                launch {
                    groupRepository.getAllGroups().collect { groups ->
                        allGroupsCache = groups
                        updateState { currentState ->
                            // Filter out groups user already joined
                            val myGroupIds = currentState.myGroups.map { it.id }.toSet()
                            val notJoinedGroups = groups.filter { it.id !in myGroupIds }
                            currentState.copy(discoverGroups = notJoinedGroups)
                        }
                    }
                }
                
                // 4. Manage - Groups where user is ADMIN
                launch {
                    groupRepository.getGroupsWhereUserIsAdmin(user.id).collect { adminGroups ->
                        updateState { currentState ->
                            currentState.copy(managedGroups = adminGroups)
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

    fun joinGroup(groupId: String) {
        viewModelScope.launch {
            val result = groupRepository.joinGroup(groupId)
            if (result is com.example.minisocialnetworkapplication.core.util.Result.Success) {
                // Remove from discover and add to myGroups
                val current = _uiState.value
                if (current is SocialGroupUiState.Success) {
                    val joinedGroup = current.discoverGroups.find { it.id == groupId }
                    if (joinedGroup != null) {
                        _uiState.value = current.copy(
                            discoverGroups = current.discoverGroups.filter { it.id != groupId },
                            myGroups = current.myGroups + joinedGroup
                        )
                    }
                }
            }
        }
    }

    fun toggleLike(post: com.example.minisocialnetworkapplication.core.domain.model.Post) {
        viewModelScope.launch {
            // Optimistic update
            updateState { currentState ->
                val updatedPosts = currentState.allPosts.map { p ->
                    if (p.id == post.id) {
                        p.copy(
                            likedByMe = !p.likedByMe,
                            likeCount = if (p.likedByMe) p.likeCount - 1 else p.likeCount + 1
                        )
                    } else p
                }
                currentState.copy(allPosts = updatedPosts)
            }
            
            // Actually toggle like in repository (fire and forget for optimistic UI)
            toggleLikeUseCase(post.id)
        }
    }
}
