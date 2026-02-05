package com.example.photoeditor.ui.components.collage

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.photoeditor.model.CollageTemplate
import kotlinx.coroutines.launch

/**
 * Template carousel showing visual previews of templates in vertical grid
 */
@Composable
fun TemplateCarousel(
    templates: List<CollageTemplate>,
    selectedTemplate: CollageTemplate?,
    onTemplateSelected: (CollageTemplate) -> Unit,
    modifier: Modifier = Modifier
) {
    val gridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()
    
    // Scroll to selected template
    LaunchedEffect(selectedTemplate) {
        selectedTemplate?.let { template ->
            val index = templates.indexOf(template)
            if (index >= 0) {
                coroutineScope.launch {
                    gridState.animateScrollToItem(index)
                }
            }
        }
    }

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(3),
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp)
    ) {
        items(templates) { template ->
            TemplatePreviewItem(
                template = template,
                isSelected = template.id == selectedTemplate?.id,
                onClick = { onTemplateSelected(template) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
