package com.example.minisocialnetworkapplication.ui.socialgroup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PostAdd
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.minisocialnetworkapplication.core.domain.model.Group

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupManagementScreen(
    groupId: String,
    groupName: String,
    group: Group? = null,
    userRole: com.example.minisocialnetworkapplication.core.domain.model.GroupRole? = null,
    onNavigateBack: () -> Unit,
    onNavigateToJoinRequests: () -> Unit,
    onNavigateToEditGroup: () -> Unit = {},
    onNavigateToMembers: () -> Unit = {},
    onNavigateToPendingPosts: () -> Unit = {},
    onNavigateToReports: () -> Unit = {},
    onTogglePostApproval: (Boolean) -> Unit = {},
    onDeleteGroup: () -> Unit = {},
    onLeaveGroup: () -> Unit = {}
) {
    var requirePostApproval by remember(group) { mutableStateOf(group?.requirePostApproval ?: false) }
    
    val isCreator = userRole == com.example.minisocialnetworkapplication.core.domain.model.GroupRole.CREATOR
    val isAdmin = userRole == com.example.minisocialnetworkapplication.core.domain.model.GroupRole.ADMIN
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Group") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = groupName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Membership Section
            item {
                Text(
                    text = "Membership",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            item {
                ManagementMenuItem(
                    icon = Icons.Default.Person,
                    title = "Pending Requests",
                    subtitle = "Review and approve join requests",
                    onClick = onNavigateToJoinRequests
                )
            }

            item {
                ManagementMenuItem(
                    icon = Icons.Default.Person,
                    title = "Members",
                    subtitle = "View and manage group members",
                    onClick = onNavigateToMembers
                )
            }

            // Post Moderation Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Post Moderation",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            item {
                PostApprovalToggle(
                    enabled = requirePostApproval,
                    onToggle = { enabled ->
                        requirePostApproval = enabled
                        onTogglePostApproval(enabled)
                    }
                )
            }
            
            item {
                ManagementMenuItem(
                    icon = Icons.Default.PostAdd,
                    title = "Pending Posts",
                    subtitle = "Review posts awaiting approval",
                    onClick = onNavigateToPendingPosts
                )
            }
            
            item {
                ManagementMenuItem(
                    icon = Icons.Default.Report,
                    title = "Reported Content",
                    subtitle = "Review reported posts in this group",
                    iconTint = MaterialTheme.colorScheme.error,
                    onClick = onNavigateToReports
                )
            }

            // Settings Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            item {
                ManagementMenuItem(
                    icon = Icons.Default.Edit,
                    title = "Edit Group",
                    subtitle = "Change name, description, privacy",
                    onClick = onNavigateToEditGroup
                )
            }

            if (isCreator || isAdmin) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Danger Zone",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                item {
                    if (isCreator) {
                        ManagementMenuItem(
                            icon = Icons.Default.Delete,
                            title = "Delete Group",
                            subtitle = "Permanently delete this group",
                            iconTint = MaterialTheme.colorScheme.error,
                            titleColor = MaterialTheme.colorScheme.error,
                            onClick = onDeleteGroup
                        )
                    } else {
                        ManagementMenuItem(
                            icon = Icons.Default.Delete,
                            title = "Leave Group",
                            subtitle = "You will no longer be an admin",
                            iconTint = MaterialTheme.colorScheme.error,
                            titleColor = MaterialTheme.colorScheme.error,
                            onClick = onLeaveGroup
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PostApprovalToggle(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Post Approval",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (enabled) "Members' posts require approval" else "Posts are published immediately",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Switch(
                checked = enabled,
                onCheckedChange = onToggle
            )
        }
    }
}

@Composable
fun ManagementMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    titleColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(28.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = titleColor
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
