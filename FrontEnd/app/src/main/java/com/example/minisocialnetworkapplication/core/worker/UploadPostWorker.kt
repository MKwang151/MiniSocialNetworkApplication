package com.example.minisocialnetworkapplication.core.worker

import android.content.Context
import android.net.Uri
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.minisocialnetworkapplication.core.util.Constants
import com.example.minisocialnetworkapplication.core.util.ImageCompressor
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.io.File
import java.util.UUID

@HiltWorker
class UploadPostWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val storage: FirebaseStorage,
    private val postRepository: com.example.minisocialnetworkapplication.core.domain.repository.PostRepository,
    private val auth: FirebaseAuth
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val postId = inputData.getString("POST_ID") ?: return Result.failure()
        val imageFilePaths = inputData.getStringArray("IMAGE_URIS") ?: emptyArray()
        val userId = inputData.getString("USER_ID") ?: auth.currentUser?.uid

        if (userId == null) {
            Timber.e("User not authenticated")
            return Result.failure()
        }

        if (imageFilePaths.isEmpty()) {
            Timber.d("No images to upload for post: $postId")
            return Result.success()
        }

        return try {
            Timber.d("Starting background image upload: postId=$postId, images=${imageFilePaths.size}")

            // Upload images from cache files in parallel for better performance
            val imageUrls = coroutineScope {
                imageFilePaths.map { filePath ->
                    async {
                        try {
                            uploadImageFromFile(filePath, userId)
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to upload image: $filePath")
                            null
                        }
                    }
                }.awaitAll().filterNotNull()
            }

            if (imageUrls.isEmpty()) {
                throw Exception("All image uploads failed")
            }

            Timber.d("Images uploaded: ${imageUrls.size}/${imageFilePaths.size} (parallel upload)")

            // Update post with uploaded image URLs
            when (val result = postRepository.updatePostImages(postId, imageUrls)) {
                is com.example.minisocialnetworkapplication.core.util.Result.Success -> {
                    Timber.d("Post updated successfully with images: $postId")
                }
                is com.example.minisocialnetworkapplication.core.util.Result.Error -> {
                    throw Exception("Failed to update post with images: ${result.message}")
                }
                else -> {}
            }

            // Clean up cache files after successful upload
            imageFilePaths.forEach { filePath ->
                try {
                    File(filePath).delete()
                    Timber.d("Deleted cache file: $filePath")
                } catch (e: Exception) {
                    Timber.w(e, "Failed to delete cache file: $filePath")
                }
            }

            Timber.d("Images uploaded and post updated successfully")
            Result.success()

        } catch (e: Exception) {
            Timber.e(e, "Image upload failed, attempt ${runAttemptCount + 1}")

            // Retry up to 3 times
            if (runAttemptCount < 2) {
                Result.retry()
            } else {
                Timber.e("Image upload failed after 3 attempts")
                // Clean up cache files on final failure
                imageFilePaths.forEach { filePath ->
                    try {
                        File(filePath).delete()
                    } catch (e: Exception) {
                        Timber.w(e, "Failed to delete cache file: $filePath")
                    }
                }
                Result.failure()
            }
        }
    }

    /**
     * Upload image from cache file path
     */
    private suspend fun uploadImageFromFile(filePath: String, userId: String): String {
        Timber.d("Uploading image from file: $filePath")

        val file = File(filePath)
        if (!file.exists()) {
            throw Exception("File not found: $filePath")
        }

        // Compress image from file
        val uri = Uri.fromFile(file)
        val compressedFile = ImageCompressor.compressImage(context, uri)
            ?: throw Exception("Image compression failed for $filePath")

        Timber.d("Image compressed: ${compressedFile.length()} bytes")

        // Generate unique filename
        val filename = "${UUID.randomUUID()}.jpg"
        val storageRef = storage.reference
            .child("${Constants.STORAGE_POSTS_PATH}/$userId/$filename")

        // Upload to Firebase Storage
        Timber.d("Uploading image to Storage: $filename")
        val uploadTask = storageRef.putFile(Uri.fromFile(compressedFile)).await()

        // Get download URL
        val downloadUrl = uploadTask.storage.downloadUrl.await().toString()

        // Delete temporary compressed file
        compressedFile.delete()

        Timber.d("Image uploaded successfully: $downloadUrl")
        return downloadUrl
    }

}

