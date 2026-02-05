package com.example.photoeditor.ui.components.collage

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.photoeditor.model.CollageTemplate

/**
 * Visual preview of a template
 */
@Composable
fun TemplatePreviewItem(
    template: CollageTemplate,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.aspectRatio(1f),
        shape = RectangleShape,
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, Color.White)
        } else {
            null
        },
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A1A)
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Draw template preview
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cellColor = Color.White.copy(alpha = 0.3f)
                val strokeWidth = 2.dp.toPx()
                
                template.cells.forEach { cell ->
                    val x = cell.x * size.width
                    val y = cell.y * size.height
                    val width = cell.width * size.width
                    val height = cell.height * size.height
                    
                    drawRect(
                        color = cellColor,
                        topLeft = Offset(x, y),
                        size = Size(width, height),
                        style = Stroke(width = strokeWidth)
                    )
                }
            }
        }
    }
}
