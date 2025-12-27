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
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.minisocialnetworkapplication.ui.navigation.Screen

// Modern color palette - synced with ProfileScreen
private val ColorAccent = Color(0xFF667EEA)

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
        modifier = Modifier
            .height(80.dp)
            .shadow(8.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { item ->
            val isSelected = currentRoute == item.route
            
            NavigationBarItem(
                selected = isSelected,
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
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = if (isSelected) ColorAccent else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) ColorAccent else MaterialTheme.colorScheme.onSurfaceVariant
                    )
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

sealed class AdminBottomNavItem(val route: String, val label: String, val icon: ImageVector) {
    object Dashboard : AdminBottomNavItem(Screen.AdminDashboard.route, "Dash", Icons.Default.Dashboard)
    object Users : AdminBottomNavItem(Screen.AdminUserManagement.route, "Users", Icons.Default.People)
    object Content : AdminBottomNavItem(Screen.AdminContentModeration.route, "Content", Icons.Default.Gavel)
    object Reports : AdminBottomNavItem(Screen.AdminReportManagement.route, "Reports", Icons.Default.ReportProblem)
    object Groups : AdminBottomNavItem(Screen.AdminGroupManagement.route, "Groups", Icons.Default.GroupWork)
}
