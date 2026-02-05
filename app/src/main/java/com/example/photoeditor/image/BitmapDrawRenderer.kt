package com.example.photoeditor.image

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import com.example.photoeditor.model.DrawPath
import androidx.compose.ui.geometry.Offset

object BitmapDrawRenderer {

    fun render(bitmap: Bitmap, drawPaths: List<DrawPath>): Bitmap {
        if (drawPaths.isEmpty()) return bitmap
        val result = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val w = result.width.toFloat()
        val h = result.height.toFloat()
        for (path in drawPaths) {
            if (path.points.size < 2) continue
            paint.color = path.color
            paint.strokeWidth = path.strokeWidthNorm * w
            val androidPath = Path()
            androidPath.moveTo(path.points[0].x * w, path.points[0].y * h)
            for (i in 1 until path.points.size) {
                androidPath.lineTo(path.points[i].x * w, path.points[i].y * h)
            }
            canvas.drawPath(androidPath, paint)
        }
        return result
    }
}
