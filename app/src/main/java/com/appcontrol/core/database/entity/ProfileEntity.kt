package com.appcontrol.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "enabled")
    val enabled: Boolean = true,

    @ColumnInfo(name = "schedule_enabled")
    val scheduleEnabled: Boolean = false,

    @ColumnInfo(name = "start_time")
    val startTime: Int = 0,

    @ColumnInfo(name = "end_time")
    val endTime: Int = 0,

    @ColumnInfo(name = "monitoring_interval")
    val monitoringInterval: Int = 15,

    @ColumnInfo(name = "notification_enabled")
    val notificationEnabled: Boolean = true,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
