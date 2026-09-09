package com.appcontrol.feature.automation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.appcontrol.core.datastore.PreferencesManager
import com.appcontrol.domain.model.NightSchedule
import com.appcontrol.domain.usecase.CheckNightScheduleUseCase
import com.appcontrol.domain.usecase.ScheduleMonitoringUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AutomationUiState(
    val nightProtectionEnabled: Boolean = false,
    val startHour: Int = 22,
    val startMinute: Int = 0,
    val endHour: Int = 7,
    val endMinute: Int = 0,
    val intervalMinutes: Int = 30,
    val isActive: Boolean = false,
    val isRefreshing: Boolean = false
)

class AutomationViewModel(
    private val preferencesManager: PreferencesManager,
    private val scheduleMonitoring: ScheduleMonitoringUseCase,
    private val checkNightSchedule: CheckNightScheduleUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(AutomationUiState())
    val state: StateFlow<AutomationUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesManager.nightProtectionEnabled.collect { value ->
                _state.update { it.copy(nightProtectionEnabled = value) }
                updateActiveState()
            }
        }
        viewModelScope.launch {
            preferencesManager.nightStartHour.collect { value ->
                _state.update { it.copy(startHour = value) }
                updateActiveState()
            }
        }
        viewModelScope.launch {
            preferencesManager.nightStartMinute.collect { value ->
                _state.update { it.copy(startMinute = value) }
                updateActiveState()
            }
        }
        viewModelScope.launch {
            preferencesManager.nightEndHour.collect { value ->
                _state.update { it.copy(endHour = value) }
                updateActiveState()
            }
        }
        viewModelScope.launch {
            preferencesManager.nightEndMinute.collect { value ->
                _state.update { it.copy(endMinute = value) }
                updateActiveState()
            }
        }
        viewModelScope.launch {
            preferencesManager.monitoringInterval.collect { value ->
                _state.update { it.copy(intervalMinutes = value) }
            }
        }
    }

    fun setNightProtection(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setNightProtectionEnabled(enabled)
            if (enabled) {
                scheduleMonitoring.invoke()
            } else {
                scheduleMonitoring.cancel()
            }
            updateActiveState()
        }
    }

    fun setStartTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            preferencesManager.setNightStartHour(hour)
            preferencesManager.setNightStartMinute(minute)
            updateActiveState()
        }
    }

    fun setEndTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            preferencesManager.setNightEndHour(hour)
            preferencesManager.setNightEndMinute(minute)
            updateActiveState()
        }
    }

    fun setInterval(minutes: Int) {
        viewModelScope.launch {
            preferencesManager.setMonitoringInterval(minutes)
        }
    }

    fun applySchedule() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            scheduleMonitoring.invoke()
            updateActiveState()
            _state.update { it.copy(isRefreshing = false) }
        }
    }

    fun refresh() {
        _state.update { it.copy(isRefreshing = true) }
        updateActiveState()
        _state.update { it.copy(isRefreshing = false) }
    }

    private fun updateActiveState() {
        val current = _state.value
        val schedule = NightSchedule(
            enabled = current.nightProtectionEnabled,
            startHour = current.startHour,
            startMinute = current.startMinute,
            endHour = current.endHour,
            endMinute = current.endMinute,
            intervalMinutes = current.intervalMinutes
        )
        _state.update { it.copy(isActive = checkNightSchedule.isInNightPeriod(schedule)) }
    }

    class Factory(
        private val preferencesManager: PreferencesManager,
        private val scheduleMonitoring: ScheduleMonitoringUseCase,
        private val checkNightSchedule: CheckNightScheduleUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AutomationViewModel::class.java)) {
                return AutomationViewModel(
                    preferencesManager,
                    scheduleMonitoring,
                    checkNightSchedule
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}