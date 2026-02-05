package com.example.photoeditor.model

/**
 * Legacy enum for frame styles.
 * Consider migrating to FrameConfig for more flexibility.
 */
enum class FrameStyle {
    NONE;
    
    /**
     * Converts this enum to FrameConfig.
     * For now, only NONE is supported.
     */
    fun toFrameConfig(): FrameConfig {
        return when (this) {
            NONE -> FrameConfig.NONE
        }
    }
}
