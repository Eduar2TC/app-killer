package com.appcontrol.data.repository

import com.appcontrol.core.database.dao.ApplicationDao
import com.appcontrol.core.database.entity.ApplicationInfoEntity
import com.appcontrol.data.system.PackageManagerProvider
import com.appcontrol.domain.model.AppInfo
import com.appcontrol.domain.repository.AppRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class AppRepositoryImpl(
    private val applicationDao: ApplicationDao,
    private val packageManagerProvider: PackageManagerProvider
) : AppRepository {

    override suspend fun getAllApps(): List<AppInfo> =
        applicationDao.getAllApps().first().map { it.toDomain() }

    override suspend fun getSelectedApps(): List<AppInfo> =
        applicationDao.getSelectedApps().first().map { it.toDomain() }

    override suspend fun updateSelection(packageName: String, isSelected: Boolean): Boolean {
        val exists = applicationDao.getAppByPackageName(packageName) != null
        if (exists) {
            applicationDao.setSelected(packageName, isSelected)
        }
        return exists
    }

    override suspend fun updateExclusion(packageName: String, isExcluded: Boolean): Boolean {
        val exists = applicationDao.getAppByPackageName(packageName) != null
        if (exists) {
            applicationDao.setExcluded(packageName, isExcluded)
        }
        return exists
    }

    override suspend fun search(query: String): List<AppInfo> =
        applicationDao.searchApps(query).first().map { it.toDomain() }

    override suspend fun getInstalledApps(): List<AppInfo> {
        val installed = packageManagerProvider.getInstalledApplications()
        val existing = applicationDao.getAllApps().first().associateBy { it.packageName }
        val entities = installed.map { it.toEntity(existing[it.packageName]) }
        applicationDao.insertApps(entities)
        return entities.map { it.toDomain() }
    }

    override fun observeSelectedApps(): Flow<List<AppInfo>> =
        applicationDao.getSelectedApps().map { apps -> apps.map { it.toDomain() } }

    private fun PackageManagerProvider.InstalledApp.toEntity(
        existing: ApplicationInfoEntity?
    ): ApplicationInfoEntity = ApplicationInfoEntity(
        packageName = packageName,
        label = label,
        versionName = versionName,
        versionCode = versionCode,
        firstInstallTime = firstInstallTime,
        lastUpdateTime = lastUpdateTime,
        isSystemApp = isSystem,
        isSelected = existing?.isSelected ?: false,
        isExcluded = existing?.isExcluded ?: false,
        lastChecked = existing?.lastChecked ?: 0L
    )
}