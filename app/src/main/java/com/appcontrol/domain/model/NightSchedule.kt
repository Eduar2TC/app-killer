package com.appcontrol.domain.model

data class NightSchedule(
    val enabled: Boolean = false,
    val startHour: Int = 22,
    val startMinute: Int = 0,
    val endHour: Int = 7,
    val endMinute: Int = 0,
    val intervalMinutes: Int = 30
)
