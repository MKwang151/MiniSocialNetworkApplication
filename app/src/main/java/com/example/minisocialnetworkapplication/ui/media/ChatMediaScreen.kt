package com.example.minisocialnetworkapplication.ui.media

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.minisocialnetworkapplication.core.domain.model.Message
import com.example.minisocialnetworkapplication.ui.gallery.ImageGalleryScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatMediaScreen(
    conversationId: String,
    viewModel: ChatMediaViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.getMediaMessages(conversationId).collectAsStateWithLifecycle()
    var selectedImageIndex by remember { mutableStateOf<Int?>(null) }

    // If viewing gallery, handle back press to close gallery
    if (selectedImageIndex != null) {
        BackHandler {
            selectedImageIndex = null
        }
    }

    Scaffold(
        topBar = {
            if (selectedImageIndex == null) {
                TopAppBar(
                    title = { Text("Media") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(if (selectedImageIndex == null) padding else androidx.compose.foundation.layout.PaddingValues(0.dp))
                .fillMaxSize()
        ) {
            when (val state = uiState) {
                is ChatMediaUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is ChatMediaUiState.Error -> {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is ChatMediaUiState.Success -> {
                    if (state.messages.isEmpty()) {
                        Text(
                            text = "No media found",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        val allMediaUrls = remember(state.messages) {
                            state.messages.flatMap { it.mediaUrls }
                        }

                        if (selectedImageIndex != null) {
                            ImageGalleryScreen(
                                imageUrls = allMediaUrls,
                                initialPage = selectedImageIndex ?: 0,
                                onNavigateBack = { selectedImageIndex = null }
                            )
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 100.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                itemsIndexed(allMediaUrls) { index, url ->
                                    Box(
                                        modifier = Modifier
                                            .aspectRatio(1f)
                                            .clickable { selectedImageIndex = index }
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        AsyncImage(
                                            model = url,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
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
