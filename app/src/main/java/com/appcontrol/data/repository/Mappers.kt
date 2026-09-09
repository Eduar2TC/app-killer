package com.appcontrol.data.repository

import com.appcontrol.core.common.padWithZero
import com.appcontrol.core.database.entity.ActivityEventEntity
import com.appcontrol.core.database.entity.AppPolicyEntity
import com.appcontrol.core.database.entity.ApplicationInfoEntity
import com.appcontrol.core.database.entity.HistoryEventEntity
import com.appcontrol.core.database.entity.ProfileEntity
import com.appcontrol.domain.model.ActivityEvent
import com.appcontrol.domain.model.AppInfo
import com.appcontrol.domain.model.AppPolicy
import com.appcontrol.domain.model.AppStatus
import com.appcontrol.domain.model.EventType
import com.appcontrol.domain.model.HistoryEvent
import com.appcontrol.domain.model.HistoryEventType
import com.appcontrol.domain.model.Profile

fun ApplicationInfoEntity.toDomain(): AppInfo = AppInfo(
    packageName = packageName,
    label = label,
    versionName = versionName,
    icon = null,
    isSystemApp = isSystemApp,
    isSelected = isSelected,
    isExcluded = isExcluded,
    lastActivityTime = lastChecked,
    status = AppStatus.UNKNOWN
)

fun AppInfo.toEntity(): ApplicationInfoEntity = ApplicationInfoEntity(
    packageName = packageName,
    label = label,
    versionName = versionName ?: "",
    versionCode = 0L,
    firstInstallTime = 0L,
    lastUpdateTime = 0L,
    isSystemApp = isSystemApp,
    isSelected = isSelected,
    isExcluded = isExcluded,
    lastChecked = lastActivityTime
)

fun HistoryEventEntity.toDomain(): HistoryEvent = HistoryEvent(
    id = id,
    timestamp = timestamp,
    eventType = HistoryEventType.valueOf(eventType),
    title = title,
    description = description,
    packageName = packageName,
    profileId = profileId
)

fun HistoryEvent.toEntity(): HistoryEventEntity = HistoryEventEntity(
    id = id,
    timestamp = timestamp,
    eventType = eventType.name,
    title = title,
    description = description,
    packageName = packageName,
    profileId = profileId
)

fun ActivityEventEntity.toDomain(): ActivityEvent = ActivityEvent(
    id = id,
    packageName = packageName,
    timestamp = timestamp,
    eventType = EventType.valueOf(eventType),
    source = source,
    profileId = profileId
)

fun ActivityEvent.toEntity(): ActivityEventEntity = ActivityEventEntity(
    id = id,
    packageName = packageName,
    timestamp = timestamp,
    eventType = eventType.name,
    source = source,
    profileId = profileId
)

fun AppPolicyEntity.toDomain(): AppPolicy = AppPolicy(
    id = id,
    packageName = packageName,
    enabled = enabled,
    monitorEnabled = monitorEnabled,
    notificationEnabled = notificationEnabled,
    retryEnabled = retryEnabled
)

fun AppPolicy.toEntity(): AppPolicyEntity = AppPolicyEntity(
    id = id,
    packageName = packageName,
    enabled = enabled,
    monitorEnabled = monitorEnabled,
    notificationEnabled = notificationEnabled,
    retryEnabled = retryEnabled
)

fun ProfileEntity.toDomain(appPackages: List<String>): Profile = Profile(
    id = id,
    name = name,
    enabled = enabled,
    scheduleEnabled = scheduleEnabled,
    startTime = startTime.toTimeString(),
    endTime = endTime.toTimeString(),
    monitoringInterval = monitoringInterval.toLong() * 60_000L,
    notificationEnabled = notificationEnabled,
    appPackages = appPackages
)

fun Profile.toEntity(): ProfileEntity = ProfileEntity(
    id = id,
    name = name,
    enabled = enabled,
    scheduleEnabled = scheduleEnabled,
    startTime = startTime.toMinutesOfDay(),
    endTime = endTime.toMinutesOfDay(),
    monitoringInterval = (monitoringInterval / 60_000L).toInt().coerceAtLeast(0),
    notificationEnabled = notificationEnabled
)

private fun Int.toTimeString(): String? {
    if (this == 0) return null
    return "${(this / 60).padWithZero()}:${(this % 60).padWithZero()}"
}

private fun String?.toMinutesOfDay(): Int {
    if (this == null) return 0
    val parts = trim().split(":")
    val hours = parts.getOrNull(0)?.toIntOrNull() ?: return 0
    val minutes = parts.getOrNull(1)?.toIntOrNull() ?: return 0
    return hours.coerceIn(0, 23) * 60 + minutes.coerceIn(0, 59)
}