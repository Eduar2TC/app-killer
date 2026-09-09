package com.appcontrol.feature.permissions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.appcontrol.core.permissions.PermissionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PermissionsUiState(
    val usageStatsPermission: Boolean = false,
    val notificationPermission: Boolean = false,
    val exactAlarmPermission: Boolean = false,
    val allGranted: Boolean = false,
    val isRefreshing: Boolean = false
)

class PermissionsViewModel(
    private val permissionManager: PermissionManager
) : ViewModel() {

    private val _state = MutableStateFlow(PermissionsUiState())
    val state: StateFlow<PermissionsUiState> = _state.asStateFlow()

    init {
        refresh()
        viewModelScope.launch {
            permissionManager.state.collect { permission ->
                _state.value = permission.toUiState()
            }
        }
    }

    fun refresh() {
        _state.update { it.copy(isRefreshing = true) }
        permissionManager.refreshState()
        _state.update { it.copy(isRefreshing = false) }
    }

    fun openUsageStatsSettings() = permissionManager.openUsageStatsSettings()

    fun openNotificationSettings() = permissionManager.openNotificationSettings()

    fun openExactAlarmSettings() = permissionManager.openExactAlarmSettings()

    private fun com.appcontrol.core.permissions.PermissionState.toUiState(): PermissionsUiState {
        return PermissionsUiState(
            usageStatsPermission = usageStatsGranted,
            notificationPermission = notificationGranted,
            exactAlarmPermission = exactAlarmGranted,
            allGranted = usageStatsGranted && notificationGranted && exactAlarmGranted,
            isRefreshing = false
        )
    }

    class Factory(
        private val permissionManager: PermissionManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PermissionsViewModel::class.java)) {
                return PermissionsViewModel(permissionManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}