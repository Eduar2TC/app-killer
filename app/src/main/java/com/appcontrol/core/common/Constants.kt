package com.appcontrol.core.common

object Constants {
    const val DATABASE_NAME = "appcontrol_database"
    const val DATABASE_VERSION = 1

    const val NOTIFICATION_CHANNEL_MONITORING = "app_monitoring_channel"
    const val NOTIFICATION_CHANNEL_GENERAL = "general_channel"
    const val NOTIFICATION_CHANNEL_MONITORING_NAME = "App Monitoring"
    const val NOTIFICATION_CHANNEL_GENERAL_NAME = "General"

    const val NOTIFICATION_ID_REAPPEARANCE_BASE = 1000
    const val NOTIFICATION_ID_SUMMARY = 1
    const val NOTIFICATION_ID_WIDGET_STOP = 2

    const val DEFAULT_MONITORING_INTERVAL_MINUTES = 15
    const val DEFAULT_NOTIFICATION_COOLDOWN_MINUTES = 30
    const val DEFAULT_MINIMUM_EVENT_INTERVAL_MINUTES = 5
    const val DEFAULT_HISTORY_RETENTION_DAYS = 30

    const val DEFAULT_NIGHT_START_HOUR = 22
    const val DEFAULT_NIGHT_START_MINUTE = 0
    const val DEFAULT_NIGHT_END_HOUR = 7
    const val DEFAULT_NIGHT_END_MINUTE = 0

    const val DEFAULT_PROFILE_NAME = "Default"

    const val SHIZUKU_REQUEST_CODE = 1001
    const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"

    const val REQUEST_CODE_REVIEW = 100
    const val REQUEST_CODE_PROCESS = 101
    const val REQUEST_CODE_SUMMARY = 102

    const val EXTRA_PACKAGE_NAME = "extra_package_name"
    const val EXTRA_PROFILE_ID = "extra_profile_id"
    const val EXTRA_EVENT_TYPE = "extra_event_type"

    const val MAX_HISTORY_EVENTS = 500
    const val MAX_ACTIVITY_EVENTS = 1000
}
