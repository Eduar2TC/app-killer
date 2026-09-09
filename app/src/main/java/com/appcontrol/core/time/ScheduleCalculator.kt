package com.appcontrol.core.time

import com.appcontrol.domain.model.NightSchedule
import java.util.Calendar
import java.util.concurrent.TimeUnit

object ScheduleCalculator {

    fun isNightPeriod(schedule: NightSchedule): Boolean {
        if (!schedule.enabled) return false
        return isCurrentTimeInNightSchedule(
            schedule.startHour,
            schedule.startMinute,
            schedule.endHour,
            schedule.endMinute
        )
    }

    fun isCurrentTimeInNightSchedule(
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int
    ): Boolean {
        val currentMinutes = TimeUtils.minutesOfDay(
            Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
            Calendar.getInstance().get(Calendar.MINUTE)
        )
        val startMinutes = TimeUtils.minutesOfDay(startHour, startMinute)
        val endMinutes = TimeUtils.minutesOfDay(endHour, endMinute)

        return if (startMinutes <= endMinutes) {
            currentMinutes >= startMinutes && currentMinutes < endMinutes
        } else {
            currentMinutes >= startMinutes || currentMinutes < endMinutes
        }
    }

    fun isWithinWindow(
        currentMinutes: Int,
        startMinutes: Int,
        endMinutes: Int
    ): Boolean {
        return if (startMinutes <= endMinutes) {
            currentMinutes >= startMinutes && currentMinutes < endMinutes
        } else {
            currentMinutes >= startMinutes || currentMinutes < endMinutes
        }
    }

    fun isWithinWindowFromSize(
        currentMinutes: Int,
        startMinutes: Int,
        windowSizeMinutes: Int
    ): Boolean {
        return currentMinutes in startMinutes until startMinutes + windowSizeMinutes
    }

    fun nextCheckTime(
        monitoringIntervalMillis: Long,
        now: Long = System.currentTimeMillis()
    ): Long = now + monitoringIntervalMillis

    fun nextCheckTime(
        monitoringIntervalMinutes: Int,
        now: Long = System.currentTimeMillis()
    ): Long = now + TimeUnit.MINUTES.toMillis(monitoringIntervalMinutes.toLong())

    fun checksPerDay(intervalMinutes: Int): Int {
        if (intervalMinutes <= 0) return 0
        return maxOf(1, (24 * 60) / intervalMinutes)
    }

    fun checksPerDay(schedule: NightSchedule): Int =
        checksPerDay(schedule.intervalMinutes)

    fun nextNightStart(schedule: NightSchedule, now: Long = System.currentTimeMillis()): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = now }
        val start = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, schedule.startHour)
            set(Calendar.MINUTE, schedule.startMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (!start.after(calendar)) {
            start.add(Calendar.DAY_OF_MONTH, 1)
        }
        return start.timeInMillis
    }

    fun nextNightEnd(schedule: NightSchedule, now: Long = System.currentTimeMillis()): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = now }
        val end = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, schedule.endHour)
            set(Calendar.MINUTE, schedule.endMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (!end.after(calendar)) {
            end.add(Calendar.DAY_OF_MONTH, 1)
        }
        return end.timeInMillis
    }
}