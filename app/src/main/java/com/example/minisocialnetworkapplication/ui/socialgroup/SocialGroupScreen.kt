package com.example.minisocialnetworkapplication.ui.socialgroup

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.minisocialnetworkapplication.core.domain.model.Group

@Composable
fun SocialGroupScreen(
    onNavigateToCreateGroup: () -> Unit,
    onNavigateToGroupDetail: (String) -> Unit,
    viewModel: SocialGroupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Your Groups", "Posts", "Discover", "Manage")

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreateGroup,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Group")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }

            when (val state = uiState) {
                is SocialGroupUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is SocialGroupUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                    }
                }
                is SocialGroupUiState.Success -> {
                    when (selectedTabIndex) {
                        0 -> GroupList(groups = state.myGroups, onGroupClick = onNavigateToGroupDetail)
                        1 -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Group Posts coming soon") }
                        2 -> GroupList(groups = state.discoverGroups, onGroupClick = onNavigateToGroupDetail)
                        3 -> GroupList(groups = state.managedGroups, onGroupClick = onNavigateToGroupDetail)
                    }
                }
            }
        }
    }
}

@Composable
fun GroupList(
    groups: List<Group>,
    onGroupClick: (String) -> Unit
) {
    if (groups.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "No groups found")
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(groups) { group ->
                androidx.compose.material3.ListItem(
                    headlineContent = { Text(group.name) },
                    supportingContent = { Text("${group.memberCount} members") },
                    leadingContent = {
                        // Avatar placeholder
                        androidx.compose.material3.Surface(
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.androidx.compose.foundation.layout.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(group.name.take(1).uppercase())
                            }
                        }
                    },
                    modifier = Modifier.androidx.compose.foundation.clickable { onGroupClick(group.id) }
                )
                androidx.compose.material3.HorizontalDivider()
            }
        }
    }
}
