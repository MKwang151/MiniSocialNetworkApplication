package com.example.minisocialnetworkapplication.ui.settings

import android.app.Activity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.minisocialnetworkapplication.R
import com.example.minisocialnetworkapplication.core.util.LanguageManager
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit,
    bottomBar: @Composable () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val currentLanguage by viewModel.currentLanguage.collectAsState()

    // Show dialog to restart app after language change
    var showRestartDialog by remember { mutableStateOf(false) }
    var pendingLanguageCode by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        },
        bottomBar = bottomBar
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text(
                text = "Language / Ngôn ngữ",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // English option
            LanguageOption(
                languageName = "English",
                languageCode = LanguageManager.ENGLISH,
                isSelected = currentLanguage == LanguageManager.ENGLISH,
                onSelect = {
                    android.util.Log.d("SettingsScreen", "English selected, current: $currentLanguage")
                    if (currentLanguage != LanguageManager.ENGLISH) {
                        pendingLanguageCode = LanguageManager.ENGLISH
                        showRestartDialog = true
                        android.util.Log.d("SettingsScreen", "Will show restart dialog for English")
                    } else {
                        android.util.Log.d("SettingsScreen", "Already English, skipping")
                    }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Vietnamese option
            LanguageOption(
                languageName = "Tiếng Việt",
                languageCode = LanguageManager.VIETNAMESE,
                isSelected = currentLanguage == LanguageManager.VIETNAMESE,
                onSelect = {
                    android.util.Log.d("SettingsScreen", "Vietnamese selected, current: $currentLanguage")
                    if (currentLanguage != LanguageManager.VIETNAMESE) {
                        pendingLanguageCode = LanguageManager.VIETNAMESE
                        showRestartDialog = true
                        android.util.Log.d("SettingsScreen", "Will show restart dialog for Vietnamese")
                    } else {
                        android.util.Log.d("SettingsScreen", "Already Vietnamese, skipping")
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "App will restart to apply language changes",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    // Restart dialog
    if (showRestartDialog && pendingLanguageCode != null) {
        RestartAppDialog(
            onConfirm = {
                // Save language synchronously first
                val langCode = pendingLanguageCode ?: LanguageManager.ENGLISH
                Timber.d("Saving language: $langCode")
                LanguageManager.setLanguageSync(context, langCode)
                viewModel.changeLanguage(langCode)

                Timber.d("Language saved, restarting app...")

                // Restart entire app (more reliable than recreate())
                val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                if (intent != null) {
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    context.startActivity(intent)

                    // Close current activity
                    (context as? Activity)?.finish()

                    // Force exit to ensure clean restart
                    android.os.Process.killProcess(android.os.Process.myPid())
                } else {
                    Timber.e("Failed to get launch intent")
                }
            },
            onDismiss = {
                showRestartDialog = false
                pendingLanguageCode = null
                onNavigateBack()
            }
        )
    }
}

@Composable
fun LanguageOption(
    languageName: String,
    languageCode: String,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onSelect
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = languageName,
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = languageCode.uppercase(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun RestartAppDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Restart App / Khởi động lại")
        },
        text = {
            Text(text = "The app needs to restart to apply the new language.\n\nỨng dụng cần khởi động lại để áp dụng ngôn ngữ mới.")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Restart / Khởi động lại")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel / Hủy")
            }
        }
    )
}

