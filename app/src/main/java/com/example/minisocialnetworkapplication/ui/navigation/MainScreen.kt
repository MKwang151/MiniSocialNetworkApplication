package com.example.minisocialnetworkapplication.ui.navigation

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.minisocialnetworkapplication.ui.auth.AuthViewModel
import com.example.minisocialnetworkapplication.ui.components.BottomNavBar

@Composable
fun MainScreen(startDestination: String) {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()

    Scaffold(
        bottomBar = {
            val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

            if (currentRoute in listOf(
                    Screen.Feed.route,
                    Screen.ConversationList.route,
                    Screen.Settings.route,
                    Screen.Profile.route,
                    Screen.SearchUser.route
                )
            ) {
                BottomNavBar(navController, authViewModel)
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            NavGraph(
                navController = navController,
                startDestination = startDestination,
                authViewModel = authViewModel
            )
        }
    }
}
