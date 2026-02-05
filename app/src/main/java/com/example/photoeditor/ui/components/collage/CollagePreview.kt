package com.example.photoeditor.ui.components.collage

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.photoeditor.model.CollageConfig
import kotlin.math.min
import kotlin.math.sqrt

private const val TAP_SLOP_PX = 20f
private const val TAP_TIMEOUT_MS = 300L
private const val LONG_PRESS_TIMEOUT_MS = 500L

/**
 * Collage preview: tap to select/deselect/swap, long-press to replace, zoom/pan on selected cell.
 *
 * Changes from previous version:
 * 1. Removed the LaunchedEffect that persisted crop on every scale/offset change — it caused
 *    race conditions and mid-gesture saves.
 * 2. curCrop is now threaded through the gesture handler and passed directly to persistCrop
 *    on gesture end, so the accumulated crop during the gesture is actually saved.
 * 3. Swap order fixed: onSwapImages is called before selectedIndex is nulled.
 * 4. Long-press no longer calls persistCrop (the image is being replaced anyway).
 * 5. persistCrop now receives the final crop values directly instead of re-deriving them
 *    from overlayScale/overlayOffset (which were already stale by the time it ran).
 */
@Composable
fun CollagePreview(
    previewBitmap: Bitmap,
    config: CollageConfig,
    onSwapImages: (Int, Int) -> Unit,
    onReplaceImage: (Int) -> Unit,
    onFocusChange: (index: Int, cropLeft: Float, cropTop: Float, cropRight: Float, cropBottom: Float) -> Unit = { _, _, _, _, _ -> },
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val latestConfig = remember { mutableStateOf(config) }
    LaunchedEffect(config) { latestConfig.value = config }

    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    // overlayScale / overlayOffset are purely visual during an active gesture.
    // They are reset to identity after the gesture ends and the crop is persisted.
    var overlayScale by remember { mutableStateOf(1f) }
    var overlayOffset by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    fun displayScale(): Float = min(
        containerSize.width.toFloat() / previewBitmap.width,
        containerSize.height.toFloat() / previewBitmap.height
    )

    fun getClickedIndex(position: Offset): Int {
        if (containerSize.width <= 0 || containerSize.height <= 0) return -1
        val ds = displayScale()
        val offX = (containerSize.width - previewBitmap.width * ds) / 2f
        val offY = (containerSize.height - previewBitmap.height * ds) / 2f
        val x = (position.x - offX) / ds
        val y = (position.y - offY) / ds
        return latestConfig.value.images.indexOfFirst { img ->
            val cx = img.x * previewBitmap.width
            val cy = img.y * previewBitmap.height
            val cw = img.width * previewBitmap.width
            val ch = img.height * previewBitmap.height
            x >= cx && x < cx + cw && y >= cy && y < cy + ch
        }
    }

    /**
     * Persist the given crop rect for [index] and reset the overlay transforms.
     * Called only when a gesture actually finishes (not speculatively).
     */
    fun persistAndDeselect(index: Int, cropLeft: Float, cropTop: Float, cropRight: Float, cropBottom: Float) {
        onFocusChange(index, cropLeft, cropTop, cropRight, cropBottom)
        overlayScale = 1f
        overlayOffset = Offset.Zero
        selectedIndex = null
    }

    /**
     * Just reset overlay without persisting (e.g. when selecting a new cell).
     */
    fun resetOverlay() {
        overlayScale = 1f
        overlayOffset = Offset.Zero
    }

    // ---------------------------------------------------------------------------
    // Tap handler  (called only after we are sure it's a tap, not a drag/zoom)
    // ---------------------------------------------------------------------------

    fun handleTap(position: Offset, finalCrop: FloatArray?) {
        val clickedIndex = getClickedIndex(position)
        val current = selectedIndex

        when {
            // Tapped outside any cell → deselect & persist current crop if any
            clickedIndex < 0 -> {
                if (current != null && finalCrop != null) {
                    persistAndDeselect(current, finalCrop[0], finalCrop[1], finalCrop[2], finalCrop[3])
                } else {
                    selectedIndex = null
                    resetOverlay()
                }
            }
            // Nothing selected yet → select the tapped cell
            current == null -> {
                selectedIndex = clickedIndex
                resetOverlay()
            }
            // Tapped the already-selected cell → deselect & persist
            current == clickedIndex -> {
                if (finalCrop != null) {
                    persistAndDeselect(current, finalCrop[0], finalCrop[1], finalCrop[2], finalCrop[3])
                } else {
                    selectedIndex = null
                    resetOverlay()
                }
            }
            // Tapped a different cell while one is selected → SWAP, then deselect
            else -> {
                // Persist crop of the previously-selected cell first
                if (finalCrop != null) {
                    onFocusChange(current, finalCrop[0], finalCrop[1], finalCrop[2], finalCrop[3])
                }
                // Swap before clearing selection so the UI state is consistent
                onSwapImages(current, clickedIndex)
                selectedIndex = null
                resetOverlay()
            }
        }
    }

    // ---------------------------------------------------------------------------
    // Gesture input
    // ---------------------------------------------------------------------------

    Box(
        modifier = modifier
            .onSizeChanged { containerSize = it }
            .pointerInput(previewBitmap, config, selectedIndex) {
                val ds = min(
                    this.size.width.toFloat() / previewBitmap.width,
                    this.size.height.toFloat() / previewBitmap.height
                )

                val idx = selectedIndex
                val cellW = if (idx != null && idx in config.images.indices)
                    config.images[idx].width * previewBitmap.width * ds else 0f
                val cellH = if (idx != null && idx in config.images.indices)
                    config.images[idx].height * previewBitmap.height * ds else 0f

                awaitEachGesture {
                    var firstDown: Offset? = null
                    var firstDownTime = 0L
                    var lastPosition: Offset? = null
                    var lastCentroid: Offset? = null
                    var lastSpread: Float? = null

                    // The single source of truth for crop during this gesture.
                    // Initialised lazily from config when the first pointer move arrives.
                    var curCrop: FloatArray? = null

                    var gestureType: String? = null   // null | "pan" | "zoom" | "longpress"
                    var maxPointers = 0

                    // Helper: initialise curCrop from the current config if not yet done
                    fun ensureCrop() {
                        if (curCrop == null && idx != null && idx in latestConfig.value.images.indices) {
                            val img = latestConfig.value.images[idx]
                            curCrop = floatArrayOf(img.cropLeft, img.cropTop, img.cropRight, img.cropBottom)
                        }
                    }

                    while (true) {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.filter { it.pressed }

                        if (pressed.isNotEmpty()) {
                            maxPointers = maxOf(maxPointers, pressed.size)
                            lastPosition = pressed.first().position
                        }

                        // ── All pointers lifted → gesture end ──────────────────
                        if (pressed.isEmpty()) {
                            when (gestureType) {
                                // Pure tap (short press, no meaningful movement)
                                null -> {
                                    if (firstDown != null && maxPointers < 2) {
                                        val duration = System.currentTimeMillis() - firstDownTime
                                        val lastPos = lastPosition ?: firstDown
                                        val moveDist = sqrt(
                                            (lastPos.x - firstDown.x) * (lastPos.x - firstDown.x) +
                                            (lastPos.y - firstDown.y) * (lastPos.y - firstDown.y)
                                        )
                                        if (duration < TAP_TIMEOUT_MS && moveDist < TAP_SLOP_PX) {
                                            handleTap(firstDown, curCrop)
                                        }
                                    }
                                }
                                // Pan or zoom finished → persist the accumulated crop
                                "pan", "zoom" -> {
                                    if (idx != null && curCrop != null) {
                                        persistAndDeselect(idx, curCrop!![0], curCrop!![1], curCrop!![2], curCrop!![3])
                                    }
                                }
                                // Long-press already handled inline; nothing to persist
                                "longpress" -> { /* no-op */ }
                            }
                            break   // exit awaitEachGesture loop
                        }

                        // ── First pointer down ─────────────────────────────────
                        if (firstDown == null) {
                            firstDown = pressed.first().position
                            firstDownTime = System.currentTimeMillis()
                        }

                        // ── Multi-touch → zoom (+ pan via centroid shift) ──────
                        if (pressed.size >= 2) {
                            gestureType = "zoom"
                            ensureCrop()

                            val centroid = pressed.map { it.position }
                                .reduce { a, p -> a + p } / pressed.size.toFloat()
                            val spread = pressed.map { (it.position - centroid).getDistance() }
                                .average().toFloat().coerceAtLeast(1f)

                            val prevC = lastCentroid
                            val prevS = lastSpread

                            if (idx != null && idx in config.images.indices &&
                                prevC != null && prevS != null && curCrop != null &&
                                cellW > 0 && cellH > 0
                            ) {
                                val zoom = spread / prevS
                                val pan = centroid - prevC

                                var l = curCrop!![0]; var t = curCrop!![1]
                                var r = curCrop!![2]; var b = curCrop!![3]
                                val cropW = (r - l).coerceIn(0.01f, 1f)
                                val cropH = (b - t).coerceIn(0.01f, 1f)

                                // Apply zoom around the crop centre
                                if (zoom != 1f) {
                                    val cx = (l + r) / 2f
                                    val cy = (t + b) / 2f
                                    val newW = (cropW / zoom).coerceIn(0.02f, 1f)
                                    val newH = (cropH / zoom).coerceIn(0.02f, 1f)
                                    l = (cx - newW / 2f).coerceIn(0f, 1f - newW); r = l + newW
                                    t = (cy - newH / 2f).coerceIn(0f, 1f - newH); b = t + newH
                                }

                                // Apply pan (centroid drift)
                                if (pan.x != 0f || pan.y != 0f) {
                                    val dx = (pan.x / cellW) * (r - l)
                                    val dy = (pan.y / cellH) * (b - t)
                                    l = (l - dx).coerceIn(0f, 1f - (r - l))
                                    r = l + (r - l)     // keep width stable
                                    t = (t - dy).coerceIn(0f, 1f - (b - t))
                                    b = t + (b - t)     // keep height stable
                                }

                                curCrop = floatArrayOf(l, t, r, b)

                                // Mirror into visual overlay so the user sees live feedback
                                overlayScale *= zoom
                                overlayOffset += pan
                            }

                            lastCentroid = centroid
                            lastSpread = spread

                            pressed.forEach { it.consumeAllChanges() }
                        }

                        // ── Single touch ───────────────────────────────────────
                        else if (pressed.size == 1) {
                            val pos = pressed.first().position
                            val dist = if (firstDown != null)
                                sqrt(
                                    (pos.x - firstDown.x) * (pos.x - firstDown.x) +
                                    (pos.y - firstDown.y) * (pos.y - firstDown.y)
                                ) else 0f
                            val elapsed = System.currentTimeMillis() - firstDownTime

                            // Long-press detection
                            if (gestureType == null && elapsed > LONG_PRESS_TIMEOUT_MS && dist < TAP_SLOP_PX) {
                                gestureType = "longpress"
                                if (firstDown != null) {
                                    val clickedIdx = getClickedIndex(firstDown)
                                    if (clickedIdx >= 0) {
                                        // No need to persist crop — the image is being replaced
                                        selectedIndex = null
                                        resetOverlay()
                                        onReplaceImage(clickedIdx)
                                    }
                                }
                            }

                            // Pan detection & accumulation
                            if (idx != null && idx in config.images.indices &&
                                (gestureType == null && dist > TAP_SLOP_PX || gestureType == "pan")
                            ) {
                                if (gestureType == null) gestureType = "pan"
                                ensureCrop()

                                val prevPos = lastCentroid ?: firstDown!!
                                val delta = pos - prevPos

                                if (curCrop != null && cellW > 0 && cellH > 0) {
                                    var l = curCrop!![0]; var t = curCrop!![1]
                                    var r = curCrop!![2]; var b = curCrop!![3]
                                    val w = r - l; val h = b - t

                                    val dx = (delta.x / cellW) * w
                                    val dy = (delta.y / cellH) * h
                                    l = (l - dx).coerceIn(0f, 1f - w); r = l + w
                                    t = (t - dy).coerceIn(0f, 1f - h); b = t + h

                                    curCrop = floatArrayOf(l, t, r, b)
                                    overlayOffset += delta
                                }

                                lastCentroid = pos
                                pressed.forEach { it.consumeAllChanges() }
                            }
                        }
                    }
                }
            }
    ) {
        // ── Base collage bitmap ────────────────────────────────────────────────
        AsyncImage(
            model = previewBitmap,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        // ── Live zoom/pan overlay for the selected cell ───────────────────────
        selectedIndex?.let { idx ->
            if (idx in config.images.indices && containerSize.width > 0 && containerSize.height > 0) {
                val image = config.images[idx]
                val ds = displayScale()
                val offX = (containerSize.width - previewBitmap.width * ds) / 2f
                val offY = (containerSize.height - previewBitmap.height * ds) / 2f
                val cellX = image.x * previewBitmap.width * ds + offX
                val cellY = image.y * previewBitmap.height * ds + offY
                val cellW = image.width * previewBitmap.width * ds
                val cellH = image.height * previewBitmap.height * ds

                val cellBitmap = remember(previewBitmap, image, idx) {
                    val bx = (image.x * previewBitmap.width).toInt().coerceIn(0, previewBitmap.width - 1)
                    val by = (image.y * previewBitmap.height).toInt().coerceIn(0, previewBitmap.height - 1)
                    val bw = (image.width * previewBitmap.width).toInt().coerceIn(1, previewBitmap.width - bx)
                    val bh = (image.height * previewBitmap.height).toInt().coerceIn(1, previewBitmap.height - by)
                    try { Bitmap.createBitmap(previewBitmap, bx, by, bw, bh) } catch (e: Exception) { null }
                }

                cellBitmap?.let { bmp ->
                    Box(
                        modifier = Modifier
                            .offset(with(density) { cellX.toDp() }, with(density) { cellY.toDp() })
                            .size(with(density) { cellW.toDp() }, with(density) { cellH.toDp() })
                            .clip(RectangleShape)
                            .graphicsLayer {
                                scaleX = overlayScale
                                scaleY = overlayScale
                                translationX = overlayOffset.x
                                translationY = overlayOffset.y
                            }
                    ) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }
        }

        // ── Selection highlight (white border + tint) ─────────────────────────
        Canvas(modifier = Modifier.fillMaxSize()) {
            val scaleX = this.size.width / previewBitmap.width
            val scaleY = this.size.height / previewBitmap.height
            val scale = minOf(scaleX, scaleY)
            val offsetX = (this.size.width - previewBitmap.width * scale) / 2f
            val offsetY = (this.size.height - previewBitmap.height * scale) / 2f

            selectedIndex?.let { index ->
                if (index in config.images.indices) {
                    val image = config.images[index]
                    val cellX = image.x * previewBitmap.width * scale + offsetX
                    val cellY = image.y * previewBitmap.height * scale + offsetY
                    val cellWidth = image.width * previewBitmap.width * scale
                    val cellHeight = image.height * previewBitmap.height * scale

                    drawRect(
                        color = Color.White.copy(alpha = 0.3f),
                        topLeft = Offset(cellX, cellY),
                        size = Size(cellWidth, cellHeight)
                    )
                    drawRect(
                        color = Color.White,
                        topLeft = Offset(cellX, cellY),
                        size = Size(cellWidth, cellHeight),
                        style = Stroke(width = 3.dp.toPx())
                    )
                }
            }
        }
    }
}