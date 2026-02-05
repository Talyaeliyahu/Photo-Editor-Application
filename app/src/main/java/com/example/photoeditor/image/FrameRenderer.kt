package com.example.photoeditor.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.graphics.drawable.NinePatchDrawable
import androidx.core.content.ContextCompat
import com.example.photoeditor.model.FrameConfig
import com.example.photoeditor.model.FrameType
import kotlin.math.min

/**
 * Professional frame rendering engine.
 * 
 * Applies frames to bitmaps in a resolution-independent way.
 * All frame dimensions are calculated as percentages of the bitmap size.
 * 
 * Key principles:
 * - Works only on the final bitmap (after all edits)
 * - Frame thickness is relative to bitmap size (never fixed pixels)
 * - Supports both stroke-based and image-based frames
 * - Optional nine-patch support for frames with fixed corners
 */
object FrameRenderer {
    
    /**
     * Applies a frame to the given bitmap.
     * 
     * @param context Application context (needed for loading drawable resources)
     * @param source The source bitmap (final edited image)
     * @param config Frame configuration
     * @return New bitmap with frame applied, or original bitmap if config is NONE
     */
    fun applyFrame(context: Context, source: Bitmap, config: FrameConfig): Bitmap {
        // No frame requested
        if (config.type == FrameType.NONE) {
            return source
        }
        
        val bitmapWidth = source.width.toFloat()
        val bitmapHeight = source.height.toFloat()
        val minDimension = min(bitmapWidth, bitmapHeight)
        
        when (config.type) {
            FrameType.STROKE -> {
                val output = if (config.cornerRadiusPercent > 0f) {
                    // Rounded frame: create fresh bitmap, clip image to rounded rect, then draw stroke
                    createRoundedStrokeFrame(source, bitmapWidth, bitmapHeight, config)
                } else {
                    val out = source.copy(source.config ?: Bitmap.Config.ARGB_8888, true)
                    val canvas = Canvas(out)
                    drawStrokeFrame(canvas, bitmapWidth, bitmapHeight, minDimension, config)
                    out
                }
                return output
            }
            
            FrameType.IMAGE -> {
                val output = source.copy(source.config ?: Bitmap.Config.ARGB_8888, true)
                val canvas = Canvas(output)
                drawImageFrame(
                    context = context,
                    canvas = canvas,
                    bitmapWidth = source.width,
                    bitmapHeight = source.height,
                    config = config
                )
                return output
            }
            
            FrameType.NONE -> {
                // Already handled above
            }
        }
        
        return source
    }
    
    /**
     * Creates output with image clipped to rounded corners, then draws the stroke frame.
     */
    private fun createRoundedStrokeFrame(
        source: Bitmap,
        bitmapWidth: Float,
        bitmapHeight: Float,
        config: FrameConfig
    ): Bitmap {
        val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val thickness = (bitmapWidth * config.thicknessPercent.coerceIn(0.005f, 0.1f))
        val cornerRadius = bitmapWidth * config.cornerRadiusPercent.coerceIn(0f, 0.2f)
        val inset = thickness / 2f
        
        val clipRect = RectF(inset, inset, bitmapWidth - inset, bitmapHeight - inset)
        val clipPath = Path().apply {
            addRoundRect(clipRect, cornerRadius, cornerRadius, Path.Direction.CW)
        }
        
        canvas.save()
        canvas.clipPath(clipPath)
        canvas.drawBitmap(source, 0f, 0f, null)
        canvas.restore()
        
        drawStrokeFrame(canvas, bitmapWidth, bitmapHeight, min(bitmapWidth, bitmapHeight), config)
        return output
    }
    
    /**
     * Draws a stroke-based frame on the canvas.
     * Frame thickness is calculated as a percentage of the bitmap width.
     */
    private fun drawStrokeFrame(
        canvas: Canvas,
        bitmapWidth: Float,
        bitmapHeight: Float,
        minDimension: Float,
        config: FrameConfig
    ) {
        // Calculate frame thickness as percentage of bitmap width
        // Clamp to reasonable range (0.5% to 10% of width)
        val thickness = (bitmapWidth * config.thicknessPercent.coerceIn(0.005f, 0.1f))
        
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = config.color
            style = Paint.Style.STROKE
            strokeWidth = thickness
        }
        
        if (config.cornerRadiusPercent > 0f) {
            // Rounded rectangle frame
            val cornerRadius = bitmapWidth * config.cornerRadiusPercent.coerceIn(0f, 0.2f)
            val inset = thickness / 2f
            
            val rect = RectF(
                inset,
                inset,
                bitmapWidth - inset,
                bitmapHeight - inset
            )
            
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)
        } else {
            // Square frame - draw four sides
            val halfThickness = thickness / 2f
            
            // Top
            canvas.drawRect(
                0f,
                0f,
                bitmapWidth,
                thickness,
                paint.apply { style = Paint.Style.FILL }
            )
            
            // Bottom
            canvas.drawRect(
                0f,
                bitmapHeight - thickness,
                bitmapWidth,
                bitmapHeight,
                paint
            )
            
            // Left
            canvas.drawRect(
                0f,
                0f,
                thickness,
                bitmapHeight,
                paint
            )
            
            // Right
            canvas.drawRect(
                bitmapWidth - thickness,
                0f,
                bitmapWidth,
                bitmapHeight,
                paint
            )
        }
    }
    
    /**
     * Draws an image-based frame (PNG/WEBP) on the canvas.
     * The frame image is scaled to match the bitmap dimensions.
     * Supports nine-patch for frames with fixed corners.
     */
    private fun drawImageFrame(
        context: Context,
        canvas: Canvas,
        bitmapWidth: Int,
        bitmapHeight: Int,
        config: FrameConfig
    ) {
        val frameResId = config.frameImageResId ?: return
        
        try {
            val drawable: Drawable? = ContextCompat.getDrawable(context, frameResId)
            if (drawable == null) {
                return
            }
            
            // Set bounds to match bitmap size
            drawable.setBounds(0, 0, bitmapWidth, bitmapHeight)
            
            if (config.useNinePatch && drawable is NinePatchDrawable) {
                // Use nine-patch scaling (preserves corners, stretches edges)
                drawable.draw(canvas)
            } else {
                // Regular drawable scaling
                drawable.draw(canvas)
            }
        } catch (e: Exception) {
            // Frame image failed to load - silently fail (no frame applied)
            // In production, you might want to log this
        }
    }
}
