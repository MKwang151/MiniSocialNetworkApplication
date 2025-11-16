package com.example.minisocialnetworkapplication.ui.post

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.minisocialnetworkapplication.core.util.Constants
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposePostScreen(
    onNavigateBack: (postCreated: Boolean) -> Unit,
    viewModel: ComposePostViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val postText by viewModel.postText.collectAsState()
    val selectedImages by viewModel.selectedImages.collectAsState()

    // Handle success state
    LaunchedEffect(uiState) {
        if (uiState is ComposePostUiState.Success) {
            onNavigateBack(true) // Post was created
        }
    }

    // Image picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(
            maxItems = Constants.MAX_IMAGE_COUNT
        )
    ) { uris ->
        if (uris.isNotEmpty()) {
            Timber.d("Selected ${uris.size} images")
            // Filter to respect max count based on already selected images
            val availableSlots = Constants.MAX_IMAGE_COUNT - selectedImages.size
            val urisToAdd = if (uris.size > availableSlots) {
                Timber.w("Too many images selected, taking first $availableSlots")
                uris.take(availableSlots)
            } else {
                uris
            }

            if (urisToAdd.isNotEmpty()) {
                viewModel.addImages(urisToAdd)
            }
        }
    }

    // Show error snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState) {
        if (uiState is ComposePostUiState.Error) {
            snackbarHostState.showSnackbar(
                message = (uiState as ComposePostUiState.Error).message,
                duration = SnackbarDuration.Short
            )
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Post") },
                navigationIcon = {
                    IconButton(onClick = { onNavigateBack(false) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = { viewModel.createPost() },
                        enabled = uiState !is ComposePostUiState.Uploading &&
                                (postText.isNotBlank() || selectedImages.isNotEmpty()),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        if (uiState is ComposePostUiState.Uploading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Post")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Text input
            OutlinedTextField(
                value = postText,
                onValueChange = { viewModel.updatePostText(it) },
                placeholder = { Text("What's on your mind?") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 150.dp),
                maxLines = 10,
                supportingText = {
                    Text(
                        text = "${postText.length}/${Constants.MAX_POST_TEXT_LENGTH}",
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                isError = postText.length > Constants.MAX_POST_TEXT_LENGTH
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Image picker button
            OutlinedButton(
                onClick = {
                    if (selectedImages.size < Constants.MAX_IMAGE_COUNT) {
                        imagePickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                },
                enabled = selectedImages.size < Constants.MAX_IMAGE_COUNT,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = "Add images"
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (selectedImages.isEmpty()) {
                        "Add Photos (up to ${Constants.MAX_IMAGE_COUNT})"
                    } else {
                        "Add More Photos (${selectedImages.size}/${Constants.MAX_IMAGE_COUNT})"
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Selected images preview
            if (selectedImages.isNotEmpty()) {
                Text(
                    text = "Selected Images:",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(selectedImages.size) { index ->
                        val uri = selectedImages[index]
                        SelectedImageItem(
                            uri = uri,
                            onRemove = { viewModel.removeImage(uri) }
                        )
                    }
                }
            }

            // Uploading progress
            if (uiState is ComposePostUiState.Uploading) {
                Spacer(modifier = Modifier.height(24.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Uploading your post...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SelectedImageItem(
    uri: Uri,
    onRemove: () -> Unit
) {
    Box(
        modifier = Modifier.size(120.dp)
    ) {
        AsyncImage(
            model = uri,
            contentDescription = "Selected image",
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )

        // Remove button
        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(28.dp)
                .background(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(14.dp)
                )
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove image",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

