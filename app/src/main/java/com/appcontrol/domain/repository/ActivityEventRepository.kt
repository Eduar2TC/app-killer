package com.appcontrol.domain.repository

import com.appcontrol.domain.model.ActivityEvent
import com.appcontrol.domain.model.EventType
import kotlinx.coroutines.flow.Flow

interface ActivityEventRepository {
    suspend fun insert(event: ActivityEvent): Long
    suspend fun getByPackage(packageName: String, limit: Int = 100): List<ActivityEvent>
    suspend fun getAll(limit: Int = 200): List<ActivityEvent>
    suspend fun getLatest(packageName: String): ActivityEvent?
    suspend fun getLatestByType(packageName: String, eventType: EventType): ActivityEvent?
    fun observeEvents(packageName: String): Flow<List<ActivityEvent>>
    suspend fun cleanup(beforeTimestamp: Long): Int
    suspend fun clearAll(): Int
}