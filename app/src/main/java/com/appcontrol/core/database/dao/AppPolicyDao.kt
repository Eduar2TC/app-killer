package com.appcontrol.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.appcontrol.core.database.entity.AppPolicyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppPolicyDao {

    @Query("SELECT * FROM app_policy ORDER BY created_at DESC")
    fun getAllPolicies(): Flow<List<AppPolicyEntity>>

    @Query("SELECT * FROM app_policy WHERE enabled = 1")
    fun getEnabledPolicies(): Flow<List<AppPolicyEntity>>

    @Query("SELECT * FROM app_policy WHERE package_name = :packageName")
    suspend fun getPolicyByPackageName(packageName: String): AppPolicyEntity?

    @Query("SELECT * FROM app_policy WHERE package_name = :packageName")
    fun observePolicyByPackageName(packageName: String): Flow<AppPolicyEntity?>

    @Query("SELECT * FROM app_policy WHERE monitor_enabled = 1 AND enabled = 1")
    fun getMonitoredPolicies(): Flow<List<AppPolicyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPolicy(policy: AppPolicyEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPolicies(policies: List<AppPolicyEntity>)

    @Update
    suspend fun updatePolicy(policy: AppPolicyEntity)

    @Query("UPDATE app_policy SET enabled = :enabled, updated_at = :timestamp WHERE package_name = :packageName")
    suspend fun setEnabled(packageName: String, enabled: Boolean, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE app_policy SET monitor_enabled = :monitorEnabled, updated_at = :timestamp WHERE package_name = :packageName")
    suspend fun setMonitorEnabled(packageName: String, monitorEnabled: Boolean, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE app_policy SET notification_enabled = :notificationEnabled, updated_at = :timestamp WHERE package_name = :packageName")
    suspend fun setNotificationEnabled(packageName: String, notificationEnabled: Boolean, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE app_policy SET retry_enabled = :retryEnabled, updated_at = :timestamp WHERE package_name = :packageName")
    suspend fun setRetryEnabled(packageName: String, retryEnabled: Boolean, timestamp: Long = System.currentTimeMillis())

    @Delete
    suspend fun deletePolicy(policy: AppPolicyEntity)

    @Query("DELETE FROM app_policy WHERE package_name = :packageName")
    suspend fun deletePolicyByPackageName(packageName: String)

    @Query("DELETE FROM app_policy")
    suspend fun deleteAllPolicies()

    @Query("SELECT COUNT(*) FROM app_policy WHERE enabled = 1")
    fun getEnabledPolicyCount(): Flow<Int>
}
