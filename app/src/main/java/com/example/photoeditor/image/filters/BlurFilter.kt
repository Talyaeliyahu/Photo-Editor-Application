package com.example.photoeditor.image.filters

import android.graphics.Bitmap
import com.example.photoeditor.image.ImageFilter

/**
 * Blur filter using a simple box blur algorithm.
 * Safe for all Android versions without using deprecated APIs.
 */
class BlurFilter(
    private val radius: Int = 5
) : ImageFilter {
    
    override fun apply(source: Bitmap): Bitmap {
        return applyBoxBlur(source, radius.coerceIn(1, 10))
    }
    
    /**
     * Applies a box blur algorithm to the bitmap.
     * This is a simple but effective blur method.
     */
    private fun applyBoxBlur(source: Bitmap, radius: Int): Bitmap {
        val width = source.width
        val height = source.height
        val result = Bitmap.createBitmap(width, height, source.config ?: Bitmap.Config.ARGB_8888)
        
        // Create pixel arrays
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)
        
        // Apply horizontal blur
        val horizontalBlur = IntArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                var r = 0
                var g = 0
                var b = 0
                var count = 0
                
                for (dx in -radius..radius) {
                    val px = (x + dx).coerceIn(0, width - 1)
                    val pixel = pixels[y * width + px]
                    r += android.graphics.Color.red(pixel)
                    g += android.graphics.Color.green(pixel)
                    b += android.graphics.Color.blue(pixel)
                    count++
                }
                
                horizontalBlur[y * width + x] = android.graphics.Color.rgb(
                    (r / count),
                    (g / count),
                    (b / count)
                )
            }
        }
        
        // Apply vertical blur
        for (y in 0 until height) {
            for (x in 0 until width) {
                var r = 0
                var g = 0
                var b = 0
                var count = 0
                
                for (dy in -radius..radius) {
                    val py = (y + dy).coerceIn(0, height - 1)
                    val pixel = horizontalBlur[py * width + x]
                    r += android.graphics.Color.red(pixel)
                    g += android.graphics.Color.green(pixel)
                    b += android.graphics.Color.blue(pixel)
                    count++
                }
                
                pixels[y * width + x] = android.graphics.Color.rgb(
                    (r / count),
                    (g / count),
                    (b / count)
                )
            }
        }
        
        result.setPixels(pixels, 0, width, 0, 0, width, height)
        return result
    }
}
