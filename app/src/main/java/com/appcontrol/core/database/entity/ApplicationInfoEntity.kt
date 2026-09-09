package com.appcontrol.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "application_info")
data class ApplicationInfoEntity(
    @PrimaryKey
    @ColumnInfo(name = "package_name")
    val packageName: String,

    @ColumnInfo(name = "label")
    val label: String,

    @ColumnInfo(name = "version_name")
    val versionName: String,

    @ColumnInfo(name = "version_code")
    val versionCode: Long,

    @ColumnInfo(name = "first_install_time")
    val firstInstallTime: Long,

    @ColumnInfo(name = "last_update_time")
    val lastUpdateTime: Long,

    @ColumnInfo(name = "is_system_app")
    val isSystemApp: Boolean,

    @ColumnInfo(name = "is_selected")
    val isSelected: Boolean = false,

    @ColumnInfo(name = "is_excluded")
    val isExcluded: Boolean = false,

    @ColumnInfo(name = "last_checked")
    val lastChecked: Long = 0L
)
