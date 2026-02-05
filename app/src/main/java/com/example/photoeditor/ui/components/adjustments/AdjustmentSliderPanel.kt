package com.example.photoeditor.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.photoeditor.R

@Composable
internal fun AdjustmentSliderPanel(
    adjustmentType: AdjustmentType,
    initialValue: Float,
    onValueChange: (Float) -> Unit,
    onConfirm: (Float) -> Unit,
    onCancel: (Float) -> Unit
) {
    val originalValue = remember(adjustmentType) { initialValue }

    val valueRange = when (adjustmentType) {
        AdjustmentType.BRIGHTNESS -> -50f..50f
        AdjustmentType.CONTRAST -> -100f..100f
        AdjustmentType.SATURATION -> -100f..100f
        AdjustmentType.EXPOSURE -> -100f..100f
        AdjustmentType.GLOW -> 0f..100f
        AdjustmentType.HIGHLIGHTS -> -100f..100f
        AdjustmentType.SHADOWS -> -100f..100f
        AdjustmentType.VIBRANCE -> -100f..100f
        AdjustmentType.WARMTH -> -100f..100f
        AdjustmentType.TINT -> -100f..100f
        AdjustmentType.HUE -> -100f..100f
        AdjustmentType.SHARPNESS -> 0f..100f
        AdjustmentType.DEFINITION -> 0f..100f
        AdjustmentType.VIGNETTE -> 0f..100f
    }

    var tempValue by remember(adjustmentType) {
        mutableStateOf(initialValue.coerceIn(valueRange.start, valueRange.endInclusive))
    }

    LaunchedEffect(adjustmentType) {
        tempValue = initialValue.coerceIn(valueRange.start, valueRange.endInclusive)
    }

    val label = labelForAdjustment(adjustmentType)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (adjustmentType == AdjustmentType.BRIGHTNESS) {
                    Icon(
                        imageVector = Icons.Default.LightMode,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                } else {
                    val iconSize = if (adjustmentType == AdjustmentType.WARMTH) 28.dp else 24.dp
                    AppleStyleAdjustmentIcon(
                        type = adjustmentType,
                        tint = MaterialTheme.colorScheme.primary,
                        iconSize = iconSize
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "${tempValue.toInt()}",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }

        Slider(
            value = tempValue,
            onValueChange = { newValue ->
                tempValue = newValue
                onValueChange(newValue)
            },
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(
                onClick = {
                    val defaultValue = 0f
                    tempValue = defaultValue
                    onValueChange(defaultValue)
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier
                        .size(48.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = label,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            IconButton(
                onClick = { onCancel(originalValue) },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier
                        .size(48.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            IconButton(
                onClick = { onConfirm(tempValue) },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .size(48.dp)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

