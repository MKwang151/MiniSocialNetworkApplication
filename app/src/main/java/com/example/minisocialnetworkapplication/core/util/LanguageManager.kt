package com.example.minisocialnetworkapplication.core.util

import android.content.Context
import android.content.res.Configuration
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale

private val Context.languageDataStore by preferencesDataStore(name = "language_settings")

object LanguageManager {
    private val LANGUAGE_KEY = stringPreferencesKey("selected_language")

    // Supported languages
    const val ENGLISH = "en"
    const val VIETNAMESE = "vi"

    /**
     * Save selected language to DataStore
     */
    suspend fun setLanguage(context: Context, languageCode: String) {
        context.languageDataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = languageCode
        }
    }

    /**
     * Get saved language from DataStore
     */
    fun getLanguage(context: Context): Flow<String> {
        return context.languageDataStore.data.map { preferences ->
            preferences[LANGUAGE_KEY] ?: getSystemLanguage()
        }
    }

    /**
     * Get saved language synchronously (for Activity.attachBaseContext)
     */
    fun getLanguageSync(context: Context): String {
        val sharedPrefs = context.getSharedPreferences("language_settings_legacy", Context.MODE_PRIVATE)
        val savedLanguage = sharedPrefs.getString("selected_language", getSystemLanguage()) ?: getSystemLanguage()
        android.util.Log.d("LanguageManager", "getLanguageSync returning: $savedLanguage")
        return savedLanguage
    }

    /**
     * Save language synchronously (for immediate access)
     */
    fun setLanguageSync(context: Context, languageCode: String) {
        android.util.Log.d("LanguageManager", "setLanguageSync called with: $languageCode")
        val sharedPrefs = context.getSharedPreferences("language_settings_legacy", Context.MODE_PRIVATE)
        val success = sharedPrefs.edit().putString("selected_language", languageCode).commit() // Use commit() for immediate save
        android.util.Log.d("LanguageManager", "Save result: $success")

        // Verify saved value
        val saved = sharedPrefs.getString("selected_language", "")
        android.util.Log.d("LanguageManager", "Verified saved value: $saved")
    }

    /**
     * Apply language to context
     */
    fun applyLanguage(context: Context, languageCode: String): Context {
        android.util.Log.d("LanguageManager", "applyLanguage called with: $languageCode")
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        android.util.Log.d("LanguageManager", "Locale.setDefault completed: ${Locale.getDefault()}")

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)

        val newContext = context.createConfigurationContext(config)
        android.util.Log.d("LanguageManager", "Context created with new configuration")
        return newContext
    }

    /**
     * Get system language (en or vi)
     */
    private fun getSystemLanguage(): String {
        val systemLanguage = Locale.getDefault().language
        return if (systemLanguage == VIETNAMESE) VIETNAMESE else ENGLISH
    }

    /**
     * Get language name for display
     */
    fun getLanguageName(languageCode: String): String {
        return when (languageCode) {
            ENGLISH -> "English"
            VIETNAMESE -> "Tiếng Việt"
            else -> "English"
        }
    }
}

