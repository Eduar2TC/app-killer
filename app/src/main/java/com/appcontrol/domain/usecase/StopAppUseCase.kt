package com.appcontrol.domain.usecase

import com.appcontrol.data.system.ProcessStopper
import com.appcontrol.domain.model.ActivityEvent
import com.appcontrol.domain.model.EventType
import com.appcontrol.domain.model.HistoryEvent
import com.appcontrol.domain.model.HistoryEventType
import com.appcontrol.domain.model.ProcessOutcome
import com.appcontrol.domain.model.StopResult
import com.appcontrol.domain.repository.ActivityEventRepository
import com.appcontrol.domain.repository.HistoryRepository

class StopAppUseCase(
    private val activityEventRepository: ActivityEventRepository,
    private val historyRepository: HistoryRepository,
    private val processStopper: ProcessStopper
) {
    suspend operator fun invoke(packageName: String, label: String): ProcessOutcome {
        return try {
            when (processStopper.stopPackage(packageName)) {
                StopResult.STOPPED -> {
                    val now = System.currentTimeMillis()
                    activityEventRepository.insert(
                        ActivityEvent(
                            packageName = packageName,
                            timestamp = now,
                            eventType = EventType.APP_INACTIVE,
                            source = "StopAppUseCase"
                        )
                    )
                    historyRepository.insert(
                        HistoryEvent(
                            timestamp = now,
                            eventType = HistoryEventType.ACTION,
                            title = "App stopped",
                            description = "$label was stopped. All its background processes were killed.",
                            packageName = packageName
                        )
                    )
                    ProcessOutcome(
                        packageName = packageName,
                        success = true,
                        message = "$label was stopped.",
                        actionTaken = true
                    )
                }
                StopResult.NOT_RUNNING -> ProcessOutcome(
                    packageName = packageName,
                    success = true,
                    message = "$label is not running. Nothing to stop.",
                    actionTaken = false
                )
                StopResult.FAILED -> ProcessOutcome(
                    packageName = packageName,
                    success = false,
                    message = "$label could not be stopped.",
                    actionTaken = false
                )
            }
        } catch (e: Exception) {
            ProcessOutcome(
                packageName = packageName,
                success = false,
                message = e.message ?: "Unknown error",
                actionTaken = false
            )
        }
    }
}