package com.example.photoeditor.ui.components.collage

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.photoeditor.R

/**
 * Compact horizontal bar for border controls (portrait layout like Gallery).
 */
@Composable
fun BorderHorizontalBar(
    borderWidth: Float,
    borderColor: Int,
    onBorderWidthChange: (Float) -> Unit,
    onBorderColorChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = listOf(
        Color.Black,
        Color.White,
        Color.Red,
        Color(0xFFFF9800),
        Color.Yellow,
        Color.Green,
        Color.Cyan,
        Color.Blue,
        Color(0xFF9C27B0),
        Color(0xFFE91E63),
        Color(0xFF00BCD4),
        Color(0xFF4CAF50),
        Color(0xFFFFEB3B),
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.draw_stroke_width),
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                modifier = Modifier.widthIn(min = 70.dp)
            )
            Slider(
                value = borderWidth.coerceIn(0f, 24f),
                onValueChange = onBorderWidthChange,
                valueRange = 0f..24f,
                steps = 12,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                )
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            colors.forEach { color ->
                val colorInt = android.graphics.Color.argb(
                    (color.alpha * 255).toInt(),
                    (color.red * 255).toInt(),
                    (color.green * 255).toInt(),
                    (color.blue * 255).toInt()
                )
                val border = when {
                    borderColor == colorInt -> BorderStroke(2.dp, Color.White)
                    color == Color.Black -> BorderStroke(1.dp, Color.White)
                    else -> null
                }
                Surface(
                    shape = CircleShape,
                    color = color,
                    modifier = Modifier.size(24.dp),
                    onClick = { onBorderColorChange(colorInt) },
                    border = border
                ) {}
            }
        }
    }
}
