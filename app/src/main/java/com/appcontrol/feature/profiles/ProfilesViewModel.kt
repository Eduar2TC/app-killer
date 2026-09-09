package com.appcontrol.feature.profiles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.appcontrol.core.common.Constants
import com.appcontrol.domain.model.Profile
import com.appcontrol.domain.repository.ProfileRepository
import com.appcontrol.domain.usecase.ManageProfileUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfilesUiState(
    val profiles: List<Profile> = emptyList(),
    val activeProfileId: Long? = null,
    val isLoading: Boolean = true
)

class ProfilesViewModel(
    private val manageProfile: ManageProfileUseCase,
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ProfilesUiState())
    val state: StateFlow<ProfilesUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            profileRepository.observeProfiles().collect { profiles ->
                _state.update { it.copy(profiles = profiles) }
            }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val profiles = manageProfile.getAll()
            val activeId = manageProfile.getActiveProfile()?.id
            _state.update {
                it.copy(
                    profiles = profiles,
                    activeProfileId = activeId,
                    isLoading = false
                )
            }
        }
    }

    fun create(name: String) {
        viewModelScope.launch {
            manageProfile.create(
                Profile(name = name.ifBlank { Constants.DEFAULT_PROFILE_NAME })
            )
            refresh()
        }
    }

    fun update(profile: Profile) {
        viewModelScope.launch {
            manageProfile.update(profile)
            refresh()
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            if (_state.value.activeProfileId == id) {
                manageProfile.setActiveProfile(null)
            }
            manageProfile.delete(id)
            refresh()
        }
    }

    fun setActive(id: Long) {
        viewModelScope.launch {
            val next = if (_state.value.activeProfileId == id) null else id
            manageProfile.setActiveProfile(next)
            refresh()
        }
    }

    class Factory(
        private val manageProfile: ManageProfileUseCase,
        private val profileRepository: ProfileRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ProfilesViewModel::class.java)) {
                return ProfilesViewModel(manageProfile, profileRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}