package com.example.minisocialnetworkapplication.ui.socialgroup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.minisocialnetworkapplication.core.domain.model.Post
import com.example.minisocialnetworkapplication.ui.components.PostItem
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToComposePost: (String) -> Unit, // groupId
    onNavigateToPostDetail: (String) -> Unit,
    viewModel: GroupDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Group Detail") }, // Can eventually show group name
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
                    contentPadding = PaddingValues(bottom = 80.dp) // Space for FAB
                ) {
                    // Header Item
                    item {
                        GroupHeader(
                            group = state.group,
                            isMember = state.isMember,
                            onJoinClick = viewModel::joinGroup,
                            onLeaveClick = viewModel::leaveGroup
                        )
                    }

                    // Separation
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Posts",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (state.posts.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("No posts yet.")
                            }
                        }
                    } else {
                        items(state.posts) { post ->
                            // Assuming PostItem handles generic post display
                            // Pass generic callbacks or stubs
                             PostItem(
                                post = post,
                                onPostClick = { onNavigateToPostDetail(post.id) },
                                onLikeClick = { /* TODO */ },
                                onCommentClick = { onNavigateToPostDetail(post.id) },
                                onShareClick = { /* TODO */ },
                                onProfileClick = { /* TODO */ },
                                onImageClick = { _, _ -> /* TODO */ }
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
    onJoinClick: () -> Unit,
    onLeaveClick: () -> Unit
) {
    Column {
        Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
            // Cover Image
            AsyncImage(
                model = group.coverUrl ?: "https://via.placeholder.com/800x400", // Placeholder
                contentDescription = "Cover Image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // Overlay gradient could be nice here
        }
        
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = group.name, style = MaterialTheme.typography.headlineMedium)
            Text(text = "${group.memberCount} members · ${group.privacy}", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = group.description, style = MaterialTheme.typography.bodyLarge)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (isMember) {
                 OutlinedButton(onClick = onLeaveClick, modifier = Modifier.fillMaxWidth()) {
                     Text("Joined")
                 }
            } else {
                Button(onClick = onJoinClick, modifier = Modifier.fillMaxWidth()) {
                    Text("Join Group")
                }
            }
        }
    }
}
