package com.example.photoeditor.image.filters

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import com.example.photoeditor.image.ImageFilter

/**
 * Sunset look: warm oranges/reds, golden hour, boost warm tones.
 */
class SunsetFilter : ImageFilter {
    override fun apply(source: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, source.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()

        val saturation = ColorMatrix().apply { setSaturation(1.15f) }

        val warm = ColorMatrix(
            floatArrayOf(
                1.25f, 0.08f, 0f, 0f, 20f,
                0.05f, 1.05f, 0.05f, 0f, 10f,
                0f, 0f, 0.85f, 0f, -15f,
                0f, 0f, 0f, 1f, 0f
            )
        )

        val combined = ColorMatrix().apply {
            postConcat(saturation)
            postConcat(warm)
        }

        paint.colorFilter = ColorMatrixColorFilter(combined)
        canvas.drawBitmap(source, 0f, 0f, paint)
        return result
    }
}
