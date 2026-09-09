package com.appcontrol.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import com.appcontrol.core.common.Constants
import com.appcontrol.data.system.ProcessStopperImpl

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
                    ProcessStopperImpl(context).stopPackage(packageName)
                }
            }
            ACTION_REVIEW -> {
                packageName?.let { openAppInfo(context, it) }
            }
            else -> Log.w(TAG, "Unhandled notification action: $action")
        }
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
