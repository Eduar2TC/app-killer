package com.appcontrol.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.appcontrol.core.database.entity.ActivityEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityEventDao {

    @Query("SELECT * FROM activity_event ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<ActivityEventEntity>>

    @Query("SELECT * FROM activity_event ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentEvents(limit: Int): Flow<List<ActivityEventEntity>>

    @Query("SELECT * FROM activity_event WHERE package_name = :packageName ORDER BY timestamp DESC")
    fun getEventsByPackage(packageName: String): Flow<List<ActivityEventEntity>>

    @Query("SELECT * FROM activity_event WHERE event_type = :eventType ORDER BY timestamp DESC")
    fun getEventsByType(eventType: String): Flow<List<ActivityEventEntity>>

    @Query("SELECT * FROM activity_event WHERE profile_id = :profileId ORDER BY timestamp DESC")
    fun getEventsByProfile(profileId: Long): Flow<List<ActivityEventEntity>>

    @Query("SELECT * FROM activity_event WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getEventsByTimeRange(startTime: Long, endTime: Long): Flow<List<ActivityEventEntity>>

    @Query("SELECT * FROM activity_event WHERE timestamp >= :since ORDER BY timestamp DESC")
    fun getEventsSince(since: Long): Flow<List<ActivityEventEntity>>

    @Query("SELECT * FROM activity_event WHERE package_name = :packageName AND timestamp >= :since ORDER BY timestamp DESC")
    fun getEventsByPackageSince(packageName: String, since: Long): Flow<List<ActivityEventEntity>>

    @Query("SELECT COUNT(*) FROM activity_event")
    fun getEventCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM activity_event WHERE package_name = :packageName AND event_type = :eventType AND timestamp >= :since")
    fun countEventsByTypeSince(packageName: String, eventType: String, since: Long): Flow<Int>

    @Query("""
        SELECT package_name, COUNT(*) as count 
        FROM activity_event 
        WHERE event_type = :eventType AND timestamp >= :since 
        GROUP BY package_name 
        ORDER BY count DESC
    """)
    fun getEventStatsByTypeSince(eventType: String, since: Long): Flow<List<EventStat>>

    @Query("""
        SELECT DISTINCT package_name FROM activity_event 
        WHERE event_type = :eventType AND timestamp >= :since
    """)
    fun getPackagesWithEventTypeSince(eventType: String, since: Long): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: ActivityEventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<ActivityEventEntity>)

    @Query("DELETE FROM activity_event WHERE id = :eventId")
    suspend fun deleteEventById(eventId: Long)

    @Query("DELETE FROM activity_event WHERE timestamp < :cutoffTime")
    suspend fun deleteEventsOlderThan(cutoffTime: Long): Int

    @Query("DELETE FROM activity_event WHERE package_name = :packageName")
    suspend fun deleteEventsForPackage(packageName: String)

    @Query("DELETE FROM activity_event")
    suspend fun deleteAllEvents(): Int

    @Query("DELETE FROM activity_event WHERE id NOT IN (SELECT id FROM activity_event ORDER BY timestamp DESC LIMIT :keepCount)")
    suspend fun keepMostRecentEvents(keepCount: Int)
}

data class EventStat(
    val packageName: String,
    val count: Int
)
