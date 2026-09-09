package com.appcontrol.feature.profiles

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.appcontrol.core.common.padWithZero
import com.appcontrol.domain.model.AppInfo
import com.appcontrol.domain.model.Profile
import com.appcontrol.feature.navigation.AppDependencies
import com.appcontrol.feature.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileDetailScreen(
    navController: NavHostController,
    deps: AppDependencies,
    profileId: Long
) {
    val viewModel: ProfileDetailViewModel = viewModel(
        key = "profile_detail_$profileId",
        factory = ProfileDetailViewModel.Factory(
            profileId,
            deps.manageProfile,
            deps.getInstalledApps
        )
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    var availableSearch by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Edit profile") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (state.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    ProfileSection(
                        title = "Profile",
                        content = {
                            OutlinedTextField(
                                value = state.profile.name,
                                onValueChange = { viewModel.updateName(it) },
                                label = { Text("Name") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Enabled", style = MaterialTheme.typography.bodyLarge)
                                Spacer(Modifier.weight(1f))
                                Switch(
                                    checked = state.profile.enabled,
                                    onCheckedChange = { viewModel.toggleEnabled() }
                                )
                            }
                        }
                    )
                }
                item {
                    ProfileSection(
                        title = "Schedule",
                        content = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Enable schedule", style = MaterialTheme.typography.bodyLarge)
                                Spacer(Modifier.weight(1f))
                                Switch(
                                    checked = state.profile.scheduleEnabled,
                                    onCheckedChange = { viewModel.toggleSchedule() }
                                )
                            }
                            if (state.profile.scheduleEnabled) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.DateRange,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("Start time", Modifier.weight(1f))
                                    TextButton(onClick = { showStartPicker = true }) {
                                        Text(
                                            (state.profile.startTime ?: "22:00"),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.DateRange,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("End time", Modifier.weight(1f))
                                    TextButton(onClick = { showEndPicker = true }) {
                                        Text(
                                            (state.profile.endTime ?: "07:00"),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Text(
                                    text = "Monitoring interval",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(listOf(5, 15, 30, 60)) { minutes ->
                                        FilterChip(
                                            selected = state.profile.monitoringInterval == minutes * 60_000L,
                                            onClick = { viewModel.updateInterval(minutes) },
                                            label = { Text("${minutes} min") }
                                        )
                                    }
                                }
                            }
                        }
                    )
                }
                item {
                    ProfileSection(
                        title = "Notifications",
                        content = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Enable notifications", Modifier.weight(1f))
                                Switch(
                                    checked = state.profile.notificationEnabled,
                                    onCheckedChange = { viewModel.toggleNotification() }
                                )
                            }
                        }
                    )
                }
                item {
                    ProfileSection(
                        title = "Apps in profile (${state.profileApps.size})",
                        content = {
                            if (state.profileApps.isEmpty()) {
                                Text(
                                    text = "No apps yet. Add apps from the list below.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    state.profileApps.forEach { app ->
                                        AppRow(
                                            app = app,
                                            trailing = {
                                                IconButton(onClick = { viewModel.removeApp(app.packageName) }) {
                                                    Icon(
                                                        Icons.Default.Close,
                                                        contentDescription = "Remove ${app.label}",
                                                        tint = MaterialTheme.colorScheme.error
                                                    )
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    )
                }
                item {
                    ProfileSection(
                        title = "Add apps",
                        content = {
                            OutlinedTextField(
                                value = availableSearch,
                                onValueChange = { availableSearch = it },
                                label = { Text("Search apps") },
                                leadingIcon = {
                                    Icon(Icons.Default.Search, contentDescription = null)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Spacer(Modifier.height(8.dp))
                            val filtered = state.availableApps.filter {
                                availableSearch.isBlank() ||
                                    it.label.contains(availableSearch, ignoreCase = true) ||
                                    it.packageName.contains(availableSearch, ignoreCase = true)
                            }
                            if (filtered.isEmpty()) {
                                Text(
                                    text = "No matching apps",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    filtered.take(30).forEach { app ->
                                        AppRow(
                                            app = app,
                                            trailing = {
                                                IconButton(onClick = { viewModel.addApp(app.packageName) }) {
                                                    Icon(
                                                        Icons.Default.Add,
                                                        contentDescription = "Add ${app.label}"
                                                    )
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    if (showStartPicker) {
        TimePickerDialog(
            title = "Start time",
            initialHour = state.profile.startTime?.timePartHour() ?: 22,
            initialMinute = state.profile.startTime?.timePartMinute() ?: 0,
            onDismiss = { showStartPicker = false },
            onConfirm = { hour, minute ->
                val start = formatTime(hour, minute)
                val end = state.profile.endTime ?: "07:00"
                viewModel.updateSchedule(start, end)
            }
        )
    }

    if (showEndPicker) {
        TimePickerDialog(
            title = "End time",
            initialHour = state.profile.endTime?.timePartHour() ?: 7,
            initialMinute = state.profile.endTime?.timePartMinute() ?: 0,
            onDismiss = { showEndPicker = false },
            onConfirm = { hour, minute ->
                val start = state.profile.startTime ?: "22:00"
                val end = formatTime(hour, minute)
                viewModel.updateSchedule(start, end)
            }
        )
    }
}

@Composable
private fun ProfileSection(
    title: String,
    content: @Composable () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            content()
        }
    }
}

@Composable
private fun AppRow(
    app: AppInfo,
    trailing: @Composable () -> Unit
) {
    ListItem(
        headlineContent = { Text(app.label, fontWeight = FontWeight.Bold) },
        supportingContent = { Text(app.packageName) },
        trailingContent = { trailing() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    title: String,
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    val timeState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            TimePicker(state = timeState)
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(timeState.hour, timeState.minute)
                onDismiss()
            }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatTime(hour: Int, minute: Int): String =
    "${hour.padWithZero()}:${minute.padWithZero()}"

private fun String.timePartHour(): Int =
    substringBefore(":").toIntOrNull() ?: 0

private fun String.timePartMinute(): Int =
    substringAfter(":").toIntOrNull() ?: 0