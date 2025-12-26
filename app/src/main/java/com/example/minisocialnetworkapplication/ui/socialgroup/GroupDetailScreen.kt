package com.example.minisocialnetworkapplication.ui.socialgroup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Report
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.minisocialnetworkapplication.core.domain.model.Group
import com.example.minisocialnetworkapplication.ui.components.PostCard
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import timber.log.Timber

// Modern color palette
private val GradientPrimary = listOf(Color(0xFF667EEA), Color(0xFF764BA2))
private val ColorAccent = Color(0xFF667EEA)

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
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
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
                                    tint = Color.White
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
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
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
                        containerColor = ColorAccent,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.shadow(8.dp, RoundedCornerShape(16.dp))
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Create Post",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is GroupDetailUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        strokeWidth = 3.dp,
                        color = ColorAccent
                    )
                }
            }
            is GroupDetailUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "😕",
                            style = MaterialTheme.typography.displayMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
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
                        ModernGroupHeader(
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
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                shape = RoundedCornerShape(16.dp)
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
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Posts",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = ColorAccent.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "${state.posts.size}",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ColorAccent,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
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
                                    Box(
                                        modifier = Modifier
                                            .size(80.dp)
                                            .background(
                                                Brush.linearGradient(
                                                    colors = listOf(
                                                        ColorAccent.copy(alpha = 0.15f),
                                                        Color(0xFF764BA2).copy(alpha = 0.15f)
                                                    )
                                                ),
                                                shape = RoundedCornerShape(24.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Private",
                                            modifier = Modifier.size(36.dp),
                                            tint = ColorAccent
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Private Group",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Join this group to see posts",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
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
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "📝",
                                        style = MaterialTheme.typography.displayMedium
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "No posts yet",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
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
        ModernEditPostDialog(
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
fun ModernGroupHeader(
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
        // Cover Image = Group Avatar (220dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
        ) {
            AsyncImage(
                model = group.avatarUrl ?: "https://ui-avatars.com/api/?name=${java.net.URLEncoder.encode(group.name, "UTF-8")}&background=667EEA&color=fff&size=400",
                contentDescription = "Group Avatar",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.4f)
                            ),
                            startY = 100f
                        )
                    )
            )
        }
        
        // Group Info Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .offset(y = (-24).dp)
                .shadow(6.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Group Name
                Text(
                    text = group.name, 
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                
                // Members · Privacy
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = ColorAccent.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${group.memberCount} members",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = ColorAccent,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "· ${group.privacy}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Description
                if (group.description.isNotBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = group.description,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp),
                            lineHeight = 20.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (isMember) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val isAdminOrCreator = userRole == com.example.minisocialnetworkapplication.core.domain.model.GroupRole.ADMIN ||
                                               userRole == com.example.minisocialnetworkapplication.core.domain.model.GroupRole.CREATOR
                        
                        if (isAdminOrCreator) {
                            OutlinedButton(
                                onClick = onManageClick,
                                modifier = Modifier.weight(1f),
                                enabled = group.status != Group.STATUS_BANNED,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Manage", fontWeight = FontWeight.SemiBold)
                            }
                            Button(
                                onClick = onInviteClick,
                                modifier = Modifier.weight(1f),
                                enabled = group.status != Group.STATUS_BANNED,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ColorAccent
                                )
                            ) {
                                Text("Invite", fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            OutlinedButton(
                                onClick = onLeaveClick,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Joined ✓", fontWeight = FontWeight.SemiBold)
                            }
                            Button(
                                onClick = onInviteClick,
                                modifier = Modifier.weight(1f),
                                enabled = group.status != Group.STATUS_BANNED,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ColorAccent
                                )
                            ) {
                                Text("Invite", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                } else if (group.status != Group.STATUS_BANNED) {
                    Button(
                        onClick = onJoinClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ColorAccent
                        )
                    ) {
                        Text(
                            "Join Group",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernEditPostDialog(
    initialText: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initialText) }
    
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Edit Post",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            androidx.compose.material3.OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                placeholder = { Text("Enter text...") },
                maxLines = 5,
                shape = RoundedCornerShape(14.dp)
            )
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = { onConfirm(text) },
                enabled = text.isNotBlank()
            ) {
                Text(
                    "Save",
                    color = ColorAccent,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

// Keep backward compatibility
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
) = ModernGroupHeader(
    group = group,
    isMember = isMember,
    userRole = userRole,
    onJoinClick = onJoinClick,
    onLeaveClick = onLeaveClick,
    onManageClick = onManageClick,
    onInviteClick = onInviteClick,
    onJoinRequestsClick = onJoinRequestsClick
)
