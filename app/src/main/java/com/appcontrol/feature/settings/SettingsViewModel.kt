package com.appcontrol.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.appcontrol.core.common.Constants
import com.appcontrol.core.datastore.PreferencesManager
import com.appcontrol.core.permissions.PermissionManager
import com.appcontrol.domain.usecase.CleanupHistoryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val showSystemApps: Boolean = true,
    val confirmActions: Boolean = true,
    val vibration: Boolean = true,
    val detectActivity: Boolean = true,
    val monitoringInterval: Int = Constants.DEFAULT_MONITORING_INTERVAL_MINUTES,
    val notificationsEnabled: Boolean = true,
    val historyRetentionDays: Int = Constants.DEFAULT_HISTORY_RETENTION_DAYS,
    val notificationCooldownMinutes: Int = Constants.DEFAULT_NOTIFICATION_COOLDOWN_MINUTES,
    val minimumEventIntervalMinutes: Int = Constants.DEFAULT_MINIMUM_EVENT_INTERVAL_MINUTES,
    val usageStatsPermission: Boolean = false,
    val notificationPermission: Boolean = false,
    val exactAlarmPermission: Boolean = false,
    val lastMessage: String? = null
)

class SettingsViewModel(
    private val preferencesManager: PreferencesManager,
    private val cleanupHistory: CleanupHistoryUseCase,
    private val permissionManager: PermissionManager
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { preferencesManager.showSystemApps.collect { it.let { v -> _state.update { s -> s.copy(showSystemApps = v) } } } }
        viewModelScope.launch { preferencesManager.confirmActions.collect { it.let { v -> _state.update { s -> s.copy(confirmActions = v) } } } }
        viewModelScope.launch { preferencesManager.vibration.collect { it.let { v -> _state.update { s -> s.copy(vibration = v) } } } }
        viewModelScope.launch { preferencesManager.detectActivity.collect { it.let { v -> _state.update { s -> s.copy(detectActivity = v) } } } }
        viewModelScope.launch { preferencesManager.monitoringInterval.collect { it.let { v -> _state.update { s -> s.copy(monitoringInterval = v) } } } }
        viewModelScope.launch { preferencesManager.notificationsEnabled.collect { it.let { v -> _state.update { s -> s.copy(notificationsEnabled = v) } } } }
        viewModelScope.launch { preferencesManager.historyRetentionDays.collect { it.let { v -> _state.update { s -> s.copy(historyRetentionDays = v) } } } }
        viewModelScope.launch { preferencesManager.notificationCooldownMinutes.collect { it.let { v -> _state.update { s -> s.copy(notificationCooldownMinutes = v) } } } }
        viewModelScope.launch { preferencesManager.minimumEventIntervalMinutes.collect { it.let { v -> _state.update { s -> s.copy(minimumEventIntervalMinutes = v) } } } }
        viewModelScope.launch {
            permissionManager.state.collect { permission ->
                _state.update {
                    it.copy(
                        usageStatsPermission = permission.usageStatsGranted,
                        notificationPermission = permission.notificationGranted,
                        exactAlarmPermission = permission.exactAlarmGranted
                    )
                }
            }
        }
        refreshPermissions()
    }

    fun refreshPermissions() {
        permissionManager.refreshState()
    }

    fun setShowSystemApps(value: Boolean) = viewModelScope.launch { preferencesManager.setShowSystemApps(value) }
    fun setConfirmActions(value: Boolean) = viewModelScope.launch { preferencesManager.setConfirmActions(value) }
    fun setVibration(value: Boolean) = viewModelScope.launch { preferencesManager.setVibration(value) }
    fun setDetectActivity(value: Boolean) = viewModelScope.launch { preferencesManager.setDetectActivity(value) }
    fun setMonitoringInterval(minutes: Int) = viewModelScope.launch { preferencesManager.setMonitoringInterval(minutes) }
    fun setNotificationsEnabled(value: Boolean) = viewModelScope.launch { preferencesManager.setNotificationsEnabled(value) }
    fun setHistoryRetentionDays(days: Int) = viewModelScope.launch { preferencesManager.setHistoryRetentionDays(days) }
    fun setNotificationCooldownMinutes(minutes: Int) = viewModelScope.launch { preferencesManager.setNotificationCooldownMinutes(minutes) }
    fun setMinimumEventIntervalMinutes(minutes: Int) = viewModelScope.launch { preferencesManager.setMinimumEventIntervalMinutes(minutes) }

    fun deleteHistory() {
        viewModelScope.launch {
            val result = cleanupHistory.invoke(retentionDays = 0)
            _state.update {
                it.copy(
                    lastMessage = "Deleted ${result.historyEventsRemoved} history and " +
                        "${result.activityEventsRemoved} activity events"
                )
            }
        }
    }

    fun exportConfig() {
        viewModelScope.launch {
            _state.update { it.copy(lastMessage = "Export is not implemented yet") }
        }
    }

    fun importConfig() {
        viewModelScope.launch {
            _state.update { it.copy(lastMessage = "Import is not implemented yet") }
        }
    }

    fun clearMessage() {
        _state.update { it.copy(lastMessage = null) }
    }

    class Factory(
        private val preferencesManager: PreferencesManager,
        private val cleanupHistory: CleanupHistoryUseCase,
        private val permissionManager: PermissionManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                return SettingsViewModel(
                    preferencesManager,
                    cleanupHistory,
                    permissionManager
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}