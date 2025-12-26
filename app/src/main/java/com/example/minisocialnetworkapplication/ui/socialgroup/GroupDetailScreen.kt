package com.example.minisocialnetworkapplication.ui.socialgroup

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Report
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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.minisocialnetworkapplication.core.domain.model.Group
import com.example.minisocialnetworkapplication.ui.components.PostCard
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToComposePost: (String) -> Unit,
    onNavigateToPostDetail: (String) -> Unit,
    onNavigateToInvite: (String) -> Unit,
    onNavigateToProfile: (String) -> Unit,
    onNavigateToImageGallery: (String, Int) -> Unit,
    onNavigateToJoinRequests: (String) -> Unit = {},
    onNavigateToManage: (groupId: String, groupName: String) -> Unit = { _, _ -> },
    onNavigateToReport: (String) -> Unit = {},
    viewModel: GroupDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    // Edit post dialog state
    var showEditPostDialog by remember { mutableStateOf(false) }
    var postToEdit by remember { mutableStateOf<com.example.minisocialnetworkapplication.core.domain.model.Post?>(null) }

    // Show error messages
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            scope.launch {
                snackbarHostState.showSnackbar(it)
                viewModel.clearError()
            }
        }
    }
    
    // Show success messages (e.g., join request submitted)
    LaunchedEffect(successMessage) {
        successMessage?.let {
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = it,
                    duration = SnackbarDuration.Long
                )
                viewModel.clearSuccessMessage()
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
                actions = {
                    if (uiState is GroupDetailUiState.Success) {
                        val state = uiState as GroupDetailUiState.Success
                        var showMenu by remember { androidx.compose.runtime.mutableStateOf(false) }
                        
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More",
                                    tint = Color.Black
                                )
                            }
                            
                            androidx.compose.material3.DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text("Report Group") },
                                    onClick = {
                                        showMenu = false
                                        onNavigateToReport(state.group.id)
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Report,
                                            contentDescription = null
                                        )
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        floatingActionButton = {
            if (uiState is GroupDetailUiState.Success) {
                val state = uiState as GroupDetailUiState.Success
                if (state.isMember && state.group.status != Group.STATUS_BANNED) {
                    FloatingActionButton(
                        onClick = {
                            val group = state.group
                            onNavigateToComposePost(group.id)
                        },
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Create Post")
                    }
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
                            onLeaveClick = { viewModel.leaveGroup { /* No action needed */ } },
                            onManageClick = { onNavigateToManage(state.group.id, state.group.name) },
                            onInviteClick = { onNavigateToInvite(state.group.id) },
                            onJoinRequestsClick = { onNavigateToJoinRequests(state.group.id) }
                        )
                    }

                    if (state.group.status == Group.STATUS_BANNED) {
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Report,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(
                                        text = "This group has been banned for violating community standards.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
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
                                        text = "You need to join the group to see posts",
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
                            val isAdmin = state.userRole == com.example.minisocialnetworkapplication.core.domain.model.GroupRole.ADMIN ||
                                          state.userRole == com.example.minisocialnetworkapplication.core.domain.model.GroupRole.CREATOR
                            
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
                                onEditClicked = { p ->
                                    if (isOwner) {
                                        postToEdit = p
                                        showEditPostDialog = true
                                    }
                                },
                                isOptimisticallyLiked = post.likedByMe,
                                showMenuButton = isOwner || isAdmin
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Edit post dialog
    if (showEditPostDialog && postToEdit != null) {
        EditPostDialog(
            initialText = postToEdit?.text ?: "",
            onConfirm = { newText ->
                postToEdit?.let { post ->
                    viewModel.updatePost(post.id, newText)
                }
                showEditPostDialog = false
                postToEdit = null
            },
            onDismiss = {
                showEditPostDialog = false
                postToEdit = null
            }
        )
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
    onInviteClick: () -> Unit,
    onJoinRequestsClick: () -> Unit = {}
) {
    Column {
        // Cover Image = Group Avatar (200dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            AsyncImage(
                model = group.avatarUrl ?: "https://ui-avatars.com/api/?name=${java.net.URLEncoder.encode(group.name, "UTF-8")}&background=6366f1&color=fff&size=400",
                contentDescription = "Group Avatar",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        
        // Group Info
        Column(modifier = Modifier.padding(16.dp)) {
            // Group Name
            Text(
                text = group.name, 
                style = MaterialTheme.typography.headlineMedium
            )
            
            // Members · Privacy
            Text(
                text = "${group.memberCount} members · ${group.privacy}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Description
            Text(
                text = group.description, 
                style = MaterialTheme.typography.bodyLarge
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (isMember) {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                ) {
                    val isAdminOrCreator = userRole == com.example.minisocialnetworkapplication.core.domain.model.GroupRole.ADMIN ||
                                           userRole == com.example.minisocialnetworkapplication.core.domain.model.GroupRole.CREATOR
                    
                    if (isAdminOrCreator) {
                        OutlinedButton(
                            onClick = onManageClick,
                            modifier = Modifier.weight(1f),
                            enabled = group.status != Group.STATUS_BANNED
                        ) {
                            Text("Manage")
                        }
                        Button(
                            onClick = onInviteClick,
                            modifier = Modifier.weight(1f),
                            enabled = group.status != Group.STATUS_BANNED
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
                            modifier = Modifier.weight(1f),
                            enabled = group.status != Group.STATUS_BANNED
                        ) {
                            Text("Invite")
                        }
                    }
                }
            } else if (group.status != Group.STATUS_BANNED) {
                Button(onClick = onJoinClick, modifier = Modifier.fillMaxWidth()) {
                    Text("Join Group")
                }
            }
        }
    }
}

@Composable
private fun EditPostDialog(
    initialText: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initialText) }
    
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Post") },
        text = {
            androidx.compose.material3.OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                placeholder = { Text("Enter text...") },
                maxLines = 5
            )
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = { onConfirm(text) },
                enabled = text.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
