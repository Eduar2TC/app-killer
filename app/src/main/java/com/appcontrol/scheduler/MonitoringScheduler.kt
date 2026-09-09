package com.appcontrol.scheduler

import com.appcontrol.domain.model.NightSchedule

interface MonitoringScheduler {
    fun scheduleNightProtection(schedule: NightSchedule)
    fun schedulePeriodicCheck(intervalMinutes: Long)
    fun scheduleHistoryCleanup()
    fun cancelAll()
    fun rescheduleAll()
}
