package com.appcontrol.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.appcontrol.core.datastore.PreferencesManager
import com.appcontrol.domain.model.NightSchedule
import com.appcontrol.scheduler.AlarmScheduler
import com.appcontrol.scheduler.WorkManagerScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"

        private val handledActions = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_DATE_CHANGED,
            AlarmScheduler.ACTION_NIGHT_PROTECTION_ALARM
        )
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action !in handledActions) return

        when (action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                Log.i(TAG, "Device boot completed. Rescheduling all monitoring.")
                rescheduleAll(context)
            }
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_DATE_CHANGED -> {
                Log.i(TAG, "System time changed ($action). Rescheduling monitoring.")
                rescheduleAll(context)
            }
            AlarmScheduler.ACTION_NIGHT_PROTECTION_ALARM -> {
                Log.i(TAG, "Night protection alarm fired. Enqueueing night protection work.")
                WorkManagerScheduler(context).enqueueNightProtectionNow()
                rearmNightProtectionAlarm(context)
            }
        }
    }

    private fun rearmNightProtectionAlarm(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val preferencesManager = PreferencesManager(context)
                if (!preferencesManager.nightProtectionEnabled.first()) return@launch

                val nightSchedule = NightSchedule(
                    enabled = true,
                    startHour = preferencesManager.nightStartHour.first(),
                    startMinute = preferencesManager.nightStartMinute.first(),
                    endHour = preferencesManager.nightEndHour.first(),
                    endMinute = preferencesManager.nightEndMinute.first()
                )
                AlarmScheduler(context).scheduleNightProtection(nightSchedule)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to rearm night protection alarm", e)
            }
        }
    }

    private fun rescheduleAll(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val preferencesManager = PreferencesManager(context)
                val workManagerScheduler = WorkManagerScheduler(context)
                val alarmScheduler = AlarmScheduler(context)

                val interval = preferencesManager.monitoringInterval.first().toLong()
                workManagerScheduler.schedulePeriodicCheck(interval)
                workManagerScheduler.scheduleHistoryCleanup()

                val nightEnabled = preferencesManager.nightProtectionEnabled.first()
                if (nightEnabled) {
                    val nightSchedule = NightSchedule(
                        enabled = true,
                        startHour = preferencesManager.nightStartHour.first(),
                        startMinute = preferencesManager.nightStartMinute.first(),
                        endHour = preferencesManager.nightEndHour.first(),
                        endMinute = preferencesManager.nightEndMinute.first()
                    )
                    workManagerScheduler.scheduleNightProtection(nightSchedule)
                    alarmScheduler.scheduleNightProtection(nightSchedule)
                } else {
                    alarmScheduler.cancelNightProtection()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reschedule monitoring", e)
            }
        }
    }
}