package com.example.minisocialnetworkapplication.ui.profile

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.minisocialnetworkapplication.core.domain.model.Friend
import com.example.minisocialnetworkapplication.core.domain.model.FriendStatus
import com.example.minisocialnetworkapplication.core.domain.model.Post
import com.example.minisocialnetworkapplication.core.domain.model.User
import com.example.minisocialnetworkapplication.ui.components.PostCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPostDetail: (String) -> Unit,
    onNavigateToImageGallery: (String, Int) -> Unit = { _, _ -> },
    onNavigateToEditProfile: (String) -> Unit = {},
    onNavigateToChat: (String) -> Unit = {},
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
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Show Edit button only for own profile
                    if (uiState is ProfileUiState.Success && (uiState as ProfileUiState.Success).isOwnProfile) {
                        IconButton(onClick = {
                            onNavigateToEditProfile((uiState as ProfileUiState.Success).user.uid)
                        }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Profile"
                            )
                        }
                    }
                }
            )
        },
        bottomBar = bottomBar
    ) { paddingValues ->
        when (val state = uiState) {
            is ProfileUiState.Loading -> {
                LoadingView(modifier = Modifier.padding(paddingValues))
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
                ErrorView(
                    message = state.message,
                    onRetryClick = viewModel::refresh,
                    modifier = Modifier.padding(paddingValues)
                )
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
            ProfileHeader(
                user = user,
                isOwnProfile = isOwnProfile,
                friendCount = userFriends.count(),
                postCount = userPosts.itemCount,
                friendStatus = friendStatus,
                onFriendClick = onFriendClick,
                onFriendDecline = onFriendDecline,
                onMessageClick = onMessageClick
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            
            // Posts section header
            Text(
                text = "Posts",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
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
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
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
                    Text(
                        text = if (isOwnProfile) {
                            "You haven't posted anything yet"
                        } else {
                            "No posts yet"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

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
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Avatar
        Surface(
            modifier = Modifier.size(80.dp),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = user.name.take(2).uppercase(),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Name
        Text(
            text = user.name,
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Email
        Text(
            text = user.email,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (!isOwnProfile) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,   // white background
                        contentColor = Color.Black
                    ),
                    border = BorderStroke(1.dp, Color(0x33000000)),
                    shape = RoundedCornerShape(12.dp),
                    onClick = onFriendClick
                ) {
                    Text(
                        text = when (friendStatus) {
                            FriendStatus.FRIEND -> "Unfriend"
                            FriendStatus.REQUEST_SENT -> "Cancel Request"
                            FriendStatus.REQUEST_RECEIVED -> "Accept Request"
                            FriendStatus.NONE -> "Add Friend"
                        },
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                if (friendStatus == FriendStatus.REQUEST_RECEIVED) {
                    Spacer(modifier = Modifier.width(16.dp))

                    Button(
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,   // white background
                            contentColor = Color.Black
                        ),
                        border = BorderStroke(1.dp, Color(0x33000000)),
                        shape = RoundedCornerShape(12.dp),
                        onClick = onFriendDecline
                    ) {
                        Text(
                            text = "Decline Request",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                // Message button
                Button(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    onClick = onMessageClick
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Message,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(
                        text = "Message",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(
                count = postCount,
                label = "Posts"
            )

            StatItem(
                count = friendCount,
                label = "Friends"
            )
        }
    }
}

@Composable
fun StatItem(
    count: Int,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun LoadingView(modifier: Modifier = Modifier) {
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
fun ErrorView(
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
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetryClick) {
                Text("Retry")
            }
        }
    }
}

