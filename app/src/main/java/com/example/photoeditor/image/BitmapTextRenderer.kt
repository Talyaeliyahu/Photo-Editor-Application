package com.example.photoeditor.image

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import com.example.photoeditor.model.TextOverlay
import kotlin.math.min

object BitmapTextRenderer {
    fun render(source: Bitmap, overlays: List<TextOverlay>): Bitmap {
        if (overlays.isEmpty()) return source

        val out = source.copy(source.config ?: Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(out)

        val minDim = min(out.width, out.height).toFloat().coerceAtLeast(1f)

        overlays.forEach { o ->
            val text = o.text
            if (text.isBlank()) return@forEach

            val typefaceStyle = when {
                o.isBold && o.isItalic -> Typeface.BOLD_ITALIC
                o.isBold -> Typeface.BOLD
                o.isItalic -> Typeface.ITALIC
                else -> Typeface.NORMAL
            }

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = o.argb
                textSize = (o.sizeNorm.coerceIn(0.01f, 0.25f) * minDim)
                typeface = Typeface.create(Typeface.DEFAULT, typefaceStyle)
                isUnderlineText = o.isUnderline
                // Readability on photos
                setShadowLayer(textSize * 0.12f, 0f, textSize * 0.06f, 0xAA000000.toInt())
            }

            val x = (o.xNorm.coerceIn(0f, 1f) * out.width)
            val yTop = (o.yNorm.coerceIn(0f, 1f) * out.height)
            val yBaseline = yTop - paint.ascent() // ascent is negative
            val lineHeight = paint.textSize * 1.15f
            val lines = text.split('\n')

            canvas.save()
            if (o.rotationDegrees != 0f) {
                canvas.rotate(o.rotationDegrees, x, yBaseline)
            }
            lines.forEachIndexed { idx, line ->
                canvas.drawText(line, x, yBaseline + (idx * lineHeight), paint)
            }
            canvas.restore()
        }

        return out
    }
}

