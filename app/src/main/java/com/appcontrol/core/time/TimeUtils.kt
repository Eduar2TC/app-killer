package com.appcontrol.core.time

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object TimeUtils {

    private val fullDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val shortDateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())

    fun formatFull(timestamp: Long): String = fullDateFormat.format(Date(timestamp))

    fun formatShort(timestamp: Long): String = shortDateFormat.format(Date(timestamp))

    fun formatTime(timestamp: Long): String = timeFormat.format(Date(timestamp))

    fun formatDay(timestamp: Long): String = dayFormat.format(Date(timestamp))

    fun minutesOfDay(hour: Int, minute: Int): Int = hour * 60 + minute

    fun timeFromMinutes(totalMinutes: Int): Pair<Int, Int> {
        val hour = (totalMinutes / 60) % 24
        val minute = totalMinutes % 60
        return Pair(hour, minute)
    }

    fun isCurrentTimeInNightSchedule(
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int
    ): Boolean {
        val now = Calendar.getInstance()
        val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val startMinutes = startHour * 60 + startMinute
        val endMinutes = endHour * 60 + endMinute

        return if (startMinutes <= endMinutes) {
            currentMinutes in startMinutes..endMinutes
        } else {
            currentMinutes >= startMinutes || currentMinutes <= endMinutes
        }
    }

    fun getNextNightStart(startHour: Int, startMinute: Int): Calendar {
        val now = Calendar.getInstance()
        val nightStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, startHour)
            set(Calendar.MINUTE, startMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (nightStart.before(now)) {
            nightStart.add(Calendar.DAY_OF_MONTH, 1)
        }

        return nightStart
    }

    fun getNextNightEnd(endHour: Int, endMinute: Int): Calendar {
        val now = Calendar.getInstance()
        val nightEnd = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, endHour)
            set(Calendar.MINUTE, endMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (nightEnd.before(now)) {
            nightEnd.add(Calendar.DAY_OF_MONTH, 1)
        }

        return nightEnd
    }

    fun getTimeUntil(targetHour: Int, targetMinute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, targetHour)
            set(Calendar.MINUTE, targetMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (target.before(now)) {
            target.add(Calendar.DAY_OF_MONTH, 1)
        }

        return target.timeInMillis - now.timeInMillis
    }

    fun getStartOfDay(timestamp: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun getEndOfDay(timestamp: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }

    fun getStartOfWeek(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun getStartOfMonth(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun daysAgo(days: Int): Long {
        return System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days.toLong())
    }

    fun minutesAgo(minutes: Int): Long {
        return System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(minutes.toLong())
    }

    fun hoursAgo(hours: Int): Long {
        return System.currentTimeMillis() - TimeUnit.HOURS.toMillis(hours.toLong())
    }
}
