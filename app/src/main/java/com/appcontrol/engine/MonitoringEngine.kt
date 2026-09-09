package com.appcontrol.engine

import android.content.Context
import com.appcontrol.core.notifications.NotificationHelper
import com.appcontrol.core.datastore.PreferencesManager
import com.appcontrol.domain.model.ActivityEvent
import com.appcontrol.domain.model.EventType
import com.appcontrol.domain.model.HistoryEvent
import com.appcontrol.domain.model.HistoryEventType
import com.appcontrol.domain.repository.ActivityEventRepository
import com.appcontrol.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.first

class MonitoringEngine(
    private val context: Context,
    private val usageStatsProvider: UsageStatsProvider,
    private val activityEventRepository: ActivityEventRepository,
    private val historyRepository: HistoryRepository,
    private val notificationHelper: NotificationHelper,
    private val preferencesManager: PreferencesManager
) {

    suspend fun runCheck(profileId: Long? = null, appPackages: List<String> = emptyList()): MonitoringResult {
        val errors = mutableListOf<String>()
        val reappearedApps = mutableListOf<String>()
        var checkedCount = 0

        try {
            val packages = if (appPackages.isNotEmpty()) {
                appPackages
            } else {
                loadMonitoredPackages(profileId)
            }

            checkedCount = packages.size

            if (packages.isEmpty()) {
                return MonitoringResult(
                    checkedCount = 0,
                    reappearanceCount = 0,
                    reappearedApps = emptyList(),
                    errors = emptyList()
                )
            }

            if (!usageStatsProvider.isUsageAccessGranted()) {
                val permissionError = "Usage access permission not granted"
                errors.add(permissionError)
                recordPermissionError(permissionError, profileId)
                return MonitoringResult(
                    checkedCount = checkedCount,
                    reappearanceCount = 0,
                    reappearedApps = emptyList(),
                    errors = errors
                )
            }

            val notificationsEnabled = preferencesManager.notificationsEnabled.first()
            val minimumEventInterval =
                preferencesManager.minimumEventIntervalMinutes.first().toLong() * 60 * 1000
            val now = System.currentTimeMillis()
            val sinceInterval = maxOf(minimumEventInterval, 5 * 60 * 1000L)

            val activePackages = try {
                usageStatsProvider.getAppsActiveInPeriod(packages, now - sinceInterval)
            } catch (e: Exception) {
                errors.add("Failed to query usage stats: ${e.message}")
                recordErrorHistory("Failed to query usage stats", profileId)
                emptySet()
            }

            for (packageName in packages) {
                try {
                    val latest = activityEventRepository.getLatest(packageName)
                    val isActive = activePackages.contains(packageName)

                    if (isActive) {
                        val reappeared = latest != null &&
                            (latest.eventType == EventType.APP_INACTIVE)
                        val eventType =
                            if (reappeared) EventType.APP_REAPPEARED else EventType.APP_ACTIVE

                        activityEventRepository.insert(
                            ActivityEvent(
                                packageName = packageName,
                                timestamp = now,
                                eventType = eventType,
                                source = "MonitoringEngine",
                                profileId = profileId
                            )
                        )

                        if (reappeared) {
                            reappearedApps.add(packageName)
                            historyRepository.insert(
                                HistoryEvent(
                                    timestamp = now,
                                    eventType = HistoryEventType.REAPPEARED,
                                    title = "App reappeared",
                                    description = "$packageName became active again.",
                                    packageName = packageName,
                                    profileId = profileId
                                )
                            )
                        }
                    } else {
                        val previousWasActive = latest != null &&
                            (latest.eventType == EventType.APP_ACTIVE ||
                                latest.eventType == EventType.APP_REAPPEARED)

                        if (previousWasActive) {
                            activityEventRepository.insert(
                                ActivityEvent(
                                    packageName = packageName,
                                    timestamp = now,
                                    eventType = EventType.APP_INACTIVE,
                                    source = "MonitoringEngine",
                                    profileId = profileId
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    errors.add("Failed to process $packageName: ${e.message}")
                }
            }

            if (reappearedApps.isNotEmpty()) {
                notifyReappearance(reappearedApps, notificationsEnabled)
            }

            historyRepository.insert(
                HistoryEvent(
                    timestamp = now,
                    eventType = HistoryEventType.CHECK,
                    title = "Monitoring check complete",
                    description = "Checked ${packages.size} app(s), ${reappearedApps.size} reappeared.",
                    profileId = profileId
                )
            )

            recordCheckEvent(checkedCount, profileId)

        } catch (e: Exception) {
            errors.add("Monitoring check failed: ${e.message}")
            recordErrorHistory(e.message ?: "Unknown error", profileId)
        }

        return MonitoringResult(
            checkedCount = checkedCount,
            reappearanceCount = reappearedApps.size,
            reappearedApps = reappearedApps,
            errors = errors,
            timestamp = System.currentTimeMillis()
        )
    }

    private suspend fun loadMonitoredPackages(profileId: Long?): List<String> {
        return if (profileId != null) {
            activityEventRepository.getAll(1000)
                .map { it.packageName }
                .distinct()
        } else {
            activityEventRepository.getAll(1000)
                .map { it.packageName }
                .distinct()
        }
    }

    private suspend fun notifyReappearance(reappearedApps: List<String>, notificationsEnabled: Boolean) {
        if (!notificationsEnabled) return

        val reappearedWithNames = reappearedApps.map { packageName ->
            packageName to context.getAppName(packageName)
        }

        if (reappearedWithNames.size == 1) {
            val (packageName, appName) = reappearedWithNames.first()
            notificationHelper.showReappearanceNotification(packageName, appName)
        } else {
            notificationHelper.showSummaryNotification(reappearedWithNames)
        }
    }

    private suspend fun recordCheckEvent(checkedCount: Int, profileId: Long?) {
        activityEventRepository.insert(
            ActivityEvent(
                packageName = "system",
                timestamp = System.currentTimeMillis(),
                eventType = EventType.CHECK_COMPLETED,
                source = "MonitoringEngine",
                profileId = profileId
            )
        )
    }

    private suspend fun recordErrorHistory(message: String, profileId: Long?) {
        historyRepository.insert(
            HistoryEvent(
                timestamp = System.currentTimeMillis(),
                eventType = HistoryEventType.ERROR,
                title = "Monitoring error",
                description = message,
                profileId = profileId
            )
        )
    }

    private suspend fun recordPermissionError(message: String, profileId: Long?) {
        historyRepository.insert(
            HistoryEvent(
                timestamp = System.currentTimeMillis(),
                eventType = HistoryEventType.PERMISSION,
                title = "Permission required",
                description = message,
                profileId = profileId
            )
        )
    }

    private fun Context.getAppName(packageName: String): String {
        return try {
            val pm = packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString()
        } catch (_: Exception) {
            packageName
        }
    }
}
