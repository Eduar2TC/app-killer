package com.appcontrol.domain.repository

import com.appcontrol.domain.model.AppInfo
import kotlinx.coroutines.flow.Flow

interface AppRepository {
    suspend fun getAllApps(): List<AppInfo>
    suspend fun getSelectedApps(): List<AppInfo>
    suspend fun updateSelection(packageName: String, isSelected: Boolean): Boolean
    suspend fun updateExclusion(packageName: String, isExcluded: Boolean): Boolean
    suspend fun search(query: String): List<AppInfo>
    suspend fun getInstalledApps(): List<AppInfo>
    fun observeSelectedApps(): Flow<List<AppInfo>>
}