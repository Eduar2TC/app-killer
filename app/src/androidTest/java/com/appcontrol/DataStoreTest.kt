package com.appcontrol

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.appcontrol.core.common.Constants
import com.appcontrol.core.datastore.PreferencesManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DataStoreTest {

    private lateinit var context: Context
    private lateinit var prefs: PreferencesManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        prefs = PreferencesManager(context)
    }

    @Test
    fun showSystemApps_defaultsTrue() = runBlocking {
        resetToDefaults()
        assertEquals(true, prefs.showSystemApps.first())
    }

    @Test
    fun showSystemApps_setAndReadBack() = runBlocking {
        prefs.setShowSystemApps(false)
        assertEquals(false, prefs.showSystemApps.first())
        prefs.setShowSystemApps(true)
        assertEquals(true, prefs.showSystemApps.first())
    }

    @Test
    fun confirmActions_defaultsTrue() = runBlocking {
        resetToDefaults()
        assertEquals(true, prefs.confirmActions.first())
    }

    @Test
    fun confirmActions_setAndReadBack() = runBlocking {
        prefs.setConfirmActions(false)
        assertEquals(false, prefs.confirmActions.first())
    }

    @Test
    fun vibration_defaultsTrue() = runBlocking {
        resetToDefaults()
        assertEquals(true, prefs.vibration.first())
    }

    @Test
    fun vibration_setAndReadBack() = runBlocking {
        prefs.setVibration(false)
        assertEquals(false, prefs.vibration.first())
    }

    @Test
    fun detectActivity_defaultsTrue() = runBlocking {
        resetToDefaults()
        assertEquals(true, prefs.detectActivity.first())
    }

    @Test
    fun detectActivity_setAndReadBack() = runBlocking {
        prefs.setDetectActivity(false)
        assertEquals(false, prefs.detectActivity.first())
    }

    @Test
    fun monitoringInterval_defaultMatchesConstant() = runBlocking {
        resetToDefaults()
        assertEquals(Constants.DEFAULT_MONITORING_INTERVAL_MINUTES, prefs.monitoringInterval.first())
    }

    @Test
    fun monitoringInterval_setAndReadBack() = runBlocking {
        prefs.setMonitoringInterval(5)
        assertEquals(5, prefs.monitoringInterval.first())
    }

    @Test
    fun notificationsEnabled_defaultsTrue() = runBlocking {
        resetToDefaults()
        assertEquals(true, prefs.notificationsEnabled.first())
    }

    @Test
    fun notificationsEnabled_setAndReadBack() = runBlocking {
        prefs.setNotificationsEnabled(false)
        assertEquals(false, prefs.notificationsEnabled.first())
    }

    @Test
    fun nightProtectionEnabled_defaultsFalse() = runBlocking {
        resetToDefaults()
        assertEquals(false, prefs.nightProtectionEnabled.first())
    }

    @Test
    fun nightProtectionEnabled_setAndReadBack() = runBlocking {
        prefs.setNightProtectionEnabled(true)
        assertEquals(true, prefs.nightProtectionEnabled.first())
    }

    @Test
    fun nightSchedule_defaultsFromConstants() = runBlocking {
        resetToDefaults()
        assertEquals(Constants.DEFAULT_NIGHT_START_HOUR, prefs.nightStartHour.first())
        assertEquals(Constants.DEFAULT_NIGHT_START_MINUTE, prefs.nightStartMinute.first())
        assertEquals(Constants.DEFAULT_NIGHT_END_HOUR, prefs.nightEndHour.first())
        assertEquals(Constants.DEFAULT_NIGHT_END_MINUTE, prefs.nightEndMinute.first())
    }

    @Test
    fun nightSchedule_setAndReadBack() = runBlocking {
        prefs.setNightStartHour(23)
        prefs.setNightStartMinute(30)
        prefs.setNightEndHour(6)
        prefs.setNightEndMinute(45)

        assertEquals(23, prefs.nightStartHour.first())
        assertEquals(30, prefs.nightStartMinute.first())
        assertEquals(6, prefs.nightEndHour.first())
        assertEquals(45, prefs.nightEndMinute.first())
    }

    @Test
    fun activeProfileId_defaultsMinusOne() = runBlocking {
        resetToDefaults()
        assertEquals(-1L, prefs.activeProfileId.first())
    }

    @Test
    fun activeProfileId_setAndReadBack() = runBlocking {
        prefs.setActiveProfileId(7L)
        assertEquals(7L, prefs.activeProfileId.first())
    }

    @Test
    fun historyRetentionDays_defaultMatchesConstant() = runBlocking {
        resetToDefaults()
        assertEquals(Constants.DEFAULT_HISTORY_RETENTION_DAYS, prefs.historyRetentionDays.first())
    }

    @Test
    fun historyRetentionDays_setAndReadBack() = runBlocking {
        prefs.setHistoryRetentionDays(7)
        assertEquals(7, prefs.historyRetentionDays.first())
    }

    @Test
    fun notificationCooldownMinutes_defaultMatchesConstant() = runBlocking {
        resetToDefaults()
        assertEquals(
            Constants.DEFAULT_NOTIFICATION_COOLDOWN_MINUTES,
            prefs.notificationCooldownMinutes.first()
        )
    }

    @Test
    fun notificationCooldownMinutes_setAndReadBack() = runBlocking {
        prefs.setNotificationCooldownMinutes(10)
        assertEquals(10, prefs.notificationCooldownMinutes.first())
    }

    @Test
    fun minimumEventIntervalMinutes_defaultMatchesConstant() = runBlocking {
        resetToDefaults()
        assertEquals(
            Constants.DEFAULT_MINIMUM_EVENT_INTERVAL_MINUTES,
            prefs.minimumEventIntervalMinutes.first()
        )
    }

    @Test
    fun minimumEventIntervalMinutes_setAndReadBack() = runBlocking {
        prefs.setMinimumEventIntervalMinutes(10)
        assertEquals(10, prefs.minimumEventIntervalMinutes.first())
    }

    @Test
    fun isOnboardingCompleted_defaultsFalse() = runBlocking {
        resetToDefaults()
        assertEquals(false, prefs.isOnboardingCompleted.first())
    }

    @Test
    fun isOnboardingCompleted_setAndReadBack() = runBlocking {
        prefs.setOnboardingCompleted(true)
        assertEquals(true, prefs.isOnboardingCompleted.first())
    }

    @Test
    fun valuesSurviveAcrossManagerInstances() = runBlocking {
        prefs.setNotificationsEnabled(true)
        prefs.setMonitoringInterval(30)

        val secondInstance = PreferencesManager(context)

        assertEquals(true, secondInstance.notificationsEnabled.first())
        assertEquals(30, secondInstance.monitoringInterval.first())
    }

    private suspend fun resetToDefaults() {
        prefs.setShowSystemApps(true)
        prefs.setConfirmActions(true)
        prefs.setVibration(true)
        prefs.setDetectActivity(true)
        prefs.setMonitoringInterval(Constants.DEFAULT_MONITORING_INTERVAL_MINUTES)
        prefs.setNotificationsEnabled(true)
        prefs.setNightProtectionEnabled(false)
        prefs.setNightStartHour(Constants.DEFAULT_NIGHT_START_HOUR)
        prefs.setNightStartMinute(Constants.DEFAULT_NIGHT_START_MINUTE)
        prefs.setNightEndHour(Constants.DEFAULT_NIGHT_END_HOUR)
        prefs.setNightEndMinute(Constants.DEFAULT_NIGHT_END_MINUTE)
        prefs.setActiveProfileId(-1L)
        prefs.setHistoryRetentionDays(Constants.DEFAULT_HISTORY_RETENTION_DAYS)
        prefs.setNotificationCooldownMinutes(Constants.DEFAULT_NOTIFICATION_COOLDOWN_MINUTES)
        prefs.setMinimumEventIntervalMinutes(Constants.DEFAULT_MINIMUM_EVENT_INTERVAL_MINUTES)
        prefs.setOnboardingCompleted(false)
    }
}