package com.example.photoeditor.image

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * ImageProcessor handles image adjustments using ColorMatrix.
 * All operations return new Bitmaps without mutating the source.
 *
 * Optimisation notes vs. the previous version:
 * 1. RGBToHSV / HSVToColor replaced with a hand-rolled inline conversion.
 *    Android's versions do extra work (allocations, defensive checks) that we
 *    don't need in a tight pixel loop.
 * 2. Vignette mask is pre-computed once into a FloatArray so that sqrt() is
 *    not called per-pixel on every slider drag.
 * 3. sharpness / definition / glow share a single getPixels → loop → setPixels
 *    pass, eliminating redundant pixel-array copies when more than one is active.
 */
object ImageProcessor {

    // ------------------------------------------------------------------
    // Public single-adjustment helpers (unchanged API)
    // ------------------------------------------------------------------

    fun adjustBrightness(source: Bitmap, brightness: Float): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, source.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()
        val normalizedBrightness = (brightness / 100f) * 255f
        val colorMatrix = ColorMatrix(
            floatArrayOf(
                1f, 0f, 0f, 0f, normalizedBrightness,
                0f, 1f, 0f, 0f, normalizedBrightness,
                0f, 0f, 1f, 0f, normalizedBrightness,
                0f, 0f, 0f, 1f, 0f
            )
        )
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(source, 0f, 0f, paint)
        return result
    }

    fun adjustContrast(source: Bitmap, contrast: Float): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, source.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()
        val scale = (contrast + 100f) / 100f
        val translate = (-0.5f * scale + 0.5f) * 255f
        val colorMatrix = ColorMatrix(
            floatArrayOf(
                scale, 0f, 0f, 0f, translate,
                0f, scale, 0f, 0f, translate,
                0f, 0f, scale, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(source, 0f, 0f, paint)
        return result
    }

    fun adjustSaturation(source: Bitmap, saturation: Float): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, source.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()
        val normalizedSaturation = (saturation + 100f) / 100f
        val colorMatrix = ColorMatrix()
        colorMatrix.setSaturation(normalizedSaturation)
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(source, 0f, 0f, paint)
        return result
    }

    // ------------------------------------------------------------------
    // Combined pipeline  (main entry-point)
    // ------------------------------------------------------------------

    fun applyAdjustments(
        source: Bitmap,
        brightness: Float,
        contrast: Float,
        saturation: Float,
        exposure: Float = 0f,
        highlights: Float = 0f,
        shadows: Float = 0f,
        vibrance: Float = 0f,
        warmth: Float = 0f,
        tint: Float = 0f,
        hue: Float = 0f,
        sharpness: Float = 0f,
        definition: Float = 0f,
        vignette: Float = 0f,
        glow: Float = 0f
    ): Bitmap {
        // Stage 1 — GPU-friendly ColorMatrix pass (brightness / contrast / sat / exposure)
        var current: Bitmap = applyLinearAdjustments(source, brightness, contrast, saturation, exposure)

        // Stage 2 — per-pixel tone / colour adjustments (highlights, shadows, vibrance, warmth, tint, hue, vignette)
        val afterTone = applyToneAndColorAdjustments(
            source = current,
            highlights = highlights, shadows = shadows,
            vibrance = vibrance, warmth = warmth, tint = tint,
            hue = hue, vignette = vignette
        )
        if (afterTone !== current) { current.recycle(); current = afterTone }

        // Stage 3 — sharpness / definition / glow in a single merged pass
        val afterPost = applyPostEffects(current, sharpness, definition, glow)
        if (afterPost !== current) { current.recycle(); current = afterPost }

        return current
    }

    // ------------------------------------------------------------------
    // Stage 1 — linear (ColorMatrix)
    // ------------------------------------------------------------------

    private fun applyLinearAdjustments(
        source: Bitmap, brightness: Float, contrast: Float,
        saturation: Float, exposure: Float
    ): Bitmap {
        val result = Bitmap.createBitmap(source.width, source.height, source.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint()

        val brightnessMatrix = ColorMatrix()
        brightnessMatrix.set(floatArrayOf(
            1f, 0f, 0f, 0f, (brightness / 100f) * 255f,
            0f, 1f, 0f, 0f, (brightness / 100f) * 255f,
            0f, 0f, 1f, 0f, (brightness / 100f) * 255f,
            0f, 0f, 0f, 1f, 0f
        ))

        val scale = (contrast + 100f) / 100f
        val translate = (-0.5f * scale + 0.5f) * 255f
        val contrastMatrix = ColorMatrix()
        contrastMatrix.set(floatArrayOf(
            scale, 0f, 0f, 0f, translate,
            0f, scale, 0f, 0f, translate,
            0f, 0f, scale, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        ))

        val saturationMatrix = ColorMatrix()
        saturationMatrix.setSaturation((saturation + 100f) / 100f)

        val factor = 2f.pow(exposure / 50f)
        val exposureMatrix = ColorMatrix()
        exposureMatrix.set(floatArrayOf(
            factor, 0f, 0f, 0f, 0f,
            0f, factor, 0f, 0f, 0f,
            0f, 0f, factor, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        ))

        val combined = ColorMatrix()
        combined.postConcat(exposureMatrix)
        combined.postConcat(saturationMatrix)
        combined.postConcat(contrastMatrix)
        combined.postConcat(brightnessMatrix)

        paint.colorFilter = RememberedColorFilter.filter(combined)
        canvas.drawBitmap(source, 0f, 0f, paint)
        return result
    }

    // ------------------------------------------------------------------
    // Stage 2 — per-pixel tone / colour
    // ------------------------------------------------------------------

    /**
     * Cached vignette mask.  Regenerated only when the image dimensions change.
     */
    private var vignetteCache: FloatArray? = null
    private var vignetteCacheW = 0
    private var vignetteCacheH = 0

    private fun getVignetteMask(w: Int, h: Int): FloatArray {
        if (vignetteCache != null && vignetteCacheW == w && vignetteCacheH == h)
            return vignetteCache!!

        val mask = FloatArray(w * h)
        val cx = (w - 1) / 2f
        val cy = (h - 1) / 2f
        val invCx = if (cx == 0f) 0f else 1f / cx
        val invCy = if (cy == 0f) 0f else 1f / cy

        for (y in 0 until h) {
            val ny = (y - cy) * invCy
            val rowBase = y * w
            for (x in 0 until w) {
                val nx = (x - cx) * invCx
                val d = sqrt(nx * nx + ny * ny).coerceIn(0f, 1.2f)
                val vigMask = smoothstep(0.4f, 1.05f, d)
                mask[rowBase + x] = vigMask * vigMask   // store squared; ready to multiply by vig later
            }
        }

        vignetteCache = mask
        vignetteCacheW = w
        vignetteCacheH = h
        return mask
    }

    private fun applyToneAndColorAdjustments(
        source: Bitmap,
        highlights: Float, shadows: Float,
        vibrance: Float, warmth: Float, tint: Float,
        hue: Float, vignette: Float
    ): Bitmap {
        val hasAny = highlights != 0f || shadows != 0f || vibrance != 0f ||
                warmth != 0f || tint != 0f || hue != 0f || vignette != 0f
        if (!hasAny) return source

        val w = source.width
        val h = source.height
        val inPixels = IntArray(w * h)
        source.getPixels(inPixels, 0, w, 0, 0, w, h)
        val out = IntArray(inPixels.size)

        // --- pre-compute normalised constants (once, outside the loop) ---
        val hl  = (highlights / 100f).coerceIn(-1f, 1f)
        val sh  = (shadows  / 100f).coerceIn(-1f, 1f)
        val vib = (vibrance / 100f).coerceIn(-1f, 1f)
        val warm = (warmth  / 100f).coerceIn(-1f, 1f)
        val tn  = (tint     / 100f).coerceIn(-1f, 1f)
        val hueDegrees = hue * 1.8f          // map -100..100 → -180..180
        val vig = (vignette / 100f).coerceIn(0f, 1f)

        val doHighlights = hl != 0f
        val doShadows    = sh != 0f
        val doWarmth     = warm != 0f
        val doTint       = tn != 0f
        val doHue        = hueDegrees != 0f
        val doVibrance   = vib != 0f
        val doVignette   = vig > 0f
        val needLuminance = doHighlights || doShadows
        val doHSV        = doHue || doVibrance   // only enter HSV math when needed

        // Pre-compute warmth / tint deltas (constant across all pixels)
        val warmDelta   = warm * 24f
        val tintGDelta  = tn * -18f
        val tintRBDelta = tn * 9f

        // Vignette mask (cached across calls — only recomputed on size change)
        val vigMask = if (doVignette) getVignetteMask(w, h) else null

        for (idx in inPixels.indices) {
            val c = inPixels[idx]
            val a = (c ushr 24) and 0xFF
            var r = (c ushr 16) and 0xFF
            var g = (c ushr 8)  and 0xFF
            var b =  c          and 0xFF

            // ── Highlights / Shadows ──────────────────────────────────
            if (needLuminance) {
                val lum = (0.2126f * r + 0.7152f * g + 0.0722f * b) / 255f
                if (doHighlights) {
                    val mask = smoothstep(0.55f, 1.0f, lum)
                    if (hl > 0f) {
                        r = (r + (255 - r) * hl * mask).toInt()
                        g = (g + (255 - g) * hl * mask).toInt()
                        b = (b + (255 - b) * hl * mask).toInt()
                    } else {
                        val f = 1f + hl * mask
                        r = (r * f).toInt(); g = (g * f).toInt(); b = (b * f).toInt()
                    }
                }
                if (doShadows) {
                    val mask = smoothstep(0.0f, 0.45f, 1f - lum)
                    if (sh > 0f) {
                        r = (r + (255 - r) * sh * mask).toInt()
                        g = (g + (255 - g) * sh * mask).toInt()
                        b = (b + (255 - b) * sh * mask).toInt()
                    } else {
                        val f = 1f + sh * mask
                        r = (r * f).toInt(); g = (g * f).toInt(); b = (b * f).toInt()
                    }
                }
            }

            // ── Warmth ────────────────────────────────────────────────
            if (doWarmth) {
                r = (r + warmDelta).toInt()
                b = (b - warmDelta).toInt()
            }

            // ── Tint ──────────────────────────────────────────────────
            if (doTint) {
                g = (g + tintGDelta).toInt()
                r = (r + tintRBDelta).toInt()
                b = (b + tintRBDelta).toInt()
            }

            // Clamp before any HSV work
            r = clamp255(r); g = clamp255(g); b = clamp255(b)

            // ── Hue + Vibrance (inline HSV — no Android API call) ─────
            if (doHSV) {
                // ---- RGB → HSV (inline) ----
                val rf = r / 255f; val gf = g / 255f; val bf = b / 255f
                val maxC = max(rf, max(gf, bf))
                val minC = min(rf, min(gf, bf))
                val diff = maxC - minC

                var hsvH: Float
                val hsvS: Float
                val hsvV = maxC

                if (diff == 0f) {
                    hsvH = 0f; hsvS = 0f
                } else {
                    hsvS = diff / maxC
                    hsvH = when (maxC) {
                        rf -> ((gf - bf) / diff) % 6f
                        gf -> (bf - rf) / diff + 2f
                        else -> (rf - gf) / diff + 4f
                    }
                    hsvH *= 60f
                    if (hsvH < 0f) hsvH += 360f
                }

                // ---- apply hue shift ----
                var newH = hsvH
                var newS = hsvS
                if (doHue) {
                    newH = (newH + hueDegrees) % 360f
                    if (newH < 0f) newH += 360f
                }

                // ---- apply vibrance ----
                if (doVibrance) {
                    newS = if (vib > 0f) {
                        (newS + vib * (1f - newS)).coerceIn(0f, 1f)
                    } else {
                        (newS * (1f + vib)).coerceIn(0f, 1f)
                    }
                }

                // ---- HSV → RGB (inline) ----
                val c2   = hsvV * newS
                val x2   = c2 * (1f - abs((newH / 60f) % 2f - 1f))
                val m    = hsvV - c2

                val (r1, g1, b1) = when {
                    newH < 60f  -> Triple(c2, x2, 0f)
                    newH < 120f -> Triple(x2, c2, 0f)
                    newH < 180f -> Triple(0f, c2, x2)
                    newH < 240f -> Triple(0f, x2, c2)
                    newH < 300f -> Triple(x2, 0f, c2)
                    else        -> Triple(c2, 0f, x2)
                }

                r = ((r1 + m) * 255f).toInt()
                g = ((g1 + m) * 255f).toInt()
                b = ((b1 + m) * 255f).toInt()
            }

            // ── Vignette ──────────────────────────────────────────────
            if (doVignette && vigMask != null) {
                val factor = 1f - vig * vigMask[idx]
                r = (r * factor).toInt()
                g = (g * factor).toInt()
                b = (b * factor).toInt()
            }

            out[idx] = (a shl 24) or (clamp255(r) shl 16) or (clamp255(g) shl 8) or clamp255(b)
        }

        val result = Bitmap.createBitmap(w, h, source.config ?: Bitmap.Config.ARGB_8888)
        result.setPixels(out, 0, w, 0, 0, w, h)
        return result
    }

    // ------------------------------------------------------------------
    // Stage 3 — post-effects  (sharpness / definition / glow) — single pass
    // ------------------------------------------------------------------

    /**
     * Merged sharpness + definition + glow.
     * All three need a 3×3 neighbourhood, so we read pixels once and write once.
     * If none are active the source bitmap is returned as-is (no copy).
     */
    private fun applyPostEffects(source: Bitmap, sharpness: Float, definition: Float, glow: Float): Bitmap {
        val sharpAmt  = (sharpness  / 100f).coerceIn(0f, 1f)
        val defAmt    = (definition / 100f).coerceIn(0f, 1f)
        val glowAmt   = (glow       / 100f).coerceIn(0f, 1f)

        if (sharpAmt == 0f && defAmt == 0f && glowAmt == 0f) return source

        val w = source.width
        val h = source.height
        val inPx = IntArray(w * h)
        source.getPixels(inPx, 0, w, 0, 0, w, h)
        val out = IntArray(inPx.size)

        val inv9  = 1f / 9f
        val defK  = defAmt * 0.9f

        var y = 0
        while (y < h) {
            val yUp   = if (y > 0)     y - 1 else 0
            val yDown = if (y < h - 1) y + 1 else h - 1
            val row     = y  * w
            val rowUp   = yUp * w
            val rowDown = yDown * w

            var x = 0
            while (x < w) {
                val xL = if (x > 0)     x - 1 else 0
                val xR = if (x < w - 1) x + 1 else w - 1

                // ── centre pixel ──
                val center = inPx[row + x]
                val a  = (center ushr 24) and 0xFF
                val r0 = (center ushr 16) and 0xFF
                val g0 = (center ushr 8)  and 0xFF
                val b0 =  center          and 0xFF

                // ── 4-neighbour sums (for sharpness) ──
                val left  = inPx[row     + xL]
                val right = inPx[row     + xR]
                val up    = inPx[rowUp   + x ]
                val down  = inPx[rowDown + x ]

                var rOut = r0.toFloat()
                var gOut = g0.toFloat()
                var bOut = b0.toFloat()

                // ── sharpness  (Laplacian-style, 4-tap) ──
                if (sharpAmt > 0f) {
                    val rSharp = 5f * r0 -
                            ((left ushr 16) and 0xFF) - ((right ushr 16) and 0xFF) -
                            ((up   ushr 16) and 0xFF) - ((down  ushr 16) and 0xFF)
                    val gSharp = 5f * g0 -
                            ((left ushr 8)  and 0xFF) - ((right ushr 8)  and 0xFF) -
                            ((up   ushr 8)  and 0xFF) - ((down  ushr 8)  and 0xFF)
                    val bSharp = 5f * b0 -
                            (left  and 0xFF) - (right and 0xFF) -
                            (up    and 0xFF) - (down  and 0xFF)

                    rOut = lerp(rOut, rSharp, sharpAmt)
                    gOut = lerp(gOut, gSharp, sharpAmt)
                    bOut = lerp(bOut, bSharp, sharpAmt)
                }

                // ── definition + glow both need the full 3×3 box mean ──
                if (defAmt > 0f || glowAmt > 0f) {
                    val p00 = inPx[rowUp   + xL]; val p01 = up;                val p02 = inPx[rowUp   + xR]
                    val p10 = left;               val p12 = right
                    val p20 = inPx[rowDown + xL]; val p21 = down;              val p22 = inPx[rowDown + xR]

                    val rSum =
                        ((p00 ushr 16) and 0xFF) + ((p01 ushr 16) and 0xFF) + ((p02 ushr 16) and 0xFF) +
                        ((p10 ushr 16) and 0xFF) +  r0                      + ((p12 ushr 16) and 0xFF) +
                        ((p20 ushr 16) and 0xFF) + ((p21 ushr 16) and 0xFF) + ((p22 ushr 16) and 0xFF)
                    val gSum =
                        ((p00 ushr 8)  and 0xFF) + ((p01 ushr 8)  and 0xFF) + ((p02 ushr 8)  and 0xFF) +
                        ((p10 ushr 8)  and 0xFF) +  g0                      + ((p12 ushr 8)  and 0xFF) +
                        ((p20 ushr 8)  and 0xFF) + ((p21 ushr 8)  and 0xFF) + ((p22 ushr 8)  and 0xFF)
                    val bSum =
                        (p00 and 0xFF) + (p01 and 0xFF) + (p02 and 0xFF) +
                        (p10 and 0xFF) +  b0            + (p12 and 0xFF) +
                        (p20 and 0xFF) + (p21 and 0xFF) + (p22 and 0xFF)

                    val rMean = rSum * inv9
                    val gMean = gSum * inv9
                    val bMean = bSum * inv9

                    // definition  (local-contrast / clarity)
                    if (defAmt > 0f) {
                        rOut += (rOut - rMean) * defK
                        gOut += (gOut - gMean) * defK
                        bOut += (bOut - bMean) * defK
                    }

                    // glow  (screen-blend with box-blurred copy)
                    if (glowAmt > 0f) {
                        val rGlow = 255f - (255f - rOut) * (255f - rMean) / 255f
                        val gGlow = 255f - (255f - gOut) * (255f - gMean) / 255f
                        val bGlow = 255f - (255f - bOut) * (255f - bMean) / 255f
                        rOut = lerp(rOut, rGlow, glowAmt)
                        gOut = lerp(gOut, gGlow, glowAmt)
                        bOut = lerp(bOut, bGlow, glowAmt)
                    }
                }

                out[row + x] =
                    (a shl 24) or
                    (clamp255(rOut.toInt()) shl 16) or
                    (clamp255(gOut.toInt()) shl 8) or
                    clamp255(bOut.toInt())
                x++
            }
            y++
        }

        val result = Bitmap.createBitmap(w, h, source.config ?: Bitmap.Config.ARGB_8888)
        result.setPixels(out, 0, w, 0, 0, w, h)
        return result
    }

    // ------------------------------------------------------------------
    // Utility
    // ------------------------------------------------------------------

    private fun clamp255(v: Int): Int = v.coerceIn(0, 255)

    private fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
        if (edge0 == edge1) return 0f
        val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    /**
     * Minor allocation saver: reuse a ColorMatrixColorFilter when possible.
     */
    private object RememberedColorFilter {
        private var last: ColorMatrixColorFilter? = null
        private var lastKey = 0

        fun filter(matrix: ColorMatrix): ColorMatrixColorFilter {
            val key = matrix.hashCode()
            if (last != null && key == lastKey) return last!!
            val f = ColorMatrixColorFilter(matrix)
            last = f; lastKey = key
            return f
        }
    }
}