package com.appcontrol.data.system

import com.appcontrol.domain.model.StopResult

interface ShizukuGateway {
    val isAvailable: Boolean
    val isPermissionGranted: Boolean
    fun forceStop(packageName: String): StopResult
}