package com.example.minisocialnetworkapplication

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.minisocialnetworkapplication.ui.auth.AuthViewModel
import com.example.minisocialnetworkapplication.ui.navigation.NavGraph
import com.example.minisocialnetworkapplication.ui.navigation.Screen
import com.example.minisocialnetworkapplication.ui.theme.MiniSocialNetworkApplicationTheme
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var firebaseAuth: FirebaseAuth

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Check auth synchronously BEFORE setContent to avoid flash
        val isLoggedIn = firebaseAuth.currentUser != null
        val initialDestination = if (isLoggedIn) Screen.AuthCheck.route else Screen.Login.route
        
        setContent {
            MiniSocialNetworkApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val authViewModel: AuthViewModel = hiltViewModel()
                    val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()
                    var hasNavigated by remember { mutableStateOf(false) }
                    
                    // Use synchronously checked destination, not the async StateFlow initially
                    val startDestination = remember { initialDestination }
                    
                    // Handle logout - if user becomes null after initial navigation
                    LaunchedEffect(currentUser) {
                        if (hasNavigated && currentUser == null && isLoggedIn) {
                            // User logged out, will be handled by NavGraph
                        }
                        hasNavigated = true
                    }

                    NavGraph(startDestination = startDestination)
                }
            }
        }
    }
    
    // Note: Online/offline presence tracking is handled by AppLifecycleObserver
    // at the Application level using ProcessLifecycleOwner for more reliability
}

