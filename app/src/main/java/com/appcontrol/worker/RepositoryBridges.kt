package com.appcontrol.worker

import androidx.room.withTransaction
import com.appcontrol.core.database.AppDatabase
import com.appcontrol.core.database.entity.ActivityEventEntity
import com.appcontrol.core.database.entity.HistoryEventEntity
import com.appcontrol.domain.model.ActivityEvent
import com.appcontrol.domain.model.EventType
import com.appcontrol.domain.model.HistoryEvent
import com.appcontrol.domain.model.HistoryEventType
import com.appcontrol.domain.repository.ActivityEventRepository
import com.appcontrol.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

class DatabaseActivityEventRepository(
    private val database: AppDatabase
) : ActivityEventRepository {

    private val dao = database.activityEventDao()

    override suspend fun insert(event: ActivityEvent): Long {
        return database.withTransaction {
            dao.insertEvent(
                ActivityEventEntity(
                    packageName = event.packageName,
                    timestamp = event.timestamp,
                    eventType = event.eventType.name,
                    source = event.source,
                    profileId = event.profileId
                )
            )
        }
    }

    override suspend fun getByPackage(packageName: String, limit: Int): List<ActivityEvent> {
        return dao.getEventsByPackage(packageName).first().take(limit).map { it.toModel() }
    }

    override suspend fun getAll(limit: Int): List<ActivityEvent> {
        return dao.getRecentEvents(limit).first().map { it.toModel() }
    }

    override suspend fun getLatest(packageName: String): ActivityEvent? {
        return dao.getEventsByPackage(packageName).first()
            .maxByOrNull { it.timestamp }
            ?.toModel()
    }

    override suspend fun getLatestByType(packageName: String, eventType: EventType): ActivityEvent? {
        return dao.getEventsByPackage(packageName).first()
            .filter { it.eventType == eventType.name }
            .maxByOrNull { it.timestamp }
            ?.toModel()
    }

    override fun observeEvents(packageName: String): Flow<List<ActivityEvent>> = flow {
        dao.getEventsByPackage(packageName).collect { entities ->
            emit(entities.map { it.toModel() })
        }
    }

    override suspend fun cleanup(beforeTimestamp: Long): Int {
        return database.withTransaction { dao.deleteEventsOlderThan(beforeTimestamp) }
    }

    override suspend fun clearAll(): Int {
        return database.withTransaction { dao.deleteAllEvents() }
    }

    private fun ActivityEventEntity.toModel(): ActivityEvent {
        val resolvedType = runCatching { EventType.valueOf(eventType) }
            .getOrDefault(EventType.APP_ACTIVE)
        return ActivityEvent(
            id = id,
            packageName = packageName,
            timestamp = timestamp,
            eventType = resolvedType,
            source = source,
            profileId = profileId
        )
    }
}

class DatabaseHistoryRepository(
    private val database: AppDatabase
) : HistoryRepository {

    private val dao = database.historyDao()

    override suspend fun insert(event: HistoryEvent): Long {
        return database.withTransaction {
            dao.insertEvent(
                HistoryEventEntity(
                    timestamp = event.timestamp,
                    eventType = event.eventType.name,
                    title = event.title,
                    description = event.description,
                    packageName = event.packageName,
                    profileId = event.profileId
                )
            )
        }
    }

    override suspend fun getById(id: Long): HistoryEvent? {
        return dao.getAllEvents().first().firstOrNull { it.id == id }?.toModel()
    }

    override suspend fun getAll(limit: Int): List<HistoryEvent> {
        return dao.getRecentEvents(limit).first().map { it.toModel() }
    }

    override suspend fun getByType(eventType: HistoryEventType, limit: Int): List<HistoryEvent> {
        return dao.getEventsByType(eventType.name).first().take(limit).map { it.toModel() }
    }

    override suspend fun getByPackage(packageName: String, limit: Int): List<HistoryEvent> {
        return dao.getEventsByPackage(packageName).first().take(limit).map { it.toModel() }
    }

    override suspend fun getByProfile(profileId: Long, limit: Int): List<HistoryEvent> {
        return dao.getEventsByProfile(profileId).first().take(limit).map { it.toModel() }
    }

    override fun observeEvents(): Flow<List<HistoryEvent>> = flow {
        dao.getAllEvents().collect { entities ->
            emit(entities.map { it.toModel() })
        }
    }

    override suspend fun cleanup(beforeTimestamp: Long): Int {
        return database.withTransaction { dao.deleteEventsOlderThan(beforeTimestamp) }
    }

    override suspend fun clearAll(): Int {
        return database.withTransaction { dao.deleteAllEvents() }
    }

    private fun HistoryEventEntity.toModel(): HistoryEvent {
        val resolvedType = runCatching { HistoryEventType.valueOf(eventType) }
            .getOrDefault(HistoryEventType.CHECK)
        return HistoryEvent(
            id = id,
            timestamp = timestamp,
            eventType = resolvedType,
            title = title,
            description = description,
            packageName = packageName,
            profileId = profileId
        )
    }
}