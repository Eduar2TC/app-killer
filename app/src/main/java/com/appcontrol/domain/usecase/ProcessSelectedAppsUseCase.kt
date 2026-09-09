package com.appcontrol.domain.usecase

import com.appcontrol.data.system.ProcessStopper
import com.appcontrol.domain.model.ActivityEvent
import com.appcontrol.domain.model.AppInfo
import com.appcontrol.domain.model.EventType
import com.appcontrol.domain.model.HistoryEvent
import com.appcontrol.domain.model.HistoryEventType
import com.appcontrol.domain.model.ProcessOutcome
import com.appcontrol.domain.model.ProcessResult
import com.appcontrol.domain.model.StopResult
import com.appcontrol.domain.repository.ActivityEventRepository
import com.appcontrol.domain.repository.AppRepository
import com.appcontrol.domain.repository.HistoryRepository

class ProcessSelectedAppsUseCase(
    private val appRepository: AppRepository,
    private val historyRepository: HistoryRepository,
    private val activityEventRepository: ActivityEventRepository,
    private val monitorAppActivity: MonitorAppActivityUseCase,
    private val processStopper: ProcessStopper
) {
    suspend operator fun invoke(): ProcessResult {
        val selected = appRepository.getSelectedApps().filter { !it.isExcluded }
        val total = selected.size
        var stopped = 0
        var inactive = 0
        var failed = 0
        val results = mutableMapOf<String, ProcessOutcome>()

        for (app in selected) {
            val outcome = processApp(app)
            results[app.packageName] = outcome
            when {
                outcome.success && outcome.actionTaken -> stopped++
                outcome.success && !outcome.actionTaken -> inactive++
                else -> failed++
            }
        }

        return ProcessResult(
            total = total,
            stopped = stopped,
            inactive = inactive,
            failed = failed,
            results = results
        )
    }

    private suspend fun processApp(app: AppInfo): ProcessOutcome {
        return try {
            val isActive = monitorAppActivity.isAppActive(app.packageName)
            if (!isActive) {
                ProcessOutcome(
                    packageName = app.packageName,
                    success = true,
                    message = "App is not running. Nothing to stop.",
                    actionTaken = false
                )
            } else {
                when (processStopper.stopPackage(app.packageName)) {
                    StopResult.STOPPED -> {
                        recordStopped(app)
                        ProcessOutcome(
                            packageName = app.packageName,
                            success = true,
                            message = "${app.label} was stopped.",
                            actionTaken = true
                        )
                    }
                    StopResult.NOT_RUNNING -> ProcessOutcome(
                        packageName = app.packageName,
                        success = true,
                        message = "App is not running. Nothing to stop.",
                        actionTaken = false
                    )
                    StopResult.FAILED -> {
                        recordError(app, "Failed to stop app")
                        ProcessOutcome(
                            packageName = app.packageName,
                            success = false,
                            message = "Failed to stop app.",
                            actionTaken = false
                        )
                    }
                }
            }
        } catch (e: Exception) {
            recordError(app, e.message ?: "Unknown error")
            ProcessOutcome(
                packageName = app.packageName,
                success = false,
                message = e.message ?: "Unknown error",
                actionTaken = false
            )
        }
    }

    private suspend fun recordStopped(app: AppInfo) {
        val now = System.currentTimeMillis()
        activityEventRepository.insert(
            ActivityEvent(
                packageName = app.packageName,
                timestamp = now,
                eventType = EventType.APP_INACTIVE,
                source = "ProcessSelectedAppsUseCase"
            )
        )
        historyRepository.insert(
            HistoryEvent(
                timestamp = now,
                eventType = HistoryEventType.ACTION,
                title = "App stopped",
                description = "${app.label} was stopped. All its background processes were killed.",
                packageName = app.packageName
            )
        )
    }

    private suspend fun recordError(app: AppInfo, message: String) {
        historyRepository.insert(
            HistoryEvent(
                timestamp = System.currentTimeMillis(),
                eventType = HistoryEventType.ACTION,
                title = "App not stopped",
                description = "$message (${app.packageName})",
                packageName = app.packageName
            )
        )
    }
}