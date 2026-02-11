package com.example.photoeditor.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.photoeditor.image.CollageRenderer
import com.example.photoeditor.model.CollageConfig
import com.example.photoeditor.model.CollageImage
import com.example.photoeditor.model.CollageTemplate
import com.example.photoeditor.model.CollageTemplates
import com.example.photoeditor.utils.ImageUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CollageUiState(
    val selectedImages: List<Uri> = emptyList(),
    val config: CollageConfig? = null,
    val availableTemplates: List<CollageTemplate> = emptyList(),
    val isRendering: Boolean = false,
    val previewBitmap: Bitmap? = null,
    val previewWidth: Int = 0,
    val previewHeight: Int = 0
)

class CollageViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(CollageUiState())
    val uiState: StateFlow<CollageUiState> = _uiState.asStateFlow()
    
    private var cropPreviewDebounceJob: Job? = null
    private var borderPreviewDebounceJob: Job? = null

    fun setSelectedImages(uris: List<Uri>) {
        // Shuffle images randomly
        val shuffledUris = uris.shuffled()
        val images = shuffledUris.mapIndexed { index, uri ->
            CollageImage(uri = uri)
        }
        
        val templates = CollageTemplates.getTemplatesForImageCount(uris.size)
        val defaultTemplate = templates.firstOrNull()
        
        val config = if (defaultTemplate != null && images.isNotEmpty()) {
            // Initialize images with template cell positions (randomly assigned)
            val shuffledImages = images.shuffled()
            val initializedImages = defaultTemplate.cells.take(shuffledImages.size).mapIndexed { index, cell ->
                shuffledImages[index].copy(
                    x = cell.x,
                    y = cell.y,
                    width = cell.width,
                    height = cell.height
                )
            }
            CollageConfig(
                images = initializedImages,
                template = defaultTemplate,
                borderWidth = 0f,
                borderColor = Color.WHITE
            )
        } else null

        _uiState.value = _uiState.value.copy(
            selectedImages = uris,
            config = config,
            availableTemplates = templates
        )
        
        // Generate preview
        config?.let { generatePreview(it) }
    }

    fun changeTemplate(template: CollageTemplate) {
        val currentConfig = _uiState.value.config ?: return
        val images = currentConfig.images
        
        // Shuffle images randomly when changing template
        val shuffledImages = images.shuffled()
        
        // Reinitialize images with new template positions (randomly assigned)
        val newImages = template.cells.take(shuffledImages.size).mapIndexed { index, cell ->
            shuffledImages[index].copy(
                x = cell.x,
                y = cell.y,
                width = cell.width,
                height = cell.height
            )
        }
        
        val newConfig = currentConfig.copy(
            template = template,
            images = newImages
        )
        
        _uiState.value = _uiState.value.copy(config = newConfig)
        generatePreview(newConfig)
    }
    
    fun swapImages(index1: Int, index2: Int) {
        val currentConfig = _uiState.value.config ?: return
        val images = currentConfig.images.toMutableList()
        
        if (index1 in images.indices && index2 in images.indices) {
            val temp = images[index1]
            images[index1] = images[index2].copy(
                x = images[index1].x,
                y = images[index1].y,
                width = images[index1].width,
                height = images[index1].height
            )
            images[index2] = temp.copy(
                x = images[index2].x,
                y = images[index2].y,
                width = images[index2].width,
                height = images[index2].height
            )
            
            val newConfig = currentConfig.copy(images = images)
            _uiState.value = _uiState.value.copy(config = newConfig)
            generatePreview(newConfig)
        }
    }
    
    fun replaceImage(index: Int, newImageUri: Uri) {
        val currentConfig = _uiState.value.config ?: return
        val images = currentConfig.images.toMutableList()
        
        if (index in images.indices) {
            images[index] = images[index].copy(
                uri = newImageUri,
                cropLeft = 0f, cropTop = 0f, cropRight = 1f, cropBottom = 1f
            )
            val newConfig = currentConfig.copy(images = images)
            _uiState.value = _uiState.value.copy(config = newConfig)
            generatePreview(newConfig)
        }
    }

    fun updateImageCrop(index: Int, cropLeft: Float, cropTop: Float, cropRight: Float, cropBottom: Float) {
        val currentConfig = _uiState.value.config ?: return
        if (index !in currentConfig.images.indices) return
        val images = currentConfig.images.toMutableList()
        images[index] = images[index].copy(
            cropLeft = cropLeft.coerceIn(0f, 1f),
            cropTop = cropTop.coerceIn(0f, 1f),
            cropRight = cropRight.coerceIn(0f, 1f),
            cropBottom = cropBottom.coerceIn(0f, 1f)
        )
        val newConfig = currentConfig.copy(images = images)
        _uiState.value = _uiState.value.copy(config = newConfig)
        // Debounce preview regeneration to avoid flickering during zoom/pan gesture
        cropPreviewDebounceJob?.cancel()
        cropPreviewDebounceJob = viewModelScope.launch {
            delay(80)
            generatePreview(newConfig, showLoadingState = false)
        }
    }

    fun updateImage(index: Int, image: CollageImage) {
        val currentConfig = _uiState.value.config ?: return
        val newImages = currentConfig.images.toMutableList()
        if (index in newImages.indices) {
            newImages[index] = image
            val newConfig = currentConfig.copy(images = newImages)
            _uiState.value = _uiState.value.copy(config = newConfig)
            generatePreview(newConfig)
        }
    }

    fun setBorderWidth(width: Float) {
        val currentConfig = _uiState.value.config ?: return
        val newConfig = currentConfig.copy(borderWidth = width)
        _uiState.value = _uiState.value.copy(config = newConfig)
        borderPreviewDebounceJob?.cancel()
        borderPreviewDebounceJob = viewModelScope.launch {
            delay(80)
            generatePreview(newConfig, showLoadingState = false)
        }
    }

    fun setBorderColor(color: Int) {
        val currentConfig = _uiState.value.config ?: return
        val newConfig = currentConfig.copy(borderColor = color)
        _uiState.value = _uiState.value.copy(config = newConfig)
        generatePreview(newConfig, showLoadingState = false)
    }

    fun setAspectRatio(ratio: Float?) {
        val currentConfig = _uiState.value.config ?: return
        val newConfig = currentConfig.copy(aspectRatio = ratio)
        _uiState.value = _uiState.value.copy(config = newConfig)
        generatePreview(newConfig)
    }

    fun setPreviewSize(width: Int, height: Int) {
        val w = width.coerceAtLeast(1)
        val h = height.coerceAtLeast(1)
        if (w < 50 || h < 50) return
        if (_uiState.value.previewWidth == w && _uiState.value.previewHeight == h) return
        _uiState.value = _uiState.value.copy(previewWidth = w, previewHeight = h)
        _uiState.value.config?.let { generatePreview(it) }
    }

    private fun generatePreview(config: CollageConfig, showLoadingState: Boolean = true) {
        viewModelScope.launch {
            if (showLoadingState) {
                _uiState.value = _uiState.value.copy(isRendering = true)
            }
            val (w, h) = _uiState.value.let { state ->
                val size = if (state.previewWidth > 0 && state.previewHeight > 0) {
                    kotlin.math.min(state.previewWidth, state.previewHeight)
                } else 1080
                size to size
            }
            val bitmap: Bitmap? = withContext(Dispatchers.IO) {
                CollageRenderer.render(getApplication(), config, w, h)
            }
            _uiState.value = _uiState.value.copy(
                previewBitmap = bitmap,
                isRendering = false
            )
        }
    }

    suspend fun renderFinalCollage(): Bitmap? {
        return _uiState.value.config?.let { config ->
            CollageRenderer.render(getApplication(), config)
        }
    }

    fun clear() {
        cropPreviewDebounceJob?.cancel()
        cropPreviewDebounceJob = null
        borderPreviewDebounceJob?.cancel()
        borderPreviewDebounceJob = null
        _uiState.value = CollageUiState()
    }
}
