package com.appcontrol.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.appcontrol.core.database.entity.ProfileAppEntity
import com.appcontrol.core.database.entity.ProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {

    @Query("SELECT * FROM profile ORDER BY name ASC")
    fun getAllProfiles(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profile WHERE enabled = 1 ORDER BY name ASC")
    fun getEnabledProfiles(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profile WHERE id = :profileId")
    suspend fun getProfileById(profileId: Long): ProfileEntity?

    @Query("SELECT * FROM profile WHERE id = :profileId")
    fun observeProfileById(profileId: Long): Flow<ProfileEntity?>

    @Query("SELECT * FROM profile WHERE name = :name")
    suspend fun getProfileByName(name: String): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ProfileEntity): Long

    @Update
    suspend fun updateProfile(profile: ProfileEntity)

    @Query("UPDATE profile SET enabled = :enabled WHERE id = :profileId")
    suspend fun setEnabled(profileId: Long, enabled: Boolean)

    @Query("UPDATE profile SET schedule_enabled = :scheduleEnabled WHERE id = :profileId")
    suspend fun setScheduleEnabled(profileId: Long, scheduleEnabled: Boolean)

    @Delete
    suspend fun deleteProfile(profile: ProfileEntity)

    @Query("DELETE FROM profile WHERE id = :profileId")
    suspend fun deleteProfileById(profileId: Long)

    @Query("DELETE FROM profile")
    suspend fun deleteAllProfiles()

    @Query("SELECT COUNT(*) FROM profile")
    fun getProfileCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfileApp(profileApp: ProfileAppEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfileApps(profileApps: List<ProfileAppEntity>)

    @Query("SELECT * FROM profile_app WHERE profile_id = :profileId")
    fun getAppsForProfile(profileId: Long): Flow<List<ProfileAppEntity>>

    @Query("SELECT * FROM profile_app WHERE profile_id = :profileId")
    suspend fun getAppsForProfileList(profileId: Long): List<ProfileAppEntity>

    @Query("SELECT * FROM profile_app WHERE profile_id = :profileId AND package_name = :packageName")
    suspend fun getProfileApp(profileId: Long, packageName: String): ProfileAppEntity?

    @Query("UPDATE profile_app SET enabled = :enabled WHERE profile_id = :profileId AND package_name = :packageName")
    suspend fun setProfileAppEnabled(profileId: Long, packageName: String, enabled: Boolean)

    @Delete
    suspend fun deleteProfileApp(profileApp: ProfileAppEntity)

    @Query("DELETE FROM profile_app WHERE profile_id = :profileId")
    suspend fun deleteAppsForProfile(profileId: Long)

    @Query("DELETE FROM profile_app WHERE profile_id = :profileId AND package_name = :packageName")
    suspend fun deleteProfileAppByPackage(profileId: Long, packageName: String)

    @Transaction
    suspend fun deleteProfileWithApps(profileId: Long) {
        deleteAppsForProfile(profileId)
        deleteProfileById(profileId)
    }
}
