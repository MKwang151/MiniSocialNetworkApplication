package com.example.minisocialnetworkapplication.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.minisocialnetworkapplication.ui.auth.AuthViewModel
import com.example.minisocialnetworkapplication.ui.auth.LoginScreen
import com.example.minisocialnetworkapplication.ui.auth.RegisterScreen
import com.example.minisocialnetworkapplication.ui.components.BottomNavBar
import com.example.minisocialnetworkapplication.ui.feed.FeedScreen
import com.example.minisocialnetworkapplication.ui.post.ComposePostScreen
import com.example.minisocialnetworkapplication.ui.postdetail.PostDetailScreen
import com.example.minisocialnetworkapplication.ui.profile.EditProfileScreen
import com.example.minisocialnetworkapplication.ui.profile.ProfileScreen
import com.example.minisocialnetworkapplication.ui.search.SearchUserScreen
import com.example.minisocialnetworkapplication.ui.settings.SettingsScreen

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String,
    authViewModel: AuthViewModel = hiltViewModel()
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
                onNavigateToImageGallery = { postId, imageIndex ->
                    navController.navigate(Screen.ImageGallery.createRoute(postId, imageIndex))
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
                },
                bottomBar = {
                    BottomNavBar(navController, authViewModel)
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
                },
                bottomBar = {
                    BottomNavBar(navController, authViewModel)
                }
            )
        }

        composable(Screen.PostDetail.route) { backStackEntry ->
            val postId = backStackEntry.arguments?.getString("postId") ?: ""

            PostDetailScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToProfile = { userId ->
                    navController.navigate(Screen.Profile.createRoute(userId))
                },
                onNavigateToImageGallery = { imageIndex ->
                    navController.navigate(Screen.ImageGallery.createRoute(postId, imageIndex))
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

        composable(Screen.Profile.route) {
            // Check if profile was updated
            val profileUpdated = it.savedStateHandle.get<Boolean>("profile_updated") ?: false
            if (profileUpdated) {
                // Clear the flag
                it.savedStateHandle.remove<Boolean>("profile_updated")
            }

            ProfileScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToPostDetail = { postId ->
                    navController.navigate(Screen.PostDetail.createRoute(postId))
                },
                onNavigateToImageGallery = { postId, imageIndex ->
                    navController.navigate(Screen.ImageGallery.createRoute(postId, imageIndex))
                },
                onNavigateToEditProfile = { userId ->
                    navController.navigate(Screen.EditProfile.createRoute(userId))
                },
                shouldRefresh = profileUpdated,
                bottomBar = {
                    BottomNavBar(navController, authViewModel)
                }
            )
        }

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
                    navController.getBackStackEntry(Screen.Feed.route)
                        .savedStateHandle["profile_updated"] = true

                    navController.popBackStack()
                }
            )
        }

        composable(Screen.ImageGallery.route) {
            val viewModel: com.example.minisocialnetworkapplication.ui.gallery.ImageGalleryViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()

            when (val state = uiState) {
                is com.example.minisocialnetworkapplication.ui.gallery.ImageGalleryUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is com.example.minisocialnetworkapplication.ui.gallery.ImageGalleryUiState.Success -> {
                    com.example.minisocialnetworkapplication.ui.gallery.ImageGalleryScreen(
                        imageUrls = state.imageUrls,
                        initialPage = viewModel.initialIndex,
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }
                is com.example.minisocialnetworkapplication.ui.gallery.ImageGalleryUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = state.message)
                    }
                }
            }
        }

        composable(Screen.SearchUser.route) {
            SearchUserScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToProfile = { userId ->
                    navController.navigate(Screen.Profile.createRoute(userId))
                },
                bottomBar = {
                    BottomNavBar(navController, authViewModel)
                }
            )
        }
    }
}
