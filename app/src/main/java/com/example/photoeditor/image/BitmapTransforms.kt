package com.example.photoeditor.image

import android.graphics.Bitmap
import android.graphics.Matrix
import kotlin.math.roundToInt

object BitmapTransforms {
    fun rotate90Clockwise(source: Bitmap): Bitmap {
        val m = Matrix().apply { postRotate(90f) }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, m, true)
    }

    fun flipHorizontal(source: Bitmap): Bitmap {
        val m = Matrix().apply {
            postScale(-1f, 1f)
            postTranslate(source.width.toFloat(), 0f)
        }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, m, true)
    }

    /**
     * Center-crop by a percentage while keeping current aspect ratio.
     * Example: 0.8f keeps 80% of width/height.
     */
    fun cropCenter(source: Bitmap, scale: Float): Bitmap {
        val s = scale.coerceIn(0.1f, 1f)
        val w = (source.width * s).roundToInt().coerceAtLeast(1)
        val h = (source.height * s).roundToInt().coerceAtLeast(1)
        val x = ((source.width - w) / 2f).roundToInt().coerceAtLeast(0)
        val y = ((source.height - h) / 2f).roundToInt().coerceAtLeast(0)
        return Bitmap.createBitmap(source, x, y, w, h)
    }

    /**
     * Crop to a target aspect ratio (width/height), using a max-area centered crop.
     *
     * @param scale 0.1..1.0. 1.0 = max-area crop for that ratio.
     */
    fun cropToAspectRatio(source: Bitmap, targetRatio: Float, scale: Float = 1f): Bitmap {
        val r = targetRatio.coerceAtLeast(0.01f)
        val w = source.width
        val h = source.height
        val current = w.toFloat() / h.toFloat()

        val (cw, ch) = if (current > r) {
            // Image too wide -> crop width
            val cropW = (h * r).roundToInt().coerceAtMost(w)
            cropW to h
        } else {
            // Image too tall -> crop height
            val cropH = (w / r).roundToInt().coerceAtMost(h)
            w to cropH
        }

        val s = scale.coerceIn(0.1f, 1f)
        val scaledW = (cw * s).roundToInt().coerceAtLeast(1).coerceAtMost(cw)
        val scaledH = (ch * s).roundToInt().coerceAtLeast(1).coerceAtMost(ch)

        val x = ((w - scaledW) / 2f).roundToInt().coerceAtLeast(0)
        val y = ((h - scaledH) / 2f).roundToInt().coerceAtLeast(0)
        return Bitmap.createBitmap(source, x, y, scaledW, scaledH)
    }
}

