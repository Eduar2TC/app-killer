package com.appcontrol

import com.appcontrol.core.database.dao.HistoryDao
import com.appcontrol.core.database.entity.HistoryEventEntity
import com.appcontrol.domain.model.ActivityEvent
import com.appcontrol.domain.model.AppInfo
import com.appcontrol.domain.model.EventType
import com.appcontrol.domain.model.HistoryEvent
import com.appcontrol.domain.model.HistoryEventType
import com.appcontrol.domain.model.Profile
import com.appcontrol.domain.repository.ActivityEventRepository
import com.appcontrol.domain.repository.AppRepository
import com.appcontrol.domain.repository.HistoryRepository
import com.appcontrol.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

class FakeHistoryDao : HistoryDao {

    val events = mutableListOf<HistoryEventEntity>()
    private var nextId = 1L

    override fun getAllEvents(): Flow<List<HistoryEventEntity>> =
        flowOf(events.sortedByDescending { it.timestamp })

    override fun getRecentEvents(limit: Int): Flow<List<HistoryEventEntity>> =
        flowOf(events.sortedByDescending { it.timestamp }.take(limit))

    override fun getEventsByType(eventType: String): Flow<List<HistoryEventEntity>> =
        flowOf(events.filter { it.eventType == eventType }.sortedByDescending { it.timestamp })

    override fun getEventsByPackage(packageName: String): Flow<List<HistoryEventEntity>> =
        flowOf(events.filter { it.packageName == packageName }.sortedByDescending { it.timestamp })

    override fun getEventsByProfile(profileId: Long): Flow<List<HistoryEventEntity>> =
        flowOf(events.filter { it.profileId == profileId }.sortedByDescending { it.timestamp })

    override fun getEventsByTimeRange(startTime: Long, endTime: Long): Flow<List<HistoryEventEntity>> =
        flowOf(events.filter { it.timestamp in startTime..endTime }.sortedByDescending { it.timestamp })

    override fun getEventsSince(since: Long): Flow<List<HistoryEventEntity>> =
        flowOf(events.filter { it.timestamp >= since }.sortedByDescending { it.timestamp })

    override fun getEventCount(): Flow<Int> = flowOf(events.size)

    override suspend fun getLatestEvent(): HistoryEventEntity? = events.maxByOrNull { it.id }

    override suspend fun insertEvent(event: HistoryEventEntity) {
        events += if (event.id > 0L) event else event.copy(id = nextId++)
    }

    override suspend fun insertEvents(items: List<HistoryEventEntity>) {
        items.forEach { insertEvent(it) }
    }

    override suspend fun deleteEventById(eventId: Long) {
        events.removeAll { it.id == eventId }
    }

    override suspend fun deleteEventsOlderThan(cutoffTime: Long): Int {
        val before = events.size
        events.removeAll { it.timestamp < cutoffTime }
        return before - events.size
    }

    override suspend fun deleteEventsForPackage(packageName: String) {
        events.removeAll { it.packageName == packageName }
    }

    override suspend fun deleteAllEvents(): Int {
        val count = events.size
        events.clear()
        return count
    }

    override suspend fun keepMostRecentEvents(keepCount: Int) {
        if (events.size <= keepCount) return
        val sorted = events.sortedByDescending { it.timestamp }
        val keepIds = sorted.take(keepCount).map { it.id }.toSet()
        events.removeAll { it.id !in keepIds }
    }

    override suspend fun countEventsSince(packageName: String, eventType: String, sinceTimestamp: Long): Int =
        events.count {
            it.packageName == packageName && it.eventType == eventType && it.timestamp > sinceTimestamp
        }
}

class FakeAppRepository : AppRepository {

    val apps = mutableListOf<AppInfo>()

    override suspend fun getAllApps(): List<AppInfo> = apps.toList()

    override suspend fun getSelectedApps(): List<AppInfo> =
        apps.filter { it.isSelected && !it.isExcluded }

    override suspend fun updateSelection(packageName: String, isSelected: Boolean): Boolean {
        val index = apps.indexOfFirst { it.packageName == packageName }
        if (index < 0) return false
        apps[index] = apps[index].copy(isSelected = isSelected)
        return true
    }

    override suspend fun updateExclusion(packageName: String, isExcluded: Boolean): Boolean {
        val index = apps.indexOfFirst { it.packageName == packageName }
        if (index < 0) return false
        apps[index] = apps[index].copy(isExcluded = isExcluded)
        return true
    }

    override suspend fun search(query: String): List<AppInfo> {
        val normalized = query.lowercase()
        return apps.filter {
            it.label.lowercase().contains(normalized) ||
                it.packageName.lowercase().contains(normalized)
        }
    }

    override suspend fun getInstalledApps(): List<AppInfo> = apps.toList()

