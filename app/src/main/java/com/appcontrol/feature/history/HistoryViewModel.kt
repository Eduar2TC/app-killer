package com.appcontrol.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.appcontrol.core.time.TimeUtils
import com.appcontrol.domain.model.HistoryEvent
import com.appcontrol.domain.usecase.CleanupHistoryUseCase
import com.appcontrol.domain.usecase.GetHistoryUseCase
import com.appcontrol.domain.usecase.GetStatisticsUseCase
import com.appcontrol.domain.usecase.StatisticsResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class HistoryTimeFilter(val label: String) {
    TODAY("Today"),
    SEVEN_DAYS("7 days"),
    THIRTY_DAYS("30 days"),
    ALL("All")
}

data class HistoryUiState(
    val events: List<HistoryEvent> = emptyList(),
    val filter: HistoryTimeFilter = HistoryTimeFilter.ALL,
    val stats: StatisticsResult? = null,
    val isLoading: Boolean = false
)

class HistoryViewModel(
    private val getHistory: GetHistoryUseCase,
    private val getStatistics: GetStatisticsUseCase,
    private val cleanupHistory: CleanupHistoryUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(HistoryUiState())
    val state: StateFlow<HistoryUiState> = _state.asStateFlow()

    private var allEvents: List<HistoryEvent> = emptyList()

    init {
        viewModelScope.launch {
            getHistory.observe().collect { events ->
                allEvents = events
                applyFilter()
            }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val stats = getStatistics.invoke()
            _state.update { it.copy(stats = stats, isLoading = false) }
        }
    }

    fun setFilter(filter: HistoryTimeFilter) {
        _state.update { it.copy(filter = filter) }
        applyFilter()
    }

    fun cleanup() {
        viewModelScope.launch {
            cleanupHistory.invoke(retentionDays = 0)
            refresh()
        }
    }

    private fun applyFilter() {
        val cutoff: Long? = when (val filter = _state.value.filter) {
            HistoryTimeFilter.TODAY -> TimeUtils.getStartOfDay(System.currentTimeMillis())
            HistoryTimeFilter.SEVEN_DAYS -> TimeUtils.daysAgo(7)
            HistoryTimeFilter.THIRTY_DAYS -> TimeUtils.daysAgo(30)
            HistoryTimeFilter.ALL -> null
        }
        val filtered = if (cutoff == null) {
            allEvents
        } else {
            allEvents.filter { it.timestamp >= cutoff }
        }
        _state.update { it.copy(events = filtered) }
    }

    class Factory(
        private val getHistory: GetHistoryUseCase,
        private val getStatistics: GetStatisticsUseCase,
        private val cleanupHistory: CleanupHistoryUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
                return HistoryViewModel(getHistory, getStatistics, cleanupHistory) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}