package com.example.photoeditor.image.filters

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import com.example.photoeditor.image.ImageFilter

/**
 * Clean skin look: soft, reduced contrast, slight warmth - smooth skin/portrait enhancement.
 */
class CleanSkinFilter : ImageFilter {
    override fun apply(source: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, source.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()

        val desat = ColorMatrix().apply { setSaturation(0.9f) }

        val scale = 0.92f
        val translate = (-0.5f * scale + 0.5f) * 255f + 12f
        val soft = ColorMatrix(
            floatArrayOf(
                scale, 0f, 0f, 0f, translate,
                0f, scale, 0f, 0f, translate,
                0f, 0f, scale, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )

        val warm = ColorMatrix(
            floatArrayOf(
                1.03f, 0f, 0f, 0f, 5f,
                0f, 1.02f, 0f, 0f, 3f,
                0f, 0f, 0.98f, 0f, -2f,
                0f, 0f, 0f, 1f, 0f
            )
        )

        val combined = ColorMatrix().apply {
            postConcat(desat)
            postConcat(soft)
            postConcat(warm)
        }

        paint.colorFilter = ColorMatrixColorFilter(combined)
        canvas.drawBitmap(source, 0f, 0f, paint)
        return result
    }
}
