package com.example.photoeditor.ui.components.frame

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.photoeditor.R
import com.example.photoeditor.model.FrameConfig
import com.example.photoeditor.model.FrameType

private data class FrameOption(
    val name: String,
    val config: FrameConfig?,
    val isCustomButton: Boolean = false
)

@Composable
private fun frameOptions(): List<FrameOption> = listOf(
    FrameOption(stringResource(R.string.frame_none), FrameConfig.NONE, false),
    FrameOption(stringResource(R.string.frame_custom), null, true),
) + listOf(
    FrameOption(stringResource(R.string.frame_blackwhite), FrameConfig(type = FrameType.IMAGE, frameImageResId = R.drawable.frame_blackwhite, useNinePatch = false)),
    FrameOption(stringResource(R.string.frame_border2), FrameConfig(type = FrameType.IMAGE, frameImageResId = R.drawable.border2, useNinePatch = false)),
    FrameOption(stringResource(R.string.frame_f159), FrameConfig(type = FrameType.IMAGE, frameImageResId = R.drawable.f159, useNinePatch = false)),
    FrameOption(stringResource(R.string.frame_100), FrameConfig(type = FrameType.IMAGE, frameImageResId = R.drawable.frame100, useNinePatch = false)),
    FrameOption(stringResource(R.string.frame_101), FrameConfig(type = FrameType.IMAGE, frameImageResId = R.drawable.frame101, useNinePatch = false)),
    FrameOption(stringResource(R.string.frame_103), FrameConfig(type = FrameType.IMAGE, frameImageResId = R.drawable.frame103, useNinePatch = false)),
    FrameOption(stringResource(R.string.frame_104), FrameConfig(type = FrameType.IMAGE, frameImageResId = R.drawable.frame104, useNinePatch = false)),
    FrameOption(stringResource(R.string.frame_105), FrameConfig(type = FrameType.IMAGE, frameImageResId = R.drawable.frame105, useNinePatch = false)),
    FrameOption(stringResource(R.string.frame_106), FrameConfig(type = FrameType.IMAGE, frameImageResId = R.drawable.frame106, useNinePatch = false)),
    FrameOption(stringResource(R.string.frame_107), FrameConfig(type = FrameType.IMAGE, frameImageResId = R.drawable.frame107, useNinePatch = false)),
    FrameOption(stringResource(R.string.frame_108), FrameConfig(type = FrameType.IMAGE, frameImageResId = R.drawable.frame108, useNinePatch = false)),
    FrameOption(stringResource(R.string.frame_109), FrameConfig(type = FrameType.IMAGE, frameImageResId = R.drawable.frame109, useNinePatch = false)),
    FrameOption(stringResource(R.string.frame_110), FrameConfig(type = FrameType.IMAGE, frameImageResId = R.drawable.frame110, useNinePatch = false)),
    FrameOption(stringResource(R.string.frame_111), FrameConfig(type = FrameType.IMAGE, frameImageResId = R.drawable.frame111, useNinePatch = false)),
    FrameOption(stringResource(R.string.frame_112), FrameConfig(type = FrameType.IMAGE, frameImageResId = R.drawable.frame112, useNinePatch = false)),
    FrameOption(stringResource(R.string.frame_113), FrameConfig(type = FrameType.IMAGE, frameImageResId = R.drawable.frame113, useNinePatch = false)),
    FrameOption(stringResource(R.string.frame_114), FrameConfig(type = FrameType.IMAGE, frameImageResId = R.drawable.frame114, useNinePatch = false)),
    FrameOption(stringResource(R.string.frame_115), FrameConfig(type = FrameType.IMAGE, frameImageResId = R.drawable.frame115, useNinePatch = false)),
    FrameOption(stringResource(R.string.frame_116), FrameConfig(type = FrameType.IMAGE, frameImageResId = R.drawable.frame116, useNinePatch = false)),
    FrameOption(stringResource(R.string.frame_117), FrameConfig(type = FrameType.IMAGE, frameImageResId = R.drawable.frame117, useNinePatch = false)),
)

private fun isConfigEqual(a: FrameConfig, b: FrameConfig): Boolean =
    a.type == b.type &&
        a.thicknessPercent == b.thicknessPercent &&
        a.color == b.color &&
        a.cornerRadiusPercent == b.cornerRadiusPercent &&
        a.frameImageResId == b.frameImageResId &&
        a.useNinePatch == b.useNinePatch

@Composable
fun FramesHorizontalBar(
    currentConfig: FrameConfig,
    onConfigChange: (FrameConfig) -> Unit,
    onCustomClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val options = frameOptions()
    LazyHorizontalGrid(
        rows = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
    ) {
        items(options, key = { "${it.name}_${it.isCustomButton}" }) { option ->
            val isSelected = !option.isCustomButton && option.config != null && isConfigEqual(currentConfig, option.config!!)
            OutlinedButton(
                onClick = {
                    if (option.isCustomButton) {
                        onCustomClick()
                    } else {
                        option.config?.let { onConfigChange(it) }
                    }
                },
                modifier = Modifier
                    .padding(2.dp)
                    .size(width = 90.dp, height = 44.dp),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                border = if (isSelected) {
                    androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                } else {
                    ButtonDefaults.outlinedButtonBorder
                }
            ) {
                Text(
                    text = option.name,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
