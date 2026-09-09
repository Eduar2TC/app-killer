package com.appcontrol.data.system

import android.app.ActivityManager
import android.content.Context
import com.appcontrol.domain.model.StopResult

class ProcessStopperImpl(private val context: Context) : ProcessStopper {

    private val activityManager =
        context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    override fun stopPackage(packageName: String): StopResult {
        return try {
            activityManager.killBackgroundProcesses(packageName)
            StopResult.STOPPED
        } catch (_: Exception) {
            StopResult.FAILED
        }
    }
}