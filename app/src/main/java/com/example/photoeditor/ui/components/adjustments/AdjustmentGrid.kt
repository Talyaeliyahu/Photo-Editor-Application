package com.example.photoeditor.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
internal fun AdjustmentsGrid(onAdjustmentClick: (AdjustmentType) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 360.dp)
    ) {
        items(adjustmentOrder) { type ->
            AdjustmentGridItem(
                type = type,
                onClick = { onAdjustmentClick(type) }
            )
        }
    }
}

@Composable
private fun AdjustmentGridItem(
    type: AdjustmentType,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(2.dp)
    ) {
        if (type == AdjustmentType.BRIGHTNESS) {
            // Keep the original "sun" icon for brightness
            AdjustmentIconButton(
                icon = Icons.Default.LightMode,
                isSelected = false,
                onClick = onClick
            )
        } else {
            AdjustmentIconButton(
                iconContent = { color ->
                    val iconSize = if (type == AdjustmentType.WARMTH) 28.dp else 24.dp
                    AppleStyleAdjustmentIcon(type = type, tint = color, iconSize = iconSize)
                },
                isSelected = false,
                onClick = onClick
            )
        }

        Text(
            text = labelForAdjustment(type),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 2,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

