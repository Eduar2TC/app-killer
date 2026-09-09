package com.appcontrol.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.appcontrol.core.database.entity.ApplicationInfoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ApplicationDao {

    @Query("SELECT * FROM application_info ORDER BY label ASC")
    fun getAllApps(): Flow<List<ApplicationInfoEntity>>

    @Query("SELECT * FROM application_info WHERE is_selected = 1 ORDER BY label ASC")
    fun getSelectedApps(): Flow<List<ApplicationInfoEntity>>

    @Query("SELECT * FROM application_info WHERE is_excluded = 0 ORDER BY label ASC")
    fun getNonExcludedApps(): Flow<List<ApplicationInfoEntity>>

    @Query("SELECT * FROM application_info WHERE package_name = :packageName")
    suspend fun getAppByPackageName(packageName: String): ApplicationInfoEntity?

    @Query("SELECT * FROM application_info WHERE package_name = :packageName")
    fun observeAppByPackageName(packageName: String): Flow<ApplicationInfoEntity?>

    @Query("SELECT * FROM application_info WHERE is_system_app = :isSystem ORDER BY label ASC")
    fun getAppsBySystemType(isSystem: Boolean): Flow<List<ApplicationInfoEntity>>

    @Query("SELECT * FROM application_info WHERE label LIKE '%' || :query || '%' OR package_name LIKE '%' || :query || '%' ORDER BY label ASC")
    fun searchApps(query: String): Flow<List<ApplicationInfoEntity>>

    @Query("SELECT COUNT(*) FROM application_info")
    fun getAppCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM application_info WHERE is_selected = 1")
    fun getSelectedAppCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM application_info WHERE is_system_app = 1")
    fun getSystemAppCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApp(app: ApplicationInfoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApps(apps: List<ApplicationInfoEntity>)

    @Update
    suspend fun updateApp(app: ApplicationInfoEntity)

    @Query("UPDATE application_info SET is_selected = :isSelected WHERE package_name = :packageName")
    suspend fun setSelected(packageName: String, isSelected: Boolean)

    @Query("UPDATE application_info SET is_excluded = :isExcluded WHERE package_name = :packageName")
    suspend fun setExcluded(packageName: String, isExcluded: Boolean)

    @Query("UPDATE application_info SET last_checked = :timestamp WHERE package_name = :packageName")
    suspend fun updateLastChecked(packageName: String, timestamp: Long)

    @Delete
    suspend fun deleteApp(app: ApplicationInfoEntity)

    @Query("DELETE FROM application_info WHERE package_name = :packageName")
    suspend fun deleteAppByPackageName(packageName: String)

    @Query("DELETE FROM application_info")
    suspend fun deleteAllApps()

    @Query("SELECT * FROM application_info WHERE is_selected = 1")
    suspend fun getSelectedAppsList(): List<ApplicationInfoEntity>
}
