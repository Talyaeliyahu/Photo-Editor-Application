package com.example.photoeditor.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.photoeditor.R

/**
 * Image indicator showing ORIGINAL or EDITED
 */
@Composable
fun ImageIndicator(
    showOriginal: Boolean,
    modifier: Modifier = Modifier
) {
    // Light-blue tag background + white text (requested)
    val tagBlue = Color(0xFF4FC3F7) // Light Blue 300
    Surface(
        modifier = modifier.padding(12.dp),
        shape = RoundedCornerShape(8.dp),
        color = tagBlue.copy(alpha = 0.92f)
    ) {
        Text(
            text = if (showOriginal) stringResource(R.string.original) else stringResource(R.string.edited),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}
