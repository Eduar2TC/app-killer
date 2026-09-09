package com.appcontrol

import android.content.Context
import com.appcontrol.core.datastore.PreferencesManager
import com.appcontrol.core.notifications.NotificationHelper
import com.appcontrol.core.permissions.PermissionManager
import com.appcontrol.data.system.ProcessStopper
import com.appcontrol.domain.model.ActivityEvent
import com.appcontrol.domain.model.AppInfo
import com.appcontrol.domain.model.AppPolicy
import com.appcontrol.domain.model.EventType
import com.appcontrol.domain.model.HistoryEvent
import com.appcontrol.domain.model.HistoryEventType
import com.appcontrol.domain.model.Profile
import com.appcontrol.domain.model.StopResult
import com.appcontrol.domain.repository.ActivityEventRepository
import com.appcontrol.domain.repository.AppRepository
import com.appcontrol.domain.repository.HistoryRepository
import com.appcontrol.domain.repository.PolicyRepository
import com.appcontrol.domain.repository.ProfileRepository
import com.appcontrol.feature.navigation.AppDependencies
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf

class UiFakeAppRepository : AppRepository {

    val apps = mutableListOf<AppInfo>()
    private val selectedFlow = MutableStateFlow<List<AppInfo>>(emptyList())

    fun seed(apps: List<AppInfo>) {
        this.apps.clear()
        this.apps.addAll(apps)
        selectedFlow.value = this.apps.filter { it.isSelected && !it.isExcluded }
    }

    private fun refreshFlow() {
        selectedFlow.value = apps.filter { it.isSelected && !it.isExcluded }
    }

    override suspend fun getAllApps(): List<AppInfo> = apps.toList()

    override suspend fun getSelectedApps(): List<AppInfo> =
        apps.filter { it.isSelected && !it.isExcluded }

    override suspend fun updateSelection(packageName: String, isSelected: Boolean): Boolean {
        val index = apps.indexOfFirst { it.packageName == packageName }
        if (index < 0) return false
        apps[index] = apps[index].copy(isSelected = isSelected)
        refreshFlow()
        return true
    }

    override suspend fun updateExclusion(packageName: String, isExcluded: Boolean): Boolean {
        val index = apps.indexOfFirst { it.packageName == packageName }
        if (index < 0) return false
        apps[index] = apps[index].copy(isExcluded = isExcluded)
        refreshFlow()
        return true
    }

    override suspend fun search(query: String): List<AppInfo> {
        val normalized = query.lowercase()
        return apps.filter {
            it.label.lowercase().contains(normalized) ||
                it.packageName.lowercase().contains(normalized)
        }
    }

    override suspend fun getInstalledApps(): List<AppInfo> = apps.toList()

    override fun observeSelectedApps(): Flow<List<AppInfo>> = selectedFlow
}

class UiFakeActivityEventRepository : ActivityEventRepository {

    val latestByPackage = mutableMapOf<String, ActivityEvent>()
    val events = mutableListOf<ActivityEvent>()

    override suspend fun insert(event: ActivityEvent): Long {
        events += event
        latestByPackage[event.packageName] = event
        return events.size.toLong()
    }

    override suspend fun getByPackage(packageName: String, limit: Int): List<ActivityEvent> =
        events.filter { it.packageName == packageName }
            .sortedByDescending { it.timestamp }
            .take(limit)

    override suspend fun getAll(limit: Int): List<ActivityEvent> =
        events.sortedByDescending { it.timestamp }.take(limit)

    override suspend fun getLatest(packageName: String): ActivityEvent? =
        latestByPackage[packageName]

    override suspend fun getLatestByType(packageName: String, eventType: EventType): ActivityEvent? =
        events.lastOrNull { it.packageName == packageName && it.eventType == eventType }

    override fun observeEvents(packageName: String): Flow<List<ActivityEvent>> =
        flowOf(events.filter { it.packageName == packageName })

    override suspend fun cleanup(beforeTimestamp: Long): Int {
        val before = events.size
        events.removeAll { it.timestamp < beforeTimestamp }
        return before - events.size
    }

    override suspend fun clearAll(): Int {
        val count = events.size
        events.clear()
        latestByPackage.clear()
        return count
    }
}

class UiFakeHistoryRepository : HistoryRepository {

    val events = mutableListOf<HistoryEvent>()

    override suspend fun insert(event: HistoryEvent): Long {
        val id = (events.maxOfOrNull { it.id } ?: 0L) + 1L
        events += event.copy(id = id)
        return id
    }

    override suspend fun getById(id: Long): HistoryEvent? =
        events.firstOrNull { it.id == id }

    override suspend fun getAll(limit: Int): List<HistoryEvent> =
        events.sortedByDescending { it.timestamp }.take(limit)

