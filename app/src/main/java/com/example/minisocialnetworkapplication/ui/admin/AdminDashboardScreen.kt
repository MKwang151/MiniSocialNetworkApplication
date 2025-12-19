package com.example.minisocialnetworkapplication.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.minisocialnetworkapplication.core.domain.repository.AdminStats
import com.example.minisocialnetworkapplication.core.domain.repository.DailyStat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onNavigateToUserManagement: () -> Unit,
    onNavigateToContentModeration: () -> Unit,
    onNavigateToReportManagement: () -> Unit,
    onNavigateToGroupManagement: () -> Unit,
    onLogout: () -> Unit,
    bottomBar: @Composable () -> Unit = {},
    viewModel: AdminDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Dashboard") },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout")
                    }
                }
            )
        },
        bottomBar = bottomBar
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is AdminDashboardUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is AdminDashboardUiState.Success -> {
                    DashboardContent(
                        stats = state.stats,
                        onNavigateToUserManagement = onNavigateToUserManagement,
                        onNavigateToContentModeration = onNavigateToContentModeration,
                        onNavigateToReportManagement = onNavigateToReportManagement,
                        onNavigateToGroupManagement = onNavigateToGroupManagement
                    )
                }
                is AdminDashboardUiState.Error -> {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardContent(
    stats: AdminStats,
    onNavigateToUserManagement: () -> Unit,
    onNavigateToContentModeration: () -> Unit,
    onNavigateToReportManagement: () -> Unit,
    onNavigateToGroupManagement: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Platform Growth",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Platform Growth Cards
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(modifier = Modifier.weight(1f)) { StatCard("Total Users", stats.totalUsers.toString(), Icons.Default.Person) }
                Box(modifier = Modifier.weight(1f)) { StatCard("New Today", stats.newUsersToday.toString(), Icons.Default.PersonAdd, containerColor = MaterialTheme.colorScheme.primaryContainer) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(modifier = Modifier.weight(1f)) { StatCard("Total Posts", stats.totalPosts.toString(), Icons.Default.Description) }
                Box(modifier = Modifier.weight(1f)) { StatCard("Posts Today", stats.postsToday.toString(), Icons.Default.PostAdd, containerColor = MaterialTheme.colorScheme.secondaryContainer) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(modifier = Modifier.weight(1f)) { StatCard("Total Groups", stats.totalGroups.toString(), Icons.Default.Groups) }
                Box(modifier = Modifier.weight(1f)) { StatCard("Groups Today", stats.groupsToday.toString(), Icons.Default.GroupAdd, containerColor = MaterialTheme.colorScheme.tertiaryContainer) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(modifier = Modifier.weight(1f)) { StatCard("Total Reports", stats.totalReports.toString(), Icons.Default.Report, containerColor = MaterialTheme.colorScheme.errorContainer) }
                Box(modifier = Modifier.weight(1f)) { StatCard("Reports Today", stats.reportsToday.toString(), Icons.Default.ReportProblem, containerColor = MaterialTheme.colorScheme.errorContainer) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(modifier = Modifier.weight(1f)) { StatCard("Active Groups", stats.activeGroups.toString(), Icons.Default.GroupWork, containerColor = MaterialTheme.colorScheme.surfaceVariant) }
                Box(modifier = Modifier.weight(1f)) { StatCard("Banned Users", stats.bannedUsers.toString(), Icons.Default.Block, containerColor = MaterialTheme.colorScheme.outlineVariant) }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Today's Pulse",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        SimpleBarChart(
            data = listOf(
                ChartData("New Users", stats.newUsersToday.toFloat(), MaterialTheme.colorScheme.primary),
                ChartData("New Posts", stats.postsToday.toFloat(), MaterialTheme.colorScheme.secondary),
                ChartData("New Reports", stats.reportsToday.toFloat(), MaterialTheme.colorScheme.error),
                ChartData("New Groups", stats.groupsToday.toFloat(), MaterialTheme.colorScheme.tertiary)
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Global Scale",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        SimpleBarChart(
            data = listOf(
                ChartData("Users", stats.totalUsers.toFloat(), MaterialTheme.colorScheme.primary),
                ChartData("Posts", stats.totalPosts.toFloat(), MaterialTheme.colorScheme.secondary),
                ChartData("Reports", stats.totalReports.toFloat(), MaterialTheme.colorScheme.error),
                ChartData("Groups", stats.totalGroups.toFloat(), MaterialTheme.colorScheme.tertiary)
            )
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "User Distribution",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        SimplePieChart(
            data = listOf(
                ChartData("Active", (stats.totalUsers - stats.bannedUsers).toFloat(), MaterialTheme.colorScheme.primary),
                ChartData("Banned", stats.bannedUsers.toFloat(), MaterialTheme.colorScheme.error)
            )
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "User Growth (7 Days)",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        SimpleLineChart(
            data = stats.userGrowthTrend,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Post Activity (7 Days)",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        SimpleLineChart(
            data = stats.postGrowthTrend,
            color = MaterialTheme.colorScheme.secondary
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Group Growth (7 Days)",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        SimpleLineChart(
            data = stats.groupGrowthTrend,
            color = MaterialTheme.colorScheme.tertiary
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(bottom = 32.dp)
        ) {
            ManagementButton("User Management", Icons.Default.People, onNavigateToUserManagement)
            ManagementButton("Content Moderation", Icons.Default.Gavel, onNavigateToContentModeration)
            ManagementButton("Report Management", Icons.Default.ReportProblem, onNavigateToReportManagement)
            ManagementButton("Group Management", Icons.Default.GroupWork, onNavigateToGroupManagement)
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    containerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surfaceVariant
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(text = title, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun ManagementButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = text, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

data class ChartData(
    val label: String,
    val value: Float,
    val color: androidx.compose.ui.graphics.Color
)

@Composable
fun SimplePieChart(data: List<ChartData>) {
    val total = data.sumOf { it.value.toDouble() }.toFloat().coerceAtLeast(1f)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Canvas(modifier = Modifier.size(120.dp)) {
                var startAngle = -90f
                data.forEach { item ->
                    val sweepAngle = (item.value / total) * 360f
                    drawArc(
                        color = item.color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = 30f, cap = StrokeCap.Round)
                    )
                    startAngle += sweepAngle
                }
            }
            
            Spacer(modifier = Modifier.width(32.dp))
            
            Column {
                data.forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(2.dp)).background(item.color))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "${item.label}: ${item.value.toInt()}", style = MaterialTheme.typography.labelMedium)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
fun SimpleLineChart(data: List<DailyStat>, color: Color) {
    if (data.isEmpty()) return
    
    val maxVal = data.maxOf { it.count }.toFloat().coerceAtLeast(1f)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val spaceBetweenPoints = width / (data.size - 1).coerceAtLeast(1)
                
                val path = Path()
                data.forEachIndexed { index, stat ->
                    val x = index * spaceBetweenPoints
                    val y = height - (stat.count / maxVal) * height
                    
                    if (index == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                    
                    drawCircle(
                        color = color,
                        radius = 4.dp.toPx(),
                        center = androidx.compose.ui.geometry.Offset(x, y)
                    )
                }
                
                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }
    }
}

@Composable
fun SimpleBarChart(data: List<ChartData>) {
    val maxVal = data.maxOfOrNull { it.value }?.coerceAtLeast(1f) ?: 1f
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            data.forEach { item ->
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = item.label, style = MaterialTheme.typography.labelSmall)
                        Text(text = item.value.toInt().toString(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(item.value / maxVal)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(item.color)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}
