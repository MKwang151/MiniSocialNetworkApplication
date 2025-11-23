package com.example.minisocialnetworkapplication.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.minisocialnetworkapplication.core.domain.model.Post
import com.example.minisocialnetworkapplication.core.util.DateTimeUtil

@Composable
fun PostCard(
    post: Post,
    onLikeClicked: (Post) -> Unit,
    onCommentClicked: (Post) -> Unit,
    onPostClicked: (Post) -> Unit,
    onAuthorClicked: (String) -> Unit,
    onImageClicked: (Int) -> Unit = {},
    onDeleteClicked: (Post) -> Unit = {},
    onEditClicked: (Post) -> Unit = {},
    modifier: Modifier = Modifier,
    isOptimisticallyLiked: Boolean = post.likedByMe,
    showMenuButton: Boolean = true
) {
    var showMenu by remember { mutableStateOf(false) }
    val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onPostClicked(post) }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header: Avatar, Name, Time, Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                AsyncImage(
                    model = post.authorAvatarUrl ?: "https://ui-avatars.com/api/?name=${post.authorName}",
                    contentDescription = "Author avatar",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable { onAuthorClicked(post.authorId) }
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Name and Time
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = post.authorName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = DateTimeUtil.formatRelativeTime(post.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Menu button (only show if showMenuButton is true)
                if (showMenuButton) {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options"
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            // Show Edit and Delete options only for post owner
                            if (post.authorId == currentUserId) {
                                DropdownMenuItem(
                                    text = { Text("Edit Post") },
                                    onClick = {
                                        showMenu = false
                                        onEditClicked(post)
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit"
                                        )
                                    }
                                )

                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "Delete Post",
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        onDeleteClicked(post)
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

            Spacer(modifier = Modifier.height(12.dp))

            // Post Text
            if (post.text.isNotBlank()) {
                Text(
                    text = post.text,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 10,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Post Images
            if (post.mediaUrls.isNotEmpty()) {
                PostImages(
                    imageUrls = post.mediaUrls,
                    onImageClicked = onImageClicked,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Divider
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons: Like, Comment
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Like Button with Animation
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onLikeClicked(post) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    AnimatedLikeButton(
                        isLiked = isOptimisticallyLiked,
                        onClick = { onLikeClicked(post) },
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${post.likeCount}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Comment Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onCommentClicked(post) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ChatBubbleOutline,
                        contentDescription = "Comment",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${post.commentCount}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun PostImages(
    imageUrls: List<String>,
    onImageClicked: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val remainingCount = if (imageUrls.size > 3) imageUrls.size - 3 else 0

    when {
        imageUrls.size == 1 -> {
            // Single image - full width
            AsyncImage(
                model = imageUrls[0],
                contentDescription = "Post image",
                modifier = modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onImageClicked(0) },
                contentScale = ContentScale.Crop
            )
        }
        imageUrls.size == 2 -> {
            // Two images - side by side
            Row(
                modifier = modifier,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                imageUrls.take(2).forEachIndexed { index, url ->
                    AsyncImage(
                        model = url,
                        contentDescription = "Post image",
                        modifier = Modifier
                            .weight(1f)
                            .height(180.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onImageClicked(index) },
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
        imageUrls.size >= 3 -> {
            // Three+ images - grid layout with overlay on 3rd image
            Column(
                modifier = modifier,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // First image - full width
                AsyncImage(
                    model = imageUrls[0],
                    contentDescription = "Post image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onImageClicked(0) },
                    contentScale = ContentScale.Crop
                )

                // Bottom row - 2 images
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Second image
                    AsyncImage(
                        model = imageUrls[1],
                        contentDescription = "Post image",
                        modifier = Modifier
                            .weight(1f)
                            .height(120.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onImageClicked(1) },
                        contentScale = ContentScale.Crop
                    )

                    // Third image with overlay if more than 3 images
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(120.dp)
                    ) {
                        AsyncImage(
                            model = imageUrls[2],
                            contentDescription = "Post image",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onImageClicked(2) },
                            contentScale = ContentScale.Crop
                        )

                        // Overlay for "+N more" if more than 3 images
                        if (remainingCount > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .clickable { onImageClicked(2) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "+$remainingCount",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


