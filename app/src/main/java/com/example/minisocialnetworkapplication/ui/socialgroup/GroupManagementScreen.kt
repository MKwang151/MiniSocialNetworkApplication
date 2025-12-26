package com.example.minisocialnetworkapplication.ui.socialgroup

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.minisocialnetworkapplication.core.domain.model.Group

// Modern color palette
// Modern color palette
// Modern color palette
private val GradientPrimary = listOf(Color(0xFF667EEA), Color(0xFF764BA2))
private val ColorAccent = Color(0xFF667EEA)
private val ColorError = Color(0xFFE53935)
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
                title = {
                    Text(
                        "Manage Group",
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = groupName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Membership Section
                item {
                    ModernSectionHeader(title = "Membership", emoji = "👥")
                }

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(4.dp, RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column {
                            ModernManagementMenuItem(
                                icon = Icons.Default.PersonAdd,
                                title = "Pending Requests",
                                subtitle = "Review and approve join requests",
                                onClick = onNavigateToJoinRequests
                            )
                            ModernManagementMenuItem(
                                icon = Icons.Default.Person,
                                title = "Members",
                                subtitle = "View and manage group members",
                                onClick = onNavigateToMembers
                            )
                        }
                    }
                }

                // Post Moderation Section
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    ModernSectionHeader(title = "Post Moderation", emoji = "📝")
                }
                
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(4.dp, RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column {
                            ModernPostApprovalToggle(
                                enabled = requirePostApproval,
                                onToggle = { enabled ->
                                    requirePostApproval = enabled
                                    onTogglePostApproval(enabled)
                                }
                            )
                            ModernManagementMenuItem(
                                icon = Icons.Default.PostAdd,
                                title = "Pending Posts",
                                subtitle = "Review posts awaiting approval",
                                onClick = onNavigateToPendingPosts
                            )
                            ModernManagementMenuItem(
                                icon = Icons.Default.Report,
                                title = "Reported Content",
                                subtitle = "Review reported posts in this group",
                                iconTint = ColorError,
                                onClick = onNavigateToReports
                            )
                        }
                    }
                }

                // Settings Section
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    ModernSectionHeader(title = "Settings", emoji = "⚙️")
                }

                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(4.dp, RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        ModernManagementMenuItem(
                            icon = Icons.Default.Edit,
                            title = "Edit Group",
                            subtitle = "Change name, description, privacy",
                            onClick = onNavigateToEditGroup
                        )
                    }
                }

                if (isCreator || isAdmin) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        ModernSectionHeader(title = "Danger Zone", emoji = "⚠️", color = ColorError)
                    }

                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(4.dp, RoundedCornerShape(20.dp)),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = ColorError.copy(alpha = 0.08f)
                            )
                        ) {
                            if (isCreator) {
                                ModernManagementMenuItem(
                                    icon = Icons.Default.Delete,
                                    title = "Delete Group",
                                    subtitle = "Permanently delete this group",
                                    iconTint = ColorError,
                                    titleColor = ColorError,
                                    onClick = onDeleteGroup
                                )
                            } else {
                                ModernManagementMenuItem(
                                    icon = Icons.Default.Delete,
                                    title = "Leave Group",
                                    subtitle = "You will no longer be an admin",
                                    iconTint = ColorError,
                                    titleColor = ColorError,
                                    onClick = onLeaveGroup
                                )
                            }
                        }
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun ModernSectionHeader(
    title: String,
    emoji: String,
    color: Color = ColorAccent
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = emoji,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun ModernPostApprovalToggle(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(44.dp),
            shape = RoundedCornerShape(14.dp),
            color = ColorAccent.copy(alpha = 0.12f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = ColorAccent,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Post Approval",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = if (enabled) "Members' posts require approval" else "Posts are published immediately",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = ColorAccent
            )
        )
    }
}

@Composable
fun ModernManagementMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    iconTint: Color = ColorAccent,
    titleColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(44.dp),
            shape = RoundedCornerShape(14.dp),
            color = iconTint.copy(alpha = 0.12f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
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
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}
// est ssdsds
// Keep backward compatibility
@Composable
fun PostApprovalToggle(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) = ModernPostApprovalToggle(enabled = enabled, onToggle = onToggle)

@Composable
fun ManagementMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    iconTint: Color = ColorAccent,
    titleColor: Color = MaterialTheme.colorScheme.onSurface
) = ModernManagementMenuItem(
    icon = icon,
    title = title,
    subtitle = subtitle,
    onClick = onClick,
    iconTint = iconTint,
    titleColor = titleColor
)
