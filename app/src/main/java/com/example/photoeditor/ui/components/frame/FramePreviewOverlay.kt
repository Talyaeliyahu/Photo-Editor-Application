package com.example.photoeditor.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.photoeditor.model.FrameConfig
import com.example.photoeditor.model.FrameType
import kotlin.math.min

/**
 * Non-destructive frame preview overlay.
 * 
 * This component displays a frame preview on top of the image WITHOUT modifying the bitmap.
 * The actual frame is applied to the bitmap only when saving (via FrameRenderer).
 * 
 * @param imageRect The rectangle where the image is displayed (in Compose coordinates)
 * @param config Frame configuration to preview
 * @param modifier Modifier for the overlay
 */
@Composable
fun FramePreviewOverlay(
    imageRect: Rect,
    config: FrameConfig,
    modifier: Modifier = Modifier
) {
    if (config.type == FrameType.NONE) return
    if (imageRect.width <= 0f || imageRect.height <= 0f) return
    
    when (config.type) {
        FrameType.STROKE -> {
            drawStrokeFramePreview(
                imageRect = imageRect,
                config = config,
                modifier = modifier
            )
        }
        
        FrameType.IMAGE -> {
            drawImageFramePreview(
                imageRect = imageRect,
                config = config,
                modifier = modifier
            )
        }
        
        FrameType.NONE -> {
            // Already handled above
        }
    }
}

@Composable
private fun drawStrokeFramePreview(
    imageRect: Rect,
    config: FrameConfig,
    modifier: Modifier
) {
    Canvas(modifier = modifier) {
        val bitmapWidth = imageRect.width
        val bitmapHeight = imageRect.height
        val minDimension = min(bitmapWidth, bitmapHeight)
        
        // Calculate frame thickness as percentage of bitmap width
        val thickness = (bitmapWidth * config.thicknessPercent.coerceIn(0.005f, 0.1f))
        
        if (config.cornerRadiusPercent > 0f) {
            // Rounded rectangle frame
            val cornerRadius = bitmapWidth * config.cornerRadiusPercent.coerceIn(0f, 0.2f)
            val inset = thickness / 2f
            
            drawRoundRect(
                color = Color(config.color),
                topLeft = Offset(imageRect.left + inset, imageRect.top + inset),
                size = androidx.compose.ui.geometry.Size(
                    bitmapWidth - thickness,
                    bitmapHeight - thickness
                ),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius),
                style = Stroke(width = thickness)
            )
        } else {
            // Square frame - draw four sides
            // Top
            drawRect(
                color = Color(config.color),
                topLeft = Offset(imageRect.left, imageRect.top),
                size = androidx.compose.ui.geometry.Size(bitmapWidth, thickness)
            )
            
            // Bottom
            drawRect(
                color = Color(config.color),
                topLeft = Offset(imageRect.left, imageRect.bottom - thickness),
                size = androidx.compose.ui.geometry.Size(bitmapWidth, thickness)
            )
            
            // Left
            drawRect(
                color = Color(config.color),
                topLeft = Offset(imageRect.left, imageRect.top),
                size = androidx.compose.ui.geometry.Size(thickness, bitmapHeight)
            )
            
            // Right
            drawRect(
                color = Color(config.color),
                topLeft = Offset(imageRect.right - thickness, imageRect.top),
                size = androidx.compose.ui.geometry.Size(thickness, bitmapHeight)
            )
        }
    }
}

@Composable
private fun drawImageFramePreview(
    imageRect: Rect,
    config: FrameConfig,
    modifier: Modifier
) {
    val density = LocalDensity.current
    val frameResId = config.frameImageResId ?: return
    
    // Convert imageRect from pixels to dp
    val offsetX = with(density) { imageRect.left.toDp() }
    val offsetY = with(density) { imageRect.top.toDp() }
    val width = with(density) { imageRect.width.toDp() }
    val height = with(density) { imageRect.height.toDp() }
    
    Box(modifier = modifier) {
        Image(
            painter = painterResource(id = frameResId),
            contentDescription = null,
            modifier = Modifier
                .offset(x = offsetX, y = offsetY)
                .size(width = width, height = height),
            contentScale = ContentScale.FillBounds
        )
    }
}
