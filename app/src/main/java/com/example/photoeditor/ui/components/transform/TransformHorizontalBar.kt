package com.example.photoeditor.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.photoeditor.R

/**
 * Single row of transform tools with horizontal scroll.
 * Used in portrait mode above the bottom navigation bar.
 */
@Composable
fun TransformHorizontalBar(
    selectedTransformTool: TransformTool?,
    onToolSelected: (TransformTool?) -> Unit,
    onRotate: () -> Unit,
    onMirror: () -> Unit,
    onStartCrop: () -> Unit,
    onCropToAspectRatio: (Float) -> Unit,
    onResetToOriginal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val aspectOptions = listOf(
        AspectItem(label = stringResource(R.string.original), ratio = 0f, isOriginal = true),
        AspectItem(label = "1:1", ratio = 1f),
        AspectItem(label = "4:3", ratio = 4f / 3f),
        AspectItem(label = "3:4", ratio = 3f / 4f),
        AspectItem(label = "16:9", ratio = 16f / 9f),
        AspectItem(label = "9:16", ratio = 9f / 16f),
        AspectItem(label = "3:2", ratio = 3f / 2f),
        AspectItem(label = "2:3", ratio = 2f / 3f)
    )

    if (selectedTransformTool == TransformTool.ASPECT_RATIO) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { onToolSelected(null) }) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = Color.White
                )
            }
            LazyHorizontalGrid(
                rows = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
            ) {
                items(aspectOptions) { item ->
                    OutlinedButton(
                        onClick = {
                            if (item.isOriginal) {
                                onResetToOriginal()
                                onToolSelected(null)
                            } else {
                                onCropToAspectRatio(item.ratio)
                                onToolSelected(null)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(2.dp)
                    ) {
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    } else {
        val tools = listOf(
            TransformItem(TransformTool.CROP, Icons.Default.Crop, R.string.crop_image),
            TransformItem(TransformTool.ROTATE, Icons.Default.RotateRight, R.string.rotate_image),
            TransformItem(TransformTool.MIRROR, Icons.Default.SwapHoriz, R.string.mirror_image),
            TransformItem(TransformTool.ASPECT_RATIO, Icons.Default.AspectRatio, R.string.change_aspect_ratio)
        )

        Row(
            modifier = modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tools.forEach { item ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(horizontal = 6.dp)
                ) {
                    AdjustmentIconButton(
                        icon = item.icon,
                        isSelected = selectedTransformTool == item.tool,
                        onClick = {
                            when (item.tool) {
                                TransformTool.CROP -> onStartCrop()
                                TransformTool.ROTATE -> onRotate()
                                TransformTool.MIRROR -> onMirror()
                                TransformTool.ASPECT_RATIO -> onToolSelected(TransformTool.ASPECT_RATIO)
                            }
                        }
                    )
                    Text(
                        text = stringResource(item.labelRes),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )
                }
            }
        }
    }
}

private data class TransformItem(
    val tool: TransformTool,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val labelRes: Int
)

private data class AspectItem(
    val label: String,
    val ratio: Float,
    val isOriginal: Boolean = false
)
