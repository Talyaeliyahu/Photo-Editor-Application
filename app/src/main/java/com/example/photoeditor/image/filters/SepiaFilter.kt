package com.example.photoeditor.image.filters

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import com.example.photoeditor.image.ImageFilter

/**
 * Sepia filter that gives an image a warm, vintage brown tone.
 */
class SepiaFilter : ImageFilter {
    override fun apply(source: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, source.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()
        
        // ColorMatrix for sepia effect
        val colorMatrix = ColorMatrix(
            floatArrayOf(
                0.393f, 0.769f, 0.189f, 0f, 0f,  // Red channel
                0.349f, 0.686f, 0.168f, 0f, 0f,  // Green channel
                0.272f, 0.534f, 0.131f, 0f, 0f,  // Blue channel
                0f, 0f, 0f, 1f, 0f                // Alpha channel
            )
        )
        
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(source, 0f, 0f, paint)
        
        return result
    }
}
