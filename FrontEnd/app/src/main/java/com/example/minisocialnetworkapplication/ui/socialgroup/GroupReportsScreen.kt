package com.example.minisocialnetworkapplication.ui.socialgroup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import com.example.minisocialnetworkapplication.ui.components.ModernSnackbarHost
import com.example.minisocialnetworkapplication.ui.components.ToastType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.minisocialnetworkapplication.core.domain.model.Post
import com.example.minisocialnetworkapplication.core.domain.model.Report
import java.text.SimpleDateFormat
import java.util.*

// Modern color palette
private val GradientPrimary = listOf(Color(0xFF667EEA), Color(0xFF764BA2))
private val ColorAccent = Color(0xFF667EEA)
private val ColorError = Color(0xFFE53935)
private val ColorWarning = Color(0xFFFFA000)
private val ColorSuccess = Color(0xFF11998E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupReportsScreen(
    onNavigateBack: () -> Unit,
    viewModel: GroupReportsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(state.actionMessage) {
        state.actionMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearActionMessage()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Reported Content",
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
        },
        snackbarHost = { ModernSnackbarHost(snackbarHostState, type = ToastType.SUCCESS) }
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
                // Modern Tab Row
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    TabRow(
                        selectedTabIndex = state.selectedTab,
                        containerColor = Color.Transparent,
                        indicator = {},
                        divider = {}
                    ) {
                        ModernTab(
                            selected = state.selectedTab == 0,
                            onClick = { viewModel.selectTab(0) },
                            icon = Icons.Default.Report,
                            text = "Reports (${state.reports.size})",
                            color = ColorError
                        )
                        ModernTab(
                            selected = state.selectedTab == 1,
                            onClick = { viewModel.selectTab(1) },
                            icon = Icons.Default.VisibilityOff,
                            text = "Hidden (${state.hiddenPosts.size})",
                            color = ColorWarning
                        )
                    }
                }
                
                if (state.isLoading) {
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
                } else {
                    when (state.selectedTab) {
                        0 -> ModernReportsTab(
                            reports = state.reports,
                            onDismiss = viewModel::dismissReport,
                            onHidePost = viewModel::hidePost,
                            onDeletePost = viewModel::deletePost,
                            onKickMember = viewModel::kickMember
                        )
                        1 -> ModernHiddenPostsTab(
                            posts = state.hiddenPosts,
                            onRestore = viewModel::restorePost
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernTab(
    selected: Boolean,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    color: Color
) {
    Tab(
        selected = selected,
        onClick = onClick,
        modifier = Modifier.padding(4.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            shape = RoundedCornerShape(12.dp),
            color = if (selected) color.copy(alpha = 0.15f) else Color.Transparent
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    color = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ModernReportsTab(
    reports: List<Report>,
    onDismiss: (String) -> Unit,
    onHidePost: (String, String) -> Unit,
    onDeletePost: (String, String) -> Unit,
    onKickMember: (String, String, String) -> Unit
) {
    if (reports.isEmpty()) {
        ModernEmptyReportsState()
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(reports, key = { it.id }) { report ->
                ModernReportCard(
                    report = report,
                    onDismiss = { onDismiss(report.id) },
                    onHidePost = { onHidePost(report.targetId, report.id) },
                    onDeletePost = { onDeletePost(report.targetId, report.id) },
                    onKickMember = { onKickMember(report.authorId, report.targetId, report.id) }
                )
            }
        }
    }
}

@Composable
private fun ModernEmptyReportsState() {
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
                                ColorSuccess.copy(alpha = 0.15f),
                                ColorAccent.copy(alpha = 0.15f)
                            )
                        ),
                        shape = RoundedCornerShape(26.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = ColorSuccess
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                "All Clear! 🎉",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "No pending reports",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ModernReportCard(
    report: Report,
    onDismiss: () -> Unit,
    onHidePost: () -> Unit,
    onDeletePost: () -> Unit,
    onKickMember: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(32.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = ColorError.copy(alpha = 0.12f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Report,
                                contentDescription = null,
                                tint = ColorError,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "by ${report.reporterName}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        formatDate(report.createdAt),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(14.dp))
            
            // Reason Badge
            Surface(
                color = ColorError.copy(alpha = 0.1f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = report.reason,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = ColorError
                )
            }
            
            if (report.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    report.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(14.dp))
            
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ModernActionButton(
                    icon = Icons.Default.CheckCircle,
                    label = "Dismiss",
                    color = ColorSuccess,
                    onClick = onDismiss
                )
                ModernActionButton(
                    icon = Icons.Default.VisibilityOff,
                    label = "Hide",
                    color = ColorWarning,
                    onClick = onHidePost
                )
                ModernActionButton(
                    icon = Icons.Default.Delete,
                    label = "Delete",
                    color = ColorError,
                    onClick = onDeletePost
                )
                ModernActionButton(
                    icon = Icons.Default.PersonRemove,
                    label = "Kick",
                    color = Color(0xFF9C27B0),
                    onClick = onKickMember
                )
            }
        }
    }
}

@Composable
private fun ModernActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            modifier = Modifier.size(44.dp),
            shape = RoundedCornerShape(14.dp),
            color = color.copy(alpha = 0.12f),
            onClick = onClick
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = label,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ModernHiddenPostsTab(
    posts: List<Post>,
    onRestore: (String) -> Unit
) {
    if (posts.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "👀",
                    style = MaterialTheme.typography.displayMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "No hidden posts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(posts, key = { it.id }) { post ->
                ModernHiddenPostCard(
                    post = post,
                    onRestore = { onRestore(post.id) }
                )
            }
        }
    }
}

@Composable
private fun ModernHiddenPostCard(
    post: Post,
    onRestore: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        post.authorName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        post.text,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (post.rejectionReason != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            color = ColorError.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Hidden: ${post.rejectionReason}",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = ColorError
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Button(
                    onClick = onRestore,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ColorSuccess
                    )
                ) {
                    Icon(
                        Icons.Default.Restore,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Restore", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

private fun formatDate(timestamp: com.google.firebase.Timestamp): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return sdf.format(timestamp.toDate())
}

// Keep backward compatibility
@Composable
private fun ReportsTab(
    reports: List<Report>,
    onDismiss: (String) -> Unit,
    onHidePost: (String, String) -> Unit,
    onDeletePost: (String, String) -> Unit,
    onKickMember: (String, String, String) -> Unit
) = ModernReportsTab(reports, onDismiss, onHidePost, onDeletePost, onKickMember)

@Composable
private fun HiddenPostsTab(
    posts: List<Post>,
    onRestore: (String) -> Unit
) = ModernHiddenPostsTab(posts, onRestore)
