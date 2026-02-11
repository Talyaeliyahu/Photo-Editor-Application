package com.example.photoeditor.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.photoeditor.R
import com.example.photoeditor.image.filters.BlurFilter
import com.example.photoeditor.image.filters.CoolFilter
import com.example.photoeditor.image.filters.DramaticFilter
import com.example.photoeditor.image.filters.FadeFilter
import com.example.photoeditor.image.filters.GrayscaleFilter
import com.example.photoeditor.image.filters.InvertFilter
import com.example.photoeditor.image.filters.NoirFilter
import com.example.photoeditor.image.filters.PastelFilter
import com.example.photoeditor.image.filters.SepiaFilter
import com.example.photoeditor.image.filters.VintageFilter
import com.example.photoeditor.image.filters.VividFilter
import com.example.photoeditor.image.filters.WarmFilter
import com.example.photoeditor.image.filters.NeonFilter
import com.example.photoeditor.image.filters.RetroFilter
import com.example.photoeditor.image.filters.DreamyFilter
import com.example.photoeditor.image.filters.SunsetFilter
import com.example.photoeditor.image.filters.CleanSkinFilter
import com.example.photoeditor.image.filters.EmbossFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Filter options panel
 */
@Composable
fun FilterOptionsPanel(
    activeFilterType: com.example.photoeditor.viewmodel.FilterType?,
    originalBitmap: Bitmap?,
    onFilterSelected: (com.example.photoeditor.viewmodel.FilterType?) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 3 columns (3x3 layout style). Easy to extend when adding more filters later.
            val options = listOf(
                FilterOptionSpec(
                    label = stringResource(R.string.original),
                    type = null
                ),
                FilterOptionSpec(
                    label = stringResource(R.string.grayscale),
                    type = com.example.photoeditor.viewmodel.FilterType.GRAYSCALE
                ),
                FilterOptionSpec(
                    label = stringResource(R.string.sepia),
                    type = com.example.photoeditor.viewmodel.FilterType.SEPIA
                ),
                FilterOptionSpec(
                    label = stringResource(R.string.blur),
                    type = com.example.photoeditor.viewmodel.FilterType.BLUR
                ),
                FilterOptionSpec(
                    label = stringResource(R.string.filter_vivid),
                    type = com.example.photoeditor.viewmodel.FilterType.VIVID
                ),
                FilterOptionSpec(
                    label = stringResource(R.string.filter_noir),
                    type = com.example.photoeditor.viewmodel.FilterType.NOIR
                ),
                FilterOptionSpec(
                    label = stringResource(R.string.filter_warm),
                    type = com.example.photoeditor.viewmodel.FilterType.WARM
                ),
                FilterOptionSpec(
                    label = stringResource(R.string.filter_cool),
                    type = com.example.photoeditor.viewmodel.FilterType.COOL
                ),
                FilterOptionSpec(
                    label = stringResource(R.string.filter_vintage),
                    type = com.example.photoeditor.viewmodel.FilterType.VINTAGE
                ),
                FilterOptionSpec(
                    label = stringResource(R.string.filter_invert),
                    type = com.example.photoeditor.viewmodel.FilterType.INVERT
                ),
                FilterOptionSpec(
                    label = stringResource(R.string.filter_fade),
                    type = com.example.photoeditor.viewmodel.FilterType.FADE
                ),
                FilterOptionSpec(
                    label = stringResource(R.string.filter_dramatic),
                    type = com.example.photoeditor.viewmodel.FilterType.DRAMATIC
                ),
                FilterOptionSpec(
                    label = stringResource(R.string.filter_pastel),
                    type = com.example.photoeditor.viewmodel.FilterType.PASTEL
                ),
                FilterOptionSpec(
                    label = stringResource(R.string.filter_neon),
                    type = com.example.photoeditor.viewmodel.FilterType.NEON
                ),
                FilterOptionSpec(
                    label = stringResource(R.string.filter_retro),
                    type = com.example.photoeditor.viewmodel.FilterType.RETRO
                ),
                FilterOptionSpec(
                    label = stringResource(R.string.filter_dreamy),
                    type = com.example.photoeditor.viewmodel.FilterType.DREAMY
                ),
                FilterOptionSpec(
                    label = stringResource(R.string.filter_sunset),
                    type = com.example.photoeditor.viewmodel.FilterType.SUNSET
                ),
                FilterOptionSpec(
                    label = stringResource(R.string.filter_clean_skin),
                    type = com.example.photoeditor.viewmodel.FilterType.CLEAN_SKIN
                ),
                FilterOptionSpec(
                    label = stringResource(R.string.filter_emboss),
                    type = com.example.photoeditor.viewmodel.FilterType.EMBOSS
                )
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(options.size) { index ->
                    val opt = options[index]
                    FilterOptionButton(
                        label = opt.label,
                        onClick = { onFilterSelected(opt.type) },
                        isSelected = activeFilterType == opt.type,
                        previewBitmap = originalBitmap,
                        filterType = opt.type,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

private data class FilterOptionSpec(
    val label: String,
    val type: com.example.photoeditor.viewmodel.FilterType?
)

/**
 * Single row of filter options with horizontal scroll.
 * Used in portrait mode above the bottom navigation bar.
 */
@Composable
fun FiltersHorizontalBar(
    activeFilterType: com.example.photoeditor.viewmodel.FilterType?,
    originalBitmap: Bitmap?,
    onFilterSelected: (com.example.photoeditor.viewmodel.FilterType?) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf(
        FilterOptionSpec(label = stringResource(R.string.original), type = null),
        FilterOptionSpec(label = stringResource(R.string.grayscale), type = com.example.photoeditor.viewmodel.FilterType.GRAYSCALE),
        FilterOptionSpec(label = stringResource(R.string.sepia), type = com.example.photoeditor.viewmodel.FilterType.SEPIA),
        FilterOptionSpec(label = stringResource(R.string.blur), type = com.example.photoeditor.viewmodel.FilterType.BLUR),
        FilterOptionSpec(label = stringResource(R.string.filter_vivid), type = com.example.photoeditor.viewmodel.FilterType.VIVID),
        FilterOptionSpec(label = stringResource(R.string.filter_noir), type = com.example.photoeditor.viewmodel.FilterType.NOIR),
        FilterOptionSpec(label = stringResource(R.string.filter_warm), type = com.example.photoeditor.viewmodel.FilterType.WARM),
        FilterOptionSpec(label = stringResource(R.string.filter_cool), type = com.example.photoeditor.viewmodel.FilterType.COOL),
        FilterOptionSpec(label = stringResource(R.string.filter_vintage), type = com.example.photoeditor.viewmodel.FilterType.VINTAGE),
        FilterOptionSpec(label = stringResource(R.string.filter_invert), type = com.example.photoeditor.viewmodel.FilterType.INVERT),
        FilterOptionSpec(label = stringResource(R.string.filter_fade), type = com.example.photoeditor.viewmodel.FilterType.FADE),
        FilterOptionSpec(label = stringResource(R.string.filter_dramatic), type = com.example.photoeditor.viewmodel.FilterType.DRAMATIC),
        FilterOptionSpec(label = stringResource(R.string.filter_pastel), type = com.example.photoeditor.viewmodel.FilterType.PASTEL),
        FilterOptionSpec(label = stringResource(R.string.filter_neon), type = com.example.photoeditor.viewmodel.FilterType.NEON),
        FilterOptionSpec(label = stringResource(R.string.filter_retro), type = com.example.photoeditor.viewmodel.FilterType.RETRO),
        FilterOptionSpec(label = stringResource(R.string.filter_dreamy), type = com.example.photoeditor.viewmodel.FilterType.DREAMY),
        FilterOptionSpec(label = stringResource(R.string.filter_sunset), type = com.example.photoeditor.viewmodel.FilterType.SUNSET),
        FilterOptionSpec(label = stringResource(R.string.filter_clean_skin), type = com.example.photoeditor.viewmodel.FilterType.CLEAN_SKIN),
        FilterOptionSpec(label = stringResource(R.string.filter_emboss), type = com.example.photoeditor.viewmodel.FilterType.EMBOSS)
    )

    LazyRow(
        modifier = modifier.fillMaxHeight(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        items(options) { opt ->
            FilterOptionButton(
                label = opt.label,
                onClick = { onFilterSelected(opt.type) },
                isSelected = activeFilterType == opt.type,
                previewBitmap = originalBitmap,
                filterType = opt.type,
                modifier = Modifier.size(88.dp)
            )
        }
    }
}

/**
 * Filter option button with preview
 */
@Composable
fun FilterOptionButton(
    label: String,
    onClick: () -> Unit,
    isSelected: Boolean = false,
    previewBitmap: Bitmap?,
    filterType: com.example.photoeditor.viewmodel.FilterType?,
    modifier: Modifier = Modifier
) {
    var previewImage by remember { mutableStateOf<Bitmap?>(null) }
    
    // Generate preview when bitmap is available
    LaunchedEffect(previewBitmap, filterType) {
        previewImage = previewBitmap?.let { bitmap ->
            // Create a small thumbnail for preview (max 200px)
            val maxSize = 200
            val scale = minOf(
                maxSize.toFloat() / bitmap.width,
                maxSize.toFloat() / bitmap.height
            ).coerceAtMost(1f)
            
            val thumbnailWidth = (bitmap.width * scale).toInt()
            val thumbnailHeight = (bitmap.height * scale).toInt()
            
            val thumbnail = withContext(Dispatchers.Default) {
                Bitmap.createScaledBitmap(bitmap, thumbnailWidth, thumbnailHeight, true)
            }
            
            // Apply filter to thumbnail (or keep original if null)
            withContext(Dispatchers.Default) {
                when (filterType) {
                    null -> thumbnail
                    com.example.photoeditor.viewmodel.FilterType.GRAYSCALE -> GrayscaleFilter().apply(thumbnail)
                    com.example.photoeditor.viewmodel.FilterType.SEPIA -> SepiaFilter().apply(thumbnail)
                    com.example.photoeditor.viewmodel.FilterType.BLUR -> BlurFilter(radius = 3).apply(thumbnail)
                    com.example.photoeditor.viewmodel.FilterType.INVERT -> InvertFilter().apply(thumbnail)
                    com.example.photoeditor.viewmodel.FilterType.NOIR -> NoirFilter().apply(thumbnail)
                    com.example.photoeditor.viewmodel.FilterType.VIVID -> VividFilter().apply(thumbnail)
                    com.example.photoeditor.viewmodel.FilterType.WARM -> WarmFilter().apply(thumbnail)
                    com.example.photoeditor.viewmodel.FilterType.COOL -> CoolFilter().apply(thumbnail)
                    com.example.photoeditor.viewmodel.FilterType.VINTAGE -> VintageFilter().apply(thumbnail)
                    com.example.photoeditor.viewmodel.FilterType.FADE -> FadeFilter().apply(thumbnail)
                    com.example.photoeditor.viewmodel.FilterType.DRAMATIC -> DramaticFilter().apply(thumbnail)
                    com.example.photoeditor.viewmodel.FilterType.PASTEL -> PastelFilter().apply(thumbnail)
                    com.example.photoeditor.viewmodel.FilterType.NEON -> NeonFilter().apply(thumbnail)
                    com.example.photoeditor.viewmodel.FilterType.RETRO -> RetroFilter().apply(thumbnail)
                    com.example.photoeditor.viewmodel.FilterType.DREAMY -> DreamyFilter().apply(thumbnail)
                    com.example.photoeditor.viewmodel.FilterType.SUNSET -> SunsetFilter().apply(thumbnail)
                    com.example.photoeditor.viewmodel.FilterType.CLEAN_SKIN -> CleanSkinFilter().apply(thumbnail)
                    com.example.photoeditor.viewmodel.FilterType.EMBOSS -> EmbossFilter().apply(thumbnail)
                }
            }
        }
    }
    val textColor = MaterialTheme.colorScheme.onSurface // Dark text for all buttons
    
    // Border only when selected - rectangular frame (not rounded)
    val borderModifier = if (isSelected) {
        Modifier.border(
            width = 2.dp,
            color = MaterialTheme.colorScheme.primary,
            shape = RectangleShape
        )
    } else {
        Modifier
    }
    
    Box(
        modifier = modifier
            .aspectRatio(1f) // Square shape
            .then(borderModifier)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null // No gray overlay on hover/press
            ) { onClick() }
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Preview image as background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                if (previewImage != null) {
                    Image(
                        bitmap = previewImage!!.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Placeholder when no image
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "?",
                            fontSize = 24.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
            
            // Label as overlay on top of image
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .background(
                            color = Color.Black.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(2.dp)
                        )
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }
    }
}
