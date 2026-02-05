package com.example.photoeditor.image.filters

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import com.example.photoeditor.image.ImageFilter

/**
 * Cool look: slightly boosts blues and reduces reds.
 */
class CoolFilter : ImageFilter {
    override fun apply(source: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, source.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()

        val matrix = ColorMatrix(
            floatArrayOf(
                0.94f, 0f, 0f, 0f, -6f,
                0f, 1.00f, 0f, 0f, 0f,
                0f, 0f, 1.06f, 0f, 10f,
                0f, 0f, 0f, 1f, 0f
            )
        )

        paint.colorFilter = ColorMatrixColorFilter(matrix)
        canvas.drawBitmap(source, 0f, 0f, paint)
        return result
    }
}

