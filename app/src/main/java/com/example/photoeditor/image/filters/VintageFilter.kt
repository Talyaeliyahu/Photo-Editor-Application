package com.example.photoeditor.image.filters

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import com.example.photoeditor.image.ImageFilter

/**
 * Vintage look: slightly desaturated + warm tone + soft contrast.
 */
class VintageFilter : ImageFilter {
    override fun apply(source: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, source.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()

        val desat = ColorMatrix().apply { setSaturation(0.85f) }

        // Soft contrast (slightly lower) + a small lift (fade)
        val scale = 0.95f
        val translate = (-0.5f * scale + 0.5f) * 255f + 6f
        val softContrast = ColorMatrix(
            floatArrayOf(
                scale, 0f, 0f, 0f, translate,
                0f, scale, 0f, 0f, translate,
                0f, 0f, scale, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )

        val warm = ColorMatrix(
            floatArrayOf(
                1.03f, 0f, 0f, 0f, 8f,
                0f, 1.00f, 0f, 0f, 2f,
                0f, 0f, 0.97f, 0f, -2f,
                0f, 0f, 0f, 1f, 0f
            )
        )

        val combined = ColorMatrix().apply {
            postConcat(desat)
            postConcat(warm)
            postConcat(softContrast)
        }

        paint.colorFilter = ColorMatrixColorFilter(combined)
        canvas.drawBitmap(source, 0f, 0f, paint)
        return result
    }
}

