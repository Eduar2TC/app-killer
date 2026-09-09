package com.appcontrol.domain.model

data class Profile(
    val id: Long = 0L,
    val name: String,
    val enabled: Boolean = true,
    val scheduleEnabled: Boolean = false,
    val startTime: String? = null,
    val endTime: String? = null,
    val monitoringInterval: Long = 60000L,
    val notificationEnabled: Boolean = true,
    val appPackages: List<String> = emptyList()
)
