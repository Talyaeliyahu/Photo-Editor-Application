package com.example.photoeditor.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.photoeditor.R
import com.example.photoeditor.model.FrameConfig
import com.example.photoeditor.model.FrameType
import android.graphics.Color as AndroidColor

/**
 * Frame selection panel.
 * 
 * This is a placeholder panel - you can extend it with:
 * - Frame style selection (stroke vs image)
 * - Color picker for stroke frames
 * - Thickness slider
 * - Corner radius slider
 * - Image frame gallery
 */
@Composable
fun FrameToolPanel(
    currentConfig: FrameConfig,
    onConfigChange: (FrameConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCustomPanel by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 4.dp
    ) {
        if (showCustomPanel) {
            CustomFramePanel(
                currentConfig = currentConfig,
                onConfigChange = onConfigChange,
                onBack = { showCustomPanel = false }
            )
        } else {
            FrameGridContent(
                currentConfig = currentConfig,
                onConfigChange = onConfigChange,
                onCustomClick = { showCustomPanel = true }
            )
        }
    }
}

/**
 * Custom frame panel for use in a bottom sheet (e.g. when "Custom" is tapped from FramesHorizontalBar).
 */
@Composable
fun FrameCustomSheetContent(
    currentConfig: FrameConfig,
    onConfigChange: (FrameConfig) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    CustomFramePanel(
        currentConfig = currentConfig,
        onConfigChange = onConfigChange,
        onBack = onDismiss,
        modifier = modifier
    )
}

