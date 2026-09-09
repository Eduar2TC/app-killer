package com.appcontrol.domain.usecase

import com.appcontrol.domain.model.Profile
import com.appcontrol.domain.repository.ProfileRepository

class ManageProfileUseCase(
    private val profileRepository: ProfileRepository
) {
    suspend fun create(profile: Profile): Long = profileRepository.saveProfile(profile)

    suspend fun update(profile: Profile): Boolean = profileRepository.updateProfile(profile)

    suspend fun delete(id: Long): Boolean = profileRepository.deleteProfile(id)

    suspend fun get(id: Long): Profile? = profileRepository.getProfile(id)

    suspend fun getAll(): List<Profile> = profileRepository.getAllProfiles()

    suspend fun getActiveProfile(): Profile? = profileRepository.getActiveProfile()

    suspend fun setActiveProfile(id: Long?): Boolean = profileRepository.setActiveProfile(id)
}