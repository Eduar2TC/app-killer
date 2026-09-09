package com.appcontrol.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.appcontrol.core.database.AppDatabase
import com.appcontrol.core.datastore.PreferencesManager
import com.appcontrol.domain.model.NightSchedule
import com.appcontrol.domain.usecase.CheckNightScheduleUseCase
import com.appcontrol.engine.MonitoringEngine
import com.appcontrol.engine.UsageStatsProvider
import com.appcontrol.scheduler.WorkManagerScheduler
import kotlinx.coroutines.flow.first

class NightProtectionWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val KEY_PROFILE_ID = "profile_id"

        fun createInputData(profileId: Long?): androidx.work.Data {
            return workDataOf(KEY_PROFILE_ID to (profileId ?: -1L))
        }
    }

    override suspend fun doWork(): Result {
        val profileId = inputData.getLong(KEY_PROFILE_ID, -1L)
            .takeIf { it >= 0 }

        return try {
            val preferencesManager = PreferencesManager(applicationContext)
            val database = AppDatabase.getInstance(applicationContext)

            val nightProtectionEnabled = preferencesManager.nightProtectionEnabled.first()
            if (!nightProtectionEnabled) {
                return Result.success()
            }

            val schedule = NightSchedule(
                enabled = true,
                startHour = preferencesManager.nightStartHour.first(),
                startMinute = preferencesManager.nightStartMinute.first(),
                endHour = preferencesManager.nightEndHour.first(),
                endMinute = preferencesManager.nightEndMinute.first()
            )

            val checkNightSchedule = CheckNightScheduleUseCase()
            if (!checkNightSchedule.isInNightPeriod(schedule)) {
                return Result.success()
            }

            val monitoringEngine = MonitoringEngine(
                context = applicationContext,
                usageStatsProvider = UsageStatsProvider(applicationContext),
                activityEventRepository = DatabaseActivityEventRepository(database),
                historyRepository = DatabaseHistoryRepository(database),
                notificationHelper = com.appcontrol.core.notifications.NotificationHelper(applicationContext),
                preferencesManager = preferencesManager
            )

            val appPackages = loadMonitoredPackages(profileId)
            monitoringEngine.runCheck(
                profileId = profileId,
                appPackages = appPackages
            )

            if (checkNightSchedule.isInNightPeriod(schedule)) {
                val intervalMillis = (schedule.intervalMinutes * 60 * 1000L).coerceAtLeast(60_000L)
                WorkManagerScheduler(applicationContext)
                    .enqueueNightProtectionDelayed(profileId, intervalMillis)
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private suspend fun loadMonitoredPackages(profileId: Long?): List<String> {
        val database = AppDatabase.getInstance(applicationContext)
        val preferencesManager = PreferencesManager(applicationContext)

        val resolvedProfileId = profileId
            ?: preferencesManager.activeProfileId.first().takeIf { it >= 0 }

        if (resolvedProfileId != null) {
            val profile = database.profileDao().getProfileById(resolvedProfileId)
            if (profile != null && profile.enabled) {
                return database.profileDao()
                    .getAppsForProfileList(resolvedProfileId)
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