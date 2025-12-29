package com.example.minisocialnetworkapplication.ui.media

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.minisocialnetworkapplication.core.domain.model.Message
import com.example.minisocialnetworkapplication.core.domain.model.MessageType
import com.example.minisocialnetworkapplication.ui.gallery.ImageGalleryScreen
import com.example.minisocialnetworkapplication.ui.gallery.VideoPlayerScreen

// Sealed class to distinguish between image and video media items
sealed class MediaItem {
    abstract val url: String
    abstract val messageId: String
    
    data class Image(override val url: String, override val messageId: String) : MediaItem()
    data class Video(override val url: String, override val messageId: String, val duration: Int?) : MediaItem()
}

// Modern color palette
private val ColorAccent = Color(0xFF667EEA)
private val GradientPrimary = listOf(Color(0xFF667EEA), Color(0xFF764BA2))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatMediaScreen(
    conversationId: String,
    viewModel: ChatMediaViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.getMediaMessages(conversationId).collectAsStateWithLifecycle()
    var selectedImageIndex by remember { mutableStateOf<Int?>(null) }
    var selectedVideoUrl by remember { mutableStateOf<String?>(null) }

    // If viewing gallery or video, handle back press to close
    if (selectedImageIndex != null || selectedVideoUrl != null) {
        BackHandler {
            selectedImageIndex = null
            selectedVideoUrl = null
        }
    }

    Scaffold(
        topBar = {
            if (selectedImageIndex == null && selectedVideoUrl == null) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Media",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            )
                            Text(
                                text = "Photos and videos",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    )
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(if (selectedImageIndex == null && selectedVideoUrl == null) padding else PaddingValues(0.dp))
                .fillMaxSize()
                .background(
                    if (selectedImageIndex == null && selectedVideoUrl == null) {
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                            )
                        )
                    } else {
                        Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                    }
                )
        ) {
            when (val state = uiState) {
                is ChatMediaUiState.Loading -> {
                    ModernLoadingView()
                }
                is ChatMediaUiState.Error -> {
                    ModernErrorView(message = state.message)
                }
                is ChatMediaUiState.Success -> {
                    if (state.messages.isEmpty()) {
                        ModernEmptyMediaView()
                    } else {
                        // Convert messages to MediaItem list
                        val allMediaItems = remember(state.messages) {
                            state.messages.flatMap { message ->
                                message.mediaUrls.map { url ->
                                    if (message.type == MessageType.VIDEO) {
                                        MediaItem.Video(url, message.id, message.duration)
                                    } else {
                                        MediaItem.Image(url, message.id)
                                    }
                                }
                            }
                        }
                        
                        // Get image-only URLs for gallery
                        val imageUrls = remember(allMediaItems) {
                            allMediaItems.filterIsInstance<MediaItem.Image>().map { it.url }
                        }
                        
                        val photoCount = allMediaItems.count { it is MediaItem.Image }
                        val videoCount = allMediaItems.count { it is MediaItem.Video }

                        if (selectedVideoUrl != null) {
                            VideoPlayerScreen(
                                videoUrl = selectedVideoUrl!!,
                                onNavigateBack = { selectedVideoUrl = null }
                            )
                        } else if (selectedImageIndex != null) {
                            ImageGalleryScreen(
                                imageUrls = imageUrls,
                                initialPage = selectedImageIndex ?: 0,
                                onNavigateBack = { selectedImageIndex = null }
                            )
                        } else {
                            Column {
                                // Stats card
                                MediaStatsCard(
                                    photoCount = photoCount,
                                    videoCount = videoCount,
                                    modifier = Modifier.padding(16.dp)
                                )
                                
                                // Media grid
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(3),
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    verticalArrangement = Arrangement.spacedBy(3.dp),
                                    contentPadding = PaddingValues(horizontal = 3.dp, vertical = 3.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    itemsIndexed(allMediaItems) { index, mediaItem ->
                                        ModernMediaGridItem(
                                            mediaItem = mediaItem,
                                            onClick = { 
                                                when (mediaItem) {
                                                    is MediaItem.Video -> selectedVideoUrl = mediaItem.url
                                                    is MediaItem.Image -> {
                                                        // Find index in imageUrls list
                                                        val imageIndex = imageUrls.indexOf(mediaItem.url)
                                                        if (imageIndex >= 0) {
                                                            selectedImageIndex = imageIndex
                                                        }
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaStatsCard(
    photoCount: Int,
    videoCount: Int = 0,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(14.dp),
                color = ColorAccent.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = ColorAccent
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(14.dp))
            
            Column {
                val statsText = buildString {
                    if (photoCount > 0) append("$photoCount Photos")
                    if (photoCount > 0 && videoCount > 0) append(" • ")
                    if (videoCount > 0) append("$videoCount Videos")
                }
                Text(
                    text = statsText,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Tap any media to view",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ModernMediaGridItem(
    mediaItem: MediaItem,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        AsyncImage(
            model = mediaItem.url,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        
        // Show play icon overlay for videos
        if (mediaItem is MediaItem.Video) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.6f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play video",
                            modifier = Modifier.size(24.dp),
                            tint = Color.White
                        )
                    }
                }
            }
            
            // Show duration if available
            mediaItem.duration?.let { duration ->
                val minutes = duration / 60
                val seconds = duration % 60
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .background(
                            Color.Black.copy(alpha = 0.7f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = String.format("%d:%02d", minutes, seconds),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ModernLoadingView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                strokeWidth = 3.dp,
                color = ColorAccent
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Loading media...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ModernEmptyMediaView() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Gradient icon background
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                ColorAccent.copy(alpha = 0.12f),
                                Color(0xFF764BA2).copy(alpha = 0.12f)
                            )
                        ),
                        RoundedCornerShape(28.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Photo,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = ColorAccent
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "No media yet",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Photos and videos shared in this\nchat will appear here",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
        }
    }
}

@Composable
private fun ModernErrorView(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
