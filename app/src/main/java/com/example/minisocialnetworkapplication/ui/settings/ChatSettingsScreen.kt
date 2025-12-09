package com.example.minisocialnetworkapplication.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.minisocialnetworkapplication.core.domain.model.ConversationType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatSettingsScreen(
    conversationId: String,
    viewModel: ChatSettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToProfile: (String) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToMedia: () -> Unit,
    onChatDeleted: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isDeleting by viewModel.isDeleting.collectAsStateWithLifecycle()
    
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(conversationId) {
        viewModel.loadConversation(conversationId)
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text(text = "Delete Conversation?") },
            text = { Text("This will hide the conversation from your list. Other participants will still see it.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        viewModel.deleteConversation(conversationId) {
                            onChatDeleted()
                        }
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val state = uiState) {
                is ChatSettingsUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is ChatSettingsUiState.Error -> {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is ChatSettingsUiState.Success -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Header
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        val avatarUrl = if (state.conversation.type == ConversationType.DIRECT) {
                            state.otherUser?.avatarUrl
                        } else {
                            state.conversation.avatarUrl
                        }
                        
                        val name = if (state.conversation.type == ConversationType.DIRECT) {
                            state.otherUser?.name ?: "Chat"
                        } else {
                            state.conversation.name ?: "Group"
                        }
                        
                        // Avatar
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            if (avatarUrl != null) {
                                AsyncImage(
                                    model = avatarUrl,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(50.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        
                        // Action Buttons
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (state.conversation.type == ConversationType.DIRECT && state.otherUser != null) {
                                ActionButton(
                                    icon = Icons.Default.Person,
                                    text = "Profile",
                                    onClick = { onNavigateToProfile(state.otherUser.uid) }
                                )
                            } else {
                                // Add member placeholder for group
                                ActionButton(
                                    icon = Icons.Default.Person, // Should include + but using Person for now
                                    text = "Add Member",
                                    onClick = { /* TODO */ }
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(32.dp))
                            
                            ActionButton(
                                icon = if (state.conversation.isMuted) Icons.Default.NotificationsOff else Icons.Default.Notifications,
                                text = if (state.conversation.isMuted) "Unmute" else "Mute",
                                onClick = { viewModel.toggleMute(conversationId, state.conversation.isMuted) }
                            )
                            

                        }
                        
                        HorizontalDivider()
                        
                        // Menu Items
                        Column(modifier = Modifier.fillMaxWidth()) {
                            MenuItem(
                                icon = Icons.Default.Search,
                                text = "Search in Conversation",
                                onClick = onNavigateToSearch
                            )
                            MenuItem(
                                icon = Icons.Default.Image,
                                text = "View Media",
                                onClick = onNavigateToMedia
                            )
                            
                            val deleteText = if (state.conversation.type == ConversationType.DIRECT) 
                                "Delete chat" else "Leave chat"
                                
                            MenuItem(
                                icon = if (state.conversation.type == ConversationType.DIRECT) 
                                    Icons.Default.Delete else Icons.Default.Block,
                                text = deleteText,
                                textColor = MaterialTheme.colorScheme.error,
                                onClick = { showDeleteConfirmDialog = true }
                            )
                        }
                    }
                }
            }
            
            if (isDeleting) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = text, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun MenuItem(
    icon: ImageVector,
    text: String,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = textColor
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = textColor
        )
    }
}
