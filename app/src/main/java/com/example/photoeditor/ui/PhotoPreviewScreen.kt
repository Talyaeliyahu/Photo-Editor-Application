package com.example.photoeditor.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.photoeditor.R
import com.example.photoeditor.utils.ImageUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Photo preview screen - shown after taking a photo
 * Shows preview with 3 options: Save to gallery, Cancel, Edit
 */
@Composable
fun PhotoPreviewScreen(
    imageUri: android.net.Uri,
    isSaving: Boolean = false,
    onSaveToGallery: () -> Unit,
    onCancel: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var previewBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    // Load preview image
    LaunchedEffect(imageUri) {
        isLoading = true
        previewBitmap = withContext(Dispatchers.IO) {
            ImageUtils.loadBitmapFromUri(context, imageUri, maxWidth = 1024, maxHeight = 1024)
        }
        isLoading = false
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.White
            )
        } else if (previewBitmap != null) {
            // Preview image
            Image(
                bitmap = previewBitmap!!.asImageBitmap(),
                contentDescription = stringResource(R.string.preview_image),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )

            // Action buttons at the bottom - styled to match the app (white with primary)
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cancel button (X)
                FloatingActionButton(
                    onClick = { if (!isSaving) onCancel() },
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    containerColor = Color.White,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.cancel)
                    )
                }

                // Edit button (pencil) - edit without saving
                FloatingActionButton(
                    onClick = { if (!isSaving) onEdit() },
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    containerColor = Color.White,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.edit)
                    )
                }

                // Save to gallery button (checkmark) - save to gallery
                FloatingActionButton(
                    onClick = { if (!isSaving) onSaveToGallery() },
                    modifier = Modifier.size(56.dp),
                    shape = CircleShape,
                    containerColor = Color.White,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = stringResource(R.string.save)
                        )
                    }
                }
            }
        } else {
            // Error state
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.failed_to_load_image),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
                Button(
                    onClick = onCancel,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    }
}
