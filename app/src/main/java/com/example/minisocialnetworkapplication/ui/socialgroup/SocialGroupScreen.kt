package com.example.minisocialnetworkapplication.ui.socialgroup

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.minisocialnetworkapplication.core.domain.model.Group
import com.example.minisocialnetworkapplication.core.domain.model.Post
import com.example.minisocialnetworkapplication.ui.auth.AuthViewModel
import com.example.minisocialnetworkapplication.ui.components.BottomNavBar
import com.example.minisocialnetworkapplication.ui.components.PostCard

@Composable
fun SocialGroupScreen(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    onNavigateToCreateGroup: () -> Unit,
    onNavigateToGroupDetail: (String) -> Unit,
    onNavigateToPostDetail: (String) -> Unit = {},
    onNavigateToProfile: (String) -> Unit = {},
    onNavigateToImageGallery: (String, Int) -> Unit = { _, _ -> },
    viewModel: SocialGroupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedFilter by remember { mutableIntStateOf(0) }
    val filters = listOf("Your Groups", "Posts", "Discover", "Manage")

    Scaffold(
        bottomBar = {
            BottomNavBar(
                navController = navController,
                authViewModel = authViewModel
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreateGroup,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Group")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            // Header with title
            Text(
                text = "Group",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
            )
            
            // Filter chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(filters.size) { index ->
                    FilterChip(
                        selected = selectedFilter == index,
                        onClick = { selectedFilter = index },
                        label = { Text(filters[index]) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))

            when (val state = uiState) {
                is SocialGroupUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is SocialGroupUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                    }
                }
                is SocialGroupUiState.Success -> {
                    when (selectedFilter) {
                        0 -> GroupList(groups = state.myGroups, onGroupClick = onNavigateToGroupDetail)
                        1 -> PostsList(
                            posts = state.allPosts, 
                            onGroupClick = onNavigateToGroupDetail,
                            onPostClick = onNavigateToPostDetail,
                            onAuthorClick = onNavigateToProfile,
                            onImageClick = onNavigateToImageGallery,
                            onLikeClick = { post -> viewModel.toggleLike(post) }
                        )
                        2 -> DiscoverGroupList(
                            groups = state.discoverGroups, 
                            onGroupClick = onNavigateToGroupDetail,
                            onJoinClick = { groupId -> viewModel.joinGroup(groupId) }
                        )
                        3 -> GroupList(groups = state.managedGroups, onGroupClick = onNavigateToGroupDetail)
                    }
                }
            }
        }
    }
}

@Composable
fun PostsList(
    posts: List<Post>,
    onGroupClick: (String) -> Unit,
    onPostClick: (String) -> Unit = {},
    onAuthorClick: (String) -> Unit = {},
    onImageClick: (String, Int) -> Unit = { _, _ -> },
    onLikeClick: (Post) -> Unit = {}
) {
    if (posts.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "No posts found")
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(vertical = 8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(posts, key = { it.id }) { post ->
                PostCard(
                    post = post,
                    onPostClicked = { onPostClick(post.id) },
                    onLikeClicked = { onLikeClick(post) },
                    onCommentClicked = { onPostClick(post.id) },
                    onAuthorClicked = { onAuthorClick(post.authorId) },
                    onImageClicked = { index -> onImageClick(post.id, index) },
                    onDeleteClicked = { /* Handle in detail screen */ },
                    onEditClicked = { /* Handle in detail screen */ },
                    isOptimisticallyLiked = post.likedByMe,
                    showMenuButton = false
                )
            }
        }
    }
}

@Composable
fun GroupList(
    groups: List<Group>,
    onGroupClick: (String) -> Unit
) {
    if (groups.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "No groups found")
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(groups) { group ->
                androidx.compose.material3.ListItem(
                    headlineContent = { Text(group.name) },
                    supportingContent = { Text("${group.memberCount} members") },
                    leadingContent = {
                        // Avatar placeholder
                        androidx.compose.material3.Surface(
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(group.name.take(1).uppercase())
                            }
                        }
                    },
                    modifier = Modifier.clickable { onGroupClick(group.id) }
                )
                androidx.compose.material3.HorizontalDivider()
            }
        }
    }
}

@Composable
fun DiscoverGroupList(
    groups: List<Group>,
    onGroupClick: (String) -> Unit,
    onJoinClick: (String) -> Unit
) {
    if (groups.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "No new groups to discover")
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(groups) { group ->
                androidx.compose.material3.ListItem(
                    headlineContent = { Text(group.name) },
                    supportingContent = { Text("${group.memberCount} members") },
                    leadingContent = {
                        androidx.compose.material3.Surface(
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(group.name.take(1).uppercase())
                            }
                        }
                    },
                    trailingContent = {
                        androidx.compose.material3.Button(
                            onClick = { onJoinClick(group.id) },
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("Join")
                        }
                    },
                    modifier = Modifier.clickable { onGroupClick(group.id) }
                )
                androidx.compose.material3.HorizontalDivider()
            }
        }
    }
}
