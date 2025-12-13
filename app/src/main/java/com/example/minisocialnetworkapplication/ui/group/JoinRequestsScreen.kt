package com.example.minisocialnetworkapplication.ui.group

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.minisocialnetworkapplication.core.domain.model.User

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinRequestsScreen(
    navController: NavController,
    conversationId: String,
    viewModel: JoinRequestsViewModel = hiltViewModel()
) {
    val requests by viewModel.requests.collectAsState()
    
    LaunchedEffect(conversationId) {
        viewModel.loadRequests(conversationId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Join Requests") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (requests.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("No pending requests")
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding)) {
                items(requests) { user ->
                    JoinRequestItem(
                        user = user,
                        onAccept = { viewModel.acceptRequest(user.uid) },
                        onDecline = { viewModel.declineRequest(user.uid) }
                    )
                }
            }
        }
    }
}

@Composable
fun JoinRequestItem(
    user: User,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    ListItem(
        headlineContent = { Text(user.name) },
        leadingContent = {
             AsyncImage(
                model = user.avatarUrl ?: "https://api.dicebear.com/7.x/initials/svg?seed=${user.name}",
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        },
        trailingContent = {
            Row {
                IconButton(onClick = onAccept) {
                    Icon(
                        Icons.Default.Check, 
                        contentDescription = "Accept",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDecline) {
                    Icon(
                        Icons.Default.Close, 
                        contentDescription = "Decline",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    )
}
