package com.example.minisocialnetworkapplication.ui.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

private val ScreenPadding = 16.dp
private val CardRadius = 20.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendScreen(
    onNavigateToProfile: (String) -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToChat: (String) -> Unit,
    bottomBar: @Composable () -> Unit,
    viewModel: FriendViewModel = hiltViewModel()
) {
    val requestState by viewModel.requestUiState.collectAsState()
    val friendState by viewModel.friendUiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Friends",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Connect & chat instantly",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
                )
            )
        },
        bottomBar = bottomBar,
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->

        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
        ) {

            item { SectionHeader(title = "Friend Requests") }

            // Friend request list (logic giữ nguyên)
            when (val state = requestState) {
                is FriendRequestUiState.Loading -> item {
                    LoadingView(modifier = Modifier.padding(ScreenPadding))
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
                        modifier = Modifier.padding(ScreenPadding)
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(6.dp))
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = ScreenPadding),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            item { SectionHeader(title = "Friends") }

            // Friend list (logic giữ nguyên)
            when (val state = friendState) {
                is FriendUiState.Loading -> item {
                    LoadingView(modifier = Modifier.padding(ScreenPadding))
                }
                is FriendUiState.Success -> {
                    friends(
                        friendList = state.friends,
                        onNavigateToProfile = onNavigateToProfile,
                        onNavigateToSearch = onNavigateToSearch,
                        onNavigateToChat = onNavigateToChat,
                        onUnfriend = viewModel::onUnfriend
                    )
                }
                is FriendUiState.Error -> item {
                    ErrorView(
                        message = state.message,
                        onRetryClick = viewModel::refresh,
                        modifier = Modifier.padding(ScreenPadding)
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenPadding, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
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
            EmptyBlock(
                title = "No friend requests",
                subtitle = "When someone requests, it will appear here."
            )
        }
    } else {
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
    onNavigateToChat: (String) -> Unit,
    onUnfriend: (String) -> Unit
) {
    if (friendList.isEmpty()) {
        item {
            EmptyBlock(
                title = "No friends yet",
                subtitle = "Search and add friends to start chatting.",
                actionText = "Search",
                onActionClick = onNavigateToSearch
            )
        }
    } else {
        items(
            items = friendList,
            key = { it.friendId }
        ) { friend ->
            FriendCard(
                friend = friend,
                onNavigateToProfile = onNavigateToProfile,
                onNavigateToChat = onNavigateToChat,
                onUnfriend = onUnfriend
            )
        }
    }
}

@Composable
private fun EmptyBlock(
    title: String,
    subtitle: String,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center
        ) {
            Text("👥", style = MaterialTheme.typography.headlineSmall)
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (actionText != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onActionClick,
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(actionText)
            }
        }
    }
}

@Composable
fun FriendCard(
    friend: Friend,
    onNavigateToProfile: (String) -> Unit,
    onNavigateToChat: (String) -> Unit,
    onUnfriend: (String) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenPadding, vertical = 7.dp)
            .clickable { onNavigateToProfile(friend.friendId) },
        shape = RoundedCornerShape(CardRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f)
        ),

        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            width = 1.dp,
            brush = androidx.compose.ui.graphics.SolidColor(
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarIG(
                url = friend.friendAvatarUrl,
                size = 56.dp
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = friend.friendName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(6.dp))

                MutualPill(text = "${friend.mutualFriends} mutual friends")
            }

            // Chat button (IG-like)
            IconButton(
                onClick = { onNavigateToChat(friend.friendId) }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Message,
                    contentDescription = "Chat",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                "Unfriend",
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        onClick = {
                            showMenu = false
                            onUnfriend(friend.friendId)
                        }
                    )
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
            .padding(horizontal = ScreenPadding, vertical = 7.dp)
            .clickable { onNavigateToProfile(friend.friendId) },
        shape = RoundedCornerShape(CardRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f)
        ),

        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            width = 1.dp,
            brush = androidx.compose.ui.graphics.SolidColor(
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                AvatarIG(
                    url = friend.friendAvatarUrl,
                    size = 56.dp
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = friend.friendName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    MutualPill(text = "${friend.mutualFriends} mutual friends")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { onAcceptRequest(friend.friendId) },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .pointerInput(Unit) {}, // prevent card click
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Accept", fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = { onDeclineRequest(friend.friendId) },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .pointerInput(Unit) {}, // prevent card click
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Decline", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/** UI-only: avatar kiểu IG (viền nhẹ) */
@Composable
private fun AvatarIG(
    url: String?,
    size: androidx.compose.ui.unit.Dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.surface,
                shape = CircleShape
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = url,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
        )
    }
}

/** UI-only: pill cho mutual friends */
@Composable
private fun MutualPill(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
