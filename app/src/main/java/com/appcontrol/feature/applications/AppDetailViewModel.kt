package com.appcontrol.feature.applications

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.appcontrol.domain.model.AppInfo
import com.appcontrol.domain.model.AppPolicy
import com.appcontrol.domain.model.HistoryEvent
import com.appcontrol.domain.repository.AppRepository
import com.appcontrol.domain.repository.PolicyRepository
import com.appcontrol.domain.usecase.GetHistoryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AppDetailUiState(
    val appInfo: AppInfo? = null,
    val policy: AppPolicy? = null,
    val recentEvents: List<HistoryEvent> = emptyList(),
    val isLoading: Boolean = true
)

class AppDetailViewModel(
    private val packageName: String,
    private val appRepository: AppRepository,
    private val policyRepository: PolicyRepository,
    private val getHistory: GetHistoryUseCase,
    private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(AppDetailUiState())
    val state: StateFlow<AppDetailUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val app = appRepository.getAllApps().find { it.packageName == packageName }
            val policy = policyRepository.getPolicy(packageName)
            val events = getHistory.byPackage(packageName, limit = 20)
            _state.value = AppDetailUiState(
                appInfo = app,
                policy = policy,
                recentEvents = events,
                isLoading = false
            )
        }
    }

    fun toggleMonitoring() {
        viewModelScope.launch {
            val current = _state.value.policy ?: AppPolicy(packageName = packageName)
            policyRepository.setMonitorEnabled(packageName, !current.monitorEnabled)
            refresh()
        }
    }

    fun exclude() {
        viewModelScope.launch {
            val isExcluded = _state.value.appInfo?.isExcluded ?: false
            appRepository.updateExclusion(packageName, !isExcluded)
            refresh()
        }
    }

    fun openApp() {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun openAppInfo() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    class Factory(
        private val packageName: String,
        private val appRepository: AppRepository,
        private val policyRepository: PolicyRepository,
        private val getHistory: GetHistoryUseCase,
        private val context: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AppDetailViewModel::class.java)) {
                return AppDetailViewModel(
                    packageName,
                    appRepository,
                    policyRepository,
                    getHistory,
                    context
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}