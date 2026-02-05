package com.example.photoeditor.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.photoeditor.R

@Composable
internal fun labelForAdjustment(type: AdjustmentType): String {
    return when (type) {
        AdjustmentType.BRIGHTNESS -> stringResource(R.string.brightness)
        AdjustmentType.CONTRAST -> stringResource(R.string.contrast)
        AdjustmentType.SATURATION -> stringResource(R.string.saturation)
        AdjustmentType.EXPOSURE -> stringResource(R.string.exposure)
        AdjustmentType.GLOW -> stringResource(R.string.glow)
        AdjustmentType.HIGHLIGHTS -> stringResource(R.string.highlights)
        AdjustmentType.SHADOWS -> stringResource(R.string.shadows)
        AdjustmentType.VIBRANCE -> stringResource(R.string.vibrance)
        AdjustmentType.WARMTH -> stringResource(R.string.warmth)
        AdjustmentType.TINT -> stringResource(R.string.tint)
        AdjustmentType.HUE -> stringResource(R.string.hue)
        AdjustmentType.SHARPNESS -> stringResource(R.string.sharpness)
        AdjustmentType.DEFINITION -> stringResource(R.string.definition)
        AdjustmentType.VIGNETTE -> stringResource(R.string.vignette)
    }
}

