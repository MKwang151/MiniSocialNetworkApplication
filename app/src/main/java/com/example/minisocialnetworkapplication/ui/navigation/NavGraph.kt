package com.example.minisocialnetworkapplication.ui.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.minisocialnetworkapplication.ui.auth.AuthViewModel
import com.example.minisocialnetworkapplication.ui.auth.LoginScreen
import com.example.minisocialnetworkapplication.ui.auth.RegisterScreen
import com.example.minisocialnetworkapplication.ui.feed.FeedScreen
import com.example.minisocialnetworkapplication.ui.post.ComposePostScreen

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                onLoginSuccess = {
                    navController.navigate(Screen.Feed.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                onRegisterSuccess = {
                    navController.navigate(Screen.Feed.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Feed.route) {
            val authViewModel: AuthViewModel = hiltViewModel()
            FeedScreen(
                onNavigateToComposePost = {
                    navController.navigate(Screen.ComposePost.route)
                },
                onNavigateToPostDetail = { postId ->
                    navController.navigate(Screen.PostDetail.createRoute(postId))
                },
                onNavigateToProfile = { userId ->
                    navController.navigate(Screen.Profile.createRoute(userId))
                },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(Screen.ComposePost.route) {
            ComposePostScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // TODO: Week 3 - Post Detail Screen
        composable(Screen.PostDetail.route) {
            // PostDetailScreen will be implemented in Week 3
        }

        // TODO: Week 3 - Profile Screen
        composable(Screen.Profile.route) {
            // ProfileScreen will be implemented in Week 3
        }
    }
}
