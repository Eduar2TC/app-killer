package com.appcontrol.feature.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.appcontrol.core.common.Constants
import com.appcontrol.feature.navigation.AppDependencies
import com.appcontrol.feature.navigation.Screen
import kotlin.math.roundToInt

private val grantedColor = Color(0xFF4CAF50)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavHostController,
    deps: AppDependencies
) {
    val viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(
            deps.preferencesManager,
            deps.cleanupHistory,
            deps.permissionManager,
            deps.shizukuManager
        )
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.lastMessage) {
        state.lastMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsSection(title = "General") {
                SettingsSwitchRow(
                    label = "Show system apps",
                    checked = state.showSystemApps,
                    onCheckedChange = { viewModel.setShowSystemApps(it) }
                )
                SettingsSwitchRow(
                    label = "Confirm before actions",
                    checked = state.confirmActions,
                    onCheckedChange = { viewModel.setConfirmActions(it) }
                )
                SettingsSwitchRow(
                    label = "Vibration",
                    checked = state.vibration,
                    onCheckedChange = { viewModel.setVibration(it) }
                )
            }

            SettingsSection(title = "Shizuku") {
                ListItem(
                    leadingContent = {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(
                                    if (state.shizukuAvailable) {
                                        grantedColor
                                    } else {
                                        MaterialTheme.colorScheme.error
                                    }
                                )
                        )
                    },
                    headlineContent = { Text("Force-stop with Shizuku") },
                    supportingContent = {
                        Text(
                            if (state.shizukuAvailable) {
                                "Force-stops apps with shell privileges"
                            } else {
                                "Shizuku app not installed"
                            }
                        )
                    },
                    trailingContent = {
                        Switch(
                            checked = state.shizukuEnabled,
                            onCheckedChange = { viewModel.setShizukuEnabled(it) }
                        )
                    }
                )
                ListItem(
                    headlineContent = {
                        Text(if (state.shizukuAvailable) "Shizuku available" else "Shizuku not installed")
                    },
                    supportingContent = {
                        Text(
                            if (state.shizukuVersion > 0) {
                                "Server version ${state.shizukuVersion}"
                            } else {
                                "Download it from shizuku.rikka.app"
                            }
                        )
                    }
                )
                ListItem(
                    leadingContent = {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(
                                    if (state.shizukuPermissionGranted) {
                                        grantedColor
                                    } else {
                                        MaterialTheme.colorScheme.error
                                    }
                                )
                        )
                    },
                    headlineContent = { Text("Shizuku permission") },
                    supportingContent = {
                        Text(if (state.shizukuPermissionGranted) "Granted" else "Not granted")
                    }
                )
                if (state.shizukuAvailable && !state.shizukuPermissionGranted) {
                    Button(
                        onClick = { viewModel.requestShizukuPermission() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Text("Grant permission")
                    }
                }
                if (!state.shizukuAvailable) {
                    OutlinedButton(
                        onClick = { openShizukuApp(deps.context) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Text("Open Shizuku")
                    }
                }
            }

            SettingsSection(title = "Monitoring") {
                Card(
                    onClick = { navController.navigate(Screen.Permissions.route) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ListItem(
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (state.usageStatsPermission) {
                                            grantedColor
                                        } else {
                                            MaterialTheme.colorScheme.error
                                        }
                                    )
                            )
                        },
                        headlineContent = { Text("Usage access permission") },
                        supportingContent = {
                            Text(if (state.usageStatsPermission) "Granted" else "Not granted")
                        }
                    )
                    ListItem(
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (state.notificationPermission) {
                                            grantedColor
                                        } else {
                                            MaterialTheme.colorScheme.error
                                        }
                                    )
                            )
                        },
                        headlineContent = { Text("Notification permission") },
                        supportingContent = {
                            Text(if (state.notificationPermission) "Granted" else "Not granted")
                        }
                    )
                    ListItem(
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (state.exactAlarmPermission) {
                                            grantedColor
                                        } else {
                                            MaterialTheme.colorScheme.error
                                        }
                                    )
                            )
                        },
                        headlineContent = { Text("Exact alarm permission") },
                        supportingContent = {
                            Text(if (state.exactAlarmPermission) "Granted" else "Not granted")
                        }
                    )
                }
                SettingsSwitchRow(
                    label = "Detect app activity",
                    checked = state.detectActivity,
                    onCheckedChange = { viewModel.setDetectActivity(it) }
                )
                SettingsSliderRow(
                    label = "Monitoring interval (${state.monitoringInterval} min)",
                    value = state.monitoringInterval.toFloat(),
                    valueRange = 5f..120f,
                    steps = 22,
                    onValueChange = { viewModel.setMonitoringInterval(it.roundToInt()) }
                )
                SettingsSliderRow(
                    label = "Min event interval (${state.minimumEventIntervalMinutes} min)",
                    value = state.minimumEventIntervalMinutes.toFloat(),
                    valueRange = 1f..60f,
                    steps = 58,
                    onValueChange = { viewModel.setMinimumEventIntervalMinutes(it.roundToInt()) }
                )
            }

            SettingsSection(title = "Notifications") {
                SettingsSwitchRow(
                    label = "Notifications enabled",
                    checked = state.notificationsEnabled,
                    onCheckedChange = { viewModel.setNotificationsEnabled(it) }
                )
                SettingsSliderRow(
                    label = "Cooldown (${state.notificationCooldownMinutes} min)",
                    value = state.notificationCooldownMinutes.toFloat(),
                    valueRange = 1f..120f,
                    steps = 118,
                    onValueChange = { viewModel.setNotificationCooldownMinutes(it.roundToInt()) }
                )
            }

            SettingsSection(title = "Automation") {
                Card(
                    onClick = { navController.navigate(Screen.Automation.route) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ListItem(
                        headlineContent = { Text("Night protection settings") },
                        supportingContent = { Text("Configure night schedule and intervals") }
                    )
                }
            }

            SettingsSection(title = "Privacy") {
                SettingsSliderRow(
                    label = "History retention (${state.historyRetentionDays} days)",
                    value = state.historyRetentionDays.toFloat(),
                    valueRange = 1f..90f,
                    steps = 88,
                    onValueChange = { viewModel.setHistoryRetentionDays(it.roundToInt()) }
                )
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Delete all history")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = { viewModel.exportConfig() }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Export")
                    }
                    OutlinedButton(onClick = { viewModel.importConfig() }, modifier = Modifier.weight(1f)) {
                        Text("Import")
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete all history") },
            text = { Text("This will permanently delete all history and activity events from the database.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteHistory()
                    showDeleteDialog = false
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(label) },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    )
}

@Composable
private fun SettingsSliderRow(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps
        )
    }
}

private fun openShizukuApp(context: Context) {
    val intent = context.packageManager.getLaunchIntentForPackage(Constants.SHIZUKU_PACKAGE)
        ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/download/"))
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}
