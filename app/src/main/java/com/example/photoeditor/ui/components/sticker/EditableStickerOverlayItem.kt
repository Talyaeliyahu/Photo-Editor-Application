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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.photoeditor.model.StickerOverlay
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt

private enum class StickerTransformMode { NONE, MOVE, SCALE, ROTATE }

@Composable
internal fun EditableStickerOverlayItem(
    overlay: StickerOverlay,
    isSelected: Boolean,
    isEditing: Boolean,
    imageRect: Rect,
    zoomScale: Float,
    onSelect: (String) -> Unit,
    onMoveBy: (dxNorm: Float, dyNorm: Float) -> Unit,
    onRotateTo: (degrees: Float) -> Unit,
    onScaleAndPositionTo: (sizeNorm: Float, xNorm: Float, yNorm: Float) -> Unit
) {
    if (overlay.emoji.isBlank()) return
    if (imageRect.width <= 0f || imageRect.height <= 0f) return

    val density = LocalDensity.current
    val minPx = min(imageRect.width, imageRect.height).coerceAtLeast(1f)
    val fontSizeSp = with(density) { (overlay.sizeNorm.coerceIn(0.03f, 0.25f) * minPx).toSp() }

    val stickerX = imageRect.left + (overlay.xNorm * imageRect.width)
    val stickerY = imageRect.top + (overlay.yNorm * imageRect.height)

    val padSidePx = with(density) { 10.dp.toPx() }
    val padBottomPx = with(density) { 8.dp.toPx() }
    val padTopPx = with(density) { 32.dp.toPx() }
    val handleRadiusPx = with(density) { 6.dp.toPx() }
    val handleTouchRadiusPx = with(density) { 28.dp.toPx() }
    val rotateOffsetPx = with(density) { 20.dp.toPx() }
    val rotateIconSize = 18.dp

    var containerSize by remember(overlay.id) { mutableStateOf(IntSize.Zero) }
    var contentSizePx by remember(overlay.id) { mutableStateOf(IntSize.Zero) }

    val selectionRect = Rect(
        left = padSidePx,
        top = padTopPx,
        right = padSidePx + contentSizePx.width,
        bottom = padTopPx + contentSizePx.height
    )

    val pivotX = if (containerSize.width > 0) {
        ((padSidePx + contentSizePx.width / 2f) / containerSize.width.toFloat()).coerceIn(0f, 1f)
    } else 0.5f
    val pivotY = if (containerSize.height > 0) {
        ((padTopPx + contentSizePx.height / 2f) / containerSize.height.toFloat()).coerceIn(0f, 1f)
    } else 0.5f

    val center = Offset(
        x = selectionRect.left + selectionRect.width / 2f,
        y = selectionRect.top + selectionRect.height / 2f
    )
    val rotateHandleCenter = Offset(center.x, selectionRect.top - rotateOffsetPx)

    val currentOverlay by rememberUpdatedState(overlay)
    val currentImageRect by rememberUpdatedState(imageRect)
    val currentZoomScale by rememberUpdatedState(zoomScale)
    val currentContentSize by rememberUpdatedState(contentSizePx)
    val currentSelectionRect by rememberUpdatedState(selectionRect)
    val currentCenter by rememberUpdatedState(center)
    val currentRotateHandleCenter by rememberUpdatedState(rotateHandleCenter)

    var mode by remember(overlay.id) { mutableStateOf(StickerTransformMode.NONE) }
    var startAngleRad by remember(overlay.id) { mutableStateOf(0f) }
    var startRotation by remember(overlay.id) { mutableStateOf(0f) }
    var startDist by remember(overlay.id) { mutableStateOf(1f) }
    var startSizeNorm by remember(overlay.id) { mutableStateOf(overlay.sizeNorm) }
    var startWH by remember(overlay.id) { mutableStateOf(IntSize.Zero) }
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
            .offset { IntOffset((stickerX - padSidePx).roundToInt(), (stickerY - padTopPx).roundToInt()) }
            .wrapContentSize()
            .onSizeChanged { containerSize = it }
            .graphicsLayer(
                rotationZ = overlay.rotationDegrees,
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(pivotX, pivotY)
            )
            .pointerInput(isEditing, overlay.id) {
                if (!isEditing) return@pointerInput
                detectTapGestures(onTap = { onSelect(overlay.id) })
            }
            .pointerInput(isEditing, isSelected, overlay.id) {
                if (!isEditing || !isSelected) return@pointerInput
                detectDragGestures(
                    onDragStart = { pos ->
                        onSelect(overlay.id)

                        val s = currentContentSize
                        if (s.width <= 0 || s.height <= 0) {
                            mode = StickerTransformMode.NONE
                            return@detectDragGestures
                        }

                        startCenter = currentCenter
                        mode = when {
                            near(pos, currentRotateHandleCenter) -> StickerTransformMode.ROTATE
                            cornerCenters().any { near(pos, it) } -> StickerTransformMode.SCALE
                            currentSelectionRect.contains(pos) -> StickerTransformMode.MOVE
                            else -> StickerTransformMode.NONE
                        }

                        if (mode == StickerTransformMode.ROTATE) {
                            startRotation = currentOverlay.rotationDegrees
                            startAngleRad = atan2(pos.y - startCenter.y, pos.x - startCenter.x)
                        } else if (mode == StickerTransformMode.SCALE) {
                            startSizeNorm = currentOverlay.sizeNorm
                            startWH = s
                            val img = currentImageRect
                            val cxNorm = (currentOverlay.xNorm + (startWH.width / 2f) / img.width).coerceIn(0f, 1f)
                            val cyNorm = (currentOverlay.yNorm + (startWH.height / 2f) / img.height).coerceIn(0f, 1f)
                            startCenterNorm = cxNorm to cyNorm
                            val d = hypot((pos.x - startCenter.x).toDouble(), (pos.y - startCenter.y).toDouble()).toFloat()
                            startDist = d.coerceAtLeast(1f)
                        }
                    },
                    onDragCancel = { mode = StickerTransformMode.NONE },
                    onDragEnd = { mode = StickerTransformMode.NONE },
                    onDrag = { change, dragAmount ->
                        change.consumeAllChanges()
                        val img = currentImageRect
                        val zoom = currentZoomScale
                        when (mode) {
                            StickerTransformMode.MOVE -> {
                                val dx = (dragAmount.x / zoom) / img.width
                                val dy = (dragAmount.y / zoom) / img.height
                                onMoveBy(dx, dy)
                            }
                            StickerTransformMode.ROTATE -> {
                                val pos = change.position
                                val angle = atan2(pos.y - startCenter.y, pos.x - startCenter.x)
                                var delta = angle - startAngleRad
                                while (delta > PI.toFloat()) delta -= (2f * PI.toFloat())
                                while (delta < -PI.toFloat()) delta += (2f * PI.toFloat())
                                val deltaDeg = (delta * 180f / PI.toFloat())
                                onRotateTo(startRotation + deltaDeg)
                            }
                            StickerTransformMode.SCALE -> {
                                val pos = change.position
                                val d = hypot((pos.x - startCenter.x).toDouble(), (pos.y - startCenter.y).toDouble()).toFloat()
                                val factor = (d / startDist).coerceIn(0.3f, 4f)
                                val newSize = (startSizeNorm * factor).coerceIn(0.03f, 0.25f)

                                val oldW = startWH.width.toFloat()
                                val oldH = startWH.height.toFloat()
                                val newW = oldW * factor
                                val newH = oldH * factor
                                val (cxNorm, cyNorm) = startCenterNorm
                                val newX = (cxNorm - (newW / 2f) / img.width).coerceIn(0f, 1f)
                                val newY = (cyNorm - (newH / 2f) / img.height).coerceIn(0f, 1f)
                                onScaleAndPositionTo(newSize, newX, newY)
                            }
                            StickerTransformMode.NONE -> Unit
                        }
                    }
                )
            }
    ) {
        Text(
            text = overlay.emoji,
            fontSize = fontSizeSp,
            modifier = Modifier
                .padding(
                    start = with(density) { padSidePx.toDp() },
                    top = with(density) { padTopPx.toDp() },
                    end = with(density) { padSidePx.toDp() },
                    bottom = with(density) { padBottomPx.toDp() }
                )
                .onSizeChanged { contentSizePx = it }
        )

        if (isSelected && isEditing && contentSizePx.width > 0 && contentSizePx.height > 0) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val borderColor = Color.White.copy(alpha = 0.9f)
                val stroke = Stroke(width = with(density) { 1.5.dp.toPx() })

                drawRect(
                    color = borderColor,
                    topLeft = Offset(selectionRect.left, selectionRect.top),
                    size = androidx.compose.ui.geometry.Size(selectionRect.width, selectionRect.height),
                    style = stroke
                )

                val corners = cornerCenters()
                corners.forEach { c ->
                    drawCircle(color = borderColor, radius = handleRadiusPx, center = c)
                }

                drawLine(
                    color = borderColor,
                    start = Offset(center.x, selectionRect.top),
                    end = rotateHandleCenter,
                    strokeWidth = with(density) { 1.dp.toPx() }
                )
                drawCircle(color = borderColor, radius = handleRadiusPx + 6f, center = rotateHandleCenter)
            }

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

