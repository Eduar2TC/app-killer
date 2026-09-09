package com.appcontrol.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "activity_event",
    indices = [Index(value = ["package_name", "timestamp"])]
)
data class ActivityEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "package_name")
    val packageName: String,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    @ColumnInfo(name = "event_type")
    val eventType: String,

    @ColumnInfo(name = "source")
    val source: String,

    @ColumnInfo(name = "profile_id")
    val profileId: Long? = null
) {
    companion object {
        const val EVENT_TYPE_APP_ACTIVE = "APP_ACTIVE"
        const val EVENT_TYPE_APP_INACTIVE = "APP_INACTIVE"
        const val EVENT_TYPE_APP_REAPPEARED = "APP_REAPPEARED"
        const val EVENT_TYPE_CHECK_COMPLETED = "CHECK_COMPLETED"
    }
}
