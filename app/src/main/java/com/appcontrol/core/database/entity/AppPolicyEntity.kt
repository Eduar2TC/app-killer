package com.appcontrol.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "app_policy",
    indices = [Index(value = ["package_name"], unique = true)]
)
data class AppPolicyEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "package_name")
    val packageName: String,

    @ColumnInfo(name = "enabled")
    val enabled: Boolean = true,

    @ColumnInfo(name = "monitor_enabled")
    val monitorEnabled: Boolean = true,

    @ColumnInfo(name = "notification_enabled")
    val notificationEnabled: Boolean = true,

    @ColumnInfo(name = "retry_enabled")
    val retryEnabled: Boolean = true,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
