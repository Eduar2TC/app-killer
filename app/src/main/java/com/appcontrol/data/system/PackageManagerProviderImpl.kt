package com.appcontrol.data.system

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PackageManagerProviderImpl(private val context: Context) : PackageManagerProvider {

    private val packageManager: PackageManager
        get() = context.packageManager

    override suspend fun getInstalledApplications(): List<InstalledApp> = withContext(Dispatchers.IO) {
        @Suppress("DEPRECATION")
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

        @Suppress("DEPRECATION")
        val resolveInfos = packageManager.queryIntentActivities(launcherIntent, 0)

        resolveInfos
            .mapNotNull { it.activityInfo?.packageName }
            .distinct()
            .mapNotNull(::applicationInfoOf)
            .sortedBy { it.label.lowercase() }
    }

    override suspend fun getAppInfo(packageName: String): InstalledApp? = withContext(Dispatchers.IO) {
        applicationInfoOf(packageName)
    }

    override suspend fun getAppLabel(packageName: String): String = withContext(Dispatchers.IO) {
        applicationInfoOf(packageName)?.label ?: packageName
    }

    override suspend fun isSystemApp(packageName: String): Boolean = withContext(Dispatchers.IO) {
        applicationInfoOf(packageName)?.isSystem ?: false
    }

    override suspend fun getAppIcon(packageName: String): Drawable? = withContext(Dispatchers.IO) {
        try {
            packageManager.getApplicationIcon(packageName)
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }

    @Suppress("DEPRECATION")
    private fun applicationInfoOf(packageName: String): InstalledApp? {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val isSystem = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
            InstalledApp(
                packageName = packageName,
                label = packageManager.getApplicationLabel(appInfo).toString(),
                isSystem = isSystem,
                versionName = packageInfo.versionName ?: "",
                versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode
                } else {
                    packageInfo.versionCode.toLong()
                },
                firstInstallTime = packageInfo.firstInstallTime,
                lastUpdateTime = packageInfo.lastUpdateTime
            )
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }
}