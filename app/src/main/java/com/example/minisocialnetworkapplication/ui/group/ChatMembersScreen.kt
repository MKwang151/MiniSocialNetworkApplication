package com.example.minisocialnetworkapplication.ui.group

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.minisocialnetworkapplication.ui.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatMembersScreen(
    navController: NavController,
    conversationId: String,
    viewModel: ChatMembersViewModel = hiltViewModel()
) {
    val members by viewModel.members.collectAsState()
    val currentUserIsAdmin by viewModel.currentUserIsAdmin.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedMember by remember { mutableStateOf<MemberUiModel?>(null) }
    val sheetState = rememberModalBottomSheetState()

    // error giữ nguyên (UI-only)
    LaunchedEffect(error) { /* keep */ }

    if (showBottomSheet && selectedMember != null) {
        val target = selectedMember!!
        val targetIsAdmin = target.isAdmin
        val targetIsCreator = target.isCreator

        ModalBottomSheet(
            onDismissRequest = {
                showBottomSheet = false
                selectedMember = null
            },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp)
            ) {
                // Header (avatar + name + role) - IG-like
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .size(58.dp)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                shape = CircleShape
                            )
                    ) {
                        AsyncImage(
                            model = target.user.avatarUrl
                                ?: "https://api.dicebear.com/7.x/initials/svg?seed=${target.user.name}",
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = target.user.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                        val sub = when {
                            targetIsCreator -> "Group creator"
                            targetIsAdmin -> "Admin"
                            else -> "Member"
                        }
                        Text(
                            text = sub,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                @Composable
                fun SheetAction(
                    title: String,
                    icon: @Composable () -> Unit,
                    tint: Color = MaterialTheme.colorScheme.onSurface,
                    onClick: () -> Unit
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(onClick = onClick),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CompositionLocalProvider(LocalContentColor provides tint) {
                                icon()
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = tint
                            )
                        }
                    }
                }

                if (currentUserIsAdmin && !targetIsCreator) {
                    SheetAction(
                        title = if (targetIsAdmin) "Dismiss as admin" else "Make group admin",
                        icon = { Icon(Icons.Default.Star, contentDescription = null) },
                        onClick = {
                            if (targetIsAdmin) viewModel.demoteAdmin(target.user.uid)
                            else viewModel.promoteToAdmin(target.user.uid)
                            showBottomSheet = false
                        }
                    )

                    Spacer(Modifier.height(10.dp))

                    SheetAction(
                        title = "Remove from group",
                        icon = { Icon(Icons.Default.DeleteOutline, contentDescription = null) },
                        tint = MaterialTheme.colorScheme.error,
                        onClick = {
                            viewModel.removeMember(target.user.uid)
                            showBottomSheet = false
                        }
                    )

                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(Modifier.height(14.dp))
                }

                SheetAction(
                    title = "View profile",
                    icon = { Icon(Icons.Default.Person, contentDescription = null) },
                    onClick = {
                        navController.navigate(Screen.Profile.createRoute(target.user.uid))
                        showBottomSheet = false
                    }
                )

                Spacer(Modifier.height(12.dp))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Members",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${members.size} members",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                )
            )
        }
    ) { padding ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = members,
                        key = { it.user.uid }
                    ) { member ->
                        MemberCard(
                            member = member,
                            onClick = {
                                navController.navigate(Screen.Profile.createRoute(member.user.uid))
                            },
                            onLongClick = {
                                if (currentUserIsAdmin) {
                                    selectedMember = member
                                    showBottomSheet = true
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MemberCard(
    member: MemberUiModel,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val roleLabel = when {
        member.isCreator -> "Creator"
        member.isAdmin -> "Admin"
        else -> null
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // avatar
            Surface(
                modifier = Modifier.size(54.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                AsyncImage(
                    model = member.user.avatarUrl
                        ?: "https://api.dicebear.com/7.x/initials/svg?seed=${member.user.name}",
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = member.user.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (roleLabel != null) {
                        Spacer(Modifier.width(10.dp))
                        RolePill(text = roleLabel, highlight = member.isCreator)
                    }
                }
                
                Spacer(Modifier.height(3.dp))

                Text(
                    text = if (member.isCreator) "Group creator"
                    else if (member.isAdmin) "Group admin"
                    else "Member",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RolePill(
    text: String,
    highlight: Boolean
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = if (highlight)
            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
        else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (highlight) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
