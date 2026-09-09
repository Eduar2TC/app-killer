package com.appcontrol.data.system

import com.appcontrol.domain.model.ActivityEvent

interface UsageStatsProvider {
    suspend fun getRecentActivity(packageName: String, sinceTimestamp: Long): Long
    suspend fun isAppActive(packageName: String, sinceTimestamp: Long): Boolean
    suspend fun getAllRecentActivity(sinceTimestamp: Long): Map<String, Long>
    suspend fun getRecentActivityEvents(sinceTimestamp: Long, untilTimestamp: Long): List<ActivityEvent>
}