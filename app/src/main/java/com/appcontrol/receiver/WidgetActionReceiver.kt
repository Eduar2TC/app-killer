package com.appcontrol.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.appcontrol.core.datastore.PreferencesManager
import com.appcontrol.core.notifications.NotificationHelper
import com.appcontrol.core.system.AppControlWidgetProvider
import com.appcontrol.data.local.AppDatabaseProvider
import com.appcontrol.data.repository.ActivityEventRepositoryImpl
import com.appcontrol.data.repository.AppRepositoryImpl
import com.appcontrol.data.repository.HistoryRepositoryImpl
import com.appcontrol.data.system.PackageManagerProviderImpl
import com.appcontrol.data.system.ProcessStopperImpl
import com.appcontrol.domain.model.NightSchedule
import com.appcontrol.domain.usecase.MonitorAppActivityUseCase
import com.appcontrol.domain.usecase.ProcessSelectedAppsUseCase
import com.appcontrol.scheduler.AlarmScheduler
import com.appcontrol.scheduler.WorkManagerScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class WidgetActionReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "WidgetActionReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            AppControlWidgetProvider.ACTION_APP_CONTROL_TOGGLE -> stopSelectedApps(context)
            AppControlWidgetProvider.ACTION_APP_CONTROL_OPEN -> toggleNightProtection(context)
            else -> Unit
        }
    }

    private fun stopSelectedApps(context: Context) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val dbProvider = AppDatabaseProvider(context)
                val packageManagerProvider = PackageManagerProviderImpl(context)

                val appRepository = AppRepositoryImpl(dbProvider.applicationDao, packageManagerProvider)
                val activityEventRepository = ActivityEventRepositoryImpl(dbProvider.activityEventDao)
                val historyRepository = HistoryRepositoryImpl(dbProvider.historyDao)
                val processStopper = ProcessStopperImpl(context)

                val monitorAppActivity = MonitorAppActivityUseCase(
                    appRepository,
                    activityEventRepository,
                    historyRepository
                )
                val stopApps = ProcessSelectedAppsUseCase(
                    appRepository,
                    historyRepository,
                    activityEventRepository,
                    monitorAppActivity,
                    processStopper
                )

                monitorAppActivity.invoke()
                val result = stopApps.invoke()

                Log.i(TAG, "Widget stop result: ${result.stopped} stopped, ${result.inactive} inactive, ${result.failed} failed")
                NotificationHelper(context)
                    .showWidgetStopResult(result.stopped, result.total, result.failed)
            } catch (e: Exception) {
                Log.e(TAG, "Widget stop failed", e)
                NotificationHelper(context).showWidgetStopError(e.message ?: "Unknown error")
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun toggleNightProtection(context: Context) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val preferencesManager = PreferencesManager(context)
                val enabled = !preferencesManager.nightProtectionEnabled.first()
                preferencesManager.setNightProtectionEnabled(enabled)

                val nightSchedule = NightSchedule(
                    enabled = enabled,
                    startHour = preferencesManager.nightStartHour.first(),
                    startMinute = preferencesManager.nightStartMinute.first(),
                    endHour = preferencesManager.nightEndHour.first(),
                    endMinute = preferencesManager.nightEndMinute.first()
                )
                WorkManagerScheduler(context).scheduleNightProtection(nightSchedule)
                val alarmScheduler = AlarmScheduler(context)
                if (enabled) alarmScheduler.scheduleNightProtection(nightSchedule)
                else alarmScheduler.cancelNightProtection()

                AppControlWidgetProvider.updateAllWidgets(context)

                Log.i(TAG, "Night protection toggled: $enabled")
                NotificationHelper(context).showGeneralNotification(
                    "Night protection",
                    if (enabled) "Enabled" else "Disabled"
                )
            } catch (e: Exception) {
                Log.e(TAG, "Widget night protection toggle failed", e)
                NotificationHelper(context).showGeneralNotification(
                    "Night protection failed",
                    e.message ?: "Unknown error"
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}