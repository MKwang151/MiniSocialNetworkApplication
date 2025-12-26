package com.example.minisocialnetworkapplication.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.minisocialnetworkapplication.core.domain.model.Report
import com.example.minisocialnetworkapplication.core.domain.model.ReportStatus
import java.text.SimpleDateFormat
import java.util.*

// Modern color palette
private val ColorPending = Color(0xFFFF416C)
private val ColorResolved = Color(0xFF11998E)
private val ColorDismissed = Color(0xFF9E9E9E)
private val ColorWarning = Color(0xFFFFA000)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportManagementScreen(
    onNavigateBack: () -> Unit,
    onNavigateToProfile: (String) -> Unit = {},
    onNavigateToGroupDetail: (String) -> Unit = {},
    onNavigateToPostDetail: (String) -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    viewModel: ReportManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val statusFilter by viewModel.statusFilter.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(
                            "Report Management",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            "Review user reports",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = bottomBar
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    )
                )
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Modern Search Bar
                ReportSearchBar(
                    query = searchQuery,
                    onQueryChange = viewModel::onSearchQueryChange,
                    modifier = Modifier.padding(16.dp)
                )

                // Stats Row
                when (val state = uiState) {
                    is ReportManagementUiState.Success -> {
                        val totalReports = state.reports.size
                        val pendingReports = state.reports.count { it.status == ReportStatus.PENDING }
                        val resolvedReports = state.reports.count { it.status == ReportStatus.RESOLVED }
                        val dismissedReports = state.reports.count { it.status == ReportStatus.DISMISSED }
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ReportStatChip(
                                modifier = Modifier.weight(1f),
                                label = "Total",
                                value = totalReports.toString(),
                                color = Color(0xFF667EEA)
                            )
                            ReportStatChip(
                                modifier = Modifier.weight(1f),
                                label = "Pending",
                                value = pendingReports.toString(),
                                color = ColorPending
                            )
                            ReportStatChip(
                                modifier = Modifier.weight(1f),
                                label = "Resolved",
                                value = resolvedReports.toString(),
                                color = ColorResolved
                            )
                            ReportStatChip(
                                modifier = Modifier.weight(1f),
                                label = "Dismissed",
                                value = dismissedReports.toString(),
                                color = ColorDismissed
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    else -> {}
                }

                // Status Filters
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filterOptions = listOf(
                        null to "All" to Icons.Default.List,
                        ReportStatus.PENDING to "Pending" to Icons.Default.Warning,
                        ReportStatus.RESOLVED to "Resolved" to Icons.Default.CheckCircle,
                        ReportStatus.DISMISSED to "Dismissed" to Icons.Default.Close
                    )
                    
                    FilterChip(
                        selected = statusFilter == null,
                        onClick = { viewModel.onStatusFilterChange(null) },
                        label = { Text("All") },
                        leadingIcon = { Icon(Icons.Default.List, null, Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF667EEA).copy(alpha = 0.15f),
                            selectedLabelColor = Color(0xFF667EEA)
                        )
                    )
                    
                    listOf(
                        ReportStatus.PENDING to ColorPending,
                        ReportStatus.RESOLVED to ColorResolved,
                        ReportStatus.DISMISSED to ColorDismissed
                    ).forEach { (status, color) ->
                        FilterChip(
                            selected = statusFilter == status,
                            onClick = { viewModel.onStatusFilterChange(status) },
                            label = { Text(status.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = color.copy(alpha = 0.15f),
                                selectedLabelColor = color
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Content
                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    when (val state = uiState) {
                        is ReportManagementUiState.Loading -> {
                            Column(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(48.dp),
                                    strokeWidth = 3.dp
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "Loading reports...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        is ReportManagementUiState.Success -> {
                            if (state.reports.isEmpty()) {
                                Column(
                                    modifier = Modifier.align(Alignment.Center),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.ReportOff,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "No reports found",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Try adjusting your filters",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    items(state.reports, key = { it.id }) { report ->
                                        ModernReportItem(
                                            report = report,
                                            onTargetClick = {
                                                when (report.targetType) {
                                                    "USER" -> onNavigateToProfile(report.targetId)
                                                    "GROUP" -> onNavigateToGroupDetail(report.targetId)
                                                    "POST" -> onNavigateToPostDetail(report.targetId)
                                                }
                                            },
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
                                    
                                    item { Spacer(modifier = Modifier.height(16.dp)) }
                                }
                            }
                        }
                        is ReportManagementUiState.Error -> {
                            Column(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = state.message,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
            }
        }

        // Warning Dialog
        val warningDialogState by viewModel.warningDialogState.collectAsState()
        warningDialogState?.let { state ->
            ModernWarningDialog(
                onDismiss = viewModel::closeWarningDialog,
                onConfirm = viewModel::sendWarning,
                targetType = state.type
            )
        }
    }
}

@Composable
private fun ReportSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { 
                Text(
                    "Search by reporter or reason...",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                ) 
            },
            leadingIcon = { 
                Icon(
                    Icons.Default.Search, 
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                ) 
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )
    }
}

@Composable
private fun ReportStatChip(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    color: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color.copy(alpha = 0.8f),
                fontSize = 9.sp
            )
        }
    }
}

@Composable
fun ModernWarningDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    targetType: String
) {
    var warningText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { 
            Icon(
                Icons.Default.Warning, 
                contentDescription = null,
                tint = ColorWarning,
                modifier = Modifier.size(32.dp)
            ) 
        },
        title = { Text("Send Warning", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    text = "Message to be sent to the ${if (targetType == "GROUP") "group admin/creator" else "user"}:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                OutlinedTextField(
                    value = warningText,
                    onValueChange = { warningText = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    placeholder = { Text("Enter warning content...") },
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (warningText.isNotBlank()) onConfirm(warningText) },
                enabled = warningText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = ColorWarning)
            ) {
                Icon(Icons.Default.Send, null, Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
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
fun ModernReportItem(
    report: Report,
    onTargetClick: () -> Unit,
    onDismiss: () -> Unit,
    onWarning: () -> Unit,
    onBanUser: () -> Unit,
    onHidePost: () -> Unit,
    onDeletePost: () -> Unit,
    onBanGroup: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    val isPending = report.status == ReportStatus.PENDING

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(if (isPending) 4.dp else 2.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPending) 
                MaterialTheme.colorScheme.surface 
            else 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Type badge, Date, Status
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Target type badge
                val (typeColor, typeIcon) = when (report.targetType) {
                    "POST" -> Color(0xFF667EEA) to Icons.Default.Article
                    "USER" -> Color(0xFF11998E) to Icons.Default.Person
                    "GROUP" -> Color(0xFF4FACFE) to Icons.Default.Group
                    else -> Color.Gray to Icons.Default.Help
                }
                
                Surface(
                    color = typeColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            typeIcon,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = typeColor
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = report.targetType,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = typeColor
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = dateFormat.format(report.createdAt.toDate()),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                ModernReportStatusBadge(report.status)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Reported Target Card (clickable)
            ModernReportedTargetCard(
                report = report,
                onClick = onTargetClick
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Report Reason
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Report,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = report.reason,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    
                    if (report.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = report.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Reporter info
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.PersonOutline,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "Reported by: ${report.reporterName}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (isPending) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))

                // ROW 1: Common Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Close, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Dismiss", style = MaterialTheme.typography.labelSmall)
                    }

                    Button(
                        onClick = onWarning,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = ColorWarning),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Warning, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
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
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Block, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Ban User", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        "POST" -> {
                            Button(
                                onClick = onHidePost,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF667EEA)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.VisibilityOff, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Hide", style = MaterialTheme.typography.labelSmall)
                            }
                            Button(
                                onClick = onDeletePost,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Delete, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Delete", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        "GROUP" -> {
                            Button(
                                onClick = onBanGroup,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Block, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Ban Group", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModernReportedTargetCard(
    report: Report,
    onClick: () -> Unit
) {
    val (bgColor, icon) = when (report.targetType) {
        "USER" -> Color(0xFF11998E).copy(alpha = 0.1f) to Icons.Default.Person
        "GROUP" -> Color(0xFF4FACFE).copy(alpha = 0.1f) to Icons.Default.Group
        "POST" -> Color(0xFF667EEA).copy(alpha = 0.1f) to Icons.Default.Article
        else -> Color.Gray.copy(alpha = 0.1f) to Icons.Default.Help
    }
    
    val iconColor = when (report.targetType) {
        "USER" -> Color(0xFF11998E)
        "GROUP" -> Color(0xFF4FACFE)
        "POST" -> Color(0xFF667EEA)
        else -> Color.Gray
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(10.dp),
                color = iconColor.copy(alpha = 0.15f)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.padding(10.dp),
                    tint = iconColor
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when (report.targetType) {
                        "USER" -> "User Profile"
                        "GROUP" -> "Group"
                        "POST" -> "Post"
                        else -> "Target"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "ID: ${report.targetId.take(12)}...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "View details",
                tint = iconColor
            )
        }
    }
}

@Composable
fun ModernReportStatusBadge(status: ReportStatus) {
    val (color, icon) = when (status) {
        ReportStatus.PENDING -> ColorPending to Icons.Default.Schedule
        ReportStatus.RESOLVED -> ColorResolved to Icons.Default.CheckCircle
        ReportStatus.DISMISSED -> ColorDismissed to Icons.Default.Close
    }
    
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = color
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = status.name,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}
