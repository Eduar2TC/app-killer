package com.appcontrol.domain.model

data class AppPolicy(
    val id: Long = 0L,
    val packageName: String,
    val enabled: Boolean = true,
    val monitorEnabled: Boolean = true,
    val notificationEnabled: Boolean = true,
    val retryEnabled: Boolean = false
)
