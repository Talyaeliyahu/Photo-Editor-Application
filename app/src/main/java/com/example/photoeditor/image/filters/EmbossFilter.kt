package com.example.photoeditor.image.filters

import android.graphics.Bitmap
import com.example.photoeditor.image.ImageFilter

/**
 * Emboss filter - gives a raised/engraved 3D effect using convolution.
 */
class EmbossFilter : ImageFilter {

    // Emboss convolution kernel (highlights edges for 3D effect)
    private val kernel = floatArrayOf(
        -2f, -1f, 0f,
        -1f,  1f, 1f,
         0f,  1f, 2f
    )

    override fun apply(source: Bitmap): Bitmap {
        val width = source.width
        val height = source.height
        val result = Bitmap.createBitmap(width, height, source.config ?: Bitmap.Config.ARGB_8888)

        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        for (y in 0 until height) {
            for (x in 0 until width) {
                var sumR = 0f
                var sumG = 0f
                var sumB = 0f
                var k = 0

                for (dy in -1..1) {
                    for (dx in -1..1) {
                        val px = (x + dx).coerceIn(0, width - 1)
                        val py = (y + dy).coerceIn(0, height - 1)
                        val pixel = pixels[py * width + px]
                        val weight = kernel[k++]
                        sumR += android.graphics.Color.red(pixel) * weight
                        sumG += android.graphics.Color.green(pixel) * weight
                        sumB += android.graphics.Color.blue(pixel) * weight
                    }
                }

                // Add 128 for gray mid-tone (emboss effect)
                val r = (sumR + 128f).coerceIn(0f, 255f).toInt()
                val g = (sumG + 128f).coerceIn(0f, 255f).toInt()
                val b = (sumB + 128f).coerceIn(0f, 255f).toInt()
                val a = android.graphics.Color.alpha(pixels[y * width + x])

                result.setPixel(x, y, android.graphics.Color.argb(a, r, g, b))
            }
        }

        return result
    }
}
