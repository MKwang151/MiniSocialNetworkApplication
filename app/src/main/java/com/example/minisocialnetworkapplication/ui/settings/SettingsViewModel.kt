package com.example.minisocialnetworkapplication.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.minisocialnetworkapplication.core.util.LanguageManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    val currentLanguage: StateFlow<String> = LanguageManager.getLanguage(context)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LanguageManager.VIETNAMESE
        )

    fun changeLanguage(languageCode: String) {
        viewModelScope.launch {
            try {
                // Save to DataStore
                LanguageManager.setLanguage(context, languageCode)
                // Save to SharedPreferences for immediate access
                LanguageManager.setLanguageSync(context, languageCode)
                Timber.d("Language changed to: $languageCode")
            } catch (e: Exception) {
                Timber.e(e, "Failed to change language")
            }
        }
    }
}

