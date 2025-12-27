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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.minisocialnetworkapplication.core.domain.model.ConversationType

// Modern color palette
private val GradientPrimary = listOf(Color(0xFF667EEA), Color(0xFF764BA2))
private val ColorAccent = Color(0xFF667EEA)
private val ColorError = Color(0xFFE53935)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatSettingsScreen(
    conversationId: String,
    viewModel: ChatSettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToProfile: (String) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToMedia: () -> Unit,
    onNavigateToMembers: () -> Unit,
    onNavigateToAddMember: () -> Unit,
    onNavigateToJoinRequests: () -> Unit,
    onNavigateToEditGroup: (String) -> Unit,
    onChatDeleted: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isDeleting by viewModel.isDeleting.collectAsStateWithLifecycle()
    
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showLeaveConfirmDialog by remember { mutableStateOf(false) }
    var showPermanentDeleteConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(conversationId) {
        viewModel.loadConversation(conversationId)
    }

    if (showDeleteConfirmDialog) {
        ModernAlertDialog(
            title = "Delete Conversation?",
            message = "This will hide the conversation from your list. Other participants will still see it.",
            confirmText = "Delete",
            onConfirm = {
                showDeleteConfirmDialog = false
                viewModel.deleteConversation(conversationId) {
                    onChatDeleted()
                }
            },
            onDismiss = { showDeleteConfirmDialog = false }
        )
    }
    
    if (showLeaveConfirmDialog) {
        ModernAlertDialog(
            title = "Leave Group?",
            message = "You will no longer receive messages from this group.",
            confirmText = "Leave",
            onConfirm = {
                showLeaveConfirmDialog = false
                viewModel.leaveGroup(conversationId) {
                    onChatDeleted()
                }
            },
            onDismiss = { showLeaveConfirmDialog = false }
        )
    }

    if (showPermanentDeleteConfirmDialog) {
        ModernAlertDialog(
            title = "Delete Group Permanently?",
            message = "This will DELETE the conversation for ALL participants. This action cannot be undone.",
            confirmText = "DELETE",
            onConfirm = {
                showPermanentDeleteConfirmDialog = false
                viewModel.deleteGroupPermanent(conversationId) {
                    onChatDeleted()
                }
            },
            onDismiss = { showPermanentDeleteConfirmDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Details",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
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
                is ChatSettingsUiState.Loading -> {
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
                is ChatSettingsUiState.Error -> {
                    ModernErrorState(message = state.message)
                }
                is ChatSettingsUiState.Success -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
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
                        
                        // Avatar with gradient fallback
                        Surface(
                            modifier = Modifier.size(100.dp),
                            shape = CircleShape,
                            color = Color.Transparent,
                            shadowElevation = 4.dp
                        ) {
                            if (avatarUrl != null) {
                                AsyncImage(
                                    model = avatarUrl,
                                    contentDescription = null,
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
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(50.dp),
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = name,
                            style = MaterialTheme.typography.headlineSmall,
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
                                ModernActionButton(
                                    icon = Icons.Default.Person,
                                    text = "Profile",
                                    onClick = { onNavigateToProfile(state.otherUser.uid) }
                                )
                            } else {
                                ModernActionButton(
                                    icon = Icons.Default.PersonAdd,
                                    text = "Add",
                                    onClick = onNavigateToAddMember
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(32.dp))
                            
                            ModernActionButton(
                                icon = if (state.conversation.isMuted) Icons.Default.NotificationsOff else Icons.Default.Notifications,
                                text = if (state.conversation.isMuted) "Unmute" else "Mute",
                                onClick = { viewModel.toggleMute(conversationId, state.conversation.isMuted) }
                            )
                        }
                        
                        // Menu Items Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .shadow(4.dp, RoundedCornerShape(20.dp)),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                if (state.conversation.type == ConversationType.GROUP) {
                                    ModernMenuItem(
                                        icon = Icons.Default.Group,
                                        text = "See chat members",
                                        onClick = onNavigateToMembers
                                    )
                                    
                                    if (state.isAdmin) {
                                        ModernMenuItem(
                                            icon = Icons.Default.Notifications, 
                                            text = "Join Requests",
                                            onClick = onNavigateToJoinRequests
                                        )
                                    }

                                    if (state.isAdmin || state.isCreator) {
                                        ModernMenuItem(
                                            icon = Icons.Default.Group,
                                            text = "Edit Group",
                                            onClick = { onNavigateToEditGroup(conversationId) }
                                        )
                                    }
                                }
                                ModernMenuItem(
                                    icon = Icons.Default.Search,
                                    text = "Search in Conversation",
                                    onClick = onNavigateToSearch
                                )
                                ModernMenuItem(
                                    icon = Icons.Default.Image,
                                    text = "View Media",
                                    onClick = onNavigateToMedia
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Danger Zone Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .shadow(4.dp, RoundedCornerShape(20.dp)),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                if (state.conversation.type == ConversationType.DIRECT) {
                                    ModernMenuItem(
                                        icon = Icons.Default.Delete,
                                        text = "Delete chat",
                                        textColor = ColorError,
                                        onClick = { showDeleteConfirmDialog = true }
                                    )
                                } else {
                                    ModernMenuItem(
                                        icon = Icons.Default.Block,
                                        text = "Leave chat",
                                        textColor = ColorError,
                                        onClick = { showLeaveConfirmDialog = true }
                                    )
                                    
                                    if (state.isCreator) {
                                        ModernMenuItem(
                                            icon = Icons.Default.Delete,
                                            text = "Delete Group (Permanent)",
                                            textColor = ColorError,
                                            onClick = { showPermanentDeleteConfirmDialog = true }
                                        )
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
            
            if (isDeleting) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = ColorAccent,
                        strokeWidth = 3.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun ModernActionButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Surface(
            modifier = Modifier.size(52.dp),
            shape = CircleShape,
            color = ColorAccent.copy(alpha = 0.12f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = ColorAccent,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ModernMenuItem(
    icon: ImageVector,
    text: String,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = RoundedCornerShape(12.dp),
            color = if (textColor == ColorError) 
                ColorError.copy(alpha = 0.1f) 
            else 
                ColorAccent.copy(alpha = 0.1f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = textColor,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ModernAlertDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                title,
                fontWeight = FontWeight.Bold
            )
        },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText, color = ColorError, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun ModernErrorState(message: String) {
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
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
