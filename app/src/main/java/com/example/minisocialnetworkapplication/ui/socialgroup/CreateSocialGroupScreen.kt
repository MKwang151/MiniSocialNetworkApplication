package com.example.minisocialnetworkapplication.ui.socialgroup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
fun CreateSocialGroupScreen(
    onNavigateBack: () -> Unit,
    onGroupCreated: (String) -> Unit,
    viewModel: CreateSocialGroupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentStep by viewModel.currentStep.collectAsState()
    val name by viewModel.name.collectAsState()
    val description by viewModel.description.collectAsState()
    val privacy by viewModel.privacy.collectAsState()
    val avatarUri by viewModel.avatarUri.collectAsState()
    val friends by viewModel.friends.collectAsState()
    val selectedFriendIds by viewModel.selectedFriendIds.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is CreateGroupUiState.Success) {
            onGroupCreated((uiState as CreateGroupUiState.Success).groupId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (currentStep == 0) "Create Group" else "Invite Friends") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentStep > 0) {
                            viewModel.previousStep()
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (currentStep == 0) {
                        TextButton(onClick = viewModel::nextStep) {
                            Text("Next")
                        }
                    } else {
                        TextButton(
                            onClick = viewModel::createGroup,
                            enabled = uiState !is CreateGroupUiState.Loading
                        ) {
                            Text("Create")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        when (currentStep) {
            0 -> Step1BasicInfo(
                paddingValues = paddingValues,
                name = name,
                onNameChange = viewModel::onNameChange,
                description = description,
                onDescriptionChange = viewModel::onDescriptionChange,
                privacy = privacy,
                onPrivacyChange = viewModel::onPrivacyChange,
                avatarUri = avatarUri,
                onAvatarSelected = viewModel::onAvatarSelected,
                uiState = uiState
            )
            1 -> Step2FriendSelection(
                paddingValues = paddingValues,
                friends = friends,
                selectedFriendIds = selectedFriendIds,
                onToggleFriend = viewModel::toggleFriendSelection,
                uiState = uiState
            )
        }
    }
}

@Composable
private fun Step1BasicInfo(
    paddingValues: PaddingValues,
    name: String,
    onNameChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    privacy: GroupPrivacy,
    onPrivacyChange: (GroupPrivacy) -> Unit,
    avatarUri: android.net.Uri?,
    onAvatarSelected: (android.net.Uri?) -> Unit,
    uiState: CreateGroupUiState
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> onAvatarSelected(uri) }

    Column(
        modifier = Modifier
            .padding(paddingValues)
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
                        contentDescription = "Group Avatar",
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
            onValueChange = onNameChange,
            label = { Text("Group Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChange,
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
                            onClick = { onPrivacyChange(option) },
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

        if (uiState is CreateGroupUiState.Error) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = uiState.message,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun Step2FriendSelection(
    paddingValues: PaddingValues,
    friends: List<com.example.minisocialnetworkapplication.core.domain.model.User>,
    selectedFriendIds: Set<String>,
    onToggleFriend: (String) -> Unit,
    uiState: CreateGroupUiState
) {
    Column(
        modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize()
    ) {
        if (friends.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No friends to invite")
            }
        } else {
            Text(
                text = "Select friends to invite (${selectedFriendIds.size} selected)",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                items(friends) { friend ->
                    ListItem(
                        headlineContent = { Text(friend.name) },
                        trailingContent = {
                            Checkbox(
                                checked = friend.id in selectedFriendIds,
                                onCheckedChange = { onToggleFriend(friend.id) }
                            )
                        },
                        modifier = Modifier.clickable { onToggleFriend(friend.id) }
                    )
                }
            }
        }

        if (uiState is CreateGroupUiState.Loading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        if (uiState is CreateGroupUiState.Error) {
            Text(
                text = uiState.message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
