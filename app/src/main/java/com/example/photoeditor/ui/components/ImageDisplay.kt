package com.example.photoeditor.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.photoeditor.R
import kotlin.math.max
import kotlin.math.min

/**
 * Shape that clips to a rounded rect at a specific position (the image rect).
 * Used when the image has letterboxing - we clip to the actual image bounds, not the full container.
 */
private class ImageRectRoundedShape(
    private val imageRect: Rect,
    private val cornerRadiusPx: Float
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val radius = cornerRadiusPx.coerceAtMost(min(imageRect.width, imageRect.height) / 2f)
        val roundRect = RoundRect(
            rect = imageRect,
            cornerRadius = CornerRadius(radius)
        )
        return Outline.Rounded(roundRect)
    }
}

/**
 * Image display section with loading indicator.
 */
@Composable
fun ImageDisplay(
    bitmap: Bitmap?,
    isLoading: Boolean,
    onLongPress: () -> Unit,
    contentScale: ContentScale = ContentScale.Fit,
    enableZoom: Boolean = true,
    resetTransform: Boolean = false,
    roundedCornerRadiusPercent: Float = 0f,
    allowSingleFingerPan: Boolean = true,
    overlayContent: @Composable (imageRect: Rect, scale: Float) -> Unit = { _, _ -> }
) {
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var scale by remember(bitmap) { mutableStateOf(1f) }
    var offset by remember(bitmap) { mutableStateOf(Offset.Zero) }

    LaunchedEffect(resetTransform) {
        if (resetTransform) {
            scale = 1f
            offset = Offset.Zero
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            // Black background for the "empty" area around the image (when using ContentScale.Fit).
            .background(Color.Black)
            .onSizeChanged { containerSize = it }
            .combinedClickable(
                onClick = { /* no-op */ },
                onLongClick = { onLongPress() },
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
            .pointerInput(enableZoom, allowSingleFingerPan, containerSize, bitmap) {
                if (!enableZoom || bitmap == null) return@pointerInput
                val imageRect = computeFitRect(containerSize, bitmap.width, bitmap.height)

                fun getPanBounds(s: Float): Pair<Float, Float> {
                    val scaledW = imageRect.width * s
                    val scaledH = imageRect.height * s
                    val maxX = max(0f, (scaledW - containerSize.width) / 2f)
                    val maxY = max(0f, (scaledH - containerSize.height) / 2f)
                    return maxX to maxY
                }

                awaitEachGesture {
                    var lastCentroid: Offset? = null
                    var lastSpread: Float? = null

                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Final)
                        val pressed = event.changes.filter { it.pressed }
                        if (pressed.isEmpty()) break

                        // Skip if overlay (sticker/text) already consumed - let them handle
                        if (pressed.any { it.isConsumed }) continue

                        val handled = when {
                            // Two fingers: pinch zoom around pivot (like gallery) + pan
                            pressed.size >= 2 -> {
                                val centroid = pressed
                                    .map { it.position }
                                    .reduce { acc, p -> acc + p } / pressed.size.toFloat()

                                val spread = pressed
                                    .map { (it.position - centroid).getDistance() }
                                    .average()
                                    .toFloat()
                                    .coerceAtLeast(1f)

                                val center = Offset(
                                    containerSize.width / 2f,
                                    containerSize.height / 2f
                                )

                                val prevC = lastCentroid
                                val prevS = lastSpread
                                if (prevC != null && prevS != null) {
                                    val zoom = spread / prevS
                                    val newScale = (scale * zoom).coerceIn(1f, 5f)

                                    // Zoom around pivot (point between fingers) - like gallery
                                    val pivot = centroid
                                    val newOffset = (pivot - center) * (1f - zoom) + offset * zoom

                                    // Add pan from centroid movement
                                    val pan = centroid - prevC
                                    val adjustedOffset = if (newScale > 1.001f) newOffset + pan else Offset.Zero

                                    val (maxX, maxY) = getPanBounds(newScale)

                                    scale = newScale
                                    offset = Offset(
                                        x = adjustedOffset.x.coerceIn(-maxX, maxX),
                                        y = adjustedOffset.y.coerceIn(-maxY, maxY)
                                    )
                                }

                                lastCentroid = centroid
                                lastSpread = spread
                                true
                            }
                            // One finger when zoomed: pan the image (skip when editing stickers/text)
                            pressed.size == 1 && scale > 1.001f && allowSingleFingerPan -> {
                                val pos = pressed.first().position
                                val prevC = lastCentroid
                                if (prevC != null) {
                                    val pan = pos - prevC
                                    val (maxX, maxY) = getPanBounds(scale)

                                    offset = Offset(
                                        x = (offset.x + pan.x).coerceIn(-maxX, maxX),
                                        y = (offset.y + pan.y).coerceIn(-maxY, maxY)
                                    )
                                }
                                lastCentroid = pos
                                lastSpread = null
                                true
                            }
                            else -> {
                                lastCentroid = pressed.first().position
                                lastSpread = null
                                false
                            }
                        }

                        if (handled) pressed.forEach { it.consumeAllChanges() }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            val imageRect = computeFitRect(
                containerSize = containerSize,
                bitmapWidth = bitmap.width,
                bitmapHeight = bitmap.height
            )

            val clipShape: Shape = if (roundedCornerRadiusPercent > 0f && imageRect.width > 0 && imageRect.height > 0) {
                ImageRectRoundedShape(
                    imageRect = imageRect,
                    cornerRadiusPx = min(imageRect.width, imageRect.height) *
                        roundedCornerRadiusPercent.coerceIn(0.01f, 0.2f)
                )
            } else {
                RectangleShape
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                        shape = clipShape
                        clip = true
                    }
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = stringResource(R.string.edited_image),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale
                )

                overlayContent(imageRect, scale)
            }
        } else {
            Text(stringResource(R.string.no_image))
        }
        
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

private fun computeFitRect(containerSize: IntSize, bitmapWidth: Int, bitmapHeight: Int): Rect {
    if (containerSize.width == 0 || containerSize.height == 0) return Rect(0f, 0f, 0f, 0f)
    if (bitmapWidth <= 0 || bitmapHeight <= 0) return Rect(0f, 0f, 0f, 0f)

    val cw = containerSize.width.toFloat()
    val ch = containerSize.height.toFloat()
    val bitmapAspect = bitmapWidth.toFloat() / bitmapHeight.toFloat()
    val containerAspect = cw / ch

    return if (containerAspect > bitmapAspect) {
        val h = ch
        val w = h * bitmapAspect
        val left = (cw - w) / 2f
        Rect(left, 0f, left + w, h)
    } else {
        val w = cw
        val h = w / bitmapAspect
        val top = (ch - h) / 2f
        Rect(0f, top, w, top + h)
    }
}
