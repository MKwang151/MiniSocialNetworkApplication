package com.example.minisocialnetworkapplication.ui.group

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.minisocialnetworkapplication.core.domain.model.Friend

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectParticipantsScreen(
    onNavigateBack: () -> Unit,
    onNavigateNext: (String) -> Unit,
    viewModel: SelectParticipantsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Group") },
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("Cancel")
                    }
                },
                actions = {
                    val selectedCount = (uiState as? SelectParticipantsUiState.Success)?.selectedIds?.size ?: 0
                    TextButton(
                        onClick = {
                            val selectedIds = (uiState as? SelectParticipantsUiState.Success)?.selectedIds ?: emptySet()
                            onNavigateNext(selectedIds.joinToString(","))
                        },
                        enabled = selectedCount >= 2
                    ) {
                        Text(
                            text = "Next", 
                            fontWeight = FontWeight.Bold,
                            color = if (selectedCount >= 2) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is SelectParticipantsUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is SelectParticipantsUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                    }
                }
                is SelectParticipantsUiState.Success -> {
                    // Search Bar
                     OutlinedTextField(
                        value = "", // TODO: Bind to viewModel.searchQuery if implemented exposed state
                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                        placeholder = { Text("Search friends") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    // Selected count header? Optional

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(state.friends) { friend ->
                            ParticipantItem(
                                friend = friend,
                                isSelected = state.selectedIds.contains(friend.friendId),
                                onToggle = { viewModel.toggleSelection(friend.friendId) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ParticipantItem(
    friend: Friend,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (!friend.friendAvatarUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = friend.friendAvatarUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Name
        Text(
            text = friend.friendName,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Radio Button (User requested Radio Button, but for multi-select Checkbox is standard. 
        // User text said: "user tích vào các radio button". Radio implies single select.
        // But context is "chọn các participants" (select participants - plural).
        // I will use RadioButton visual but it acts as checkbox (toggleable).
        // Or actually a Checkbox is better UX. But user explicitly asked for "radio button".
        // I'll use RadioButton visual but verify behavior. Visual constraints: user asked for radio.
        RadioButton(
            selected = isSelected,
            onClick = onToggle // RadioButton usually doesn't toggle off on click if selected, but here we want toggle
        )
    }
}
