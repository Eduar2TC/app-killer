package com.appcontrol.feature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.appcontrol.core.common.formatTimeAgo
import com.appcontrol.core.time.TimeUtils
import com.appcontrol.domain.model.HistoryEvent
import com.appcontrol.domain.model.HistoryEventType
import com.appcontrol.domain.usecase.StatisticsResult
import com.appcontrol.feature.navigation.AppDependencies

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    navController: NavHostController,
    deps: AppDependencies
) {
    val viewModel: HistoryViewModel = viewModel(
        factory = HistoryViewModel.Factory(
            deps.getHistory,
            deps.getStatistics,
            deps.cleanupHistory
        )
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showCleanupDialog by remember { mutableStateOf(false) }

    val grouped = remember(state.events) {
        state.events.groupBy { TimeUtils.formatDay(it.timestamp) }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("History") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showCleanupDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clean up history")
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                StatsSection(stats = state.stats)
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(HistoryTimeFilter.entries) { filter ->
                        FilterChip(
                            selected = state.filter == filter,
                            onClick = { viewModel.setFilter(filter) },
                            label = { Text(filter.label) }
                        )
                    }
                }
            }
            if (state.events.isEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "No events for the selected period",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
            for ((day, events) in grouped) {
                item(key = "header_$day") {
                    Text(
                        text = day,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                items(events, key = { it.id }) { event ->
                    HistoryEventCard(event = event)
                }
            }
            item {
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    if (showCleanupDialog) {
        AlertDialog(
            onDismissRequest = { showCleanupDialog = false },
            title = { Text("Clean up history") },
            text = { Text("This will permanently delete all history and activity events.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.cleanup()
                    showCleanupDialog = false
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCleanupDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun StatsSection(stats: StatisticsResult?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard(
            label = "Monitored",
            value = stats?.totalMonitoredApps?.toString() ?: "-",
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "Weekly events",
            value = stats?.totalEvents?.toString() ?: "-",
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "Most active app",
            value = stats?.perAppStats?.maxByOrNull { it.totalEvents }?.packageName?.substringBeforeLast(".") ?: "-",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
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
                Text(event.title, fontWeight = FontWeight.Bold)
            },
            supportingContent = {
                Column {
                    Text(event.description)
                    Text(
                        text = "${TimeUtils.formatTime(event.timestamp)} · ${event.timestamp.formatTimeAgo()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )
    }
}

private fun eventTypeIcon(type: HistoryEventType) = when (type) {
    HistoryEventType.CHECK -> Icons.Default.DateRange
    HistoryEventType.REAPPEARED -> Icons.Default.Warning
    HistoryEventType.ACTION -> Icons.Default.Check
    HistoryEventType.ERROR -> Icons.Default.Warning
    HistoryEventType.PERMISSION -> Icons.Default.Lock
}

@Composable
private fun eventTypeColor(type: HistoryEventType): Color = when (type) {
    HistoryEventType.CHECK -> MaterialTheme.colorScheme.primary
    HistoryEventType.REAPPEARED, HistoryEventType.ERROR -> MaterialTheme.colorScheme.error
    HistoryEventType.ACTION -> MaterialTheme.colorScheme.tertiary
    HistoryEventType.PERMISSION -> MaterialTheme.colorScheme.secondary
}