package com.example.minisocialnetworkapplication.ui.search

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import com.example.minisocialnetworkapplication.core.domain.model.Message
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MessageSearchScreen(
    conversationId: String,
    viewModel: MessageSearchViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onMessageClick: (String) -> Unit
) {
    val uiState by viewModel.searchMessages(conversationId).collectAsStateWithLifecycle()
    val query by viewModel.searchQuery.collectAsState()

    Scaffold(
        modifier = Modifier.padding(top = 16.dp), // Add some top padding or rely on system bars padding if scaffold handles it poorly
        topBar = {
            Column(modifier = Modifier.padding(top = 24.dp)) { // Explicit top padding requested
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }

                    MessageSearchField(
                        value = query,
                        onValueChange = { viewModel.onQueryChange(it) },
                        placeholder = "Search in conversation...",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp)
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp))
                    )
                }
                HorizontalDivider()
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val state = uiState) {
                is MessageSearchUiState.Idle -> {
                    // Show nothing or suggestions
                }
                is MessageSearchUiState.Loading -> {
                    // Optional loading indicator (debounce handles most)
                }
                is MessageSearchUiState.Error -> {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is MessageSearchUiState.Success -> {
                    if (state.messages.isEmpty() && query.isNotEmpty()) {
                        Text(
                            text = "No results found",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(state.messages, key = { it.id }) { message ->
                                SearchMessageItem(
                                    message = message,
                                    onClick = { onMessageClick(message.id) }
                                )
                                HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MessageSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
        decorationBox = { innerTextField ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .height(36.dp)
                    .fillMaxWidth()
                    .background(Color(0xFFF0F0F0), RoundedCornerShape(18.dp))
                    .padding(horizontal = 12.dp)
            ) {
                if (value.isEmpty() && !isFocused) {
                    Text(placeholder, color = Color.Gray, fontSize = 14.sp)
                }
                innerTextField()
            }
        },
        modifier = modifier.onFocusChanged { focusState -> isFocused = focusState.isFocused }
    )
}

@Composable
fun SearchMessageItem(
    message: Message,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        if (message.senderAvatarUrl != null) {
            Image(
                painter = rememberAsyncImagePainter(message.senderAvatarUrl),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Gray)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = message.senderName,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(message.timestamp.toDate()),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2
            )
        }
    }
}
