package com.example.minisocialnetworkapplication.ui.feed

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import com.example.minisocialnetworkapplication.ui.components.ModernSnackbarHost
import com.example.minisocialnetworkapplication.ui.components.ToastType
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.minisocialnetworkapplication.R
import com.example.minisocialnetworkapplication.core.domain.model.Post
import com.example.minisocialnetworkapplication.core.domain.model.User
import com.example.minisocialnetworkapplication.ui.components.PostCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.graphics.Color



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    modifier: Modifier = Modifier,
    onNavigateToComposePost: () -> Unit,
    onNavigateToPostDetail: (String) -> Unit,
    onNavigateToProfile: (String) -> Unit,
    onNavigateToGroups: () -> Unit,
    onNavigateToImageGallery: (String, Int) -> Unit,
    onNavigateToReportPost: (postId: String, authorId: String, groupId: String?) -> Unit = { _, _, _ -> },
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToAdminDashboard: () -> Unit = {},
    onLogout: () -> Unit,
    shouldRefresh: StateFlow<Boolean>? = null,
    postDeleted: StateFlow<Boolean>? = null,
    profileUpdated: StateFlow<Boolean>? = null,
    currentUser: User?,
    unreadNotificationCount: Int = 0,
    bottomBar: @Composable () -> Unit,
    viewModel: FeedViewModel = hiltViewModel()
) {
    val lazyPagingItems = viewModel.feedPosts.collectAsLazyPagingItems()
    val uiState by viewModel.uiState.collectAsState()

    // State to trigger scroll to top
    var shouldScrollToTop by remember { mutableStateOf(false) }

    // State for delete dialog
    var showDeleteDialog by remember { mutableStateOf(false) }
    var postToDelete by remember { mutableStateOf<Post?>(null) }

    // State for edit dialog
    var showEditDialog by remember { mutableStateOf(false) }
    var postToEdit by remember { mutableStateOf<Post?>(null) }

    // Auto refresh when screen appears/resumes (handles navigation back)
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.syncCache()
                lazyPagingItems.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Redirect admins to dashboard
    LaunchedEffect(currentUser) {
        if (currentUser?.role == User.ROLE_ADMIN) {
            onNavigateToAdminDashboard()
        }
    }

    // Refresh when returning from ComposePost
    val postCreated by (shouldRefresh ?: MutableStateFlow(false)).collectAsState()
    LaunchedEffect(postCreated) {
        if (postCreated) {
            shouldScrollToTop = true
            lazyPagingItems.refresh()
        }
    }

    // Refresh when post is deleted from PostDetailScreen
    val isPostDeleted by (postDeleted ?: MutableStateFlow(false)).collectAsState()
    LaunchedEffect(isPostDeleted) {
        if (isPostDeleted) {
            shouldScrollToTop = true
            lazyPagingItems.refresh()
        }
    }

    // Refresh when profile is updated (to show new author names)
    val isProfileUpdated by (profileUpdated ?: MutableStateFlow(false)).collectAsState()
    LaunchedEffect(isProfileUpdated) {
        if (isProfileUpdated) {
            timber.log.Timber.d("Profile updated - refreshing Feed to show new author names")
            // Refresh to load new data from Firestore (cache already cleared by EditProfileViewModel)
            lazyPagingItems.refresh()
        }
    }

    // Show error snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    var currentToastType by remember { mutableStateOf(ToastType.INFO) }
    
    LaunchedEffect(uiState) {
        if (uiState is FeedUiState.Error) {
            currentToastType = ToastType.ERROR
            snackbarHostState.showSnackbar(
                message = (uiState as FeedUiState.Error).message
            )
            viewModel.clearError()
        }
    }

    val drawerState = androidx.compose.material3.rememberDrawerState(initialValue = androidx.compose.material3.DrawerValue.Closed)
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    androidx.compose.material3.ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            com.example.minisocialnetworkapplication.ui.components.DrawerContent(
                user = currentUser,
                onNavigateToProfile = { 
                    currentUser?.let { onNavigateToProfile(it.id) }
                },
                onNavigateToGroups = onNavigateToGroups,
                onLogout = onLogout,
                onCloseDrawer = { 
                    scope.launch { drawerState.close() } 
                }
            )
        }
    ) {
        Scaffold(
            modifier = modifier,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Feed",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu"
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onNavigateToNotifications) {
                            BadgedBox(
                                badge = {
                                    if (unreadNotificationCount > 0) {
                                        Badge(
                                            containerColor = Color(0xFFFF416C)
                                        ) {
                                            Text(
                                                text = if (unreadNotificationCount > 99) "99+"
                                                else unreadNotificationCount.toString(),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Notifications"
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
                    )
                )
            },
            bottomBar = bottomBar,
            floatingActionButton = {
                FloatingActionButton(
                    onClick = onNavigateToComposePost,
                    modifier = Modifier.shadow(8.dp, RoundedCornerShape(16.dp)),
                    containerColor = Color(0xFF667EEA),
                    contentColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create post",
                        modifier = Modifier.size(26.dp)
                    )
                }
            },
            snackbarHost = { ModernSnackbarHost(snackbarHostState, type = currentToastType) }
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
                FeedContent(
                    lazyPagingItems = lazyPagingItems,
                    viewModel = viewModel,
                    onNavigateToPostDetail = onNavigateToPostDetail,
                    onNavigateToProfile = onNavigateToProfile,
                    onNavigateToImageGallery = onNavigateToImageGallery,
                    shouldScrollToTop = shouldScrollToTop,
                    onScrolledToTop = { shouldScrollToTop = false },
                    onDeletePost = { postToDelete = it; showDeleteDialog = true },
                    onEditPost = { postToEdit = it; showEditDialog = true },
                    onReportPost = onNavigateToReportPost,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog && postToDelete != null) {
        com.example.minisocialnetworkapplication.ui.components.DeletePostDialog(
            onConfirm = {
                postToDelete?.let { post: Post ->
                    viewModel.deletePost(post.id)
                }
                showDeleteDialog = false
                postToDelete = null
                lazyPagingItems.refresh()
            },
            onDismiss = {
                showDeleteDialog = false
                postToDelete = null
            }
        )
    }

    // Edit post dialog
    if (showEditDialog && postToEdit != null) {
        com.example.minisocialnetworkapplication.ui.components.EditTextDialog(
            title = "Edit Post",
            initialText = postToEdit?.text ?: "",
            onConfirm = { newText ->
                postToEdit?.let { post ->
                    viewModel.updatePost(post.id, newText)
                }
                showEditDialog = false
                postToEdit = null
                lazyPagingItems.refresh()
            },
            onDismiss = {
                showEditDialog = false
                postToEdit = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedContent(
    lazyPagingItems: LazyPagingItems<Post>,
    viewModel: FeedViewModel,
    onNavigateToPostDetail: (String) -> Unit,
    onNavigateToProfile: (String) -> Unit,
    onNavigateToImageGallery: (String, Int) -> Unit,
    shouldScrollToTop: Boolean,
    onScrolledToTop: () -> Unit,
    modifier: Modifier = Modifier,
    onDeletePost: (Post) -> Unit = {},
    onEditPost: (Post) -> Unit = {},
    onReportPost: (postId: String, authorId: String, groupId: String?) -> Unit = { _, _, _ -> }
) {
    val isRefreshing = lazyPagingItems.loadState.refresh is LoadState.Loading
    val listState = rememberLazyListState()

    // Scroll to top when shouldScrollToTop is true and refresh completes
    LaunchedEffect(shouldScrollToTop, lazyPagingItems.loadState.refresh) {
        if (shouldScrollToTop &&
            lazyPagingItems.loadState.refresh is LoadState.NotLoading &&
            lazyPagingItems.itemCount > 0) {
            listState.animateScrollToItem(0)
            onScrolledToTop()
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { lazyPagingItems.refresh() },
        modifier = modifier
    ) {
        // Show shimmer loading on initial load
        if (lazyPagingItems.loadState.refresh is LoadState.Loading && lazyPagingItems.itemCount == 0) {
            com.example.minisocialnetworkapplication.ui.components.FeedLoadingShimmer(
                modifier = Modifier.fillMaxSize()
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp) // Instagram-like minimal spacing
            ) {
                items(
                    count = lazyPagingItems.itemCount,
                    key = { index -> lazyPagingItems.peek(index)?.id ?: index }
                ) { index ->
                    val post = lazyPagingItems[index]
                    if (post != null) {
                        PostCard(
                            post = post,
                            onLikeClicked = { viewModel.toggleLike(post) },
                            onCommentClicked = { onNavigateToPostDetail(post.id) },
                            onPostClicked = { onNavigateToPostDetail(post.id) },
                            onAuthorClicked = { onNavigateToProfile(post.authorId) },
                            onImageClicked = { imageIndex ->
                                onNavigateToImageGallery(post.id, imageIndex)
                            },
                            onEditClicked = { onEditPost(it) },
                            onDeleteClicked = { onDeletePost(it) },
                            onReportClicked = { post ->
                                onReportPost(post.id, post.authorId, post.groupId)
                            },
                            isOptimisticallyLiked = post.likedByMe
                        )
                    }
                }

                // Loading indicator at the end
                when (lazyPagingItems.loadState.append) {
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
                                    color = Color(0xFF667EEA)
                                )
                            }
                        }
                    }
                    is LoadState.Error -> {
                        item {
                            ModernErrorItem(
                                message = "Failed to load more posts",
                                onRetryClick = { lazyPagingItems.retry() }
                            )
                        }
                    }
                    else -> {}
                }
            }
        }

        // Handle empty state
        if (lazyPagingItems.loadState.refresh is LoadState.NotLoading &&
            lazyPagingItems.itemCount == 0
        ) {
            ModernEmptyFeedView()
        }

        // Handle refresh error
        if (lazyPagingItems.loadState.refresh is LoadState.Error) {
            ModernErrorFullScreen(
                message = "Failed to load feed",
                onRetryClick = { lazyPagingItems.refresh() }
            )
        }
    }
}

@Composable
fun ModernEmptyFeedView() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Modern empty state with gradient icon background
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF667EEA).copy(alpha = 0.15f),
                                Color(0xFF764BA2).copy(alpha = 0.15f)
                            )
                        ),
                        shape = RoundedCornerShape(28.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(44.dp),
                    tint = Color(0xFF667EEA)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "No posts yet",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Be the first to share something!\nTap the + button to create a post",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
        }
    }
}


@Composable
fun ModernErrorFullScreen(
    message: String,
    onRetryClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Error emoji icon with styled background
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .background(
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(26.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "😕",
                    style = MaterialTheme.typography.displayMedium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

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
                modifier = Modifier.height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF667EEA)
                )
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Retry",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}


@Composable
fun ModernErrorItem(
    message: String,
    onRetryClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f),
                    RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text(
                text = "⚠️",
                style = MaterialTheme.typography.titleMedium
            )
            
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )

            TextButton(
                onClick = onRetryClick,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color(0xFF667EEA)
                )
            ) {
                Text(
                    "Retry",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// Keep backward compatibility
@Composable
fun EmptyFeedView() = ModernEmptyFeedView()

@Composable
fun ErrorFullScreen(
    message: String,
    onRetryClick: () -> Unit
) = ModernErrorFullScreen(message, onRetryClick)

@Composable
fun ErrorItem(
    message: String,
    onRetryClick: () -> Unit
) = ModernErrorItem(message, onRetryClick)
