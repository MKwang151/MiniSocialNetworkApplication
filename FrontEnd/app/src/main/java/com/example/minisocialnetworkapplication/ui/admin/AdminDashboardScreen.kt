package com.example.minisocialnetworkapplication.ui.admin

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.minisocialnetworkapplication.core.domain.repository.AdminStats
import com.example.minisocialnetworkapplication.core.domain.repository.DailyStat

// Modern color palette
private val GradientPrimary = listOf(Color(0xFF667EEA), Color(0xFF764BA2))
private val GradientSuccess = listOf(Color(0xFF11998E), Color(0xFF38EF7D))
private val GradientWarning = listOf(Color(0xFFF093FB), Color(0xFFF5576C))
private val GradientInfo = listOf(Color(0xFF4FACFE), Color(0xFF00F2FE))
private val GradientDanger = listOf(Color(0xFFFF416C), Color(0xFFFF4B2B))
private val GradientPurple = listOf(Color(0xFFA770EF), Color(0xFFCF8BF3), Color(0xFFFDB99B))

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
                title = { 
                    Column {
                        Text(
                            "Admin Dashboard",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Text(
                            "Platform Overview",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(
                            Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Logout",
                            tint = MaterialTheme.colorScheme.error
                        )
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
            when (val state = uiState) {
                is AdminDashboardUiState.Loading -> {
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
                            "Loading dashboard...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                is AdminDashboardUiState.Success -> {
                    ModernDashboardContent(
                        stats = state.stats,
                        onNavigateToUserManagement = onNavigateToUserManagement,
                        onNavigateToContentModeration = onNavigateToContentModeration,
                        onNavigateToReportManagement = onNavigateToReportManagement,
                        onNavigateToGroupManagement = onNavigateToGroupManagement
                    )
                }
                is AdminDashboardUiState.Error -> {
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

@Composable
fun ModernDashboardContent(
    stats: AdminStats,
    onNavigateToUserManagement: () -> Unit,
    onNavigateToContentModeration: () -> Unit,
    onNavigateToReportManagement: () -> Unit,
    onNavigateToGroupManagement: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Stats Overview Cards - Horizontal Scroll
        SectionHeader(
            title = "Overview",
            subtitle = "Real-time platform statistics"
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            item {
                GradientStatCard(
                    title = "Total Users",
                    value = stats.totalUsers.toString(),
                    subtitle = "+${stats.newUsersToday} today",
                    icon = Icons.Default.People,
                    gradient = GradientPrimary
                )
            }
            item {
                GradientStatCard(
                    title = "Total Posts",
                    value = stats.totalPosts.toString(),
                    subtitle = "+${stats.postsToday} today",
                    icon = Icons.Default.Article,
                    gradient = GradientSuccess
                )
            }
            item {
                GradientStatCard(
                    title = "Groups",
                    value = stats.totalGroups.toString(),
                    subtitle = "${stats.activeGroups} active",
                    icon = Icons.Default.Groups,
                    gradient = GradientInfo
                )
            }
            item {
                GradientStatCard(
                    title = "Reports",
                    value = stats.totalReports.toString(),
                    subtitle = "+${stats.reportsToday} today",
                    icon = Icons.Default.Report,
                    gradient = GradientDanger
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Quick Stats Grid
        SectionHeader(
            title = "Today's Activity",
            subtitle = "What's happening right now"
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MiniStatCard(
                modifier = Modifier.weight(1f),
                title = "New Users",
                value = stats.newUsersToday.toString(),
                icon = Icons.Default.PersonAdd,
                color = Color(0xFF667EEA)
            )
            MiniStatCard(
                modifier = Modifier.weight(1f),
                title = "New Posts",
                value = stats.postsToday.toString(),
                icon = Icons.Default.PostAdd,
                color = Color(0xFF11998E)
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MiniStatCard(
                modifier = Modifier.weight(1f),
                title = "New Reports",
                value = stats.reportsToday.toString(),
                icon = Icons.Default.Flag,
                color = Color(0xFFFF416C)
            )
            MiniStatCard(
                modifier = Modifier.weight(1f),
                title = "Banned",
                value = stats.bannedUsers.toString(),
                icon = Icons.Default.Block,
                color = Color(0xFFFF4B2B)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // User Growth Chart
        SectionHeader(
            title = "User Growth",
            subtitle = "Last 7 days trend"
        )
        
        ModernLineChart(
            data = stats.userGrowthTrend,
            gradientColors = GradientPrimary,
            modifier = Modifier.height(180.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Post Activity Chart
        SectionHeader(
            title = "Post Activity",
            subtitle = "Content creation trends"
        )
        
        ModernLineChart(
            data = stats.postGrowthTrend,
            gradientColors = GradientSuccess,
            modifier = Modifier.height(180.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // User Distribution
        SectionHeader(
            title = "User Status",
            subtitle = "Active vs Banned users"
        )
        
        ModernDonutChart(
            activeUsers = (stats.totalUsers - stats.bannedUsers).toInt(),
            bannedUsers = stats.bannedUsers.toInt()
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Quick Actions
        SectionHeader(
            title = "Quick Actions",
            subtitle = "Manage your platform"
        )
        
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(bottom = 32.dp)
        ) {
            ModernActionButton(
                title = "User Management",
                subtitle = "Manage users & permissions",
                icon = Icons.Default.People,
                gradient = GradientPrimary,
                onClick = onNavigateToUserManagement
            )
            ModernActionButton(
                title = "Content Moderation",
                subtitle = "Review & moderate content",
                icon = Icons.Default.Gavel,
                gradient = GradientSuccess,
                onClick = onNavigateToContentModeration
            )
            ModernActionButton(
                title = "Report Management",
                subtitle = "Handle user reports",
                icon = Icons.Default.ReportProblem,
                gradient = GradientWarning,
                onClick = onNavigateToReportManagement
            )
            ModernActionButton(
                title = "Group Management",
                subtitle = "Manage communities",
                icon = Icons.Default.GroupWork,
                gradient = GradientInfo,
                onClick = onNavigateToGroupManagement
            )
        }
    }
}

@Composable
fun SectionHeader(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun GradientStatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    gradient: List<Color>
) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .shadow(8.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .background(Brush.linearGradient(gradient))
                .padding(20.dp)
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.TrendingUp,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun MiniStatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ModernLineChart(
    data: List<DailyStat>,
    gradientColors: List<Color>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return
    
    val maxVal = data.maxOf { it.count }.toFloat().coerceAtLeast(1f)
    
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.padding(20.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val spaceBetweenPoints = width / (data.size - 1).coerceAtLeast(1)
                
                // Draw gradient area under the line
                val areaPath = Path().apply {
                    moveTo(0f, height)
                    data.forEachIndexed { index, stat ->
                        val x = index * spaceBetweenPoints
                        val y = height - (stat.count / maxVal) * height * 0.9f
                        lineTo(x, y)
                    }
                    lineTo(width, height)
                    close()
                }
                
                drawPath(
                    path = areaPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            gradientColors[0].copy(alpha = 0.3f),
                            gradientColors[0].copy(alpha = 0.05f)
                        )
                    )
                )
                
                // Draw the line
                val linePath = Path()
                data.forEachIndexed { index, stat ->
                    val x = index * spaceBetweenPoints
                    val y = height - (stat.count / maxVal) * height * 0.9f
                    
                    if (index == 0) {
                        linePath.moveTo(x, y)
                    } else {
                        linePath.lineTo(x, y)
                    }
                }
                
                drawPath(
                    path = linePath,
                    brush = Brush.horizontalGradient(gradientColors),
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                )
                
                // Draw points
                data.forEachIndexed { index, stat ->
                    val x = index * spaceBetweenPoints
                    val y = height - (stat.count / maxVal) * height * 0.9f
                    
                    // Outer circle (white border)
                    drawCircle(
                        color = Color.White,
                        radius = 8.dp.toPx(),
                        center = Offset(x, y)
                    )
                    
                    // Inner circle (gradient color)
                    drawCircle(
                        color = gradientColors[0],
                        radius = 5.dp.toPx(),
                        center = Offset(x, y)
                    )
                }
            }
        }
    }
}

@Composable
fun ModernDonutChart(
    activeUsers: Int,
    bannedUsers: Int
) {
    val total = (activeUsers + bannedUsers).toFloat().coerceAtLeast(1f)
    val animatedProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "donut"
    )
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(140.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val sweepActive = (activeUsers / total) * 360f * animatedProgress
                    val sweepBanned = (bannedUsers / total) * 360f * animatedProgress
                    
                    // Active users arc
                    drawArc(
                        brush = Brush.sweepGradient(GradientSuccess),
                        startAngle = -90f,
                        sweepAngle = sweepActive,
                        useCenter = false,
                        style = Stroke(width = 28.dp.toPx(), cap = StrokeCap.Round)
                    )
                    
                    // Banned users arc
                    drawArc(
                        brush = Brush.sweepGradient(GradientDanger),
                        startAngle = -90f + sweepActive,
                        sweepAngle = sweepBanned,
                        useCenter = false,
                        style = Stroke(width = 28.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${((activeUsers / total) * 100).toInt()}%",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Active",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(32.dp))
            
            Column {
                LegendItem(
                    color = GradientSuccess[0],
                    label = "Active Users",
                    value = activeUsers.toString()
                )
                Spacer(modifier = Modifier.height(16.dp))
                LegendItem(
                    color = GradientDanger[0],
                    label = "Banned Users",
                    value = bannedUsers.toString()
                )
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ModernActionButton(
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradient: List<Color>,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
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
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Brush.linearGradient(gradient)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// Data classes
data class ChartData(
    val label: String,
    val value: Float,
    val color: Color
)
