package com.appcontrol.engine

data class MonitoringResult(
    val checkedCount: Int,
    val reappearanceCount: Int,
    val reappearedApps: List<String>,
    val errors: List<String>,
    val timestamp: Long = System.currentTimeMillis()
) {
    val isSuccessful: Boolean get() = errors.isEmpty()
}
