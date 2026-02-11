package com.example.photoeditor

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.FileProvider
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.photoeditor.R
import com.example.photoeditor.ui.CollageScreen
import com.example.photoeditor.ui.CollageEditorScreen
import com.example.photoeditor.ui.EditorScreen
import com.example.photoeditor.ui.PhotoPreviewScreen
import com.example.photoeditor.ui.SettingsScreen
import com.example.photoeditor.ui.WelcomeScreen
import com.example.photoeditor.ui.theme.PhotoEditorTheme
import com.example.photoeditor.utils.ImageUtils
import com.example.photoeditor.utils.LocaleHelper
import com.example.photoeditor.utils.rememberPermissionState
import com.example.photoeditor.viewmodel.EditorViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

/**
 * Saver for Uri to preserve it across configuration changes
 */
val UriSaver = androidx.compose.runtime.saveable.Saver<Uri?, String>(
    save = { uri -> uri?.toString() ?: "" },
    restore = { value -> if (value.isEmpty()) null else Uri.parse(value) }
)

class MainActivity : ComponentActivity() {
    
    /**
     * Saver for Screen enum to preserve it across configuration changes
     */
    private val screenSaver = androidx.compose.runtime.saveable.Saver<Screen, String>(
        save = { screen -> screen.name },
        restore = { value -> 
            try {
                Screen.valueOf(value)
            } catch (e: Exception) {
                Screen.WELCOME
            }
        }
    )
    
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrapContext(newBase))
    }
    
    @Composable
    private fun AppContent(onLanguageChange: (String) -> Unit) {
        PhotoEditorTheme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                // Request permissions on first launch
                val hasPermissions = rememberPermissionState()
                
                // Screen state: WELCOME, EDITOR, SETTINGS
                // Use rememberSaveable to preserve state on configuration changes (screen rotation)
                var currentScreen by rememberSaveable(stateSaver = screenSaver) { 
                    mutableStateOf<Screen>(Screen.WELCOME) 
                }
                
                // Initialize ViewModel
                val viewModel: EditorViewModel = viewModel()
                
                // Remember the launcher in the composable
                // Use rememberSaveable to preserve URI on configuration changes
                var selectedImageUri by rememberSaveable(stateSaver = UriSaver) { 
                    mutableStateOf<Uri?>(null) 
                }
                var selectedCollageImages by rememberSaveable { 
                    mutableStateOf<List<Uri>>(emptyList()) 
                }
                val context = LocalContext.current
                val scope = rememberCoroutineScope()

                fun logPickedUri(event: String, uri: Uri?) {
                    val tag = "GalleryFlow"
                    if (uri == null) {
                        Log.d(tag, "$event: uri=null")
                        return
                    }

                    Log.d(tag, "$event: uri=$uri scheme=${uri.scheme} authority=${uri.authority} path=${uri.path} last=${uri.lastPathSegment}")

                    scope.launch(Dispatchers.IO) {
                        val type = runCatching { context.contentResolver.getType(uri) }.getOrNull()
                        Log.d(tag, "$event: mimeType=$type")

                        runCatching {
                            context.contentResolver.query(
                                uri,
                                arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                                null,
                                null,
                                null
                            )?.use { cursor ->
                                if (cursor.moveToFirst()) {
                                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                                    val name = if (nameIdx >= 0) cursor.getString(nameIdx) else null
                                    val size = if (sizeIdx >= 0) cursor.getLong(sizeIdx) else null
                                    Log.d(tag, "$event: displayName=$name sizeBytes=$size")
                                } else {
                                    Log.d(tag, "$event: query returned 0 rows")
                                }
                            }
                        }.onFailure {
                            Log.w(tag, "$event: query failed: ${it.message}")
                        }

                        // Quick "can we open" check to spot permission/URI issues
                        val canOpen = runCatching {
                            context.contentResolver.openInputStream(uri)?.use { true } ?: false
                        }.getOrElse {
                            Log.w(tag, "$event: openInputStream failed: ${it.message}")
                            false
                        }
                        Log.d(tag, "$event: canOpenInputStream=$canOpen")
                    }
                }
                
                // Camera photo file
                var cameraPhotoFilePath by rememberSaveable { mutableStateOf<String?>(null) }
                var cameraPhotoUri by rememberSaveable(stateSaver = UriSaver) { mutableStateOf<Uri?>(null) }
                var previewPhotoUri by rememberSaveable(stateSaver = UriSaver) { mutableStateOf<Uri?>(null) }
                
                // Flag to trigger save to gallery after photo is taken
                var shouldSaveToGallery by remember { mutableStateOf<Uri?>(null) }
                var isSavingCapturedPhotoToGallery by remember { mutableStateOf(false) }
                var returnToWelcomeAfterSave by remember { mutableStateOf(false) }

                fun cleanupCameraTemp(deleteFile: Boolean) {
                    val path = cameraPhotoFilePath
                    if (deleteFile && !path.isNullOrBlank()) {
                        runCatching { File(path).delete() }
                    }
                    cameraPhotoFilePath = null
                    cameraPhotoUri = null
                    previewPhotoUri = null
                    shouldSaveToGallery = null
                    isSavingCapturedPhotoToGallery = false
                    returnToWelcomeAfterSave = false
                }
                
                // Gallery launcher
                val pickImageLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.PickVisualMedia()
                ) { uri: Uri? ->
                    logPickedUri("pickerResult", uri)
                    uri?.let {
                        // If user switches to gallery, clear any pending camera temp state
                        cleanupCameraTemp(deleteFile = true)
                        selectedImageUri = it
                        Log.d("GalleryFlow", "pickerResult: calling viewModel.loadImage() then navigating to EDITOR")
                        viewModel.loadImage(it)
                        currentScreen = Screen.EDITOR
                    } ?: run {
                        Log.d("GalleryFlow", "pickerResult: user cancelled picker")
                    }
                }
                
                // Function to open camera (defined as a lambda that will be set after launcher is created)
                var openCamera: (() -> Unit)? = null
                
                // Camera launcher
                val takePictureLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.TakePicture()
                ) { success ->
                    Log.d("CameraFlow", "=== CAMERA RESULT CALLBACK ===")
                    Log.d("CameraFlow", "Success: $success")
                    
                    val photoFile = cameraPhotoFilePath?.let { File(it) }
                    if (success && photoFile != null && cameraPhotoUri != null) {
                        Log.d("CameraFlow", "Photo taken successfully - showing preview")
                        
                        if (photoFile!!.exists()) {
                            Log.d("CameraFlow", "File exists! Going to preview screen")
                            previewPhotoUri = cameraPhotoUri
                            currentScreen = Screen.PHOTO_PREVIEW
                        } else {
                            Log.e("CameraFlow", "ERROR: File does not exist")
                            cleanupCameraTemp(deleteFile = true)
                        }
                    } else {
                        Log.d("CameraFlow", "Camera cancelled/failed - returning to welcome")
                        cleanupCameraTemp(deleteFile = true)
                        currentScreen = Screen.WELCOME
                    }
                }
                
                // Now define openCamera function after launcher is created
                openCamera = {
                    try {
                        // Clear any previous temp capture first
                        cleanupCameraTemp(deleteFile = true)

                        // Create temporary file for camera photo
                        val filesDir = context.getExternalFilesDir(null)
                            ?: context.filesDir // Fallback to internal files dir
                        val file = File(filesDir, "photo_${System.currentTimeMillis()}.jpg")
                        
                        // Ensure parent directory exists
                        file.parentFile?.mkdirs()
                        
                        cameraPhotoFilePath = file.absolutePath
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )
                        cameraPhotoUri = uri
                        takePictureLauncher.launch(uri)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        // Reset on error
                        cleanupCameraTemp(deleteFile = true)
                    }
                }
                
                // Save to gallery when flag is set (from preview screen)
                LaunchedEffect(shouldSaveToGallery) {
                    shouldSaveToGallery?.let { uri ->
                        Log.d("CameraFlow", "LaunchedEffect triggered - saving to gallery")
                        isSavingCapturedPhotoToGallery = true
                        
                        // Load bitmap from URI
                        val bitmap = withContext(Dispatchers.IO) {
                            ImageUtils.loadBitmapFromUri(
                                context,
                                uri,
                                maxWidth = 0, // Load full size for saving
                                maxHeight = 0
                            )
                        }
                        
                        if (bitmap != null) {
                            Log.d("CameraFlow", "Bitmap loaded, saving to gallery")
                            // Save to gallery
                            val saved = withContext(Dispatchers.IO) {
                                ImageUtils.saveBitmapToGallery(context, bitmap)
                            }
                            
                            if (saved) {
                                Log.d("CameraFlow", "Image saved to gallery successfully")
                                ImageUtils.showToast(
                                    context,
                                    context.getString(R.string.image_saved_successfully)
                                )

                                if (returnToWelcomeAfterSave) {
                                    cleanupCameraTemp(deleteFile = true)
                                    currentScreen = Screen.WELCOME
                                }
                            } else {
                                Log.e("CameraFlow", "Failed to save image to gallery")
                                ImageUtils.showToast(
                                    context,
                                    context.getString(R.string.failed_to_save_image)
                                )
                            }
                            
                            shouldSaveToGallery = null // Reset flag
                            isSavingCapturedPhotoToGallery = false
                            returnToWelcomeAfterSave = false
                        } else {
                            Log.e("CameraFlow", "ERROR: Failed to load bitmap from URI")
                            shouldSaveToGallery = null // Reset flag
                            isSavingCapturedPhotoToGallery = false
                            returnToWelcomeAfterSave = false
                            ImageUtils.showToast(
                                context,
                                context.getString(R.string.failed_to_save_image)
                            )
                        }
                    }
                }
                
                if (!hasPermissions) {
                    // Show message while requesting permissions
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(stringResource(R.string.requesting_permissions))
                    }
                } else {
                    when (currentScreen) {
                        Screen.WELCOME -> {
                        WelcomeScreen(
                            onCameraClick = {
                                openCamera?.invoke()
                            },
                            onGalleryClick = {
                                cleanupCameraTemp(deleteFile = true)
                                selectedImageUri = null
                                Log.d("GalleryFlow", "welcomeGalleryClick: launching picker")
                                pickImageLauncher.launch(
                                    PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            },
                            onCollageClick = {
                                currentScreen = Screen.COLLAGE
                            },
                            onSettingsClick = {
                                currentScreen = Screen.SETTINGS
                            }
                        )
                    }
                        Screen.EDITOR -> {
                            EditorScreen(
                                viewModel = viewModel,
                                onSelectImage = {
                                    // If user picks a different image, any pending camera temp can be discarded
                                    cleanupCameraTemp(deleteFile = true)
                                    selectedImageUri = null
                                    Log.d("GalleryFlow", "editorSelectImage: launching picker")
                                    pickImageLauncher.launch(
                                        PickVisualMediaRequest(
                                            ActivityResultContracts.PickVisualMedia.ImageOnly
                                        )
                                    )
                                },
                                onLanguageChange = onLanguageChange,
                                onBackClick = {
                                    currentScreen = Screen.WELCOME
                                    viewModel.clearImage()
                                    selectedImageUri = null
                                    // If we were editing a camera temp file, clean it up
                                    cleanupCameraTemp(deleteFile = true)
                                },
                                onSaveComplete = {
                                    // After saving image, go back to welcome screen
                                    viewModel.clearImage()
                                    selectedImageUri = null
                                    // Remove any temporary camera capture after successful save
                                    cleanupCameraTemp(deleteFile = true)
                                    currentScreen = Screen.WELCOME
                                }
                            )
                        }
                        Screen.PHOTO_PREVIEW -> {
                            val uri = previewPhotoUri
                            if (uri == null) {
                                // Safety fallback
                                currentScreen = Screen.WELCOME
                            } else {
                                PhotoPreviewScreen(
                                    imageUri = uri,
                                    isSaving = isSavingCapturedPhotoToGallery,
                                    onSaveToGallery = {
                                        if (!isSavingCapturedPhotoToGallery) {
                                            returnToWelcomeAfterSave = true
                                            shouldSaveToGallery = uri
                                        }
                                    },
                                    onCancel = {
                                        if (!isSavingCapturedPhotoToGallery) {
                                            cleanupCameraTemp(deleteFile = true)
                                            currentScreen = Screen.WELCOME
                                        }
                                    },
                                    onEdit = {
                                        if (!isSavingCapturedPhotoToGallery) {
                                            previewPhotoUri = null
                                            selectedImageUri = uri
                                            viewModel.loadImage(uri)
                                            currentScreen = Screen.EDITOR
                                        }
                                    }
                                )
                            }
                        }
                        Screen.SETTINGS -> {
                            SettingsScreen(
                                onBackClick = {
                                    currentScreen = Screen.WELCOME
                                },
                                onLanguageChange = { languageCode ->
                                    onLanguageChange(languageCode)
                                }
                            )
                        }
                        Screen.COLLAGE -> {
                            CollageScreen(
                                onBackClick = {
                                    currentScreen = Screen.WELCOME
                                },
                                onEditCollage = { uris ->
                                    selectedCollageImages = uris
                                    currentScreen = Screen.COLLAGE_EDITOR
                                }
                            )
                        }
                        Screen.COLLAGE_EDITOR -> {
                            CollageEditorScreen(
                                selectedImages = selectedCollageImages,
                                onBackClick = {
                                    currentScreen = Screen.COLLAGE
                                },
                                onSaveComplete = {
                                    selectedCollageImages = emptyList()
                                    currentScreen = Screen.WELCOME
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    
    private enum class Screen {
        WELCOME, EDITOR, PHOTO_PREVIEW, SETTINGS, COLLAGE, COLLAGE_EDITOR
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocaleHelper.wrapContext(this)
        
        // Clear "en" preference if system language is Hebrew
        val savedLang = LocaleHelper.getSavedLanguage(this)
        if (savedLang == "en") {
            val systemLang = LocaleHelper.getSystemLanguage()
            if (systemLang == "he") {
                LocaleHelper.clearLocale(this)
                recreate()
                return
            }
        }
        
        // Log locale information
        val systemLocale = Locale.getDefault()
        val appLanguage = LocaleHelper.getAppLanguage(this)
        val savedLanguage = LocaleHelper.getSavedLanguage(this)
        val configLocale = resources.configuration.locales[0]
        
        Log.d("LocaleInfo", "=== LOCALE DEBUG INFO ===")
        Log.d("LocaleInfo", "System Language: ${systemLocale.language}-${systemLocale.country}")
        Log.d("LocaleInfo", "System Locale: $systemLocale")
        Log.d("LocaleInfo", "Saved Language Preference: $savedLanguage")
        Log.d("LocaleInfo", "App Language (selected): $appLanguage")
        Log.d("LocaleInfo", "App Configuration Locale: ${configLocale.language}-${configLocale.country}")
        Log.d("LocaleInfo", "App Name Resource: ${getString(R.string.app_name)}")
        Log.d("LocaleInfo", "=========================")
        
        enableEdgeToEdge()

        // Hide navigation bar by default; user can swipe up from bottom to show it
        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        insetsController.hide(WindowInsetsCompat.Type.navigationBars())
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        setContent {
            AppContent(
                onLanguageChange = { languageCode ->
                    LocaleHelper.setLocale(this@MainActivity, languageCode)
                    // Restart activity to apply new locale
                    recreate()
                }
            )
        }
    }
}
