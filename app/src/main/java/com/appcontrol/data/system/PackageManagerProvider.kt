package com.appcontrol.data.system

import android.graphics.drawable.Drawable

interface PackageManagerProvider {
    suspend fun getInstalledApplications(): List<InstalledApp>
    suspend fun getAppInfo(packageName: String): InstalledApp?
    suspend fun getAppLabel(packageName: String): String
    suspend fun isSystemApp(packageName: String): Boolean
    suspend fun getAppIcon(packageName: String): Drawable?

    data class InstalledApp(
        val packageName: String,
        val label: String,
        val isSystem: Boolean,
        val versionName: String,
        val versionCode: Long,
        val firstInstallTime: Long,
        val lastUpdateTime: Long
    )
}