package com.appcontrol.data.system

import android.app.ActivityManager
import android.content.Context
import android.util.Log
import com.appcontrol.domain.model.StopResult

class ProcessStopperImpl(private val context: Context) : ProcessStopper {

    companion object {
        private const val TAG = "ProcessStopperImpl"
    }

    private val activityManager =
        context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    override fun stopPackage(packageName: String): StopResult {
        return try {
            // killBackgroundProcesses returns void: there is no reliable way to know whether
            // the package had background processes (getRunningAppProcesses is privacy-filtered
            // since API 22 and does not list cached/background apps), so a successful call is
            // reported as STOPPED. A false NOT_RUNNING here would skip killing real background
            // processes, which is the app's core purpose.
            activityManager.killBackgroundProcesses(packageName)
            StopResult.STOPPED
        } catch (e: Exception) {
            Log.w(TAG, "killBackgroundProcesses failed for $packageName", e)
            StopResult.FAILED
        }
    }
}