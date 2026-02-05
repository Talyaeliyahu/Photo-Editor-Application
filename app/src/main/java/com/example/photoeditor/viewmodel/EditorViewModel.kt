package com.example.photoeditor.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.photoeditor.R
import com.example.photoeditor.image.BitmapTransforms
import com.example.photoeditor.image.BitmapDrawRenderer
import com.example.photoeditor.image.BitmapStickerRenderer
import com.example.photoeditor.image.BitmapTextRenderer
import com.example.photoeditor.image.FrameRenderer
import com.example.photoeditor.image.ImageFilter
import com.example.photoeditor.image.ImageProcessor
import com.example.photoeditor.model.DrawPath
import com.example.photoeditor.model.StickerOverlay
import com.example.photoeditor.model.TextOverlay
import com.example.photoeditor.model.FrameConfig
import com.example.photoeditor.utils.ImageUtils
import com.example.photoeditor.utils.LocaleHelper
import com.example.photoeditor.ui.components.AdjustmentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.geometry.Offset

/**
 * Enum for filter types
 */
enum class FilterType {
    GRAYSCALE,
    SEPIA,
    BLUR,
    INVERT,
    NOIR,
    VIVID,
    WARM,
    COOL,
    VINTAGE,
    FADE,
    DRAMATIC,
    PASTEL,
    NEON,
    RETRO,
    DREAMY,
    SUNSET,
    CLEAN_SKIN
}

/**
 * ViewModel for the image editor screen.
 * Manages image state, undo/redo functionality, and image processing operations.
 */
