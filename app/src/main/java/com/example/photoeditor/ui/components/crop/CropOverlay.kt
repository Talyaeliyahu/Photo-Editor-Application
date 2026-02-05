package com.example.photoeditor.ui.components

import android.graphics.Bitmap
import android.graphics.Rect
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect as UiRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.photoeditor.R
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Interactive crop overlay (iPhone-like): resize/move crop frame directly on the image,
 * then confirm to apply crop.
 */
@Composable
fun CropOverlay(
    bitmap: Bitmap?,
    onCancel: () -> Unit,
    onConfirm: (Rect) -> Unit,
    modifier: Modifier = Modifier
) {
    if (bitmap == null) return

    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    // Crop rect in view (pixel) coordinates
    var cropRect by remember(bitmap, containerSize) { mutableStateOf(UiRect.Zero) }

    // Compute displayed image rect within the container (ContentScale.Fit, centered)
    val imageRect = remember(bitmap, containerSize) {
        computeFitRect(
            containerSize = containerSize,
            bitmapWidth = bitmap.width,
            bitmapHeight = bitmap.height
        )
    }

    LaunchedEffect(bitmap, containerSize) {
        if (containerSize.width == 0 || containerSize.height == 0) return@LaunchedEffect
        if (imageRect.width <= 0f || imageRect.height <= 0f) return@LaunchedEffect
        // Init crop rect to a slightly inset image rect
        val inset = min(imageRect.width, imageRect.height) * 0.08f
        cropRect = UiRect(
            left = imageRect.left + inset,
            top = imageRect.top + inset,
            right = imageRect.right - inset,
            bottom = imageRect.bottom - inset
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it }
    ) {
        // Dimming + crop frame
        CropFrameLayer(
            imageRect = imageRect,
            cropRect = cropRect,
            onCropRectChange = { cropRect = it },
            modifier = Modifier
                .matchParentSize()
                // IMPORTANT: do NOT key this on cropRect, otherwise the gesture detector
                // restarts on every small update and feels "stuttery".
                .pointerInput(bitmap, containerSize, imageRect) {
                    detectCropGestures(
                        imageRect = imageRect,
                        getRect = { cropRect },
                        setRect = { cropRect = it }
                    )
                }
        )

        // Bottom actions
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.35f))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(onClick = onCancel) {
                Text(stringResource(R.string.cancel))
            }
            OutlinedButton(
                onClick = {
                    val rectPx = cropRectToBitmapRect(
                        cropRect = cropRect,
                        imageRect = imageRect,
                        bitmapWidth = bitmap.width,
                        bitmapHeight = bitmap.height
                    )
                    onConfirm(rectPx)
                }
            ) {
                Text(stringResource(R.string.save))
            }
        }
    }
}

@Composable
private fun CropFrameLayer(
    imageRect: UiRect,
    cropRect: UiRect,
    onCropRectChange: (UiRect) -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = MaterialTheme.colorScheme.primary
    val gridColor = Color.White.copy(alpha = 0.55f)
    val dimColor = Color.Black.copy(alpha = 0.55f)

    val strokePx = with(LocalDensity.current) { 2.dp.toPx() }
    val gridStrokePx = with(LocalDensity.current) { 1.dp.toPx() }
    val handleRadiusPx = with(LocalDensity.current) { 6.dp.toPx() }

    Canvas(modifier = modifier) {
        if (imageRect == UiRect.Zero || cropRect == UiRect.Zero) return@Canvas

        // Dim outside crop rect (within imageRect)
        val topDim = UiRect(imageRect.left, imageRect.top, imageRect.right, cropRect.top)
        val bottomDim = UiRect(imageRect.left, cropRect.bottom, imageRect.right, imageRect.bottom)
        val leftDim = UiRect(imageRect.left, cropRect.top, cropRect.left, cropRect.bottom)
        val rightDim = UiRect(cropRect.right, cropRect.top, imageRect.right, cropRect.bottom)
        drawRect(dimColor, topLeft = topDim.topLeft, size = topDim.size)
        drawRect(dimColor, topLeft = bottomDim.topLeft, size = bottomDim.size)
        drawRect(dimColor, topLeft = leftDim.topLeft, size = leftDim.size)
        drawRect(dimColor, topLeft = rightDim.topLeft, size = rightDim.size)

        // Border
        drawRect(
            color = borderColor,
            topLeft = cropRect.topLeft,
            size = cropRect.size,
            style = Stroke(width = strokePx)
        )

        // 3x3 grid
        val thirdW = cropRect.width / 3f
        val thirdH = cropRect.height / 3f
        for (i in 1..2) {
            val x = cropRect.left + thirdW * i
            drawLine(
                color = gridColor,
                start = Offset(x, cropRect.top),
                end = Offset(x, cropRect.bottom),
                strokeWidth = gridStrokePx
            )
            val y = cropRect.top + thirdH * i
            drawLine(
                color = gridColor,
                start = Offset(cropRect.left, y),
                end = Offset(cropRect.right, y),
                strokeWidth = gridStrokePx
            )
        }

        // Corner handles
        val corners = listOf(
            Offset(cropRect.left, cropRect.top),
            Offset(cropRect.right, cropRect.top),
            Offset(cropRect.left, cropRect.bottom),
            Offset(cropRect.right, cropRect.bottom)
        )
        corners.forEach { c ->
            drawCircle(color = borderColor, radius = handleRadiusPx, center = c)
        }
    }
}