@Composable
private fun CustomFramePanel(
    currentConfig: FrameConfig,
    onConfigChange: (FrameConfig) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val customColor = if (currentConfig.type == FrameType.STROKE) currentConfig.color else AndroidColor.WHITE
    val customThickness = when (currentConfig.thicknessPercent) {
        0.02f -> ThicknessOption.THIN
        0.04f -> ThicknessOption.MEDIUM
        0.06f -> ThicknessOption.THICK
        else -> ThicknessOption.MEDIUM
    }
    var selectedColor by remember(customColor) { mutableStateOf(customColor) }
    var selectedThickness by remember(customThickness) { mutableStateOf(customThickness) }
    var isRounded by remember { mutableStateOf(currentConfig.cornerRadiusPercent > 0f) }

    val colors = listOf(
        Color.Black, Color.White, Color.Red, Color(0xFFFF9800), Color.Yellow, Color.Green,
        Color.Cyan, Color.Blue, Color(0xFF9C27B0), Color(0xFFE91E63), Color(0xFF00BCD4),
        Color(0xFF4CAF50), Color(0xFFFFEB3B), Color(0xFF795548), Color(0xFF607D8B)
    )

    fun applyCustomConfig() {
        val config = FrameConfig(
            type = FrameType.STROKE,
            thicknessPercent = when (selectedThickness) {
                ThicknessOption.THIN -> 0.02f
                ThicknessOption.MEDIUM -> 0.04f
                ThicknessOption.THICK -> 0.06f
            },
            color = selectedColor,
            cornerRadiusPercent = if (isRounded) 0.05f else 0f
        )
        onConfigChange(config)
    }

    LaunchedEffect(Unit) {
        applyCustomConfig()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.frame_back))
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = stringResource(R.string.frame_thickness),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.widthIn(min = 50.dp)
                )
                ThicknessOption.entries.forEach { opt ->
                    FilterChip(
                        selected = selectedThickness == opt,
                        onClick = {
                            selectedThickness = opt
                            applyCustomConfig()
                        },
                        label = {
                            Text(
                                when (opt) {
                                    ThicknessOption.THIN -> stringResource(R.string.frame_thin)
                                    ThicknessOption.MEDIUM -> stringResource(R.string.frame_medium)
                                    ThicknessOption.THICK -> stringResource(R.string.frame_thick)
                                },
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        modifier = Modifier.widthIn(min = 55.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.frame_shape),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.widthIn(min = 50.dp)
            )
            FilterChip(
                selected = isRounded,
                onClick = {
                    isRounded = true
                    applyCustomConfig()
                },
                label = {
                    Text(
                        stringResource(R.string.frame_rounded),
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                modifier = Modifier.widthIn(min = 55.dp)
            )
            FilterChip(
                selected = !isRounded,
                onClick = {
                    isRounded = false
                    applyCustomConfig()
                },
                label = {
                    Text(
                        stringResource(R.string.frame_regular),
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                modifier = Modifier.widthIn(min = 55.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 0.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.frame_color),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.widthIn(min = 36.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                colors.forEach { color ->
                    val colorArgb = color.toArgb()
                    val border = when {
                        selectedColor == colorArgb -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                        colorArgb == AndroidColor.BLACK -> BorderStroke(1.dp, Color.White)
                        else -> null
                    }
                    Surface(
                        shape = CircleShape,
                        color = color,
                        modifier = Modifier.size(24.dp),
                        onClick = {
                            selectedColor = colorArgb
                            applyCustomConfig()
                        },
                        border = border
                    ) {}
                }
            }
        }
    }
}

private enum class ThicknessOption { THIN, MEDIUM, THICK }

@Composable
private fun FrameGridContent(
    currentConfig: FrameConfig,
    onConfigChange: (FrameConfig) -> Unit,
    onCustomClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val frameOptions = listOf(
            FrameOption(
                name = stringResource(R.string.frame_none),
                config = FrameConfig.NONE,
                isCustomButton = false
            ),
            FrameOption(
                name = stringResource(R.string.frame_custom),
                config = null,
                isCustomButton = true
            ),
        ) + listOf(
            // Image frames (PNG/WEBP)
                FrameOption(
                    name = stringResource(R.string.frame_blackwhite),
                    config = FrameConfig(
                        type = FrameType.IMAGE,
                        frameImageResId = R.drawable.frame_blackwhite,
                        useNinePatch = false
                    )
                ),
                FrameOption(
                    name = stringResource(R.string.frame_border2),
                    config = FrameConfig(
                        type = FrameType.IMAGE,
                        frameImageResId = R.drawable.border2,
                        useNinePatch = false
                    )
                ),
                FrameOption(
                    name = stringResource(R.string.frame_f159),
                    config = FrameConfig(
                        type = FrameType.IMAGE,
                        frameImageResId = R.drawable.f159,
                        useNinePatch = false
                    )
                ),
                FrameOption(
                    name = stringResource(R.string.frame_100),
                    config = FrameConfig(
                        type = FrameType.IMAGE,
                        frameImageResId = R.drawable.frame100,
                        useNinePatch = false
                    )
                ),
                FrameOption(
                    name = stringResource(R.string.frame_101),
                    config = FrameConfig(
                        type = FrameType.IMAGE,
                        frameImageResId = R.drawable.frame101,
                        useNinePatch = false
                    )
                ),
                FrameOption(
                    name = stringResource(R.string.frame_103),
                    config = FrameConfig(
                        type = FrameType.IMAGE,
                        frameImageResId = R.drawable.frame103,
                        useNinePatch = false
                    )
                ),
                FrameOption(
                    name = stringResource(R.string.frame_104),
                    config = FrameConfig(
                        type = FrameType.IMAGE,
                        frameImageResId = R.drawable.frame104,
                        useNinePatch = false
                    )
                ),
                FrameOption(
                    name = stringResource(R.string.frame_105),
                    config = FrameConfig(
                        type = FrameType.IMAGE,
                        frameImageResId = R.drawable.frame105,
                        useNinePatch = false
                    )
                ),
                FrameOption(
                    name = stringResource(R.string.frame_106),
                    config = FrameConfig(
                        type = FrameType.IMAGE,
                        frameImageResId = R.drawable.frame106,
                        useNinePatch = false
                    )
                ),
                FrameOption(
                    name = stringResource(R.string.frame_107),
                    config = FrameConfig(
                        type = FrameType.IMAGE,
                        frameImageResId = R.drawable.frame107,
                        useNinePatch = false
                    )
                ),
                FrameOption(
                    name = stringResource(R.string.frame_108),
                    config = FrameConfig(
                        type = FrameType.IMAGE,
                        frameImageResId = R.drawable.frame108,
                        useNinePatch = false
                    )
                ),
                FrameOption(
                    name = stringResource(R.string.frame_109),
                    config = FrameConfig(
                        type = FrameType.IMAGE,
                        frameImageResId = R.drawable.frame109,
                        useNinePatch = false
                    )
                ),
                FrameOption(
                    name = stringResource(R.string.frame_110),
                    config = FrameConfig(
                        type = FrameType.IMAGE,
                        frameImageResId = R.drawable.frame110,
                        useNinePatch = false
                    )
                ),
                FrameOption(
                    name = stringResource(R.string.frame_111),
                    config = FrameConfig(
                        type = FrameType.IMAGE,
                        frameImageResId = R.drawable.frame111,
                        useNinePatch = false
                    )
                ),
                FrameOption(
                    name = stringResource(R.string.frame_112),
                    config = FrameConfig(
                        type = FrameType.IMAGE,
                        frameImageResId = R.drawable.frame112,
                        useNinePatch = false
                    )
                ),
                FrameOption(
                    name = stringResource(R.string.frame_113),
                    config = FrameConfig(
                        type = FrameType.IMAGE,
                        frameImageResId = R.drawable.frame113,
                        useNinePatch = false
                    )
                ),
                FrameOption(
                    name = stringResource(R.string.frame_114),
                    config = FrameConfig(
                        type = FrameType.IMAGE,
                        frameImageResId = R.drawable.frame114,
                        useNinePatch = false
                    )
                ),
                FrameOption(
                    name = stringResource(R.string.frame_115),
                    config = FrameConfig(
                        type = FrameType.IMAGE,
                        frameImageResId = R.drawable.frame115,
                        useNinePatch = false
                    )
                ),
                FrameOption(
                    name = stringResource(R.string.frame_116),
                    config = FrameConfig(
                        type = FrameType.IMAGE,
                        frameImageResId = R.drawable.frame116,
                        useNinePatch = false
                    )
                ),
                FrameOption(
                    name = stringResource(R.string.frame_117),
                    config = FrameConfig(
                        type = FrameType.IMAGE,
                        frameImageResId = R.drawable.frame117,
                        useNinePatch = false
                    )
                ),
            )
            
            // Display frames in a grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
            ) {
                items(
                    items = frameOptions,
                    key = { "${it.name}_${it.isCustomButton}" }
                ) { option ->
                    val isSelected = !option.isCustomButton && option.config != null && isConfigEqual(currentConfig, option.config)
                    OutlinedButton(
                        onClick = {
                            if (option.isCustomButton) {
                                onCustomClick()
                            } else {
                                option.config?.let { onConfigChange(it) }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        border = if (isSelected) {
                            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                        } else {
                            ButtonDefaults.outlinedButtonBorder
                        }
                    ) {
                        Text(
                            text = option.name,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
}

/**
 * Helper data class for frame options in the UI
 */
private data class FrameOption(
    val name: String,
    val config: FrameConfig?,
    val isCustomButton: Boolean = false
)

/**
 * Compares two FrameConfig objects for equality
 */
private fun isConfigEqual(config1: FrameConfig, config2: FrameConfig): Boolean {
    return config1.type == config2.type &&
            config1.thicknessPercent == config2.thicknessPercent &&
            config1.color == config2.color &&
            config1.cornerRadiusPercent == config2.cornerRadiusPercent &&
            config1.frameImageResId == config2.frameImageResId &&
            config1.useNinePatch == config2.useNinePatch
}
