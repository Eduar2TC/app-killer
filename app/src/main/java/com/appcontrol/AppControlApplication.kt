package com.appcontrol

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.appcontrol.core.common.Constants
import com.appcontrol.core.database.AppDatabase
import com.appcontrol.core.datastore.PreferencesManager
import com.appcontrol.core.notifications.NotificationHelper
import com.appcontrol.domain.model.NightSchedule
import com.appcontrol.scheduler.AlarmScheduler
import com.appcontrol.scheduler.WorkManagerScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppControlApplication : Application() {

    companion object {
        lateinit var instance: AppControlApplication
            private set
    }

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var database: AppDatabase
        private set
    lateinit var preferencesManager: PreferencesManager
        private set
    lateinit var notificationHelper: NotificationHelper
        private set
    lateinit var workManagerScheduler: WorkManagerScheduler
        private set
    lateinit var alarmScheduler: AlarmScheduler
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = AppDatabase.getInstance(this)
        preferencesManager = PreferencesManager(this)
        notificationHelper = NotificationHelper(this)
        workManagerScheduler = WorkManagerScheduler(this)
        alarmScheduler = AlarmScheduler(this)

        createNotificationChannels()

        scheduleOnStartIfNeeded()
    }

    private fun scheduleOnStartIfNeeded() {
        applicationScope.launch {
            try {
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
                    alarmScheduler.scheduleNightProtection(nightSchedule)
                }
            } catch (e: Exception) {
                // ignore failures on startup schedule
            }
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val monitoringChannel = NotificationChannel(
            Constants.NOTIFICATION_CHANNEL_MONITORING,
            Constants.NOTIFICATION_CHANNEL_MONITORING_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifications for app monitoring events"
        }

        val generalChannel = NotificationChannel(
            Constants.NOTIFICATION_CHANNEL_GENERAL,
            Constants.NOTIFICATION_CHANNEL_GENERAL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "General app notifications"
        }

        notificationManager.createNotificationChannel(monitoringChannel)
        notificationManager.createNotificationChannel(generalChannel)
    }
}
