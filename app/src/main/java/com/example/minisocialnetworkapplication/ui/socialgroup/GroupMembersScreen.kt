package com.example.minisocialnetworkapplication.ui.socialgroup

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.minisocialnetworkapplication.core.domain.model.GroupRole

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
                title = { Text("Members") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Bar
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search members...") },
                leadingIcon = { Icon(Icons.Default.Search, "Search") },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                            Icon(Icons.Default.Close, "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp)
            )
            
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (state.filteredMembers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (state.searchQuery.isNotEmpty()) "No members found" else "No members",
                        color = Color.Gray
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.filteredMembers) { memberInfo ->
                        MemberCard(
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
        
        // Modal Bottom Sheet
        if (showBottomSheet && selectedMember != null) {
            ModalBottomSheet(
                onDismissRequest = { 
                    showBottomSheet = false
                    selectedMember = null
                },
                sheetState = sheetState
            ) {
                MemberActionsSheet(
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MemberCard(
    memberInfo: MemberWithInfo,
    onLongPress: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            AsyncImage(
                model = memberInfo.avatarUrl ?: "https://ui-avatars.com/api/?name=${memberInfo.displayName}",
                contentDescription = "Avatar",
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Name and Role
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = memberInfo.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = memberInfo.member.role.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = when (memberInfo.member.role) {
                        GroupRole.CREATOR -> MaterialTheme.colorScheme.tertiary
                        GroupRole.ADMIN -> MaterialTheme.colorScheme.primary
                        else -> Color.Gray
                    }
                )
            }
            
            // Role Badge
            when (memberInfo.member.role) {
                GroupRole.CREATOR -> {
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = "Creator",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
                GroupRole.ADMIN -> {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = "Admin",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
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
private fun MemberActionsSheet(
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
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = memberInfo.avatarUrl ?: "https://ui-avatars.com/api/?name=${memberInfo.displayName}",
                contentDescription = "Avatar",
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = memberInfo.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = memberInfo.member.role.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
        
        HorizontalDivider()
        
        // View Profile - Always visible
        ListItem(
            headlineContent = { Text("View Profile") },
            leadingContent = { Icon(Icons.Default.Person, null) },
            modifier = Modifier.clickable { onViewProfile() }
        )
        
        // Creator-only actions (can't act on themselves)
        val isCreator = currentUserRole == GroupRole.CREATOR
        val targetRole = memberInfo.member.role
        
        if (isCreator && targetRole != GroupRole.CREATOR) {
            // Make Admin - only for non-admin members
            if (targetRole == GroupRole.MEMBER || targetRole == GroupRole.MODERATOR) {
                ListItem(
                    headlineContent = { Text("Make Group Admin") },
                    leadingContent = { Icon(Icons.Default.Star, null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.clickable { onMakeAdmin() }
                )
            }
            
            // Dismiss Admin - only for admins
            if (targetRole == GroupRole.ADMIN) {
                ListItem(
                    headlineContent = { Text("Dismiss as Admin") },
                    leadingContent = { Icon(Icons.Default.StarBorder, null, tint = Color.Gray) },
                    modifier = Modifier.clickable { onDismissAdmin() }
                )
            }
            
            // Remove from Group - for all except Creator
            ListItem(
                headlineContent = { Text("Remove from Group") },
                leadingContent = { Icon(Icons.Default.Delete, null, tint = Color.Red) },
                modifier = Modifier.clickable { onRemoveMember() }
            )
        }
        
        // Admin (not Creator) can remove regular members only
        if (currentUserRole == GroupRole.ADMIN && targetRole == GroupRole.MEMBER) {
            ListItem(
                headlineContent = { Text("Remove from Group") },
                leadingContent = { Icon(Icons.Default.Delete, null, tint = Color.Red) },
                modifier = Modifier.clickable { onRemoveMember() }
            )
        }
    }
}
