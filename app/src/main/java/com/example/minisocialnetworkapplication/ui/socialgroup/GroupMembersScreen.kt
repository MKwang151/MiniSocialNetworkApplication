package com.example.minisocialnetworkapplication.ui.socialgroup

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import coil.compose.AsyncImage
import com.example.minisocialnetworkapplication.core.domain.model.GroupRole

// Modern color palette
private val GradientPrimary = listOf(Color(0xFF667EEA), Color(0xFF764BA2))
private val ColorAccent = Color(0xFF667EEA)
private val ColorError = Color(0xFFE53935)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun GroupMembersScreen(
    onNavigateBack: () -> Unit,
    onNavigateToProfile: (String) -> Unit,
    viewModel: GroupMembersViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedMember by remember { mutableStateOf<MemberWithInfo?>(null) }
    val sheetState = rememberModalBottomSheetState()
    
    // Show action message
    LaunchedEffect(state.actionMessage) {
        state.actionMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearActionMessage()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Members",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
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
            Column(modifier = Modifier.fillMaxSize()) {
                // Modern Search Bar
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = { viewModel.onSearchQueryChange(it) },
                        modifier = Modifier.fillMaxSize(),
                        placeholder = { Text("Search members...") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                "Search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingIcon = {
                            if (state.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                    Icon(Icons.Default.Close, "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = ColorAccent
                        )
                    )
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
                } else if (state.filteredMembers.isEmpty()) {
                    ModernEmptyMembersState(
                        message = if (state.searchQuery.isNotEmpty()) "No members found" else "No members"
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(state.filteredMembers) { memberInfo ->
                            ModernMemberCard(
                                memberInfo = memberInfo,
                                onLongPress = {
                                    selectedMember = memberInfo
                                    showBottomSheet = true
                                },
                                onClick = {
                                    onNavigateToProfile(memberInfo.member.userId)
                                }
                            )
                        }
                    }
                }
            }
        }
        
        // Modal Bottom Sheet
        if (showBottomSheet && selectedMember != null) {
            ModalBottomSheet(
                onDismissRequest = { 
                    showBottomSheet = false
                    selectedMember = null
                },
                sheetState = sheetState,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                ModernMemberActionsSheet(
                    memberInfo = selectedMember!!,
                    currentUserRole = state.currentUserRole,
                    onViewProfile = {
                        showBottomSheet = false
                        onNavigateToProfile(selectedMember!!.member.userId)
                    },
                    onMakeAdmin = {
                        showBottomSheet = false
                        viewModel.makeAdmin(selectedMember!!.member.userId)
                    },
                    onDismissAdmin = {
                        showBottomSheet = false
                        viewModel.dismissAdmin(selectedMember!!.member.userId)
                    },
                    onRemoveMember = {
                        showBottomSheet = false
                        viewModel.removeMember(selectedMember!!.member.userId)
                    }
                )
            }
        }
    }
}

@Composable
private fun ModernEmptyMembersState(message: String) {
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
                Icon(
                    Icons.Default.Groups,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = ColorAccent
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ModernMemberCard(
    memberInfo: MemberWithInfo,
    onLongPress: () -> Unit,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            ),
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
                if (memberInfo.avatarUrl != null) {
                    AsyncImage(
                        model = memberInfo.avatarUrl,
                        contentDescription = "Avatar",
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
                            text = memberInfo.displayName.take(1).uppercase(),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(14.dp))
            
            // Name and Role
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = memberInfo.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = memberInfo.member.role.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = when (memberInfo.member.role) {
                        GroupRole.CREATOR -> Color(0xFF764BA2)
                        GroupRole.ADMIN -> ColorAccent
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            
            // Role Badge
            when (memberInfo.member.role) {
                GroupRole.CREATOR -> {
                    Surface(
                        color = Color(0xFF764BA2).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = "👑 Creator",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF764BA2),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                GroupRole.ADMIN -> {
                    Surface(
                        color = ColorAccent.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = "⭐ Admin",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = ColorAccent,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                else -> {}
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ModernMemberActionsSheet(
    memberInfo: MemberWithInfo,
    currentUserRole: GroupRole?,
    onViewProfile: () -> Unit,
    onMakeAdmin: () -> Unit,
    onDismissAdmin: () -> Unit,
    onRemoveMember: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = CircleShape
            ) {
                if (memberInfo.avatarUrl != null) {
                    AsyncImage(
                        model = memberInfo.avatarUrl,
                        contentDescription = "Avatar",
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
                            text = memberInfo.displayName.take(1).uppercase(),
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = memberInfo.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Surface(
                    color = when (memberInfo.member.role) {
                        GroupRole.CREATOR -> Color(0xFF764BA2).copy(alpha = 0.15f)
                        GroupRole.ADMIN -> ColorAccent.copy(alpha = 0.12f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = memberInfo.member.role.name,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = when (memberInfo.member.role) {
                            GroupRole.CREATOR -> Color(0xFF764BA2)
                            GroupRole.ADMIN -> ColorAccent
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
        
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // View Profile - Always visible
        ModernSheetAction(
            icon = Icons.Default.Person,
            text = "View Profile",
            onClick = onViewProfile
        )
        
        // Creator-only actions (can't act on themselves)
        val isCreator = currentUserRole == GroupRole.CREATOR
        val targetRole = memberInfo.member.role
        
        if (isCreator && targetRole != GroupRole.CREATOR) {
            // Make Admin - only for non-admin members
            if (targetRole == GroupRole.MEMBER || targetRole == GroupRole.MODERATOR) {
                ModernSheetAction(
                    icon = Icons.Default.Star,
                    text = "Make Group Admin",
                    iconTint = ColorAccent,
                    onClick = onMakeAdmin
                )
            }
            
            // Dismiss Admin - only for admins
            if (targetRole == GroupRole.ADMIN) {
                ModernSheetAction(
                    icon = Icons.Default.StarBorder,
                    text = "Dismiss as Admin",
                    onClick = onDismissAdmin
                )
            }
            
            // Remove from Group - for all except Creator
            ModernSheetAction(
                icon = Icons.Default.Delete,
                text = "Remove from Group",
                iconTint = ColorError,
                textColor = ColorError,
                onClick = onRemoveMember
            )
        }
        
        // Admin (not Creator) can remove regular members only
        if (currentUserRole == GroupRole.ADMIN && targetRole == GroupRole.MEMBER) {
            ModernSheetAction(
                icon = Icons.Default.Delete,
                text = "Remove from Group",
                iconTint = ColorError,
                textColor = ColorError,
                onClick = onRemoveMember
            )
        }
    }
}

@Composable
private fun ModernSheetAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    iconTint: Color = MaterialTheme.colorScheme.onSurface,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = RoundedCornerShape(12.dp),
            color = iconTint.copy(alpha = 0.1f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}

// Keep backward compatibility
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MemberCard(
    memberInfo: MemberWithInfo,
    onLongPress: () -> Unit,
    onClick: () -> Unit
) = ModernMemberCard(memberInfo, onLongPress, onClick)
