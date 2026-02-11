package com.example.photoeditor.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Single row of circular adjustment items with horizontal scroll.
 * Used in portrait mode above the bottom navigation bar.
 */
@Composable
fun AdjustmentsHorizontalBar(
    selectedAdjustment: AdjustmentType?,
    onAdjustmentClick: (AdjustmentType) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxHeight(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        items(adjustmentOrder) { type ->
            AdjustmentHorizontalItem(
                type = type,
                isSelected = selectedAdjustment == type,
                onClick = { onAdjustmentClick(type) }
            )
        }
    }
}

@Composable
private fun AdjustmentHorizontalItem(
    type: AdjustmentType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(2.dp)
    ) {
        if (type == AdjustmentType.BRIGHTNESS) {
            AdjustmentIconButton(
                icon = Icons.Default.LightMode,
                isSelected = isSelected,
                onClick = onClick
            )
        } else {
            AdjustmentIconButton(
                iconContent = { color ->
                    val iconSize = if (type == AdjustmentType.WARMTH) 28.dp else 24.dp
                    AppleStyleAdjustmentIcon(type = type, tint = color, iconSize = iconSize)
                },
                isSelected = isSelected,
                onClick = onClick
            )
        }

        Text(
            text = labelForAdjustment(type),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            maxLines = 1,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
    }
}
