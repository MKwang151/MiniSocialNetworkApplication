package com.example.minisocialnetworkapplication.ui.admin

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.minisocialnetworkapplication.core.domain.model.Report
import com.example.minisocialnetworkapplication.core.domain.model.ReportStatus
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportManagementScreen(
    onNavigateBack: () -> Unit,
    bottomBar: @Composable () -> Unit = {},
    viewModel: ReportManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val statusFilter by viewModel.statusFilter.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Report Management") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search by reporter or reason...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Status Filters
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = statusFilter == null,
                    onClick = { viewModel.onStatusFilterChange(null) },
                    label = { Text("All") }
                )
                listOf(ReportStatus.PENDING, ReportStatus.RESOLVED, ReportStatus.DISMISSED).forEach { status ->
                    FilterChip(
                        selected = statusFilter == status,
                        onClick = { viewModel.onStatusFilterChange(status) },
                        label = { Text(status.name) }
                    )
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (val state = uiState) {
                    is ReportManagementUiState.Loading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    is ReportManagementUiState.Success -> {
                        if (state.reports.isEmpty()) {
                            Text(
                                text = "No reports found matching filters",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(state.reports) { report ->
                                    ReportItem(
                                        report = report,
                                        onDismiss = { viewModel.dismissReport(report.id) },
                                        onWarning = { viewModel.openWarningDialog(report) },
                                        onBanUser = { 
                                            val userId = if (report.targetType == "USER") report.targetId else report.authorId
                                            if (userId.isNotBlank()) viewModel.banUserAndResolve(report.id, userId)
                                        },
                                        onHidePost = { viewModel.hidePostAndResolve(report.id, report.targetId) },
                                        onDeletePost = { viewModel.deletePostAndResolve(report.id, report.targetId) },
                                        onBanGroup = { 
                                            val gId = if (report.targetType == "GROUP") report.targetId else report.groupId
                                            gId?.let { viewModel.banGroupAndResolve(report.id, it) }
                                        }
                                    )
                                }
                            }
                        }
                    }
                    is ReportManagementUiState.Error -> {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }

        // Warning Dialog
        val warningDialogState by viewModel.warningDialogState.collectAsState()
        warningDialogState?.let { state ->
            WarningDialog(
                onDismiss = viewModel::closeWarningDialog,
                onConfirm = viewModel::sendWarning,
                targetType = state.type
            )
        }
    }
}

@Composable
fun WarningDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    targetType: String
) {
    var warningText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Send Warning") },
        text = {
            Column {
                Text(
                    text = "Message to be sent to the ${if (targetType == "GROUP") "group admin/creator" else "user"}:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = warningText,
                    onValueChange = { warningText = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    placeholder = { Text("Enter warning content...") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (warningText.isNotBlank()) onConfirm(warningText) },
                enabled = warningText.isNotBlank()
            ) {
                Text("Send Warning")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ReportItem(
    report: Report,
    onDismiss: () -> Unit,
    onWarning: () -> Unit,
    onBanUser: () -> Unit,
    onHidePost: () -> Unit,
    onDeletePost: () -> Unit,
    onBanGroup: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = if (report.status != ReportStatus.PENDING) CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) else CardDefaults.cardColors()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = when (report.targetType) {
                        "POST" -> MaterialTheme.colorScheme.primaryContainer
                        "USER" -> MaterialTheme.colorScheme.secondaryContainer
                        "GROUP" -> MaterialTheme.colorScheme.tertiaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text(
                        text = report.targetType,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = dateFormat.format(report.createdAt.toDate()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.weight(1f))
                ReportStatusBadge(report.status)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Reason: ${report.reason}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            if (report.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = report.description,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Reporter: ${report.reporterName}",
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                text = "Author: ${report.authorId.take(8)}...", // Show some context
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            if (report.status == ReportStatus.PENDING) {
                // ROW 1: Common Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Dismiss", style = MaterialTheme.typography.labelSmall)
                    }

                    Button(
                        onClick = onWarning,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.warningColor),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Warning", style = MaterialTheme.typography.labelSmall)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ROW 2: Specific Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when (report.targetType) {
                        "USER" -> {
                            Button(
                                onClick = onBanUser,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Ban User", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        "POST" -> {
                            Button(
                                onClick = onHidePost,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(Icons.Default.VisibilityOff, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Hide", style = MaterialTheme.typography.labelSmall)
                            }
                            Button(
                                onClick = onDeletePost,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Delete", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        "GROUP" -> {
                            Button(
                                onClick = onBanGroup,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Ban Group", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Helper for Warning Color
private val ColorScheme.warningColor: androidx.compose.ui.graphics.Color
    @Composable
    get() = androidx.compose.ui.graphics.Color(0xFFFFA000) // Amber 700

@Composable
fun ReportStatusBadge(status: ReportStatus) {
    val (color, text) = when (status) {
        ReportStatus.PENDING -> MaterialTheme.colorScheme.error to "PENDING"
        ReportStatus.RESOLVED -> MaterialTheme.colorScheme.secondary to "RESOLVED"
        ReportStatus.DISMISSED -> MaterialTheme.colorScheme.onSurfaceVariant to "DISMISSED"
    }
    
    Surface(
        color = color.copy(alpha = 0.1f),
        contentColor = color,
        shape = MaterialTheme.shapes.extraSmall,
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}
