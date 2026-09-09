package com.appcontrol.domain.usecase

import com.appcontrol.domain.repository.AppRepository

class ToggleAppExclusionUseCase(
    private val appRepository: AppRepository
) {
    suspend operator fun invoke(packageName: String): Boolean {
        val allApps = appRepository.getAllApps()
        val app = allApps.find { it.packageName == packageName } ?: return false
        return appRepository.updateExclusion(packageName, !app.isExcluded)
    }
}