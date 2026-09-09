package com.appcontrol.domain.usecase

class ScheduleMonitoringUseCase(
    private val scheduler: SchedulerDelegate
) {
    suspend operator fun invoke() {
        scheduler.schedule()
    }

    suspend fun cancel() {
        scheduler.cancel()
    }

    fun interface SchedulerDelegate {
        suspend fun schedule()
        suspend fun cancel() = Unit
    }
}