package com.example.photoeditor.model

import androidx.compose.ui.geometry.Offset

/**
 * A single drawn path (stroke) on the image.
 * Points are in normalized coordinates 0-1 relative to image dimensions.
 */
data class DrawPath(
    val points: List<Offset>,
    val color: Int,
    val strokeWidthNorm: Float // fraction of image width, e.g. 0.02 = 2%
)
