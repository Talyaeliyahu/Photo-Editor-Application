package com.example.photoeditor.ui.components.draw

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import com.example.photoeditor.model.DrawPath

/**
 * Draws the paths on screen without capturing touches. Used under stickers/text so it doesn't block.
 */
@Composable
fun DrawingPathsCanvas(
    drawPaths: List<DrawPath>,
    imageRect: Rect,
    modifier: Modifier = Modifier
) {
    if (imageRect.width <= 0f || imageRect.height <= 0f) return
    Canvas(modifier = modifier.fillMaxSize()) {
        for (path in drawPaths) {
            if (path.points.size < 2) continue
            val pathStrokeWidth = path.strokeWidthNorm * imageRect.width
            val composePath = Path().apply {
                moveTo(
                    imageRect.left + path.points[0].x * imageRect.width,
                    imageRect.top + path.points[0].y * imageRect.height
                )
                for (i in 1 until path.points.size) {
                    lineTo(
                        imageRect.left + path.points[i].x * imageRect.width,
                        imageRect.top + path.points[i].y * imageRect.height
                    )
                }
            }
            drawPath(
                path = composePath,
                color = Color(path.color),
                style = Stroke(width = pathStrokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
            )
        }
    }
}

@Composable
fun DrawingOverlay(
    drawPaths: List<DrawPath>,
    imageRect: Rect,
    strokeWidthNorm: Float,
    drawColorArgb: Int,
    enabled: Boolean,
    onStartPath: (normX: Float, normY: Float) -> Unit,
    onAddPoint: (normX: Float, normY: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    if (imageRect.width <= 0f || imageRect.height <= 0f) return

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(enabled, imageRect) {
                if (!enabled) return@pointerInput
                detectDragGestures(
                    onDragStart = { offset ->
                        val normX = ((offset.x - imageRect.left) / imageRect.width).coerceIn(0f, 1f)
                        val normY = ((offset.y - imageRect.top) / imageRect.height).coerceIn(0f, 1f)
                        onStartPath(normX, normY)
                    },
                    onDrag = { change, _ ->
                        change.consumeAllChanges()
                        val offset = change.position
                        val normX = ((offset.x - imageRect.left) / imageRect.width).coerceIn(0f, 1f)
                        val normY = ((offset.y - imageRect.top) / imageRect.height).coerceIn(0f, 1f)
                        onAddPoint(normX, normY)
                    }
                )
            }
    ) {
        for (path in drawPaths) {
            if (path.points.size < 2) continue
            val pathStrokeWidth = path.strokeWidthNorm * imageRect.width
            val composePath = Path().apply {
                moveTo(
                    imageRect.left + path.points[0].x * imageRect.width,
                    imageRect.top + path.points[0].y * imageRect.height
                )
                for (i in 1 until path.points.size) {
                    lineTo(
                        imageRect.left + path.points[i].x * imageRect.width,
                        imageRect.top + path.points[i].y * imageRect.height
                    )
                }
            }
            drawPath(
                path = composePath,
                color = Color(path.color),
                style = Stroke(width = pathStrokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
            )
        }
    }
}
