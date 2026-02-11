package com.example.photoeditor.ui.components.draw

import android.graphics.Color
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.photoeditor.R

@Composable
fun DrawHorizontalBar(
    strokeWidth: Float,
    drawColorArgb: Int,
    onStrokeWidthChange: (Float) -> Unit,
    onColorChange: (Int) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = listOf(
        ComposeColor.Black,
        ComposeColor.White,
        ComposeColor.Red,
        ComposeColor(0xFFFF9800),
        ComposeColor.Yellow,
        ComposeColor.Green,
        ComposeColor.Cyan,
        ComposeColor.Blue,
        ComposeColor(0xFF9C27B0),
        ComposeColor(0xFFE91E63),
        ComposeColor(0xFF00BCD4),
        ComposeColor(0xFF4CAF50),
        ComposeColor(0xFFFFEB3B),
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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onClear,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.draw_clear),
                    style = MaterialTheme.typography.labelMedium
                )
            }
            Slider(
                value = strokeWidth,
                onValueChange = onStrokeWidthChange,
                valueRange = 0.005f..0.05f,
                modifier = Modifier.widthIn(min = 80.dp, max = 140.dp)
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
                val colorArgb = color.toArgb()
                val border = when {
                    drawColorArgb == colorArgb -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                    colorArgb == Color.BLACK -> BorderStroke(1.dp, ComposeColor.White)
                    else -> null
                }
                Surface(
                    shape = CircleShape,
                    color = color,
                    modifier = Modifier.size(24.dp),
                    onClick = { onColorChange(colorArgb) },
                    border = border
                ) {}
            }
        }
    }
}
