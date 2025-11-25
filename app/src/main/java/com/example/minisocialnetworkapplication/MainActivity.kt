package com.example.minisocialnetworkapplication

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.minisocialnetworkapplication.core.util.LanguageManager
import com.example.minisocialnetworkapplication.ui.auth.AuthViewModel
import com.example.minisocialnetworkapplication.ui.navigation.MainScreen
import com.example.minisocialnetworkapplication.ui.navigation.Screen
import com.example.minisocialnetworkapplication.ui.theme.MiniSocialNetworkApplicationTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        // Apply saved language before activity is created
        val languageCode = LanguageManager.getLanguageSync(newBase)
        android.util.Log.d("MainActivity", "Applying language: $languageCode")
        val context = LanguageManager.applyLanguage(newBase, languageCode)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MiniSocialNetworkApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val authViewModel: AuthViewModel = hiltViewModel()
                    val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()

                    val startDestination = if (currentUser != null) {
                        Screen.Feed.route
                    } else {
                        Screen.Login.route
                    }

                    MainScreen(startDestination = startDestination)
                }
            }
        }
    }
}
