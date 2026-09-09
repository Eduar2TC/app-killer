package com.appcontrol.feature.applications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.appcontrol.core.common.formatTimeAgo
import com.appcontrol.domain.model.AppStatus
import com.appcontrol.domain.model.HistoryEvent
import com.appcontrol.domain.model.HistoryEventType
import com.appcontrol.feature.navigation.AppDependencies
import com.appcontrol.feature.navigation.Screen
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailScreen(
    navController: NavHostController,
    deps: AppDependencies,
    packageName: String
) {
    val viewModel: AppDetailViewModel = viewModel(
        key = "app_detail_$packageName",
        factory = AppDetailViewModel.Factory(
            packageName,
            deps.appRepository,
            deps.policyRepository,
            deps.getHistory,
            deps.stopApp,
            deps.context
        )
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("App details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (state.isLoading) {
                item {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else {
                state.appInfo?.let { app ->
                    item {
                        AppHeaderCard(app = app)
                    }
                    item {
                        ActionCard(
                            policyEnabled = state.policy?.monitorEnabled ?: true,
                            isExcluded = app.isExcluded,
                            isStopping = state.isStopping,
                            lastMessage = state.lastMessage,
                            onToggleMonitoring = { viewModel.toggleMonitoring() },
                            onAddToProfile = { navController.navigate(Screen.Profiles.route) },
                            onOpenApp = { viewModel.openApp() },
                            onOpenAppInfo = { viewModel.openAppInfo() },
                            onToggleExclusion = { viewModel.exclude() },
                            onStopApp = { viewModel.stopApp() }
                        )
                    }
                }
                item {
                    SectionTitle("Recent activity")
                }
                if (state.recentEvents.isEmpty()) {
                    item {
                        Card(Modifier.fillMaxWidth()) {
                            Text(
                                text = "No recent events for this app",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(state.recentEvents, key = { it.id }) { event ->
                        HistoryEventCard(event)
                    }
                }
            }
        }
    }
}

@Composable
private fun AppHeaderCard(app: com.appcontrol.domain.model.AppInfo) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val initial = app.label.firstOrNull()?.uppercase() ?: "?"
            val colors = listOf(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.secondary,
                MaterialTheme.colorScheme.tertiary
            )
            val avatarColor = colors[abs(app.packageName.hashCode()) % colors.size]
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(avatarColor.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = avatarColor
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Status: ${statusLabel(app.status)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = statusColor(app.status)
                )
                if (app.lastActivityTime > 0) {
                    Text(
                        text = "Last activity: ${app.lastActivityTime.formatTimeAgo()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = if (app.isSystemApp) "System app" else "User app",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ActionCard(
    policyEnabled: Boolean,
    isExcluded: Boolean,
    isStopping: Boolean,
    lastMessage: String?,
    onToggleMonitoring: () -> Unit,
    onAddToProfile: () -> Unit,
    onOpenApp: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onToggleExclusion: () -> Unit,
    onStopApp: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onStopApp,
                enabled = !isStopping,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Warning, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (isStopping) "Stopping..." else "Stop App")
            }
            if (lastMessage != null) {
                Text(
                    text = lastMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Button(
                onClick = onToggleMonitoring,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (policyEnabled) "Deactivate monitoring" else "Activate monitoring")
            }
            OutlinedButton(
                onClick = onAddToProfile,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Person, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Add to profile")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onOpenApp, modifier = Modifier.weight(1f)) {
                    Text("Open app")
                }
                OutlinedButton(onClick = onOpenAppInfo, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Info, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Info")
                }
            }
            OutlinedButton(
                onClick = onToggleExclusion,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Lock, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (isExcluded) "Include in monitoring" else "Exclude")
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun HistoryEventCard(event: HistoryEvent) {
    Card(modifier = Modifier.fillMaxWidth()) {
        ListItem(
            leadingContent = {
                Icon(
                    eventTypeIcon(event.eventType),
                    contentDescription = null,
                    tint = eventTypeColor(event.eventType)
                )
            },
            headlineContent = {
                Text(text = event.title, fontWeight = FontWeight.Bold)
            },
            supportingContent = {
                Column {
                    Text(text = event.description)
                    Text(
                        text = event.timestamp.formatTimeAgo(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )
    }
}

private fun statusLabel(status: AppStatus): String = when (status) {
    AppStatus.UNKNOWN -> "Unknown"
    AppStatus.INACTIVE -> "Inactive"
    AppStatus.ACTIVE -> "Active"
    AppStatus.REAPPEARED -> "Reappeared"
    AppStatus.EXCLUDED -> "Excluded"
    AppStatus.NOT_SUPPORTED -> "Not supported"
}

@Composable
private fun statusColor(status: AppStatus) = when (status) {
    AppStatus.ACTIVE, AppStatus.REAPPEARED -> MaterialTheme.colorScheme.error
    AppStatus.EXCLUDED, AppStatus.NOT_SUPPORTED -> MaterialTheme.colorScheme.outline
    AppStatus.INACTIVE, AppStatus.UNKNOWN -> MaterialTheme.colorScheme.primary
}

private fun eventTypeIcon(type: HistoryEventType) = when (type) {
    HistoryEventType.CHECK -> Icons.Default.DateRange
    HistoryEventType.REAPPEARED -> Icons.Default.Warning
    HistoryEventType.ACTION -> Icons.Default.Check
    HistoryEventType.ERROR -> Icons.Default.Warning
    HistoryEventType.PERMISSION -> Icons.Default.Lock
}

@Composable
private fun eventTypeColor(type: HistoryEventType) = when (type) {
    HistoryEventType.CHECK -> MaterialTheme.colorScheme.primary
    HistoryEventType.REAPPEARED, HistoryEventType.ERROR -> MaterialTheme.colorScheme.error
    HistoryEventType.ACTION -> MaterialTheme.colorScheme.tertiary
    HistoryEventType.PERMISSION -> MaterialTheme.colorScheme.secondary
}