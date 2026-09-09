package com.appcontrol.domain.usecase

import com.appcontrol.domain.model.NightSchedule
import java.util.Calendar

class CheckNightScheduleUseCase {
    fun isInNightPeriod(): Boolean = isInNightPeriod(NightSchedule())

    fun isInNightPeriod(schedule: NightSchedule): Boolean {
        if (!schedule.enabled) return false

        val now = Calendar.getInstance()
        val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val startMinutes = schedule.startHour * 60 + schedule.startMinute
        val endMinutes = schedule.endHour * 60 + schedule.endMinute

        return if (startMinutes <= endMinutes) {
            currentMinutes in startMinutes until endMinutes
        } else {
            currentMinutes >= startMinutes || currentMinutes < endMinutes
        }
    }
}