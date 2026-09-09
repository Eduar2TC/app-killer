package com.appcontrol.data.repository

import com.appcontrol.core.database.dao.ActivityEventDao
import com.appcontrol.core.database.dao.EventStat
import com.appcontrol.domain.model.ActivityEvent
import com.appcontrol.domain.model.EventType
import com.appcontrol.domain.repository.ActivityEventRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class ActivityEventRepositoryImpl(
    private val activityEventDao: ActivityEventDao
) : ActivityEventRepository {

    override suspend fun insert(event: ActivityEvent): Long {
        activityEventDao.insertEvent(event.toEntity())
        return activityEventDao.getRecentEvents(1).first().firstOrNull()?.id ?: 0L
    }

    override suspend fun getByPackage(packageName: String, limit: Int): List<ActivityEvent> =
        activityEventDao.getEventsByPackage(packageName).first().take(limit).map { it.toDomain() }

    override suspend fun getAll(limit: Int): List<ActivityEvent> =
        activityEventDao.getRecentEvents(limit).first().map { it.toDomain() }

    override suspend fun getLatest(packageName: String): ActivityEvent? =
        activityEventDao.getEventsByPackage(packageName).first().firstOrNull()?.toDomain()

    override suspend fun getLatestByType(packageName: String, eventType: EventType): ActivityEvent? =
        activityEventDao.getEventsByPackage(packageName)
            .first()
            .firstOrNull { it.eventType == eventType.name }
            ?.toDomain()

    override fun observeEvents(packageName: String): Flow<List<ActivityEvent>> =
        activityEventDao.getEventsByPackage(packageName).map { events -> events.map { it.toDomain() } }

    override suspend fun cleanup(beforeTimestamp: Long): Int {
        val stale = activityEventDao.getAllEvents().first().filter { it.timestamp < beforeTimestamp }
        activityEventDao.deleteEventsOlderThan(beforeTimestamp)
        return stale.size
    }

    override suspend fun clearAll(): Int {
        val count = activityEventDao.getAllEvents().first().size
        activityEventDao.deleteAllEvents()
        return count
    }

    suspend fun getEventStats(eventType: EventType, since: Long): List<EventStat> =
        activityEventDao.getEventStatsByTypeSince(eventType.name, since).first()

    suspend fun getPackagesWithEventTypeSince(eventType: EventType, since: Long): List<String> =
        activityEventDao.getPackagesWithEventTypeSince(eventType.name, since).first()
}