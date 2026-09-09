package com.appcontrol.feature.dashboard

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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.appcontrol.core.common.formatTimeAgo
import com.appcontrol.domain.model.AppInfo
import com.appcontrol.domain.model.AppStatus
import com.appcontrol.feature.navigation.AppDependencies
import com.appcontrol.feature.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavHostController,
    deps: AppDependencies
) {
    val viewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModel.Factory(
            deps.getSelectedApps,
            deps.monitorAppActivity,
            deps.processSelectedApps,
            deps.checkNightSchedule,
            deps.manageProfile,
            deps.preferencesManager
        )
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "APP CONTROL",
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = state.isLoading,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    HeaderCard(
                        profileName = state.profileName,
                        nightProtectionActive = state.nightProtectionActive,
                        onNavigateProfiles = { navController.navigate(Screen.Profiles.route) },
                        onNavigateHistory = { navController.navigate(Screen.History.route) },
                        onNavigateSettings = { navController.navigate(Screen.Settings.route) }
                    )
                }
                item {
                    ControlCard(state = state, onProcess = { viewModel.processApps() })
                }
                if (state.activeApps.isNotEmpty()) {
                    item {
                        SectionTitle("Active apps")
                    }
                    items(state.activeApps, key = { it.packageName }) { app ->
                        AppStatusItem(app = app, revived = false)
                    }
                }
                if (state.recentlyRevivedApps.isNotEmpty()) {
                    item {
                        SectionTitle("Recently revived")
                    }
                    items(state.recentlyRevivedApps, key = { it.packageName }) { app ->
                        AppStatusItem(app = app, revived = true)
                    }
                }
                item {
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun HeaderCard(
    profileName: String,
    nightProtectionActive: Boolean,
    onNavigateProfiles: () -> Unit,
    onNavigateHistory: () -> Unit,
    onNavigateSettings: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Active profile: $profileName",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (nightProtectionActive) "Night protection is active" else "Night protection is inactive",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (nightProtectionActive) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickNavButton(label = "Profiles", onClick = onNavigateProfiles, modifier = Modifier.weight(1f))
                QuickNavButton(label = "History", onClick = onNavigateHistory, modifier = Modifier.weight(1f))
                QuickNavButton(label = "Settings", onClick = onNavigateSettings, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun QuickNavButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(onClick = onClick, modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ControlCard(
    state: DashboardUiState,
    onProcess: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "${state.selectedAppsCount} apps monitored",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = onProcess,
                enabled = !state.isProcessing,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (state.isProcessing) "Stopping..." else "Stop Apps",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            if (state.lastMessage != null) {
                Text(
                    text = state.lastMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            HorizontalDivider()
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(
                    color = if (state.nightProtectionActive) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (state.nightProtectionActive) "Night protection active" else "Night protection inactive",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(
                text = "Next check: ${state.nextCheckTime}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
private fun AppStatusItem(app: AppInfo, revived: Boolean) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.DateRange,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = app.label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (revived) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = "Revived",
                        tint = MaterialTheme.colorScheme.error
                    )
                } else {
                    StatusDot(
                        color = statusColor(app.status)
                    )
                }
            }
            if (app.lastActivityTime > 0) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Last activity: ${app.lastActivityTime.formatTimeAgo()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatusDot(color: Color) {
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
private fun statusColor(status: AppStatus): Color = when (status) {
    AppStatus.ACTIVE, AppStatus.REAPPEARED -> MaterialTheme.colorScheme.error
    AppStatus.EXCLUDED -> MaterialTheme.colorScheme.outline
    AppStatus.NOT_SUPPORTED -> MaterialTheme.colorScheme.tertiary
    AppStatus.INACTIVE, AppStatus.UNKNOWN -> MaterialTheme.colorScheme.primary
}