package com.example.photoeditor.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStream

/**
 * Utility class for image operations like loading from URI and saving to gallery.
 */
object ImageUtils {
    
    /**
     * Loads a bitmap from a URI.
     * @param context Application context
     * @param uri The URI of the image
     * @param maxWidth Maximum width for downscaling (0 = no limit)
     * @param maxHeight Maximum height for downscaling (0 = no limit)
     * @return The loaded bitmap, or null if loading failed
     */
    suspend fun loadBitmapFromUri(
        context: Context,
        uri: Uri,
        maxWidth: Int = 0,
        maxHeight: Int = 0
    ): Bitmap? = withContext(Dispatchers.IO) {
        Log.d("ImageUtils", "=== LOAD BITMAP FROM URI START ===")
        Log.d("ImageUtils", "URI: $uri")
        Log.d("ImageUtils", "MaxWidth: $maxWidth, MaxHeight: $maxHeight")
        
        try {
            // On Android 9+ use ImageDecoder, which applies EXIF orientation correctly by default.
            // This avoids the "everything rotated" bug that can happen with manual EXIF handling on some devices/URIs.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val decoded = decodeWithImageDecoder(context, uri, maxWidth, maxHeight)
                if (decoded != null) {
                    Log.d("ImageUtils", "Bitmap decoded successfully (ImageDecoder): ${decoded.width}x${decoded.height}")
                    Log.d("ImageUtils", "=== LOAD BITMAP FROM URI SUCCESS ===")
                    return@withContext decoded
                }
                // Fall through to legacy path as a last resort
                Log.w("ImageUtils", "ImageDecoder failed; falling back to BitmapFactory path")
            }

            // Android 8.1 and below: decode + apply EXIF manually
            val orientation = readExifOrientation(context, uri)

            val bitmap = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                Log.d("ImageUtils", "Decoding bitmap from stream...")
                BitmapFactory.decodeStream(inputStream)
            }

            if (bitmap == null) {
                Log.e("ImageUtils", "ERROR: BitmapFactory.decodeStream returned null")
                return@withContext null
            }

            Log.d("ImageUtils", "Bitmap decoded successfully: ${bitmap.width}x${bitmap.height}")

            val scaled = if (maxWidth > 0 || maxHeight > 0) {
                Log.d("ImageUtils", "Downscaling bitmap...")
                val downscaled = downscaleBitmap(bitmap, maxWidth, maxHeight)
                if (downscaled !== bitmap) bitmap.recycle()
                Log.d("ImageUtils", "Bitmap downscaled to: ${downscaled.width}x${downscaled.height}")
                downscaled
            } else {
                bitmap
            }

            val oriented = applyExifOrientation(scaled, orientation)
            if (oriented !== scaled) scaled.recycle()

