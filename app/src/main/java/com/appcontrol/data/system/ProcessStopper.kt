package com.appcontrol.data.system

import com.appcontrol.domain.model.StopResult

interface ProcessStopper {
    fun stopPackage(packageName: String): StopResult
}