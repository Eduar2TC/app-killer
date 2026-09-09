package com.appcontrol.feature.navigation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.appcontrol.core.datastore.PreferencesManager
import com.appcontrol.core.notifications.NotificationHelper
import com.appcontrol.core.permissions.PermissionManager
import com.appcontrol.data.system.ProcessStopper
import com.appcontrol.data.system.ShizukuManager
import com.appcontrol.domain.model.NightSchedule
import com.appcontrol.domain.repository.ActivityEventRepository
import com.appcontrol.domain.repository.AppRepository
import com.appcontrol.domain.repository.HistoryRepository
import com.appcontrol.domain.repository.PolicyRepository
import com.appcontrol.domain.repository.ProfileRepository
import com.appcontrol.domain.usecase.CheckNightScheduleUseCase
import com.appcontrol.domain.usecase.CleanupHistoryUseCase
import com.appcontrol.domain.usecase.GetHistoryUseCase
import com.appcontrol.domain.usecase.GetInstalledAppsUseCase
import com.appcontrol.domain.usecase.GetSelectedAppsUseCase
import com.appcontrol.domain.usecase.GetStatisticsUseCase
import com.appcontrol.domain.usecase.ManageProfileUseCase
import com.appcontrol.domain.usecase.MonitorAppActivityUseCase
import com.appcontrol.domain.usecase.ProcessSelectedAppsUseCase
import com.appcontrol.domain.usecase.ScheduleMonitoringUseCase
import com.appcontrol.domain.usecase.StopAppUseCase
import com.appcontrol.domain.usecase.ToggleAppExclusionUseCase
import com.appcontrol.domain.usecase.ToggleAppSelectionUseCase
import com.appcontrol.feature.applications.AppDetailScreen
import com.appcontrol.feature.applications.ApplicationsScreen
import com.appcontrol.feature.automation.AutomationScreen
import com.appcontrol.feature.dashboard.DashboardScreen
import com.appcontrol.feature.history.HistoryScreen
import com.appcontrol.feature.onboarding.OnboardingScreen
import com.appcontrol.feature.permissions.PermissionsScreen
import com.appcontrol.feature.profiles.ProfileDetailScreen
import com.appcontrol.feature.profiles.ProfilesScreen
import com.appcontrol.feature.settings.SettingsScreen
import com.appcontrol.scheduler.AlarmScheduler
import com.appcontrol.scheduler.WorkManagerScheduler
import kotlinx.coroutines.flow.first

class AppDependencies(
    val context: Context,
    val appRepository: AppRepository,
    val profileRepository: ProfileRepository,
    val historyRepository: HistoryRepository,
    val activityEventRepository: ActivityEventRepository,
    val policyRepository: PolicyRepository,
    val processStopper: ProcessStopper,
    val shizukuManager: ShizukuManager,
    val preferencesManager: PreferencesManager,
    val permissionManager: PermissionManager,
    val notificationHelper: NotificationHelper
) {
    val getInstalledApps: GetInstalledAppsUseCase by lazy { GetInstalledAppsUseCase(appRepository) }
    val getSelectedApps: GetSelectedAppsUseCase by lazy { GetSelectedAppsUseCase(appRepository) }
    val toggleAppSelection: ToggleAppSelectionUseCase by lazy { ToggleAppSelectionUseCase(appRepository) }
    val toggleAppExclusion: ToggleAppExclusionUseCase by lazy { ToggleAppExclusionUseCase(appRepository) }
    val checkNightSchedule: CheckNightScheduleUseCase by lazy { CheckNightScheduleUseCase() }
    val monitorAppActivity: MonitorAppActivityUseCase by lazy {
        MonitorAppActivityUseCase(appRepository, activityEventRepository, historyRepository)
    }
    val processSelectedApps: ProcessSelectedAppsUseCase by lazy {
        ProcessSelectedAppsUseCase(
            appRepository,
            historyRepository,
            activityEventRepository,
            monitorAppActivity,
            processStopper
        )
    }
    val stopApp: StopAppUseCase by lazy {
        StopAppUseCase(activityEventRepository, historyRepository, processStopper)
    }
    val scheduleMonitoring: ScheduleMonitoringUseCase by lazy {
        ScheduleMonitoringUseCase(
            object : ScheduleMonitoringUseCase.SchedulerDelegate {
                override suspend fun schedule() {
                    val prefs = PreferencesManager(context)
                    val workManagerScheduler = WorkManagerScheduler(context)
                    val alarmScheduler = AlarmScheduler(context)

                    val interval = prefs.monitoringInterval.first().toLong()
                    workManagerScheduler.schedulePeriodicCheck(interval)
                    workManagerScheduler.scheduleHistoryCleanup()

                    val nightEnabled = prefs.nightProtectionEnabled.first()
                    if (nightEnabled) {
                        val nightSchedule = NightSchedule(
                            enabled = true,
                            startHour = prefs.nightStartHour.first(),
                            startMinute = prefs.nightStartMinute.first(),
                            endHour = prefs.nightEndHour.first(),
                            endMinute = prefs.nightEndMinute.first()
                        )
                        alarmScheduler.scheduleNightProtection(nightSchedule)
                        workManagerScheduler.scheduleNightProtection(nightSchedule)
                    } else {
                        alarmScheduler.cancelNightProtection()
                    }
                }

                override suspend fun cancel() {
                    WorkManagerScheduler(context).cancelAll()
                    AlarmScheduler(context).cancelNightProtection()
                }
            }
        )
    }
    val manageProfile: ManageProfileUseCase by lazy { ManageProfileUseCase(profileRepository) }
    val getHistory: GetHistoryUseCase by lazy { GetHistoryUseCase(historyRepository) }
    val getStatistics: GetStatisticsUseCase by lazy {
        GetStatisticsUseCase(appRepository, historyRepository, activityEventRepository)
    }
    val cleanupHistory: CleanupHistoryUseCase by lazy {
        CleanupHistoryUseCase(historyRepository, activityEventRepository)
    }
}

@Composable
fun AppNavGraph(
    navController: NavHostController,
    deps: AppDependencies,
    startDestination: String = Screen.Dashboard.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Dashboard.route) {
            DashboardScreen(navController, deps)
        }
        composable(Screen.Applications.route) {
            ApplicationsScreen(navController, deps)
        }
        composable(
            route = Screen.ApplicationDetail.route,
            arguments = listOf(navArgument("packageName") { type = NavType.StringType })
        ) { entry ->
            val packageName = entry.arguments?.getString("packageName").orEmpty()
            AppDetailScreen(navController, deps, packageName)
        }
        composable(Screen.Profiles.route) {
            ProfilesScreen(navController, deps)
        }
        composable(
            route = Screen.ProfileDetail.route,
            arguments = listOf(navArgument("id") { type = NavType.LongType })
        ) { entry ->
            val id = entry.arguments?.getLong("id") ?: 0L
            ProfileDetailScreen(navController, deps, id)
        }
        composable(Screen.Automation.route) {
            AutomationScreen(navController, deps)
        }
        composable(Screen.History.route) {
            HistoryScreen(navController, deps)
        }
        composable(Screen.Settings.route) {
            SettingsScreen(navController, deps)
        }
        composable(Screen.Onboarding.route) {
            OnboardingScreen(navController, deps)
        }
        composable(Screen.Permissions.route) {
            PermissionsScreen(navController, deps)
        }
    }
}