class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private data class EditorSnapshot(
        val originalBitmap: Bitmap,
        val editedBitmap: Bitmap,
        val brightness: Float,
        val contrast: Float,
        val saturation: Float,
        val exposure: Float,
        val highlights: Float,
        val shadows: Float,
        val vibrance: Float,
        val warmth: Float,
        val tint: Float,
        val hue: Float,
        val sharpness: Float,
        val definition: Float,
        val vignette: Float,
        val glow: Float,
        val activeFilterType: FilterType?,
        val textOverlays: List<TextOverlay>,
        val selectedTextId: String?,
        val stickerOverlays: List<StickerOverlay>,
        val selectedStickerId: String?,
        val drawPaths: List<DrawPath>,
        val frameConfig: FrameConfig
    ) {
        fun recycle() {
            originalBitmap.recycle()
            editedBitmap.recycle()
        }
    }
    
    // Immutable first-loaded image (used for "always from original" operations like aspect-ratio cropping)
    private var initialBitmap: Bitmap? = null

    // Base/original bitmap for the current editing session (may change after crop/rotate/flip)
    private var originalBitmap: Bitmap? = null
    
    // Current edited bitmap
    private var editedBitmap: Bitmap? = null
    
    // Undo stack: stores previous states
    private val undoStack = ArrayDeque<EditorSnapshot>()
    
    // Redo stack: stores states that were undone
    private val redoStack = ArrayDeque<EditorSnapshot>()
    
    // UI State
    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()
    
    // Adjustment values
    private var brightness: Float = 0f
    private var contrast: Float = 0f
    private var saturation: Float = 0f
    private var exposure: Float = 0f
    private var highlights: Float = 0f
    private var shadows: Float = 0f
    private var vibrance: Float = 0f
    private var warmth: Float = 0f
    private var tint: Float = 0f
    private var hue: Float = 0f
    private var sharpness: Float = 0f
    private var definition: Float = 0f
    private var vignette: Float = 0f
    private var glow: Float = 0f
    
    // Active filter type (null = no filter)
    private var activeFilterType: FilterType? = null

    // Text overlays (non-destructive until save)
    private var textOverlays: List<TextOverlay> = emptyList()
    private var selectedTextId: String? = null

    // Sticker overlays (emoji stickers)
    private var stickerOverlays: List<StickerOverlay> = emptyList()
    private var selectedStickerId: String? = null
    
    // Frame configuration (applied on save, previewed in UI)
    private var currentFrameConfig: FrameConfig = FrameConfig.NONE

    // Draw overlays (freehand drawing)
    private var drawPaths: List<DrawPath> = emptyList()
    private var drawColor: Int = Color.BLACK
    private var drawStrokeWidthNorm: Float = 0.015f

    // Coalesce rapid slider updates to avoid slow/queued processing
    private var applyWorkerJob: Job? = null
    private var requestedApplyVersion: Int = 0
    private var appliedApplyVersion: Int = 0

    private fun createSnapshot(): EditorSnapshot? {
        val o = originalBitmap ?: return null
        val e = editedBitmap ?: return null

        val oCopy = o.copy(o.config ?: Bitmap.Config.ARGB_8888, true)
        val eCopy = e.copy(e.config ?: Bitmap.Config.ARGB_8888, true)

        return EditorSnapshot(
            originalBitmap = oCopy,
            editedBitmap = eCopy,
            brightness = brightness,
            contrast = contrast,
            saturation = saturation,
            exposure = exposure,
            highlights = highlights,
            shadows = shadows,
            vibrance = vibrance,
            warmth = warmth,
            tint = tint,
            hue = hue,
            sharpness = sharpness,
            definition = definition,
            vignette = vignette,
            glow = glow,
            activeFilterType = activeFilterType,
            textOverlays = textOverlays,
            selectedTextId = selectedTextId,
            stickerOverlays = stickerOverlays,
            selectedStickerId = selectedStickerId,
            drawPaths = drawPaths,
            frameConfig = currentFrameConfig
        )
    }

    private fun clearRedoStack() {
        redoStack.forEach { it.recycle() }
        redoStack.clear()
    }

    private fun clearUndoStack() {
        undoStack.forEach { it.recycle() }
        undoStack.clear()
    }

    private fun pushUndoSnapshot() {
        val snap = createSnapshot() ?: return
        if (undoStack.size >= 10) {
            undoStack.removeFirst().recycle()
        }
        undoStack.addLast(snap)
    }

    private fun pushRedoSnapshot() {
        val snap = createSnapshot() ?: return
        if (redoStack.size >= 10) {
            redoStack.removeFirst().recycle()
        }
        redoStack.addLast(snap)
    }

    private fun cancelApplyWorker() {
        applyWorkerJob?.cancel()
        applyWorkerJob = null
    }

    private fun recycleSessionBitmaps() {
        val init = initialBitmap
        val orig = originalBitmap
        val edited = editedBitmap

        edited?.recycle()

        // Avoid double-recycle if original == initial
        if (orig != null && orig !== init) {
            orig.recycle()
        }
        init?.recycle()

        initialBitmap = null
        originalBitmap = null
        editedBitmap = null
    }

    private fun buildFilter(type: FilterType?): ImageFilter? {
        return when (type) {
            FilterType.GRAYSCALE -> com.example.photoeditor.image.filters.GrayscaleFilter()
            FilterType.SEPIA -> com.example.photoeditor.image.filters.SepiaFilter()
            FilterType.BLUR -> com.example.photoeditor.image.filters.BlurFilter(radius = 5)
            FilterType.INVERT -> com.example.photoeditor.image.filters.InvertFilter()
            FilterType.NOIR -> com.example.photoeditor.image.filters.NoirFilter()
            FilterType.VIVID -> com.example.photoeditor.image.filters.VividFilter()
            FilterType.WARM -> com.example.photoeditor.image.filters.WarmFilter()
            FilterType.COOL -> com.example.photoeditor.image.filters.CoolFilter()
            FilterType.VINTAGE -> com.example.photoeditor.image.filters.VintageFilter()
            FilterType.FADE -> com.example.photoeditor.image.filters.FadeFilter()
            FilterType.DRAMATIC -> com.example.photoeditor.image.filters.DramaticFilter()
            FilterType.PASTEL -> com.example.photoeditor.image.filters.PastelFilter()
            FilterType.NEON -> com.example.photoeditor.image.filters.NeonFilter()
            FilterType.RETRO -> com.example.photoeditor.image.filters.RetroFilter()
            FilterType.DREAMY -> com.example.photoeditor.image.filters.DreamyFilter()
            FilterType.SUNSET -> com.example.photoeditor.image.filters.SunsetFilter()
            FilterType.CLEAN_SKIN -> com.example.photoeditor.image.filters.CleanSkinFilter()
            null -> null
        }
    }

    private suspend fun renderFromOriginal(source: Bitmap): Bitmap {
        val adjusted = withContext(Dispatchers.Default) {
            ImageProcessor.applyAdjustments(
                source = source,
                brightness = brightness,
                contrast = contrast,
                saturation = saturation,
                exposure = exposure,
                highlights = highlights,
                shadows = shadows,
                vibrance = vibrance,
                warmth = warmth,
                tint = tint,
                hue = hue,
                sharpness = sharpness,
                definition = definition,
                vignette = vignette,
                glow = glow
            )
        }

        val filter = buildFilter(activeFilterType)
        val finalResult = if (filter != null) {
            withContext(Dispatchers.Default) { filter.apply(adjusted) }
        } else {
            adjusted
        }

        if (finalResult != adjusted) {
            adjusted.recycle()
        }
        return finalResult
    }
    
    /**
     * Loads an image from URI and sets it as the original bitmap.
     */
    fun loadImage(uri: android.net.Uri) {
        Log.d("EditorViewModel", "=== LOAD IMAGE START ===")
        Log.d("EditorViewModel", "URI: $uri")

        // Set loading immediately (prevents UI race where editor navigates back before load starts)
        Log.d("EditorViewModel", "Setting isLoading = true (sync)")
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            
            Log.d("EditorViewModel", "Calling ImageUtils.loadBitmapFromUri()")
            val bitmap = withContext(Dispatchers.IO) {
                // Downscale for preview to avoid performance issues
                ImageUtils.loadBitmapFromUri(
                    getApplication(),
                    uri,
                    maxWidth = 2048,
                    maxHeight = 2048
                )
            }
            
            Log.d("EditorViewModel", "Bitmap loaded: ${bitmap != null}")
            
            if (bitmap != null) {
                Log.d("EditorViewModel", "Bitmap dimensions: ${bitmap.width}x${bitmap.height}")
                Log.d("EditorViewModel", "Bitmap config: ${bitmap.config}")
                
                // Reset any previous session state
                cancelApplyWorker()
                clearUndoStack()
                clearRedoStack()
                recycleSessionBitmaps()

                initialBitmap = bitmap
                originalBitmap = bitmap
                editedBitmap = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, bitmap.isMutable)

                textOverlays = emptyList()
                selectedTextId = null
                stickerOverlays = emptyList()
                selectedStickerId = null
                drawPaths = emptyList()
                currentFrameConfig = FrameConfig.NONE
                
                Log.d("EditorViewModel", "Created editedBitmap copy")
                
                brightness = 0f
                contrast = 0f
                saturation = 0f
                exposure = 0f
                highlights = 0f
                shadows = 0f
                vibrance = 0f
                warmth = 0f
                tint = 0f
                hue = 0f
                sharpness = 0f
                definition = 0f
                vignette = 0f
                glow = 0f
                activeFilterType = null
                
                Log.d("EditorViewModel", "Updating UI state with bitmap")
                _uiState.value = _uiState.value.copy(
                    currentBitmap = editedBitmap,
                    originalBitmap = originalBitmap,
                    isLoading = false,
                    hasImage = true,
                    canUndo = false,
                    canRedo = false,
                    brightness = 0f,
                    contrast = 0f,
                    saturation = 0f,
                    exposure = 0f,
                    highlights = 0f,
                    shadows = 0f,
                    vibrance = 0f,
                    warmth = 0f,
                    tint = 0f,
                    hue = 0f,
                    sharpness = 0f,
                    definition = 0f,
                    vignette = 0f,
                    glow = 0f,
                    activeFilterType = null,
                    textOverlays = textOverlays,
                    selectedTextId = selectedTextId,
                    stickerOverlays = stickerOverlays,
                    selectedStickerId = selectedStickerId,
                    drawPaths = drawPaths,
                    drawColor = drawColor,
                    drawStrokeWidthNorm = drawStrokeWidthNorm,
                    frameConfig = currentFrameConfig
                )
                Log.d("EditorViewModel", "=== LOAD IMAGE SUCCESS ===")
            } else {
                Log.e("EditorViewModel", "ERROR: Failed to load bitmap from URI")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load image"
                )
                Log.d("EditorViewModel", "=== LOAD IMAGE FAILED ===")
            }
        }
    }
    
    /**
     * Updates brightness adjustment value.
     * Limited to -50..50 to prevent completely black/white images.
     */
    fun setBrightness(value: Float) {
        brightness = value.coerceIn(-50f, 50f)
        requestApplyAdjustments()
    }
    
    /**
     * Updates contrast adjustment value.
     */
    fun setContrast(value: Float) {
        contrast = value.coerceIn(-100f, 100f)
        requestApplyAdjustments()
    }
    
    /**
     * Updates saturation adjustment value.
     */
    fun setSaturation(value: Float) {
        saturation = value.coerceIn(-100f, 100f)
        requestApplyAdjustments()
    }

    fun setExposure(value: Float) {
        exposure = value.coerceIn(-100f, 100f)
        requestApplyAdjustments()
    }

    fun setHighlights(value: Float) {
        highlights = value.coerceIn(-100f, 100f)
        requestApplyAdjustments()
    }

    fun setShadows(value: Float) {
        shadows = value.coerceIn(-100f, 100f)
        requestApplyAdjustments()
    }

    fun setVibrance(value: Float) {
        vibrance = value.coerceIn(-100f, 100f)
        requestApplyAdjustments(debounceMs = 90L) // vibrance is heavier; coalesce a bit more
    }

    fun setWarmth(value: Float) {
        warmth = value.coerceIn(-100f, 100f)
        requestApplyAdjustments()
    }

    fun setTint(value: Float) {
        tint = value.coerceIn(-100f, 100f)
        requestApplyAdjustments()
    }

    fun setHue(value: Float) {
        hue = value.coerceIn(-100f, 100f)
        requestApplyAdjustments()
    }

    fun setSharpness(value: Float) {
        sharpness = value.coerceIn(0f, 100f)
        requestApplyAdjustments()
    }

    fun setDefinition(value: Float) {
        definition = value.coerceIn(0f, 100f)
        requestApplyAdjustments()
    }

    fun setVignette(value: Float) {
        vignette = value.coerceIn(0f, 100f)
        requestApplyAdjustments()
    }

    fun setGlow(value: Float) {
        glow = value.coerceIn(0f, 100f)
        requestApplyAdjustments()
    }

    fun setAdjustment(type: AdjustmentType, value: Float) {
        when (type) {
            AdjustmentType.BRIGHTNESS -> setBrightness(value)
            AdjustmentType.CONTRAST -> setContrast(value)
            AdjustmentType.SATURATION -> setSaturation(value)
            AdjustmentType.EXPOSURE -> setExposure(value)
            AdjustmentType.HIGHLIGHTS -> setHighlights(value)
            AdjustmentType.SHADOWS -> setShadows(value)
            AdjustmentType.VIBRANCE -> setVibrance(value)
            AdjustmentType.WARMTH -> setWarmth(value)
            AdjustmentType.TINT -> setTint(value)
            AdjustmentType.HUE -> setHue(value)
            AdjustmentType.SHARPNESS -> setSharpness(value)
            AdjustmentType.DEFINITION -> setDefinition(value)
            AdjustmentType.VIGNETTE -> setVignette(value)
            AdjustmentType.GLOW -> setGlow(value)
        }
    }

    fun getAdjustmentValue(type: AdjustmentType): Float {
        return when (type) {
            AdjustmentType.BRIGHTNESS -> brightness
            AdjustmentType.CONTRAST -> contrast
            AdjustmentType.SATURATION -> saturation
            AdjustmentType.EXPOSURE -> exposure
            AdjustmentType.HIGHLIGHTS -> highlights
            AdjustmentType.SHADOWS -> shadows
            AdjustmentType.VIBRANCE -> vibrance
            AdjustmentType.WARMTH -> warmth
            AdjustmentType.TINT -> tint
            AdjustmentType.HUE -> hue
            AdjustmentType.SHARPNESS -> sharpness
            AdjustmentType.DEFINITION -> definition
            AdjustmentType.VIGNETTE -> vignette
            AdjustmentType.GLOW -> glow
        }
    }
    
    /**
     * Request applying all current adjustments to the image.
     * Coalesces rapid updates to keep the UI responsive (especially for heavy adjustments like Vibrance).
     */
    private fun requestApplyAdjustments(debounceMs: Long = 60L) {
        requestedApplyVersion += 1
        val target = requestedApplyVersion

        if (applyWorkerJob?.isActive == true) {
            // Worker will pick up the newest version when it finishes current work.
            return
        }

        applyWorkerJob = viewModelScope.launch {
            while (appliedApplyVersion < requestedApplyVersion && isActive) {
                val expected = requestedApplyVersion

                // Debounce/coalesce rapid slider updates.
                withContext(Dispatchers.Default) { /* keep dispatcher warm */ }
                kotlinx.coroutines.delay(debounceMs)
                if (expected != requestedApplyVersion) continue

                val source = originalBitmap ?: break
                _uiState.value = _uiState.value.copy(isProcessing = true)

                val result = withContext(Dispatchers.Default) {
                    ImageProcessor.applyAdjustments(
                        source = source,
                        brightness = brightness,
                        contrast = contrast,
                        saturation = saturation,
                        exposure = exposure,
                        highlights = highlights,
                        shadows = shadows,
                        vibrance = vibrance,
                        warmth = warmth,
                        tint = tint,
                        hue = hue,
                        sharpness = sharpness,
                        definition = definition,
                        vignette = vignette,
                        glow = glow
                    )
                }

                pushUndoSnapshot()
                editedBitmap = result
                clearRedoStack()

                // If there's an active filter, reapply it after adjustments
                val finalResult = if (activeFilterType != null) {
                    val filter = when (activeFilterType) {
                        FilterType.GRAYSCALE -> com.example.photoeditor.image.filters.GrayscaleFilter()
                        FilterType.SEPIA -> com.example.photoeditor.image.filters.SepiaFilter()
                        FilterType.BLUR -> com.example.photoeditor.image.filters.BlurFilter(radius = 5)
                        FilterType.INVERT -> com.example.photoeditor.image.filters.InvertFilter()
                        FilterType.NOIR -> com.example.photoeditor.image.filters.NoirFilter()
                        FilterType.VIVID -> com.example.photoeditor.image.filters.VividFilter()
                        FilterType.WARM -> com.example.photoeditor.image.filters.WarmFilter()
                        FilterType.COOL -> com.example.photoeditor.image.filters.CoolFilter()
                        FilterType.VINTAGE -> com.example.photoeditor.image.filters.VintageFilter()
                        FilterType.FADE -> com.example.photoeditor.image.filters.FadeFilter()
                        FilterType.DRAMATIC -> com.example.photoeditor.image.filters.DramaticFilter()
                        FilterType.PASTEL -> com.example.photoeditor.image.filters.PastelFilter()
                        FilterType.NEON -> com.example.photoeditor.image.filters.NeonFilter()
                        FilterType.RETRO -> com.example.photoeditor.image.filters.RetroFilter()
                        FilterType.DREAMY -> com.example.photoeditor.image.filters.DreamyFilter()
                        FilterType.SUNSET -> com.example.photoeditor.image.filters.SunsetFilter()
                        FilterType.CLEAN_SKIN -> com.example.photoeditor.image.filters.CleanSkinFilter()
                        null -> null
                    }
                    if (filter != null) {
                        withContext(Dispatchers.Default) { filter.apply(result) }
                    } else {
                        result
                    }
                } else {
                    result
                }

                // Recycle intermediate result if different
                if (finalResult != result) {
                    result.recycle()
                }
                editedBitmap = finalResult

                _uiState.value = _uiState.value.copy(
                    currentBitmap = finalResult,
                    originalBitmap = originalBitmap,
                    isProcessing = false,
                    canUndo = undoStack.isNotEmpty(),
                    canRedo = false,
                    brightness = brightness,
                    contrast = contrast,
                    saturation = saturation,
                    exposure = exposure,
                    highlights = highlights,
                    shadows = shadows,
                    vibrance = vibrance,
                    warmth = warmth,
                    tint = tint,
                    hue = hue,
                    sharpness = sharpness,
                    definition = definition,
                    vignette = vignette,
                    glow = glow,
                    activeFilterType = activeFilterType
                )

                appliedApplyVersion = expected
            }
        }
    }
    
    /**
     * Applies a filter to the image.
     * Always starts from the original image with adjustments, then applies the filter.
     * Only one filter can be active at a time.
     */
    fun applyFilter(filter: ImageFilter, filterType: FilterType) {
        val source = originalBitmap ?: return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true)
            
            // First apply adjustments to original image
            val adjustedImage = withContext(Dispatchers.Default) {
                ImageProcessor.applyAdjustments(
                    source = source,
                    brightness = brightness,
                    contrast = contrast,
                    saturation = saturation,
                    exposure = exposure,
                    highlights = highlights,
                    shadows = shadows,
                    vibrance = vibrance,
                    warmth = warmth,
                    tint = tint,
                    hue = hue,
                    sharpness = sharpness,
                    definition = definition,
                    vignette = vignette,
                    glow = glow
                )
            }
            
            // Then apply the filter
            val result = withContext(Dispatchers.Default) {
                filter.apply(adjustedImage)
            }
            
            // Clean up the adjusted image if it's not the same as editedBitmap
            if (adjustedImage != editedBitmap) {
                adjustedImage.recycle()
            }
            
            pushUndoSnapshot()
            editedBitmap = result
            activeFilterType = filterType
            clearRedoStack()
            
            _uiState.value = _uiState.value.copy(
                currentBitmap = result,
                originalBitmap = originalBitmap,
                isProcessing = false,
                canUndo = undoStack.isNotEmpty(),
                canRedo = false,
                activeFilterType = filterType
            )
        }
    }
    
    /**
     * Removes the active filter.
     */
    fun removeFilter() {
        val source = originalBitmap ?: return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessing = true)
            
            // Apply only adjustments (no filter)
            val result = withContext(Dispatchers.Default) {
                ImageProcessor.applyAdjustments(
                    source = source,
                    brightness = brightness,
                    contrast = contrast,
                    saturation = saturation,
                    exposure = exposure,
                    highlights = highlights,
                    shadows = shadows,
                    vibrance = vibrance,
                    warmth = warmth,
                    tint = tint,
                    hue = hue,
                    sharpness = sharpness,
                    definition = definition,
                    vignette = vignette,
                    glow = glow
                )
            }
            
            pushUndoSnapshot()
            editedBitmap = result
            activeFilterType = null
            clearRedoStack()
            
            _uiState.value = _uiState.value.copy(
                currentBitmap = result,
                originalBitmap = originalBitmap,
                isProcessing = false,
                canUndo = undoStack.isNotEmpty(),
                canRedo = false,
                activeFilterType = null
            )
        }
    }
    
    /**
     * Resets all adjustments to their default values.
     */
    fun resetAdjustments() {
        brightness = 0f
        contrast = 0f
        saturation = 0f
        exposure = 0f
        highlights = 0f
        shadows = 0f
        vibrance = 0f
        warmth = 0f
        tint = 0f
        hue = 0f
        sharpness = 0f
        definition = 0f
        vignette = 0f
        glow = 0f
        activeFilterType = null
        
        originalBitmap?.let {
            pushUndoSnapshot()
            editedBitmap = it.copy(it.config ?: Bitmap.Config.ARGB_8888, it.isMutable)
            clearRedoStack()
            
            _uiState.value = _uiState.value.copy(
                currentBitmap = editedBitmap,
                originalBitmap = originalBitmap,
                canUndo = undoStack.isNotEmpty(),
                canRedo = false,
                brightness = 0f,
                contrast = 0f,
                saturation = 0f,
                exposure = 0f,
                highlights = 0f,
                shadows = 0f,
                vibrance = 0f,
                warmth = 0f,
                tint = 0f,
                hue = 0f,
                sharpness = 0f,
                definition = 0f,
                vignette = 0f,
                glow = 0f,
                activeFilterType = null
            )
        }
    }

    /**
     * Rotate the image 90 degrees clockwise.
     */
    fun rotate90Clockwise() {
        val source = originalBitmap ?: return
        viewModelScope.launch {
            cancelApplyWorker()
            _uiState.value = _uiState.value.copy(isProcessing = true)

            pushUndoSnapshot()

            val rotatedOriginal = withContext(Dispatchers.Default) {
                BitmapTransforms.rotate90Clockwise(source)
            }

            // Replace original and re-render edited from it (keep current adjustments + active filter)
            originalBitmap = rotatedOriginal
            val newEdited = renderFromOriginal(rotatedOriginal)
            editedBitmap = newEdited

            clearRedoStack()

            _uiState.value = _uiState.value.copy(
                currentBitmap = newEdited,
                originalBitmap = rotatedOriginal,
                isProcessing = false,
                canUndo = undoStack.isNotEmpty(),
                canRedo = false,
                activeFilterType = activeFilterType
            )
        }
    }

    /**
     * Mirror (flip horizontally) the image.
     */
    fun flipHorizontal() {
        val source = originalBitmap ?: return
        viewModelScope.launch {
            cancelApplyWorker()
            _uiState.value = _uiState.value.copy(isProcessing = true)

            pushUndoSnapshot()

            val flippedOriginal = withContext(Dispatchers.Default) {
                BitmapTransforms.flipHorizontal(source)
            }

            originalBitmap = flippedOriginal
            val newEdited = renderFromOriginal(flippedOriginal)
            editedBitmap = newEdited

            clearRedoStack()

            _uiState.value = _uiState.value.copy(
                currentBitmap = newEdited,
                originalBitmap = flippedOriginal,
                isProcessing = false,
                canUndo = undoStack.isNotEmpty(),
                canRedo = false,
                activeFilterType = activeFilterType
            )
        }
    }

    /**
     * Center-crop the image by a percentage (keeps current aspect ratio).
     * Example: 0.8f keeps 80% of width/height.
     */
    fun cropCenter(scale: Float) {
        val source = originalBitmap ?: return
        viewModelScope.launch {
            cancelApplyWorker()
            _uiState.value = _uiState.value.copy(isProcessing = true)

            pushUndoSnapshot()

            val croppedOriginal = withContext(Dispatchers.Default) {
                BitmapTransforms.cropCenter(source, scale)
            }

            originalBitmap = croppedOriginal
            val newEdited = renderFromOriginal(croppedOriginal)
            editedBitmap = newEdited

            clearRedoStack()

            _uiState.value = _uiState.value.copy(
                currentBitmap = newEdited,
                originalBitmap = croppedOriginal,
                isProcessing = false,
                canUndo = undoStack.isNotEmpty(),
                canRedo = false,
                activeFilterType = activeFilterType
            )
        }
    }

    /**
     * Crop using an explicit rect in bitmap pixel coordinates.
     * This updates the current session base image (originalBitmap) then re-renders editedBitmap.
     */
    fun cropToRect(rect: Rect) {
        val source = originalBitmap ?: return
        if (rect.width() <= 1 || rect.height() <= 1) return

        viewModelScope.launch {
            cancelApplyWorker()
            _uiState.value = _uiState.value.copy(isProcessing = true)

            pushUndoSnapshot()

            val safeLeft = rect.left.coerceIn(0, source.width - 2)
            val safeTop = rect.top.coerceIn(0, source.height - 2)
            val safeRight = rect.right.coerceIn(safeLeft + 1, source.width)
            val safeBottom = rect.bottom.coerceIn(safeTop + 1, source.height)

            val croppedOriginal = withContext(Dispatchers.Default) {
                Bitmap.createBitmap(source, safeLeft, safeTop, safeRight - safeLeft, safeBottom - safeTop)
            }

            originalBitmap = croppedOriginal
            val newEdited = renderFromOriginal(croppedOriginal)
            editedBitmap = newEdited

            clearRedoStack()

            _uiState.value = _uiState.value.copy(
                currentBitmap = newEdited,
                originalBitmap = originalBitmap,
                isProcessing = false,
                canUndo = undoStack.isNotEmpty(),
                canRedo = false,
                activeFilterType = activeFilterType
            )
        }
    }

    /**
     * Crop to a target aspect ratio (width/height), using a max-area centered crop.
     */
    fun cropToAspectRatio(targetRatio: Float) {
        val source = originalBitmap ?: return
        viewModelScope.launch {
            cancelApplyWorker()
            _uiState.value = _uiState.value.copy(isProcessing = true)

            pushUndoSnapshot()

            val croppedOriginal = withContext(Dispatchers.Default) {
                BitmapTransforms.cropToAspectRatio(source, targetRatio)
            }

            originalBitmap = croppedOriginal
            val newEdited = renderFromOriginal(croppedOriginal)
            editedBitmap = newEdited

            clearRedoStack()

            _uiState.value = _uiState.value.copy(
                currentBitmap = newEdited,
                originalBitmap = croppedOriginal,
                isProcessing = false,
                canUndo = undoStack.isNotEmpty(),
                canRedo = false,
                activeFilterType = activeFilterType
            )
        }
    }

    /**
     * Aspect ratio crop that always uses the first-loaded image as the source,
     * regardless of the current base image.
     */
    fun cropToAspectRatioFromInitial(targetRatio: Float, scale: Float = 1f) {
        val source = initialBitmap ?: return
        viewModelScope.launch {
            cancelApplyWorker()
            _uiState.value = _uiState.value.copy(isProcessing = true)

            pushUndoSnapshot()

            val croppedOriginal = withContext(Dispatchers.Default) {
                BitmapTransforms.cropToAspectRatio(source, targetRatio, scale)
            }

            originalBitmap = croppedOriginal
            val newEdited = renderFromOriginal(croppedOriginal)
            editedBitmap = newEdited

            clearRedoStack()

            _uiState.value = _uiState.value.copy(
                currentBitmap = newEdited,
                originalBitmap = originalBitmap,
                isProcessing = false,
                canUndo = undoStack.isNotEmpty(),
                canRedo = false,
                activeFilterType = activeFilterType
            )
        }
    }

    /**
     * Resets the base image back to the first-loaded image.
     * Keeps current adjustments + active filter.
     */
    fun resetToInitialImage() {
        val source = initialBitmap ?: return
        viewModelScope.launch {
            cancelApplyWorker()
            _uiState.value = _uiState.value.copy(isProcessing = true)

            pushUndoSnapshot()

            originalBitmap = source
            val newEdited = renderFromOriginal(source)
            editedBitmap = newEdited

            clearRedoStack()

            _uiState.value = _uiState.value.copy(
                currentBitmap = newEdited,
                originalBitmap = originalBitmap,
                isProcessing = false,
                canUndo = undoStack.isNotEmpty(),
                canRedo = false,
                activeFilterType = activeFilterType
            )
        }
    }
    
    /**
     * Undoes the last operation.
     */
    fun undo() {
        if (undoStack.isEmpty()) return

        cancelApplyWorker()

        pushRedoSnapshot()
        val snap = undoStack.removeLast()

        originalBitmap = snap.originalBitmap
        editedBitmap = snap.editedBitmap

        brightness = snap.brightness
        contrast = snap.contrast
        saturation = snap.saturation
        exposure = snap.exposure
        highlights = snap.highlights
        shadows = snap.shadows
        vibrance = snap.vibrance
        warmth = snap.warmth
        tint = snap.tint
        hue = snap.hue
        sharpness = snap.sharpness
        definition = snap.definition
        vignette = snap.vignette
        glow = snap.glow
        activeFilterType = snap.activeFilterType
        textOverlays = snap.textOverlays
        selectedTextId = snap.selectedTextId
        stickerOverlays = snap.stickerOverlays
        selectedStickerId = snap.selectedStickerId
        drawPaths = snap.drawPaths
        currentFrameConfig = snap.frameConfig

        _uiState.value = _uiState.value.copy(
            currentBitmap = editedBitmap,
            originalBitmap = originalBitmap,
            canUndo = undoStack.isNotEmpty(),
            canRedo = redoStack.isNotEmpty(),
            brightness = brightness,
            contrast = contrast,
            saturation = saturation,
            exposure = exposure,
            highlights = highlights,
            shadows = shadows,
            vibrance = vibrance,
            warmth = warmth,
            tint = tint,
            hue = hue,
            sharpness = sharpness,
            definition = definition,
            vignette = vignette,
            glow = glow,
            activeFilterType = activeFilterType,
            textOverlays = textOverlays,
            selectedTextId = selectedTextId,
            stickerOverlays = stickerOverlays,
            selectedStickerId = selectedStickerId,
            drawPaths = drawPaths,
            drawColor = drawColor,
            drawStrokeWidthNorm = drawStrokeWidthNorm,
            frameConfig = currentFrameConfig
        )
    }
    
    /**
     * Redoes the last undone operation.
     */
    fun redo() {
        if (redoStack.isEmpty()) return

        cancelApplyWorker()

        pushUndoSnapshot()
        val snap = redoStack.removeLast()

        originalBitmap = snap.originalBitmap
        editedBitmap = snap.editedBitmap

        brightness = snap.brightness
        contrast = snap.contrast
        saturation = snap.saturation
        exposure = snap.exposure
        highlights = snap.highlights
        shadows = snap.shadows
        vibrance = snap.vibrance
        warmth = snap.warmth
        tint = snap.tint
        hue = snap.hue
        sharpness = snap.sharpness
        definition = snap.definition
        vignette = snap.vignette
        glow = snap.glow
        activeFilterType = snap.activeFilterType
        textOverlays = snap.textOverlays
        selectedTextId = snap.selectedTextId
        stickerOverlays = snap.stickerOverlays
        selectedStickerId = snap.selectedStickerId
        drawPaths = snap.drawPaths
        currentFrameConfig = snap.frameConfig

        _uiState.value = _uiState.value.copy(
            currentBitmap = editedBitmap,
            originalBitmap = originalBitmap,
            canUndo = undoStack.isNotEmpty(),
            canRedo = redoStack.isNotEmpty(),
            brightness = brightness,
            contrast = contrast,
            saturation = saturation,
            exposure = exposure,
            highlights = highlights,
            shadows = shadows,
            vibrance = vibrance,
            warmth = warmth,
            tint = tint,
            hue = hue,
            sharpness = sharpness,
            definition = definition,
            vignette = vignette,
            glow = glow,
            activeFilterType = activeFilterType,
            textOverlays = textOverlays,
            selectedTextId = selectedTextId,
            stickerOverlays = stickerOverlays,
            selectedStickerId = selectedStickerId,
            drawPaths = drawPaths,
            drawColor = drawColor,
            drawStrokeWidthNorm = drawStrokeWidthNorm,
            frameConfig = currentFrameConfig
        )
    }
    
    /**
     * Saves the edited image to the gallery.
     */
    fun saveImage() {
        val bitmap = editedBitmap ?: return
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)

            val stickers = stickerOverlays
            val texts = textOverlays
            val draws = drawPaths
            val frameConfig = currentFrameConfig
            
            // Apply layers in order: stickers -> text -> draw -> frame
            val withStickers = if (stickers.isEmpty()) bitmap else BitmapStickerRenderer.render(bitmap, stickers)
            val withText = if (texts.isEmpty()) withStickers else BitmapTextRenderer.render(withStickers, texts)
            val withDraw = if (draws.isEmpty()) withText else BitmapDrawRenderer.render(withText, draws)
            val finalBitmap = if (frameConfig.type == com.example.photoeditor.model.FrameType.NONE) {
                withDraw
            } else {
                FrameRenderer.applyFrame(getApplication(), withDraw, frameConfig)
            }
            
            val success = withContext(Dispatchers.IO) {
                ImageUtils.saveBitmapToGallery(getApplication(), finalBitmap)
            }
            
            // Clean up intermediate bitmaps
            if (withDraw !== withText && withDraw !== bitmap && withDraw !== finalBitmap) {
                withDraw.recycle()
            }
            if (withText !== withStickers && withText !== bitmap && withText !== finalBitmap) {
                withText.recycle()
            }
            if (withStickers !== bitmap && withStickers !== withText && withStickers !== finalBitmap) {
                withStickers.recycle()
            }
            
            val appContext = getApplication<Application>()
            // Wrap context with correct locale to get localized strings
            val localizedContext = LocaleHelper.wrapContext(appContext)
            if (success) {
                ImageUtils.showToast(localizedContext, localizedContext.getString(R.string.image_saved_successfully))
            } else {
                ImageUtils.showToast(localizedContext, localizedContext.getString(R.string.failed_to_save_image))
            }
            
            _uiState.value = _uiState.value.copy(isSaving = false)
        }
    }

    // ---- Text tool API ----

    fun addTextOverlay(initialText: String = "") {
        val id = java.util.UUID.randomUUID().toString()
        val overlay = TextOverlay(
            id = id,
            text = initialText,
            xNorm = 0.20f,
            yNorm = 0.20f,
            sizeNorm = 0.07f,
            rotationDegrees = 0f,
            isBold = false,
            isItalic = false,
            isUnderline = false,
            argb = Color.WHITE
        )
        textOverlays = textOverlays + overlay
        selectedTextId = id
        _uiState.value = _uiState.value.copy(textOverlays = textOverlays, selectedTextId = selectedTextId)
    }

    fun selectTextOverlay(id: String?) {
        selectedTextId = id
        if (id != null) selectedStickerId = null
        _uiState.value = _uiState.value.copy(selectedTextId = selectedTextId, selectedStickerId = selectedStickerId)
    }

    fun updateSelectedText(text: String) {
        val id = selectedTextId ?: return
        textOverlays = textOverlays.map { if (it.id == id) it.copy(text = text) else it }
        _uiState.value = _uiState.value.copy(textOverlays = textOverlays)
    }

    fun toggleSelectedTextBold() {
        val id = selectedTextId ?: return
        textOverlays = textOverlays.map { if (it.id == id) it.copy(isBold = !it.isBold) else it }
        _uiState.value = _uiState.value.copy(textOverlays = textOverlays)
    }

    fun toggleSelectedTextItalic() {
        val id = selectedTextId ?: return
        textOverlays = textOverlays.map { if (it.id == id) it.copy(isItalic = !it.isItalic) else it }
        _uiState.value = _uiState.value.copy(textOverlays = textOverlays)
    }

    fun toggleSelectedTextUnderline() {
        val id = selectedTextId ?: return
        textOverlays = textOverlays.map { if (it.id == id) it.copy(isUnderline = !it.isUnderline) else it }
        _uiState.value = _uiState.value.copy(textOverlays = textOverlays)
    }

    fun setSelectedTextColor(argb: Int) {
        val id = selectedTextId ?: return
        textOverlays = textOverlays.map { if (it.id == id) it.copy(argb = argb) else it }
        _uiState.value = _uiState.value.copy(textOverlays = textOverlays)
    }

    fun setSelectedTextSizeNorm(sizeNorm: Float) {
        val id = selectedTextId ?: return
        val clamped = sizeNorm.coerceIn(0.02f, 0.20f)
        textOverlays = textOverlays.map { if (it.id == id) it.copy(sizeNorm = clamped) else it }
        _uiState.value = _uiState.value.copy(textOverlays = textOverlays)
    }

    fun setSelectedTextRotationDegrees(degrees: Float) {
        val id = selectedTextId ?: return
        setTextRotationDegreesById(id, degrees)
    }

    fun setTextRotationDegreesById(id: String, degrees: Float) {
        // Keep rotation in a reasonable range for UI usage
        val clamped = degrees.coerceIn(-180f, 180f)
        textOverlays = textOverlays.map { o ->
            if (o.id == id) o.copy(rotationDegrees = clamped) else o
        }
        _uiState.value = _uiState.value.copy(textOverlays = textOverlays)
    }

    fun setSelectedTextSizeAndPosition(sizeNorm: Float, xNorm: Float, yNorm: Float) {
        val id = selectedTextId ?: return
        setTextSizeAndPositionById(id, sizeNorm, xNorm, yNorm)
    }

    fun moveSelectedTextBy(dxNorm: Float, dyNorm: Float) {
        val id = selectedTextId ?: return
        moveTextById(id, dxNorm, dyNorm)
    }

    fun setTextSizeAndPositionById(id: String, sizeNorm: Float, xNorm: Float, yNorm: Float) {
        val s = sizeNorm.coerceIn(0.02f, 0.20f)
        val x = xNorm.coerceIn(0f, 1f)
        val y = yNorm.coerceIn(0f, 1f)
        textOverlays = textOverlays.map { o ->
            if (o.id == id) o.copy(sizeNorm = s, xNorm = x, yNorm = y) else o
        }
        _uiState.value = _uiState.value.copy(textOverlays = textOverlays)
    }

    fun moveTextById(id: String, dxNorm: Float, dyNorm: Float) {
        textOverlays = textOverlays.map { o ->
            if (o.id == id) {
                o.copy(
                    xNorm = (o.xNorm + dxNorm).coerceIn(0f, 1f),
                    yNorm = (o.yNorm + dyNorm).coerceIn(0f, 1f)
                )
            } else o
        }
        _uiState.value = _uiState.value.copy(textOverlays = textOverlays)
    }

    fun deleteSelectedText() {
        val id = selectedTextId ?: return
        textOverlays = textOverlays.filterNot { it.id == id }
        selectedTextId = textOverlays.lastOrNull()?.id
        _uiState.value = _uiState.value.copy(textOverlays = textOverlays, selectedTextId = selectedTextId)
    }

    // ---- Sticker tool API ----

    fun addStickerOverlay(emoji: String) {
        val id = java.util.UUID.randomUUID().toString()
        val overlay = StickerOverlay(
            id = id,
            emoji = emoji,
            xNorm = 0.35f,
            yNorm = 0.35f,
            sizeNorm = 0.10f,
            rotationDegrees = 0f
        )
        stickerOverlays = stickerOverlays + overlay
        selectedStickerId = id
        selectedTextId = null
        _uiState.value = _uiState.value.copy(
            stickerOverlays = stickerOverlays,
            selectedStickerId = selectedStickerId,
            selectedTextId = selectedTextId
        )
    }

    fun selectStickerOverlay(id: String?) {
        selectedStickerId = id
        if (id != null) selectedTextId = null
        _uiState.value = _uiState.value.copy(selectedStickerId = selectedStickerId, selectedTextId = selectedTextId)
    }

    fun deleteSelectedSticker() {
        val id = selectedStickerId ?: return
        stickerOverlays = stickerOverlays.filterNot { it.id == id }
        selectedStickerId = null
        _uiState.value = _uiState.value.copy(stickerOverlays = stickerOverlays, selectedStickerId = selectedStickerId)
    }

    // ---- Frame tool API ----
    
    /**
     * Sets the frame configuration.
     * Frame is applied on save, previewed in UI.
     */
    fun setFrameConfig(config: FrameConfig) {
        currentFrameConfig = config
        _uiState.value = _uiState.value.copy(frameConfig = currentFrameConfig)
    }

    // ---- Draw tool API ----

    fun addPointToCurrentDrawPath(normX: Float, normY: Float) {
        if (drawPaths.isEmpty()) return
        val last = drawPaths.last()
        val updated = last.copy(points = last.points + Offset(normX, normY))
        drawPaths = drawPaths.dropLast(1) + updated
        _uiState.value = _uiState.value.copy(drawPaths = drawPaths)
    }

    fun startNewDrawPath(normX: Float, normY: Float) {
        pushUndoSnapshot()
        val path = DrawPath(
            points = listOf(Offset(normX, normY)),
            color = drawColor,
            strokeWidthNorm = drawStrokeWidthNorm
        )
        drawPaths = drawPaths + path
        _uiState.value = _uiState.value.copy(drawPaths = drawPaths)
    }

    fun clearDrawings() {
        if (drawPaths.isEmpty()) return
        pushUndoSnapshot()
        drawPaths = emptyList()
        _uiState.value = _uiState.value.copy(drawPaths = drawPaths)
    }

    fun setDrawColor(color: Int) {
        drawColor = color
        _uiState.value = _uiState.value.copy(drawColor = drawColor)
    }

    fun setDrawStrokeWidth(norm: Float) {
        drawStrokeWidthNorm = norm.coerceIn(0.005f, 0.05f)
        _uiState.value = _uiState.value.copy(drawStrokeWidthNorm = drawStrokeWidthNorm)
    }
    
    fun moveStickerById(id: String, dxNorm: Float, dyNorm: Float) {
        stickerOverlays = stickerOverlays.map { o ->
            if (o.id == id) {
                o.copy(
                    xNorm = (o.xNorm + dxNorm).coerceIn(0f, 1f),
                    yNorm = (o.yNorm + dyNorm).coerceIn(0f, 1f)
                )
            } else o
        }
        _uiState.value = _uiState.value.copy(stickerOverlays = stickerOverlays)
    }

    fun setStickerRotationDegreesById(id: String, degrees: Float) {
        val clamped = degrees.coerceIn(-180f, 180f)
        stickerOverlays = stickerOverlays.map { o -> if (o.id == id) o.copy(rotationDegrees = clamped) else o }
        _uiState.value = _uiState.value.copy(stickerOverlays = stickerOverlays)
    }

    fun setStickerSizeAndPositionById(id: String, sizeNorm: Float, xNorm: Float, yNorm: Float) {
        val s = sizeNorm.coerceIn(0.03f, 0.25f)
        val x = xNorm.coerceIn(0f, 1f)
        val y = yNorm.coerceIn(0f, 1f)
        stickerOverlays = stickerOverlays.map { o ->
            if (o.id == id) o.copy(sizeNorm = s, xNorm = x, yNorm = y) else o
        }
        _uiState.value = _uiState.value.copy(stickerOverlays = stickerOverlays)
    }
    
    /**
     * Toggles between showing original and edited image.
     */
    fun toggleShowOriginal() {
        _uiState.value = _uiState.value.copy(
            showOriginal = !_uiState.value.showOriginal
        )
    }
    
    /**
     * Clears the current image and resets all state.
     * Returns to the initial state (no image selected).
     */
    fun clearImage() {
        // Clean up bitmaps
        recycleSessionBitmaps()
        clearUndoStack()
        clearRedoStack()
        
        // Clear all state
        brightness = 0f
        contrast = 0f
        saturation = 0f
        exposure = 0f
        highlights = 0f
        shadows = 0f
        vibrance = 0f
        warmth = 0f
        tint = 0f
        hue = 0f
        sharpness = 0f
        definition = 0f
        vignette = 0f
        glow = 0f
        activeFilterType = null
        textOverlays = emptyList()
        selectedTextId = null
        stickerOverlays = emptyList()
        selectedStickerId = null
        drawPaths = emptyList()
        currentFrameConfig = FrameConfig.NONE
        
        // Reset UI state
        _uiState.value = EditorUiState()
    }
    
    override fun onCleared() {
        super.onCleared()
        // Clean up bitmaps to prevent memory leaks
        recycleSessionBitmaps()
        clearUndoStack()
        clearRedoStack()
    }
}

