package com.example.minisocialnetworkapplication.ui.postdetail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
                title = { Text("Post Detail") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is PostDetailUiState.Loading -> {
                LoadingView(modifier = Modifier.padding(paddingValues))
            }
            is PostDetailUiState.Success -> {
                PostDetailContent(
                    post = state.post,
                    comments = state.comments,
                    commentText = commentText,
                    isAddingComment = isAddingComment,
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
                    modifier = Modifier.padding(paddingValues)
                )
            }
            is PostDetailUiState.Error -> {
                ErrorView(
                    message = state.message,
                    onRetryClick = viewModel::refresh,
                    modifier = Modifier.padding(paddingValues)
                )
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
        EditTextDialog(
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

@Composable
fun PostDetailContent(
    post: Post,
    comments: List<Comment>,
    commentText: String,
    isAddingComment: Boolean,
    onCommentTextChange: (String) -> Unit,
    onSendComment: () -> Unit,
    onLikeClicked: () -> Unit,
    onAuthorClicked: (String) -> Unit,
    onImageClicked: (Int) -> Unit = {},
    onEditPost: (String) -> Unit = {},
    onDeletePost: (Post) -> Unit = {},
    onDeleteComment: (Comment) -> Unit = {},
    onEditComment: (Comment, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var commentToDelete by remember { mutableStateOf<Comment?>(null) }
    var showEditCommentDialog by remember { mutableStateOf(false) }
    var commentToEdit by remember { mutableStateOf<Comment?>(null) }

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
                        // Set flag to show delete dialog in PostDetailScreen
                        onDeletePost(post)
                    },
                    isOptimisticallyLiked = post.likedByMe,
                    showMenuButton = true  // Show menu to allow edit and delete
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Comments header
                Text(
                    text = "Comments (${comments.size})",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
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
                        Text(
                            text = "No comments yet.\nBe the first to comment!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
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
                        postAuthorId = post.authorId
                    )
                }
            }
        }

        // Comment input at bottom
        CommentInputBar(
            text = commentText,
            onTextChange = onCommentTextChange,
            onSendClick = onSendComment,
            isLoading = isAddingComment
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
        EditTextDialog(
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

@Composable
fun CommentItem(
    comment: Comment,
    onAuthorClicked: (String) -> Unit,
    onDeleteClicked: (Comment) -> Unit = {},
    onEditClicked: (Comment) -> Unit = {},
    postAuthorId: String = "",
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid

    // Check if user can delete: comment owner OR post owner
    val canDelete = currentUserId != null &&
        (comment.authorId == currentUserId || postAuthorId == currentUserId)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Avatar placeholder - clickable
        Surface(
            modifier = Modifier
                .size(40.dp)
                .clickable { onAuthorClicked(comment.authorId) },
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = comment.authorName.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            // Author name - clickable
            Text(
                text = comment.authorName,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onAuthorClicked(comment.authorId) }
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Comment text
            Text(
                text = comment.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Timestamp
            Text(
                text = com.example.minisocialnetworkapplication.core.util.DateTimeUtil.getRelativeTime(
                    comment.createdAt
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Menu button (only show if user can delete)
        if (canDelete) {
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options",
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    // Show Edit only for comment owner
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

                    // Show Delete for comment owner OR post owner
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
fun CommentInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Write a comment...") },
                enabled = !isLoading,
                maxLines = 3,
                shape = MaterialTheme.shapes.medium
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onSendClick,
                enabled = text.isNotBlank() && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (text.isNotBlank()) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun LoadingView(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun ErrorView(
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
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetryClick) {
                Text("Retry")
            }
        }
    }
}

@Composable
fun EditTextDialog(
    title: String,
    initialText: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initialText) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                placeholder = { Text("Enter text...") },
                maxLines = 5
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text) },
                enabled = text.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