private enum class DragMode {
    NONE,
    MOVE,
    TL,
    TR,
    BL,
    BR
}

private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.detectCropGestures(
    imageRect: UiRect,
    getRect: () -> UiRect,
    setRect: (UiRect) -> Unit
) {
    val touchRadius = 32f
    val minSize = 80f

    var mode = DragMode.NONE

    detectDragGestures(
        onDragStart = { pos ->
            val r = getRect()
            mode = pickMode(pos, r, touchRadius)
        },
        onDragEnd = { mode = DragMode.NONE },
        onDragCancel = { mode = DragMode.NONE },
        onDrag = { change, drag ->
            change.consumeAllChanges()
            val r = getRect()
            val next = when (mode) {
                DragMode.MOVE -> r.translate(drag.x, drag.y)
                DragMode.TL -> UiRect(r.left + drag.x, r.top + drag.y, r.right, r.bottom)
                DragMode.TR -> UiRect(r.left, r.top + drag.y, r.right + drag.x, r.bottom)
                DragMode.BL -> UiRect(r.left + drag.x, r.top, r.right, r.bottom + drag.y)
                DragMode.BR -> UiRect(r.left, r.top, r.right + drag.x, r.bottom + drag.y)
                DragMode.NONE -> r
            }

            val clamped = clampRect(next, imageRect, minSize)
            setRect(clamped)
        }
    )
}

private fun pickMode(pos: Offset, rect: UiRect, radius: Float): DragMode {
    fun near(a: Offset, b: Offset) = abs(a.x - b.x) <= radius && abs(a.y - b.y) <= radius

    val tl = Offset(rect.left, rect.top)
    val tr = Offset(rect.right, rect.top)
    val bl = Offset(rect.left, rect.bottom)
    val br = Offset(rect.right, rect.bottom)

    return when {
        near(pos, tl) -> DragMode.TL
        near(pos, tr) -> DragMode.TR
        near(pos, bl) -> DragMode.BL
        near(pos, br) -> DragMode.BR
        rect.contains(pos) -> DragMode.MOVE
        else -> DragMode.NONE
    }
}

private fun clampRect(rect: UiRect, bounds: UiRect, minSize: Float): UiRect {
    var l = rect.left
    var t = rect.top
    var r = rect.right
    var b = rect.bottom

    // Ensure min size
    if (r - l < minSize) {
        val mid = (l + r) / 2f
        l = mid - minSize / 2f
        r = mid + minSize / 2f
    }
    if (b - t < minSize) {
        val mid = (t + b) / 2f
        t = mid - minSize / 2f
        b = mid + minSize / 2f
    }

    // Clamp to bounds
    val dx = when {
        l < bounds.left -> bounds.left - l
        r > bounds.right -> bounds.right - r
        else -> 0f
    }
    val dy = when {
        t < bounds.top -> bounds.top - t
        b > bounds.bottom -> bounds.bottom - b
        else -> 0f
    }
    l += dx; r += dx
    t += dy; b += dy

    // Final clamp
    l = max(bounds.left, min(l, bounds.right - minSize))
    t = max(bounds.top, min(t, bounds.bottom - minSize))
    r = min(bounds.right, max(r, bounds.left + minSize))
    b = min(bounds.bottom, max(b, bounds.top + minSize))

    return UiRect(l, t, r, b)
}

private fun UiRect.translate(dx: Float, dy: Float): UiRect =
    UiRect(left + dx, top + dy, right + dx, bottom + dy)

private fun computeFitRect(containerSize: IntSize, bitmapWidth: Int, bitmapHeight: Int): UiRect {
    if (containerSize.width == 0 || containerSize.height == 0) return UiRect.Zero
    if (bitmapWidth <= 0 || bitmapHeight <= 0) return UiRect.Zero

    val cw = containerSize.width.toFloat()
    val ch = containerSize.height.toFloat()
    val bitmapAspect = bitmapWidth.toFloat() / bitmapHeight.toFloat()
    val containerAspect = cw / ch

    return if (containerAspect > bitmapAspect) {
        val h = ch
        val w = h * bitmapAspect
        val left = (cw - w) / 2f
        UiRect(left, 0f, left + w, h)
    } else {
        val w = cw
        val h = w / bitmapAspect
        val top = (ch - h) / 2f
        UiRect(0f, top, w, top + h)
    }
}

private fun cropRectToBitmapRect(
    cropRect: UiRect,
    imageRect: UiRect,
    bitmapWidth: Int,
    bitmapHeight: Int
): Rect {
    val iw = imageRect.width.coerceAtLeast(1f)
    val ih = imageRect.height.coerceAtLeast(1f)

    val leftN = ((cropRect.left - imageRect.left) / iw).coerceIn(0f, 1f)
    val topN = ((cropRect.top - imageRect.top) / ih).coerceIn(0f, 1f)
    val rightN = ((cropRect.right - imageRect.left) / iw).coerceIn(0f, 1f)
    val bottomN = ((cropRect.bottom - imageRect.top) / ih).coerceIn(0f, 1f)

    val left = (leftN * bitmapWidth).roundToInt().coerceIn(0, bitmapWidth - 1)
    val top = (topN * bitmapHeight).roundToInt().coerceIn(0, bitmapHeight - 1)
    val right = (rightN * bitmapWidth).roundToInt().coerceIn(left + 1, bitmapWidth)
    val bottom = (bottomN * bitmapHeight).roundToInt().coerceIn(top + 1, bitmapHeight)

    return Rect(left, top, right, bottom)
}

