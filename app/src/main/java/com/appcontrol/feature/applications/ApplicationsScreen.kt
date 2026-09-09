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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.appcontrol.domain.model.AppInfo
import com.appcontrol.domain.model.AppStatus
import com.appcontrol.feature.navigation.AppDependencies
import com.appcontrol.feature.navigation.Screen
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplicationsScreen(
    navController: NavHostController,
    deps: AppDependencies
) {
    val viewModel: ApplicationsViewModel = viewModel(
        factory = ApplicationsViewModel.Factory(
            deps.getInstalledApps,
            deps.toggleAppSelection,
            deps.toggleAppExclusion,
            deps.appRepository
        )
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Applications") })
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.search(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search apps") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(onClick = { viewModel.selectAll() }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Select all")
                }
                OutlinedButton(onClick = { viewModel.deselectAll() }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Close, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Deselect all")
                }
            }
            Text(
                text = "${state.selectedCount} selected",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(AppFilter.entries) { filter ->
                    FilterChip(
                        selected = state.filter == filter,
                        onClick = { viewModel.setFilter(filter) },
                        label = { Text(filter.name) }
                    )
                }
            }
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(AppSort.entries) { sort ->
                    FilterChip(
                        selected = state.sortBy == sort,
                        onClick = { viewModel.setSort(sort) },
                        label = { Text(if (sort == AppSort.NAME) "Name" else "Last activity") }
                    )
                }
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(state.displayedApps, key = { it.packageName }) { app ->
                    AppListItem(
                        app = app,
                        onToggleSelection = { viewModel.toggleSelection(app.packageName) },
                        onToggleExclusion = { viewModel.toggleExclusion(app.packageName) },
                        onClick = { navController.navigate(Screen.ApplicationDetail.createRoute(app.packageName)) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppListItem(
    app: AppInfo,
    onToggleSelection: () -> Unit,
    onToggleExclusion: () -> Unit,
    onClick: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 4.dp)) {
        ListItem(
            onClick = onClick,
            leadingContent = {
                AppIcon(app = app)
            },
            headlineContent = {
                Text(text = app.label, fontWeight = FontWeight.Bold)
            },
            supportingContent = {
                Column {
                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = statusLabel(app.status),
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor(app.status)
                    )
                }
            },
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = app.isSelected,
                        onCheckedChange = { onToggleSelection() }
                    )
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text(if (app.isExcluded) "Include in monitoring" else "Exclude from monitoring") },
                                onClick = {
                                    menuExpanded = false
                                    onToggleExclusion()
                                }
                            )
                        }
                    }
                }
            }
        )
    }
}

@Composable
private fun AppIcon(app: AppInfo) {
    val initial = app.label.firstOrNull()?.uppercase() ?: "?"
    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary
    )
    val color = colors[abs(app.packageName.hashCode()) % colors.size]
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.25f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            color = color,
            fontWeight = FontWeight.Bold
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
private fun statusColor(status: AppStatus): Color = when (status) {
    AppStatus.ACTIVE, AppStatus.REAPPEARED -> MaterialTheme.colorScheme.error
    AppStatus.EXCLUDED, AppStatus.NOT_SUPPORTED -> MaterialTheme.colorScheme.outline
    AppStatus.INACTIVE, AppStatus.UNKNOWN -> MaterialTheme.colorScheme.primary
}