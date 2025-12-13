package com.example.minisocialnetworkapplication.ui.group

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.minisocialnetworkapplication.core.domain.model.Friend
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMemberScreen(
    navController: NavController,
    conversationId: String,
    viewModel: AddMemberViewModel = hiltViewModel()
) {
    val friends by viewModel.filteredFriends.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFriends by viewModel.selectedFriends.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(conversationId) {
        viewModel.loadData(conversationId)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Add Members") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            viewModel.addMembers(
                                onSuccess = {
                                    navController.popBackStack()
                                },
                                onError = { msg ->
                                    // Show error
                                    scope.launch {
                                        snackbarHostState.showSnackbar(msg)
                                    }
                                }
                            )
                        },
                        enabled = selectedFriends.isNotEmpty() && !isLoading
                    ) {
                        Text("Add")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Search Bar (Simplified)
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search friends") },
                singleLine = true
            )

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(friends) { friend ->
                        AddMemberItem(
                            friend = friend,
                            isSelected = selectedFriends.contains(friend.friendId),
                            onToggle = { viewModel.toggleSelection(friend.friendId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddMemberItem(
    friend: Friend,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    ListItem(
        modifier = Modifier.clickable(onClick = onToggle),
        headlineContent = { Text(friend.friendName) },
        leadingContent = {
            AsyncImage(
                model = friend.friendAvatarUrl ?: "https://api.dicebear.com/7.x/initials/svg?seed=${friend.friendName}",
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        },
        trailingContent = {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() }
            )
        }
    )
}
