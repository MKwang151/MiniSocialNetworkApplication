package com.example.minisocialnetworkapplication.ui.feed

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.minisocialnetworkapplication.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.minisocialnetworkapplication.core.domain.model.Post
import com.example.minisocialnetworkapplication.ui.components.PostCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    onNavigateToComposePost: () -> Unit,
    onNavigateToPostDetail: (String) -> Unit,
    onNavigateToProfile: (String) -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onLogout: () -> Unit,
    shouldRefresh: StateFlow<Boolean>? = null,
    viewModel: FeedViewModel = hiltViewModel()
) {
    val lazyPagingItems = viewModel.feedPagingFlow.collectAsLazyPagingItems()
    val uiState by viewModel.uiState.collectAsState()

    // State to trigger scroll to top
    var shouldScrollToTop by remember { mutableStateOf(false) }

    // Auto refresh when screen appears
    LaunchedEffect(Unit) {
        lazyPagingItems.refresh()
    }

    // Refresh when returning from ComposePost
    val postCreated by (shouldRefresh ?: MutableStateFlow(false)).collectAsState()
    LaunchedEffect(postCreated) {
        if (postCreated) {
            shouldScrollToTop = true
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.feed)) },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Logout"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToComposePost,
                containerColor = MaterialTheme.colorScheme.primary
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
            shouldScrollToTop = shouldScrollToTop,
            onScrolledToTop = { shouldScrollToTop = false },
            modifier = Modifier.padding(paddingValues)
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
    shouldScrollToTop: Boolean,
    onScrolledToTop: () -> Unit,
    modifier: Modifier = Modifier
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
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
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
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "No posts yet",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
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
        modifier = Modifier.fillMaxSize(),
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
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onRetryClick) {
                Text("Retry")
            }
        }
    }
}

