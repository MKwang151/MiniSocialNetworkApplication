package com.example.minisocialnetworkapplication.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

// Modern color palette - synced with ProfileScreen
private val ColorAccent = Color(0xFF667EEA)
private val ColorSuccess = Color(0xFF11998E)
private val ColorError = Color(0xFFFF416C)
private val ColorWarning = Color(0xFFFF9F43)
private val ColorInfo = Color(0xFF54A0FF)

/**
 * Toast type enum for different notification styles
 */
enum class ToastType {
    SUCCESS, ERROR, WARNING, INFO
}

/**
 * Modern Toast State for managing toast visibility
 */
class ModernToastState {
    var isVisible by mutableStateOf(false)
        private set
    var message by mutableStateOf("")
        private set
    var type by mutableStateOf(ToastType.INFO)
        private set

    suspend fun show(message: String, type: ToastType = ToastType.INFO, duration: Long = 3000L) {
        this.message = message
        this.type = type
        isVisible = true
        delay(duration)
        isVisible = false
    }

    fun hide() {
        isVisible = false
    }
}

@Composable
fun rememberModernToastState(): ModernToastState {
    return remember { ModernToastState() }
}

/**
 * Modern Toast Composable - Beautiful animated notification
 * Replaces traditional Snackbar with a more premium look
 */
@Composable
fun ModernToast(
    state: ModernToastState,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = state.isVisible,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ) + fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(300)
        ) + fadeOut(),
        modifier = modifier
    ) {
        ModernToastContent(
            message = state.message,
            type = state.type,
            onDismiss = { state.hide() }
        )
    }
}

@Composable
private fun ModernToastContent(
    message: String,
    type: ToastType,
    onDismiss: () -> Unit
) {
    val (backgroundColor, icon) = when (type) {
        ToastType.SUCCESS -> ColorSuccess to Icons.Default.CheckCircle
        ToastType.ERROR -> ColorError to Icons.Default.Error
        ToastType.WARNING -> ColorWarning to Icons.Default.Warning
        ToastType.INFO -> ColorInfo to Icons.Default.Info
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(8.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon with colored background
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(12.dp),
                color = backgroundColor.copy(alpha = 0.15f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.padding(8.dp),
                    tint = backgroundColor
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Message
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface
            )

            // Dismiss button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Modern Snackbar Host - Drop-in replacement for standard SnackbarHost
 * Uses bottom slide animation with modern styling
 */
@Composable
fun ModernSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    type: ToastType = ToastType.INFO
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier,
        snackbar = { snackbarData ->
            ModernSnackbar(
                message = snackbarData.visuals.message,
                type = type,
                actionLabel = snackbarData.visuals.actionLabel,
                onAction = { snackbarData.performAction() },
                onDismiss = { snackbarData.dismiss() }
            )
        }
    )
}

@Composable
fun ModernSnackbar(
    message: String,
    type: ToastType = ToastType.INFO,
    actionLabel: String? = null,
    onAction: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    val (accentColor, icon) = when (type) {
        ToastType.SUCCESS -> ColorSuccess to Icons.Default.CheckCircle
        ToastType.ERROR -> ColorError to Icons.Default.Error
        ToastType.WARNING -> ColorWarning to Icons.Default.Warning
        ToastType.INFO -> ColorAccent to Icons.Default.Info
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .shadow(12.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.inverseSurface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Colored accent bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp)
                    .background(accentColor, RoundedCornerShape(2.dp))
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Icon
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Message
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.inverseOnSurface
            )

            // Action button
            if (actionLabel != null) {
                TextButton(onClick = onAction) {
                    Text(
                        text = actionLabel,
                        color = accentColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Simple Toast function for quick notifications
 * Can be used with LaunchedEffect
 */
@Composable
fun SimpleToast(
    visible: Boolean,
    message: String,
    type: ToastType = ToastType.INFO,
    onDismiss: () -> Unit = {}
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ) + fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(200)
        ) + fadeOut()
    ) {
        ModernSnackbar(
            message = message,
            type = type,
            onDismiss = onDismiss
        )
    }
}

/**
 * Success Toast shortcut
 */
suspend fun ModernToastState.showSuccess(message: String, duration: Long = 3000L) {
    show(message, ToastType.SUCCESS, duration)
}

/**
 * Error Toast shortcut
 */
suspend fun ModernToastState.showError(message: String, duration: Long = 3000L) {
    show(message, ToastType.ERROR, duration)
}

/**
 * Warning Toast shortcut
 */
suspend fun ModernToastState.showWarning(message: String, duration: Long = 3000L) {
    show(message, ToastType.WARNING, duration)
}

/**
 * Info Toast shortcut
 */
suspend fun ModernToastState.showInfo(message: String, duration: Long = 3000L) {
    show(message, ToastType.INFO, duration)
}