    override suspend fun getByType(eventType: HistoryEventType, limit: Int): List<HistoryEvent> =
        events.filter { it.eventType == eventType }
            .sortedByDescending { it.timestamp }
            .take(limit)

    override suspend fun getByPackage(packageName: String, limit: Int): List<HistoryEvent> =
        events.filter { it.packageName == packageName }
            .sortedByDescending { it.timestamp }
            .take(limit)

    override suspend fun getByProfile(profileId: Long, limit: Int): List<HistoryEvent> =
        events.filter { it.profileId == profileId }
            .sortedByDescending { it.timestamp }
            .take(limit)

    override fun observeEvents(): Flow<List<HistoryEvent>> =
        flowOf(events.sortedByDescending { it.timestamp })

    override suspend fun cleanup(beforeTimestamp: Long): Int {
        val before = events.size
        events.removeAll { it.timestamp < beforeTimestamp }
        return before - events.size
    }

    override suspend fun clearAll(): Int {
        val count = events.size
        events.clear()
        return count
    }
}

class UiFakeProfileRepository : ProfileRepository {

    val profiles = mutableMapOf<Long, Profile>()
    var activeProfileId: Long? = null
    private var nextId = 1L

    override suspend fun getProfile(id: Long): Profile? = profiles[id]

    override suspend fun getAllProfiles(): List<Profile> = profiles.values.toList()

    override suspend fun saveProfile(profile: Profile): Long {
        val id = if (profile.id > 0L) profile.id else nextId++
        profiles[id] = profile.copy(id = id)
        return id
    }

    override suspend fun updateProfile(profile: Profile): Boolean {
        if (!profiles.containsKey(profile.id)) return false
        profiles[profile.id] = profile
        return true
    }

    override suspend fun deleteProfile(id: Long): Boolean =
        profiles.remove(id) != null

    override suspend fun getActiveProfile(): Profile? =
        activeProfileId?.let { profiles[it] }

    override suspend fun setActiveProfile(id: Long?): Boolean {
        if (id == null) {
            activeProfileId = null
            return true
        }
        if (!profiles.containsKey(id)) return false
        activeProfileId = id
        return true
    }

    override fun observeProfiles(): Flow<List<Profile>> =
        flowOf(profiles.values.toList())
}

class UiFakePolicyRepository : PolicyRepository {

    val policies = mutableMapOf<String, AppPolicy>()

    override suspend fun getPolicy(packageName: String): AppPolicy? =
        policies[packageName]

    override suspend fun getAllPolicies(): List<AppPolicy> = policies.values.toList()

    override suspend fun savePolicy(policy: AppPolicy): Long {
        policies[policy.packageName] = policy
        return if (policy.id > 0L) policy.id else (policies.size.toLong())
    }

    override suspend fun deletePolicy(packageName: String): Boolean =
        policies.remove(packageName) != null

    override suspend fun setMonitorEnabled(packageName: String, enabled: Boolean): Boolean {
        val policy = policies[packageName] ?: return false
        policies[packageName] = policy.copy(monitorEnabled = enabled)
        return true
    }

    override suspend fun setNotificationEnabled(packageName: String, enabled: Boolean): Boolean {
        val policy = policies[packageName] ?: return false
        policies[packageName] = policy.copy(notificationEnabled = enabled)
        return true
    }

    override suspend fun setRetryEnabled(packageName: String, enabled: Boolean): Boolean {
        val policy = policies[packageName] ?: return false
        policies[packageName] = policy.copy(retryEnabled = enabled)
        return true
    }

    override fun observePolicies(): Flow<List<AppPolicy>> =
        flowOf(policies.values.toList())
}

class UiFakeProcessStopper : ProcessStopper {

    var result: StopResult = StopResult.STOPPED
    val stoppedPackages = mutableListOf<String>()

    override fun stopPackage(packageName: String): StopResult {
        stoppedPackages += packageName
        return result
    }
}

class UiTestDependencies(context: Context) {

    val appRepository = UiFakeAppRepository()
    val activityEventRepository = UiFakeActivityEventRepository()
    val historyRepository = UiFakeHistoryRepository()
    val profileRepository = UiFakeProfileRepository()
    val policyRepository = UiFakePolicyRepository()
    val processStopper = UiFakeProcessStopper()
    val preferencesManager = PreferencesManager(context)
    val permissionManager = PermissionManager(context)
    val notificationHelper = NotificationHelper(context)

    fun deps(): AppDependencies = AppDependencies(
        context = context,
        appRepository = appRepository,
        profileRepository = profileRepository,
        historyRepository = historyRepository,
        activityEventRepository = activityEventRepository,
        policyRepository = policyRepository,
        processStopper = processStopper,
        preferencesManager = preferencesManager,
        permissionManager = permissionManager,
        notificationHelper = notificationHelper
    )
}