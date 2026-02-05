package com.example.photoeditor.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import com.example.photoeditor.model.CollageConfig
import com.example.photoeditor.utils.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

object CollageRenderer {
    suspend fun render(context: Context, config: CollageConfig, outputWidth: Int = 1080, outputHeight: Int = 1080): Bitmap? = withContext(Dispatchers.IO) {
        // Always use square aspect ratio (1:1)
        val finalWidth = minOf(outputWidth, outputHeight)
        val finalHeight = finalWidth
        
        val output = Bitmap.createBitmap(finalWidth, finalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        
        // Draw background
        canvas.drawColor(android.graphics.Color.WHITE)
        
        // Load and draw each image
        config.images.forEachIndexed { index, collageImage ->
            val bitmap = ImageUtils.loadBitmapFromUri(
                context,
                collageImage.uri,
                maxWidth = finalWidth,
                maxHeight = finalHeight
            ) ?: return@withContext null
            
            // Calculate position and size in pixels
            val x = collageImage.x * finalWidth
            val y = collageImage.y * finalHeight
            val width = collageImage.width * finalWidth
            val height = collageImage.height * finalHeight
            
            // Account for border
            val borderOffset = config.borderWidth / 2f
            val imageX = x + borderOffset
            val imageY = y + borderOffset
            val imageWidth = width - config.borderWidth
            val imageHeight = height - config.borderWidth
            
            // Source rect: use user crop if set, otherwise center-crop to fill cell
            val cropW = (collageImage.cropRight - collageImage.cropLeft).coerceIn(0.01f, 1f)
            val cropH = (collageImage.cropBottom - collageImage.cropTop).coerceIn(0.01f, 1f)
            val srcX = (collageImage.cropLeft * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
            val srcY = (collageImage.cropTop * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
            val srcW = (cropW * bitmap.width).toInt().coerceIn(1, bitmap.width - srcX)
            val srcH = (cropH * bitmap.height).toInt().coerceIn(1, bitmap.height - srcY)

            val srcRect = android.graphics.Rect(srcX, srcY, srcX + srcW, srcY + srcH)
            // Scale crop to FILL cell without distortion (preserve aspect ratio, no white gaps)
            val scale = max(imageWidth / srcW, imageHeight / srcH)
            val drawnWidth = srcW * scale
            val drawnHeight = srcH * scale
            val dstLeft = imageX + (imageWidth - drawnWidth) / 2f
            val dstTop = imageY + (imageHeight - drawnHeight) / 2f
            val dstRect = RectF(dstLeft, dstTop, dstLeft + drawnWidth, dstTop + drawnHeight)

            canvas.save()
            if (collageImage.rotation != 0f) {
                canvas.rotate(
                    collageImage.rotation,
                    imageX + imageWidth / 2f,
                    imageY + imageHeight / 2f
                )
            }
            canvas.clipRect(imageX, imageY, imageX + imageWidth, imageY + imageHeight)
            canvas.drawBitmap(bitmap, srcRect, dstRect, null)
            canvas.restore()
            
            // Draw border if needed
            if (config.borderWidth > 0) {
                val borderPaint = Paint().apply {
                    color = config.borderColor
                    style = Paint.Style.STROKE
                    strokeWidth = config.borderWidth
                }
                canvas.drawRect(
                    RectF(x, y, x + width, y + height),
                    borderPaint
                )
            }
            
            // Don't recycle - bitmap might be reused or managed by Coil
        }
        
        output
    }
}
