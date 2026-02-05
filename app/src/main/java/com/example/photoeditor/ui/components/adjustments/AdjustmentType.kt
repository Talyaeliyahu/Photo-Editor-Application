package com.example.photoeditor.ui.components

/**
 * Adjustment types supported by the editor.
 */
enum class AdjustmentType {
    // Existing (do not duplicate)
    BRIGHTNESS,
    CONTRAST,
    SATURATION,

    // Apple-like adjustments (added)
    EXPOSURE,
    HIGHLIGHTS,
    SHADOWS,
    VIBRANCE,
    WARMTH,
    TINT,
    HUE,
    SHARPNESS,
    DEFINITION,
    VIGNETTE,
    GLOW
}

/**
 * Controls the order (and the set) of adjustments shown in the grid.
 * Only includes adjustments the user asked for (plus the existing ones).
 */
internal val adjustmentOrder: List<AdjustmentType> = listOf(
    AdjustmentType.BRIGHTNESS,
    AdjustmentType.EXPOSURE,
    AdjustmentType.GLOW,
    AdjustmentType.HIGHLIGHTS,
    AdjustmentType.SHADOWS,
    AdjustmentType.CONTRAST,
    AdjustmentType.SATURATION,
    AdjustmentType.VIBRANCE,
    AdjustmentType.WARMTH,
    AdjustmentType.HUE, // "גוון"
    AdjustmentType.SHARPNESS,
    AdjustmentType.DEFINITION,
    AdjustmentType.VIGNETTE
)

