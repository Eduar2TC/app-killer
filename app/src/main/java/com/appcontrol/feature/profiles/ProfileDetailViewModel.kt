package com.appcontrol.feature.profiles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.appcontrol.domain.model.AppInfo
import com.appcontrol.domain.model.Profile
import com.appcontrol.domain.usecase.GetInstalledAppsUseCase
import com.appcontrol.domain.usecase.ManageProfileUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileDetailUiState(
    val profile: Profile = Profile(name = ""),
    val profileApps: List<AppInfo> = emptyList(),
    val availableApps: List<AppInfo> = emptyList(),
    val isLoading: Boolean = true
)

class ProfileDetailViewModel(
    private val profileId: Long,
    private val manageProfile: ManageProfileUseCase,
    private val getInstalledApps: GetInstalledAppsUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileDetailUiState())
    val state: StateFlow<ProfileDetailUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            var profile = manageProfile.get(profileId)
            if (profile == null) {
                val savedId = manageProfile.create(Profile(name = "New Profile"))
                profile = Profile(id = savedId, name = "New Profile")
            }
            val installed = getInstalledApps.invoke()
            val packageSet = profile.appPackages.toHashSet()
            _state.value = ProfileDetailUiState(
                profile = profile,
                profileApps = installed.filter { it.packageName in packageSet },
                availableApps = installed.filter { it.packageName !in packageSet },
                isLoading = false
            )
        }
    }

    fun updateName(name: String) {
        viewModelScope.launch {
            val updated = _state.value.profile.copy(name = name)
            manageProfile.update(updated)
            refresh()
        }
    }

    fun toggleEnabled() {
        viewModelScope.launch {
            val updated = _state.value.profile.copy(enabled = !_state.value.profile.enabled)
            manageProfile.update(updated)
            refresh()
        }
    }

    fun toggleSchedule() {
        viewModelScope.launch {
            val current = _state.value.profile
            val updated = current.copy(scheduleEnabled = !current.scheduleEnabled)
            manageProfile.update(updated)
            refresh()
        }
    }

    fun updateSchedule(startTime: String, endTime: String) {
        viewModelScope.launch {
            val current = _state.value.profile
            val updated = current.copy(
                startTime = startTime,
                endTime = endTime,
                scheduleEnabled = true
            )
            manageProfile.update(updated)
            refresh()
        }
    }

    fun updateInterval(minutes: Int) {
        viewModelScope.launch {
            val current = _state.value.profile
            val updated = current.copy(monitoringInterval = minutes * 60_000L)
            manageProfile.update(updated)
            refresh()
        }
    }

    fun toggleNotification() {
        viewModelScope.launch {
            val current = _state.value.profile
            val updated = current.copy(notificationEnabled = !current.notificationEnabled)
            manageProfile.update(updated)
            refresh()
        }
    }

    fun addApp(packageName: String) {
        viewModelScope.launch {
            val current = _state.value.profile
            val updated = current.copy(appPackages = current.appPackages + packageName)
            manageProfile.update(updated)
            refresh()
        }
    }

    fun removeApp(packageName: String) {
        viewModelScope.launch {
            val current = _state.value.profile
            val updated = current.copy(appPackages = current.appPackages - packageName)
            manageProfile.update(updated)
            refresh()
        }
    }

    class Factory(
        private val profileId: Long,
        private val manageProfile: ManageProfileUseCase,
        private val getInstalledApps: GetInstalledAppsUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ProfileDetailViewModel::class.java)) {
                return ProfileDetailViewModel(profileId, manageProfile, getInstalledApps) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}