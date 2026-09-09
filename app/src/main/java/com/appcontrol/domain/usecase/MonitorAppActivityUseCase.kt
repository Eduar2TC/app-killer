package com.appcontrol.domain.usecase

import com.appcontrol.domain.model.ActivityEvent
import com.appcontrol.domain.model.AppInfo
import com.appcontrol.domain.model.AppStatus
import com.appcontrol.domain.model.EventType
import com.appcontrol.domain.model.HistoryEvent
import com.appcontrol.domain.model.HistoryEventType
import com.appcontrol.domain.repository.ActivityEventRepository
import com.appcontrol.domain.repository.AppRepository
import com.appcontrol.domain.repository.HistoryRepository

class MonitorAppActivityUseCase(
    private val appRepository: AppRepository,
    private val activityEventRepository: ActivityEventRepository,
    private val historyRepository: HistoryRepository
) {
    suspend operator fun invoke(): List<AppInfo> {
        val monitored = appRepository.getAllApps().filter { it.isSelected && !it.isExcluded }
        val now = System.currentTimeMillis()
        val updated = mutableListOf<AppInfo>()

        for (app in monitored) {
            val latest = activityEventRepository.getLatest(app.packageName)
            val isActive = isAppActive(app.packageName)

            if (isActive) {
                val reappeared = latest != null && latest.eventType == EventType.APP_INACTIVE
                val eventType = if (reappeared) EventType.APP_REAPPEARED else EventType.APP_ACTIVE

                activityEventRepository.insert(
                    ActivityEvent(
                        packageName = app.packageName,
                        timestamp = now,
                        eventType = eventType,
                        source = "MonitorAppActivityUseCase"
                    )
                )

                if (reappeared) {
                    historyRepository.insert(
                        HistoryEvent(
                            timestamp = now,
                            eventType = HistoryEventType.REAPPEARED,
                            title = "App reappeared",
                            description = "${app.label} became active again.",
                            packageName = app.packageName
                        )
                    )
                    updated.add(app.copy(status = AppStatus.REAPPEARED, lastActivityTime = now))
                } else {
                    updated.add(app.copy(status = AppStatus.ACTIVE, lastActivityTime = now))
                }
            } else {
                activityEventRepository.insert(
                    ActivityEvent(
                        packageName = app.packageName,
                        timestamp = now,
                        eventType = EventType.APP_INACTIVE,
                        source = "MonitorAppActivityUseCase"
                    )
                )
                updated.add(app.copy(status = AppStatus.INACTIVE, lastActivityTime = now))
            }
        }

        return updated
    }

    suspend fun isAppActive(packageName: String): Boolean {
        val latest = activityEventRepository.getLatest(packageName)
        if (latest == null) return false
        return latest.eventType == EventType.APP_ACTIVE ||
            latest.eventType == EventType.APP_REAPPEARED
    }
}