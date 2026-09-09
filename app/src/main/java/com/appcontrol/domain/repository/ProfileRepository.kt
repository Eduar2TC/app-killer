package com.appcontrol.domain.repository

import com.appcontrol.domain.model.Profile
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    suspend fun getProfile(id: Long): Profile?
    suspend fun getAllProfiles(): List<Profile>
    suspend fun saveProfile(profile: Profile): Long
    suspend fun updateProfile(profile: Profile): Boolean
    suspend fun deleteProfile(id: Long): Boolean
    suspend fun getActiveProfile(): Profile?
    suspend fun setActiveProfile(id: Long?): Boolean
    fun observeProfiles(): Flow<List<Profile>>
}