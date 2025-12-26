package com.example.minisocialnetworkapplication.ui.chat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.heightIn
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.minisocialnetworkapplication.core.domain.model.ConversationType
import com.example.minisocialnetworkapplication.core.domain.model.Message
import com.example.minisocialnetworkapplication.core.domain.model.MessageStatus
import com.example.minisocialnetworkapplication.core.domain.model.MessageType
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.material3.Surface
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.shadow
import androidx.compose.material3.CardDefaults

// Modern color palette
private val ColorAccent = Color(0xFF667EEA)
private val ColorSuccess = Color(0xFF4CAF50)
private val GradientPrimary = listOf(Color(0xFF667EEA), Color(0xFF764BA2))



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    viewModel: ChatDetailViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToSettings: (String) -> Unit,
    scrollToMessageId: String? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentUserId = viewModel.currentUserId ?: ""

    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    
    // State for context menu bottom sheet
    var selectedMessage by remember { mutableStateOf<Message?>(null) }
    val sheetState = rememberModalBottomSheetState()
    
    // State for pinned message scroll and highlight
    var highlightedMessageId by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.sendMediaMessage(uris, MessageType.IMAGE)
        }
    }

    // Scroll to specific message if requested
    // We use a flag to track if we are in "search result mode" to prevent auto-scrolling to bottom
    var focusingOnSearchResult by remember { mutableStateOf(false) }

    LaunchedEffect(scrollToMessageId, uiState.messages) {
        if (scrollToMessageId != null && uiState.messages.isNotEmpty()) {
            val index = uiState.messages.indexOfFirst { it.id == scrollToMessageId }
            if (index != -1) {
                focusingOnSearchResult = true
                listState.animateScrollToItem(index)
                highlightedMessageId = scrollToMessageId
                // Reset focus mode after a delay or interaction? 
                // For now, let's keep it until user manual scroll? 
                // Actually, just preventing the immediate next auto-scroll is enough.
            } else {
                // Fallback: fuzzy match by timestamp if ID mismatch (Local vs Remote)
                // Try finding message with same timestamp (ignoring millis)
                // Note: We don't have the target timestamp/content here, only ID. 
                // So we can't fuzzy match unless we pass more info.
                // But given duplicate fix in Search, ID should match if message is in list.
            }
        }
    }

    // Scroll to bottom when new messages arrive
    // ONLY if not currently focusing on a search result
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty() && !focusingOnSearchResult) {
            // Check if we are already at bottom?
            // If user scrolled up, we might not want to force scroll unless it's a NEW message from SELF?
            // But existing behavior was "scroll on size change". 
            // We just add the guard.
            listState.animateScrollToItem(0)
        }
    }
    
    // Mark as read when leaving the screen (ensure no unread when navigating back)
    DisposableEffect(Unit) {
        onDispose {
            viewModel.markAllAsRead()
        }
    }

    // Determine title
    val title = when {
        uiState.conversation?.type == ConversationType.GROUP -> uiState.conversation?.name ?: "Group"
        uiState.otherUser != null -> uiState.otherUser?.name ?: "Chat"
        else -> "Chat"
    }

    val avatarUrl = when {
        uiState.conversation?.type == ConversationType.GROUP -> uiState.conversation?.avatarUrl
        else -> uiState.otherUser?.avatarUrl
    }

    // Message context menu bottom sheet
    if (selectedMessage != null) {
        val message = selectedMessage!!
        val isOutgoing = message.isOutgoing(currentUserId)
        val isPinned = uiState.conversation?.pinnedMessageIds?.contains(message.id) == true
        
        ModalBottomSheet(
            onDismissRequest = { selectedMessage = null },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                // Reaction bar at top
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ReactionBar(
                        onReactionSelected = { emoji ->
                            viewModel.toggleReaction(message.id, emoji)
                        },
                        onDismiss = { selectedMessage = null }
                    )
                }
                // Pin/Unpin option (available for all messages)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (isPinned) {
                                viewModel.unpinMessage(message.id)
                            } else {
                                viewModel.pinMessage(message.id)
                            }
                            selectedMessage = null
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (isPinned) "📌" else "📌", modifier = Modifier.padding(end = 12.dp))
                    Text(
                        if (isPinned) "Unpin" else "Pin", 
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                
                if (isOutgoing) {
                    // Delete option for own messages
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.revokeMessage(message.id)
                                selectedMessage = null
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🗑️", modifier = Modifier.padding(end = 12.dp))
                        Text("Delete", style = MaterialTheme.typography.bodyLarge)
                    }
                } else {
                    // Reply option for received messages
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setReplyToMessage(message)
                                selectedMessage = null
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("↩️", modifier = Modifier.padding(end = 12.dp))
                        Text("Reply", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                uiState.conversation?.id?.let { onNavigateToSettings(it) }
                            }
                    ) {
                        // Avatar with gradient fallback
                        Box(modifier = Modifier.size(44.dp)) {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                shape = CircleShape,
                                color = Color.Transparent
                            ) {
                                if (avatarUrl != null) {
                                    AsyncImage(
                                        model = avatarUrl,
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
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            modifier = Modifier.size(22.dp),
                                            tint = Color.White
                                        )
                                    }
                                }
                            }

                            // Online dot
                            if (uiState.otherUser?.isOnline == true) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .align(Alignment.BottomEnd)
                                        .background(MaterialTheme.colorScheme.surface, CircleShape)
                                        .padding(2.dp)
                                        .background(ColorSuccess, CircleShape)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.SemiBold
                            )

                            val statusText = uiState.otherUser?.getStatusText().orEmpty()
                            if (statusText.isNotEmpty()) {
                                Text(
                                    text = statusText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (uiState.otherUser?.isOnline == true)
                                        Color(0xFF4CAF50)
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {
            // Pinned Messages Bar
            val pinnedMessageIds = uiState.conversation?.pinnedMessageIds ?: emptyList()
            val pinnedMessages = uiState.messages.filter { it.id in pinnedMessageIds }
            // State for pinned bar expansion
            var isPinnedBarExpanded by remember { mutableStateOf(false) }
            
            AnimatedVisibility(
                visible = pinnedMessages.isNotEmpty(),
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                ) {
                    // Header row - always visible
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (pinnedMessages.size > 1) {
                                    isPinnedBarExpanded = !isPinnedBarExpanded
                                } else {
                                    // Single pin - scroll to message
                                    val firstPinned = pinnedMessages.first()
                                    val index = uiState.messages.indexOfFirst { it.id == firstPinned.id }
                                    if (index >= 0) {
                                        coroutineScope.launch {
                                            listState.animateScrollToItem(index)
                                            highlightedMessageId = firstPinned.id
                                            delay(2000)
                                            highlightedMessageId = null
                                        }
                                    }
                                }
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📌", modifier = Modifier.padding(end = 8.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (pinnedMessages.size > 1) "${pinnedMessages.size} pinned messages" else "Pinned message",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (!isPinnedBarExpanded && pinnedMessages.isNotEmpty()) {
                                Text(
                                    text = pinnedMessages.first().getDisplayText(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                        
                        // Expand/Collapse button (only if multiple pins)
                        if (pinnedMessages.size > 1) {
                            IconButton(
                                onClick = { isPinnedBarExpanded = !isPinnedBarExpanded },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Text(
                                    if (isPinnedBarExpanded) "▲" else "▼",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                        
                        // Quick unpin first pin
                        if (!isPinnedBarExpanded && pinnedMessages.isNotEmpty()) {
                            IconButton(
                                onClick = { viewModel.unpinMessage(pinnedMessages.first().id) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Unpin",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                    
                    // Expanded list of all pinned messages
                    AnimatedVisibility(visible = isPinnedBarExpanded) {
                        Column {
                            pinnedMessages.forEach { pinnedMessage ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val index = uiState.messages.indexOfFirst { it.id == pinnedMessage.id }
                                            if (index >= 0) {
                                                coroutineScope.launch {
                                                    listState.animateScrollToItem(index)
                                                    highlightedMessageId = pinnedMessage.id
                                                    delay(2000)
                                                    highlightedMessageId = null
                                                    isPinnedBarExpanded = false
                                                }
                                            }
                                        }
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = pinnedMessage.getDisplayText(),
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f),
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    
                                    IconButton(
                                        onClick = { viewModel.unpinMessage(pinnedMessage.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Unpin",
                                            modifier = Modifier.size(12.dp),
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // Messages List
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when {
                    uiState.isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    uiState.messages.isEmpty() -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
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
                                Text("👋", style = MaterialTheme.typography.displaySmall)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Start the conversation",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Say hello!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            state = listState,
                            reverseLayout = true, // Newest messages at bottom
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                bottom = if (uiState.typingUsers.isNotEmpty()) 56.dp else 0.dp
                            )
                        ) {
                            items(
                                items = uiState.messages,
                                key = { it.localId }
                            ) { message ->
                                val isPinned = uiState.conversation?.pinnedMessageIds?.contains(message.id) == true
                                val isHighlighted = highlightedMessageId == message.id
                                SwipeableMessageBubble(
                                    message = message,
                                    isOutgoing = message.isOutgoing(currentUserId),
                                    isPinned = isPinned,
                                    isHighlighted = isHighlighted,
                                    senderAvatarUrl = if (message.isOutgoing(currentUserId)) null 
                                        else if (uiState.conversation?.type == ConversationType.GROUP) message.senderAvatarUrl 
                                        else uiState.otherUser?.avatarUrl,
                                    senderName = if (!message.isOutgoing(currentUserId) && uiState.conversation?.type == ConversationType.GROUP) message.senderName else null,
                                    currentUserId = currentUserId,
                                    onReply = {
                                        viewModel.setReplyToMessage(message)
                                    },
                                    onDelete = {
                                        viewModel.revokeMessage(message.id)
                                    },
                                    onLongClick = {
                                        selectedMessage = message
                                    },
                                    onReplyMessageClick = { replyToId ->
                                        // Scroll to the original replied message
                                        val index = uiState.messages.indexOfFirst { it.id == replyToId }
                                        if (index >= 0) {
                                            coroutineScope.launch {
                                                listState.animateScrollToItem(index)
                                                highlightedMessageId = replyToId
                                                delay(2000)
                                                highlightedMessageId = null
                                            }
                                        }
                                    },
                                    onDoubleTap = {
                                        // Show reaction bar by setting selected message
                                        selectedMessage = message
                                    },
                                    onReactionClick = { emoji ->
                                        viewModel.toggleReaction(message.id, emoji)
                                    }
                                )
                            }
                        }
                    }
                }
                
                // Typing indicator bubble - positioned at bottom
                if (uiState.typingUsers.isNotEmpty()) {
                    TypingIndicatorBubble(
                        avatarUrl = uiState.otherUser?.avatarUrl,
                        modifier = Modifier.align(Alignment.BottomStart)
                    )
                }
            }

            // Reply preview
            AnimatedVisibility(
                visible = uiState.replyToMessage != null,
                enter = slideInVertically { it } + fadeIn(),
                exit = fadeOut()
            ) {
                uiState.replyToMessage?.let { replyMessage ->
                    ReplyPreview(
                        message = replyMessage,
                        onDismiss = { viewModel.setReplyToMessage(null) }
                    )
                }
            }

            // Message Input
            MessageInput(
                text = messageText,
                onTextChange = {
                    messageText = it
                    viewModel.onTextChanged(it)
                },
                onSendClick = {
                    viewModel.sendMessage(messageText)
                    messageText = ""
                },
                onAttachClick = {
                    imagePickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                isSending = uiState.isSending
            )
        }
    }
}

/**
 * Swipeable wrapper for MessageBubble to trigger reply on swipe
 * Also handles long press context menu
 * - Incoming messages: swipe right to reply
 * - Outgoing messages: swipe left to reply
 */
@Composable
private fun SwipeableMessageBubble(
    message: Message,
    isOutgoing: Boolean,
    isPinned: Boolean = false,
    isHighlighted: Boolean = false,
    senderAvatarUrl: String? = null,
    senderName: String? = null, // Added senderName
    currentUserId: String = "",
    onReply: () -> Unit,
    onDelete: () -> Unit,
    onLongClick: () -> Unit,
    onReplyMessageClick: (String) -> Unit = {},
    onDoubleTap: () -> Unit = {},
    onReactionClick: (String) -> Unit = {}
) {
    var offsetX by remember { mutableStateOf(0f) }
    val swipeThreshold = 80f
    val maxSwipe = 100f
    
    // Calculate swipe progress for visual feedback
    val swipeProgress = (kotlin.math.abs(offsetX) / swipeThreshold).coerceIn(0f, 1f)
    val triggerReply = kotlin.math.abs(offsetX) >= swipeThreshold

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Reply icon shown during swipe
        if (offsetX != 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = if (isOutgoing) Alignment.CenterEnd else Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .size((24 * swipeProgress).coerceAtLeast(16f).dp)
                        .clip(CircleShape)
                        .background(
                            if (triggerReply) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Reply",
                        modifier = Modifier.size((16 * swipeProgress).coerceAtLeast(12f).dp),
                        tint = if (triggerReply) 
                            MaterialTheme.colorScheme.onPrimary 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        // Message bubble with drag gesture
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.toInt(), 0) }
                .pointerInput(isOutgoing) {
                    detectHorizontalDragGestures(
                        onDragStart = { },
                        onDragEnd = {
                            // Check threshold at the moment of release
                            val shouldReply = kotlin.math.abs(offsetX) >= swipeThreshold
                            if (shouldReply) {
                                onReply()
                            }
                            offsetX = 0f
                        },
                        onDragCancel = {
                            offsetX = 0f
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            // Outgoing: allow only left swipe (negative)
                            // Incoming: allow only right swipe (positive)
                            val newOffset = if (isOutgoing) {
                                (offsetX + dragAmount).coerceIn(-maxSwipe, 0f)
                            } else {
                                (offsetX + dragAmount).coerceIn(0f, maxSwipe)
                            }
                            offsetX = newOffset
                        }
                    )
                }
        ) {
            MessageBubble(
                message = message,
                isOutgoing = isOutgoing,
                isPinned = isPinned,
                isHighlighted = isHighlighted,
                senderAvatarUrl = senderAvatarUrl,
                senderName = senderName, // Pass to Bubble
                currentUserId = currentUserId,
                onLongClick = onLongClick,
                onReplyClick = onReply,
                onReplyMessageClick = onReplyMessageClick,
                onDoubleTap = onDoubleTap,
                onReactionClick = onReactionClick
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    message: Message,
    isOutgoing: Boolean,
    isPinned: Boolean = false,
    isHighlighted: Boolean = false,
    senderAvatarUrl: String? = null,
    senderName: String? = null, // Added parameter
    currentUserId: String = "",
    onLongClick: () -> Unit,
    onReplyClick: () -> Unit,
    onReplyMessageClick: (String) -> Unit = {},
    onDoubleTap: () -> Unit = {},
    onReactionClick: (String) -> Unit = {}
) {
    val bubbleColor = if (isOutgoing) {
        ColorAccent
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
    }

    val textColor = if (isOutgoing) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    // Highlight background for scroll-to-pinned
    val highlightColor = if (isHighlighted) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    } else {
        Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(highlightColor)
            .padding(horizontal = 10.dp, vertical = 2.dp),
        horizontalArrangement = if (isOutgoing) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        // Avatar for incoming messages and Name
        if (!isOutgoing) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (senderName != null) {
                    Text(
                        text = senderName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 2.dp),
                        maxLines = 1,
                        fontSize = 10.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (senderAvatarUrl != null) {
                        AsyncImage(
                            model = senderAvatarUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (isOutgoing) Alignment.End else Alignment.Start
        ) {
            // Reply preview if this message is a reply
            message.replyToMessage?.let { reply ->
                // Spacer for visual separation if needed
                Box(
                    modifier = Modifier
                        .widthIn(max = 280.dp)
                        .padding(bottom = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                        .clickable { onReplyMessageClick(reply.id) }
                        .padding(8.dp)
                ) {
                   Column {
                        Text(
                            text = reply.senderName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = reply.content,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Message bubble - different layout for images
            if (message.type == MessageType.IMAGE && !message.isRevoked) {
                // Image message - no bubble background, show all images
                Column {
                    // Images grid/column
                    message.mediaUrls.forEachIndexed { index, url ->
                        if (index > 0) Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .widthIn(max = 280.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .combinedClickable(
                                onClick = { },
                                onLongClick = onLongClick
                            )
                    ) {
                        AsyncImage(
                            model = url,
                            contentDescription = "Image ${index + 1}",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 100.dp, max = 300.dp),
                            contentScale = ContentScale.Crop
                        )
                        
                        // Timestamp overlay on last image
                        if (index == message.mediaUrls.lastIndex) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(8.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black.copy(alpha = 0.55f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isPinned) {
                                        Text(
                                            text = "📌",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                    }
                                    Text(
                                        text = SimpleDateFormat("HH:mm", Locale.getDefault())
                                            .format(message.timestamp.toDate()),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium
                                    )
                                    if (isOutgoing) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        MessageStatusIcon(status = message.status, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Caption if any
                if (message.content.isNotBlank() && message.content != "Message was unsent") {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        } else {
            // Text and other message types - with bubble
            Box(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 20.dp,
                            topEnd = 20.dp,
                            bottomStart = if (isOutgoing) 20.dp else 6.dp,
                            bottomEnd = if (isOutgoing) 6.dp else 20.dp
                        )
                    )
                    .background(bubbleColor)
                    .combinedClickable(
                        onClick = { },
                        onLongClick = onLongClick
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Column {
                    when (message.type) {
                        MessageType.TEXT -> {
                            Text(
                                text = if (message.isRevoked) message.content else message.content,
                                color = textColor,
                                fontStyle = if (message.isRevoked) FontStyle.Italic else FontStyle.Normal,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        else -> {
                            Text(
                                text = message.getDisplayText(),
                                color = textColor,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Timestamp and status
                    Row(
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        // Pin indicator
                        if (isPinned) {
                            Text(
                                text = "📌",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        
                        Text(
                            text = SimpleDateFormat("HH:mm", Locale.getDefault())
                                .format(message.timestamp.toDate()),
                            style = MaterialTheme.typography.labelSmall,
                            color = textColor.copy(alpha = 0.7f)
                        )

                        if (isOutgoing) {
                            Spacer(modifier = Modifier.width(4.dp))
                            MessageStatusIcon(status = message.status, color = textColor)
                        }
                    }
                }
            }
        }
        
        // Reaction display below message
        if (message.reactions.isNotEmpty()) {
            com.example.minisocialnetworkapplication.ui.common.ReactionDisplay(
                reactions = message.reactions,
                currentUserId = currentUserId,
                onReactionClick = onReactionClick,
                modifier = Modifier.align(if (isOutgoing) Alignment.End else Alignment.Start)
            )
        }
        }
    }
}

@Composable
private fun MessageStatusIcon(status: MessageStatus, color: Color) {
    val text = when (status) {
        MessageStatus.PENDING, MessageStatus.SENDING -> "⏳"
        MessageStatus.SENT -> "✓"
        MessageStatus.DELIVERED -> "✓✓"
        MessageStatus.SEEN -> "✓✓"
        MessageStatus.FAILED -> "⚠️"
    }

    val statusColor = when (status) {
        MessageStatus.SEEN -> MaterialTheme.colorScheme.primary
        MessageStatus.FAILED -> MaterialTheme.colorScheme.error
        else -> color.copy(alpha = 0.7f)
    }

    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = statusColor
    )
}

/**
 * Messenger-style typing indicator bubble with animated dots
 */
@Composable
private fun TypingIndicatorBubble(
    avatarUrl: String?,
    modifier: Modifier = Modifier
) {
    val dotCount = 3
    val infiniteTransition = rememberInfiniteTransition(label = "typing")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (avatarUrl != null) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Surface(
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 20.dp, bottomStart = 6.dp),
            tonalElevation = 0.dp,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(dotCount) { index ->
                    val delay = index * 150
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(650, delayMillis = delay, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "dot$index"
                    )

                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha))
                    )
                }
            }
        }
    }
}


@Composable
private fun ReplyPreview(
    message: Message,
    onDismiss: () -> Unit
) {
    Surface(
        tonalElevation = 1.dp,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Replying to ${message.senderName}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = message.getDisplayText(),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Cancel reply")
            }
        }
    }
}


@Composable
private fun MessageInput(
    text: String,
    onTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onAttachClick: () -> Unit,
    isSending: Boolean
) {
    Surface(
        tonalElevation = 3.dp,
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Attach
            IconButton(
                onClick = onAttachClick,
                enabled = !isSending,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
            ) {
                Icon(
                    imageVector = Icons.Default.AttachFile,
                    contentDescription = "Attach image",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Input pill
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                placeholder = { Text("Message…") },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 46.dp),
                shape = RoundedCornerShape(24.dp),
                maxLines = 5,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                )
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Send
            IconButton(
                onClick = onSendClick,
                enabled = text.isNotBlank() && !isSending,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (text.isNotBlank() && !isSending)
                            ColorAccent
                        else
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                    )
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (text.isNotBlank())
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}


/**
 * Reaction bar popup with emoji selection
 */
@Composable
private fun ReactionBar(
    onReactionSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val emojis = listOf("❤️", "😂", "👍", "😮", "😢", "😡")

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        tonalElevation = 3.dp,
        shadowElevation = 12.dp,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            emojis.forEach { emoji ->
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
                        .clickable {
                            onReactionSelected(emoji)
                            onDismiss()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = emoji, style = MaterialTheme.typography.titleLarge)
                }
            }
        }
    }
}



