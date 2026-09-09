package com.appcontrol.domain.usecase

import com.appcontrol.domain.model.HistoryEventType
import com.appcontrol.domain.repository.ActivityEventRepository
import com.appcontrol.domain.repository.AppRepository
import com.appcontrol.domain.repository.HistoryRepository

data class AppMonitoringStats(
    val packageName: String,
    val totalEvents: Int,
    val activeCount: Int,
    val inactiveCount: Int,
    val reappearedCount: Int
)

data class StatisticsResult(
    val totalMonitoredApps: Int,
    val totalEvents: Int,
    val activeNow: Int,
    val inactiveNow: Int,
    val reappearedTotal: Int,
    val checkCount: Int,
    val errorCount: Int,
    val perAppStats: List<AppMonitoringStats>
)

class GetStatisticsUseCase(
    private val appRepository: AppRepository,
    private val historyRepository: HistoryRepository,
    private val activityEventRepository: ActivityEventRepository
) {
    suspend operator fun invoke(): StatisticsResult {
        val apps = appRepository.getAllApps().filter { it.isSelected }
        val historyEvents = historyRepository.getAll(limit = Int.MAX_VALUE)
        val activityEvents = activityEventRepository.getAll(limit = Int.MAX_VALUE)

        val perAppStats = apps.map { app ->
            val appEvents = activityEvents.filter { it.packageName == app.packageName }
            AppMonitoringStats(
                packageName = app.packageName,
                totalEvents = appEvents.size,
                activeCount = appEvents.count { it.eventType == com.appcontrol.domain.model.EventType.APP_ACTIVE },
                inactiveCount = appEvents.count { it.eventType == com.appcontrol.domain.model.EventType.APP_INACTIVE },
                reappearedCount = appEvents.count { it.eventType == com.appcontrol.domain.model.EventType.APP_REAPPEARED }
            )
        }

        return StatisticsResult(
            totalMonitoredApps = apps.size,
            totalEvents = activityEvents.size,
            activeNow = apps.count { it.status == com.appcontrol.domain.model.AppStatus.ACTIVE },
            inactiveNow = apps.count { it.status == com.appcontrol.domain.model.AppStatus.INACTIVE },
            reappearedTotal = perAppStats.sumOf { it.reappearedCount },
            checkCount = historyEvents.count { it.eventType == HistoryEventType.CHECK },
            errorCount = historyEvents.count { it.eventType == HistoryEventType.ERROR },
            perAppStats = perAppStats
        )
    }
}