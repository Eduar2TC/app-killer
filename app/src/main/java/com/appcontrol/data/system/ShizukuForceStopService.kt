package com.appcontrol.data.system

import android.content.Context
import android.util.Log
import androidx.annotation.Keep
import com.appcontrol.shizuku.IAppControlService

/**
 * Runs inside the process spawned by the Shizuku server (UID shell or root), so the
 * [forceStop]/[killBackground] shell commands execute with those privileges.
 */
class ShizukuForceStopService : IAppControlService.Stub() {

    override fun destroy() {
        System.exit(0)
    }

    override fun forceStop(packageName: String): Int =
        exec("am", "force-stop", packageName)

    override fun killBackground(packageName: String): Int =
        exec("am", "kill", packageName)

    override fun ping(): String = "pong"

    constructor() : super()

    @Keep
    constructor(context: Context) : super()

    private fun exec(command: String, vararg args: String): Int {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf(command, *args))
            process.waitFor()
        } catch (e: Exception) {
            Log.w(TAG, "exec failed", e)
            -1
        }
    }

    private companion object {
        const val TAG = "ShizukuForceStopService"
    }
}