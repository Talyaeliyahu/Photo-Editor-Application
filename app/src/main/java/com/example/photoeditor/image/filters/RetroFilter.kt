package com.example.photoeditor.image.filters

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import com.example.photoeditor.image.ImageFilter

/**
 * Retro look: sepia-like, warm, slightly faded - old photo feel.
 */
class RetroFilter : ImageFilter {
    override fun apply(source: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, source.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()

        val desat = ColorMatrix().apply { setSaturation(0.6f) }

        val warm = ColorMatrix(
            floatArrayOf(
                1.2f, 0.1f, 0f, 0f, 15f,
                0.05f, 0.95f, 0.05f, 0f, 5f,
                0f, 0f, 0.8f, 0f, -5f,
                0f, 0f, 0f, 1f, 0f
            )
        )

        val scale = 0.9f
        val translate = (-0.5f * scale + 0.5f) * 255f + 12f
        val fade = ColorMatrix(
            floatArrayOf(
                scale, 0f, 0f, 0f, translate,
                0f, scale, 0f, 0f, translate,
                0f, 0f, scale, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )

        val combined = ColorMatrix().apply {
            postConcat(desat)
            postConcat(warm)
            postConcat(fade)
        }

        paint.colorFilter = ColorMatrixColorFilter(combined)
        canvas.drawBitmap(source, 0f, 0f, paint)
        return result
    }
}
