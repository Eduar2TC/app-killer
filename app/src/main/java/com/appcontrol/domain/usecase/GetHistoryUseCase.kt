package com.appcontrol.domain.usecase

import com.appcontrol.domain.model.HistoryEvent
import com.appcontrol.domain.model.HistoryEventType
import com.appcontrol.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow

class GetHistoryUseCase(
    private val historyRepository: HistoryRepository
) {
    suspend operator fun invoke(limit: Int = 200): List<HistoryEvent> =
        historyRepository.getAll(limit)

    suspend fun byType(eventType: HistoryEventType, limit: Int = 200): List<HistoryEvent> =
        historyRepository.getByType(eventType, limit)

    suspend fun byPackage(packageName: String, limit: Int = 200): List<HistoryEvent> =
        historyRepository.getByPackage(packageName, limit)

    suspend fun byProfile(profileId: Long, limit: Int = 200): List<HistoryEvent> =
        historyRepository.getByProfile(profileId, limit)

    fun observe(): Flow<List<HistoryEvent>> = historyRepository.observeEvents()
}