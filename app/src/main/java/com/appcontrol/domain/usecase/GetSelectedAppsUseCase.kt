package com.appcontrol.domain.usecase

import com.appcontrol.domain.model.AppInfo
import com.appcontrol.domain.repository.AppRepository

class GetSelectedAppsUseCase(
    private val appRepository: AppRepository
) {
    suspend operator fun invoke(): List<AppInfo> = appRepository.getSelectedApps()
}