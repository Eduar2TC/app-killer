package com.appcontrol.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.appcontrol.core.database.AppDatabase
import com.appcontrol.core.datastore.PreferencesManager
import com.appcontrol.engine.MonitoringEngine
import com.appcontrol.engine.UsageStatsProvider
import com.appcontrol.scheduler.WorkManagerScheduler
import kotlinx.coroutines.flow.first

class PeriodicCheckWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val preferencesManager = PreferencesManager(applicationContext)
            val database = AppDatabase.getInstance(applicationContext)

            val monitoringEngine = MonitoringEngine(
                context = applicationContext,
                usageStatsProvider = UsageStatsProvider(applicationContext),
                activityEventRepository = DatabaseActivityEventRepository(database),
                historyRepository = DatabaseHistoryRepository(database),
                notificationHelper = com.appcontrol.core.notifications.NotificationHelper(applicationContext),
                preferencesManager = preferencesManager
            )

            val appPackages = loadMonitoredPackages()
            val result = monitoringEngine.runCheck(
                profileId = null,
                appPackages = appPackages
            )

            if (result.reappearanceCount > 0) {
                val interval = preferencesManager.monitoringInterval.first().toLong()
                WorkManagerScheduler(applicationContext).schedulePeriodicCheck(interval)
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private suspend fun loadMonitoredPackages(): List<String> {
        val database = AppDatabase.getInstance(applicationContext)
        val preferencesManager = PreferencesManager(applicationContext)

        val activeProfileId = preferencesManager.activeProfileId.first().takeIf { it >= 0 }
        if (activeProfileId != null) {
            val profile = database.profileDao().getProfileById(activeProfileId)
            if (profile != null && profile.enabled) {
                return database.profileDao()
                    .getAppsForProfileList(activeProfileId)
                    .filter { it.enabled }
                    .map { it.packageName }
            }
        }

        return database.applicationDao()
            .getSelectedAppsList()
            .filter { !it.isExcluded }
            .map { it.packageName }
    }
}