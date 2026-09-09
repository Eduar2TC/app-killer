package com.appcontrol.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.appcontrol.core.time.TimeUtils
import com.appcontrol.domain.model.NightSchedule
import com.appcontrol.receiver.BootReceiver

class AlarmScheduler(private val context: Context) {

    companion object {
        const val ALARM_REQUEST_CODE_NIGHT_PROTECTION = 1001

        const val ACTION_NIGHT_PROTECTION_ALARM =
            "com.appcontrol.action.NIGHT_PROTECTION_ALARM"
    }

    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleNightProtection(schedule: NightSchedule) {
        if (!schedule.enabled) {
            cancelNightProtection()
            return
        }

        if (!canScheduleExactAlarms()) return

        val nextStart = TimeUtils.getNextNightStart(schedule.startHour, schedule.startMinute)
        val triggerAtMillis = nextStart.timeInMillis

        val nightProtectionIntent = Intent(context, BootReceiver::class.java).apply {
            action = ACTION_NIGHT_PROTECTION_ALARM
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE_NIGHT_PROTECTION,
            nightProtectionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent
        )
    }

    fun cancelNightProtection() {
        val nightProtectionIntent = Intent(context, BootReceiver::class.java).apply {
            action = ACTION_NIGHT_PROTECTION_ALARM
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE_NIGHT_PROTECTION,
            nightProtectionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }
}