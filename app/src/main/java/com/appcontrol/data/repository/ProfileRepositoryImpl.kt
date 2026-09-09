package com.appcontrol.data.repository

import com.appcontrol.core.database.dao.ProfileDao
import com.appcontrol.core.database.entity.ProfileAppEntity
import com.appcontrol.core.datastore.PreferencesManager
import com.appcontrol.domain.model.Profile
import com.appcontrol.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class ProfileRepositoryImpl(
    private val profileDao: ProfileDao,
    private val preferencesManager: PreferencesManager
) : ProfileRepository {

    override suspend fun getProfile(id: Long): Profile? {
        val entity = profileDao.getProfileById(id) ?: return null
        return entity.toDomain(getAppPackages(id))
    }

    override suspend fun getAllProfiles(): List<Profile> =
        profileDao.getAllProfiles().first().map { entity ->
            entity.toDomain(getAppPackages(entity.id))
        }

    override suspend fun saveProfile(profile: Profile): Long {
        val id = profileDao.insertProfile(profile.toEntity())
        replaceProfileApps(id, profile.appPackages)
        return id
    }

    override suspend fun updateProfile(profile: Profile): Boolean {
        if (profile.id <= 0L || profileDao.getProfileById(profile.id) == null) return false
        profileDao.updateProfile(profile.toEntity())
        replaceProfileApps(profile.id, profile.appPackages)
        return true
    }

    override suspend fun deleteProfile(id: Long): Boolean {
        val existed = profileDao.getProfileById(id) != null
        if (existed) {
            profileDao.deleteProfileWithApps(id)
        }
        return existed
    }

    override suspend fun getActiveProfile(): Profile? {
        val id = preferencesManager.activeProfileId.first()
        return if (id <= 0L) null else getProfile(id)
    }

    override suspend fun setActiveProfile(id: Long?): Boolean {
        if (id == null) {
            preferencesManager.setActiveProfileId(-1L)
            return true
        }
        val existed = profileDao.getProfileById(id) != null
        if (existed) {
            preferencesManager.setActiveProfileId(id)
        }
        return existed
    }

    override fun observeProfiles(): Flow<List<Profile>> =
        profileDao.getAllProfiles().map { profiles ->
            profiles.map { entity -> entity.toDomain(getAppPackages(entity.id)) }
        }

    private suspend fun getAppPackages(profileId: Long): List<String> =
        profileDao.getAppsForProfileList(profileId).map { it.packageName }

    private suspend fun replaceProfileApps(profileId: Long, packages: List<String>) {
        profileDao.deleteAppsForProfile(profileId)
        if (packages.isNotEmpty()) {
            profileDao.insertProfileApps(
                packages.map { packageName ->
                    ProfileAppEntity(
                        profileId = profileId,
                        packageName = packageName
                    )
                }
            )
        }
    }
}