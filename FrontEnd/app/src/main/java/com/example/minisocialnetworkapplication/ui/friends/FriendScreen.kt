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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.minisocialnetworkapplication.core.domain.model.Friend
import com.example.minisocialnetworkapplication.ui.profile.ErrorView
import com.example.minisocialnetworkapplication.ui.profile.LoadingView

// Modern color palette
private val ColorAccent = Color(0xFF667EEA)
private val ColorSuccess = Color(0xFF11998E)
private val ColorError = Color(0xFFFF416C)
private val GradientPrimary = listOf(Color(0xFF667EEA), Color(0xFF764BA2))

private val ScreenPadding = 16.dp
private val CardRadius = 18.dp

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
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        )
                        Text(
                            text = "Connect & chat instantly",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                )
            )
        },
        bottomBar = bottomBar
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                        )
                    )
                )
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                item { ModernSectionHeader(title = "Friend Requests", icon = Icons.Default.PersonAdd) }

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
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = ScreenPadding),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                item { ModernSectionHeader(title = "Friends", icon = Icons.Default.Group) }

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

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun ModernSectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenPadding, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            color = ColorAccent.copy(alpha = 0.1f),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier
                    .padding(8.dp)
                    .size(20.dp),
                tint = ColorAccent
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
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
            ModernEmptyBlock(
                emoji = "📩",
                title = "No friend requests",
                subtitle = "When someone sends a request, it will appear here."
            )
        }
    } else {
        items(
            items = requestList,
            key = { it.friendId }
        ) { friend ->
            ModernFriendRequestCard(
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
            ModernEmptyBlock(
                emoji = "👥",
                title = "No friends yet",
                subtitle = "Search and add friends to start chatting.",
                actionText = "Search",
                actionIcon = Icons.Default.Search,
                onActionClick = onNavigateToSearch
            )
        }
    } else {
        items(
            items = friendList,
            key = { it.friendId }
        ) { friend ->
            ModernFriendCard(
                friend = friend,
                onNavigateToProfile = onNavigateToProfile,
                onNavigateToChat = onNavigateToChat,
                onUnfriend = onUnfriend
            )
        }
    }
}

@Composable
private fun ModernEmptyBlock(
    emoji: String,
    title: String,
    subtitle: String,
    actionText: String? = null,
    actionIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
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
                .size(90.dp)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            ColorAccent.copy(alpha = 0.12f),
                            Color(0xFF764BA2).copy(alpha = 0.12f)
                        )
                    ),
                    RoundedCornerShape(24.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(emoji, style = MaterialTheme.typography.displaySmall)
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        if (actionText != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onActionClick,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ColorAccent)
            ) {
                if (actionIcon != null) {
                    Icon(actionIcon, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text(actionText, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun ModernFriendCard(
    friend: Friend,
    onNavigateToProfile: (String) -> Unit,
    onNavigateToChat: (String) -> Unit,
    onUnfriend: (String) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenPadding, vertical = 6.dp)
            .shadow(3.dp, RoundedCornerShape(CardRadius))
            .clickable { onNavigateToProfile(friend.friendId) },
        shape = RoundedCornerShape(CardRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ModernAvatarIG(
                url = friend.friendAvatarUrl,
                name = friend.friendName,
                size = 56.dp
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = friend.friendName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(4.dp))

                ModernMutualPill(count = friend.mutualFriends)
            }

            // Chat button
            Surface(
                color = ColorAccent.copy(alpha = 0.1f),
                shape = CircleShape,
                modifier = Modifier.size(42.dp)
            ) {
                IconButton(onClick = { onNavigateToChat(friend.friendId) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Message,
                        contentDescription = "Chat",
                        tint = ColorAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.width(4.dp))

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
                                color = ColorError,
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
fun ModernFriendRequestCard(
    friend: Friend,
    onNavigateToProfile: (String) -> Unit,
    onAcceptRequest: (String) -> Unit,
    onDeclineRequest: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenPadding, vertical = 6.dp)
            .shadow(4.dp, RoundedCornerShape(CardRadius))
            .clickable { onNavigateToProfile(friend.friendId) },
        shape = RoundedCornerShape(CardRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ModernAvatarIG(
                    url = friend.friendAvatarUrl,
                    name = friend.friendName,
                    size = 56.dp
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = friend.friendName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    ModernMutualPill(count = friend.mutualFriends)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { onAcceptRequest(friend.friendId) },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .pointerInput(Unit) {},
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ColorSuccess
                    )
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Accept", fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = { onDeclineRequest(friend.friendId) },
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .pointerInput(Unit) {},
                    shape = RoundedCornerShape(12.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.linearGradient(listOf(ColorError.copy(alpha = 0.5f), ColorError.copy(alpha = 0.5f)))
                    )
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = ColorError
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Decline", fontWeight = FontWeight.SemiBold, color = ColorError)
                }
            }
        }
    }
}

@Composable
private fun ModernAvatarIG(
    url: String?,
    name: String,
    size: androidx.compose.ui.unit.Dp
) {
    Surface(
        modifier = Modifier
            .size(size)
            .border(2.dp, ColorAccent.copy(alpha = 0.3f), CircleShape),
        shape = CircleShape,
        color = Color.Transparent
    ) {
        if (!url.isNullOrEmpty()) {
            AsyncImage(
                model = url,
                contentDescription = null,
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
                    text = name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ModernMutualPill(count: Int) {
    Surface(
        color = ColorAccent.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = "$count mutual friends",
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = ColorAccent,
            fontWeight = FontWeight.Medium
        )
    }
}

// Keep backward compatibility
@Composable
fun FriendCard(
    friend: Friend,
    onNavigateToProfile: (String) -> Unit,
    onNavigateToChat: (String) -> Unit,
    onUnfriend: (String) -> Unit
) = ModernFriendCard(friend, onNavigateToProfile, onNavigateToChat, onUnfriend)

@Composable
fun FriendRequestCard(
    friend: Friend,
    onNavigateToProfile: (String) -> Unit,
    onAcceptRequest: (String) -> Unit,
    onDeclineRequest: (String) -> Unit
) = ModernFriendRequestCard(friend, onNavigateToProfile, onAcceptRequest, onDeclineRequest)
