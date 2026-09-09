package com.appcontrol.engine

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context

data class AppUsageInfo(
    val packageName: String,
    val totalTimeInForeground: Long,
    val lastTimeUsed: Long,
    val count: Int
)

class UsageStatsProvider(private val context: Context) {

    fun isUsageAccessGranted(): Boolean {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val usageStats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            now - 7 * 24 * 60 * 60 * 1000L,
            now
        )
        return !usageStats.isNullOrEmpty()
    }

    fun getAppUsageInfo(packageName: String, since: Long): AppUsageInfo? {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            since,
            System.currentTimeMillis()
        )
        val appStat = stats?.firstOrNull { it.packageName == packageName }
            ?: return AppUsageInfo(packageName, 0L, 0L, 0)

        return AppUsageInfo(
            packageName = appStat.packageName,
            totalTimeInForeground = appStat.totalTimeInForeground,
            lastTimeUsed = appStat.lastTimeUsed,
            count = appStat.appLaunchCount
        )
    }

    fun getRecentlyUsedApps(since: Long): List<AppUsageInfo> {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            since,
            now
        ) ?: return emptyList()

        return stats
            .filter { it.lastTimeUsed >= since && !it.packageName.isNullOrBlank() }
            .map {
                AppUsageInfo(
                    packageName = it.packageName,
                    totalTimeInForeground = it.totalTimeInForeground,
                    lastTimeUsed = it.lastTimeUsed,
                    count = it.appLaunchCount
                )
            }
            .sortedByDescending { it.lastTimeUsed }
    }

    fun getPackageUsageEvents(since: Long): Map<String, Long> {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val events = usageStatsManager.queryEvents(since, System.currentTimeMillis())
        val lastActiveTimes = mutableMapOf<String, Long>()
        val event = UsageEvents.Event()

        if (events == null) return lastActiveTimes

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                lastActiveTimes[event.packageName] = event.timeStamp
            }
        }

        return lastActiveTimes
    }

    fun isAppRecentlyActive(packageName: String, since: Long): Boolean {
        val events = getPackageUsageEvents(since)
        return events.containsKey(packageName)
    }

    fun getAppsActiveInPeriod(appPackages: List<String>, since: Long): Set<String> {
        val activeEvents = getPackageUsageEvents(since)
        return appPackages.filter { activeEvents.containsKey(it) }.toSet()
    }
}
