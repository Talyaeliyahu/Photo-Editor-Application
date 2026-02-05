package com.example.photoeditor.model

/**
 * A text layer positioned relative to the current image (normalized 0..1).
 *
 * xNorm/yNorm are the top-left position as a fraction of image width/height.
 * sizeNorm is a fraction of min(imageWidth, imageHeight) used as text size.
 */
data class TextOverlay(
    val id: String,
    val text: String,
    val xNorm: Float,
    val yNorm: Float,
    val sizeNorm: Float,
    val rotationDegrees: Float,
    val isBold: Boolean,
    val isItalic: Boolean,
    val isUnderline: Boolean,
    val argb: Int
)

