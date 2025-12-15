package com.example.minisocialnetworkapplication.ui.socialgroup

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.minisocialnetworkapplication.ui.components.PostCard
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToComposePost: (String) -> Unit,
    onNavigateToPostDetail: (String) -> Unit,
    onNavigateToInvite: (String) -> Unit,
    onNavigateToProfile: (String) -> Unit,
    onNavigateToImageGallery: (String, Int) -> Unit,
    viewModel: GroupDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Show error messages
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            scope.launch {
                snackbarHostState.showSnackbar(it)
                viewModel.clearError()
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        floatingActionButton = {
            if (uiState is GroupDetailUiState.Success && (uiState as GroupDetailUiState.Success).isMember) {
                FloatingActionButton(
                    onClick = {
                        val group = (uiState as GroupDetailUiState.Success).group
                        onNavigateToComposePost(group.id)
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create Post")
                }
            }
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is GroupDetailUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is GroupDetailUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = state.message, color = MaterialTheme.colorScheme.error)
                }
            }
            is GroupDetailUiState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    item {
                        GroupHeader(
                            group = state.group,
                            isMember = state.isMember,
                            userRole = state.userRole,
                            onJoinClick = viewModel::joinGroup,
                            onLeaveClick = viewModel::leaveGroup,
                            onManageClick = { /* TODO: Navigate to manage screen */ },
                            onInviteClick = { onNavigateToInvite(state.group.id) }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Posts",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    // Check if user can see posts
                    val isPrivateGroup = state.group.privacy == com.example.minisocialnetworkapplication.core.domain.model.GroupPrivacy.PRIVATE
                    val canSeePosts = !isPrivateGroup || state.isMember

                    if (!canSeePosts) {
                        // Private group and user is not a member
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Private",
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.outline
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Bạn cần tham gia nhóm để xem bài viết",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else if (state.posts.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No posts yet.")
                            }
                        }
                    } else {
                        items(state.posts) { post ->
                            val isOwner = state.currentUserId == post.authorId
                            val isAdmin = state.userRole == com.example.minisocialnetworkapplication.core.domain.model.GroupRole.ADMIN
                            
                            PostCard(
                                post = post,
                                onPostClicked = { p -> onNavigateToPostDetail(p.id) },
                                onLikeClicked = { p -> viewModel.toggleLike(p) },
                                onCommentClicked = { p -> onNavigateToPostDetail(p.id) },
                                onAuthorClicked = { onNavigateToProfile(post.authorId) },
                                onImageClicked = { index -> onNavigateToImageGallery(post.id, index) },
                                onDeleteClicked = { p -> 
                                    if (isOwner || isAdmin) viewModel.deletePost(p.id) 
                                },
                                onEditClicked = { /* Edit will be handled via dialog */ },
                                isOptimisticallyLiked = post.likedByMe,
                                showMenuButton = isOwner || isAdmin
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GroupHeader(
    group: com.example.minisocialnetworkapplication.core.domain.model.Group,
    isMember: Boolean,
    userRole: com.example.minisocialnetworkapplication.core.domain.model.GroupRole?,
    onJoinClick: () -> Unit,
    onLeaveClick: () -> Unit,
    onManageClick: () -> Unit,
    onInviteClick: () -> Unit
) {
    Column {
        // Cover image with avatar overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            AsyncImage(
                model = group.coverUrl ?: "https://via.placeholder.com/800x400",
                contentDescription = "Cover Image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        
        // Avatar and Group Info Row
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Group Avatar - overlapping cover image
            Box(
                modifier = Modifier
                    .offset(y = (-40).dp)
                    .size(80.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                    )
                    .padding(3.dp)
            ) {
                AsyncImage(
                    model = group.avatarUrl ?: "https://ui-avatars.com/api/?name=${group.name}&background=6366f1&color=fff&size=200",
                    contentDescription = "Group Avatar",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(10.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Group Name and Info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 8.dp)
            ) {
                Text(text = group.name, style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = "${group.memberCount} members · ${group.privacy}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        }
        
        // Description
        Column(modifier = Modifier.padding(horizontal = 16.dp).offset(y = (-24).dp)) {
            Text(text = group.description, style = MaterialTheme.typography.bodyLarge)            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (isMember) {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                ) {
                    if (userRole == com.example.minisocialnetworkapplication.core.domain.model.GroupRole.ADMIN) {
                        OutlinedButton(
                            onClick = onManageClick,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Manage")
                        }
                        Button(
                            onClick = onInviteClick,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Invite")
                        }
                    } else {
                        OutlinedButton(
                            onClick = onLeaveClick,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Joined")
                        }
                        Button(
                            onClick = onInviteClick,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Invite")
                        }
                    }
                }
            } else {
                Button(onClick = onJoinClick, modifier = Modifier.fillMaxWidth()) {
                    Text("Join Group")
                }
            }
        }
    }
}
