package com.appcontrol.feature.permissions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.appcontrol.feature.navigation.AppDependencies
import com.appcontrol.feature.navigation.Screen

private val greenColor = Color(0xFF4CAF50)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsScreen(
    navController: NavHostController,
    deps: AppDependencies
) {
    val viewModel: PermissionsViewModel = viewModel(
        factory = PermissionsViewModel.Factory(deps.permissionManager)
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Permissions") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "App Control needs a few permissions to monitor your apps and schedule " +
                    "checks. Grant each permission below.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            PermissionCard(
                icon = Icons.Default.Build,
                title = "Usage access",
                description = "Allows App Control to detect which of your selected apps are active.",
                granted = state.usageStatsPermission,
                buttonLabel = if (state.usageStatsPermission) "Granted" else "Open settings",
                onOpenSettings = { viewModel.openUsageStatsSettings() }
            )

            PermissionCard(
                icon = Icons.Default.Notifications,
                title = "Notifications",
                description = "Allows App Control to alert you when a monitored app reappears.",
                granted = state.notificationPermission,
                buttonLabel = if (state.notificationPermission) "Granted" else "Open settings",
                onOpenSettings = { viewModel.openNotificationSettings() }
            )

            PermissionCard(
                icon = Icons.Default.Lock,
                title = "Exact alarms",
                description = "Allows App Control to schedule precise night-protection checks.",
                granted = state.exactAlarmPermission,
                buttonLabel = if (state.exactAlarmPermission) "Granted" else "Open settings",
                onOpenSettings = { viewModel.openExactAlarmSettings() }
            )

            Spacer(modifier = Modifier.size(8.dp))

            Button(
                onClick = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Permissions.route) { inclusive = true }
                    }
                },
                enabled = state.allGranted,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (state.allGranted) "Continue" else "Grant all permissions to continue")
            }
        }
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    description: String,
    granted: Boolean,
    buttonLabel: String,
    onOpenSettings: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(if (granted) greenColor else MaterialTheme.colorScheme.error)
                )
                Spacer(Modifier.width(10.dp))
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (granted) {
                Text(
                    text = "Granted",
                    style = MaterialTheme.typography.labelLarge,
                    color = greenColor,
                    fontWeight = FontWeight.Bold
                )
            } else {
                OutlinedButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(buttonLabel)
                }
            }
        }
    }
}