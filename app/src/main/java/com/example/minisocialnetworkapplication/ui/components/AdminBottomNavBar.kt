package com.example.minisocialnetworkapplication.ui.components

import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.GroupWork
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.minisocialnetworkapplication.ui.navigation.Screen

@Composable
fun AdminBottomNavBar(
    navController: NavHostController
) {
    val items = listOf(
        AdminBottomNavItem.Dashboard,
        AdminBottomNavItem.Users,
        AdminBottomNavItem.Content,
        AdminBottomNavItem.Reports,
        AdminBottomNavItem.Groups
    )

    NavigationBar(
        modifier = Modifier.height(80.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = {
                    if (currentRoute != item.route) {
                        navController.navigate(item.route) {
                            popUpTo(Screen.AdminDashboard.route) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    Icon(item.icon, contentDescription = item.label)
                },
                label = {
                    Text(text = item.label, style = MaterialTheme.typography.labelSmall)
                }
            )
        }
    }
}

sealed class AdminBottomNavItem(val route: String, val label: String, val icon: ImageVector) {
    object Dashboard : AdminBottomNavItem(Screen.AdminDashboard.route, "Dash", Icons.Default.Dashboard)
    object Users : AdminBottomNavItem(Screen.AdminUserManagement.route, "Users", Icons.Default.People)
    object Content : AdminBottomNavItem(Screen.AdminContentModeration.route, "Content", Icons.Default.Gavel)
    object Reports : AdminBottomNavItem(Screen.AdminReportManagement.route, "Reports", Icons.Default.ReportProblem)
    object Groups : AdminBottomNavItem(Screen.AdminGroupManagement.route, "Groups", Icons.Default.GroupWork)
}
