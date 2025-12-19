package com.example.minisocialnetworkapplication.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.minisocialnetworkapplication.core.domain.model.User

@Composable
fun DrawerContent(
    user: User?,
    onNavigateToProfile: () -> Unit,
    onNavigateToGroups: () -> Unit,
    onLogout: () -> Unit,
    onCloseDrawer: () -> Unit
) {
    ModalDrawerSheet {
        // ID Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            val name = user?.name ?: "User"
            val email = user?.email ?: ""
            
            Text(text = "Social App", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = name, style = MaterialTheme.typography.titleMedium)
            Text(text = email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        
        HorizontalDivider()
        
        // Menu Items
        DrawerItem(
            icon = Icons.Default.Person, 
            label = "Profile", 
            onClick = { onNavigateToProfile(); onCloseDrawer() }
        )
        DrawerItem(
            icon = Icons.Default.Group, 
            label = "Groups", 
            onClick = { onNavigateToGroups(); onCloseDrawer() }
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        DrawerItem(
            icon = Icons.AutoMirrored.Filled.ExitToApp, 
            label = "Logout", 
            onClick = { onLogout(); onCloseDrawer() }
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun DrawerItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        icon = { Icon(icon, contentDescription = null) },
        label = { Text(label) },
        selected = false,
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 12.dp)
    )
}
