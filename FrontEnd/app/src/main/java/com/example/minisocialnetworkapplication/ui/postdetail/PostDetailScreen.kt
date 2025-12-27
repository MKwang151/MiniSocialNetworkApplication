package com.example.minisocialnetworkapplication.ui.postdetail

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.minisocialnetworkapplication.core.domain.model.Comment
import com.example.minisocialnetworkapplication.core.domain.model.Post
import com.example.minisocialnetworkapplication.ui.components.PostCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToProfile: (String) -> Unit,
    onNavigateToImageGallery: (Int) -> Unit = {},
    onPostDeleted: () -> Unit = {},
    viewModel: PostDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val commentText by viewModel.commentText.collectAsState()
    val isAddingComment by viewModel.isAddingComment.collectAsState()
    val deletionSuccess by viewModel.deletionSuccess.collectAsState()
    val replyToComment by viewModel.replyToComment.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditPostDialog by remember { mutableStateOf(false) }
    var editPostText by remember { mutableStateOf("") }

    // Navigate back when deletion completes successfully
    LaunchedEffect(deletionSuccess) {
        if (deletionSuccess) {
            onPostDeleted()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(
                            "Post Detail",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    )
                )
        ) {
            when (val state = uiState) {
                is PostDetailUiState.Loading -> {
                    ModernLoadingView(modifier = Modifier.padding(paddingValues))
                }
                is PostDetailUiState.Success -> {
                    PostDetailContent(
                        post = state.post,
                        comments = state.comments,
                        commentText = commentText,
                        isAddingComment = isAddingComment,
                        replyToComment = replyToComment,
                        onCommentTextChange = viewModel::updateCommentText,
                        onSendComment = viewModel::addComment,
                        onLikeClicked = viewModel::toggleLike,
                        onAuthorClicked = onNavigateToProfile,
                        onImageClicked = onNavigateToImageGallery,
                        onEditPost = { text ->
                            editPostText = text
                            showEditPostDialog = true
                        },
                        onDeletePost = { post ->
                            showDeleteDialog = true
                        },
                        onDeleteComment = { comment ->
                            viewModel.deleteComment(comment.id)
                        },
                        onEditComment = { comment, newText ->
                            viewModel.updateComment(comment.id, newText)
                        },
                        onReplyClick = viewModel::setReplyToComment,
                        onClearReply = viewModel::clearReply,
                        onReactionClick = { comment, emoji ->
                            viewModel.toggleCommentReaction(comment.id, emoji)
                        },
                        modifier = Modifier.padding(paddingValues)
                    )
                }
                is PostDetailUiState.Error -> {
                    ModernErrorView(
                        message = state.message,
                        onRetryClick = viewModel::refresh,
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        com.example.minisocialnetworkapplication.ui.components.DeletePostDialog(
            onConfirm = {
                viewModel.deletePost()
                showDeleteDialog = false
            },
            onDismiss = {
                showDeleteDialog = false
            }
        )
    }

    // Edit post dialog
    if (showEditPostDialog) {
        ModernEditTextDialog(
            title = "Edit Post",
            initialText = editPostText,
            onConfirm = { newText ->
                viewModel.updatePost(newText)
                showEditPostDialog = false
            },
            onDismiss = {
                showEditPostDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailContent(
    post: Post,
    comments: List<Comment>,
    commentText: String,
    isAddingComment: Boolean,
    replyToComment: Comment? = null,
    onCommentTextChange: (String) -> Unit,
    onSendComment: () -> Unit,
    onLikeClicked: () -> Unit,
    onAuthorClicked: (String) -> Unit,
    onImageClicked: (Int) -> Unit = {},
    onEditPost: (String) -> Unit = {},
    onDeletePost: (Post) -> Unit = {},
    onDeleteComment: (Comment) -> Unit = {},
    onEditComment: (Comment, String) -> Unit = { _, _ -> },
    onReplyClick: (Comment) -> Unit = {},
    onClearReply: () -> Unit = {},
    onReactionClick: (Comment, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var commentToDelete by remember { mutableStateOf<Comment?>(null) }
    var showEditCommentDialog by remember { mutableStateOf(false) }
    var commentToEdit by remember { mutableStateOf<Comment?>(null) }
    
    // State for long-press bottom sheet
    var selectedComment by remember { mutableStateOf<Comment?>(null) }
    val sheetState = rememberModalBottomSheetState()
    
    // Bottom Sheet for reactions
    if (selectedComment != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedComment = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Reaction Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val emojis = listOf("❤️", "😂", "👍", "😮", "😢", "😡")
                    emojis.forEach { emoji ->
                        Text(
                            text = emoji,
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier
                                .clickable {
                                    selectedComment?.let { comment ->
                                        onReactionClick(comment, emoji)
                                    }
                                    selectedComment = null
                                }
                                .padding(8.dp)
                        )
                    }
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                // Reply option
                DropdownMenuItem(
                    text = { Text("Reply") },
                    onClick = {
                        selectedComment?.let { onReplyClick(it) }
                        selectedComment = null
                    },
                    leadingIcon = {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                    }
                )
                
                // Edit/Delete options if authorized
                val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                val isAuthor = selectedComment?.authorId == currentUserId
                val isPostAuthor = post.authorId == currentUserId
                
                if (isAuthor) {
                    DropdownMenuItem(
                        text = { Text("Edit Comment") },
                        onClick = {
                            commentToEdit = selectedComment
                            showEditCommentDialog = true
                            selectedComment = null
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                    )
                }
                
                if (isAuthor || isPostAuthor) {
                    DropdownMenuItem(
                        text = { Text("Delete Comment", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            commentToDelete = selectedComment
                            showDeleteDialog = true
                            selectedComment = null
                        },
                        leadingIcon = { 
                            Icon(
                                Icons.Default.Delete, 
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            ) 
                        }
                    )
                }
            }
        }
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Comments list (weight 1 to take remaining space)
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // Post card at top
            item {
                PostCard(
                    post = post,
                    onLikeClicked = { onLikeClicked() },
                    onCommentClicked = {},
                    onPostClicked = {},
                    onAuthorClicked = onAuthorClicked,
                    onImageClicked = onImageClicked,
                    onEditClicked = { onEditPost(post.text) },
                    onDeleteClicked = {
                        onDeletePost(post)
                    },
                    isOptimisticallyLiked = post.likedByMe,
                    showMenuButton = true
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Comments header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Comments",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${comments.size}",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Comments
            if (comments.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "💬",
                                style = MaterialTheme.typography.displayMedium
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No comments yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Be the first to comment!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(
                    items = comments,
                    key = { it.id }
                ) { comment ->
                    CommentItem(
                        comment = comment,
                        onAuthorClicked = onAuthorClicked,
                        onDeleteClicked = {
                            commentToDelete = it
                            showDeleteDialog = true
                        },
                        onEditClicked = { comment ->
                            commentToEdit = comment
                            showEditCommentDialog = true
                        },
                        onReplyClicked = onReplyClick,
                        onReactionClicked = onReactionClick,
                        onLongClick = { selectedComment = it },
                        postAuthorId = post.authorId
                    )
                }
            }
        }

        // Comment input at bottom
        ModernCommentInputBar(
            text = commentText,
            onTextChange = onCommentTextChange,
            onSendClick = onSendComment,
            isLoading = isAddingComment,
            replyToComment = replyToComment,
            onClearReply = onClearReply,
            modifier = Modifier.fillMaxWidth()
        )
    }

    // Delete comment confirmation dialog
    if (showDeleteDialog && commentToDelete != null) {
        com.example.minisocialnetworkapplication.ui.components.DeleteCommentDialog(
            onConfirm = {
                commentToDelete?.let { comment ->
                    onDeleteComment(comment)
                }
                showDeleteDialog = false
                commentToDelete = null
            },
            onDismiss = {
                showDeleteDialog = false
                commentToDelete = null
            }
        )
    }

    // Edit comment dialog
    if (showEditCommentDialog && commentToEdit != null) {
        ModernEditTextDialog(
            title = "Edit Comment",
            initialText = commentToEdit?.text ?: "",
            onConfirm = { newText ->
                commentToEdit?.let { comment ->
                    onEditComment(comment, newText)
                }
                showEditCommentDialog = false
                commentToEdit = null
            },
            onDismiss = {
                showEditCommentDialog = false
                commentToEdit = null
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CommentItem(
    comment: Comment,
    onAuthorClicked: (String) -> Unit,
    onDeleteClicked: (Comment) -> Unit = {},
    onEditClicked: (Comment) -> Unit = {},
    onReplyClicked: (Comment) -> Unit,
    onReactionClicked: (Comment, String) -> Unit,
    onLongClick: (Comment) -> Unit,
    postAuthorId: String = "",
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid

    val canDelete = currentUserId != null &&
        (comment.authorId == currentUserId || postAuthorId == currentUserId)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = { onLongClick(comment) }
            )
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        // Avatar with gradient fallback
        Surface(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .clickable { onAuthorClicked(comment.authorId) },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            if (!comment.authorAvatarUrl.isNullOrEmpty()) {
                coil.compose.AsyncImage(
                    model = comment.authorAvatarUrl,
                    contentDescription = "Avatar of ${comment.authorName}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF667EEA), Color(0xFF764BA2))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = comment.authorName.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            // Author name - clickable
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = comment.authorName,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.clickable { onAuthorClicked(comment.authorId) }
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Timestamp
                Text(
                    text = com.example.minisocialnetworkapplication.core.util.DateTimeUtil.getRelativeTime(
                        comment.createdAt
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Reply indicator
            if (comment.replyToAuthorName != null) {
                Text(
                    text = "↳ Replying to ${comment.replyToAuthorName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Comment text
            Text(
                text = comment.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            // Reactions display
            if (comment.reactions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                com.example.minisocialnetworkapplication.ui.common.ReactionDisplay(
                    reactions = comment.reactions,
                    currentUserId = currentUserId,
                    onReactionClick = { emoji -> onReactionClicked(comment, emoji) }
                )
            }
        }

        // Menu button
        if (canDelete) {
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    if (comment.authorId == currentUserId) {
                        DropdownMenuItem(
                            text = { Text("Edit Comment") },
                            onClick = {
                                showMenu = false
                                onEditClicked(comment)
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit"
                                )
                            }
                        )
                    }

                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "Delete Comment",
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            showMenu = false
                            onDeleteClicked(comment)
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ModernCommentInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
    isLoading: Boolean,
    replyToComment: Comment? = null,
    onClearReply: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        tonalElevation = 4.dp,
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column {
            // Reply indicator
            if (replyToComment != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "↳",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Replying to ${replyToComment.authorName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    IconButton(
                        onClick = onClearReply,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel reply",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Modern text field
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    TextField(
                        value = text,
                        onValueChange = onTextChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { 
                            Text(
                                if (replyToComment != null) "Write a reply..." else "Write a comment...",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            ) 
                        },
                        enabled = !isLoading,
                        maxLines = 3,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Send button
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    color = if (text.isNotBlank() && !isLoading) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.surfaceVariant
                ) {
                    IconButton(
                        onClick = onSendClick,
                        enabled = text.isNotBlank() && !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                modifier = Modifier.size(22.dp),
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
    }
}

@Composable
fun ModernLoadingView(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                strokeWidth = 3.dp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Loading post...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ModernErrorView(
    message: String,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            // Error emoji
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .background(
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(24.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "😕",
                    style = MaterialTheme.typography.displayMedium
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Oops!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onRetryClick,
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Retry", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun ModernEditTextDialog(
    title: String,
    initialText: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initialText) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                title, 
                fontWeight = FontWeight.Bold
            ) 
        },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                placeholder = { Text("Enter text...") },
                maxLines = 5,
                shape = RoundedCornerShape(12.dp)
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(text) },
                enabled = text.isNotBlank(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Keep these for backward compatibility
@Composable
fun LoadingView(modifier: Modifier = Modifier) = ModernLoadingView(modifier)

@Composable
fun ErrorView(
    message: String,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) = ModernErrorView(message, onRetryClick, modifier)

@Composable
fun EditTextDialog(
    title: String,
    initialText: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) = ModernEditTextDialog(title, initialText, onConfirm, onDismiss)