            Log.d("ImageUtils", "=== LOAD BITMAP FROM URI SUCCESS ===")
            oriented
        } catch (e: Exception) {
            Log.e("ImageUtils", "EXCEPTION in loadBitmapFromUri: ${e.message}")
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Downscales a bitmap to fit within maxWidth and maxHeight while maintaining aspect ratio.
     */
    private fun downscaleBitmap(
        bitmap: Bitmap,
        maxWidth: Int,
        maxHeight: Int
    ): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        // Treat 0 as "no limit" for that dimension
        val effectiveMaxWidth = if (maxWidth > 0) maxWidth else width
        val effectiveMaxHeight = if (maxHeight > 0) maxHeight else height
        
        if (width <= effectiveMaxWidth && height <= effectiveMaxHeight) {
            return bitmap
        }
        
        val ratio = minOf(
            effectiveMaxWidth.toFloat() / width,
            effectiveMaxHeight.toFloat() / height
        )
        
        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * Applies EXIF orientation (rotate/flip) to the bitmap if required.
     * Many camera/gallery images are stored with an orientation flag instead of being physically rotated.
     */
    private fun applyExifOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        if (orientation == ExifInterface.ORIENTATION_NORMAL || orientation == ExifInterface.ORIENTATION_UNDEFINED) {
            return bitmap
        }

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { // flip + rotate 90
                matrix.postScale(-1f, 1f)
                matrix.postRotate(90f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> { // flip + rotate 270
                matrix.postScale(-1f, 1f)
                matrix.postRotate(270f)
            }
            else -> return bitmap
        }

        return runCatching {
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }.getOrElse {
            Log.w("ImageUtils", "Failed to apply EXIF orientation transform: ${it.message}")
            bitmap
        }
    }

    /**
     * Android 9+ decode path (handles EXIF automatically).
     * Includes:
     * - accept partial images (some PhotoPicker/cloud sources stream data)
     * - fallback to FileDescriptor source
     * - small retry for "Input was incomplete"
     */
    private fun decodeWithImageDecoder(
        context: Context,
        uri: Uri,
        maxWidth: Int,
        maxHeight: Int
    ): Bitmap? {
        val expectedSizeBytes = queryOpenableSizeBytes(context, uri)

        fun configure(decoder: ImageDecoder, info: ImageDecoder.ImageInfo) {
            decoder.isMutableRequired = true
            decoder.setOnPartialImageListener { e ->
                // IMPORTANT: do NOT accept partial images, because they look "cut" (missing bottom/parts).
                Log.w("ImageUtils", "Partial image encountered; will retry instead. error=${e.error} msg=${e.message}")
                false
            }

            if (maxWidth > 0 || maxHeight > 0) {
                val w = info.size.width
                val h = info.size.height
                val effectiveMaxWidth = if (maxWidth > 0) maxWidth else w
                val effectiveMaxHeight = if (maxHeight > 0) maxHeight else h

                val ratio = minOf(
                    effectiveMaxWidth.toFloat() / w.toFloat(),
                    effectiveMaxHeight.toFloat() / h.toFloat(),
                    1f
                )

                val targetW = (w * ratio).toInt().coerceAtLeast(1)
                val targetH = (h * ratio).toInt().coerceAtLeast(1)
                decoder.setTargetSize(targetW, targetH)
            }
        }

        val sources: List<() -> ImageDecoder.Source?> = listOf(
            { ImageDecoder.createSource(context.contentResolver, uri) },
            {
                // Some providers are more stable via AssetFileDescriptor
                runCatching {
                    ImageDecoder.createSource {
                        context.contentResolver.openAssetFileDescriptor(uri, "r")
                    }
                }.getOrNull()
            }
        )

        // Some providers (Photo Picker / cloud-backed) may return incomplete data on first read.
        val maxAttempts = 4
        for (attempt in 1..maxAttempts) {
            for (sourceProvider in sources) {
                val source = runCatching { sourceProvider() }.getOrNull() ?: continue
                val bitmap = runCatching {
                    ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                        configure(decoder, info)
                    }
                }.getOrElse { throwable ->
                    val msg = throwable.message ?: ""
                    if (throwable is ImageDecoder.DecodeException) {
                        Log.w("ImageUtils", "ImageDecoder attempt=$attempt failed: ${throwable.message}")
                        null
                    } else {
                        Log.w("ImageUtils", "ImageDecoder attempt=$attempt failed: ${throwable.javaClass.simpleName}: ${throwable.message}")
                        null
                    }
                }
                if (bitmap != null) return bitmap
            }

            // Fallback: copy to a local cache file and decode from File.
            // This helps when the provider stream is flaky/partial on first reads.
            val cached = decodeViaCacheFile(context, uri, expectedSizeBytes, maxWidth, maxHeight)
            if (cached != null) return cached

            // Retry delay (kept tiny to avoid blocking UI; runs on IO dispatcher)
            try {
                Thread.sleep(220L)
            } catch (_: InterruptedException) {
                // ignore
            }
        }

        return null
    }

    private fun decodeViaCacheFile(
        context: Context,
        uri: Uri,
        expectedSizeBytes: Long?,
        maxWidth: Int,
        maxHeight: Int
    ): Bitmap? {
        // Avoid huge memory spikes by decoding from file, not byte[].
        val tmp = runCatching { File.createTempFile("picker_", ".img", context.cacheDir) }.getOrNull()
            ?: return null

        try {
            val copied = runCatching {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    tmp.outputStream().use { output ->
                        input.copyTo(output)
                    }
                } ?: return null
                tmp.length()
            }.getOrNull() ?: return null

            if (expectedSizeBytes != null && expectedSizeBytes > 0 && copied in 1 until expectedSizeBytes) {
                Log.w("ImageUtils", "Cache copy incomplete: got=$copied expected=$expectedSizeBytes uri=$uri")
                return null
            }

            val source = runCatching { ImageDecoder.createSource(tmp) }.getOrNull() ?: return null
            return runCatching {
                ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    decoder.isMutableRequired = true
                    decoder.setOnPartialImageListener { e ->
                        Log.w("ImageUtils", "Partial image even from cache; rejecting. error=${e.error} msg=${e.message}")
                        false
                    }

                    if (maxWidth > 0 || maxHeight > 0) {
                        val w = info.size.width
                        val h = info.size.height
                        val effectiveMaxWidth = if (maxWidth > 0) maxWidth else w
                        val effectiveMaxHeight = if (maxHeight > 0) maxHeight else h

                        val ratio = minOf(
                            effectiveMaxWidth.toFloat() / w.toFloat(),
                            effectiveMaxHeight.toFloat() / h.toFloat(),
                            1f
                        )

                        val targetW = (w * ratio).toInt().coerceAtLeast(1)
                        val targetH = (h * ratio).toInt().coerceAtLeast(1)
                        decoder.setTargetSize(targetW, targetH)
                    }
                }
            }.getOrElse { throwable ->
                Log.w("ImageUtils", "decodeViaCacheFile failed: ${throwable.message}")
                null
            }
        } finally {
            runCatching { tmp.delete() }
        }
    }

    private fun queryOpenableSizeBytes(context: Context, uri: Uri): Long? {
        return runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(android.provider.OpenableColumns.SIZE),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    if (idx >= 0) cursor.getLong(idx) else null
                } else null
            }
        }.getOrNull()
    }

    private fun readExifOrientation(context: Context, uri: Uri): Int {
        return runCatching {
            // Prefer FileDescriptor when available (more reliable for some content URIs)
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                ExifInterface(pfd.fileDescriptor).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: run {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    ExifInterface(input).getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                } ?: ExifInterface.ORIENTATION_NORMAL
            }
        }.getOrElse {
            Log.w("ImageUtils", "Failed to read EXIF orientation: ${it.message}")
            ExifInterface.ORIENTATION_NORMAL
        }
    }
    
    /**
     * Saves a bitmap to the device gallery using MediaStore.
     * Creates a new file with a unique name and does not overwrite the original.
     * @param context Application context
     * @param bitmap The bitmap to save
     * @param displayName Optional display name (will add timestamp if not provided)
     * @return true if saved successfully, false otherwise
     */
    suspend fun saveBitmapToGallery(
        context: Context,
        bitmap: Bitmap,
        displayName: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val contentValues = ContentValues().apply {
                val name = displayName ?: "PhotoEditor_${System.currentTimeMillis()}"
                put(MediaStore.MediaColumns.DISPLAY_NAME, "$name.jpg")
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/PhotoEditor")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                } else {
                    @Suppress("DEPRECATION")
                    put(MediaStore.MediaColumns.DATA, "${android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES)}/PhotoEditor/$name.jpg")
                }
            }
            
            val uri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            ) ?: return@withContext false
            
            context.contentResolver.openOutputStream(uri)?.use { outputStream: OutputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                context.contentResolver.update(uri, contentValues, null, null)
            }
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Shows a toast message on the main thread.
     */
    fun showToast(context: Context, message: String) {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}
