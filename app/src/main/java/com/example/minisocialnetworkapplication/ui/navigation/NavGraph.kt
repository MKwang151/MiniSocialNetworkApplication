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
import com.example.minisocialnetworkapplication.ui.postdetail.PostDetailScreen
import com.example.minisocialnetworkapplication.ui.profile.EditProfileScreen
import com.example.minisocialnetworkapplication.ui.profile.ProfileScreen
import com.example.minisocialnetworkapplication.ui.settings.SettingsScreen

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

            // Get result from ComposePost to trigger refresh
            val shouldRefresh = navController.currentBackStackEntry
                ?.savedStateHandle
                ?.getStateFlow("post_created", false)

            // Check if post was deleted from PostDetailScreen
            val postDeleted = navController.currentBackStackEntry
                ?.savedStateHandle
                ?.getStateFlow("post_deleted", false)

            // Check if profile was updated (need to refresh to show new author names)
            val profileUpdated = navController.currentBackStackEntry
                ?.savedStateHandle
                ?.getStateFlow("profile_updated", false)

            FeedScreen(
                shouldRefresh = shouldRefresh,
                postDeleted = postDeleted,
                profileUpdated = profileUpdated,
                onNavigateToComposePost = {
                    navController.navigate(Screen.ComposePost.route)
                },
                onNavigateToPostDetail = { postId ->
                    navController.navigate(Screen.PostDetail.createRoute(postId))
                },
                onNavigateToProfile = { userId ->
                    navController.navigate(Screen.Profile.createRoute(userId))
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
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
                onNavigateBack = { postCreated ->
                    // Set result for Feed to know if post was created
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("post_created", postCreated)
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Week 3 - Post Detail Screen
        composable(Screen.PostDetail.route) {
            PostDetailScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToProfile = { userId ->
                    navController.navigate(Screen.Profile.createRoute(userId))
                },
                onPostDeleted = {
                    // Set flag for FeedScreen to refresh
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("post_deleted", true)
                    navController.popBackStack()
                }
            )
        }

        // Week 3 - Profile Screen
        composable(Screen.Profile.route) {
            // Check if profile was updated
            val profileUpdated = it.savedStateHandle.get<Boolean>("profile_updated") ?: false
            val profileUpdatedForFeed = it.savedStateHandle.get<Boolean>("profile_updated_for_feed") ?: false

            if (profileUpdated) {
                // Clear the flag
                it.savedStateHandle.remove<Boolean>("profile_updated")
            }

            if (profileUpdatedForFeed) {
                // Clear the flag and forward to FeedScreen
                it.savedStateHandle.remove<Boolean>("profile_updated_for_feed")
                // Set flag for FeedScreen (previousBackStackEntry of Profile is Feed)
                navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.set("profile_updated", true)
            }

            ProfileScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToPostDetail = { postId ->
                    navController.navigate(Screen.PostDetail.createRoute(postId))
                },
                onNavigateToEditProfile = { userId ->
                    navController.navigate(Screen.EditProfile.createRoute(userId))
                },
                shouldRefresh = profileUpdated
            )
        }

        // Week 4 - Edit Profile Screen
        composable(Screen.EditProfile.route) {
            EditProfileScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onProfileUpdated = {
                    // Set flag for ProfileScreen to refresh
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("profile_updated", true)

                    // Also set flag for FeedScreen to refresh
                    // ProfileScreen will be the previousBackStackEntry
                    // So we need to go 2 levels back to reach FeedScreen
                    // Get Profile entry, then its previousBackStackEntry should be Feed
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("profile_updated_for_feed", true)
                }
            )
        }
    }
}
