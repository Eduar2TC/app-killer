package com.appcontrol.data.repository

import com.appcontrol.core.database.dao.HistoryDao
import com.appcontrol.domain.model.HistoryEvent
import com.appcontrol.domain.model.HistoryEventType
import com.appcontrol.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class HistoryRepositoryImpl(private val historyDao: HistoryDao) : HistoryRepository {

    override suspend fun insert(event: HistoryEvent): Long {
        historyDao.insertEvent(event.toEntity())
        return historyDao.getLatestEvent()?.id ?: 0L
    }

    override suspend fun getById(id: Long): HistoryEvent? =
        historyDao.getAllEvents().first().firstOrNull { it.id == id }?.toDomain()

    override suspend fun getAll(limit: Int): List<HistoryEvent> =
        historyDao.getRecentEvents(limit).first().map { it.toDomain() }

    override suspend fun getByType(eventType: HistoryEventType, limit: Int): List<HistoryEvent> =
        historyDao.getEventsByType(eventType.name).first().take(limit).map { it.toDomain() }

    override suspend fun getByPackage(packageName: String, limit: Int): List<HistoryEvent> =
        historyDao.getEventsByPackage(packageName).first().take(limit).map { it.toDomain() }

    override suspend fun getByProfile(profileId: Long, limit: Int): List<HistoryEvent> =
        historyDao.getEventsByProfile(profileId).first().take(limit).map { it.toDomain() }

    override fun observeEvents(): Flow<List<HistoryEvent>> =
        historyDao.getAllEvents().map { events -> events.map { it.toDomain() } }

    override suspend fun cleanup(beforeTimestamp: Long): Int {
        val stale = historyDao.getAllEvents().first().filter { it.timestamp < beforeTimestamp }
        historyDao.deleteEventsOlderThan(beforeTimestamp)
        return stale.size
    }

    override suspend fun clearAll(): Int {
        val count = historyDao.getAllEvents().first().size
        historyDao.deleteAllEvents()
        return count
    }
}