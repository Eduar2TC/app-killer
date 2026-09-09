package com.appcontrol.data.system

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Process
import com.appcontrol.domain.model.ActivityEvent
import com.appcontrol.domain.model.EventType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UsageStatsProviderImpl(private val context: Context) : UsageStatsProvider {

    private val usageStatsManager: UsageStatsManager
        get() = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    @Suppress("DEPRECATION")
    private fun hasUsageAccessPermission(): Boolean {
        val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOpsManager.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            appOpsManager.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    override suspend fun getRecentActivity(packageName: String, sinceTimestamp: Long): Long =
        withContext(Dispatchers.IO) {
            if (!hasUsageAccessPermission()) return@withContext 0L
            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                sinceTimestamp,
                System.currentTimeMillis()
            )
            stats.firstOrNull { it.packageName == packageName }?.totalTimeInForeground ?: 0L
        }

    override suspend fun isAppActive(packageName: String, sinceTimestamp: Long): Boolean =
        getRecentActivity(packageName, sinceTimestamp) > 0L

    override suspend fun getAllRecentActivity(sinceTimestamp: Long): Map<String, Long> =
        withContext(Dispatchers.IO) {
            if (!hasUsageAccessPermission()) return@withContext emptyMap()
            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                sinceTimestamp,
                System.currentTimeMillis()
            )
            stats
                .filter { it.totalTimeInForeground > 0L }
                .associate { it.packageName to it.totalTimeInForeground }
        }

    override suspend fun getRecentActivityEvents(
        sinceTimestamp: Long,
        untilTimestamp: Long
    ): List<ActivityEvent> =
        withContext(Dispatchers.IO) {
            if (!hasUsageAccessPermission()) return@withContext emptyList()
            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                sinceTimestamp,
                untilTimestamp
            )
            stats
                .filter { it.totalTimeInForeground > 0L }
                .map { usage ->
                    ActivityEvent(
                        packageName = usage.packageName,
                        timestamp = usage.lastTimeUsed,
                        eventType = EventType.APP_ACTIVE,
                        source = "usage_stats_provider"
                    )
                }
        }
}