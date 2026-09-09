package com.appcontrol.data.system

import android.content.Context

interface SystemSettingsLauncher {
    fun openAppInfo(context: Context, packageName: String)
    fun openUsageAccessSettings(context: Context)
    fun openNotificationSettings(context: Context)
    fun openAppSettings(context: Context)
}