package com.appcontrol.domain.repository

import com.appcontrol.domain.model.HistoryEvent
import kotlinx.coroutines.flow.Flow

interface HistoryRepository {
    suspend fun insert(event: HistoryEvent): Long
    suspend fun getById(id: Long): HistoryEvent?
    suspend fun getAll(limit: Int = 200): List<HistoryEvent>
    suspend fun getByType(eventType: com.appcontrol.domain.model.HistoryEventType, limit: Int = 200): List<HistoryEvent>
    suspend fun getByPackage(packageName: String, limit: Int = 200): List<HistoryEvent>
    suspend fun getByProfile(profileId: Long, limit: Int = 200): List<HistoryEvent>
    fun observeEvents(): Flow<List<HistoryEvent>>
    suspend fun cleanup(beforeTimestamp: Long): Int
    suspend fun clearAll(): Int
}