package com.appcontrol.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "history_event",
    indices = [Index(value = ["timestamp"])]
)
data class HistoryEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    @ColumnInfo(name = "event_type")
    val eventType: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "description")
    val description: String = "",

    @ColumnInfo(name = "package_name")
    val packageName: String? = null,

    @ColumnInfo(name = "profile_id")
    val profileId: Long? = null
) {
    companion object {
        const val EVENT_TYPE_CHECK = "CHECK"
        const val EVENT_TYPE_REAPPEARED = "REAPPEARED"
        const val EVENT_TYPE_ACTION = "ACTION"
        const val EVENT_TYPE_ERROR = "ERROR"
        const val EVENT_TYPE_PERMISSION = "PERMISSION"
    }
}
