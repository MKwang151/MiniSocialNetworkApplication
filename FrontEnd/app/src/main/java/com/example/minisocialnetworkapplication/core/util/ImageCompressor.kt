package com.example.minisocialnetworkapplication.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object ImageCompressor {

    /**
     * Compress image from URI
     * @param context Android context
     * @param uri Image URI
     * @param quality Compression quality (0-100)
     * @param maxWidth Maximum width of output image
     * @param maxHeight Maximum height of output image
     * @return Compressed image file
     */
    fun compressImage(
        context: Context,
        uri: Uri,
        quality: Int = Constants.IMAGE_QUALITY,
        maxWidth: Int = Constants.MAX_IMAGE_WIDTH,
        maxHeight: Int = Constants.MAX_IMAGE_HEIGHT
    ): File? {
        var inputStream: java.io.InputStream? = null
        var originalBitmap: Bitmap? = null
        var scaledBitmap: Bitmap? = null

        return try {
            Timber.d("Starting image compression for: $uri")

            // First, decode with inJustDecodeBounds to get dimensions without loading full bitmap
            inputStream = context.contentResolver.openInputStream(uri)
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            val imageWidth = options.outWidth
            val imageHeight = options.outHeight
            Timber.d("Original image size: ${imageWidth}x${imageHeight}")

            // Calculate sample size to reduce memory usage
            val (scaledWidth, scaledHeight) = calculateScaledDimensions(
                imageWidth,
                imageHeight,
                maxWidth,
                maxHeight
            )

            options.inJustDecodeBounds = false
            options.inSampleSize = calculateInSampleSize(options, scaledWidth, scaledHeight)
            options.inPreferredConfig = Bitmap.Config.RGB_565 // Use less memory

            Timber.d("Using sample size: ${options.inSampleSize}")

            // Now decode with sample size
            inputStream = context.contentResolver.openInputStream(uri)
            originalBitmap = BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            if (originalBitmap == null) {
                Timber.e("Failed to decode bitmap from URI: $uri")
                return null
            }

            Timber.d("Decoded bitmap: ${originalBitmap.width}x${originalBitmap.height}")

            // Scale bitmap if needed
            scaledBitmap = if (originalBitmap.width != scaledWidth || originalBitmap.height != scaledHeight) {
                Bitmap.createScaledBitmap(
                    originalBitmap,
                    scaledWidth,
                    scaledHeight,
                    true
                )
            } else {
                originalBitmap
            }

            // Compress to JPEG
            val outputStream = ByteArrayOutputStream()
            val compressed = scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)

            if (!compressed) {
                Timber.e("Failed to compress bitmap")
                return null
            }

            val compressedBytes = outputStream.toByteArray()
            outputStream.close()

            // Save to temp file
            val tempFile = File.createTempFile(
                "compressed_${System.currentTimeMillis()}",
                ".jpg",
                context.cacheDir
            )

            FileOutputStream(tempFile).use { fos ->
                fos.write(compressedBytes)
            }

            Timber.d("Image compressed successfully: ${tempFile.length() / 1024} KB")
            tempFile

        } catch (e: OutOfMemoryError) {
            Timber.e(e, "Out of memory compressing image: $uri")
            null
        } catch (e: SecurityException) {
            Timber.e(e, "Security exception accessing image: $uri")
            null
        } catch (e: Exception) {
            Timber.e(e, "Error compressing image: $uri")
            null
        } finally {
            // Clean up resources
            try {
                inputStream?.close()
                originalBitmap?.recycle()
                if (scaledBitmap != null && scaledBitmap != originalBitmap) {
                    scaledBitmap.recycle()
                }
            } catch (e: Exception) {
                Timber.w(e, "Error cleaning up bitmap resources")
            }
        }
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    private fun calculateScaledDimensions(
        originalWidth: Int,
        originalHeight: Int,
        maxWidth: Int,
        maxHeight: Int
    ): Pair<Int, Int> {
        var width = originalWidth
        var height = originalHeight

        if (width > maxWidth || height > maxHeight) {
            val ratio = width.toFloat() / height.toFloat()

            if (ratio > 1) {
                // Landscape
                width = maxWidth
                height = (maxWidth / ratio).toInt()
            } else {
                // Portrait
                height = maxHeight
                width = (maxHeight * ratio).toInt()
            }
        }

        return Pair(width, height)
    }
}

