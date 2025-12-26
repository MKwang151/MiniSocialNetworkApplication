package com.example.minisocialnetworkapplication.ui.profile

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Report
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.minisocialnetworkapplication.core.domain.model.Friend
import com.example.minisocialnetworkapplication.core.domain.model.FriendStatus
import com.example.minisocialnetworkapplication.core.domain.model.Post
import com.example.minisocialnetworkapplication.core.domain.model.User
import com.example.minisocialnetworkapplication.ui.components.PostCard

// Modern color palette
private val GradientPrimary = listOf(Color(0xFF667EEA), Color(0xFF764BA2))
private val ColorAccent = Color(0xFF667EEA)
private val ColorSuccess = Color(0xFF11998E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPostDetail: (String) -> Unit,
    onNavigateToImageGallery: (String, Int) -> Unit = { _, _ -> },
    onNavigateToEditProfile: (String) -> Unit = {},
    onNavigateToChat: (String) -> Unit = {},
    onNavigateToReport: (String) -> Unit = {},
    shouldRefresh: Boolean = false,
    bottomBar: @Composable () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val userPosts = viewModel.userPostsFlow.collectAsLazyPagingItems()

    // Refresh when profile is updated
    LaunchedEffect(shouldRefresh) {
        if (shouldRefresh) {
            viewModel.refresh()
            userPosts.refresh()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Profile",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    val state = uiState
                    if (state is ProfileUiState.Success) {
                        if (state.isOwnProfile) {
                            IconButton(onClick = {
                                onNavigateToEditProfile(state.user.uid)
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Profile"
                                )
                            }
                        } else {
                            // Show Report button for other's profile
                            var showMenu by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                            
                            Box {
                                IconButton(onClick = { showMenu = true }) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "More"
                                    )
                                }
                                
                                androidx.compose.material3.DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false }
                                ) {
                                    androidx.compose.material3.DropdownMenuItem(
                                        text = { Text("Report User") },
                                        onClick = {
                                            showMenu = false
                                            onNavigateToReport(state.user.uid)
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
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                )
            )
        },
        bottomBar = bottomBar
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        )
                    )
                )
        ) {
            when (val state = uiState) {
                is ProfileUiState.Loading -> {
                    ModernLoadingView(modifier = Modifier.padding(paddingValues))
                }
                is ProfileUiState.Success -> {
                    ProfileContent(
                        user = state.user,
                        isOwnProfile = state.isOwnProfile,
                        userFriends = state.friends,
                        userPosts = userPosts,
                        onPostClicked = onNavigateToPostDetail,
                        onImageClicked = onNavigateToImageGallery,
                        onLikeClicked = viewModel::toggleLike,
                        onMessageClick = { onNavigateToChat(state.user.uid) },
                        friendStatus = state.friendStatus,
                        onFriendClick = when (state.friendStatus) {
                            FriendStatus.FRIEND -> viewModel::unfriend
                            FriendStatus.REQUEST_SENT -> viewModel::cancelRequest
                            FriendStatus.REQUEST_RECEIVED -> viewModel::acceptRequest
                            FriendStatus.NONE -> viewModel::sendRequest
                        },
                        onFriendDecline = viewModel::declineRequest,
                        modifier = Modifier.padding(paddingValues)
                    )
                }
                is ProfileUiState.Error -> {
                    ModernErrorView(
                        message = state.message,
                        onRetryClick = viewModel::refresh,
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileContent(
    user: User,
    isOwnProfile: Boolean,
    userFriends: List<Friend>,
    userPosts: LazyPagingItems<Post>,
    onPostClicked: (String) -> Unit,
    onImageClicked: (String, Int) -> Unit = { _, _ -> },
    onLikeClicked: (Post) -> Unit,
    friendStatus: FriendStatus,
    onFriendClick: () -> Unit,
    onFriendDecline: () -> Unit,
    onMessageClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        // Profile header
        item {
            ModernProfileHeader(
                user = user,
                isOwnProfile = isOwnProfile,
                friendCount = userFriends.count(),
                postCount = userPosts.itemCount,
                friendStatus = friendStatus,
                onFriendClick = onFriendClick,
                onFriendDecline = onFriendDecline,
                onMessageClick = onMessageClick
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Posts section header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Posts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(8.dp))
                Surface(
                    color = ColorAccent.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${userPosts.itemCount}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = ColorAccent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // User posts
        items(
            count = userPosts.itemCount,
            key = { index -> userPosts.peek(index)?.id ?: index }
        ) { index ->
            val post = userPosts[index]
            if (post != null) {
                PostCard(
                    post = post,
                    onLikeClicked = { onLikeClicked(post) },
                    onCommentClicked = { onPostClicked(post.id) },
                    onPostClicked = { onPostClicked(post.id) },
                    onAuthorClicked = {},
                    onImageClicked = { imageIndex -> onImageClicked(post.id, imageIndex) },
                    isOptimisticallyLiked = post.likedByMe
                )
            }
        }

        // Loading indicator
        when (userPosts.loadState.append) {
            is LoadState.Loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 3.dp,
                            color = ColorAccent
                        )
                    }
                }
            }
            else -> {}
        }

        // Empty state
        if (userPosts.loadState.refresh is LoadState.NotLoading && userPosts.itemCount == 0) {
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
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = if (isOwnProfile) {
                                "You haven't posted anything yet"
                            } else {
                                "No posts yet"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ModernProfileHeader(
    user: User,
    isOwnProfile: Boolean,
    friendCount: Int,
    postCount: Int,
    friendStatus: FriendStatus,
    onFriendClick: () -> Unit,
    onMessageClick: () -> Unit = {},
    onFriendDecline: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .shadow(6.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar with gradient fallback
            Surface(
                modifier = Modifier.size(100.dp),
                shape = CircleShape,
                color = Color.Transparent,
                shadowElevation = 4.dp
            ) {
                if (!user.avatarUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = user.avatarUrl,
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(GradientPrimary),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = user.name.take(2).uppercase(),
                            style = MaterialTheme.typography.headlineLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Name
            Text(
                text = user.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Email
            Text(
                text = user.email,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Bio - NEW SECTION
            if (!user.bio.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = user.bio,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ModernStatItem(
                    count = postCount,
                    label = "Posts",
                    color = ColorAccent
                )
                
                // Divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(40.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )

                ModernStatItem(
                    count = friendCount,
                    label = "Friends",
                    color = ColorSuccess
                )
            }

            if (!isOwnProfile) {
                Spacer(modifier = Modifier.height(20.dp))
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Friend action button
                    val (buttonColor, buttonText, buttonIcon) = when (friendStatus) {
                        FriendStatus.FRIEND -> Triple(
                            MaterialTheme.colorScheme.error,
                            "Unfriend",
                            Icons.Default.PersonOff
                        )
                        FriendStatus.REQUEST_SENT -> Triple(
                            Color.Gray,
                            "Cancel",
                            Icons.Default.PersonOff
                        )
                        FriendStatus.REQUEST_RECEIVED -> Triple(
                            ColorSuccess,
                            "Accept",
                            Icons.Default.PersonAdd
                        )
                        FriendStatus.NONE -> Triple(
                            ColorAccent,
                            "Add Friend",
                            Icons.Default.PersonAdd
                        )
                    }
                    
                    OutlinedButton(
                        onClick = onFriendClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, buttonColor)
                    ) {
                        Icon(
                            buttonIcon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = buttonColor
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            buttonText,
                            color = buttonColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (friendStatus == FriendStatus.REQUEST_RECEIVED) {
                        OutlinedButton(
                            onClick = onFriendDecline,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                        ) {
                            Text(
                                "Decline",
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        // Message button
                        Button(
                            onClick = onMessageClick,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ColorAccent
                            )
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Message,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Message",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModernStatItem(
    count: Int,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ModernLoadingView(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize()) {
        // Profile header shimmer
        com.example.minisocialnetworkapplication.ui.components.ProfileHeaderShimmer()

        Spacer(modifier = Modifier.height(16.dp))

        // Posts shimmer
        repeat(2) {
            com.example.minisocialnetworkapplication.ui.components.PostCardShimmer()
        }
    }
}

@Composable
fun ModernErrorView(
    message: String,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            // Error emoji
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .background(
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(24.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "😕",
                    style = MaterialTheme.typography.displayMedium
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Oops!",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onRetryClick,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ColorAccent
                )
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Retry", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// Keep backward compatibility
@Composable
fun LoadingView(modifier: Modifier = Modifier) = ModernLoadingView(modifier)

@Composable
fun ErrorView(
    message: String,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) = ModernErrorView(message, onRetryClick, modifier)

@Composable
fun ProfileHeader(
    user: User,
    isOwnProfile: Boolean,
    friendCount: Int,
    postCount: Int,
    friendStatus: FriendStatus,
    onFriendClick: () -> Unit,
    onMessageClick: () -> Unit = {},
    onFriendDecline: () -> Unit,
    modifier: Modifier = Modifier
) = ModernProfileHeader(
    user = user,
    isOwnProfile = isOwnProfile,
    friendCount = friendCount,
    postCount = postCount,
    friendStatus = friendStatus,
    onFriendClick = onFriendClick,
    onMessageClick = onMessageClick,
    onFriendDecline = onFriendDecline,
    modifier = modifier
)

@Composable
fun StatItem(
    count: Int,
    label: String,
    modifier: Modifier = Modifier
) = ModernStatItem(
    count = count,
    label = label,
    color = ColorAccent,
    modifier = modifier
)
