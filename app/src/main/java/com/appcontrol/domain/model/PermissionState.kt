package com.appcontrol.domain.model

enum class PermissionState {
    GRANTED,
    DENIED,
    UNKNOWN
}

data class AllPermissionStates(
    val usageAccess: PermissionState = PermissionState.UNKNOWN,
    val notificationAccess: PermissionState = PermissionState.UNKNOWN,
    val batteryOptimizationExempt: PermissionState = PermissionState.UNKNOWN,
    val overlayPermission: PermissionState = PermissionState.UNKNOWN
)
