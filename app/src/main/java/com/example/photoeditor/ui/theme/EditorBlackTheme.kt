package com.example.photoeditor.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Local "all black" theme for the editor screen (preview).
 * Keeps typography/shapes from the app theme, but forces a black-first color scheme.
 */
private val EditorBlackColorScheme = darkColorScheme(
    background = Color.Black,
    onBackground = Color.White,

    surface = Color.Black,
    onSurface = Color.White,

    surfaceVariant = Color.Black,
    onSurfaceVariant = Color.White,

    // Use white as the main accent so buttons are visible on black.
    primary = Color.White,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF1E1E1E),
    onPrimaryContainer = Color.White,

    secondary = Color(0xFFBDBDBD),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF1A1A1A),
    onSecondaryContainer = Color.White,

    tertiary = Color(0xFFB0BEC5),
    onTertiary = Color.Black,

    error = Color(0xFFFF6B6B),
    onError = Color.Black,
    errorContainer = Color(0xFF3A0E0E),
    onErrorContainer = Color(0xFFFFDAD6),

    outline = Color(0xFF333333)
)

@Composable
fun EditorBlackTheme(content: @Composable () -> Unit) {
    // Preserve typography/shapes from the app theme
    val typography = MaterialTheme.typography
    val shapes = MaterialTheme.shapes

    MaterialTheme(
        colorScheme = EditorBlackColorScheme,
        typography = typography,
        shapes = shapes,
        content = content
    )
}

