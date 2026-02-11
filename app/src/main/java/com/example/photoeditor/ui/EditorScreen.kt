package com.example.photoeditor.ui

import android.graphics.Bitmap
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Shadow

import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import android.content.res.Configuration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.example.photoeditor.ui.components.*
import com.example.photoeditor.ui.components.draw.DrawHorizontalBar
import com.example.photoeditor.ui.components.frame.FramesHorizontalBar
import com.example.photoeditor.ui.components.draw.DrawToolPanel
import com.example.photoeditor.ui.components.sticker.StickersHorizontalBar
import com.example.photoeditor.ui.components.draw.DrawingOverlay
import com.example.photoeditor.ui.components.draw.DrawingPathsCanvas
import com.example.photoeditor.ui.theme.EditorBlackTheme
import com.example.photoeditor.viewmodel.EditorViewModel
import com.example.photoeditor.model.TextOverlay
import com.example.photoeditor.model.StickerOverlay
import com.example.photoeditor.model.FrameConfig
import kotlinx.coroutines.delay
import kotlin.math.min

/**
 * Main editor screen composable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    onSelectImage: () -> Unit,
    onLanguageChange: ((String) -> Unit)? = null,
    onBackClick: (() -> Unit)? = null,
    onSaveComplete: (() -> Unit)? = null
) {
    EditorBlackTheme {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        // Track previous saving state to detect when save completes
        var wasSaving by remember { mutableStateOf(false) }

        // When save completes (isSaving changes from true to false), call callback
        LaunchedEffect(uiState.isSaving) {
            if (wasSaving && !uiState.isSaving) {
                onSaveComplete?.invoke()
            }
            wasSaving = uiState.isSaving
        }

        // Track which category is selected - default to ADJUSTMENTS when image is loaded
        var selectedCategory by remember { mutableStateOf<MenuCategory>(MenuCategory.NONE) }
        // Track which adjustment is selected
        var selectedAdjustment by remember { mutableStateOf<AdjustmentType?>(null) }
        // Track which transform tool is selected (crop/aspect sub-panels)
        var selectedTransformTool by remember { mutableStateOf<TransformTool?>(null) }
        // Fullscreen crop mode (interactive crop on the image)
        var isCropping by remember { mutableStateOf(false) }
        // Track if cancel dialog should be shown
        var showCancelDialog by remember { mutableStateOf(false) }
        // Dismiss other-category sheet when starting crop (so user sees full image)
        var dismissOtherCategorySheet by remember { mutableStateOf(false) }
        // Show Text sheet only when "New" is clicked (not auto when selecting TEXT category)
        var showTextSheet by remember { mutableStateOf(false) }
        // Show Custom frame sheet when "Custom" is tapped from Frames bar
        var showCustomFrameSheet by remember { mutableStateOf(false) }

        // Remember original adjustment values when opening an adjustment (for cancel/close behavior)
        val originalAdjustmentValues = remember { mutableStateMapOf<AdjustmentType, Float>() }

        // When image is loaded, set default category to ADJUSTMENTS
        LaunchedEffect(uiState.hasImage) {
            if (uiState.hasImage && selectedCategory == MenuCategory.NONE) {
                selectedCategory = MenuCategory.ADJUSTMENTS
            } else if (!uiState.hasImage) {
                selectedCategory = MenuCategory.NONE
                selectedAdjustment = null
            }
        }

        // Hide Android system nav bar (triangle, square, circle) when adjustment slider is open
        val view = LocalView.current
        LaunchedEffect(selectedAdjustment) {
            (view.context as? ComponentActivity)?.let { activity ->
                val insetsController = WindowInsetsControllerCompat(activity.window, activity.window.decorView)
                insetsController.hide(WindowInsetsCompat.Type.navigationBars())
                insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
        if (!uiState.hasImage) {
            // Avoid immediately navigating back while the image is still being loaded.
            // This prevents the "pick twice" issue where the editor closes before load finishes.
            LaunchedEffect(uiState.hasImage, uiState.isLoading) {
                if (!uiState.hasImage && !uiState.isLoading) {
                    delay(250)
                    val latest = viewModel.uiState.value
                    if (!latest.hasImage && !latest.isLoading) {
                        Log.d("EditorNav", "No image after delay; navigating back (hasImage=${latest.hasImage}, isLoading=${latest.isLoading})")
                        onBackClick?.invoke()
                    } else {
                        Log.d("EditorNav", "Stayed on editor after delay (hasImage=${latest.hasImage}, isLoading=${latest.isLoading})")
                    }
                }
            }

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val config = LocalConfiguration.current
            val isPortrait = config.orientation == Configuration.ORIENTATION_PORTRAIT

            if (isPortrait) {
                // Portrait layout: Top bar → Photo → Categories (horizontal scroll) → Bottom nav (horizontal scroll)
                val context = LocalContext.current
                val appLanguage = com.example.photoeditor.utils.LocaleHelper.getAppLanguage(context)
                val panelLayoutDirection = if (appLanguage == "he") LayoutDirection.Rtl else LayoutDirection.Ltr

                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Column(modifier = Modifier.weight(1f)) {
                        // Top action bar - full width
                        CompositionLocalProvider(LocalLayoutDirection provides panelLayoutDirection) {
                            TopActionBarHorizontal(
                                canUndo = uiState.canUndo,
                                canRedo = uiState.canRedo,
                                showOriginal = uiState.showOriginal,
                                isSaving = uiState.isSaving,
                                onUndo = { viewModel.undo() },
                                onRedo = { viewModel.redo() },
                                onToggle = { viewModel.toggleShowOriginal() },
                                onSave = { viewModel.saveImage() },
                                onNew = { showCancelDialog = true },
                                flipUndoRedoForRtl = appLanguage == "he",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Box(modifier = Modifier.height(1.dp).fillMaxWidth().background(MaterialTheme.colorScheme.outline))

                        // Photo - takes most space
                        val displayedBitmap = if (uiState.showOriginal) uiState.originalBitmap else uiState.currentBitmap
                        Box(modifier = Modifier.weight(1f)) {
                            ImageDisplay(
                                bitmap = displayedBitmap,
                                isLoading = uiState.isLoading || uiState.isProcessing,
                                onLongPress = { },
                                enableZoom = !isCropping,
                                allowSingleFingerPan = selectedCategory != MenuCategory.STICKERS && selectedCategory != MenuCategory.TEXT && uiState.selectedTextId == null,
                                resetTransform = isCropping,
                                roundedCornerRadiusPercent = if (!uiState.showOriginal && uiState.frameConfig.type == com.example.photoeditor.model.FrameType.STROKE && uiState.frameConfig.cornerRadiusPercent > 0f) {
                                    uiState.frameConfig.cornerRadiusPercent
                                } else 0f,
                                overlayContent = { imageRect, scale ->
                                    if (!uiState.showOriginal && uiState.frameConfig.type != com.example.photoeditor.model.FrameType.NONE) {
                                        FramePreviewOverlay(imageRect = imageRect, config = uiState.frameConfig, modifier = Modifier.fillMaxSize())
                                    }
                                    if (!uiState.showOriginal) {
                                        if (uiState.drawPaths.isNotEmpty()) {
                                            DrawingPathsCanvas(drawPaths = uiState.drawPaths, imageRect = imageRect)
                                        }
                                        StickerOverlaysOnImage(
                                            overlays = uiState.stickerOverlays, selectedId = uiState.selectedStickerId,
                                            imageRect = imageRect, scale = scale, isEditing = selectedCategory == MenuCategory.STICKERS,
                                            onSelect = { id -> viewModel.selectStickerOverlay(id) },
                                            onMoveById = { id, dx, dy -> viewModel.moveStickerById(id, dx, dy) },
                                            onRotateToId = { id, deg -> viewModel.setStickerRotationDegreesById(id, deg) },
                                            onScaleAndPositionToId = { id, sizeNorm, xNorm, yNorm -> viewModel.setStickerSizeAndPositionById(id, sizeNorm, xNorm, yNorm) }
                                        )
                                        TextOverlaysOnImage(
                                            overlays = uiState.textOverlays, selectedId = uiState.selectedTextId,
                                            imageRect = imageRect, imageBitmapWidth = displayedBitmap?.width ?: 0, imageBitmapHeight = displayedBitmap?.height ?: 0,
                                            scale = scale, isEditing = selectedCategory == MenuCategory.TEXT || uiState.selectedTextId != null,
                                            onSelect = { id -> viewModel.selectTextOverlay(id) },
                                            onMoveById = { id, dxNorm, dyNorm -> viewModel.moveTextById(id, dxNorm, dyNorm) },
                                            onRotateToId = { id, deg -> viewModel.setTextRotationDegreesById(id, deg) },
                                            onScaleAndPositionToId = { id, sizeNorm, xNorm, yNorm -> viewModel.setTextSizeAndPositionById(id, sizeNorm, xNorm, yNorm) },
                                        )
                                        if (selectedCategory == MenuCategory.DRAW) {
                                            DrawingOverlay(
                                                drawPaths = uiState.drawPaths, imageRect = imageRect,
                                                strokeWidthNorm = uiState.drawStrokeWidthNorm, drawColorArgb = uiState.drawColor, enabled = true,
                                                onStartPath = { nx, ny -> viewModel.startNewDrawPath(nx, ny) },
                                                onAddPoint = { nx, ny -> viewModel.addPointToCurrentDrawPath(nx, ny) }
                                            )
                                        }
                                    }
                                }
                            )
                            ImageIndicator(showOriginal = uiState.showOriginal, modifier = Modifier.align(Alignment.TopEnd))
                            if (isCropping) {
                                CropOverlay(
                                    bitmap = uiState.currentBitmap,
                                    onCancel = { isCropping = false },
                                    onConfirm = { rect -> viewModel.cropToRect(rect); isCropping = false },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        // Fixed category bar - above bottom nav, content changes by category
                        // Slightly more than one row height, horizontal scroll for Adjustments and Filters
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .background(MaterialTheme.colorScheme.surface)
                        ) {
                            Box(modifier = Modifier.height(1.dp).fillMaxWidth().background(MaterialTheme.colorScheme.outline))
                            CompositionLocalProvider(LocalLayoutDirection provides panelLayoutDirection) {
                                when (selectedCategory) {
                                    MenuCategory.ADJUSTMENTS -> AdjustmentsHorizontalBar(
                                        selectedAdjustment = selectedAdjustment,
                                        onAdjustmentClick = { type ->
                                            val current = selectedAdjustment
                                            if (current == type) {
                                                originalAdjustmentValues[type]?.let { original -> viewModel.setAdjustment(type, original) }
                                                selectedAdjustment = null
                                            } else {
                                                current?.let { prev -> originalAdjustmentValues[prev] = viewModel.getAdjustmentValue(prev) }
                                                if (!originalAdjustmentValues.containsKey(type)) {
                                                    originalAdjustmentValues[type] = viewModel.getAdjustmentValue(type)
                                                }
                                                selectedAdjustment = type
                                            }
                                        }
                                    )
                                    MenuCategory.FILTERS -> FiltersHorizontalBar(
                                        activeFilterType = uiState.activeFilterType,
                                        originalBitmap = uiState.originalBitmap,
                                        onFilterSelected = { type ->
                                            if (type == null) {
                                                viewModel.removeFilter()
                                                return@FiltersHorizontalBar
                                            }
                                            if (uiState.activeFilterType == type) {
                                                viewModel.removeFilter()
                                                return@FiltersHorizontalBar
                                            }
                                            val filter = when (type) {
                                                com.example.photoeditor.viewmodel.FilterType.GRAYSCALE -> GrayscaleFilter()
                                                com.example.photoeditor.viewmodel.FilterType.SEPIA -> SepiaFilter()
                                                com.example.photoeditor.viewmodel.FilterType.BLUR -> BlurFilter(radius = 5)
                                                com.example.photoeditor.viewmodel.FilterType.INVERT -> InvertFilter()
                                                com.example.photoeditor.viewmodel.FilterType.NOIR -> NoirFilter()
                                                com.example.photoeditor.viewmodel.FilterType.VIVID -> VividFilter()
                                                com.example.photoeditor.viewmodel.FilterType.WARM -> WarmFilter()
                                                com.example.photoeditor.viewmodel.FilterType.COOL -> CoolFilter()
                                                com.example.photoeditor.viewmodel.FilterType.VINTAGE -> VintageFilter()
                                                com.example.photoeditor.viewmodel.FilterType.FADE -> FadeFilter()
                                                com.example.photoeditor.viewmodel.FilterType.DRAMATIC -> DramaticFilter()
                                                com.example.photoeditor.viewmodel.FilterType.PASTEL -> PastelFilter()
                                                com.example.photoeditor.viewmodel.FilterType.NEON -> NeonFilter()
                                                com.example.photoeditor.viewmodel.FilterType.RETRO -> RetroFilter()
                                                com.example.photoeditor.viewmodel.FilterType.DREAMY -> DreamyFilter()
                                                com.example.photoeditor.viewmodel.FilterType.SUNSET -> SunsetFilter()
                                                com.example.photoeditor.viewmodel.FilterType.CLEAN_SKIN -> CleanSkinFilter()
                                                com.example.photoeditor.viewmodel.FilterType.EMBOSS -> EmbossFilter()
                                                else -> null
                                            }
                                            filter?.let { viewModel.applyFilter(it, type) }
                                        }
                                    )
                                    MenuCategory.TEXT -> com.example.photoeditor.ui.components.text.TextHorizontalBar(
                                        textOverlays = uiState.textOverlays,
                                        selectedTextId = uiState.selectedTextId,
                                        onAddNew = { viewModel.selectTextOverlay(null); showTextSheet = true },
                                        onSelectText = { id -> viewModel.selectTextOverlay(id); showTextSheet = true }
                                    )
                                    MenuCategory.TRANSFORM -> TransformHorizontalBar(
                                        selectedTransformTool = selectedTransformTool,
                                        onToolSelected = { selectedTransformTool = it },
                                        onRotate = { viewModel.rotate90Clockwise() },
                                        onMirror = { viewModel.flipHorizontal() },
                                        onStartCrop = { isCropping = true },
                                        onCropToAspectRatio = { viewModel.cropToAspectRatioFromInitial(it, 1f) },
                                        onResetToOriginal = { viewModel.resetToInitialImage() }
                                    )
                                    MenuCategory.STICKERS -> StickersHorizontalBar(
                                        selectedId = uiState.selectedStickerId,
                                        onAddSticker = { emoji -> viewModel.addStickerOverlay(emoji) },
                                        onDeleteSelected = { viewModel.deleteSelectedSticker() }
                                    )
                                    MenuCategory.DRAW -> DrawHorizontalBar(
                                        strokeWidth = uiState.drawStrokeWidthNorm,
                                        drawColorArgb = uiState.drawColor,
                                        onStrokeWidthChange = { viewModel.setDrawStrokeWidth(it) },
                                        onColorChange = { viewModel.setDrawColor(it) },
                                        onClear = { viewModel.clearDrawings() }
                                    )
                                    MenuCategory.FRAMES -> FramesHorizontalBar(
                                        currentConfig = uiState.frameConfig,
                                        onConfigChange = { config -> viewModel.setFrameConfig(config) },
                                        onCustomClick = { showCustomFrameSheet = true }
                                    )
                                    else -> {
                                        if (selectedCategory != MenuCategory.NONE) {
                                            Box(Modifier.fillMaxSize())
                                        }
                                    }
                                }
                            }
                        }
                        Box(modifier = Modifier.height(1.dp).fillMaxWidth().background(MaterialTheme.colorScheme.outline))
                        CompositionLocalProvider(LocalLayoutDirection provides panelLayoutDirection) {
                            BottomNavigationBar(
                                selectedCategory = selectedCategory,
                                onCategorySelected = { category ->
                                    if (selectedCategory == category && (selectedAdjustment != null || selectedTransformTool != null)) {
                                        selectedAdjustment?.let { adj ->
                                            originalAdjustmentValues[adj]?.let { original -> viewModel.setAdjustment(adj, original) }
                                        }
                                        selectedAdjustment = null
                                        selectedTransformTool = null
                                    } else {
                                        selectedCategory = if (selectedCategory == category) MenuCategory.NONE else category
                                        selectedAdjustment?.let { adj ->
                                            originalAdjustmentValues[adj]?.let { original -> viewModel.setAdjustment(adj, original) }
                                        }
                                        selectedAdjustment = null
                                        selectedTransformTool = null
                                    }
                                }
                            )
                        }
                    }
                }

                // Bottom sheet for other categories (Stickers, Draw, Frames) - Text and Transform show in bar
                val otherCategories = emptyList<MenuCategory>()
                val showOtherCategorySheet = selectedCategory in otherCategories && !dismissOtherCategorySheet
                LaunchedEffect(selectedCategory) { dismissOtherCategorySheet = false }
                // Text sheet - only when "New" clicked from TextHorizontalBar
                if (showTextSheet && selectedCategory == MenuCategory.TEXT) {
                    val textSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                    ModalBottomSheet(
                        onDismissRequest = { showTextSheet = false },
                        sheetState = textSheetState,
                        containerColor = MaterialTheme.colorScheme.surface,
                        scrimColor = androidx.compose.ui.graphics.Color.Transparent
                    ) {
                        CompositionLocalProvider(LocalLayoutDirection provides panelLayoutDirection) {
                            EditorCategoryContent(
                                selectedCategory = MenuCategory.TEXT,
                                selectedAdjustment = selectedAdjustment,
                                selectedTransformTool = selectedTransformTool,
                                uiState = uiState,
                                viewModel = viewModel,
                                originalAdjustmentValues = originalAdjustmentValues,
                                onAdjustmentChange = { selectedAdjustment = it },
                                onTransformToolChange = { selectedTransformTool = it },
                                onStartCrop = { },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 260.dp)
                                    .imePadding()
                            )
                        }
                    }
                }

                if (showCustomFrameSheet && selectedCategory == MenuCategory.FRAMES) {
                    val customSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                    ModalBottomSheet(
                        onDismissRequest = { showCustomFrameSheet = false },
                        sheetState = customSheetState,
                        containerColor = MaterialTheme.colorScheme.surface,
                        scrimColor = androidx.compose.ui.graphics.Color.Transparent,
                        shape = androidx.compose.ui.graphics.RectangleShape,
                        dragHandle = null,
                        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
                    ) {
                        CompositionLocalProvider(LocalLayoutDirection provides panelLayoutDirection) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(androidx.compose.ui.graphics.Color.Black)
                                    .navigationBarsPadding()
                            ) {
                                FrameCustomSheetContent(
                                    currentConfig = uiState.frameConfig,
                                    onConfigChange = { config -> viewModel.setFrameConfig(config) },
                                    onDismiss = { showCustomFrameSheet = false },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
                if (showOtherCategorySheet) {
                    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                    ModalBottomSheet(
                        onDismissRequest = { selectedCategory = MenuCategory.NONE },
                        sheetState = sheetState,
                        containerColor = MaterialTheme.colorScheme.surface,
                        scrimColor = androidx.compose.ui.graphics.Color.Transparent
                    ) {
                        CompositionLocalProvider(LocalLayoutDirection provides panelLayoutDirection) {
                            val textMaxHeight = if (selectedCategory == MenuCategory.TEXT) 260.dp else 400.dp
                            EditorCategoryContent(
                                selectedCategory = selectedCategory,
                                selectedAdjustment = selectedAdjustment,
                                selectedTransformTool = selectedTransformTool,
                                uiState = uiState,
                                viewModel = viewModel,
                                originalAdjustmentValues = originalAdjustmentValues,
                                onAdjustmentChange = { selectedAdjustment = it },
                                onTransformToolChange = { selectedTransformTool = it },
                                onStartCrop = { dismissOtherCategorySheet = true; isCropping = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = textMaxHeight)
                                    .imePadding()
                            )
                        }
                    }
                }

                // Slider bottom sheet when adjustment selected in portrait
                selectedAdjustment?.let { adj ->
                    val values = mapOf(
                        AdjustmentType.BRIGHTNESS to uiState.brightness,
                        AdjustmentType.CONTRAST to uiState.contrast,
                        AdjustmentType.SATURATION to uiState.saturation,
                        AdjustmentType.EXPOSURE to uiState.exposure,
                        AdjustmentType.HIGHLIGHTS to uiState.highlights,
                        AdjustmentType.SHADOWS to uiState.shadows,
                        AdjustmentType.VIBRANCE to uiState.vibrance,
                        AdjustmentType.WARMTH to uiState.warmth,
                        AdjustmentType.TINT to uiState.tint,
                        AdjustmentType.HUE to uiState.hue,
                        AdjustmentType.SHARPNESS to uiState.sharpness,
                        AdjustmentType.DEFINITION to uiState.definition,
                        AdjustmentType.VIGNETTE to uiState.vignette,
                        AdjustmentType.GLOW to uiState.glow
                    )
                    ModalBottomSheet(
                        onDismissRequest = {
                            originalAdjustmentValues[adj]?.let { orig -> viewModel.setAdjustment(adj, orig) }
                            selectedAdjustment = null
                        },
                        containerColor = MaterialTheme.colorScheme.surface,
                        scrimColor = androidx.compose.ui.graphics.Color.Transparent,
                        contentWindowInsets = { WindowInsets(0, 0, 0, 0) }
                    ) {
                        CompositionLocalProvider(LocalLayoutDirection provides panelLayoutDirection) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface)
                                    .navigationBarsPadding()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    AdjustmentSliderPanel(
                                        adjustmentType = adj,
                                        initialValue = values[adj] ?: 0f,
                                        onValueChange = { viewModel.setAdjustment(adj, it) },
                                        onConfirm = { value ->
                                            originalAdjustmentValues[adj] = value
                                            selectedAdjustment = null
                                        },
                                        onCancel = { original ->
                                            viewModel.setAdjustment(adj, original)
                                            selectedAdjustment = null
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Landscape layout - unchanged: Row with image | side panel
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(modifier = Modifier.weight(1f)) {
                // Image display - takes remaining space (always on left)
                val displayedBitmap = if (uiState.showOriginal) uiState.originalBitmap else uiState.currentBitmap
                Box(modifier = Modifier.weight(1f)) {
                    ImageDisplay(
                        bitmap = displayedBitmap,
                        isLoading = uiState.isLoading || uiState.isProcessing,
                        onLongPress = { },
                        enableZoom = !isCropping,
                        allowSingleFingerPan = selectedCategory != MenuCategory.STICKERS && selectedCategory != MenuCategory.TEXT && uiState.selectedTextId == null,
                        resetTransform = isCropping,
                        roundedCornerRadiusPercent = if (!uiState.showOriginal && uiState.frameConfig.type == com.example.photoeditor.model.FrameType.STROKE && uiState.frameConfig.cornerRadiusPercent > 0f) {
                            uiState.frameConfig.cornerRadiusPercent
                        } else 0f,
                        overlayContent = { imageRect, scale ->
                            // Frame preview (non-destructive overlay)
                            if (!uiState.showOriginal && uiState.frameConfig.type != com.example.photoeditor.model.FrameType.NONE) {
                                FramePreviewOverlay(
                                    imageRect = imageRect,
                                    config = uiState.frameConfig,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            // Only show stickers, text, and drawing when viewing edited (not original)
                            if (!uiState.showOriginal) {
                                // Draw paths canvas - rendered UNDER stickers so it doesn't block touches
                                if (uiState.drawPaths.isNotEmpty()) {
                                    DrawingPathsCanvas(
                                        drawPaths = uiState.drawPaths,
                                        imageRect = imageRect
                                )
                            }
                            StickerOverlaysOnImage(
                                overlays = uiState.stickerOverlays,
                                selectedId = uiState.selectedStickerId,
                                imageRect = imageRect,
                                scale = scale,
                                    isEditing = selectedCategory == MenuCategory.STICKERS,
                                onSelect = { id -> viewModel.selectStickerOverlay(id) },
                                onMoveById = { id, dx, dy -> viewModel.moveStickerById(id, dx, dy) },
                                onRotateToId = { id, deg -> viewModel.setStickerRotationDegreesById(id, deg) },
                                onScaleAndPositionToId = { id, sizeNorm, xNorm, yNorm ->
                                    viewModel.setStickerSizeAndPositionById(id, sizeNorm, xNorm, yNorm)
                                }
                            )
                            TextOverlaysOnImage(
                                overlays = uiState.textOverlays,
                                selectedId = uiState.selectedTextId,
                                imageRect = imageRect,
                                imageBitmapWidth = displayedBitmap?.width ?: 0,
                                imageBitmapHeight = displayedBitmap?.height ?: 0,
                                scale = scale,
                                    isEditing = selectedCategory == MenuCategory.TEXT || uiState.selectedTextId != null,
                                onSelect = { id -> viewModel.selectTextOverlay(id) },
                                onMoveById = { id, dxNorm, dyNorm -> viewModel.moveTextById(id, dxNorm, dyNorm) },
                                onRotateToId = { id, deg -> viewModel.setTextRotationDegreesById(id, deg) },
                                onScaleAndPositionToId = { id, sizeNorm, xNorm, yNorm ->
                                    viewModel.setTextSizeAndPositionById(id, sizeNorm, xNorm, yNorm)
                                },
                            )
                                // Drawing touch overlay - only when in DRAW mode, on top for capturing touches
                                if (selectedCategory == MenuCategory.DRAW) {
                                    DrawingOverlay(
                                        drawPaths = uiState.drawPaths,
                                        imageRect = imageRect,
                                        strokeWidthNorm = uiState.drawStrokeWidthNorm,
                                        drawColorArgb = uiState.drawColor,
                                        enabled = true,
                                        onStartPath = { nx, ny -> viewModel.startNewDrawPath(nx, ny) },
                                        onAddPoint = { nx, ny -> viewModel.addPointToCurrentDrawPath(nx, ny) }
                                    )
                                }
                            }
                        }
                    )

                    // Show indicator for before/after
                    ImageIndicator(
                        showOriginal = uiState.showOriginal,
                        modifier = Modifier.align(Alignment.TopEnd)
                    )

                    if (isCropping) {
                        CropOverlay(
                            bitmap = uiState.currentBitmap,
                            onCancel = { isCropping = false },
                            onConfirm = { rect ->
                                viewModel.cropToRect(rect)
                                isCropping = false
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Divider between image area and the right panel
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.outline)
                )
                
                // Side panel - always visible with navigation bar
                // Restore RTL for text content inside the panel based on app language
                val context = LocalContext.current
                val appLanguage = com.example.photoeditor.utils.LocaleHelper.getAppLanguage(context)
                val panelLayoutDirection = if (appLanguage == "he") LayoutDirection.Rtl else LayoutDirection.Ltr
                CompositionLocalProvider(LocalLayoutDirection provides panelLayoutDirection) {
                    Column(
                        modifier = Modifier
                            .width(300.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                    // Actions live in the right panel so the image area stays maximal and never gets covered.
                    TopActionBarHorizontal(
                        canUndo = uiState.canUndo,
                        canRedo = uiState.canRedo,
                        showOriginal = uiState.showOriginal,
                        isSaving = uiState.isSaving,
                        onUndo = { viewModel.undo() },
                        onRedo = { viewModel.redo() },
                        onToggle = { viewModel.toggleShowOriginal() },
                        onSave = { viewModel.saveImage() },
                        onNew = { showCancelDialog = true },
                        flipUndoRedoForRtl = appLanguage == "he",
                        modifier = Modifier.padding(top = 6.dp)
                    )

                    // Divider under the top actions
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outline)
                    )

                    // Category options panel - takes available space
                    Box(modifier = Modifier.weight(1f)) {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = selectedCategory != MenuCategory.NONE || selectedAdjustment != null,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            if (selectedCategory != MenuCategory.NONE) {
                                when (selectedCategory) {
                                        MenuCategory.ADJUSTMENTS -> {
                                            val values = mapOf(
                                                AdjustmentType.BRIGHTNESS to uiState.brightness,
                                                AdjustmentType.CONTRAST to uiState.contrast,
                                                AdjustmentType.SATURATION to uiState.saturation,
                                                AdjustmentType.EXPOSURE to uiState.exposure,
                                                AdjustmentType.HIGHLIGHTS to uiState.highlights,
                                                AdjustmentType.SHADOWS to uiState.shadows,
                                                AdjustmentType.VIBRANCE to uiState.vibrance,
                                                AdjustmentType.WARMTH to uiState.warmth,
                                                AdjustmentType.TINT to uiState.tint,
                                                AdjustmentType.HUE to uiState.hue,
                                                AdjustmentType.SHARPNESS to uiState.sharpness,
                                                AdjustmentType.DEFINITION to uiState.definition,
                                                AdjustmentType.VIGNETTE to uiState.vignette,
                                                AdjustmentType.GLOW to uiState.glow
                                            )

                                            fun openOrClose(type: AdjustmentType) {
                                                val current = selectedAdjustment
                                                if (current == type) {
                                                    // Close: restore original if present
                                                    val original = originalAdjustmentValues[type]
                                                    if (original != null) {
                                                        viewModel.setAdjustment(type, original)
                                                    }
                                                    selectedAdjustment = null
                                                } else {
                                                    // Switching from another adjustment: consider it confirmed (keep current)
                                                    current?.let { prev ->
                                                        originalAdjustmentValues[prev] = viewModel.getAdjustmentValue(prev)
                                                    }
                                                    // Opening: capture original value once
                                                    if (!originalAdjustmentValues.containsKey(type)) {
                                                        originalAdjustmentValues[type] = viewModel.getAdjustmentValue(type)
                                                    }
                                                    selectedAdjustment = type
                                                }
                                            }

                                            AdjustmentPanel(
                                                values = values,
                                                selectedAdjustment = selectedAdjustment,
                                                onAdjustmentClick = { type -> openOrClose(type) },
                                                onValueChange = { type, value -> viewModel.setAdjustment(type, value) },
                                                onReset = {
                                                    originalAdjustmentValues.clear()
                                                    selectedAdjustment = null
                                                    viewModel.resetAdjustments()
                                                },
                                                onConfirmAdjustment = { type, value ->
                                                    originalAdjustmentValues[type] = value
                                                }
                                            )
                                        }
                                        MenuCategory.FILTERS -> {
                                            fun selectFilter(type: com.example.photoeditor.viewmodel.FilterType?) {
                                                if (type == null) {
                                                    viewModel.removeFilter()
                                                    return
                                                }

                                                if (uiState.activeFilterType == type) {
                                                    viewModel.removeFilter()
                                                    return
                                                }

                                                val filter = when (type) {
                                                    com.example.photoeditor.viewmodel.FilterType.GRAYSCALE -> GrayscaleFilter()
                                                    com.example.photoeditor.viewmodel.FilterType.SEPIA -> SepiaFilter()
                                                    com.example.photoeditor.viewmodel.FilterType.BLUR -> BlurFilter(radius = 5)
                                                    com.example.photoeditor.viewmodel.FilterType.INVERT -> InvertFilter()
                                                    com.example.photoeditor.viewmodel.FilterType.NOIR -> NoirFilter()
                                                    com.example.photoeditor.viewmodel.FilterType.VIVID -> VividFilter()
                                                    com.example.photoeditor.viewmodel.FilterType.WARM -> WarmFilter()
                                                    com.example.photoeditor.viewmodel.FilterType.COOL -> CoolFilter()
                                                    com.example.photoeditor.viewmodel.FilterType.VINTAGE -> VintageFilter()
                                                    com.example.photoeditor.viewmodel.FilterType.FADE -> FadeFilter()
                                                    com.example.photoeditor.viewmodel.FilterType.DRAMATIC -> DramaticFilter()
                                                    com.example.photoeditor.viewmodel.FilterType.PASTEL -> PastelFilter()
                                                    com.example.photoeditor.viewmodel.FilterType.NEON -> NeonFilter()
                                                    com.example.photoeditor.viewmodel.FilterType.RETRO -> RetroFilter()
                                                    com.example.photoeditor.viewmodel.FilterType.DREAMY -> DreamyFilter()
                                                    com.example.photoeditor.viewmodel.FilterType.SUNSET -> SunsetFilter()
                                                    com.example.photoeditor.viewmodel.FilterType.CLEAN_SKIN -> CleanSkinFilter()
                                                    com.example.photoeditor.viewmodel.FilterType.EMBOSS -> EmbossFilter()
                                                    else -> null
                                                }

                                                filter?.let { viewModel.applyFilter(it, type) }
                                            }

                                            FilterOptionsPanel(
                                                activeFilterType = uiState.activeFilterType,
                                                originalBitmap = uiState.originalBitmap,
                                                onFilterSelected = { type -> selectFilter(type) }
                                            )
                                        }
                                        MenuCategory.TRANSFORM -> {
                                            TransformPanel(
                                                selectedTool = selectedTransformTool,
                                                onToolSelected = { selectedTransformTool = it },
                                                onRotate = { viewModel.rotate90Clockwise() },
                                                onMirror = { viewModel.flipHorizontal() },
                                                onStartCrop = { isCropping = true },
                                                onResetToOriginal = { viewModel.resetToInitialImage() },
                                                onCropToAspectRatio = { ratio ->
                                                    viewModel.cropToAspectRatioFromInitial(ratio, 1f)
                                                },
                                            )
                                        }
                                        MenuCategory.TEXT -> {
                                TextToolPanel(
                                                overlays = uiState.textOverlays,
                                                selectedId = uiState.selectedTextId,
                                                onAdd = { viewModel.addTextOverlay() },
                                                onSelect = { id -> viewModel.selectTextOverlay(id) },
                                                onDeselect = { viewModel.selectTextOverlay(null) },
                                                onTextChange = { text -> viewModel.updateSelectedText(text) },
                                                onToggleBold = { viewModel.toggleSelectedTextBold() },
                                                onToggleItalic = { viewModel.toggleSelectedTextItalic() },
                                                onToggleUnderline = { viewModel.toggleSelectedTextUnderline() },
                                                onColorChange = { argb -> viewModel.setSelectedTextColor(argb) },
                                                onDelete = { viewModel.deleteSelectedText() },
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                        MenuCategory.STICKERS -> {
                                            Box(modifier = Modifier.heightIn(max = 360.dp).fillMaxWidth()) {
                                            StickerToolPanel(
                                                stickers = uiState.stickerOverlays,
                                                selectedId = uiState.selectedStickerId,
                                                onAddSticker = { emoji -> viewModel.addStickerOverlay(emoji) },
                                                onDeleteSelected = { viewModel.deleteSelectedSticker() },
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                        }
                                        MenuCategory.DRAW -> {
                                            DrawToolPanel(
                                                strokeWidth = uiState.drawStrokeWidthNorm,
                                                drawColorArgb = uiState.drawColor,
                                                onStrokeWidthChange = { viewModel.setDrawStrokeWidth(it) },
                                                onColorChange = { viewModel.setDrawColor(it) },
                                                onClear = { viewModel.clearDrawings() },
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                        MenuCategory.FRAMES -> {
                                            FrameToolPanel(
                                                currentConfig = uiState.frameConfig,
                                                onConfigChange = { config -> viewModel.setFrameConfig(config) },
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                        MenuCategory.NONE -> {}
                                    }
                                }
                        }
                    }
                    
                    // Bottom navigation bar with categories - always visible at bottom of right panel
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outline)
                    )
                    BottomNavigationBar(
                        selectedCategory = selectedCategory,
                        onCategorySelected = { category ->
                            if (selectedCategory == category && (selectedAdjustment != null || selectedTransformTool != null)) {
                                // If clicking on already selected category with an adjustment open,
                                // cancel the adjustment (restore original values) and return to main category screen
                                selectedAdjustment?.let { adj ->
                                    originalAdjustmentValues[adj]?.let { original ->
                                        viewModel.setAdjustment(adj, original)
                                    }
                                }
                                selectedAdjustment = null
                                selectedTransformTool = null
                            } else {
                                // Otherwise, toggle category selection
                                selectedCategory = if (selectedCategory == category) MenuCategory.NONE else category
                                // Cancel any open adjustment when switching categories
                                selectedAdjustment?.let { adj ->
                                    originalAdjustmentValues[adj]?.let { original ->
                                        viewModel.setAdjustment(adj, original)
                                    }
                                }
                                selectedAdjustment = null
                                selectedTransformTool = null
                            }
                        }
                    )
                    }
                    }
                }
                }
            }
        }
        
        // Cancel confirmation dialog
        if (showCancelDialog) {
            AlertDialog(
                onDismissRequest = { showCancelDialog = false },
                title = {
                    Text(stringResource(R.string.cancel_editing))
                },
                text = {
                    Text(stringResource(R.string.cancel_editing_message))
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showCancelDialog = false
                            selectedCategory = MenuCategory.NONE
                            selectedAdjustment = null
                            viewModel.clearImage()
                        }
                    ) {
                        Text(stringResource(R.string.yes_cancel))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showCancelDialog = false }
                    ) {
                        Text(stringResource(R.string.no_continue))
                    }
                }
            )
        }

        }
    }
}

@Composable
private fun EditorCategoryContent(
    selectedCategory: MenuCategory,
    selectedAdjustment: AdjustmentType?,
    selectedTransformTool: TransformTool?,
    uiState: com.example.photoeditor.viewmodel.EditorUiState,
    viewModel: EditorViewModel,
    originalAdjustmentValues: MutableMap<AdjustmentType, Float>,
    onAdjustmentChange: (AdjustmentType?) -> Unit,
    onTransformToolChange: (TransformTool?) -> Unit,
    onStartCrop: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        when (selectedCategory) {
            MenuCategory.ADJUSTMENTS -> {
                val values = mapOf(
                    AdjustmentType.BRIGHTNESS to uiState.brightness,
                    AdjustmentType.CONTRAST to uiState.contrast,
                    AdjustmentType.SATURATION to uiState.saturation,
                    AdjustmentType.EXPOSURE to uiState.exposure,
                    AdjustmentType.HIGHLIGHTS to uiState.highlights,
                    AdjustmentType.SHADOWS to uiState.shadows,
                    AdjustmentType.VIBRANCE to uiState.vibrance,
                    AdjustmentType.WARMTH to uiState.warmth,
                    AdjustmentType.TINT to uiState.tint,
                    AdjustmentType.HUE to uiState.hue,
                    AdjustmentType.SHARPNESS to uiState.sharpness,
                    AdjustmentType.DEFINITION to uiState.definition,
                    AdjustmentType.VIGNETTE to uiState.vignette,
                    AdjustmentType.GLOW to uiState.glow
                )
                fun openOrClose(type: AdjustmentType) {
                    val current = selectedAdjustment
                    if (current == type) {
                        originalAdjustmentValues[type]?.let { original -> viewModel.setAdjustment(type, original) }
                        onAdjustmentChange(null)
                    } else {
                        current?.let { prev -> originalAdjustmentValues[prev] = viewModel.getAdjustmentValue(prev) }
                        if (!originalAdjustmentValues.containsKey(type)) {
                            originalAdjustmentValues[type] = viewModel.getAdjustmentValue(type)
                        }
                        onAdjustmentChange(type)
                    }
                }
                AdjustmentPanel(
                    values = values,
                    selectedAdjustment = selectedAdjustment,
                    onAdjustmentClick = { openOrClose(it) },
                    onValueChange = { type, value -> viewModel.setAdjustment(type, value) },
                    onReset = {
                        originalAdjustmentValues.clear()
                        onAdjustmentChange(null)
                        viewModel.resetAdjustments()
                    },
                    onConfirmAdjustment = { type, value -> originalAdjustmentValues[type] = value }
                )
            }
            MenuCategory.FILTERS -> {
                fun selectFilter(type: com.example.photoeditor.viewmodel.FilterType?) {
                    if (type == null) {
                        viewModel.removeFilter()
                        return
                    }
                    if (uiState.activeFilterType == type) {
                        viewModel.removeFilter()
                        return
                    }
                    val filter = when (type) {
                        com.example.photoeditor.viewmodel.FilterType.GRAYSCALE -> GrayscaleFilter()
                        com.example.photoeditor.viewmodel.FilterType.SEPIA -> SepiaFilter()
                        com.example.photoeditor.viewmodel.FilterType.BLUR -> BlurFilter(radius = 5)
                        com.example.photoeditor.viewmodel.FilterType.INVERT -> InvertFilter()
                        com.example.photoeditor.viewmodel.FilterType.NOIR -> NoirFilter()
                        com.example.photoeditor.viewmodel.FilterType.VIVID -> VividFilter()
                        com.example.photoeditor.viewmodel.FilterType.WARM -> WarmFilter()
                        com.example.photoeditor.viewmodel.FilterType.COOL -> CoolFilter()
                        com.example.photoeditor.viewmodel.FilterType.VINTAGE -> VintageFilter()
                        com.example.photoeditor.viewmodel.FilterType.FADE -> FadeFilter()
                        com.example.photoeditor.viewmodel.FilterType.DRAMATIC -> DramaticFilter()
                        com.example.photoeditor.viewmodel.FilterType.PASTEL -> PastelFilter()
                        com.example.photoeditor.viewmodel.FilterType.NEON -> NeonFilter()
                        com.example.photoeditor.viewmodel.FilterType.RETRO -> RetroFilter()
                        com.example.photoeditor.viewmodel.FilterType.DREAMY -> DreamyFilter()
                        com.example.photoeditor.viewmodel.FilterType.SUNSET -> SunsetFilter()
                        com.example.photoeditor.viewmodel.FilterType.CLEAN_SKIN -> CleanSkinFilter()
                        com.example.photoeditor.viewmodel.FilterType.EMBOSS -> EmbossFilter()
                        else -> null
                    }
                    filter?.let { viewModel.applyFilter(it, type) }
                }
                FilterOptionsPanel(
                    activeFilterType = uiState.activeFilterType,
                    originalBitmap = uiState.originalBitmap,
                    onFilterSelected = { selectFilter(it) }
                )
            }
            MenuCategory.TRANSFORM -> TransformPanel(
                selectedTool = selectedTransformTool,
                onToolSelected = onTransformToolChange,
                onRotate = { viewModel.rotate90Clockwise() },
                onMirror = { viewModel.flipHorizontal() },
                onStartCrop = onStartCrop,
                onResetToOriginal = { viewModel.resetToInitialImage() },
                onCropToAspectRatio = { viewModel.cropToAspectRatioFromInitial(it, 1f) }
            )
            MenuCategory.TEXT -> TextToolPanel(
                overlays = uiState.textOverlays,
                selectedId = uiState.selectedTextId,
                onAdd = { viewModel.addTextOverlay() },
                onSelect = { id -> viewModel.selectTextOverlay(id) },
                onDeselect = { viewModel.selectTextOverlay(null) },
                onTextChange = { text -> viewModel.updateSelectedText(text) },
                onToggleBold = { viewModel.toggleSelectedTextBold() },
                onToggleItalic = { viewModel.toggleSelectedTextItalic() },
                onToggleUnderline = { viewModel.toggleSelectedTextUnderline() },
                onColorChange = { argb -> viewModel.setSelectedTextColor(argb) },
                onDelete = { viewModel.deleteSelectedText() },
                modifier = Modifier.fillMaxWidth()
            )
            MenuCategory.STICKERS -> StickerToolPanel(
                stickers = uiState.stickerOverlays,
                selectedId = uiState.selectedStickerId,
                onAddSticker = { emoji -> viewModel.addStickerOverlay(emoji) },
                onDeleteSelected = { viewModel.deleteSelectedSticker() },
                modifier = Modifier.fillMaxWidth()
            )
            MenuCategory.DRAW -> DrawToolPanel(
                strokeWidth = uiState.drawStrokeWidthNorm,
                drawColorArgb = uiState.drawColor,
                onStrokeWidthChange = { viewModel.setDrawStrokeWidth(it) },
                onColorChange = { viewModel.setDrawColor(it) },
                onClear = { viewModel.clearDrawings() },
                modifier = Modifier.fillMaxWidth()
            )
            MenuCategory.FRAMES -> FrameToolPanel(
                currentConfig = uiState.frameConfig,
                onConfigChange = { config -> viewModel.setFrameConfig(config) },
                modifier = Modifier.fillMaxWidth()
            )
            MenuCategory.NONE -> {}
        }
    }
}

@Composable
private fun TextOverlaysOnImage(
    overlays: List<TextOverlay>,
    selectedId: String?,
    imageRect: Rect,
    imageBitmapWidth: Int,
    imageBitmapHeight: Int,
    scale: Float,
    isEditing: Boolean,
    onSelect: (String?) -> Unit,
    onMoveById: (id: String, dxNorm: Float, dyNorm: Float) -> Unit,
    onRotateToId: (id: String, degrees: Float) -> Unit,
    onScaleAndPositionToId: (id: String, sizeNorm: Float, xNorm: Float, yNorm: Float) -> Unit
) {
    if (overlays.isEmpty()) return
    if (imageBitmapWidth <= 0 || imageBitmapHeight <= 0) return
    if (imageRect.width <= 0f || imageRect.height <= 0f) return

    overlays.forEach { o ->
        EditableTextOverlayItem(
            overlay = o,
            isSelected = (o.id == selectedId),
            isEditing = isEditing,
            imageRect = imageRect,
            zoomScale = scale,
            onSelect = { id -> onSelect(id) },
            onMoveBy = { dx, dy -> onMoveById(o.id, dx, dy) },
            onRotateTo = { deg -> onRotateToId(o.id, deg) },
            onScaleAndPositionTo = { sizeNorm, xNorm, yNorm -> onScaleAndPositionToId(o.id, sizeNorm, xNorm, yNorm) }
        )
    }
}

@Composable
private fun StickerOverlaysOnImage(
    overlays: List<StickerOverlay>,
    selectedId: String?,
    imageRect: Rect,
    scale: Float,
    isEditing: Boolean,
    onSelect: (String?) -> Unit,
    onMoveById: (id: String, dxNorm: Float, dyNorm: Float) -> Unit,
    onRotateToId: (id: String, degrees: Float) -> Unit,
    onScaleAndPositionToId: (id: String, sizeNorm: Float, xNorm: Float, yNorm: Float) -> Unit
) {
    if (overlays.isEmpty()) return
    if (imageRect.width <= 0f || imageRect.height <= 0f) return

    overlays.forEach { o ->
        EditableStickerOverlayItem(
            overlay = o,
            isSelected = (o.id == selectedId),
            isEditing = isEditing,
            imageRect = imageRect,
            zoomScale = scale,
            onSelect = { id -> onSelect(id) },
            onMoveBy = { dx, dy -> onMoveById(o.id, dx, dy) },
            onRotateTo = { deg -> onRotateToId(o.id, deg) },
            onScaleAndPositionTo = { sizeNorm, xNorm, yNorm -> onScaleAndPositionToId(o.id, sizeNorm, xNorm, yNorm) }
        )
    }
}
