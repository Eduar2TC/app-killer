package com.appcontrol.feature.dashboard

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.appcontrol.AppControlApplication
import com.appcontrol.core.notifications.NotificationHelper
import com.appcontrol.core.permissions.PermissionManager
import com.appcontrol.data.local.AppDatabaseProvider
import com.appcontrol.data.repository.ActivityEventRepositoryImpl
import com.appcontrol.data.repository.AppRepositoryImpl
import com.appcontrol.data.repository.HistoryRepositoryImpl
import com.appcontrol.data.repository.PolicyRepositoryImpl
import com.appcontrol.data.repository.ProfileRepositoryImpl
import com.appcontrol.data.system.PackageManagerProviderImpl
import com.appcontrol.data.system.ProcessStopperImpl
import com.appcontrol.data.system.ResolvingProcessStopper
import com.appcontrol.data.system.ShizukuManager
import com.appcontrol.feature.AppControlTheme
import com.appcontrol.feature.navigation.AppDependencies
import com.appcontrol.feature.navigation.AppNavGraph
import com.appcontrol.feature.navigation.Screen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            // notification permission outcome handled internally by the system
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestNotificationPermissionIfNeeded()

        setContent {
            AppControlTheme {
                val app = LocalContext.current.applicationContext as AppControlApplication
                val viewModel: MainActivityViewModel = viewModel(
                    factory = remember {
                        MainActivityViewModelFactory(app.preferencesManager)
                    }
                )
                val navigationState by viewModel.navigationState.collectAsStateWithLifecycle()
                val deps = remember { buildDependencies(app) }
                val navController = rememberNavController()

                AppNavGraph(
                    navController = navController,
                    deps = deps,
                    startDestination = when (navigationState) {
                        NavigationState.Loading -> Screen.Dashboard.route
                        NavigationState.Onboarding -> Screen.Onboarding.route
                        NavigationState.Dashboard -> Screen.Dashboard.route
                    }
                )
            }
        }
    }

    private fun buildDependencies(app: AppControlApplication): AppDependencies {
        val dbProvider = AppDatabaseProvider(applicationContext)
        val packageManagerProvider = PackageManagerProviderImpl(applicationContext)
        val shizukuManager = ShizukuManager(applicationContext)
        val fallbackStopper = ProcessStopperImpl(applicationContext)
        val processStopper = ResolvingProcessStopper(
            shizukuManager,
            fallbackStopper
        ) {
            runBlocking { app.preferencesManager.shizukuEnabled.first() }
        }

        return AppDependencies(
            context = applicationContext,
            appRepository = AppRepositoryImpl(dbProvider.applicationDao, packageManagerProvider),
            profileRepository = ProfileRepositoryImpl(dbProvider.profileDao, app.preferencesManager),
            historyRepository = HistoryRepositoryImpl(dbProvider.historyDao),
            activityEventRepository = ActivityEventRepositoryImpl(dbProvider.activityEventDao),
            policyRepository = PolicyRepositoryImpl(dbProvider.appPolicyDao),
            processStopper = processStopper,
            shizukuManager = shizukuManager,
            preferencesManager = app.preferencesManager,
            permissionManager = PermissionManager(applicationContext),
            notificationHelper = NotificationHelper(applicationContext)
        )
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}