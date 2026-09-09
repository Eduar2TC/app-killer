package com.appcontrol.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.appcontrol.core.datastore.PreferencesManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface NavigationState {
    data object Loading : NavigationState
    data object Onboarding : NavigationState
    data object Dashboard : NavigationState
}

class MainActivityViewModel(
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    val navigationState: StateFlow<NavigationState> =
        preferencesManager.isOnboardingCompleted
            .map { completed ->
                if (completed) NavigationState.Dashboard else NavigationState.Onboarding
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = NavigationState.Loading
            )
}