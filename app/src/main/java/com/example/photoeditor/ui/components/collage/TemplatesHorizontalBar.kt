package com.example.photoeditor.ui.components.collage

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.photoeditor.model.CollageTemplate
import kotlinx.coroutines.launch

/**
 * Horizontal scroll bar of collage templates (for portrait layout like Gallery).
 */
@Composable
fun TemplatesHorizontalBar(
    templates: List<CollageTemplate>,
    selectedTemplate: CollageTemplate?,
    onTemplateSelected: (CollageTemplate) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(selectedTemplate) {
        selectedTemplate?.let { template ->
            val index = templates.indexOf(template)
            if (index >= 0) {
                scope.launch {
                    listState.animateScrollToItem(index)
                }
            }
        }
    }

    LazyRow(
        state = listState,
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        items(templates) { template ->
            TemplatePreviewItem(
                template = template,
                isSelected = template.id == selectedTemplate?.id,
                onClick = { onTemplateSelected(template) },
                modifier = Modifier.size(80.dp)
            )
        }
    }
}
