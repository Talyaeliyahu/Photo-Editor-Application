package com.example.photoeditor.ui.components

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.photoeditor.R

@Composable
fun TransformPanel(
    selectedTool: TransformTool?,
    onToolSelected: (TransformTool?) -> Unit,
    onRotate: () -> Unit,
    onMirror: () -> Unit,
    onStartCrop: () -> Unit,
    onResetToOriginal: () -> Unit,
    onCropToAspectRatio: (ratio: Float) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = selectedTool == null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    TransformToolsGrid(
                        onToolClick = { tool ->
                            when (tool) {
                                TransformTool.ROTATE -> onRotate()
                                TransformTool.MIRROR -> onMirror()
                                TransformTool.CROP -> onStartCrop()
                                TransformTool.ASPECT_RATIO -> onToolSelected(tool)
                            }
                        }
                    )
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = selectedTool != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    when (selectedTool) {
                        TransformTool.ASPECT_RATIO -> AspectRatioOptionsPanel(
                            onBack = { onToolSelected(null) },
                            onResetToOriginal = {
                                onResetToOriginal()
                                onToolSelected(null)
                            },
                            onSelect = { ratio ->
                                onCropToAspectRatio(ratio)
                                onToolSelected(null)
                            },
                        )
                        TransformTool.ROTATE,
                        TransformTool.MIRROR,
                        TransformTool.CROP,
                        null -> {}
                    }
                }
            }
        }
    }
}

@Composable
private fun TransformToolsGrid(onToolClick: (TransformTool) -> Unit) {
    val options = listOf(
        TransformGridSpec(
            tool = TransformTool.CROP,
            icon = Icons.Default.Crop,
            labelRes = R.string.crop_image
        ),
        TransformGridSpec(
            tool = TransformTool.ROTATE,
            icon = Icons.Default.RotateRight,
            labelRes = R.string.rotate_image
        ),
        TransformGridSpec(
            tool = TransformTool.MIRROR,
            icon = Icons.Default.SwapHoriz,
            labelRes = R.string.mirror_image
        ),
        TransformGridSpec(
            tool = TransformTool.ASPECT_RATIO,
            icon = Icons.Default.AspectRatio,
            labelRes = R.string.change_aspect_ratio
        )
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 360.dp)
    ) {
        items(options) { spec ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(2.dp)
            ) {
                AdjustmentIconButton(
                    icon = spec.icon,
                    isSelected = false,
                    onClick = { onToolClick(spec.tool) }
                )
                Text(
                    text = stringResource(spec.labelRes),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 2,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private data class TransformGridSpec(
    val tool: TransformTool,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val labelRes: Int
)

@Composable
private fun AspectRatioOptionsPanel(
    onBack: () -> Unit,
    onResetToOriginal: () -> Unit,
    onSelect: (ratio: Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PanelHeader(
            title = stringResource(R.string.change_aspect_ratio),
            onBack = onBack,
            compact = true
        )

        val options = listOf(
            AspectSpec(label = stringResource(R.string.original), ratio = 0f, isOriginal = true),
            AspectSpec(label = "1:1", ratio = 1f),
            AspectSpec(label = "4:3", ratio = 4f / 3f),
            AspectSpec(label = "3:4", ratio = 3f / 4f),
            AspectSpec(label = "16:9", ratio = 16f / 9f),
            AspectSpec(label = "9:16", ratio = 9f / 16f),
            AspectSpec(label = "3:2", ratio = 3f / 2f),
            AspectSpec(label = "2:3", ratio = 2f / 3f)
        )

        // 3 buttons per row
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(options) { spec ->
                OutlinedButton(
                    onClick = {
                        if (spec.isOriginal) {
                            onResetToOriginal()
                        } else {
                            onSelect(spec.ratio)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = spec.label,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

private data class AspectSpec(
    val label: String,
    val ratio: Float,
    val isOriginal: Boolean = false
)

@Composable
private fun PanelHeader(
    title: String,
    onBack: () -> Unit,
    compact: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = if (compact) 0.dp else 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(if (compact) 18.dp else 24.dp)
            )
        }
    }
}

