package com.example.photoeditor.model

import android.graphics.Color
import android.net.Uri

/**
 * Configuration for a collage
 */
data class CollageConfig(
    val images: List<CollageImage>,
    val template: CollageTemplate,
    val borderWidth: Float = 0f, // in pixels
    val borderColor: Int = Color.WHITE,
    val aspectRatio: Float? = null // null = auto, or width/height ratio
)

/**
 * Represents a single image in the collage with its position and size.
 * Crop defines which part of the image is visible (0-1 normalized); user can adjust via zoom/pan in preview.
 */
data class CollageImage(
    val uri: Uri,
    val x: Float = 0f, // position in template (0-1 normalized)
    val y: Float = 0f,
    val width: Float = 1f, // size in template (0-1 normalized)
    val height: Float = 1f,
    val rotation: Float = 0f, // rotation in degrees
    val cropLeft: Float = 0f,
    val cropTop: Float = 0f,
    val cropRight: Float = 1f,
    val cropBottom: Float = 1f
)

/**
 * Collage template defines the layout structure
 */
data class CollageTemplate(
    val id: String,
    val name: String,
    val imageCount: Int, // number of images this template supports
    val cells: List<Cell> // cells define where each image goes
) {
    data class Cell(
        val x: Float, // position 0-1
        val y: Float,
        val width: Float, // size 0-1
        val height: Float
    )
}

/**
 * Predefined collage templates
 */