/**
 * UI State for the editor screen.
 */
data class EditorUiState(
    val currentBitmap: Bitmap? = null,
    val originalBitmap: Bitmap? = null,
    val isLoading: Boolean = false,
    val isProcessing: Boolean = false,
    val isSaving: Boolean = false,
    val hasImage: Boolean = false,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val saturation: Float = 0f,
    val exposure: Float = 0f,
    val highlights: Float = 0f,
    val shadows: Float = 0f,
    val vibrance: Float = 0f,
    val warmth: Float = 0f,
    val tint: Float = 0f,
    val hue: Float = 0f,
    val sharpness: Float = 0f,
    val definition: Float = 0f,
    val vignette: Float = 0f,
    val glow: Float = 0f,
    val errorMessage: String? = null,
    val showOriginal: Boolean = false,
    val activeFilterType: FilterType? = null,
    val textOverlays: List<TextOverlay> = emptyList(),
    val selectedTextId: String? = null,
    val stickerOverlays: List<StickerOverlay> = emptyList(),
    val selectedStickerId: String? = null,
    val drawPaths: List<DrawPath> = emptyList(),
    val drawColor: Int = Color.BLACK,
    val drawStrokeWidthNorm: Float = 0.015f,
    val frameConfig: FrameConfig = FrameConfig.NONE
)
