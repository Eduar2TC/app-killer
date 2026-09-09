package com.appcontrol.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import com.appcontrol.core.common.Constants
import com.appcontrol.core.datastore.PreferencesManager
import com.appcontrol.data.system.ProcessStopperImpl
import com.appcontrol.data.system.ResolvingProcessStopper
import com.appcontrol.data.system.ShizukuManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class NotificationReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "NotificationReceiver"

        const val ACTION_REVIEW = "com.appcontrol.action.NOTIFICATION_REVIEW"
        const val ACTION_PROCESS = "com.appcontrol.action.NOTIFICATION_PROCESS"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val packageName = intent.getStringExtra(Constants.EXTRA_PACKAGE_NAME)

        when (action) {
            ACTION_PROCESS -> {
                if (packageName != null) {
                    Log.i(TAG, "Stopping package $packageName from notification")
                    val pendingResult = goAsync()
                    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                        try {
                            stopPackage(context, packageName)
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
            }
            ACTION_REVIEW -> {
                packageName?.let { openAppInfo(context, it) }
            }
            else -> Log.w(TAG, "Unhandled notification action: $action")
        }
    }

    private fun stopPackage(context: Context, packageName: String) {
        val preferencesManager = PreferencesManager(context)
        val stopper = ResolvingProcessStopper(
            ShizukuManager(context),
            ProcessStopperImpl(context)
        ) {
            runBlocking { preferencesManager.shizukuEnabled.first() }
        }
        stopper.stopPackage(packageName)
    }

    private fun openAppInfo(context: Context, packageName: String) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open app info for $packageName", e)
        }
    }
}
