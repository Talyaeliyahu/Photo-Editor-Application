package com.example.photoeditor.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.photoeditor.model.TextOverlay
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.PI

private enum class TransformMode { NONE, MOVE, SCALE, ROTATE }

@Composable
internal fun EditableTextOverlayItem(
    overlay: TextOverlay,
    isSelected: Boolean,
    isEditing: Boolean,
    imageRect: Rect,
    zoomScale: Float,
    onSelect: (String) -> Unit,
    onMoveBy: (dxNorm: Float, dyNorm: Float) -> Unit,
    onRotateTo: (degrees: Float) -> Unit,
    onScaleAndPositionTo: (sizeNorm: Float, xNorm: Float, yNorm: Float) -> Unit
) {
    if (overlay.text.isBlank()) return
    if (imageRect.width <= 0f || imageRect.height <= 0f) return

    val density = LocalDensity.current
    val minPx = min(imageRect.width, imageRect.height).coerceAtLeast(1f)
    val fontSize = with(density) { (overlay.sizeNorm.coerceIn(0.02f, 0.20f) * minPx).toSp() }

    val textX = imageRect.left + (overlay.xNorm * imageRect.width)
    val textY = imageRect.top + (overlay.yNorm * imageRect.height)

    // Layout constants (in px)
    val padSidePx = with(density) { 14.dp.toPx() }
    val padBottomPx = with(density) { 12.dp.toPx() }
    val padTopPx = with(density) { 36.dp.toPx() } // includes room for rotate handle above the box
    val handleRadiusPx = with(density) { 6.dp.toPx() }
    val handleTouchRadiusPx = with(density) { 28.dp.toPx() }
    val rotateOffsetPx = with(density) { 22.dp.toPx() }
    val rotateIconSize = 18.dp

    // IMPORTANT: don't key these on fontSize; otherwise size resets to 0 mid-edit and gestures stop.
    var containerSize by remember(overlay.id) { mutableStateOf(IntSize.Zero) }
    var textSizePx by remember(overlay.id) { mutableStateOf(IntSize.Zero) }

    val selectionRect = Rect(
        left = padSidePx,
        top = padTopPx,
        right = padSidePx + textSizePx.width,
        bottom = padTopPx + textSizePx.height
    )

    val pivotX = if (containerSize.width > 0) {
        ((padSidePx + textSizePx.width / 2f) / containerSize.width.toFloat()).coerceIn(0f, 1f)
    } else 0.5f
    val pivotY = if (containerSize.height > 0) {
        ((padTopPx + textSizePx.height / 2f) / containerSize.height.toFloat()).coerceIn(0f, 1f)
    } else 0.5f

    val center = Offset(
        x = selectionRect.left + selectionRect.width / 2f,
        y = selectionRect.top + selectionRect.height / 2f
    )
    val rotateHandleCenter = Offset(center.x, selectionRect.top - rotateOffsetPx)

    // Feed updated geometry into the long-lived pointerInput coroutine without restarting it.
    val currentOverlay by rememberUpdatedState(overlay)
    val currentImageRect by rememberUpdatedState(imageRect)
    val currentZoomScale by rememberUpdatedState(zoomScale)
    val currentTextSizePx by rememberUpdatedState(textSizePx)
    val currentSelectionRect by rememberUpdatedState(selectionRect)
    val currentCenter by rememberUpdatedState(center)
    val currentRotateHandleCenter by rememberUpdatedState(rotateHandleCenter)

    var mode by remember(overlay.id) { mutableStateOf(TransformMode.NONE) }
    var startAngleRad by remember(overlay.id) { mutableStateOf(0f) }
    var startRotation by remember(overlay.id) { mutableStateOf(0f) }
    var startDist by remember(overlay.id) { mutableStateOf(1f) }
    var startSizeNorm by remember(overlay.id) { mutableStateOf(overlay.sizeNorm) }
    var startTextWH by remember(overlay.id) { mutableStateOf(IntSize.Zero) }
    var startXYNorm by remember(overlay.id) { mutableStateOf(overlay.xNorm to overlay.yNorm) }
    var startCenter by remember(overlay.id) { mutableStateOf(Offset.Zero) }
    var startCenterNorm by remember(overlay.id) { mutableStateOf(0.5f to 0.5f) }

    fun near(p: Offset, c: Offset) =
        hypot((p.x - c.x).toDouble(), (p.y - c.y).toDouble()) <= handleTouchRadiusPx

    fun cornerCenters(): List<Offset> = listOf(
        Offset(currentSelectionRect.left, currentSelectionRect.top),
        Offset(currentSelectionRect.right, currentSelectionRect.top),
        Offset(currentSelectionRect.left, currentSelectionRect.bottom),
        Offset(currentSelectionRect.right, currentSelectionRect.bottom)
    )

    Box(
        modifier = Modifier
            .offset { IntOffset((textX - padSidePx).roundToInt(), (textY - padTopPx).roundToInt()) }
            .wrapContentSize()
            .onSizeChanged { containerSize = it }
            .graphicsLayer(
                rotationZ = overlay.rotationDegrees,
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(pivotX, pivotY)
            )
            .pointerInput(isEditing, overlay.id) {
                if (!isEditing) return@pointerInput
                detectTapGestures(
                    onTap = { onSelect(overlay.id) }
                )
            }
            .pointerInput(isEditing, isSelected, overlay.id) {
                if (!isEditing || !isSelected) return@pointerInput
                detectDragGestures(
                    onDragStart = { pos ->
                        onSelect(overlay.id)

                        // Freeze geometry for the duration of the gesture (prevents jitter while state updates).
                        val ts = currentTextSizePx
                        if (ts.width <= 0 || ts.height <= 0) {
                            mode = TransformMode.NONE
                            return@detectDragGestures
                        }

                        startCenter = currentCenter
                        mode = when {
                            near(pos, currentRotateHandleCenter) -> TransformMode.ROTATE
                            cornerCenters().any { near(pos, it) } -> TransformMode.SCALE
                            currentSelectionRect.contains(pos) -> TransformMode.MOVE
                            else -> TransformMode.NONE
                        }

                        if (mode == TransformMode.ROTATE) {
                            startRotation = currentOverlay.rotationDegrees
                            startAngleRad = atan2(pos.y - startCenter.y, pos.x - startCenter.x)
                        } else if (mode == TransformMode.SCALE) {
                            startSizeNorm = currentOverlay.sizeNorm
                            startTextWH = ts
                            startXYNorm = currentOverlay.xNorm to currentOverlay.yNorm
                            // Store the text center in image-normalized coords so scaling keeps center fixed.
                            val img = currentImageRect
                            val cxNorm = (currentOverlay.xNorm + (startTextWH.width / 2f) / img.width).coerceIn(0f, 1f)
                            val cyNorm = (currentOverlay.yNorm + (startTextWH.height / 2f) / img.height).coerceIn(0f, 1f)
                            startCenterNorm = cxNorm to cyNorm
                            val d = hypot((pos.x - startCenter.x).toDouble(), (pos.y - startCenter.y).toDouble()).toFloat()
                            startDist = d.coerceAtLeast(1f)
                        }
                    },
        onDragCancel = { mode = TransformMode.NONE },
        onDragEnd = { mode = TransformMode.NONE },
                    onDrag = { change, dragAmount ->
                        change.consumeAllChanges()
                        val img = currentImageRect
                        val zoom = currentZoomScale
                        when (mode) {
                            TransformMode.MOVE -> {
                                val dx = (dragAmount.x / zoom) / img.width
                                val dy = (dragAmount.y / zoom) / img.height
                                onMoveBy(dx, dy)
                            }
                            TransformMode.ROTATE -> {
                                val pos = change.position
                                val angle = atan2(pos.y - startCenter.y, pos.x - startCenter.x)
                                var delta = angle - startAngleRad
                                // Normalize to [-PI, PI] to avoid sudden jumps when crossing the atan2 wrap boundary.
                                while (delta > PI.toFloat()) delta -= (2f * PI.toFloat())
                                while (delta < -PI.toFloat()) delta += (2f * PI.toFloat())
                                val deltaDeg = (delta * 180f / PI.toFloat())
                                onRotateTo(startRotation + deltaDeg)
                            }
                            TransformMode.SCALE -> {
                                val pos = change.position
                                val d = hypot((pos.x - startCenter.x).toDouble(), (pos.y - startCenter.y).toDouble()).toFloat()
                                val factor = (d / startDist).coerceIn(0.3f, 4f)
                                val newSize = (startSizeNorm * factor).coerceIn(0.02f, 0.20f)

                                // Keep center fixed in image space.
                                val oldW = startTextWH.width.toFloat()
                                val oldH = startTextWH.height.toFloat()
                                val newW = oldW * factor
                                val newH = oldH * factor
                                val (cxNorm, cyNorm) = startCenterNorm
                                val newX = (cxNorm - (newW / 2f) / img.width).coerceIn(0f, 1f)
                                val newY = (cyNorm - (newH / 2f) / img.height).coerceIn(0f, 1f)
                                onScaleAndPositionTo(newSize, newX, newY)
                            }
                            TransformMode.NONE -> Unit
                        }
                    }
                )
            }
    ) {
        // Content (text) – padding creates room for handles/rotate control.
        Text(
            text = overlay.text,
            color = Color(overlay.argb),
            fontSize = fontSize,
            fontWeight = if (overlay.isBold) FontWeight.Bold else FontWeight.Normal,
            fontStyle = if (overlay.isItalic) FontStyle.Italic else FontStyle.Normal,
            textDecoration = if (overlay.isUnderline) TextDecoration.Underline else null,
            modifier = Modifier
                .padding(start = with(density) { padSidePx.toDp() }, top = with(density) { padTopPx.toDp() }, end = with(density) { padSidePx.toDp() }, bottom = with(density) { padBottomPx.toDp() })
                .onSizeChanged { textSizePx = it },
            style = TextStyle(
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.65f),
                    offset = Offset(0f, 2f),
                    blurRadius = 6f
                )
            )
        )

        if (isSelected && isEditing && textSizePx.width > 0 && textSizePx.height > 0) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val borderColor = Color.White.copy(alpha = 0.9f)
                val stroke = Stroke(width = with(density) { 1.5.dp.toPx() })

                // Selection rectangle
                drawRect(
                    color = borderColor,
                    topLeft = Offset(selectionRect.left, selectionRect.top),
                    size = androidx.compose.ui.geometry.Size(selectionRect.width, selectionRect.height),
                    style = stroke
                )

                // Corner handles
                val corners = cornerCenters()
                corners.forEach { c ->
                    drawCircle(color = borderColor, radius = handleRadiusPx, center = c)
                    drawCircle(color = Color.Black.copy(alpha = 0.35f), radius = handleRadiusPx + 2f, center = c, style = Stroke(width = 2f))
                }

                // Rotate handle + line
                drawLine(
                    color = borderColor,
                    start = Offset(center.x, selectionRect.top),
                    end = rotateHandleCenter,
                    strokeWidth = with(density) { 1.dp.toPx() }
                )
                drawCircle(color = borderColor, radius = handleRadiusPx + 6f, center = rotateHandleCenter)
            }

            // Rotate icon in the rotate handle
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            (rotateHandleCenter.x - with(density) { (rotateIconSize / 2).toPx() }).roundToInt(),
                            (rotateHandleCenter.y - with(density) { (rotateIconSize / 2).toPx() }).roundToInt()
                        )
                    }
            ) {
                Icon(
                    imageVector = Icons.Default.RotateRight,
                    contentDescription = null,
                    tint = Color.Black.copy(alpha = 0.75f),
                    modifier = Modifier.size(rotateIconSize)
                )
            }
        }
    }
}

