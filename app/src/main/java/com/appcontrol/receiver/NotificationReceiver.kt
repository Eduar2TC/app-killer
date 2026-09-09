package com.appcontrol.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.appcontrol.core.common.Constants
import com.appcontrol.feature.dashboard.MainActivity

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
            ACTION_REVIEW, ACTION_PROCESS -> {
                Log.i(TAG, "Notification action '$action' for ${packageName ?: "unknown"}")
                routeToMainActivity(context, action, packageName)
            }
            else -> Log.w(TAG, "Unhandled notification action: $action")
        }
    }

    private fun routeToMainActivity(context: Context, action: String, packageName: String?) {
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                this.action = action
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                packageName?.let {
                    putExtra(Constants.EXTRA_PACKAGE_NAME, it)
                }
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to route notification action to MainActivity", e)
        }
    }
}
