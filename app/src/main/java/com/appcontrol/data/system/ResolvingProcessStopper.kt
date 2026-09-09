package com.appcontrol.data.system

import com.appcontrol.domain.model.StopResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/**
 * Chooses the force-stop path when Shizuku is enabled (from Settings), available and with
 * permission granted. Falls back to the official [ProcessStopper] otherwise and also when
 * the force-stop attempt fails.
 */
class ResolvingProcessStopper(
    private val shizukuManager: ShizukuGateway,
    private val fallback: ProcessStopper,
    private val shizukuEnabled: suspend () -> Boolean
) : ProcessStopper {

    override fun stopPackage(packageName: String): StopResult {
        val useShizuku = runBlocking(Dispatchers.IO) { shizukuEnabled() }
        if (useShizuku && shizukuManager.isAvailable && shizukuManager.isPermissionGranted) {
            when (shizukuManager.forceStop(packageName)) {
                StopResult.STOPPED -> return StopResult.STOPPED
                else -> Unit
            }
        }
        return fallback.stopPackage(packageName)
    }
}