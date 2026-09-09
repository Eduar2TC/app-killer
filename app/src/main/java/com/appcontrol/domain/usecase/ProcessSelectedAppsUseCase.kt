package com.appcontrol.domain.usecase

import com.appcontrol.domain.model.ActivityEvent
import com.appcontrol.domain.model.AppInfo
import com.appcontrol.domain.model.EventType
import com.appcontrol.domain.model.HistoryEvent
import com.appcontrol.domain.model.HistoryEventType
import com.appcontrol.domain.model.ProcessOutcome
import com.appcontrol.domain.model.ProcessResult
import com.appcontrol.domain.repository.ActivityEventRepository
import com.appcontrol.domain.repository.AppRepository
import com.appcontrol.domain.repository.HistoryRepository

class ProcessSelectedAppsUseCase(
    private val appRepository: AppRepository,
    private val historyRepository: HistoryRepository,
    private val activityEventRepository: ActivityEventRepository,
    private val monitorAppActivity: MonitorAppActivityUseCase
) {
    suspend operator fun invoke(): ProcessResult {
        val selected = appRepository.getSelectedApps().filter { !it.isExcluded }
        val total = selected.size
        var processed = 0
        var failed = 0
        var notAllowed = 0
        val results = mutableMapOf<String, ProcessOutcome>()

        for (app in selected) {
            val outcome = processApp(app)
            results[app.packageName] = outcome
            when {
                outcome.actionTaken -> processed++
                outcome.success && !outcome.actionTaken -> notAllowed++
                else -> failed++
            }
        }

        return ProcessResult(
            total = total,
            processed = processed,
            failed = failed,
            notAllowed = notAllowed,
            results = results
        )
    }

    private suspend fun processApp(app: AppInfo): ProcessOutcome {
        return try {
            val isActive = monitorAppActivity.isAppActive(app.packageName)
            if (isActive) {
                historyRepository.insert(
                    HistoryEvent(
                        timestamp = System.currentTimeMillis(),
                        eventType = HistoryEventType.ACTION,
                        title = "Manual action required",
                        description = "${app.label} is active but force-stop could not be performed.",
                        packageName = app.packageName
                    )
                )
                storeErrorEvent(app, "Manual action required")
                ProcessOutcome(
                    packageName = app.packageName,
                    success = true,
                    message = "Active app detected. Manual intervention required.",
                    actionTaken = false,
                    requiresManualIntervention = true
                )
            } else {
                storeInactiveEvent(app)
                ProcessOutcome(
                    packageName = app.packageName,
                    success = true,
                    message = "App is inactive. No action needed.",
                    actionTaken = true
                )
            }
        } catch (e: Exception) {
            storeErrorEvent(app, e.message ?: "Unknown error")
            ProcessOutcome(
                packageName = app.packageName,
                success = false,
                message = e.message ?: "Unknown error",
                actionTaken = false,
                requiresManualIntervention = true
            )
        }
    }

    private suspend fun storeInactiveEvent(app: AppInfo) {
        activityEventRepository.insert(
            ActivityEvent(
                packageName = app.packageName,
                timestamp = System.currentTimeMillis(),
                eventType = EventType.APP_INACTIVE,
                source = "ProcessSelectedAppsUseCase"
            )
        )
    }

    private suspend fun storeErrorEvent(app: AppInfo, message: String) {
        historyRepository.insert(
            HistoryEvent(
                timestamp = System.currentTimeMillis(),
                eventType = HistoryEventType.ACTION,
                title = "Action skipped",
                description = message,
                packageName = app.packageName
            )
        )
    }
}