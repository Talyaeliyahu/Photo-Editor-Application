package com.example.photoeditor.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Utility class for handling runtime permissions.
 */
object PermissionUtils {
    
    /**
     * Checks if all required permissions are granted.
     */
    fun hasAllPermissions(context: Context): Boolean {
        return hasGalleryPermission(context) && hasCameraPermission(context) && hasWritePermissionIfNeeded(context)
    }
    
    /**
     * Checks if gallery permission is granted.
     */
    fun hasGalleryPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Checks if camera permission is granted.
     */
    fun hasCameraPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Gets the list of permissions that need to be requested.
     */
    fun getRequiredPermissions(): Array<String> {
        val permissions = mutableListOf<String>()
        
        // Camera permission
        permissions.add(Manifest.permission.CAMERA)
        
        // Gallery permission based on Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        // Write permission only needed on Android 9 and below for saving to gallery
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        
        return permissions.toTypedArray()
    }

    /**
     * Checks if write permission is required and granted.
     * On Android 10+ (Q) we use scoped storage via MediaStore and don't need it.
     */
    fun hasWritePermissionIfNeeded(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}

/**
 * Composable that requests permissions on first launch.
 * Returns true if all permissions are granted.
 */
@Composable
fun rememberPermissionState(): Boolean {
    val context = LocalContext.current
    var hasPermissions by remember { mutableStateOf(PermissionUtils.hasAllPermissions(context)) }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasPermissions = permissions.values.all { it }
    }
    
    LaunchedEffect(Unit) {
        if (!hasPermissions) {
            permissionLauncher.launch(PermissionUtils.getRequiredPermissions())
        }
    }
    
    return hasPermissions
}
