package com.appcontrol.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.appcontrol.core.datastore.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val currentStep: Int = 0,
    val isCompleted: Boolean = false,
    val totalSteps: Int = 5
)

class OnboardingViewModel(
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesManager.isOnboardingCompleted.collect { completed ->
                if (completed) {
                    _state.update { it.copy(isCompleted = true, currentStep = it.totalSteps - 1) }
                }
            }
        }
    }

    fun next() {
        val current = _state.value
        if (current.currentStep >= current.totalSteps - 1) {
            complete()
        } else {
            _state.update { it.copy(currentStep = it.currentStep + 1) }
        }
    }

    fun previous() {
        _state.update { it.copy(currentStep = (it.currentStep - 1).coerceAtLeast(0)) }
    }

    fun setStep(step: Int) {
        _state.update { it.copy(currentStep = step.coerceIn(0, it.totalSteps - 1)) }
    }

    fun complete() {
        viewModelScope.launch {
            preferencesManager.setOnboardingCompleted(true)
            _state.update { it.copy(isCompleted = true) }
        }
    }

    fun skip() {
        complete()
    }

    class Factory(
        private val preferencesManager: PreferencesManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(OnboardingViewModel::class.java)) {
                return OnboardingViewModel(preferencesManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}