package com.example.photoeditor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import android.content.res.Configuration
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.photoeditor.R
import com.example.photoeditor.utils.ImageUtils
import com.example.photoeditor.viewmodel.CollageViewModel
import com.example.photoeditor.ui.theme.EditorBlackTheme
import com.example.photoeditor.ui.components.collage.*
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri

/**
 * Collage editor screen - edit collage layout with template carousel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollageEditorScreen(
    selectedImages: List<android.net.Uri>,
    onBackClick: () -> Unit,
    onSaveComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    EditorBlackTheme {
        val viewModel: CollageViewModel = viewModel()
        val uiState by viewModel.uiState.collectAsState()
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        
        // Track which panel category is selected
        var selectedPanelCategory by remember { mutableStateOf<CollagePanelCategory>(CollagePanelCategory.TEMPLATES) }
        
        // Track which image index to replace
        var imageIndexToReplace by remember { mutableStateOf<Int?>(null) }
        
        // Launcher for picking replacement image
        val pickReplacementImageLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri: Uri? ->
            uri?.let {
                imageIndexToReplace?.let { index ->
                    viewModel.replaceImage(index, it)
                }
            }
            imageIndexToReplace = null
        }

        // Initialize images if not already set
        LaunchedEffect(selectedImages) {
            if (uiState.selectedImages != selectedImages) {
                viewModel.setSelectedImages(selectedImages)
            }
        }

        val config = LocalConfiguration.current
        val isPortrait = config.orientation == Configuration.ORIENTATION_PORTRAIT
        val appLanguage = com.example.photoeditor.utils.LocaleHelper.getAppLanguage(context)
        val panelLayoutDirection = if (appLanguage == "he") LayoutDirection.Rtl else LayoutDirection.Ltr

        if (isPortrait) {
            // Portrait: like Gallery - TopBar | Preview | Fixed category bar | Bottom nav
            CompositionLocalProvider(LocalLayoutDirection provides panelLayoutDirection) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    CollageTopBar(
                        title = when (selectedPanelCategory) {
                            CollagePanelCategory.TEMPLATES -> stringResource(R.string.templates)
                            CollagePanelCategory.BORDERS -> stringResource(R.string.border)
                            CollagePanelCategory.NONE -> stringResource(R.string.collage)
                        },
                        onBackClick = onBackClick,
                        onSaveClick = {
                            scope.launch {
                                val bitmap = viewModel.renderFinalCollage()
                                bitmap?.let {
                                    val saved = ImageUtils.saveBitmapToGallery(context, it)
                                    if (saved) {
                                        ImageUtils.showToast(
                                            context,
                                            context.getString(R.string.image_saved_successfully)
                                        )
                                        onSaveComplete()
                                    } else {
                                        ImageUtils.showToast(
                                            context,
                                            context.getString(R.string.failed_to_save_image)
                                        )
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(Color.Black)
                            .onSizeChanged { size ->
                                if (size.width > 0 && size.height > 0) {
                                    viewModel.setPreviewSize(size.width, size.height)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        uiState.previewBitmap?.let { bitmap ->
                            uiState.config?.let { config ->
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                    CollagePreview(
                                        previewBitmap = bitmap,
                                        config = config,
                                        onSwapImages = { index1, index2 ->
                                            viewModel.swapImages(index1, index2)
                                        },
                                        onReplaceImage = { index ->
                                            imageIndexToReplace = index
                                            pickReplacementImageLauncher.launch(
                                                PickVisualMediaRequest(
                                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                                )
                                            )
                                        },
                                        onFocusChange = { index, l, t, r, b ->
                                            viewModel.updateImageCrop(index, l, t, r, b)
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        } ?: run {
                            if (uiState.isRendering) {
                                CircularProgressIndicator(
                                    modifier = Modifier.align(Alignment.Center),
                                    color = Color.White
                                )
                            }
                        }
                    }
                    // Fixed category bar - above bottom nav (like Gallery)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .background(Color.Black)
                    ) {
                        Box(modifier = Modifier.height(1.dp).fillMaxWidth().background(Color(0xFF333333)))
                        when (selectedPanelCategory) {
                            CollagePanelCategory.TEMPLATES -> {
                                if (uiState.availableTemplates.isNotEmpty()) {
                                    TemplatesHorizontalBar(
                                        templates = uiState.availableTemplates,
                                        selectedTemplate = uiState.config?.template,
                                        onTemplateSelected = { viewModel.changeTemplate(it) },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                            CollagePanelCategory.BORDERS -> BorderHorizontalBar(
                                borderWidth = uiState.config?.borderWidth ?: 0f,
                                borderColor = uiState.config?.borderColor ?: android.graphics.Color.WHITE,
                                onBorderWidthChange = { viewModel.setBorderWidth(it) },
                                onBorderColorChange = { viewModel.setBorderColor(it) },
                                modifier = Modifier.fillMaxSize()
                            )
                            CollagePanelCategory.NONE -> {}
                        }
                    }
                    Box(modifier = Modifier.height(1.dp).fillMaxWidth().background(Color(0xFF333333)))
                    CollageBottomNavigationBar(
                        selectedCategory = selectedPanelCategory,
                        onCategorySelected = { selectedPanelCategory = it }
                    )
                }
            }
        } else {
            // Landscape: side-by-side layout
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(Color.Black)
                            .onSizeChanged { size ->
                                if (size.width > 0 && size.height > 0) {
                                    viewModel.setPreviewSize(size.width, size.height)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        uiState.previewBitmap?.let { bitmap ->
                            uiState.config?.let { config ->
                                CollagePreview(
                                    previewBitmap = bitmap,
                                    config = config,
                                    onSwapImages = { index1, index2 ->
                                        viewModel.swapImages(index1, index2)
                                    },
                                    onReplaceImage = { index ->
                                        imageIndexToReplace = index
                                        pickReplacementImageLauncher.launch(
                                            PickVisualMediaRequest(
                                                ActivityResultContracts.PickVisualMedia.ImageOnly
                                            )
                                        )
                                    },
                                    onFocusChange = { index, l, t, r, b ->
                                        viewModel.updateImageCrop(index, l, t, r, b)
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        } ?: run {
                            if (uiState.isRendering) {
                                CircularProgressIndicator(
                                    modifier = Modifier.align(Alignment.Center),
                                    color = Color.White
                                )
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(Color(0xFF333333))
                    )
                    CompositionLocalProvider(LocalLayoutDirection provides panelLayoutDirection) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(Color.Black)
                        ) {
                            CollageTopBar(
                                title = when (selectedPanelCategory) {
                                    CollagePanelCategory.TEMPLATES -> stringResource(R.string.templates)
                                    CollagePanelCategory.BORDERS -> stringResource(R.string.border)
                                    CollagePanelCategory.NONE -> ""
                                },
                                onBackClick = onBackClick,
                                onSaveClick = {
                                    scope.launch {
                                        val bitmap = viewModel.renderFinalCollage()
                                        bitmap?.let {
                                            val saved = ImageUtils.saveBitmapToGallery(context, it)
                                            if (saved) {
                                                ImageUtils.showToast(
                                                    context,
                                                    context.getString(R.string.image_saved_successfully)
                                                )
                                                onSaveComplete()
                                            } else {
                                                ImageUtils.showToast(
                                                    context,
                                                    context.getString(R.string.failed_to_save_image)
                                                )
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .background(Color.Black)
                            ) {
                                when (selectedPanelCategory) {
                                    CollagePanelCategory.TEMPLATES -> {
                                        if (uiState.availableTemplates.isNotEmpty()) {
                                            TemplateCarousel(
                                                templates = uiState.availableTemplates,
                                                selectedTemplate = uiState.config?.template,
                                                onTemplateSelected = { viewModel.changeTemplate(it) },
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }
                                    CollagePanelCategory.BORDERS -> BorderPanel(
                                        borderWidth = uiState.config?.borderWidth ?: 0f,
                                        borderColor = uiState.config?.borderColor ?: android.graphics.Color.WHITE,
                                        onBorderWidthChange = { viewModel.setBorderWidth(it) },
                                        onBorderColorChange = { viewModel.setBorderColor(it) },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    CollagePanelCategory.NONE -> {}
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(Color(0xFF333333))
                            )
                            CollageBottomNavigationBar(
                                selectedCategory = selectedPanelCategory,
                                onCategorySelected = { selectedPanelCategory = it }
                            )
                        }
                    }
                }
            }
        }
    }
}

