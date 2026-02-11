package com.example.photoeditor.ui.components.text

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.photoeditor.R
import com.example.photoeditor.model.TextOverlay
import com.example.photoeditor.ui.components.AdjustmentIconButton

/**
 * Horizontal bar for Text category: "New" button + one button per text overlay.
 * "New" opens the sheet to add text; text buttons select that overlay for move/resize.
 */
@Composable
fun TextHorizontalBar(
    textOverlays: List<TextOverlay>,
    selectedTextId: String?,
    onAddNew: () -> Unit,
    onSelectText: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxHeight(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(2.dp)
            ) {
                AdjustmentIconButton(
                    icon = Icons.Default.Add,
                    isSelected = false,
                    onClick = onAddNew
                )
                Text(
                    text = stringResource(R.string.new_text),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
            }
        }
        items(textOverlays) { overlay ->
            val displayText = overlay.text.take(8).ifEmpty { "..." }
            val isSelected = overlay.id == selectedTextId
            Surface(
                onClick = { onSelectText(overlay.id) },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.padding(2.dp)
            ) {
                Text(
                    text = displayText,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                )
            }
        }
    }
}
