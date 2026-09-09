package com.appcontrol.feature.applications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.appcontrol.domain.model.AppInfo
import com.appcontrol.domain.repository.AppRepository
import com.appcontrol.domain.usecase.GetInstalledAppsUseCase
import com.appcontrol.domain.usecase.ToggleAppExclusionUseCase
import com.appcontrol.domain.usecase.ToggleAppSelectionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppFilter {
    ALL, SELECTED, USER, SYSTEM
}

enum class AppSort {
    NAME, LAST_ACTIVITY
}

data class ApplicationsUiState(
    val allApps: List<AppInfo> = emptyList(),
    val displayedApps: List<AppInfo> = emptyList(),
    val searchQuery: String = "",
    val filter: AppFilter = AppFilter.ALL,
    val sortBy: AppSort = AppSort.NAME,
    val selectedCount: Int = 0,
    val isRefreshing: Boolean = false
)

class ApplicationsViewModel(
    private val getInstalledApps: GetInstalledAppsUseCase,
    private val toggleAppSelection: ToggleAppSelectionUseCase,
    private val toggleAppExclusion: ToggleAppExclusionUseCase,
    private val appRepository: AppRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ApplicationsUiState())
    val state: StateFlow<ApplicationsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            appRepository.observeSelectedApps().collect { selectedApps ->
                val selectedPackages = selectedApps.map { it.packageName }.toSet()
                _state.update { current ->
                    current.copy(
                        allApps = current.allApps.map { it.copy(isSelected = it.packageName in selectedPackages) },
                        selectedCount = selectedPackages.size
                    )
                }
                applyDisplay()
            }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            val apps = getInstalledApps.invoke()
            _state.update { it.copy(allApps = apps, selectedCount = apps.count { it.isSelected }) }
            applyDisplay()
            _state.update { it.copy(isRefreshing = false) }
        }
    }

    fun search(query: String) {
        _state.update { it.copy(searchQuery = query) }
        applyDisplay()
    }

    fun setFilter(filter: AppFilter) {
        _state.update { it.copy(filter = filter) }
        applyDisplay()
    }

    fun setSort(sortBy: AppSort) {
        _state.update { it.copy(sortBy = sortBy) }
        applyDisplay()
    }

    fun toggleSelection(packageName: String) {
        viewModelScope.launch {
            toggleAppSelection.invoke(packageName)
            refresh()
        }
    }

    fun toggleExclusion(packageName: String) {
        viewModelScope.launch {
            toggleAppExclusion.invoke(packageName)
            refresh()
        }
    }

    fun selectAll() {
        viewModelScope.launch {
            _state.value.displayedApps
                .filter { !it.isSelected }
                .forEach { appRepository.updateSelection(it.packageName, true) }
            refresh()
        }
    }

    fun deselectAll() {
        viewModelScope.launch {
            _state.value.displayedApps
                .filter { it.isSelected }
                .forEach { appRepository.updateSelection(it.packageName, false) }
            refresh()
        }
    }

    private fun applyDisplay() {
        val current = _state.value
        val query = current.searchQuery.trim().lowercase()
        var result = current.allApps.filter { app ->
            query.isEmpty() ||
                app.label.lowercase().contains(query) ||
                app.packageName.lowercase().contains(query)
        }
        result = when (current.filter) {
            AppFilter.ALL -> result
            AppFilter.SELECTED -> result.filter { it.isSelected }
            AppFilter.USER -> result.filter { !it.isSystemApp }
            AppFilter.SYSTEM -> result.filter { it.isSystemApp }
        }
        result = when (current.sortBy) {
            AppSort.NAME -> result.sortedBy { it.label.lowercase() }
            AppSort.LAST_ACTIVITY -> result.sortedByDescending { it.lastActivityTime }
        }
        _state.update { it.copy(displayedApps = result) }
    }

    class Factory(
        private val getInstalledApps: GetInstalledAppsUseCase,
        private val toggleAppSelection: ToggleAppSelectionUseCase,
        private val toggleAppExclusion: ToggleAppExclusionUseCase,
        private val appRepository: AppRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ApplicationsViewModel::class.java)) {
                return ApplicationsViewModel(
                    getInstalledApps,
                    toggleAppSelection,
                    toggleAppExclusion,
                    appRepository
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}