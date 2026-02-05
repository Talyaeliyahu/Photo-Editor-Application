package com.example.photoeditor.ui.components.draw

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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.photoeditor.R

@Composable
fun DrawToolPanel(
    strokeWidth: Float,
    drawColorArgb: Int,
    onStrokeWidthChange: (Float) -> Unit,
    onColorChange: (Int) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = listOf(
        Color.Black,
        Color.White,
        Color.Red,
        Color(0xFFFF9800), // Orange
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

    val titleColor = Color.White

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Slider and title on the same row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.draw_stroke_width),
                style = MaterialTheme.typography.titleSmall,
                color = titleColor,
                modifier = Modifier.widthIn(min = 70.dp)
            )
            Slider(
                value = strokeWidth,
                onValueChange = onStrokeWidthChange,
                valueRange = 0.005f..0.05f,
                modifier = Modifier.weight(1f)
            )
        }
        Text(
            text = stringResource(R.string.draw_color),
            style = MaterialTheme.typography.titleSmall,
            color = titleColor
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            colors.forEach { color ->
                val colorArgb = color.toArgb()
                val border = when {
                    drawColorArgb == colorArgb -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                    colorArgb == android.graphics.Color.BLACK -> BorderStroke(1.dp, Color.White)
                    else -> null
                }
                Surface(
                    shape = CircleShape,
                    color = color,
                    modifier = Modifier.size(28.dp),
                    onClick = { onColorChange(colorArgb) },
                    border = border
                ) {}
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = onClear,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        ) {
            Text(
                text = stringResource(R.string.draw_clear),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
