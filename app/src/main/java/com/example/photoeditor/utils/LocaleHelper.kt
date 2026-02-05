package com.example.photoeditor.utils

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import java.util.Locale

object LocaleHelper {
    private const val PREFS_NAME = "locale_prefs"
    private const val KEY_SELECTED_LANGUAGE = "selected_language"
    
    /**
     * Save selected language to preferences
     * Use "system" to clear preference and use system default
     * Uses commit() instead of apply() to ensure preference is saved before recreate()
     * Normalizes "iw" to "he" - never stores "iw"
     */
    fun setLocale(context: Context, languageCode: String) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (languageCode == "system") {
            // Clear preference to use system default
            prefs.edit().remove(KEY_SELECTED_LANGUAGE).commit()
        } else {
            // Normalize language code (iw -> he) before storing
            val normalized = normalizeLanguageCode(languageCode)
            prefs.edit().putString(KEY_SELECTED_LANGUAGE, normalized).commit()
        }
    }
    
    /**
     * Clear saved language preference to use system default
     */
    fun clearLocale(context: Context) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_SELECTED_LANGUAGE).commit()
    }
    
    /**
     * Get saved language from preferences, or return system default
     */
    fun getSavedLanguage(context: Context): String {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_SELECTED_LANGUAGE, null) ?: "system"
    }
    
    /**
     * Get locale based on saved preference or system default
     * Returns null if no explicit language was selected (use system default)
     */
    fun getLocale(context: Context): Locale? {
        val savedLanguage = getSavedLanguage(context)
        
        // Only use saved language if user explicitly selected one
        // If "system" or null, return null to use system locale
        return when (savedLanguage) {
            "he" -> Locale("he", "IL")
            "en" -> Locale("en", "US")
            else -> null // null = use system default
        }
    }
    
    /**
     * Wrap context with selected locale
     * If no explicit language is saved, normalizes system locale (iw -> he) and returns wrapped context
     */
    fun wrapContext(context: Context): Context {
        val prefs = try {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        } catch (e: Exception) {
            return context
        }
        
        val savedLanguage = prefs.getString(KEY_SELECTED_LANGUAGE, null)
        
        val locale = when (savedLanguage) {
            "he" -> Locale("iw", "IL")  // עברית
            "en" -> Locale("en", "US")  // אנגלית - גם אם המערכת עברית!
            else -> {
                // Use system default - reset Locale.getDefault() to system locale
                val locales = context.resources.configuration.locales
                val systemLocale = if (locales.isEmpty) Locale.getDefault() else locales.get(0)
                Locale.setDefault(systemLocale)
                return context
            }
        }
        
        Locale.setDefault(locale)  // חשוב!
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            configuration.setLayoutDirection(locale)
        }
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.createConfigurationContext(configuration)
        } else {
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(configuration, context.resources.displayMetrics)
            context
        }
    }
    
    /**
     * Get current app language code
     * Returns system language if no explicit language is selected
     * Normalizes "iw" to "he"
     */
    fun getAppLanguage(context: Context): String {
        val locales = context.resources.configuration.locales
        val locale = getLocale(context)
            ?: if (locales.isEmpty) Locale.getDefault() else locales.get(0)
        return normalizeLanguageCode(locale.language)
    }
    
    /**
     * Get system language code
     * Normalizes "iw" to "he"
     */
    fun getSystemLanguage(): String {
        return normalizeLanguageCode(Locale.getDefault().language)
    }
    
    /**
     * Normalize language code - public version for external use
     */
    fun normalizeLanguageCode(langCode: String): String {
        return when (langCode) {
            "iw" -> "he"  // Deprecated Hebrew code -> modern "he"
            else -> langCode
        }
    }
}
