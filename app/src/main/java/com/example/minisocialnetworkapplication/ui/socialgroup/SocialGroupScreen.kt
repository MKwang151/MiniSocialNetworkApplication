package com.example.minisocialnetworkapplication.ui.socialgroup

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.minisocialnetworkapplication.core.domain.model.Group
import com.example.minisocialnetworkapplication.core.domain.model.Post
import com.example.minisocialnetworkapplication.ui.auth.AuthViewModel
import com.example.minisocialnetworkapplication.ui.components.BottomNavBar
import com.example.minisocialnetworkapplication.ui.components.PostCard

// Modern color palette
private val GradientPrimary = listOf(Color(0xFF667EEA), Color(0xFF764BA2))
private val ColorAccent = Color(0xFF667EEA)

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
    val filterEmojis = listOf("👥", "📝", "🔍", "⚙️")

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
                containerColor = ColorAccent,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.shadow(8.dp, RoundedCornerShape(16.dp))
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Create Group",
                    tint = Color.White
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
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
            Column(modifier = Modifier.fillMaxSize()) {
                // Header with title
                Text(
                    text = "Groups",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 4.dp)
                )
                Text(
                    text = "Connect with your communities",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 20.dp, bottom = 12.dp)
                )
                
                // Modern Filter chips
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(filters.size) { index ->
                        FilterChip(
                            selected = selectedFilter == index,
                            onClick = { selectedFilter = index },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(filterEmojis[index])
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        filters[index],
                                        fontWeight = if (selectedFilter == index) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            },
                            shape = RoundedCornerShape(999.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ColorAccent,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))

                when (val state = uiState) {
                    is SocialGroupUiState.Loading -> {
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
                    is SocialGroupUiState.Error -> {
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
                    is SocialGroupUiState.Success -> {
                        when (selectedFilter) {
                            0 -> ModernGroupList(
                                groups = state.myGroups,
                                onGroupClick = onNavigateToGroupDetail,
                                emptyMessage = "You haven't joined any groups yet"
                            )
                            1 -> ModernPostsList(
                                posts = state.allPosts, 
                                onGroupClick = onNavigateToGroupDetail,
                                onPostClick = onNavigateToPostDetail,
                                onAuthorClick = onNavigateToProfile,
                                onImageClick = onNavigateToImageGallery,
                                onLikeClick = { post -> viewModel.toggleLike(post) }
                            )
                            2 -> ModernDiscoverGroupList(
                                groups = state.discoverGroups, 
                                onGroupClick = onNavigateToGroupDetail,
                                onJoinClick = { groupId -> viewModel.joinGroup(groupId) }
                            )
                            3 -> ModernGroupList(
                                groups = state.managedGroups,
                                onGroupClick = onNavigateToGroupDetail,
                                emptyMessage = "You don't manage any groups"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModernPostsList(
    posts: List<Post>,
    onGroupClick: (String) -> Unit,
    onPostClick: (String) -> Unit = {},
    onAuthorClick: (String) -> Unit = {},
    onImageClick: (String, Int) -> Unit = { _, _ -> },
    onLikeClick: (Post) -> Unit = {}
) {
    if (posts.isEmpty()) {
        ModernEmptyGroupState(
            emoji = "📝",
            title = "No Posts Yet",
            message = "Posts from your groups will appear here"
        )
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
fun ModernGroupList(
    groups: List<Group>,
    onGroupClick: (String) -> Unit,
    emptyMessage: String = "No groups found"
) {
    if (groups.isEmpty()) {
        ModernEmptyGroupState(
            emoji = "👥",
            title = "No Groups",
            message = emptyMessage
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(groups) { group ->
                ModernGroupListItem(
                    group = group,
                    onClick = { onGroupClick(group.id) }
                )
            }
        }
    }
}

@Composable
private fun ModernGroupListItem(
    group: Group,
    onClick: () -> Unit,
    trailing: @Composable (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with gradient fallback
            Surface(
                modifier = Modifier.size(52.dp),
                shape = CircleShape
            ) {
                if (group.avatarUrl != null) {
                    AsyncImage(
                        model = group.avatarUrl,
                        contentDescription = "Group Avatar",
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
                            text = group.name.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(14.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = ColorAccent.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "${group.memberCount} members",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = ColorAccent
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "• ${group.privacy}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            trailing?.invoke()
        }
    }
}

@Composable
fun ModernDiscoverGroupList(
    groups: List<Group>,
    onGroupClick: (String) -> Unit,
    onJoinClick: (String) -> Unit
) {
    if (groups.isEmpty()) {
        ModernEmptyGroupState(
            emoji = "🔍",
            title = "No New Groups",
            message = "No new groups to discover right now"
        )
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(groups) { group ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onGroupClick(group.id) },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar with gradient fallback
                        Surface(
                            modifier = Modifier.size(52.dp),
                            shape = CircleShape
                        ) {
                            if (group.avatarUrl != null) {
                                AsyncImage(
                                    model = group.avatarUrl,
                                    contentDescription = "Group Avatar",
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
                                        text = group.name.take(1).uppercase(),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(14.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = group.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${group.memberCount} members",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Button(
                            onClick = { onJoinClick(group.id) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ColorAccent
                            )
                        ) {
                            Text("Join", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernEmptyGroupState(
    emoji: String,
    title: String,
    message: String
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                ColorAccent.copy(alpha = 0.15f),
                                Color(0xFF764BA2).copy(alpha = 0.15f)
                            )
                        ),
                        shape = RoundedCornerShape(26.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = emoji,
                    style = MaterialTheme.typography.displaySmall
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Keep backward compatibility
@Composable
fun PostsList(
    posts: List<Post>,
    onGroupClick: (String) -> Unit,
    onPostClick: (String) -> Unit = {},
    onAuthorClick: (String) -> Unit = {},
    onImageClick: (String, Int) -> Unit = { _, _ -> },
    onLikeClick: (Post) -> Unit = {}
) = ModernPostsList(posts, onGroupClick, onPostClick, onAuthorClick, onImageClick, onLikeClick)

@Composable
fun GroupList(
    groups: List<Group>,
    onGroupClick: (String) -> Unit
) = ModernGroupList(groups, onGroupClick)

@Composable
fun DiscoverGroupList(
    groups: List<Group>,
    onGroupClick: (String) -> Unit,
    onJoinClick: (String) -> Unit
) = ModernDiscoverGroupList(groups, onGroupClick, onJoinClick)
