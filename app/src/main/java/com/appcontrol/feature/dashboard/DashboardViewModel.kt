package com.appcontrol.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.appcontrol.core.datastore.PreferencesManager
import com.appcontrol.core.time.TimeUtils
import com.appcontrol.domain.model.AppInfo
import com.appcontrol.domain.model.AppStatus
import com.appcontrol.domain.model.NightSchedule
import com.appcontrol.domain.usecase.CheckNightScheduleUseCase
import com.appcontrol.domain.usecase.GetSelectedAppsUseCase
import com.appcontrol.domain.usecase.ManageProfileUseCase
import com.appcontrol.domain.usecase.MonitorAppActivityUseCase
import com.appcontrol.domain.usecase.ProcessSelectedAppsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardUiState(
    val isLoading: Boolean = false,
    val isProcessing: Boolean = false,
    val selectedAppsCount: Int = 0,
    val activeApps: List<AppInfo> = emptyList(),
    val recentlyRevivedApps: List<AppInfo> = emptyList(),
    val nightProtectionActive: Boolean = false,
    val nextCheckTime: String = "--:--",
    val profileName: String = "Default",
    val lastMessage: String? = null
)

class DashboardViewModel(
    private val getSelectedApps: GetSelectedAppsUseCase,
    private val monitorAppActivity: MonitorAppActivityUseCase,
    private val processSelectedApps: ProcessSelectedAppsUseCase,
    private val checkNightSchedule: CheckNightScheduleUseCase,
    private val manageProfile: ManageProfileUseCase,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _state = MutableStateFlow(DashboardUiState())
    val state: StateFlow<DashboardUiState> = _state.asStateFlow()

    private var nightEnabled = false
    private var nightStartHour = 22
    private var nightStartMinute = 0
    private var nightEndHour = 7
    private var nightEndMinute = 0
    private var monitoringIntervalMinutes = 15

    init {
        viewModelScope.launch {
            preferencesManager.nightProtectionEnabled.collect { nightEnabled = it; refresh() }
        }
        viewModelScope.launch {
            preferencesManager.nightStartHour.collect { nightStartHour = it; updateNightState() }
        }
        viewModelScope.launch {
            preferencesManager.nightStartMinute.collect { nightStartMinute = it; updateNightState() }
        }
        viewModelScope.launch {
            preferencesManager.nightEndHour.collect { nightEndHour = it; updateNightState() }
        }
        viewModelScope.launch {
            preferencesManager.nightEndMinute.collect { nightEndMinute = it; updateNightState() }
        }
        viewModelScope.launch {
            preferencesManager.monitoringInterval.collect { monitoringIntervalMinutes = it; updateNextCheckTime() }
        }
        refresh()
    }

    fun processApps() {
        viewModelScope.launch {
            _state.update { it.copy(isProcessing = true) }
            monitorAppActivity.invoke()
            val result = processSelectedApps.invoke()
            _state.update {
                it.copy(
                    isProcessing = false,
                    lastMessage = "Processed ${result.processed} of ${result.total} apps"
                )
            }
            load()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            load()
            _state.update { it.copy(isLoading = false) }
        }
    }

    private suspend fun load() {
        val selected = getSelectedApps.invoke()
        val monitored = monitorAppActivity.invoke()
        val activeProfileName = manageProfile.getActiveProfile()?.name ?: "Default"
        val now = System.currentTimeMillis()

        _state.update {
            it.copy(
                selectedAppsCount = selected.size,
                activeApps = monitored.filter { app ->
                    app.status == AppStatus.ACTIVE || app.status == AppStatus.REAPPEARED
                },
                recentlyRevivedApps = selected.filter { app ->
                    app.status == AppStatus.REAPPEARED ||
                        (app.lastActivityTime > 0 && now - app.lastActivityTime < 60_000L)
                },
                profileName = activeProfileName,
                nextCheckTime = TimeUtils.formatTime(now + monitoringIntervalMinutes * 60_000L)
            )
        }
        updateNightState()
    }

    private fun updateNightState() {
        val schedule = NightSchedule(
            enabled = nightEnabled,
            startHour = nightStartHour,
            startMinute = nightStartMinute,
            endHour = nightEndHour,
            endMinute = nightEndMinute,
            intervalMinutes = monitoringIntervalMinutes
        )
        _state.update { it.copy(nightProtectionActive = checkNightSchedule.isInNightPeriod(schedule)) }
    }

    private fun updateNextCheckTime() {
        val next = System.currentTimeMillis() + monitoringIntervalMinutes * 60_000L
        _state.update { it.copy(nextCheckTime = TimeUtils.formatTime(next)) }
    }

    class Factory(
        private val getSelectedApps: GetSelectedAppsUseCase,
        private val monitorAppActivity: MonitorAppActivityUseCase,
        private val processSelectedApps: ProcessSelectedAppsUseCase,
        private val checkNightSchedule: CheckNightScheduleUseCase,
        private val manageProfile: ManageProfileUseCase,
        private val preferencesManager: PreferencesManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
                return DashboardViewModel(
                    getSelectedApps,
                    monitorAppActivity,
                    processSelectedApps,
                    checkNightSchedule,
                    manageProfile,
                    preferencesManager
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}