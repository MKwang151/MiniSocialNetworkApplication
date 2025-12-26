package com.example.minisocialnetworkapplication.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.minisocialnetworkapplication.core.domain.model.Post
import com.example.minisocialnetworkapplication.core.util.DateTimeUtil

// Modern color palette
private val ColorAccent = Color(0xFF667EEA)
private val ColorLike = Color(0xFFFF416C)
private val GradientPrimary = listOf(Color(0xFF667EEA), Color(0xFF764BA2))

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
    onReportClicked: (Post) -> Unit = {},
    modifier: Modifier = Modifier,
    isOptimisticallyLiked: Boolean = post.likedByMe,
    showMenuButton: Boolean = true
) {
    var showMenu by remember { mutableStateOf(false) }
    val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .clickable { onPostClicked(post) },
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header: Avatar, Name, Time, Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isGroupPost = post.groupId != null
                
                if (isGroupPost) {
                    // Group post: Group avatar large with user avatar small overlay
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clickable { onAuthorClicked(post.authorId) }
                    ) {
                        // Group avatar with gradient fallback
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            shape = RoundedCornerShape(12.dp),
                            color = Color.Transparent
                        ) {
                            if (!post.groupAvatarUrl.isNullOrEmpty()) {
                                AsyncImage(
                                    model = post.groupAvatarUrl,
                                    contentDescription = "Group avatar",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.linearGradient(GradientPrimary),
                                            RoundedCornerShape(12.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = (post.groupName ?: "G").take(1).uppercase(),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        
                        // User avatar overlay
                        Surface(
                            modifier = Modifier
                                .size(24.dp)
                                .align(Alignment.BottomEnd)
                                .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                            shape = CircleShape,
                            color = Color.Transparent
                        ) {
                            if (!post.authorAvatarUrl.isNullOrEmpty()) {
                                AsyncImage(
                                    model = post.authorAvatarUrl,
                                    contentDescription = "Author avatar",
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
                                        text = post.authorName.take(1).uppercase(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Regular post: User avatar with gradient fallback
                    Surface(
                        modifier = Modifier
                            .size(44.dp)
                            .clickable { onAuthorClicked(post.authorId) },
                        shape = CircleShape,
                        color = Color.Transparent
                    ) {
                        if (!post.authorAvatarUrl.isNullOrEmpty()) {
                            AsyncImage(
                                model = post.authorAvatarUrl,
                                contentDescription = "Author avatar",
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
                                    text = post.authorName.take(1).uppercase(),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Name and Time section
                Column(modifier = Modifier.weight(1f)) {
                    if (isGroupPost) {
                        Text(
                            text = post.groupName ?: "Group",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = post.authorName,
                                style = MaterialTheme.typography.bodySmall,
                                color = ColorAccent,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = " · ",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = DateTimeUtil.formatRelativeTime(post.createdAt),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
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
                }

                // Menu button
                if (showMenuButton) {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More options",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            if (post.authorId == currentUserId) {
                                DropdownMenuItem(
                                    text = { Text("Edit Post") },
                                    onClick = {
                                        showMenu = false
                                        onEditClicked(post)
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                                    }
                                )

                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "Delete Post",
                                            color = ColorLike
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        onDeleteClicked(post)
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = ColorLike
                                        )
                                    }
                                )
                            } else {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "Report Post",
                                            color = ColorLike
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        onReportClicked(post)
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Warning,
                                            contentDescription = "Report",
                                            tint = ColorLike
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
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 22.sp
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

            // Action Buttons Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Like Button
                Surface(
                    color = if (isOptimisticallyLiked) ColorLike.copy(alpha = 0.1f) 
                           else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.clickable { onLikeClicked(post) }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        AnimatedLikeButton(
                            isLiked = isOptimisticallyLiked,
                            onClick = { onLikeClicked(post) },
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${post.likeCount}",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isOptimisticallyLiked) ColorLike 
                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Comment Button
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.clickable { onCommentClicked(post) }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
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
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
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
            AsyncImage(
                model = imageUrls[0],
                contentDescription = "Post image",
                modifier = modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onImageClicked(0) },
                contentScale = ContentScale.Crop
            )
        }
        imageUrls.size == 2 -> {
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
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onImageClicked(index) },
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
        imageUrls.size >= 3 -> {
            Column(
                modifier = modifier,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AsyncImage(
                    model = imageUrls[0],
                    contentDescription = "Post image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onImageClicked(0) },
                    contentScale = ContentScale.Crop
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    AsyncImage(
                        model = imageUrls[1],
                        contentDescription = "Post image",
                        modifier = Modifier
                            .weight(1f)
                            .height(120.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onImageClicked(1) },
                        contentScale = ContentScale.Crop
                    )

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
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onImageClicked(2) },
                            contentScale = ContentScale.Crop
                        )

                        if (remainingCount > 0) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black.copy(alpha = 0.55f))
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
