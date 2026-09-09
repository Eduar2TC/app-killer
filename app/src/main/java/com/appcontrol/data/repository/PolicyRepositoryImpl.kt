package com.appcontrol.data.repository

import com.appcontrol.core.database.dao.AppPolicyDao
import com.appcontrol.domain.model.AppPolicy
import com.appcontrol.domain.repository.PolicyRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class PolicyRepositoryImpl(private val appPolicyDao: AppPolicyDao) : PolicyRepository {

    override suspend fun getPolicy(packageName: String): AppPolicy? =
        appPolicyDao.getPolicyByPackageName(packageName)?.toDomain()

    override suspend fun getAllPolicies(): List<AppPolicy> =
        appPolicyDao.getAllPolicies().first().map { it.toDomain() }

    override suspend fun savePolicy(policy: AppPolicy): Long {
        appPolicyDao.insertPolicy(policy.toEntity())
        return appPolicyDao.getPolicyByPackageName(policy.packageName)?.id ?: 0L
    }

    override suspend fun deletePolicy(packageName: String): Boolean {
        val existed = appPolicyDao.getPolicyByPackageName(packageName) != null
        appPolicyDao.deletePolicyByPackageName(packageName)
        return existed
    }

    override suspend fun setMonitorEnabled(packageName: String, enabled: Boolean): Boolean {
        val existed = appPolicyDao.getPolicyByPackageName(packageName) != null
        appPolicyDao.setMonitorEnabled(packageName, enabled)
        return existed
    }

    override suspend fun setNotificationEnabled(packageName: String, enabled: Boolean): Boolean {
        val existed = appPolicyDao.getPolicyByPackageName(packageName) != null
        appPolicyDao.setNotificationEnabled(packageName, enabled)
        return existed
    }

    override suspend fun setRetryEnabled(packageName: String, enabled: Boolean): Boolean {
        val existed = appPolicyDao.getPolicyByPackageName(packageName) != null
        appPolicyDao.setRetryEnabled(packageName, enabled)
        return existed
    }

    override fun observePolicies(): Flow<List<AppPolicy>> =
        appPolicyDao.getAllPolicies().map { policies -> policies.map { it.toDomain() } }
}