    override fun observeSelectedApps(): Flow<List<AppInfo>> =
        MutableStateFlow(apps.filter { it.isSelected && !it.isExcluded })
}

class FakeActivityEventRepository : ActivityEventRepository {

    val latestByPackage = mutableMapOf<String, ActivityEvent>()
    val queuedLatest = mutableMapOf<String, ArrayDeque<ActivityEvent>>()
    val events = mutableListOf<ActivityEvent>()
    var throwOnGetLatest = false

    override suspend fun insert(event: ActivityEvent): Long {
        events += event
        latestByPackage[event.packageName] = event
        return events.size.toLong()
    }

    override suspend fun getByPackage(packageName: String, limit: Int): List<ActivityEvent> =
        events.filter { it.packageName == packageName }
            .sortedByDescending { it.timestamp }
            .take(limit)

    override suspend fun getAll(limit: Int): List<ActivityEvent> =
        events.sortedByDescending { it.timestamp }.take(limit)

    override suspend fun getLatest(packageName: String): ActivityEvent? {
        if (throwOnGetLatest) throw IllegalStateException("usage stats unavailable")
        val queue = queuedLatest[packageName]
        if (!queue.isNullOrEmpty()) return queue.removeFirst()
        return latestByPackage[packageName]
    }

    override suspend fun getLatestByType(packageName: String, eventType: EventType): ActivityEvent? =
        events.lastOrNull { it.packageName == packageName && it.eventType == eventType }

    override fun observeEvents(packageName: String): Flow<List<ActivityEvent>> = flowOf(
        events.filter { it.packageName == packageName }
    )

    override suspend fun cleanup(beforeTimestamp: Long): Int {
        val before = events.size
        events.removeAll { it.timestamp < beforeTimestamp }
        return before - events.size
    }

    override suspend fun clearAll(): Int {
        val count = events.size
        events.clear()
        latestByPackage.clear()
        queuedLatest.clear()
        throwOnGetLatest = false
        return count
    }
}

class FakeHistoryRepository : HistoryRepository {

    val events = mutableListOf<HistoryEvent>()

    override suspend fun insert(event: HistoryEvent): Long {
        val id = (events.maxOfOrNull { it.id } ?: 0L) + 1L
        events += event.copy(id = id)
        return id
    }

    override suspend fun getById(id: Long): HistoryEvent? =
        events.firstOrNull { it.id == id }

    override suspend fun getAll(limit: Int): List<HistoryEvent> =
        events.sortedByDescending { it.timestamp }.take(limit)

    override suspend fun getByType(eventType: HistoryEventType, limit: Int): List<HistoryEvent> =
        events.filter { it.eventType == eventType }
            .sortedByDescending { it.timestamp }
            .take(limit)

    override suspend fun getByPackage(packageName: String, limit: Int): List<HistoryEvent> =
        events.filter { it.packageName == packageName }
            .sortedByDescending { it.timestamp }
            .take(limit)

    override suspend fun getByProfile(profileId: Long, limit: Int): List<HistoryEvent> =
        events.filter { it.profileId == profileId }
            .sortedByDescending { it.timestamp }
            .take(limit)

    override fun observeEvents(): Flow<List<HistoryEvent>> =
        flowOf(events.sortedByDescending { it.timestamp })

    override suspend fun cleanup(beforeTimestamp: Long): Int {
        val before = events.size
        events.removeAll { it.timestamp < beforeTimestamp }
        return before - events.size
    }

    override suspend fun clearAll(): Int {
        val count = events.size
        events.clear()
        return count
    }
}

class FakeProfileRepository : ProfileRepository {

    val profiles = mutableMapOf<Long, Profile>()
    var activeProfileId: Long? = null
    private var nextId = 1L

    override suspend fun getProfile(id: Long): Profile? = profiles[id]

    override suspend fun getAllProfiles(): List<Profile> =
        profiles.values.sortedBy { it.name }

    override suspend fun saveProfile(profile: Profile): Long {
        val id = if (profile.id > 0L) profile.id else nextId++
        profiles[id] = profile.copy(id = id)
        return id
    }

    override suspend fun updateProfile(profile: Profile): Boolean {
        if (!profiles.containsKey(profile.id)) return false
        profiles[profile.id] = profile
        return true
    }

    override suspend fun deleteProfile(id: Long): Boolean =
        profiles.remove(id) != null

    override suspend fun getActiveProfile(): Profile? =
        activeProfileId?.let { profiles[it] }

    override suspend fun setActiveProfile(id: Long?): Boolean {
        if (id == null) {
            activeProfileId = null
            return true
        }
        if (!profiles.containsKey(id)) return false
        activeProfileId = id
        return true
    }

    override fun observeProfiles(): Flow<List<Profile>> =
        MutableStateFlow(profiles.values.sortedBy { it.name })
}