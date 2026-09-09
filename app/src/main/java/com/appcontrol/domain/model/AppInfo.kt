package com.appcontrol.domain.model

data class AppInfo(
    val packageName: String,
    val label: String,
    val versionName: String? = null,
    val icon: Any? = null,
    val isSystemApp: Boolean = false,
    val isSelected: Boolean = false,
    val isExcluded: Boolean = false,
    val lastActivityTime: Long = 0L,
    val status: AppStatus = AppStatus.UNKNOWN
)
