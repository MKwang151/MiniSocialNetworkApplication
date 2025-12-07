package com.example.minisocialnetworkapplication.ui.components

import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.minisocialnetworkapplication.ui.auth.AuthViewModel
import com.example.minisocialnetworkapplication.ui.navigation.Screen

@Composable
fun BottomNavBar(
    navController: NavHostController,
    authViewModel: AuthViewModel
) {
    val currentUser by authViewModel.currentUser.collectAsStateWithLifecycle()
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Search,
        BottomNavItem.Chat,
        BottomNavItem.Profile,
        BottomNavItem.Friends
    )

    NavigationBar(
        modifier = Modifier.height(80.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

        items.forEach { item ->
            NavigationBarItem(
                selected = item.route == currentRoute,
                onClick = {
                    if (item.route != currentRoute) {
                        navController.navigate(
                            if (item is BottomNavItem.Profile)
                                item.createRoute(currentUser?.id ?: "0")
                            else item.route
                        )
                    } },
                icon = { Icon(item.icon, contentDescription = item.label) },
//                label = { Text(item.label) }
            )
        }
    }
}

sealed class BottomNavItem(val route: String, val label: String, val icon: ImageVector) {
    object Home : BottomNavItem(Screen.Feed.route, "Home", Icons.Default.Home)
    object Search: BottomNavItem(Screen.SearchUser.route, "Search", Icons.Default.Search)
    object Chat: BottomNavItem(Screen.ConversationList.route, "Chat", Icons.Default.Inbox)
    object Chat: BottomNavItem(Screen.Chat.route, "Chat", Icons.Default.Inbox)
    object Friends : BottomNavItem(Screen.Friends.route, "Friends", Icons.Default.Group)
    // navController returns Screen.Profile.route (profile/{userId})
    // actual route is profile/$userId
    object Profile : BottomNavItem(Screen.Profile.route, "Profile", Icons.Default.Person) {
        fun createRoute(userId: String) = "profile/$userId"
    }
}