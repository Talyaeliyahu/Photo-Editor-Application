package com.example.photoeditor.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.FilterFrames
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.photoeditor.R

/**
 * Enum for menu categories
 */
enum class MenuCategory {
    ADJUSTMENTS, FILTERS, TRANSFORM, TEXT, STICKERS, DRAW, FRAMES, NONE
}

/**
 * Bottom navigation bar with category buttons
 */
@Composable
fun BottomNavigationBar(
    selectedCategory: MenuCategory,
    onCategorySelected: (MenuCategory) -> Unit
) {
    val navItems = listOf(
        BottomNavItem(
            category = MenuCategory.ADJUSTMENTS,
            labelRes = R.string.adjustments,
            icon = Icons.Default.Tune
        ),
        BottomNavItem(
            category = MenuCategory.FILTERS,
            labelRes = R.string.filters,
            icon = Icons.Default.AutoFixHigh
        ),
        BottomNavItem(
            category = MenuCategory.TRANSFORM,
            labelRes = R.string.crop_tools,
            icon = Icons.Default.Crop
        ),
        BottomNavItem(
            category = MenuCategory.TEXT,
            labelRes = R.string.text_tools,
            icon = Icons.Default.TextFields
        ),
        BottomNavItem(
            category = MenuCategory.STICKERS,
            labelRes = R.string.stickers_tools,
            icon = Icons.Default.EmojiEmotions
        ),
        BottomNavItem(
            category = MenuCategory.DRAW,
            labelRes = R.string.draw_tools,
            icon = Icons.Default.Brush
        ),
        BottomNavItem(
            category = MenuCategory.FRAMES,
            labelRes = R.string.frames_tools,
            icon = Icons.Default.FilterFrames
        )
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentPadding = PaddingValues(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(navItems) { item ->
                BottomNavIconItem(
                    label = stringResource(item.labelRes),
                    icon = item.icon,
                    isSelected = selectedCategory == item.category,
                    onClick = { onCategorySelected(item.category) }
                )
            }
        }
    }
}

/**
 * Category icon item in bottom navigation
 */
@Composable
private fun BottomNavIconItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val border = BorderStroke(
        width = if (isSelected) 2.dp else 1.dp,
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    )

    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.widthIn(min = 66.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = containerColor,
            contentColor = contentColor,
            border = border,
            modifier = Modifier.size(46.dp),
            onClick = onClick
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            maxLines = 2,
            modifier = Modifier.widthIn(max = 86.dp)
        )
    }
}

private data class BottomNavItem(
    val category: MenuCategory,
    val labelRes: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)
