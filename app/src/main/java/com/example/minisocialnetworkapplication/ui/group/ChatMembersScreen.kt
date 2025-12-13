package com.example.minisocialnetworkapplication.ui.group

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.minisocialnetworkapplication.core.domain.model.User
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
    
    // Error handling
    LaunchedEffect(error) {
        if (error != null) {
            // Show error (e.g. Snackbar) - simplified here
        }
    }

    if (showBottomSheet && selectedMember != null) {
        ModalBottomSheet(
            onDismissRequest = { 
                showBottomSheet = false 
                selectedMember = null
            },
            sheetState = sheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = selectedMember?.user?.name ?: "",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (currentUserIsAdmin) {
                     val targetIsAdmin = selectedMember?.isAdmin == true
                     val targetIsCreator = selectedMember?.isCreator == true

                     if (!targetIsCreator) {
                         ListItem(
                             headlineContent = { 
                                 Text(if (targetIsAdmin) "Dismiss as Admin" else "Make Group Admin") 
                             },
                             leadingContent = {
                                 Icon(Icons.Default.Star, contentDescription = null)
                             },
                             modifier = Modifier.clickable {
                                 if (targetIsAdmin) {
                                     viewModel.demoteAdmin(selectedMember!!.user.uid)
                                 } else {
                                     viewModel.promoteToAdmin(selectedMember!!.user.uid)
                                 }
                                 showBottomSheet = false
                             }
                         )
                         
                         ListItem(
                             headlineContent = { Text("Remove from Group", color = MaterialTheme.colorScheme.error) },
                             modifier = Modifier.clickable {
                                 viewModel.removeMember(selectedMember!!.user.uid)
                                 showBottomSheet = false
                             }
                         )
                     }
                }
                
                ListItem(
                     headlineContent = { Text("View Profile") },
                     modifier = Modifier.clickable {
                         navController.navigate(Screen.Profile.createRoute(selectedMember!!.user.uid))
                         showBottomSheet = false
                     }
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Group Members")
                        Text(
                            "${members.size} members", 
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(members) { member ->
                    MemberItem(
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MemberItem(
    member: MemberUiModel,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    ListItem(
        modifier = Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick
        ),
        headlineContent = { Text(member.user.name) },
        supportingContent = {
            if (member.isAdmin) {
                Text(
                    text = if(member.isCreator) "Group Creator" else "Admin",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        },
        leadingContent = {
            AsyncImage(
                model = member.user.avatarUrl ?: "https://api.dicebear.com/7.x/initials/svg?seed=${member.user.name}",
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        },
        trailingContent = {
            if (member.isAdmin) {
                Text(
                    "Admin",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    )
}
