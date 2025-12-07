package com.example.minisocialnetworkapplication.ui.chat

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.minisocialnetworkapplication.core.domain.model.ConversationType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationListScreen(
    viewModel: ConversationListViewModel = hiltViewModel(),
    onNavigateToChat: (String) -> Unit,
    onNavigateToNewChat: () -> Unit,
    bottomBar: @Composable () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentUserId = viewModel.currentUserId

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Messages") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToNewChat,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Edit, contentDescription = "New Chat")
            }
        },
        bottomBar = bottomBar
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                placeholder = { Text("Search conversations") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
                singleLine = true
            )

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState.error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.error ?: "Unknown error",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                uiState.conversations.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No conversations yet",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Start chatting with your friends!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = uiState.conversations,
                            key = { it.conversation.id }
                        ) { conversationWithUser ->
                            ConversationItem(
                                conversationWithUser = conversationWithUser,
                                currentUserId = currentUserId ?: "",
                                onClick = {
                                    viewModel.markAsRead(conversationWithUser.conversation.id)
                                    onNavigateToChat(conversationWithUser.conversation.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationItem(
    conversationWithUser: ConversationWithUser,
    currentUserId: String,
    onClick: () -> Unit
) {
    val conversation = conversationWithUser.conversation
    val otherUser = conversationWithUser.otherUser

    // Determine display name and avatar
    val displayName = when {
        conversation.type == ConversationType.GROUP -> conversation.name ?: "Group"
        otherUser != null -> otherUser.name
        else -> "Unknown"
    }

    val avatarUrl = when {
        conversation.type == ConversationType.GROUP -> conversation.avatarUrl
        otherUser != null -> otherUser.avatarUrl
        else -> null
    }

    val lastMessagePreview = conversation.lastMessage?.getPreviewText(currentUserId) ?: "No messages yet"
    val lastMessageTime = conversation.lastMessage?.timestamp?.toDate()?.let { formatTimestamp(it) } ?: ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar with online indicator
        Box(
            modifier = Modifier.size(56.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (avatarUrl != null) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = "Avatar",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            // Online indicator dot
            if (otherUser?.isOnline == true) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .align(Alignment.BottomEnd)
                        .background(MaterialTheme.colorScheme.surface, CircleShape)
                        .padding(2.dp)
                        .background(Color(0xFF4CAF50), CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Content
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (conversation.unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = lastMessageTime,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (conversation.unreadCount > 0) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = lastMessagePreview,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (conversation.unreadCount > 0) 
                        MaterialTheme.colorScheme.onSurface 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (conversation.unreadCount > 0) FontWeight.Medium else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                // Unread badge
                if (conversation.unreadCount > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (conversation.unreadCount > 99) "99+" else conversation.unreadCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

private fun formatTimestamp(date: Date): String {
    val now = Calendar.getInstance()
    val messageTime = Calendar.getInstance().apply { time = date }

    return when {
        // Today: show time
        now.get(Calendar.DATE) == messageTime.get(Calendar.DATE) &&
        now.get(Calendar.MONTH) == messageTime.get(Calendar.MONTH) &&
        now.get(Calendar.YEAR) == messageTime.get(Calendar.YEAR) -> {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
        }
        // Yesterday
        now.get(Calendar.DATE) - messageTime.get(Calendar.DATE) == 1 &&
        now.get(Calendar.MONTH) == messageTime.get(Calendar.MONTH) &&
        now.get(Calendar.YEAR) == messageTime.get(Calendar.YEAR) -> {
            "Yesterday"
        }
        // This week: show day name
        now.get(Calendar.WEEK_OF_YEAR) == messageTime.get(Calendar.WEEK_OF_YEAR) &&
        now.get(Calendar.YEAR) == messageTime.get(Calendar.YEAR) -> {
            SimpleDateFormat("EEE", Locale.getDefault()).format(date)
        }
        // Older: show date
        else -> {
            SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(date)
        }
    }
}
