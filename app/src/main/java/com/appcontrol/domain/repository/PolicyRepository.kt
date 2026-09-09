package com.appcontrol.domain.repository

import com.appcontrol.domain.model.AppPolicy
import kotlinx.coroutines.flow.Flow

interface PolicyRepository {
    suspend fun getPolicy(packageName: String): AppPolicy?
    suspend fun getAllPolicies(): List<AppPolicy>
    suspend fun savePolicy(policy: AppPolicy): Long
    suspend fun deletePolicy(packageName: String): Boolean
    suspend fun setMonitorEnabled(packageName: String, enabled: Boolean): Boolean
    suspend fun setNotificationEnabled(packageName: String, enabled: Boolean): Boolean
    suspend fun setRetryEnabled(packageName: String, enabled: Boolean): Boolean
    fun observePolicies(): Flow<List<AppPolicy>>
}