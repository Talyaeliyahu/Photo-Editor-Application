package com.example.photoeditor.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.min

@Composable
internal fun AppleStyleAdjustmentIcon(
    type: AdjustmentType,
    tint: Color,
    iconSize: Dp = 24.dp
) {
    Canvas(modifier = Modifier.size(iconSize)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val cy = h / 2f
        val r = min(w, h) * 0.45f

        val circleRect = Rect(cx - r, cy - r, cx + r, cy + r)
        val circlePath = Path().apply { addOval(circleRect) }

        fun drawCircleOutline() {
            drawCircle(
                color = tint,
                radius = r,
                center = Offset(cx, cy),
                style = Stroke(width = r * 0.14f)
            )
        }

        fun dropletPath(): Path {
            // Teardrop: pointy top + round bottom
            val tipY = cy - r * 0.95f
            val bottomY = cy + r * 0.98f
            val cX = r * 0.95f
            val cY1 = cy - r * 0.55f
            val cY2 = cy + r * 0.25f
            return Path().apply {
                moveTo(cx, tipY)
                cubicTo(cx + cX, cY1, cx + cX, cY2, cx, bottomY)
                cubicTo(cx - cX, cY2, cx - cX, cY1, cx, tipY)
                close()
            }
        }

        when (type) {
            AdjustmentType.EXPOSURE -> {
                // Circle with plus/minus
                drawCircleOutline()
                val strokeW = r * 0.12f
                val y1 = cy - r * 0.25f
                drawLine(tint, Offset(cx - r * 0.22f, y1), Offset(cx + r * 0.22f, y1), strokeWidth = strokeW)
                drawLine(tint, Offset(cx, y1 - r * 0.22f), Offset(cx, y1 + r * 0.22f), strokeWidth = strokeW)
                val y2 = cy + r * 0.28f
                drawLine(tint, Offset(cx - r * 0.22f, y2), Offset(cx + r * 0.22f, y2), strokeWidth = strokeW)
            }

            AdjustmentType.GLOW -> {
                // Simple yin-yang-like glow mark
                drawCircleOutline()
                drawArc(
                    color = tint,
                    startAngle = -90f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = Offset(cx - r, cy - r),
                    size = androidx.compose.ui.geometry.Size(r * 2, r * 2),
                    alpha = 0.85f
                )
                drawCircle(color = Color.White, radius = r * 0.20f, center = Offset(cx, cy - r * 0.35f))
                drawCircle(color = tint, radius = r * 0.20f, center = Offset(cx, cy + r * 0.35f), alpha = 0.85f)
                drawCircle(color = tint, radius = r * 0.07f, center = Offset(cx, cy - r * 0.35f), alpha = 0.85f)
                drawCircle(color = Color.White, radius = r * 0.07f, center = Offset(cx, cy + r * 0.35f))
            }

            AdjustmentType.HIGHLIGHTS -> {
                // Right-half stripes clipped to circle
                clipPath(circlePath) {
                    clipRect(left = cx, top = cy - r, right = cx + r, bottom = cy + r) {
                        val stripeCount = 6
                        val strokeW = r * 0.12f
                        val top = cy - r
                        val bottom = cy + r
                        for (i in 0 until stripeCount) {
                            val x = cx + (i + 1) * (r / (stripeCount + 1))
                            drawLine(
                                color = tint,
                                start = Offset(x, top),
                                end = Offset(x, bottom),
                                strokeWidth = strokeW,
                                alpha = 0.85f
                            )
                        }
                    }
                }
                drawCircleOutline()
            }

            AdjustmentType.SHADOWS -> {
                // Left half fill + right-half stripes clipped to circle
                drawArc(
                    color = tint,
                    startAngle = 90f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = Offset(cx - r, cy - r),
                    size = androidx.compose.ui.geometry.Size(r * 2, r * 2),
                    alpha = 0.9f
                )
                clipPath(circlePath) {
                    clipRect(left = cx, top = cy - r, right = cx + r, bottom = cy + r) {
                        val stripeCount = 5
                        val strokeW = r * 0.12f
                        val top = cy - r
                        val bottom = cy + r
                        for (i in 0 until stripeCount) {
                            val x = cx + (i + 1) * (r / (stripeCount + 1))
                            drawLine(
                                color = tint,
                                start = Offset(x, top),
                                end = Offset(x, bottom),
                                strokeWidth = strokeW,
                                alpha = 0.65f
                            )
                        }
                    }
                }
                drawCircleOutline()
            }

            AdjustmentType.CONTRAST -> {
                drawCircleOutline()
                drawArc(
                    color = tint,
                    startAngle = 90f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = Offset(cx - r, cy - r),
                    size = androidx.compose.ui.geometry.Size(r * 2, r * 2),
                    alpha = 0.9f
                )
            }

            AdjustmentType.SATURATION -> {
                drawCircle(
                    brush = Brush.sweepGradient(
                        listOf(
                            Color(0xFFFF3B30),
                            Color(0xFFFF9500),
                            Color(0xFFFFCC00),
                            Color(0xFF34C759),
                            Color(0xFF007AFF),
                            Color(0xFFAF52DE),
                            Color(0xFFFF3B30)
                        ),
                        center = Offset(cx, cy)
                    ),
                    radius = r,
                    center = Offset(cx, cy)
                )
                drawCircle(color = Color.White, radius = r * 0.42f, center = Offset(cx, cy))
            }

            AdjustmentType.VIBRANCE -> {
                val colors = listOf(
                    Color(0xFFFF3B30),
                    Color(0xFFFF9500),
                    Color(0xFFFFCC00),
                    Color(0xFF34C759),
                    Color(0xFF007AFF),
                    Color(0xFFAF52DE)
                )
                val stripeW = (r * 2f) / (colors.size + 2)
                val top = cy - r
                val bottom = cy + r
                val startX = cx - r + stripeW
                clipPath(circlePath) {
                    for (i in colors.indices) {
                        val x = startX + i * stripeW
                        drawLine(
                            color = colors[i],
                            start = Offset(x, top),
                            end = Offset(x, bottom),
                            strokeWidth = stripeW * 0.65f
                        )
                    }
                }
                drawCircleOutline()
            }

            AdjustmentType.WARMTH -> {
                // Thermometer: outline stem + bulb + ticks + mercury fill
                val strokeW = r * 0.10f
                val stemW = r * 0.30f
                val stemTop = cy - r * 0.92f
                val stemBottom = cy + r * 0.18f
                val stemLeft = cx - stemW / 2f
                val stemH = stemBottom - stemTop

                val bulbR = r * 0.30f
                val bulbCenter = Offset(cx, cy + r * 0.56f)

                drawRoundRect(
                    color = tint,
                    topLeft = Offset(stemLeft, stemTop),
                    size = androidx.compose.ui.geometry.Size(stemW, stemH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(stemW / 2f, stemW / 2f),
                    style = Stroke(width = strokeW)
                )

                drawCircle(
                    color = tint,
                    radius = bulbR,
                    center = bulbCenter,
                    style = Stroke(width = strokeW)
                )

                val tickX1 = stemLeft + stemW + strokeW * 0.6f
                val tickX2 = tickX1 + stemW * 0.35f
                val tickStroke = strokeW * 0.55f
                for (i in 1..4) {
                    val y = stemTop + (stemH * (i / 5f))
                    drawLine(
                        color = tint,
                        start = Offset(tickX1, y),
                        end = Offset(tickX2, y),
                        strokeWidth = tickStroke,
                        alpha = 0.9f
                    )
                }

                val innerPad = strokeW * 1.1f
                val innerW = stemW - innerPad * 2
                val innerLeft = stemLeft + innerPad
                val mercuryTop = stemTop + stemH * 0.55f
                val mercuryBottom = stemBottom - innerPad
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(innerLeft, mercuryTop),
                    size = androidx.compose.ui.geometry.Size(innerW, mercuryBottom - mercuryTop),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(innerW / 2f, innerW / 2f),
                    alpha = 0.92f
                )
                drawCircle(color = tint, radius = bulbR * 0.68f, center = bulbCenter, alpha = 0.92f)
                drawCircle(
                    color = tint,
                    radius = bulbR * 0.12f,
                    center = Offset(bulbCenter.x - bulbR * 0.28f, bulbCenter.y - bulbR * 0.20f),
                    alpha = 0.35f
                )
            }

            AdjustmentType.HUE -> {
                // Droplet filled with hue gradient
                val p = dropletPath()
                clipPath(p) {
                    drawRect(
                        brush = Brush.horizontalGradient(
                            listOf(
                                Color(0xFFFF3B30),
                                Color(0xFFFF9500),
                                Color(0xFFFFCC00),
                                Color(0xFF34C759),
                                Color(0xFF007AFF),
                                Color(0xFFAF52DE),
                                Color(0xFFFF3B30)
                            ),
                            startX = cx - r,
                            endX = cx + r
                        ),
                        topLeft = Offset(cx - r, cy - r),
                        size = androidx.compose.ui.geometry.Size(r * 2, r * 2),
                        alpha = 0.95f
                    )
                }
                drawPath(path = p, color = tint, style = Stroke(width = r * 0.12f))
            }

            AdjustmentType.SHARPNESS -> {
                // Triangle with one half filled
                val p = Path().apply {
                    moveTo(cx - r * 0.85f, cy + r * 0.70f)
                    lineTo(cx + r * 0.85f, cy + r * 0.70f)
                    lineTo(cx + r * 0.10f, cy - r * 0.85f)
                    close()
                }
                val fill = Path().apply {
                    moveTo(cx - r * 0.85f, cy + r * 0.70f)
                    lineTo(cx + r * 0.10f, cy - r * 0.85f)
                    lineTo(cx + r * 0.10f, cy + r * 0.70f)
                    close()
                }
                drawPath(p, color = tint, style = Stroke(width = r * 0.12f))
                drawPath(fill, color = tint, alpha = 0.9f)
            }

            AdjustmentType.DEFINITION -> {
                val p = Path().apply {
                    moveTo(cx - r * 0.80f, cy + r * 0.70f)
                    lineTo(cx + r * 0.80f, cy + r * 0.70f)
                    lineTo(cx + r * 0.05f, cy - r * 0.85f)
                    close()
                }
                drawPath(p, color = tint, style = Stroke(width = r * 0.12f))
            }

            AdjustmentType.VIGNETTE -> {
                drawCircle(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Transparent,
                            0.55f to Color.Transparent,
                            1.0f to tint.copy(alpha = 0.95f)
                        ),
                        center = Offset(cx, cy),
                        radius = r
                    ),
                    radius = r,
                    center = Offset(cx, cy)
                )
                drawCircle(color = Color.White, radius = r * 0.45f, center = Offset(cx, cy), alpha = 0.65f)
                drawCircleOutline()
            }

            // Not shown in the screenshots; safe fallback.
            AdjustmentType.TINT -> {
                val p = dropletPath()
                drawPath(path = p, color = tint, alpha = 0.9f)
            }

            // Not shown in this UI order; keep a minimal fallback.
            else -> drawCircleOutline()
        }
    }
}

