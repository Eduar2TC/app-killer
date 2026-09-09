package com.appcontrol.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.appcontrol.core.database.entity.HistoryEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {

    @Query("SELECT * FROM history_event ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<HistoryEventEntity>>

    @Query("SELECT * FROM history_event ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentEvents(limit: Int): Flow<List<HistoryEventEntity>>

    @Query("SELECT * FROM history_event WHERE event_type = :eventType ORDER BY timestamp DESC")
    fun getEventsByType(eventType: String): Flow<List<HistoryEventEntity>>

    @Query("SELECT * FROM history_event WHERE package_name = :packageName ORDER BY timestamp DESC")
    fun getEventsByPackage(packageName: String): Flow<List<HistoryEventEntity>>

    @Query("SELECT * FROM history_event WHERE profile_id = :profileId ORDER BY timestamp DESC")
    fun getEventsByProfile(profileId: Long): Flow<List<HistoryEventEntity>>

    @Query("SELECT * FROM history_event WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getEventsByTimeRange(startTime: Long, endTime: Long): Flow<List<HistoryEventEntity>>

    @Query("SELECT * FROM history_event WHERE timestamp >= :since ORDER BY timestamp DESC")
    fun getEventsSince(since: Long): Flow<List<HistoryEventEntity>>

    @Query("SELECT COUNT(*) FROM history_event")
    fun getEventCount(): Flow<Int>

    @Query("SELECT * FROM history_event ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestEvent(): HistoryEventEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: HistoryEventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<HistoryEventEntity>)

    @Query("DELETE FROM history_event WHERE id = :eventId")
    suspend fun deleteEventById(eventId: Long)

    @Query("DELETE FROM history_event WHERE timestamp < :cutoffTime")
    suspend fun deleteEventsOlderThan(cutoffTime: Long): Int

    @Query("DELETE FROM history_event WHERE package_name = :packageName")
    suspend fun deleteEventsForPackage(packageName: String)

    @Query("DELETE FROM history_event")
    suspend fun deleteAllEvents(): Int

    @Query("DELETE FROM history_event WHERE id NOT IN (SELECT id FROM history_event ORDER BY timestamp DESC LIMIT :keepCount)")
    suspend fun keepMostRecentEvents(keepCount: Int)

    @Query("""
        SELECT COUNT(*) FROM history_event 
        WHERE package_name = :packageName 
        AND event_type = :eventType 
        AND timestamp > :sinceTimestamp
    """)
    suspend fun countEventsSince(packageName: String, eventType: String, sinceTimestamp: Long): Int
}
