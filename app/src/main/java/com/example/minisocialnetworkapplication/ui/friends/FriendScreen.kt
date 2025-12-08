package com.example.minisocialnetworkapplication.ui.friends

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.minisocialnetworkapplication.core.domain.model.Friend
import com.example.minisocialnetworkapplication.ui.profile.ErrorView
import com.example.minisocialnetworkapplication.ui.profile.LoadingView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendScreen(
    onNavigateToProfile: (String) -> Unit,
    onNavigateToSearch: () -> Unit,
    bottomBar: @Composable () -> Unit,
    viewModel: FriendViewModel = hiltViewModel()
) {
    val requestState by viewModel.requestUiState.collectAsState()
    val friendState by viewModel.friendUiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Friends") }
            )
        },
        bottomBar = bottomBar
    ) { paddingValues ->

        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            item {
                Text(
                    "Friend Requests",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
            }

            // Friend request list
            when (val state = requestState) {
                is FriendRequestUiState.Loading -> item {
                    LoadingView(modifier = Modifier.padding(paddingValues))
                }
                is FriendRequestUiState.Success -> {
                    friendRequests(
                        requestList = state.friendRequests,
                        onNavigateToProfile = onNavigateToProfile,
                        onAcceptRequest = viewModel::onAcceptRequest,
                        onDeclineRequest = viewModel::onDeclineRequest
                    )
                }
                is FriendRequestUiState.Error -> item {
                    ErrorView(
                        message = state.message,
                        onRetryClick = viewModel::refresh,
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            }

            item { HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp)) }

            item {
                Text(
                    "Friends",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp)
                )
            }

            // Friend list
            when (val state = friendState) {
                is FriendUiState.Loading -> item {
                    LoadingView(modifier = Modifier.padding(paddingValues))
                }
                is FriendUiState.Success -> {
                    friends(
                        friendList = state.friends,
                        onNavigateToProfile = onNavigateToProfile,
                        onNavigateToSearch = onNavigateToSearch,
                        onUnfriend = viewModel::onUnfriend
                    )
                }
                is FriendUiState.Error -> item {
                    ErrorView(
                        message = state.message,
                        onRetryClick = viewModel::refresh,
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            }
        }
    }
}

fun LazyListScope.friendRequests(
    requestList: List<Friend>,
    onNavigateToProfile: (String) -> Unit,
    onAcceptRequest: (String) -> Unit,
    onDeclineRequest: (String) -> Unit
) {
    if (requestList.isEmpty()) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "No friend requests",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    else {
        items(
            items = requestList,
            key = { it.friendId }
        ) { friend ->
            FriendRequestCard(
                friend = friend,
                onNavigateToProfile = onNavigateToProfile,
                onAcceptRequest = onAcceptRequest,
                onDeclineRequest = onDeclineRequest
            )
        }
    }
}

fun LazyListScope.friends(
    friendList: List<Friend>,
    onNavigateToProfile: (String) -> Unit,
    onNavigateToSearch: () -> Unit,
    onUnfriend: (String) -> Unit
) {
    if (friendList.isEmpty()) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "No friends :(( Search for friends now",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(onClick = onNavigateToSearch) {
                    Text("Search")
                }
            }
        }
    }
    else {
        items(
            items = friendList,
            key = { it.friendId }
        ) { friend ->
            FriendCard(
                friend = friend,
                onNavigateToProfile = onNavigateToProfile,
                onUnfriend = onUnfriend
            )
        }
    }
}

@Composable
fun FriendCard(
    friend: Friend,
    onNavigateToProfile: (String) -> Unit,
    onUnfriend: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable { onNavigateToProfile(friend.friendId) },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = friend.friendAvatarUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray),
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = friend.friendName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { onUnfriend(friend.friendId) },
                    modifier = Modifier
                        .weight(1f)
                        .pointerInput(Unit) {}, // prevent card click)
                ) {
                    Text("Unfriend")
                }
            }
        }
    }
}

@Composable
fun FriendRequestCard(
    friend: Friend,
    onNavigateToProfile: (String) -> Unit,
    onAcceptRequest: (String) -> Unit,
    onDeclineRequest: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable { onNavigateToProfile(friend.friendId) },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            AsyncImage(
                model = friend.friendAvatarUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray),
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Name
                Text(
                    text = friend.friendName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row {
                    Button(
                        onClick = { onAcceptRequest(friend.friendId) },
                        modifier = Modifier
                            .weight(1f)
                            .pointerInput(Unit) {},     // prevent card click
                    ) {
                        Text("Accept")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedButton(
                        onClick = { onDeclineRequest(friend.friendId) },
                        modifier = Modifier
                            .weight(1f)
                            .pointerInput(Unit) {},     // prevent card click
                    ) {
                        Text("Decline")
                    }
                }
            }
        }
    }
}