package com.appcontrol.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.appcontrol.core.common.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "appcontrol_preferences")

class PreferencesManager(private val context: Context) {

    private object Keys {
        val SHOW_SYSTEM_APPS = booleanPreferencesKey("show_system_apps")
        val CONFIRM_ACTIONS = booleanPreferencesKey("confirm_actions")
        val VIBRATION = booleanPreferencesKey("vibration")
        val DETECT_ACTIVITY = booleanPreferencesKey("detect_activity")
        val MONITORING_INTERVAL = intPreferencesKey("monitoring_interval")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val NIGHT_PROTECTION_ENABLED = booleanPreferencesKey("night_protection_enabled")
        val NIGHT_START_HOUR = intPreferencesKey("night_start_hour")
        val NIGHT_START_MINUTE = intPreferencesKey("night_start_minute")
        val NIGHT_END_HOUR = intPreferencesKey("night_end_hour")
        val NIGHT_END_MINUTE = intPreferencesKey("night_end_minute")
        val ACTIVE_PROFILE_ID = longPreferencesKey("active_profile_id")
        val HISTORY_RETENTION_DAYS = intPreferencesKey("history_retention_days")
        val NOTIFICATION_COOLDOWN_MINUTES = intPreferencesKey("notification_cooldown_minutes")
        val MINIMUM_EVENT_INTERVAL_MINUTES = intPreferencesKey("minimum_event_interval_minutes")
        val SHIZUKU_ENABLED = booleanPreferencesKey("shizuku_enabled")
        val IS_ONBOARDING_COMPLETED = booleanPreferencesKey("is_onboarding_completed")
    }

    val showSystemApps: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.SHOW_SYSTEM_APPS] ?: true
    }

    val confirmActions: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.CONFIRM_ACTIONS] ?: true
    }

    val vibration: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.VIBRATION] ?: true
    }

    val detectActivity: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.DETECT_ACTIVITY] ?: true
    }

    val monitoringInterval: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.MONITORING_INTERVAL] ?: Constants.DEFAULT_MONITORING_INTERVAL_MINUTES
    }

    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.NOTIFICATIONS_ENABLED] ?: true
    }

    val nightProtectionEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.NIGHT_PROTECTION_ENABLED] ?: false
    }

    val nightStartHour: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.NIGHT_START_HOUR] ?: Constants.DEFAULT_NIGHT_START_HOUR
    }

    val nightStartMinute: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.NIGHT_START_MINUTE] ?: Constants.DEFAULT_NIGHT_START_MINUTE
    }

    val nightEndHour: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.NIGHT_END_HOUR] ?: Constants.DEFAULT_NIGHT_END_HOUR
    }

    val nightEndMinute: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.NIGHT_END_MINUTE] ?: Constants.DEFAULT_NIGHT_END_MINUTE
    }

    val activeProfileId: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[Keys.ACTIVE_PROFILE_ID] ?: -1L
    }

    val historyRetentionDays: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.HISTORY_RETENTION_DAYS] ?: Constants.DEFAULT_HISTORY_RETENTION_DAYS
    }

    val notificationCooldownMinutes: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.NOTIFICATION_COOLDOWN_MINUTES] ?: Constants.DEFAULT_NOTIFICATION_COOLDOWN_MINUTES
    }

    val minimumEventIntervalMinutes: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.MINIMUM_EVENT_INTERVAL_MINUTES] ?: Constants.DEFAULT_MINIMUM_EVENT_INTERVAL_MINUTES
    }

    val isOnboardingCompleted: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.IS_ONBOARDING_COMPLETED] ?: false
    }

    val shizukuEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.SHIZUKU_ENABLED] ?: false
    }

    suspend fun setShowSystemApps(value: Boolean) {
        context.dataStore.edit { it[Keys.SHOW_SYSTEM_APPS] = value }
    }

    suspend fun setConfirmActions(value: Boolean) {
        context.dataStore.edit { it[Keys.CONFIRM_ACTIONS] = value }
    }

    suspend fun setVibration(value: Boolean) {
        context.dataStore.edit { it[Keys.VIBRATION] = value }
    }

    suspend fun setDetectActivity(value: Boolean) {
        context.dataStore.edit { it[Keys.DETECT_ACTIVITY] = value }
    }

    suspend fun setMonitoringInterval(minutes: Int) {
        context.dataStore.edit { it[Keys.MONITORING_INTERVAL] = minutes }
    }

    suspend fun setNotificationsEnabled(value: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = value }
    }

    suspend fun setNightProtectionEnabled(value: Boolean) {
        context.dataStore.edit { it[Keys.NIGHT_PROTECTION_ENABLED] = value }
    }

    suspend fun setNightStartHour(hour: Int) {
        context.dataStore.edit { it[Keys.NIGHT_START_HOUR] = hour }
    }

    suspend fun setNightStartMinute(minute: Int) {
        context.dataStore.edit { it[Keys.NIGHT_START_MINUTE] = minute }
    }

    suspend fun setNightEndHour(hour: Int) {
        context.dataStore.edit { it[Keys.NIGHT_END_HOUR] = hour }
    }

    suspend fun setNightEndMinute(minute: Int) {
        context.dataStore.edit { it[Keys.NIGHT_END_MINUTE] = minute }
    }

    suspend fun setActiveProfileId(profileId: Long) {
        context.dataStore.edit { it[Keys.ACTIVE_PROFILE_ID] = profileId }
    }

    suspend fun setHistoryRetentionDays(days: Int) {
        context.dataStore.edit { it[Keys.HISTORY_RETENTION_DAYS] = days }
    }

    suspend fun setNotificationCooldownMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.NOTIFICATION_COOLDOWN_MINUTES] = minutes }
    }

    suspend fun setMinimumEventIntervalMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.MINIMUM_EVENT_INTERVAL_MINUTES] = minutes }
    }

    suspend fun setOnboardingCompleted(value: Boolean) {
        context.dataStore.edit { it[Keys.IS_ONBOARDING_COMPLETED] = value }
    }

    suspend fun setShizukuEnabled(value: Boolean) {
        context.dataStore.edit { it[Keys.SHIZUKU_ENABLED] = value }
    }
}
