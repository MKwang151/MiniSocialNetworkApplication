package com.example.minisocialnetworkapplication.ui.components

import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.minisocialnetworkapplication.ui.auth.AuthViewModel
import com.example.minisocialnetworkapplication.ui.navigation.Screen

// Modern color palette - synced with ProfileScreen
private val ColorAccent = Color(0xFF667EEA)

@Composable
fun BottomNavBar(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    bottomNavViewModel: BottomNavViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()
    val unreadMessageCount by bottomNavViewModel.unreadMessageCount.collectAsStateWithLifecycle()
    val friendRequestCount by bottomNavViewModel.friendRequestCount.collectAsStateWithLifecycle()

    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Search,
        BottomNavItem.Chat,
        BottomNavItem.Profile,
        BottomNavItem.Friends
    )

    NavigationBar(
        modifier = Modifier
            .height(80.dp)
            .shadow(8.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

        items.forEach { item ->
            val isSelected = item.route == currentRoute
            
            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    if (item.route != currentRoute) {
                        navController.navigate(
                            if (item is BottomNavItem.Profile)
                                item.createRoute(currentUser?.id ?: "0")
                            else item.route
                        )
                    }
                },
                icon = {
                    when (item) {
                        BottomNavItem.Chat -> {
                            BadgedBox(
                                badge = {
                                    if (unreadMessageCount > 0) {
                                        Badge(
                                            containerColor = ColorAccent
                                        ) {
                                            Text(
                                                text = if (unreadMessageCount > 99) "99+" else unreadMessageCount.toString(),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    tint = if (isSelected) ColorAccent else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        BottomNavItem.Friends -> {
                            BadgedBox(
                                badge = {
                                    if (friendRequestCount > 0) {
                                        Badge(
                                            containerColor = ColorAccent
                                        ) {
                                            Text(
                                                text = if (friendRequestCount > 99) "99+" else friendRequestCount.toString(),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    tint = if (isSelected) ColorAccent else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        else -> {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                tint = if (isSelected) ColorAccent else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = ColorAccent,
                    selectedTextColor = ColorAccent,
                    indicatorColor = ColorAccent.copy(alpha = 0.1f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

sealed class BottomNavItem(val route: String, val label: String, val icon: ImageVector) {
    object Home : BottomNavItem(Screen.Feed.route, "Home", Icons.Default.Home)
    object Search: BottomNavItem(Screen.SearchUser.route, "Search", Icons.Default.Search)
    object Chat: BottomNavItem(Screen.ConversationList.route, "Chat", Icons.Default.Inbox)
    object Friends : BottomNavItem(Screen.Friends.route, "Friends", Icons.Default.Group)
    object Profile : BottomNavItem(Screen.Profile.route, "Profile", Icons.Default.Person) {
        fun createRoute(userId: String) = "profile/$userId"
    }
}