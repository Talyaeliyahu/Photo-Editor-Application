package com.example.photoeditor.image

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import com.example.photoeditor.model.StickerOverlay
import kotlin.math.min

object BitmapStickerRenderer {
    fun render(source: Bitmap, overlays: List<StickerOverlay>): Bitmap {
        if (overlays.isEmpty()) return source

        val out = source.copy(source.config ?: Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(out)

        val minDim = min(out.width, out.height).toFloat().coerceAtLeast(1f)

        overlays.forEach { o ->
            val emoji = o.emoji
            if (emoji.isBlank()) return@forEach

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = (o.sizeNorm.coerceIn(0.03f, 0.25f) * minDim)
                typeface = Typeface.DEFAULT
            }

            val x = (o.xNorm.coerceIn(0f, 1f) * out.width)
            val yTop = (o.yNorm.coerceIn(0f, 1f) * out.height)
            val yBaseline = yTop - paint.ascent()

            canvas.save()
            if (o.rotationDegrees != 0f) {
                canvas.rotate(o.rotationDegrees, x, yBaseline)
            }
            canvas.drawText(emoji, x, yBaseline, paint)
            canvas.restore()
        }

        return out
    }
}

