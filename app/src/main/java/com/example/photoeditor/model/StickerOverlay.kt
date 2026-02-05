package com.example.photoeditor.model

/**
 * A sticker layer positioned relative to the current image (normalized 0..1).
 *
 * For now we model stickers as emoji characters (fast to prototype, no assets needed).
 * xNorm/yNorm are the top-left position as a fraction of image width/height.
 * sizeNorm is a fraction of min(imageWidth, imageHeight) used as "sticker size".
 */
data class StickerOverlay(
    val id: String,
    val emoji: String,
    val xNorm: Float,
    val yNorm: Float,
    val sizeNorm: Float,
    val rotationDegrees: Float
)

