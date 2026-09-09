package com.appcontrol.domain.usecase

import com.appcontrol.domain.repository.ActivityEventRepository
import com.appcontrol.domain.repository.HistoryRepository

data class CleanupResult(
    val historyEventsRemoved: Int,
    val activityEventsRemoved: Int
)

class CleanupHistoryUseCase(
    private val historyRepository: HistoryRepository,
    private val activityEventRepository: ActivityEventRepository
) {
    suspend operator fun invoke(retentionDays: Long = 14): CleanupResult {
        val beforeTimestamp = System.currentTimeMillis() - (retentionDays * 24 * 60 * 60 * 1000L)
        val historyRemoved = historyRepository.cleanup(beforeTimestamp)
        val activityRemoved = activityEventRepository.cleanup(beforeTimestamp)
        return CleanupResult(historyRemoved, activityRemoved)
    }
}