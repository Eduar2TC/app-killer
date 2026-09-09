package com.appcontrol.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "profile_app",
    indices = [
        Index(
            value = ["profile_id", "package_name"],
            unique = true
        )
    ]
)
data class ProfileAppEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "profile_id")
    val profileId: Long,

    @ColumnInfo(name = "package_name")
    val packageName: String,

    @ColumnInfo(name = "enabled")
    val enabled: Boolean = true
)
