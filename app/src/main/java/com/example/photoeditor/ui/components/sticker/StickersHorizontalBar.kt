package com.example.photoeditor.ui.components.sticker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.photoeditor.R

@Composable
fun StickersHorizontalBar(
    selectedId: String?,
    onAddSticker: (String) -> Unit,
    onDeleteSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    val catalog = listOf(
        "😀", "😍", "🥳", "😎", "😂", "😭", "🤩", "😊", "🥰", "😇", "🙃", "😘",
        "❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍", "💕", "💖", "💗",
        "✨", "⭐️", "🌟", "💫", "🔥", "💯", "🎉", "🎈", "🎊", "🎁", "🏆",
        "🌸", "🌼", "🌺", "🌻", "🌹", "💐", "🍀", "🍭", "🍬", "🍩", "🍪", "☕",
        "👑", "💎", "💍", "🌈", "☀️", "🌙", "⭐", "🦋", "🐝", "🐶", "🐱", "🦊"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selectedId != null) {
            IconButton(onClick = onDeleteSelected) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete_sticker),
                    tint = androidx.compose.ui.graphics.Color.White
                )
            }
        }
        LazyHorizontalGrid(
            rows = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            items(catalog) { emoji ->
                OutlinedButton(
                    onClick = { onAddSticker(emoji) },
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.padding(2.dp)
                ) {
                    Text(
                        text = emoji,
                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

