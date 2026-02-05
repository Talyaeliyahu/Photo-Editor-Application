package com.example.photoeditor.image

import android.graphics.Bitmap

/**
 * Interface for image filters.
 * All filters must return a new Bitmap without mutating the source.
 */
interface ImageFilter {
    /**
     * Applies the filter to the source bitmap and returns a new bitmap.
     * @param source The original bitmap (will not be modified)
     * @return A new bitmap with the filter applied
     */
    fun apply(source: Bitmap): Bitmap
}