object CollageTemplates {
    fun getTemplatesForImageCount(count: Int): List<CollageTemplate> {
        return when (count) {
            1 -> listOf(
                CollageTemplate("1_full", "Full", 1, listOf(
                    CollageTemplate.Cell(0f, 0f, 1f, 1f)
                ))
            )
            2 -> listOf(
                CollageTemplate("2_horizontal", "Horizontal Split", 2, listOf(
                    CollageTemplate.Cell(0f, 0f, 0.5f, 1f),
                    CollageTemplate.Cell(0.5f, 0f, 0.5f, 1f)
                )),
                CollageTemplate("2_vertical", "Vertical Split", 2, listOf(
                    CollageTemplate.Cell(0f, 0f, 1f, 0.5f),
                    CollageTemplate.Cell(0f, 0.5f, 1f, 0.5f)
                ))
            )
            3 -> listOf(
                // Large on left, 2 small on right
                CollageTemplate("3_large_left", "Large Left", 3, listOf(
                    CollageTemplate.Cell(0f, 0f, 0.5f, 1f),
                    CollageTemplate.Cell(0.5f, 0f, 0.5f, 0.5f),
                    CollageTemplate.Cell(0.5f, 0.5f, 0.5f, 0.5f)
                )),
                // Large on right, 2 small on left
                CollageTemplate("3_large_right", "Large Right", 3, listOf(
                    CollageTemplate.Cell(0f, 0f, 0.5f, 0.5f),
                    CollageTemplate.Cell(0f, 0.5f, 0.5f, 0.5f),
                    CollageTemplate.Cell(0.5f, 0f, 0.5f, 1f)
                )),
                // Large on top, 2 small on bottom
                CollageTemplate("3_large_top", "Large Top", 3, listOf(
                    CollageTemplate.Cell(0f, 0f, 1f, 0.5f),
                    CollageTemplate.Cell(0f, 0.5f, 0.5f, 0.5f),
                    CollageTemplate.Cell(0.5f, 0.5f, 0.5f, 0.5f)
                )),
                // Large on bottom, 2 small on top
                CollageTemplate("3_large_bottom", "Large Bottom", 3, listOf(
                    CollageTemplate.Cell(0f, 0f, 0.5f, 0.5f),
                    CollageTemplate.Cell(0.5f, 0f, 0.5f, 0.5f),
                    CollageTemplate.Cell(0f, 0.5f, 1f, 0.5f)
                )),
                CollageTemplate("3_horizontal", "3 Horizontal", 3, listOf(
                    CollageTemplate.Cell(0f, 0f, 0.33f, 1f),
                    CollageTemplate.Cell(0.33f, 0f, 0.34f, 1f),
                    CollageTemplate.Cell(0.67f, 0f, 0.33f, 1f)
                )),
                CollageTemplate("3_vertical", "3 Vertical", 3, listOf(
                    CollageTemplate.Cell(0f, 0f, 1f, 0.33f),
                    CollageTemplate.Cell(0f, 0.33f, 1f, 0.34f),
                    CollageTemplate.Cell(0f, 0.67f, 1f, 0.33f)
                ))
            )
            4 -> listOf(
                CollageTemplate("4_grid", "2x2 Grid", 4, listOf(
                    CollageTemplate.Cell(0f, 0f, 0.5f, 0.5f),
                    CollageTemplate.Cell(0.5f, 0f, 0.5f, 0.5f),
                    CollageTemplate.Cell(0f, 0.5f, 0.5f, 0.5f),
                    CollageTemplate.Cell(0.5f, 0.5f, 0.5f, 0.5f)
                )),
                // Large on left, 3 small on right
                CollageTemplate("4_large_left", "Large Left", 4, listOf(
                    CollageTemplate.Cell(0f, 0f, 0.5f, 1f),
                    CollageTemplate.Cell(0.5f, 0f, 0.5f, 0.33f),
                    CollageTemplate.Cell(0.5f, 0.33f, 0.5f, 0.34f),
                    CollageTemplate.Cell(0.5f, 0.67f, 0.5f, 0.33f)
                )),
                // Large on right, 3 small on left
                CollageTemplate("4_large_right", "Large Right", 4, listOf(
                    CollageTemplate.Cell(0f, 0f, 0.5f, 0.33f),
                    CollageTemplate.Cell(0f, 0.33f, 0.5f, 0.34f),
                    CollageTemplate.Cell(0f, 0.67f, 0.5f, 0.33f),
                    CollageTemplate.Cell(0.5f, 0f, 0.5f, 1f)
                )),
                // Large on top, 3 small on bottom
                CollageTemplate("4_large_top", "Large Top", 4, listOf(
                    CollageTemplate.Cell(0f, 0f, 1f, 0.5f),
                    CollageTemplate.Cell(0f, 0.5f, 0.33f, 0.5f),
                    CollageTemplate.Cell(0.33f, 0.5f, 0.34f, 0.5f),
                    CollageTemplate.Cell(0.67f, 0.5f, 0.33f, 0.5f)
                )),
                // Large on bottom, 3 small on top
                CollageTemplate("4_large_bottom", "Large Bottom", 4, listOf(
                    CollageTemplate.Cell(0f, 0f, 0.33f, 0.5f),
                    CollageTemplate.Cell(0.33f, 0f, 0.34f, 0.5f),
                    CollageTemplate.Cell(0.67f, 0f, 0.33f, 0.5f),
                    CollageTemplate.Cell(0f, 0.5f, 1f, 0.5f)
                )),
                // L-shape: large top-right + 3 small (4 cells)
                CollageTemplate("4_corner_top_right", "Large Top Right", 4, listOf(
                    CollageTemplate.Cell(0f, 0f, 0.33f, 0.67f),     // Top-left tall
                    CollageTemplate.Cell(0.33f, 0f, 0.67f, 0.67f), // Large top-right
                    CollageTemplate.Cell(0f, 0.67f, 0.33f, 0.33f), // Bottom-left small
                    CollageTemplate.Cell(0.33f, 0.67f, 0.67f, 0.33f) // Bottom-right wide
                )),
                // Large top-left
                CollageTemplate("4_corner_top_left", "Large Top Left", 4, listOf(
                    CollageTemplate.Cell(0f, 0f, 0.67f, 0.67f),    // Large top-left
                    CollageTemplate.Cell(0.67f, 0f, 0.33f, 0.67f), // Top-right tall
                    CollageTemplate.Cell(0f, 0.67f, 0.67f, 0.33f), // Bottom-left wide
                    CollageTemplate.Cell(0.67f, 0.67f, 0.33f, 0.33f) // Bottom-right small
                )),
                // Large bottom-right: top = wide left + small right; bottom = tall left + large right
                CollageTemplate("4_corner_bottom_right", "Large Bottom Right", 4, listOf(
                    CollageTemplate.Cell(0f, 0f, 0.67f, 0.33f),    // Top-left wide
                    CollageTemplate.Cell(0.67f, 0f, 0.33f, 0.33f),  // Top-right small
                    CollageTemplate.Cell(0f, 0.33f, 0.33f, 0.67f),  // Bottom-left tall
                    CollageTemplate.Cell(0.33f, 0.33f, 0.67f, 0.67f) // Large bottom-right
                )),
                // Large bottom-left: top = small left + wide right; bottom = large left + small right
                CollageTemplate("4_corner_bottom_left", "Large Bottom Left", 4, listOf(
                    CollageTemplate.Cell(0f, 0f, 0.33f, 0.33f),    // Top-left small
                    CollageTemplate.Cell(0.33f, 0f, 0.67f, 0.33f), // Top-right wide
                    CollageTemplate.Cell(0f, 0.33f, 0.67f, 0.67f),  // Bottom-left: large rectangle
                    CollageTemplate.Cell(0.67f, 0.33f, 0.33f, 0.67f) // Bottom-right: small square
                ))
            )
            5 -> listOf(
                // Image layout: top = 1 medium left + 2 stacked right; bottom = 2 large
                CollageTemplate("5_top_1left_2right_2bottom", "Top 1+2, Bottom 2", 5, listOf(
                    CollageTemplate.Cell(0f, 0f, 0.33f, 0.5f),      // Top-left medium
                    CollageTemplate.Cell(0.33f, 0f, 0.67f, 0.25f), // Top-right top small
                    CollageTemplate.Cell(0.33f, 0.25f, 0.67f, 0.25f), // Top-right bottom small
                    CollageTemplate.Cell(0f, 0.5f, 0.5f, 0.5f),    // Bottom left large
                    CollageTemplate.Cell(0.5f, 0.5f, 0.5f, 0.5f)   // Bottom right large
                )),
                // 2 on top, 3 on bottom
                CollageTemplate("5_2top_3bottom", "2 Top, 3 Bottom", 5, listOf(
                    CollageTemplate.Cell(0f, 0f, 0.5f, 0.5f),
                    CollageTemplate.Cell(0.5f, 0f, 0.5f, 0.5f),
                    CollageTemplate.Cell(0f, 0.5f, 0.33f, 0.5f),
                    CollageTemplate.Cell(0.33f, 0.5f, 0.34f, 0.5f),
                    CollageTemplate.Cell(0.67f, 0.5f, 0.33f, 0.5f)
                )),
                // 3 on top, 2 on bottom
                CollageTemplate("5_3top_2bottom", "3 Top, 2 Bottom", 5, listOf(
                    CollageTemplate.Cell(0f, 0f, 0.33f, 0.5f),
                    CollageTemplate.Cell(0.33f, 0f, 0.34f, 0.5f),
                    CollageTemplate.Cell(0.67f, 0f, 0.33f, 0.5f),
                    CollageTemplate.Cell(0f, 0.5f, 0.5f, 0.5f),
                    CollageTemplate.Cell(0.5f, 0.5f, 0.5f, 0.5f)
                )),
                // Large left, 4 small on right (2x2)
                CollageTemplate("5_large_left", "Large Left", 5, listOf(
                    CollageTemplate.Cell(0f, 0f, 0.5f, 1f),
                    CollageTemplate.Cell(0.5f, 0f, 0.25f, 0.5f),
                    CollageTemplate.Cell(0.75f, 0f, 0.25f, 0.5f),
                    CollageTemplate.Cell(0.5f, 0.5f, 0.25f, 0.5f),
                    CollageTemplate.Cell(0.75f, 0.5f, 0.25f, 0.5f)
                )),
                // Large right, 4 small on left (2x2)
                CollageTemplate("5_large_right", "Large Right", 5, listOf(
                    CollageTemplate.Cell(0f, 0f, 0.25f, 0.5f),
                    CollageTemplate.Cell(0.25f, 0f, 0.25f, 0.5f),
                    CollageTemplate.Cell(0f, 0.5f, 0.25f, 0.5f),
                    CollageTemplate.Cell(0.25f, 0.5f, 0.25f, 0.5f),
                    CollageTemplate.Cell(0.5f, 0f, 0.5f, 1f)
                )),
                // Pyramid: 1 top, 2 middle, 2 bottom
                CollageTemplate("5_pyramid", "Pyramid", 5, listOf(
                    CollageTemplate.Cell(0f, 0f, 1f, 0.33f),
                    CollageTemplate.Cell(0f, 0.33f, 0.5f, 0.34f),
                    CollageTemplate.Cell(0.5f, 0.33f, 0.5f, 0.34f),
                    CollageTemplate.Cell(0f, 0.67f, 0.5f, 0.33f),
                    CollageTemplate.Cell(0.5f, 0.67f, 0.5f, 0.33f)
                )),
                // Inverted pyramid: 2 top, 2 middle, 1 bottom
                CollageTemplate("5_inverted_pyramid", "Inverted Pyramid", 5, listOf(
                    CollageTemplate.Cell(0f, 0f, 0.5f, 0.33f),
                    CollageTemplate.Cell(0.5f, 0f, 0.5f, 0.33f),
                    CollageTemplate.Cell(0f, 0.33f, 0.5f, 0.34f),
                    CollageTemplate.Cell(0.5f, 0.33f, 0.5f, 0.34f),
                    CollageTemplate.Cell(0f, 0.67f, 1f, 0.33f)
                )),
                // 5 horizontal strips
                CollageTemplate("5_horizontal", "5 Rows", 5, listOf(
                    CollageTemplate.Cell(0f, 0f, 1f, 0.2f),
                    CollageTemplate.Cell(0f, 0.2f, 1f, 0.2f),
                    CollageTemplate.Cell(0f, 0.4f, 1f, 0.2f),
                    CollageTemplate.Cell(0f, 0.6f, 1f, 0.2f),
                    CollageTemplate.Cell(0f, 0.8f, 1f, 0.2f)
                )),
                // Tips layout (טיפים) - tall left, large top-right, 3 on bottom - and all 4 orientations
                CollageTemplate("5_tips_left", "טיפים שמאל", 5, listOf(
                    CollageTemplate.Cell(0f, 0f, 0.33f, 1f),       // Left tall column
                    CollageTemplate.Cell(0.33f, 0f, 0.67f, 0.5f), // Top-right large
                    CollageTemplate.Cell(0f, 0.5f, 0.33f, 0.5f),  // Bottom left
                    CollageTemplate.Cell(0.33f, 0.5f, 0.34f, 0.5f), // Bottom middle
                    CollageTemplate.Cell(0.67f, 0.5f, 0.33f, 0.5f)  // Bottom right
                )),
                CollageTemplate("5_tips_right", "טיפים ימין", 5, listOf(
                    CollageTemplate.Cell(0.67f, 0f, 0.33f, 1f),   // Right tall column
                    CollageTemplate.Cell(0f, 0f, 0.67f, 0.5f),    // Top-left large
                    CollageTemplate.Cell(0.67f, 0.5f, 0.33f, 0.5f), // Bottom right
                    CollageTemplate.Cell(0.33f, 0.5f, 0.34f, 0.5f), // Bottom middle
                    CollageTemplate.Cell(0f, 0.5f, 0.33f, 0.5f)   // Bottom left
                )),
                CollageTemplate("5_tips_top", "טיפים למעלה", 5, listOf(
                    CollageTemplate.Cell(0f, 0f, 1f, 0.33f),       // Top tall row
                    CollageTemplate.Cell(0.33f, 0.33f, 0.67f, 0.67f), // Bottom-right large
                    CollageTemplate.Cell(0f, 0.33f, 0.33f, 0.22f), // Left column top
                    CollageTemplate.Cell(0f, 0.55f, 0.33f, 0.22f), // Left column middle
                    CollageTemplate.Cell(0f, 0.77f, 0.33f, 0.23f)  // Left column bottom
                )),
                CollageTemplate("5_tips_bottom", "טיפים למטה", 5, listOf(
                    CollageTemplate.Cell(0f, 0.67f, 1f, 0.33f),    // Bottom tall row
                    CollageTemplate.Cell(0f, 0f, 0.67f, 0.67f),    // Top-left large
                    CollageTemplate.Cell(0.67f, 0f, 0.33f, 0.22f), // Right column top
                    CollageTemplate.Cell(0.67f, 0.22f, 0.33f, 0.22f), // Right column middle
                    CollageTemplate.Cell(0.67f, 0.44f, 0.33f, 0.23f)  // Right column bottom
                ))
            )
            6 -> listOf(
                CollageTemplate("6_grid", "3x2 Grid", 6, listOf(
                    CollageTemplate.Cell(0f, 0f, 0.33f, 0.5f),
                    CollageTemplate.Cell(0.33f, 0f, 0.34f, 0.5f),
                    CollageTemplate.Cell(0.67f, 0f, 0.33f, 0.5f),
                    CollageTemplate.Cell(0f, 0.5f, 0.33f, 0.5f),
                    CollageTemplate.Cell(0.33f, 0.5f, 0.34f, 0.5f),
                    CollageTemplate.Cell(0.67f, 0.5f, 0.33f, 0.5f)
                )),
                // 2x3 vertical grid (2 columns, 3 rows)
                CollageTemplate("6_grid_vertical", "2x3 Vertical", 6, listOf(
                    CollageTemplate.Cell(0f, 0f, 0.5f, 0.33f),
                    CollageTemplate.Cell(0.5f, 0f, 0.5f, 0.33f),
                    CollageTemplate.Cell(0f, 0.33f, 0.5f, 0.34f),
                    CollageTemplate.Cell(0.5f, 0.33f, 0.5f, 0.34f),
                    CollageTemplate.Cell(0f, 0.67f, 0.5f, 0.33f),
                    CollageTemplate.Cell(0.5f, 0.67f, 0.5f, 0.33f)
                )),
                // Large image top-right, 3 small left, 2 small bottom
                CollageTemplate("6_large_top_right", "Large Top Right", 6, listOf(
                    CollageTemplate.Cell(0f, 0f, 0.33f, 0.33f),      // Top left small
                    CollageTemplate.Cell(0f, 0.33f, 0.33f, 0.34f), // Middle left small
                    CollageTemplate.Cell(0f, 0.67f, 0.33f, 0.33f), // Bottom left small
                    CollageTemplate.Cell(0.33f, 0f, 0.67f, 0.67f), // Large top-right
                    CollageTemplate.Cell(0.33f, 0.67f, 0.33f, 0.33f), // Bottom middle small
                    CollageTemplate.Cell(0.67f, 0.67f, 0.33f, 0.33f)  // Bottom right small
                )),
                // Large image top-left, 3 small right, 2 small bottom
                CollageTemplate("6_large_top_left", "Large Top Left", 6, listOf(
                    CollageTemplate.Cell(0f, 0f, 0.67f, 0.67f),    // Large top-left
                    CollageTemplate.Cell(0.67f, 0f, 0.33f, 0.33f), // Top right small
                    CollageTemplate.Cell(0.67f, 0.33f, 0.33f, 0.34f), // Middle right small
                    CollageTemplate.Cell(0.67f, 0.67f, 0.33f, 0.33f), // Bottom right small
                    CollageTemplate.Cell(0f, 0.67f, 0.33f, 0.33f), // Bottom left small
                    CollageTemplate.Cell(0.33f, 0.67f, 0.34f, 0.33f)  // Bottom middle small
                )),
                // Large image bottom-right, 3 small left, 2 small top
                CollageTemplate("6_large_bottom_right", "Large Bottom Right", 6, listOf(
                    CollageTemplate.Cell(0f, 0f, 0.33f, 0.33f),      // Top left small
                    CollageTemplate.Cell(0.33f, 0f, 0.34f, 0.33f),  // Top middle small
                    CollageTemplate.Cell(0.67f, 0f, 0.33f, 0.33f),  // Top right small
                    CollageTemplate.Cell(0f, 0.33f, 0.33f, 0.34f),  // Middle left small
                    CollageTemplate.Cell(0f, 0.67f, 0.33f, 0.33f),  // Bottom left small
                    CollageTemplate.Cell(0.33f, 0.33f, 0.67f, 0.67f) // Large bottom-right
                )),
                // Large image bottom-left, 3 small right, 2 small top
                CollageTemplate("6_large_bottom_left", "Large Bottom Left", 6, listOf(
                    CollageTemplate.Cell(0f, 0f, 0.33f, 0.33f),      // Top left small
                    CollageTemplate.Cell(0.33f, 0f, 0.34f, 0.33f),  // Top middle small
                    CollageTemplate.Cell(0.67f, 0f, 0.33f, 0.33f),  // Top right small
                    CollageTemplate.Cell(0.67f, 0.33f, 0.33f, 0.34f), // Middle right small
                    CollageTemplate.Cell(0.67f, 0.67f, 0.33f, 0.33f), // Bottom right small
                    CollageTemplate.Cell(0f, 0.33f, 0.67f, 0.67f)    // Large bottom-left
                ))
            )
            7 -> listOf(
                // 1 square (quarter) + 6 rectangles (3/4) - square top-left
                CollageTemplate("7_grid", "7 Grid", 7, listOf(
                    CollageTemplate.Cell(0f, 0f, 0.5f, 0.5f),
                    CollageTemplate.Cell(0.5f, 0f, 0.25f, 0.5f),
                    CollageTemplate.Cell(0.75f, 0f, 0.25f, 0.5f),
                    CollageTemplate.Cell(0f, 0.5f, 0.25f, 0.5f),
                    CollageTemplate.Cell(0.25f, 0.5f, 0.25f, 0.5f),
                    CollageTemplate.Cell(0.5f, 0.5f, 0.25f, 0.5f),
                    CollageTemplate.Cell(0.75f, 0.5f, 0.25f, 0.5f)
                )),
                // Large top, 6 small in 2x3 grid below
                CollageTemplate("7_large_top", "Large Top", 7, listOf(
                    CollageTemplate.Cell(0f, 0f, 1f, 0.5f),
                    CollageTemplate.Cell(0f, 0.5f, 0.33f, 0.25f),
                    CollageTemplate.Cell(0.33f, 0.5f, 0.34f, 0.25f),
                    CollageTemplate.Cell(0.67f, 0.5f, 0.33f, 0.25f),
                    CollageTemplate.Cell(0f, 0.75f, 0.33f, 0.25f),
                    CollageTemplate.Cell(0.33f, 0.75f, 0.34f, 0.25f),
                    CollageTemplate.Cell(0.67f, 0.75f, 0.33f, 0.25f)
                )),
                // Large bottom, 6 small in 2x3 grid above
                CollageTemplate("7_large_bottom", "Large Bottom", 7, listOf(
                    CollageTemplate.Cell(0f, 0f, 0.33f, 0.25f),
                    CollageTemplate.Cell(0.33f, 0f, 0.34f, 0.25f),
                    CollageTemplate.Cell(0.67f, 0f, 0.33f, 0.25f),
                    CollageTemplate.Cell(0f, 0.25f, 0.33f, 0.25f),
                    CollageTemplate.Cell(0.33f, 0.25f, 0.34f, 0.25f),
                    CollageTemplate.Cell(0.67f, 0.25f, 0.33f, 0.25f),
                    CollageTemplate.Cell(0f, 0.5f, 1f, 0.5f)
                )),
                // Large left, 6 small in 2 columns x 3 rows on right
                CollageTemplate("7_large_left", "Large Left", 7, listOf(
                    CollageTemplate.Cell(0f, 0f, 0.5f, 1f),
                    CollageTemplate.Cell(0.5f, 0f, 0.25f, 0.33f),
                    CollageTemplate.Cell(0.75f, 0f, 0.25f, 0.33f),
                    CollageTemplate.Cell(0.5f, 0.33f, 0.25f, 0.34f),
                    CollageTemplate.Cell(0.75f, 0.33f, 0.25f, 0.34f),
                    CollageTemplate.Cell(0.5f, 0.67f, 0.25f, 0.33f),
                    CollageTemplate.Cell(0.75f, 0.67f, 0.25f, 0.33f)
                )),
                // Large right, 6 small in 2 columns x 3 rows on left
                CollageTemplate("7_large_right", "Large Right", 7, listOf(
                    CollageTemplate.Cell(0f, 0f, 0.25f, 0.33f),
                    CollageTemplate.Cell(0.25f, 0f, 0.25f, 0.33f),
                    CollageTemplate.Cell(0f, 0.33f, 0.25f, 0.34f),
                    CollageTemplate.Cell(0.25f, 0.33f, 0.25f, 0.34f),
                    CollageTemplate.Cell(0f, 0.67f, 0.25f, 0.33f),
                    CollageTemplate.Cell(0.25f, 0.67f, 0.25f, 0.33f),
                    CollageTemplate.Cell(0.5f, 0f, 0.5f, 1f)
                )),
                // Pyramid: 1 top, 2 middle, 4 bottom
                CollageTemplate("7_pyramid", "Pyramid", 7, listOf(
                    CollageTemplate.Cell(0f, 0f, 1f, 0.25f),
                    CollageTemplate.Cell(0f, 0.25f, 0.5f, 0.375f),
                    CollageTemplate.Cell(0.5f, 0.25f, 0.5f, 0.375f),
                    CollageTemplate.Cell(0f, 0.625f, 0.25f, 0.375f),
                    CollageTemplate.Cell(0.25f, 0.625f, 0.25f, 0.375f),
                    CollageTemplate.Cell(0.5f, 0.625f, 0.25f, 0.375f),
                    CollageTemplate.Cell(0.75f, 0.625f, 0.25f, 0.375f)
                )),
                // Inverted pyramid: 4 top, 2 middle, 1 bottom
                CollageTemplate("7_inverted_pyramid", "Inverted Pyramid", 7, listOf(
                    CollageTemplate.Cell(0f, 0f, 0.25f, 0.375f),
                    CollageTemplate.Cell(0.25f, 0f, 0.25f, 0.375f),
                    CollageTemplate.Cell(0.5f, 0f, 0.25f, 0.375f),
                    CollageTemplate.Cell(0.75f, 0f, 0.25f, 0.375f),
                    CollageTemplate.Cell(0f, 0.375f, 0.5f, 0.375f),
                    CollageTemplate.Cell(0.5f, 0.375f, 0.5f, 0.375f),
                    CollageTemplate.Cell(0f, 0.75f, 1f, 0.25f)
                )),
                // Same layout flipped - square top-right, 6 rects in L on left
                CollageTemplate("7_grid_top_right", "7 Grid Top Right", 7, listOf(
                    CollageTemplate.Cell(0.5f, 0f, 0.5f, 0.5f),
                    CollageTemplate.Cell(0f, 0f, 0.25f, 0.5f),
                    CollageTemplate.Cell(0.25f, 0f, 0.25f, 0.5f),
                    CollageTemplate.Cell(0f, 0.5f, 0.25f, 0.5f),
                    CollageTemplate.Cell(0.25f, 0.5f, 0.25f, 0.5f),
                    CollageTemplate.Cell(0.5f, 0.5f, 0.25f, 0.5f),
                    CollageTemplate.Cell(0.75f, 0.5f, 0.25f, 0.5f)
                )),
                // Same layout flipped - square bottom-left, 6 rects in L on top+right
                CollageTemplate("7_grid_bottom_left", "7 Grid Bottom Left", 7, listOf(
                    CollageTemplate.Cell(0f, 0.5f, 0.5f, 0.5f),
                    CollageTemplate.Cell(0f, 0f, 0.25f, 0.5f),
                    CollageTemplate.Cell(0.25f, 0f, 0.25f, 0.5f),
                    CollageTemplate.Cell(0.5f, 0f, 0.25f, 0.5f),
                    CollageTemplate.Cell(0.75f, 0f, 0.25f, 0.5f),
                    CollageTemplate.Cell(0.5f, 0.5f, 0.25f, 0.5f),
                    CollageTemplate.Cell(0.75f, 0.5f, 0.25f, 0.5f)
                )),
                // Same layout flipped - square bottom-right, 6 rects in L on top+left
                CollageTemplate("7_grid_bottom_right", "7 Grid Bottom Right", 7, listOf(
                    CollageTemplate.Cell(0.5f, 0.5f, 0.5f, 0.5f),
                    CollageTemplate.Cell(0f, 0f, 0.25f, 0.5f),
                    CollageTemplate.Cell(0.25f, 0f, 0.25f, 0.5f),
                    CollageTemplate.Cell(0.5f, 0f, 0.25f, 0.5f),
                    CollageTemplate.Cell(0.75f, 0f, 0.25f, 0.5f),
                    CollageTemplate.Cell(0f, 0.5f, 0.25f, 0.5f),
                    CollageTemplate.Cell(0.25f, 0.5f, 0.25f, 0.5f)
                ))
            )
            8 -> listOf(
                CollageTemplate("8_grid", "4x2 Grid", 8, listOf(
                    CollageTemplate.Cell(0f, 0f, 0.25f, 0.5f),
                    CollageTemplate.Cell(0.25f, 0f, 0.25f, 0.5f),
                    CollageTemplate.Cell(0.5f, 0f, 0.25f, 0.5f),
                    CollageTemplate.Cell(0.75f, 0f, 0.25f, 0.5f),
                    CollageTemplate.Cell(0f, 0.5f, 0.25f, 0.5f),
                    CollageTemplate.Cell(0.25f, 0.5f, 0.25f, 0.5f),
                    CollageTemplate.Cell(0.5f, 0.5f, 0.25f, 0.5f),
                    CollageTemplate.Cell(0.75f, 0.5f, 0.25f, 0.5f)
                )),
                // Large right
                CollageTemplate("8_layout_right", "מימין", 8, listOf(
                    CollageTemplate.Cell(0.5f, 0f, 0.5f, 1f),
                    CollageTemplate.Cell(0.25f, 0f, 0.25f, 0.25f),
                    CollageTemplate.Cell(0f, 0f, 0.25f, 0.25f),
                    CollageTemplate.Cell(0.25f, 0.25f, 0.25f, 0.25f),
                    CollageTemplate.Cell(0f, 0.25f, 0.25f, 0.25f),
                    CollageTemplate.Cell(0.334f, 0.5f, 0.166f, 0.5f),
                    CollageTemplate.Cell(0.166f, 0.5f, 0.168f, 0.5f),
                    CollageTemplate.Cell(0f, 0.5f, 0.166f, 0.5f)
                )),
                // Large top; bottom: 4 squares left + 3 rectangles right
                CollageTemplate("8_layout_top", "למעלה", 8, listOf(
                    CollageTemplate.Cell(0f, 0f, 1f, 0.5f),
                    CollageTemplate.Cell(0f, 0.5f, 0.25f, 0.25f),
                    CollageTemplate.Cell(0.25f, 0.5f, 0.25f, 0.25f),
                    CollageTemplate.Cell(0f, 0.75f, 0.25f, 0.25f),
                    CollageTemplate.Cell(0.25f, 0.75f, 0.25f, 0.25f),
                    CollageTemplate.Cell(0.5f, 0.5f, 0.166f, 0.5f),
                    CollageTemplate.Cell(0.666f, 0.5f, 0.168f, 0.5f),
                    CollageTemplate.Cell(0.834f, 0.5f, 0.166f, 0.5f)
                )),
                // Large bottom; top: 4 squares left + 3 rectangles right
                CollageTemplate("8_layout_bottom", "למטה", 8, listOf(
                    CollageTemplate.Cell(0f, 0.5f, 1f, 0.5f),
                    CollageTemplate.Cell(0f, 0f, 0.25f, 0.25f),
                    CollageTemplate.Cell(0.25f, 0f, 0.25f, 0.25f),
                    CollageTemplate.Cell(0f, 0.25f, 0.25f, 0.25f),
                    CollageTemplate.Cell(0.25f, 0.25f, 0.25f, 0.25f),
                    CollageTemplate.Cell(0.5f, 0f, 0.166f, 0.5f),
                    CollageTemplate.Cell(0.666f, 0f, 0.168f, 0.5f),
                    CollageTemplate.Cell(0.834f, 0f, 0.166f, 0.5f)
                )),
                // Large left full height; right top 2x2; right bottom 3 in row (legacy)
                CollageTemplate("8_large_left_grid", "Large Left + Grid", 8, listOf(
                    CollageTemplate.Cell(0f, 0f, 0.5f, 1f),         // Left large full height
                    CollageTemplate.Cell(0.5f, 0f, 0.25f, 0.25f),  // Top-right 2x2
                    CollageTemplate.Cell(0.75f, 0f, 0.25f, 0.25f),
                    CollageTemplate.Cell(0.5f, 0.25f, 0.25f, 0.25f),
                    CollageTemplate.Cell(0.75f, 0.25f, 0.25f, 0.25f),
                    CollageTemplate.Cell(0.5f, 0.5f, 0.166f, 0.5f), // Bottom row 3
                    CollageTemplate.Cell(0.666f, 0.5f, 0.168f, 0.5f),
                    CollageTemplate.Cell(0.834f, 0.5f, 0.166f, 0.5f)
                )),
                // 2x4 vertical grid
                CollageTemplate("8_grid_vertical", "2x4 Grid", 8, listOf(
                    CollageTemplate.Cell(0f, 0f, 0.5f, 0.25f),
                    CollageTemplate.Cell(0.5f, 0f, 0.5f, 0.25f),
                    CollageTemplate.Cell(0f, 0.25f, 0.5f, 0.25f),
                    CollageTemplate.Cell(0.5f, 0.25f, 0.5f, 0.25f),
                    CollageTemplate.Cell(0f, 0.5f, 0.5f, 0.25f),
                    CollageTemplate.Cell(0.5f, 0.5f, 0.5f, 0.25f),
                    CollageTemplate.Cell(0f, 0.75f, 0.5f, 0.25f),
                    CollageTemplate.Cell(0.5f, 0.75f, 0.5f, 0.25f)
                )),
                // Large left (mirror of layout_right)
                CollageTemplate("8_layout_left", "מישמאל", 8, listOf(
                    CollageTemplate.Cell(0f, 0f, 0.5f, 1f),
                    CollageTemplate.Cell(0.5f, 0f, 0.25f, 0.25f),
                    CollageTemplate.Cell(0.75f, 0f, 0.25f, 0.25f),
                    CollageTemplate.Cell(0.5f, 0.25f, 0.25f, 0.25f),
                    CollageTemplate.Cell(0.75f, 0.25f, 0.25f, 0.25f),
                    CollageTemplate.Cell(0.5f, 0.5f, 0.166f, 0.5f),
                    CollageTemplate.Cell(0.666f, 0.5f, 0.168f, 0.5f),
                    CollageTemplate.Cell(0.834f, 0.5f, 0.166f, 0.5f)
                )),
                // Pyramid: 1, 2, 3, 2
                CollageTemplate("8_pyramid", "Pyramid", 8, listOf(
                    CollageTemplate.Cell(0f, 0f, 1f, 0.2f),
                    CollageTemplate.Cell(0f, 0.2f, 0.5f, 0.25f),
                    CollageTemplate.Cell(0.5f, 0.2f, 0.5f, 0.25f),
                    CollageTemplate.Cell(0f, 0.45f, 0.33f, 0.28f),
                    CollageTemplate.Cell(0.33f, 0.45f, 0.34f, 0.28f),
                    CollageTemplate.Cell(0.67f, 0.45f, 0.33f, 0.28f),
                    CollageTemplate.Cell(0f, 0.73f, 0.5f, 0.27f),
                    CollageTemplate.Cell(0.5f, 0.73f, 0.5f, 0.27f)
                )),
                // Inverted pyramid: 2, 3, 2, 1
                CollageTemplate("8_inverted_pyramid", "Inverted Pyramid", 8, listOf(
                    CollageTemplate.Cell(0f, 0f, 0.5f, 0.27f),
                    CollageTemplate.Cell(0.5f, 0f, 0.5f, 0.27f),
                    CollageTemplate.Cell(0f, 0.27f, 0.33f, 0.28f),
                    CollageTemplate.Cell(0.33f, 0.27f, 0.34f, 0.28f),
                    CollageTemplate.Cell(0.67f, 0.27f, 0.33f, 0.28f),
                    CollageTemplate.Cell(0f, 0.55f, 0.5f, 0.25f),
                    CollageTemplate.Cell(0.5f, 0.55f, 0.5f, 0.25f),
                    CollageTemplate.Cell(0f, 0.8f, 1f, 0.2f)
                )),
                // 3 top, 2 middle, 3 bottom
                CollageTemplate("8_3_2_3", "3-2-3", 8, listOf(
                    CollageTemplate.Cell(0f, 0f, 0.33f, 0.33f),
                    CollageTemplate.Cell(0.33f, 0f, 0.34f, 0.33f),
                    CollageTemplate.Cell(0.67f, 0f, 0.33f, 0.33f),
                    CollageTemplate.Cell(0f, 0.33f, 0.5f, 0.34f),
                    CollageTemplate.Cell(0.5f, 0.33f, 0.5f, 0.34f),
                    CollageTemplate.Cell(0f, 0.67f, 0.33f, 0.33f),
                    CollageTemplate.Cell(0.33f, 0.67f, 0.34f, 0.33f),
                    CollageTemplate.Cell(0.67f, 0.67f, 0.33f, 0.33f)
                ))
            )
            9 -> listOf<CollageTemplate>(
                CollageTemplate("9_grid", "3x3 Grid", 9, listOf(
                    CollageTemplate.Cell(0f, 0f, 0.33f, 0.33f),
                    CollageTemplate.Cell(0.33f, 0f, 0.34f, 0.33f),
                    CollageTemplate.Cell(0.67f, 0f, 0.33f, 0.33f),
                    CollageTemplate.Cell(0f, 0.33f, 0.33f, 0.34f),
                    CollageTemplate.Cell(0.33f, 0.33f, 0.34f, 0.34f),
                    CollageTemplate.Cell(0.67f, 0.33f, 0.33f, 0.34f),
                    CollageTemplate.Cell(0f, 0.67f, 0.33f, 0.33f),
                    CollageTemplate.Cell(0.33f, 0.67f, 0.34f, 0.33f),
                    CollageTemplate.Cell(0.67f, 0.67f, 0.33f, 0.33f)
                )),
                // Large top, 8 small in 4x2 below
                CollageTemplate("9_large_top", "Large Top", 9, listOf(
                    CollageTemplate.Cell(0f, 0f, 1f, 0.5f),
                    CollageTemplate.Cell(0f, 0.5f, 0.25f, 0.25f),
                    CollageTemplate.Cell(0.25f, 0.5f, 0.25f, 0.25f),
                    CollageTemplate.Cell(0.5f, 0.5f, 0.25f, 0.25f),
                    CollageTemplate.Cell(0.75f, 0.5f, 0.25f, 0.25f),
                    CollageTemplate.Cell(0f, 0.75f, 0.25f, 0.25f),
                    CollageTemplate.Cell(0.25f, 0.75f, 0.25f, 0.25f),
                    CollageTemplate.Cell(0.5f, 0.75f, 0.25f, 0.25f),
                    CollageTemplate.Cell(0.75f, 0.75f, 0.25f, 0.25f)
                )),
                // Large bottom, 8 small in 4x2 above
                CollageTemplate("9_large_bottom", "Large Bottom", 9, listOf(
                    CollageTemplate.Cell(0f, 0f, 0.25f, 0.25f),
                    CollageTemplate.Cell(0.25f, 0f, 0.25f, 0.25f),
                    CollageTemplate.Cell(0.5f, 0f, 0.25f, 0.25f),
                    CollageTemplate.Cell(0.75f, 0f, 0.25f, 0.25f),
                    CollageTemplate.Cell(0f, 0.25f, 0.25f, 0.25f),
                    CollageTemplate.Cell(0.25f, 0.25f, 0.25f, 0.25f),
                    CollageTemplate.Cell(0.5f, 0.25f, 0.25f, 0.25f),
                    CollageTemplate.Cell(0.75f, 0.25f, 0.25f, 0.25f),
                    CollageTemplate.Cell(0f, 0.5f, 1f, 0.5f)
                )),
                // Large left, 8 small in 2x4 on right
                CollageTemplate("9_large_left", "Large Left", 9, listOf(
                    CollageTemplate.Cell(0f, 0f, 0.5f, 1f),
                    CollageTemplate.Cell(0.5f, 0f, 0.25f, 0.25f),
                    CollageTemplate.Cell(0.75f, 0f, 0.25f, 0.25f),
                    CollageTemplate.Cell(0.5f, 0.25f, 0.25f, 0.25f),
                    CollageTemplate.Cell(0.75f, 0.25f, 0.25f, 0.25f),
                    CollageTemplate.Cell(0.5f, 0.5f, 0.25f, 0.25f),
                    CollageTemplate.Cell(0.75f, 0.5f, 0.25f, 0.25f),
                    CollageTemplate.Cell(0.5f, 0.75f, 0.25f, 0.25f),
                    CollageTemplate.Cell(0.75f, 0.75f, 0.25f, 0.25f)
                )),
                // Large right, 8 small in 2x4 on left
                CollageTemplate("9_large_right", "Large Right", 9, listOf(
                    CollageTemplate.Cell(0f, 0f, 0.25f, 0.25f),
                    CollageTemplate.Cell(0.25f, 0f, 0.25f, 0.25f),
                    CollageTemplate.Cell(0f, 0.25f, 0.25f, 0.25f),
                    CollageTemplate.Cell(0.25f, 0.25f, 0.25f, 0.25f),
                    CollageTemplate.Cell(0f, 0.5f, 0.25f, 0.25f),
                    CollageTemplate.Cell(0.25f, 0.5f, 0.25f, 0.25f),
                    CollageTemplate.Cell(0f, 0.75f, 0.25f, 0.25f),
                    CollageTemplate.Cell(0.25f, 0.75f, 0.25f, 0.25f),
                    CollageTemplate.Cell(0.5f, 0f, 0.5f, 1f)
                )),
                // 4 top, 5 bottom
                CollageTemplate("9_4_5", "4 Top, 5 Bottom", 9, listOf(
                    CollageTemplate.Cell(0f, 0f, 0.25f, 0.4f),
                    CollageTemplate.Cell(0.25f, 0f, 0.25f, 0.4f),
                    CollageTemplate.Cell(0.5f, 0f, 0.25f, 0.4f),
                    CollageTemplate.Cell(0.75f, 0f, 0.25f, 0.4f),
                    CollageTemplate.Cell(0f, 0.4f, 0.2f, 0.6f),
                    CollageTemplate.Cell(0.2f, 0.4f, 0.2f, 0.6f),
                    CollageTemplate.Cell(0.4f, 0.4f, 0.2f, 0.6f),
                    CollageTemplate.Cell(0.6f, 0.4f, 0.2f, 0.6f),
                    CollageTemplate.Cell(0.8f, 0.4f, 0.2f, 0.6f)
                )),
                // Large center, 8 small around
                CollageTemplate("9_center_large", "Center Large", 9, listOf(
                    CollageTemplate.Cell(0f, 0f, 0.33f, 0.25f),
                    CollageTemplate.Cell(0.33f, 0f, 0.34f, 0.25f),
                    CollageTemplate.Cell(0.67f, 0f, 0.33f, 0.25f),
                    CollageTemplate.Cell(0f, 0.25f, 0.25f, 0.5f),
                    CollageTemplate.Cell(0.25f, 0.25f, 0.5f, 0.5f),
                    CollageTemplate.Cell(0.75f, 0.25f, 0.25f, 0.5f),
                    CollageTemplate.Cell(0f, 0.75f, 0.33f, 0.25f),
                    CollageTemplate.Cell(0.33f, 0.75f, 0.34f, 0.25f),
                    CollageTemplate.Cell(0.67f, 0.75f, 0.33f, 0.25f)
                ))
            )
            10 -> listOf<CollageTemplate>(
                CollageTemplate("10_grid", "5x2 Grid", 10, listOf(
                    CollageTemplate.Cell(0f, 0f, 0.2f, 0.5f),
                    CollageTemplate.Cell(0.2f, 0f, 0.2f, 0.5f),
                    CollageTemplate.Cell(0.4f, 0f, 0.2f, 0.5f),
                    CollageTemplate.Cell(0.6f, 0f, 0.2f, 0.5f),
                    CollageTemplate.Cell(0.8f, 0f, 0.2f, 0.5f),
                    CollageTemplate.Cell(0f, 0.5f, 0.2f, 0.5f),
                    CollageTemplate.Cell(0.2f, 0.5f, 0.2f, 0.5f),
                    CollageTemplate.Cell(0.4f, 0.5f, 0.2f, 0.5f),
                    CollageTemplate.Cell(0.6f, 0.5f, 0.2f, 0.5f),
                    CollageTemplate.Cell(0.8f, 0.5f, 0.2f, 0.5f)
                )),
                CollageTemplate("10_2x5", "2x5 Grid", 10, listOf(
                    CollageTemplate.Cell(0f, 0f, 0.5f, 0.2f),
                    CollageTemplate.Cell(0.5f, 0f, 0.5f, 0.2f),
                    CollageTemplate.Cell(0f, 0.2f, 0.5f, 0.2f),
                    CollageTemplate.Cell(0.5f, 0.2f, 0.5f, 0.2f),
                    CollageTemplate.Cell(0f, 0.4f, 0.5f, 0.2f),
                    CollageTemplate.Cell(0.5f, 0.4f, 0.5f, 0.2f),
                    CollageTemplate.Cell(0f, 0.6f, 0.5f, 0.2f),
                    CollageTemplate.Cell(0.5f, 0.6f, 0.5f, 0.2f),
                    CollageTemplate.Cell(0f, 0.8f, 0.5f, 0.2f),
                    CollageTemplate.Cell(0.5f, 0.8f, 0.5f, 0.2f)
                )),
                // 4 quadrants: top-left large, top-right 2x2, bottom-left 2x2, bottom-right large
                CollageTemplate("10_quadrants", "4 Quadrants", 10, listOf(
                    CollageTemplate.Cell(0f, 0f, 0.5f, 0.5f),         // Top-left large
                    CollageTemplate.Cell(0.5f, 0f, 0.25f, 0.25f),     // Top-right 2x2
                    CollageTemplate.Cell(0.75f, 0f, 0.25f, 0.25f),
                    CollageTemplate.Cell(0.5f, 0.25f, 0.25f, 0.25f),
                    CollageTemplate.Cell(0.75f, 0.25f, 0.25f, 0.25f),
                    CollageTemplate.Cell(0f, 0.5f, 0.25f, 0.25f),     // Bottom-left 2x2
                    CollageTemplate.Cell(0.25f, 0.5f, 0.25f, 0.25f),
                    CollageTemplate.Cell(0f, 0.75f, 0.25f, 0.25f),
                    CollageTemplate.Cell(0.25f, 0.75f, 0.25f, 0.25f),
                    CollageTemplate.Cell(0.5f, 0.5f, 0.5f, 0.5f)      // Bottom-right large
                )),
                // 4 quadrants reversed: top-left 2x2, top-right large, bottom-left large, bottom-right 2x2
                CollageTemplate("10_quadrants_reversed", "4 Quadrants Reversed", 10, listOf(
                    CollageTemplate.Cell(0f, 0f, 0.25f, 0.25f),       // Top-left 2x2
                    CollageTemplate.Cell(0.25f, 0f, 0.25f, 0.25f),
                    CollageTemplate.Cell(0f, 0.25f, 0.25f, 0.25f),
                    CollageTemplate.Cell(0.25f, 0.25f, 0.25f, 0.25f),
                    CollageTemplate.Cell(0.5f, 0f, 0.5f, 0.5f),       // Top-right large
                    CollageTemplate.Cell(0f, 0.5f, 0.5f, 0.5f),       // Bottom-left large
                    CollageTemplate.Cell(0.5f, 0.5f, 0.25f, 0.25f),   // Bottom-right 2x2
                    CollageTemplate.Cell(0.75f, 0.5f, 0.25f, 0.25f),
                    CollageTemplate.Cell(0.5f, 0.75f, 0.25f, 0.25f),
                    CollageTemplate.Cell(0.75f, 0.75f, 0.25f, 0.25f)
                )),
                // Large top, 9 small in 5+4 below (full coverage)
                CollageTemplate("10_large_top", "Large Top", 10, listOf(
                    CollageTemplate.Cell(0f, 0f, 1f, 0.5f),
                    CollageTemplate.Cell(0f, 0.5f, 0.2f, 0.25f),
                    CollageTemplate.Cell(0.2f, 0.5f, 0.2f, 0.25f),
                    CollageTemplate.Cell(0.4f, 0.5f, 0.2f, 0.25f),
                    CollageTemplate.Cell(0.6f, 0.5f, 0.2f, 0.25f),
                    CollageTemplate.Cell(0.8f, 0.5f, 0.2f, 0.25f),
                    CollageTemplate.Cell(0f, 0.75f, 0.25f, 0.25f),
                    CollageTemplate.Cell(0.25f, 0.75f, 0.25f, 0.25f),
                    CollageTemplate.Cell(0.5f, 0.75f, 0.25f, 0.25f),
                    CollageTemplate.Cell(0.75f, 0.75f, 0.25f, 0.25f)
                )),
                // Large bottom, 9 small above (full coverage)
                CollageTemplate("10_large_bottom", "Large Bottom", 10, listOf(
                    CollageTemplate.Cell(0f, 0f, 0.2f, 0.25f),
                    CollageTemplate.Cell(0.2f, 0f, 0.2f, 0.25f),
                    CollageTemplate.Cell(0.4f, 0f, 0.2f, 0.25f),
                    CollageTemplate.Cell(0.6f, 0f, 0.2f, 0.25f),
                    CollageTemplate.Cell(0.8f, 0f, 0.2f, 0.25f),
                    CollageTemplate.Cell(0f, 0.25f, 0.25f, 0.25f),
                    CollageTemplate.Cell(0.25f, 0.25f, 0.25f, 0.25f),
                    CollageTemplate.Cell(0.5f, 0.25f, 0.25f, 0.25f),
                    CollageTemplate.Cell(0.75f, 0.25f, 0.25f, 0.25f),
                    CollageTemplate.Cell(0f, 0.5f, 1f, 0.5f)
                )),
                // Large left, 9 small in 3x3 on right
                CollageTemplate("10_large_left", "Large Left", 10, listOf(
                    CollageTemplate.Cell(0f, 0f, 0.5f, 1f),
                    CollageTemplate.Cell(0.5f, 0f, 0.166f, 0.33f),
                    CollageTemplate.Cell(0.666f, 0f, 0.168f, 0.33f),
                    CollageTemplate.Cell(0.834f, 0f, 0.166f, 0.33f),
                    CollageTemplate.Cell(0.5f, 0.33f, 0.166f, 0.34f),
                    CollageTemplate.Cell(0.666f, 0.33f, 0.168f, 0.34f),
                    CollageTemplate.Cell(0.834f, 0.33f, 0.166f, 0.34f),
                    CollageTemplate.Cell(0.5f, 0.67f, 0.166f, 0.33f),
                    CollageTemplate.Cell(0.666f, 0.67f, 0.168f, 0.33f),
                    CollageTemplate.Cell(0.834f, 0.67f, 0.166f, 0.33f)
                )),
                // Large right, 9 small in 3x3 on left
                CollageTemplate("10_large_right", "Large Right", 10, listOf(
                    CollageTemplate.Cell(0f, 0f, 0.166f, 0.33f),
                    CollageTemplate.Cell(0.166f, 0f, 0.168f, 0.33f),
                    CollageTemplate.Cell(0.334f, 0f, 0.166f, 0.33f),
                    CollageTemplate.Cell(0f, 0.33f, 0.166f, 0.34f),
                    CollageTemplate.Cell(0.166f, 0.33f, 0.168f, 0.34f),
                    CollageTemplate.Cell(0.334f, 0.33f, 0.166f, 0.34f),
                    CollageTemplate.Cell(0f, 0.67f, 0.166f, 0.33f),
                    CollageTemplate.Cell(0.166f, 0.67f, 0.168f, 0.33f),
                    CollageTemplate.Cell(0.334f, 0.67f, 0.166f, 0.33f),
                    CollageTemplate.Cell(0.5f, 0f, 0.5f, 1f)
                )),
                // Pyramid: 1, 2, 3, 4 (full coverage, no gaps)
                CollageTemplate("10_pyramid", "Pyramid", 10, listOf(
                    CollageTemplate.Cell(0f, 0f, 1f, 0.25f),
                    CollageTemplate.Cell(0f, 0.25f, 0.5f, 0.25f),
                    CollageTemplate.Cell(0.5f, 0.25f, 0.5f, 0.25f),
                    CollageTemplate.Cell(0f, 0.5f, 0.33f, 0.25f),
                    CollageTemplate.Cell(0.33f, 0.5f, 0.34f, 0.25f),
                    CollageTemplate.Cell(0.67f, 0.5f, 0.33f, 0.25f),
                    CollageTemplate.Cell(0f, 0.75f, 0.25f, 0.25f),
                    CollageTemplate.Cell(0.25f, 0.75f, 0.25f, 0.25f),
                    CollageTemplate.Cell(0.5f, 0.75f, 0.25f, 0.25f),
                    CollageTemplate.Cell(0.75f, 0.75f, 0.25f, 0.25f)
                )),
                // Inverted pyramid: 4, 3, 2, 1 (full coverage, no gaps)
                CollageTemplate("10_inverted_pyramid", "Inverted Pyramid", 10, listOf(
                    CollageTemplate.Cell(0f, 0f, 0.25f, 0.25f),
                    CollageTemplate.Cell(0.25f, 0f, 0.25f, 0.25f),
                    CollageTemplate.Cell(0.5f, 0f, 0.25f, 0.25f),
                    CollageTemplate.Cell(0.75f, 0f, 0.25f, 0.25f),
                    CollageTemplate.Cell(0f, 0.25f, 0.33f, 0.25f),
                    CollageTemplate.Cell(0.33f, 0.25f, 0.34f, 0.25f),
                    CollageTemplate.Cell(0.67f, 0.25f, 0.33f, 0.25f),
                    CollageTemplate.Cell(0f, 0.5f, 0.5f, 0.25f),
                    CollageTemplate.Cell(0.5f, 0.5f, 0.5f, 0.25f),
                    CollageTemplate.Cell(0f, 0.75f, 1f, 0.25f)
                )),
                // 4 top, 3 middle, 3 bottom (full coverage)
                CollageTemplate("10_4_3_3", "4-3-3", 10, listOf(
                    CollageTemplate.Cell(0f, 0f, 0.25f, 0.33f),
                    CollageTemplate.Cell(0.25f, 0f, 0.25f, 0.33f),
                    CollageTemplate.Cell(0.5f, 0f, 0.25f, 0.33f),
                    CollageTemplate.Cell(0.75f, 0f, 0.25f, 0.33f),
                    CollageTemplate.Cell(0f, 0.33f, 0.33f, 0.34f),
                    CollageTemplate.Cell(0.33f, 0.33f, 0.34f, 0.34f),
                    CollageTemplate.Cell(0.67f, 0.33f, 0.33f, 0.34f),
                    CollageTemplate.Cell(0f, 0.67f, 0.33f, 0.33f),
                    CollageTemplate.Cell(0.33f, 0.67f, 0.34f, 0.33f),
                    CollageTemplate.Cell(0.67f, 0.67f, 0.33f, 0.33f)
                )),
                // 3-4-3 layout (full coverage)
                CollageTemplate("10_3_4_3", "3-4-3", 10, listOf(
                    CollageTemplate.Cell(0f, 0f, 0.33f, 0.33f),
                    CollageTemplate.Cell(0.33f, 0f, 0.34f, 0.33f),
                    CollageTemplate.Cell(0.67f, 0f, 0.33f, 0.33f),
                    CollageTemplate.Cell(0f, 0.33f, 0.25f, 0.34f),
                    CollageTemplate.Cell(0.25f, 0.33f, 0.25f, 0.34f),
                    CollageTemplate.Cell(0.5f, 0.33f, 0.25f, 0.34f),
                    CollageTemplate.Cell(0.75f, 0.33f, 0.25f, 0.34f),
                    CollageTemplate.Cell(0f, 0.67f, 0.33f, 0.33f),
                    CollageTemplate.Cell(0.33f, 0.67f, 0.34f, 0.33f),
                    CollageTemplate.Cell(0.67f, 0.67f, 0.33f, 0.33f)
                ))
            )
            else -> emptyList()
        }
    }
}
