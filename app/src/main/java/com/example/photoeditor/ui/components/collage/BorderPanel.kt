package com.example.photoeditor.ui.components.collage

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.photoeditor.R

/**
 * Border panel for editing border width and color.
 * Layout matches the draw tool panel: "עובי קו" + slider, then "צבע" + circular swatches.
 */
@Composable
fun BorderPanel(
    borderWidth: Float,
    borderColor: Int,
    onBorderWidthChange: (Float) -> Unit,
    onBorderColorChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Same colors as draw tool panel
    val colors = listOf(
        Color.Black,
        Color.White,
        Color.Red,
        Color(0xFFFF9800),   // Orange
        Color.Yellow,
        Color.Green,
        Color.Cyan,
        Color.Blue,
        Color(0xFF9C27B0),  // Purple
        Color(0xFFE91E63),  // Pink
        Color(0xFF00BCD4),  // Light blue
        Color(0xFF4CAF50),  // Material green
        Color(0xFFFFEB3B),  // Material yellow
    )

    Card(
        modifier = modifier.padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A1A)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // עובי קו - thickness slider (same layout as draw panel)
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

            // צבע - color swatches (circular, like draw panel)
            Text(
                text = stringResource(R.string.draw_color),
                style = MaterialTheme.typography.titleSmall,
                color = Color.White
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                        modifier = Modifier.size(28.dp),
                        onClick = { onBorderColorChange(colorInt) },
                        border = border
                    ) {}
                }
            }
        }
    }
}
