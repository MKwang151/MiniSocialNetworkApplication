package com.example.minisocialnetworkapplication.ui.search

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.example.minisocialnetworkapplication.core.domain.model.User

@Composable
fun SearchUserScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit,
    onNavigateToProfile: (String) -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val query by viewModel.query.collectAsState()
    val searchResult by viewModel.searchResults.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState) {
        when (uiState) {
            is SearchUiState.Idle -> {
                viewModel.search(query)
            }
            is SearchUiState.Error -> {
                snackbarHostState.showSnackbar(
                    message = (uiState as SearchUiState.Error).message,
                    duration = SnackbarDuration.Short
                )
                viewModel.clearError()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }

            CompactSearchField(
                value = query,
                onValueChange = { viewModel.onQueryChange(it) },
                placeholder = "Search someone",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
            )
        }

        HorizontalDivider(thickness = 2.dp, color = Color.Gray)

        // Search results
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(searchResult) { result ->
                SearchResultItem(
                    user = result,
                    onNavigateToProfile = onNavigateToProfile
                )
            }
        }
    }
}

@Composable
fun CompactSearchField(
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

                innerTextField() // <- actual text input
            }
        },
        modifier = modifier
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
            }
    )
}


@Composable
fun SearchResultItem(
    user: User,
    onNavigateToProfile: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigateToProfile(user.id) }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // optional avatar
        if (user.avatarUrl != null) {
            Image(
                painter = rememberAsyncImagePainter(user.avatarUrl),
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(text = user.name, fontWeight = FontWeight.Bold)
        }
    }
}