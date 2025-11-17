package com.example.minisocialnetworkapplication.ui.components

import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.minisocialnetworkapplication.ui.navigation.Screen

@Composable
fun BottomNavBar(navController: NavHostController) {
    val items = listOf(
        BottomNavItem.Home,
        BottomNavItem.Search,
        BottomNavItem.Chat,
        BottomNavItem.Profile,
        BottomNavItem.Settings
    )

    NavigationBar(
        modifier = Modifier.height(80.dp),
        containerColor = Color.Black
    ) {
        val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

        items.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = { navController.navigate(item.route) {
                    launchSingleTop = true
                    restoreState = true
                    popUpTo(Screen.Feed.route) { saveState = true }
                } },
                icon = { Icon(item.icon, contentDescription = item.label) },
//                label = { Text(item.label) }
            )
        }
    }
}

sealed class BottomNavItem(val route: String, val label: String, val icon: ImageVector) {
    object Home: BottomNavItem(Screen.Feed.route, "Home", Icons.Default.Home)
    object Search: BottomNavItem("Search", "Search", Icons.Default.Search)
    object Chat: BottomNavItem("Chat", "Chat", Icons.Default.Inbox)
    object Profile: BottomNavItem(Screen.Profile.route, "Profile", Icons.Default.Person)
    object Settings: BottomNavItem(Screen.Settings.route, "Settings", Icons.Default.Settings)
}
