package com.appcontrol.scheduler

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.appcontrol.core.datastore.PreferencesManager
import com.appcontrol.core.time.TimeUtils
import com.appcontrol.domain.model.NightSchedule
import com.appcontrol.worker.HistoryCleanupWorker
import com.appcontrol.worker.NightProtectionWorker
import com.appcontrol.worker.PeriodicCheckWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class WorkManagerScheduler(private val context: Context) : MonitoringScheduler {

    companion object {
        const val TAG_NIGHT_PROTECTION = "night_protection_work"
        const val TAG_PERIODIC_CHECK = "periodic_check_work"
        const val TAG_HISTORY_CLEANUP = "history_cleanup_work"

        const val UNIQUE_NIGHT_PROTECTION = "night_protection_run"
        const val PERIODIC_CHECK_SCHEDULE_NAME = "periodic_check_schedule"
        const val HISTORY_CLEANUP_SCHEDULE_NAME = "history_cleanup_schedule"
    }

    private val workManager = WorkManager.getInstance(context)

    override fun scheduleNightProtection(schedule: NightSchedule) {
        if (!schedule.enabled) {
            cancelNightProtection()
            return
        }

        val nextStart = TimeUtils.getNextNightStart(schedule.startHour, schedule.startMinute)
        val delayMillis = (nextStart.timeInMillis - System.currentTimeMillis()).coerceAtLeast(0L)

        val nightProtectionWork = OneTimeWorkRequestBuilder<NightProtectionWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(NightProtectionWorker.createInputData(null))
            .addTag(TAG_NIGHT_PROTECTION)
            .build()

        workManager.enqueueUniqueWork(
            UNIQUE_NIGHT_PROTECTION,
            ExistingWorkPolicy.REPLACE,
            nightProtectionWork
        )
    }

    fun enqueueNightProtectionNow(profileId: Long? = null) {
        val nightProtectionWork = OneTimeWorkRequestBuilder<NightProtectionWorker>()
            .setInputData(NightProtectionWorker.createInputData(profileId))
            .addTag(TAG_NIGHT_PROTECTION)
            .build()

        workManager.enqueueUniqueWork(
            UNIQUE_NIGHT_PROTECTION,
            ExistingWorkPolicy.REPLACE,
            nightProtectionWork
        )
    }

    fun enqueueNightProtectionDelayed(profileId: Long?, delayMillis: Long) {
        val nightProtectionWork = OneTimeWorkRequestBuilder<NightProtectionWorker>()
            .setInitialDelay(delayMillis.coerceAtLeast(0L), TimeUnit.MILLISECONDS)
            .setInputData(NightProtectionWorker.createInputData(profileId))
            .addTag(TAG_NIGHT_PROTECTION)
            .build()

        workManager.enqueueUniqueWork(
            UNIQUE_NIGHT_PROTECTION,
            ExistingWorkPolicy.REPLACE,
            nightProtectionWork
        )
    }

    override fun schedulePeriodicCheck(intervalMinutes: Long) {
        val effectiveInterval = intervalMinutes.coerceAtLeast(15L)

        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val periodicWork = PeriodicWorkRequestBuilder<PeriodicCheckWorker>(
            effectiveInterval,
            TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .addTag(TAG_PERIODIC_CHECK)
            .build()

        workManager.enqueueUniquePeriodicWork(
            PERIODIC_CHECK_SCHEDULE_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicWork
        )
    }

    override fun scheduleHistoryCleanup() {
        val cleanupWork = PeriodicWorkRequestBuilder<HistoryCleanupWorker>(
            1,
            TimeUnit.DAYS
        )
            .addTag(TAG_HISTORY_CLEANUP)
            .build()

        workManager.enqueueUniquePeriodicWork(
            HISTORY_CLEANUP_SCHEDULE_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            cleanupWork
        )
    }

    override fun cancelAll() {
        cancelNightProtection()
        cancelPeriodicCheck()
        cancelHistoryCleanup()
    }

    override fun rescheduleAll() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = PreferencesManager(context)
                val interval = prefs.monitoringInterval.first().toLong()
                schedulePeriodicCheck(interval)
                scheduleHistoryCleanup()

                val nightEnabled = prefs.nightProtectionEnabled.first()
                if (nightEnabled) {
                    val schedule = NightSchedule(
                        enabled = true,
                        startHour = prefs.nightStartHour.first(),
                        startMinute = prefs.nightStartMinute.first(),
                        endHour = prefs.nightEndHour.first(),
                        endMinute = prefs.nightEndMinute.first()
                    )
                    scheduleNightProtection(schedule)
                } else {
                    cancelNightProtection()
                }
            } catch (_: Exception) {
                // ignore scheduling errors during reschedule
            }
        }
    }

    private fun cancelNightProtection() {
        workManager.cancelUniqueWork(UNIQUE_NIGHT_PROTECTION)
    }

    private fun cancelPeriodicCheck() {
        workManager.cancelUniqueWork(PERIODIC_CHECK_SCHEDULE_NAME)
    }

    private fun cancelHistoryCleanup() {
        workManager.cancelUniqueWork(HISTORY_CLEANUP_SCHEDULE_NAME)
    }
}