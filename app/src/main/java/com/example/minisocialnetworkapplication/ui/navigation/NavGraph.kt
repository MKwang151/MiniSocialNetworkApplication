package com.example.minisocialnetworkapplication.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import com.example.minisocialnetworkapplication.ui.friends.FriendScreen
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

            val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()
            
            // Get unread notification count for bell icon badge
            val notificationsViewModel: com.example.minisocialnetworkapplication.ui.notifications.NotificationsViewModel = hiltViewModel()
            val notificationsState by notificationsViewModel.uiState.collectAsStateWithLifecycle()
            val unreadCount = when (val state = notificationsState) {
                is com.example.minisocialnetworkapplication.ui.notifications.NotificationsUiState.Success -> state.unreadCount
                else -> 0
            }

            FeedScreen(
                currentUser = currentUser,
                shouldRefresh = shouldRefresh,
                postDeleted = postDeleted,
                profileUpdated = profileUpdated,
                onNavigateToComposePost = {
                    navController.navigate(Screen.ComposePost.createRoute()) // Use createRoute() for personal posts
                },
                onNavigateToPostDetail = { postId ->
                    navController.navigate(Screen.PostDetail.createRoute(postId))
                },
                onNavigateToProfile = { userId ->
                    navController.navigate(Screen.Profile.createRoute(userId))
                },
                onNavigateToGroups = {
                    navController.navigate(Screen.GroupList.route)
                },
                onNavigateToImageGallery = { postId, imageIndex ->
                    navController.navigate(Screen.ImageGallery.createRoute(postId, imageIndex))
                },
                onNavigateToReportPost = { postId, authorId, groupId ->
                    navController.navigate(Screen.ReportPost.createRoute(postId, authorId, groupId))
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToNotifications = {
                    navController.navigate(Screen.Notifications.route)
                },
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            inclusive = true
                        }
                    }
                },
                unreadNotificationCount = unreadCount,
                bottomBar = {
                    BottomNavBar(navController, authViewModel)
                }
            )
        }

        composable(Screen.GroupList.route) {
             com.example.minisocialnetworkapplication.ui.socialgroup.SocialGroupScreen(
                 navController = navController,
                 authViewModel = authViewModel,
                 onNavigateToCreateGroup = {
                     navController.navigate(Screen.CreateSocialGroup.route)
                 },
                 onNavigateToGroupDetail = { groupId ->
                     navController.navigate(Screen.GroupDetail.createRoute(groupId))
                 },
                 onNavigateToPostDetail = { postId ->
                     navController.navigate(Screen.PostDetail.createRoute(postId))
                 },
                 onNavigateToProfile = { userId ->
                     navController.navigate(Screen.Profile.createRoute(userId))
                 },
                 onNavigateToImageGallery = { postId, index ->
                     navController.navigate(Screen.ImageGallery.createRoute(postId, index))
                 }
             )
        }

        composable(Screen.CreateSocialGroup.route) {
            com.example.minisocialnetworkapplication.ui.socialgroup.CreateSocialGroupScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onGroupCreated = { groupId ->
                    // Navigate to Group Detail and pop creation
                     navController.navigate(Screen.GroupDetail.createRoute(groupId)) {
                         popUpTo(Screen.GroupList.route) { inclusive = false }
                     }
                }
            )
        }
        
        composable(
            route = Screen.GroupDetail.route,
            arguments = listOf(androidx.navigation.navArgument("groupId") { type = androidx.navigation.NavType.StringType })
        ) {
             com.example.minisocialnetworkapplication.ui.socialgroup.GroupDetailScreen(
                 onNavigateBack = {
                     navController.popBackStack()
                 },
                 onNavigateToComposePost = { groupId ->
                     navController.navigate(Screen.ComposePost.createRoute(groupId)) 
                 },
                 onNavigateToPostDetail = { postId ->
                     navController.navigate(Screen.PostDetail.createRoute(postId))
                 },
                 onNavigateToInvite = { groupId ->
                     navController.navigate(Screen.GroupInvite.createRoute(groupId))
                 },
                 onNavigateToProfile = { userId ->
                     navController.navigate(Screen.Profile.createRoute(userId))
                 },
                 onNavigateToImageGallery = { postId, index ->
                     navController.navigate(Screen.ImageGallery.createRoute(postId, index))
                 },
                 onNavigateToJoinRequests = { groupId ->
                     navController.navigate(Screen.GroupJoinRequests.createRoute(groupId))
                 },
                 onNavigateToManage = { groupId, groupName ->
                     navController.navigate(Screen.GroupManagement.createRoute(groupId, groupName))
                 }
             )
        }

        composable(
            route = Screen.GroupInvite.route,
            arguments = listOf(androidx.navigation.navArgument("groupId") { type = androidx.navigation.NavType.StringType })
        ) {
            com.example.minisocialnetworkapplication.ui.socialgroup.GroupInviteScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = Screen.ComposePost.route,
            arguments = listOf(
                androidx.navigation.navArgument("groupId") { 
                    type = androidx.navigation.NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
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

        composable(Screen.Notifications.route) {
            com.example.minisocialnetworkapplication.ui.notifications.NotificationsScreen(
                onNavigateBack = {
                    navController.popBackStack()
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
                onNavigateToChat = { otherUserId ->
                    navController.navigate(Screen.StartChat.createRoute(otherUserId))
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

        // Chat - Conversation List
        composable(Screen.ConversationList.route) {
            com.example.minisocialnetworkapplication.ui.chat.ConversationListScreen(
                onNavigateToChat = { conversationId ->
                    navController.navigate(Screen.ChatDetail.createRoute(conversationId))
                },
                onNavigateToNewChat = {
                    navController.navigate(Screen.SelectParticipants.route)
                },
                bottomBar = {
                    BottomNavBar(navController, authViewModel)
                }
            )
        }

        // Chat Detail
        composable(Screen.ChatDetail.route) { backStackEntry ->
            val scrollToMessageId = backStackEntry.savedStateHandle.get<String>("scroll_to_message_id")
            if (scrollToMessageId != null) {
                backStackEntry.savedStateHandle.remove<String>("scroll_to_message_id")
            }

            com.example.minisocialnetworkapplication.ui.chat.ChatDetailScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToSettings = { conversationId ->
                    navController.navigate(Screen.ChatSettings.createRoute(conversationId))
                },
                scrollToMessageId = scrollToMessageId
            )
        }

        // Start Chat - Creates/gets conversation with a user
        composable(Screen.StartChat.route) {
            val viewModel: com.example.minisocialnetworkapplication.ui.chat.StartChatViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsState()

            when (val state = uiState) {
                is com.example.minisocialnetworkapplication.ui.chat.StartChatUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is com.example.minisocialnetworkapplication.ui.chat.StartChatUiState.Success -> {
                    androidx.compose.runtime.LaunchedEffect(state.conversationId) {
                        navController.navigate(Screen.ChatDetail.createRoute(state.conversationId)) {
                            popUpTo(Screen.StartChat.route) { inclusive = true }
                        }
                    }
                }
                is com.example.minisocialnetworkapplication.ui.chat.StartChatUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = state.message,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        composable(Screen.Friends.route) {
            FriendScreen(
                onNavigateToProfile = { userId ->
                    // Corrected route: Navigate to Profile, not PostDetail
                    navController.navigate(Screen.Profile.createRoute(userId))
                },
                onNavigateToSearch = {
                    navController.navigate(Screen.SearchUser.route)
                },
                onNavigateToChat = { userId ->
                    navController.navigate(Screen.StartChat.createRoute(userId))
                },
                bottomBar = {
                    BottomNavBar(navController, authViewModel)
                }
            )
        }

        composable(Screen.ChatSettings.route) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getString("conversationId") ?: ""
            com.example.minisocialnetworkapplication.ui.settings.ChatSettingsScreen(
                conversationId = conversationId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToProfile = { userId ->
                    navController.navigate(Screen.Profile.createRoute(userId))
                },
                onNavigateToSearch = { 
                    navController.navigate(Screen.MessageSearch.createRoute(conversationId))
                },
                onNavigateToMedia = { 
                    navController.navigate(Screen.ChatMedia.createRoute(conversationId))
                },
                onNavigateToMembers = {
                    navController.navigate(Screen.ChatMembers.createRoute(conversationId))
                },
                onNavigateToAddMember = {
                    navController.navigate(Screen.AddMember.createRoute(conversationId))
                },
                onNavigateToJoinRequests = {
                    navController.navigate(Screen.JoinRequests.createRoute(conversationId))
                },
                onChatDeleted = {
                    // Navigate back to conversation list, popping everything up to it
                    navController.navigate(Screen.ConversationList.route) {
                        popUpTo(Screen.ConversationList.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.ChatMedia.route) { backStackEntry ->
             val conversationId = backStackEntry.arguments?.getString("conversationId") ?: ""
             com.example.minisocialnetworkapplication.ui.media.ChatMediaScreen(
                 conversationId = conversationId,
                 onNavigateBack = {
                     navController.popBackStack()
                 }
             )
        }

        composable(Screen.MessageSearch.route) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getString("conversationId") ?: ""
            com.example.minisocialnetworkapplication.ui.search.MessageSearchScreen(
                conversationId = conversationId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onMessageClick = { messageId ->
                    // Set result on the ChatDetailScreen's back stack entry
                    // We need to find the back stack entry for ChatDetail.
                    // Since the route contains params, we can try to find by route prefix or just popBackStack to it.
                    // But popping doesn't let us set handle before popping unless we have the entry.
                    
                    // Simple hack: We assume ChatDetail is exactly 2 steps back (Detail -> Settings -> Search)
                    // Or 1 step back if came from Detail -> Search (Wait, path is Settings -> Search).
                    // Detail -> Settings -> Search. 
                    // So previous is Settings. entries[size-2] is Settings. entries[size-3] is ChatDetail.
                    
                    // Better approach utilizing Route string:
                    // navController.getBackStackEntry(Screen.ChatDetail.createRoute(conversationId)).savedStateHandle
                    // This requires reconstructing the exact route string used.
                    try {
                        val chatDetailRoute = Screen.ChatDetail.createRoute(conversationId)
                        navController.getBackStackEntry(chatDetailRoute).savedStateHandle["scroll_to_message_id"] = messageId
                        
                        // Pop everything up to ChatDetail (inclusive=false means we stay on ChatDetail)
                        navController.popBackStack(chatDetailRoute, inclusive = false)
                    } catch (e: Exception) {
                        // Fallback in case finding entry fails (should not happen if flow is correct)
                        navController.popBackStack() 
                    }
                }
            )
        }

        composable(Screen.SelectParticipants.route) {
            com.example.minisocialnetworkapplication.ui.group.SelectParticipantsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateNext = { participantIds ->
                    navController.navigate(Screen.CreateGroup.createRoute(participantIds))
                }
            )
        }

        composable(Screen.CreateGroup.route) { backStackEntry ->
            // participantIds argument is handled by ViewModel's SavedStateHandle
            
            com.example.minisocialnetworkapplication.ui.group.CreateGroupScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToChat = { conversationId ->
                    // Navigate to ChatDetail, popping the creation flow from backstack so user can't go back to creation
                    navController.navigate(Screen.ChatDetail.createRoute(conversationId)) {
                        popUpTo(Screen.ConversationList.route) { inclusive = false }
                    }
                }
            )
        }
        
        composable(
            route = Screen.ChatMembers.route,
            arguments = listOf(androidx.navigation.navArgument("conversationId") { type = androidx.navigation.NavType.StringType })
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getString("conversationId") ?: return@composable
            com.example.minisocialnetworkapplication.ui.group.ChatMembersScreen(
                navController = navController,
                conversationId = conversationId
            )
        }

        composable(
            route = Screen.AddMember.route,
            arguments = listOf(androidx.navigation.navArgument("conversationId") { type = androidx.navigation.NavType.StringType })
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getString("conversationId") ?: return@composable
            com.example.minisocialnetworkapplication.ui.group.AddMemberScreen(
                navController = navController,
                conversationId = conversationId
            )
        }

        composable(
            route = Screen.JoinRequests.route,
            arguments = listOf(androidx.navigation.navArgument("conversationId") { type = androidx.navigation.NavType.StringType })
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getString("conversationId") ?: return@composable
            com.example.minisocialnetworkapplication.ui.group.JoinRequestsScreen(
                navController = navController,
                conversationId = conversationId
            )
        }

        // Report Post Screen
        composable(
            route = Screen.ReportPost.route,
            arguments = listOf(
                androidx.navigation.navArgument("postId") { type = androidx.navigation.NavType.StringType },
                androidx.navigation.navArgument("authorId") { type = androidx.navigation.NavType.StringType },
                androidx.navigation.navArgument("groupId") { 
                    type = androidx.navigation.NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            com.example.minisocialnetworkapplication.ui.report.ReportScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Social Group Join Requests Screen (for Admin)
        composable(
            route = "social_group_join_requests/{groupId}",
            arguments = listOf(androidx.navigation.navArgument("groupId") { type = androidx.navigation.NavType.StringType })
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
            com.example.minisocialnetworkapplication.ui.socialgroup.JoinRequestsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Group Management Screen
        composable(
            route = Screen.GroupManagement.route,
            arguments = listOf(
                androidx.navigation.navArgument("groupId") { type = androidx.navigation.NavType.StringType },
                androidx.navigation.navArgument("groupName") { type = androidx.navigation.NavType.StringType }
            )
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
            val groupName = java.net.URLDecoder.decode(
                backStackEntry.arguments?.getString("groupName") ?: "", "UTF-8"
            )
            
            // Get group details for post approval state
            val groupViewModel: com.example.minisocialnetworkapplication.ui.socialgroup.GroupDetailViewModel = androidx.hilt.navigation.compose.hiltViewModel()
            val groupState by groupViewModel.uiState.collectAsState()
            
            com.example.minisocialnetworkapplication.ui.socialgroup.GroupManagementScreen(
                groupId = groupId,
                groupName = groupName,
                group = (groupState as? com.example.minisocialnetworkapplication.ui.socialgroup.GroupDetailUiState.Success)?.group,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToJoinRequests = {
                    navController.navigate("social_group_join_requests/$groupId")
                },
                onNavigateToEditGroup = { /* TODO */ },
                onNavigateToMembers = {
                    navController.navigate(Screen.GroupMembers.createRoute(groupId))
                },
                onNavigateToPendingPosts = {
                    navController.navigate(Screen.PendingPosts.createRoute(groupId))
                },
                onNavigateToReports = {
                    navController.navigate(Screen.GroupReports.createRoute(groupId))
                },
                onTogglePostApproval = { enabled ->
                    groupViewModel.togglePostApproval(enabled)
                },
                onDeleteGroup = { /* TODO */ }
            )
        }

        // Group Members Screen
        composable(
            route = Screen.GroupMembers.route,
            arguments = listOf(androidx.navigation.navArgument("groupId") { type = androidx.navigation.NavType.StringType })
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
            com.example.minisocialnetworkapplication.ui.socialgroup.GroupMembersScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToProfile = { userId ->
                    navController.navigate(Screen.Profile.createRoute(userId))
                }
            )
        }
        
        // Pending Posts Screen
        composable(
            route = Screen.PendingPosts.route,
            arguments = listOf(androidx.navigation.navArgument("groupId") { type = androidx.navigation.NavType.StringType })
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
            com.example.minisocialnetworkapplication.ui.socialgroup.PendingPostsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToProfile = { userId ->
                    navController.navigate(Screen.Profile.createRoute(userId))
                }
            )
        }
        
        // Group Reports Screen
        composable(
            route = Screen.GroupReports.route,
            arguments = listOf(androidx.navigation.navArgument("groupId") { type = androidx.navigation.NavType.StringType })
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
            com.example.minisocialnetworkapplication.ui.socialgroup.GroupReportsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
