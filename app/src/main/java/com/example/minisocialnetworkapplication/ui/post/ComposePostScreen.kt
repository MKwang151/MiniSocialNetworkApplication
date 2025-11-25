package com.example.minisocialnetworkapplication.ui.post

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.minisocialnetworkapplication.core.util.Constants
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposePostScreen(
    onNavigateBack: (postCreated: Boolean) -> Unit,
    viewModel: ComposePostViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val postText by viewModel.postText.collectAsState()
    val selectedImages by viewModel.selectedImages.collectAsState()

    // Camera URI state
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }

    // Handle success state
    LaunchedEffect(uiState) {
        if (uiState is ComposePostUiState.Success) {
            onNavigateBack(true) // Post was created
        }
    }

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(
            maxItems = Constants.MAX_IMAGE_COUNT
        )
    ) { uris ->
        if (uris.isNotEmpty()) {
            Timber.d("Selected ${uris.size} images from gallery")
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

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            Timber.d("Camera photo captured successfully")
            cameraImageUri?.let { uri ->
                viewModel.addImageFromCamera(uri)
            }
        } else {
            Timber.w("Camera photo capture failed or cancelled")
        }
    }

    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Timber.d("Camera permission granted")
            // Create URI and launch camera
            cameraImageUri = createImageUri(context)
            cameraImageUri?.let { uri ->
                cameraLauncher.launch(uri)
            }
        } else {
            Timber.w("Camera permission denied")
            viewModel.showError("Camera permission is required to take photos")
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

            // Image picker and camera buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Gallery button
                OutlinedButton(
                    onClick = {
                        if (selectedImages.size < Constants.MAX_IMAGE_COUNT) {
                            imagePickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    },
                    enabled = selectedImages.size < Constants.MAX_IMAGE_COUNT,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "Gallery"
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Gallery")
                }

                // Camera button
                OutlinedButton(
                    onClick = {
                        if (selectedImages.size < Constants.MAX_IMAGE_COUNT) {
                            // Check camera permission
                            if (context.checkSelfPermission(android.Manifest.permission.CAMERA) ==
                                android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                // Permission already granted, launch camera
                                cameraImageUri = createImageUri(context)
                                cameraImageUri?.let { uri ->
                                    cameraLauncher.launch(uri)
                                }
                            } else {
                                // Request permission
                                cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                            }
                        }
                    },
                    enabled = selectedImages.size < Constants.MAX_IMAGE_COUNT,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Camera"
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Camera")
                }
            }

            // Photo count indicator
            if (selectedImages.isNotEmpty()) {
                Text(
                    text = "Photos: ${selectedImages.size}/${Constants.MAX_IMAGE_COUNT}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
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
                val uploadingState = uiState as ComposePostUiState.Uploading
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
                            text = uploadingState.message,
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

/**
 * Create a URI for camera capture using FileProvider
 */
private fun createImageUri(context: Context): Uri? {
    return try {
        // Create camera directory if it doesn't exist
        val cameraDir = File(context.cacheDir, "camera")
        if (!cameraDir.exists()) {
            cameraDir.mkdirs()
        }

        // Create unique file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFile = File(cameraDir, "IMG_${timeStamp}.jpg")

        // Get URI using FileProvider
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )
    } catch (e: Exception) {
        Timber.e(e, "Error creating camera image URI")
        null
    }
}

