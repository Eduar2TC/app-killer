package com.appcontrol.feature.automation

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.appcontrol.core.common.padWithZero
import com.appcontrol.feature.navigation.AppDependencies

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutomationScreen(
    navController: NavHostController,
    deps: AppDependencies
) {
    val viewModel: AutomationViewModel = viewModel(
        factory = AutomationViewModel.Factory(
            deps.preferencesManager,
            deps.scheduleMonitoring,
            deps.checkNightSchedule
        )
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Automation") },
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
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = "Night protection",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Monitor selected apps during night hours",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = state.nightProtectionEnabled,
                                onCheckedChange = { viewModel.setNightProtection(it) }
                            )
                        }
                        HorizontalDivider()
                        StatusIndicator(
                            enabled = state.nightProtectionEnabled,
                            isActive = state.isActive
                        )
                    }
                }
            }
            if (state.nightProtectionEnabled) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Schedule",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
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
                                        formatTime(state.startHour, state.startMinute),
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
                                        formatTime(state.endHour, state.endMinute),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = "Monitoring interval",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(listOf(5, 15, 30, 60)) { minutes ->
                                    FilterChip(
                                        selected = state.intervalMinutes == minutes,
                                        onClick = { viewModel.setInterval(minutes) },
                                        label = { Text("${minutes} min") }
                                    )
                                }
                            }
                            Button(
                                onClick = { viewModel.applySchedule() },
                                enabled = !state.isRefreshing,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (state.isRefreshing) "Applying..." else "Apply schedule")
                            }
                        }
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "How scheduling works",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "App Control uses WorkManager to run periodic monitoring jobs. " +
                                "Selecting a short interval keeps monitoring fresh but may increase battery " +
                                "usage. Exact alarms are scheduled when night protection is enabled, so the " +
                                "app can check your selected apps at the configured times.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showStartPicker) {
        TimePickerDialog(
            title = "Start time",
            initialHour = state.startHour,
            initialMinute = state.startMinute,
            onDismiss = { showStartPicker = false },
            onConfirm = { hour, minute ->
                viewModel.setStartTime(hour, minute)
            }
        )
    }

    if (showEndPicker) {
        TimePickerDialog(
            title = "End time",
            initialHour = state.endHour,
            initialMinute = state.endMinute,
            onDismiss = { showEndPicker = false },
            onConfirm = { hour, minute ->
                viewModel.setEndTime(hour, minute)
            }
        )
    }
}

@Composable
private fun StatusIndicator(enabled: Boolean, isActive: Boolean) {
    val active = enabled && isActive
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(
                    if (active) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                )
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = when {
                !enabled -> "Disabled"
                isActive -> "Active now"
                else -> "Idle"
            },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (active) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
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