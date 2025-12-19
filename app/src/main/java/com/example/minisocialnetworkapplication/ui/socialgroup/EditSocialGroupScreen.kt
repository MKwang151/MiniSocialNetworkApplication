package com.example.minisocialnetworkapplication.ui.socialgroup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.minisocialnetworkapplication.core.domain.model.GroupPrivacy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSocialGroupScreen(
    onNavigateBack: () -> Unit,
    onGroupUpdated: () -> Unit,
    viewModel: EditSocialGroupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val name by viewModel.name.collectAsState()
    val description by viewModel.description.collectAsState()
    val privacy by viewModel.privacy.collectAsState()
    val avatarUri by viewModel.avatarUri.collectAsState()
    val currentAvatarUrl by viewModel.currentAvatarUrl.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is EditSocialGroupUiState.Updated) {
            onGroupUpdated()
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> viewModel.onAvatarChange(uri) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Group") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = viewModel::updateGroup,
                        enabled = uiState !is EditSocialGroupUiState.Loading && name.isNotBlank()
                    ) {
                        Text("Save")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize()
            ) {
                // Avatar Picker
                Surface(
                    modifier = Modifier
                        .size(100.dp)
                        .align(Alignment.CenterHorizontally)
                        .clip(CircleShape)
                        .clickable { launcher.launch("image/*") },
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (avatarUri != null) {
                            AsyncImage(
                                model = avatarUri,
                                contentDescription = "New Avatar",
                                modifier = Modifier.fillMaxSize()
                            )
                        } else if (currentAvatarUrl != null) {
                            AsyncImage(
                                model = currentAvatarUrl,
                                contentDescription = "Current Avatar",
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(Icons.Default.AddAPhoto, contentDescription = "Add Photo")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = viewModel::onNameChange,
                    label = { Text("Group Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = viewModel::onDescriptionChange,
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text("Privacy", style = MaterialTheme.typography.titleMedium)
                Column(Modifier.selectableGroup()) {
                    GroupPrivacy.entries.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .selectable(
                                    selected = (option == privacy),
                                    onClick = { viewModel.onPrivacyChange(option) },
                                    role = Role.RadioButton
                                )
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (option == privacy),
                                onClick = null
                            )
                            Text(
                                text = option.name,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    }
                }

                if (uiState is EditSocialGroupUiState.Error) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = (uiState as EditSocialGroupUiState.Error).message,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            if (uiState is EditSocialGroupUiState.Loading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}
