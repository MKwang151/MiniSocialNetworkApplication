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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
import androidx.compose.material3.TopAppBarDefaults




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
    LaunchedEffect(uiState) {
        if (uiState is FeedUiState.Error) {
            snackbarHostState.showSnackbar(
                message = (uiState as FeedUiState.Error).message,
                duration = SnackbarDuration.Short
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
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
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
                                        Badge {
                                            Text(
                                                text = if (unreadNotificationCount > 99) "99+"
                                                else unreadNotificationCount.toString()
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
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                    )
                )
            },
            bottomBar = bottomBar,
            floatingActionButton = {
                FloatingActionButton(
                    onClick = onNavigateToComposePost,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create post"
                    )
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
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
                contentPadding = PaddingValues(vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                    is LoadState.Error -> {
                        item {
                            ErrorItem(
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
            EmptyFeedView()
        }

        // Handle refresh error
        if (lazyPagingItems.loadState.refresh is LoadState.Error) {
            ErrorFullScreen(
                message = "Failed to load feed",
                onRetryClick = { lazyPagingItems.refresh() }
            )
        }
    }
}

@Composable
fun EmptyFeedView() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(86.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(24.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(34.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "No posts yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Be the first to share something!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}


@Composable
fun ErrorFullScreen(
    message: String,
    onRetryClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Oops!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onRetryClick,
                modifier = Modifier.height(46.dp)
            ) {
                Text("Retry")
            }
        }
    }
}


@Composable
fun ErrorItem(
    message: String,
    onRetryClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )

            TextButton(onClick = onRetryClick) {
                Text("Retry")
            }
        }
    }
}


