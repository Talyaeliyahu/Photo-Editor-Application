package com.example.photoeditor.model

import android.graphics.Color

/**
 * Configuration for applying a frame to an image.
 * All dimensions are relative to the bitmap size (0.0 - 1.0) for resolution independence.
 */
data class FrameConfig(
    /**
     * Frame type - stroke-based or image-based
     */
    val type: FrameType,
    
    /**
     * For STROKE frames: thickness as percentage of bitmap width (0.01 = 1%, 0.05 = 5%)
     * Recommended range: 0.01f to 0.1f
     */
    val thicknessPercent: Float = 0.03f,
    
    /**
     * For STROKE frames: frame color (ARGB)
     */
    val color: Int = Color.WHITE,
    
    /**
     * For STROKE frames: corner radius as percentage of bitmap width (0.0 = square, 0.1 = 10% rounded)
     */
    val cornerRadiusPercent: Float = 0f,
    
    /**
     * For IMAGE frames: drawable resource ID of the frame image (PNG/WEBP with transparency)
     */
    val frameImageResId: Int? = null,
    
    /**
     * For IMAGE frames: whether to use nine-patch scaling (if frame supports it)
     */
    val useNinePatch: Boolean = false
) {
    companion object {
        /**
         * No frame
         */
        val NONE = FrameConfig(
            type = FrameType.NONE,
            thicknessPercent = 0f
        )
    }
}

enum class FrameType {
    NONE,
    STROKE,      // Simple stroke-based frame
    IMAGE        // Frame from PNG/WEBP image resource
}
