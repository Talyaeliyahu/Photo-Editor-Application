package com.example.photoeditor.ui.components.collage

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BorderColor
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.photoeditor.R

/**
 * Bottom navigation bar for collage editor
 */
@Composable
fun CollageBottomNavigationBar(
    selectedCategory: CollagePanelCategory,
    onCategorySelected: (CollagePanelCategory) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Black,
        shadowElevation = 8.dp
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            item {
                CollageNavButton(
                    category = CollagePanelCategory.TEMPLATES,
                    label = stringResource(R.string.templates),
                    icon = Icons.Default.GridOn,
                    isSelected = selectedCategory == CollagePanelCategory.TEMPLATES,
                    onClick = { onCategorySelected(CollagePanelCategory.TEMPLATES) }
                )
            }
            item {
                CollageNavButton(
                    category = CollagePanelCategory.BORDERS,
                    label = stringResource(R.string.border),
                    icon = Icons.Default.BorderColor,
                    isSelected = selectedCategory == CollagePanelCategory.BORDERS,
                    onClick = { onCategorySelected(CollagePanelCategory.BORDERS) }
                )
            }
        }
    }
}

/**
 * Navigation button for collage bottom bar
 */
@Composable
fun CollageNavButton(
    category: CollagePanelCategory,
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(80.dp)
            .pointerInput(Unit) {
                detectTapGestures { onClick() }
            }
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(48.dp)
                .background(
                    color = if (isSelected) Color(0xFF333333) else Color(0xFF252525),
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) Color.White else Color(0xFFB0B0B0),
                modifier = Modifier.size(24.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) Color.White else Color(0xFFB0B0B0),
            fontSize = 10.sp,
            textAlign = TextAlign.Center
        )
    }
}
