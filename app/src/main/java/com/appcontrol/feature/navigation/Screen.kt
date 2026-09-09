package com.appcontrol.feature.navigation

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object Applications : Screen("applications")
    data object ApplicationDetail : Screen("application/{packageName}") {
        fun createRoute(packageName: String) = "application/$packageName"
    }
    data object Profiles : Screen("profiles")
    data object ProfileDetail : Screen("profile/{id}") {
        fun createRoute(id: Long) = "profile/$id"
    }
    data object Automation : Screen("automation")
    data object History : Screen("history")
    data object Settings : Screen("settings")
    data object Onboarding : Screen("onboarding")
    data object Permissions : Screen("permissions")
}