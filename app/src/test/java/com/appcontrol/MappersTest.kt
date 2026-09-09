package com.appcontrol

import com.appcontrol.core.database.entity.ActivityEventEntity
import com.appcontrol.core.database.entity.AppPolicyEntity
import com.appcontrol.core.database.entity.ApplicationInfoEntity
import com.appcontrol.core.database.entity.HistoryEventEntity
import com.appcontrol.core.database.entity.ProfileEntity
import com.appcontrol.data.repository.toDomain
import com.appcontrol.data.repository.toEntity
import com.appcontrol.domain.model.ActivityEvent
import com.appcontrol.domain.model.AppInfo
import com.appcontrol.domain.model.AppPolicy
import com.appcontrol.domain.model.AppStatus
import com.appcontrol.domain.model.EventType
import com.appcontrol.domain.model.HistoryEvent
import com.appcontrol.domain.model.HistoryEventType
import com.appcontrol.domain.model.Profile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MappersTest {

    @Test
    fun applicationInfoEntity_toDomain_mapsAllFields() {
        val entity = ApplicationInfoEntity(
            packageName = "com.android.chrome",
            label = "Chrome",
            versionName = "1.2.3",
            versionCode = 12,
            firstInstallTime = 1000L,
            lastUpdateTime = 2000L,
            isSystemApp = false,
            isSelected = true,
            isExcluded = false,
            lastChecked = 5000L
        )

        val domain = entity.toDomain()

        assertEquals("com.android.chrome", domain.packageName)
        assertEquals("Chrome", domain.label)
        assertEquals("1.2.3", domain.versionName)
        assertEquals(false, domain.isSystemApp)
        assertEquals(true, domain.isSelected)
        assertEquals(false, domain.isExcluded)
        assertEquals(5000L, domain.lastActivityTime)
        assertEquals(AppStatus.UNKNOWN, domain.status)
        assertNull(domain.icon)
    }

    @Test
    fun appInfo_toEntity_mapsRoundTripFields() {
        val app = AppInfo(
            packageName = "com.android.settings",
            label = "Settings",
            versionName = "5.0",
            isSystemApp = true,
            isSelected = true,
            isExcluded = true,
            lastActivityTime = 7000L,
            status = AppStatus.ACTIVE
        )

        val entity = app.toEntity()

        assertEquals("com.android.settings", entity.packageName)
        assertEquals("Settings", entity.label)
        assertEquals("5.0", entity.versionName)
        assertEquals(true, entity.isSystemApp)
        assertEquals(true, entity.isSelected)
        assertEquals(true, entity.isExcluded)
        assertEquals(7000L, entity.lastChecked)
    }

    @Test
    fun appInfo_roundTrip_preservesIdentityFields() {
        val app = AppInfo(
            packageName = "com.app",
            label = "App",
            versionName = "2.0",
            isSystemApp = false,
            isSelected = true,
            isExcluded = false,
            lastActivityTime = 42L
        )
        val restored = app.toEntity().toDomain()

        assertEquals(app.packageName, restored.packageName)
        assertEquals(app.label, restored.label)
        assertEquals(app.versionName, restored.versionName)
        assertEquals(app.isSystemApp, restored.isSystemApp)
        assertEquals(app.isSelected, restored.isSelected)
        assertEquals(app.isExcluded, restored.isExcluded)
        assertEquals(app.lastActivityTime, restored.lastActivityTime)
    }

    @Test
    fun appInfo_nullVersion_mapsToEmptyString() {
        val app = AppInfo(packageName = "com.app", label = "App", versionName = null)
        assertEquals("", app.toEntity().versionName)
    }

    @Test
    fun historyEvent_toEntity_toDomain_roundTrips() {
        val event = HistoryEvent(
            id = 9L,
            timestamp = 12345L,
            eventType = HistoryEventType.REAPPEARED,
            title = "App reappeared",
            description = "detail",
            packageName = "com.a",
            profileId = 3L
        )

        val restored = event.toEntity().toDomain()

        assertEquals(event, restored)
    }

    @Test
    fun historyEventEntity_storesTypeAsName() {
        val entity = HistoryEvent(
            timestamp = 1L,
            eventType = HistoryEventType.ERROR,
            title = "err"
        ).toEntity()

        assertEquals(HistoryEventEntity.EVENT_TYPE_ERROR, entity.eventType)
    }

    @Test
    fun historyEvent_nullOptionalFields_roundTrip() {
        val event = HistoryEvent(
            timestamp = 1L,
            eventType = HistoryEventType.CHECK,
            title = "check",
            description = ""
        )
        val restored = event.toEntity().toDomain()

        assertEquals(event, restored)
        assertNull(restored.packageName)
        assertNull(restored.profileId)
    }

    @Test
    fun activityEvent_toEntity_toDomain_roundTrips() {
        val event = ActivityEvent(
            id = 4L,
            packageName = "com.android.chrome",
            timestamp = 5500L,
            eventType = EventType.APP_ACTIVE,
            source = "MonitorAppActivityUseCase",
            profileId = 2L
        )

        val restored = event.toEntity().toDomain()

        assertEquals(event, restored)
    }

    @Test
    fun activityEventEntity_storesTypeAsName() {
        val entity = ActivityEvent(
            packageName = "com.a",
            timestamp = 1L,
            eventType = EventType.APP_REAPPEARED,
            source = "engine"
        ).toEntity()

        assertEquals(ActivityEventEntity.EVENT_TYPE_APP_REAPPEARED, entity.eventType)
    }

    @Test
    fun activityEvent_nullProfileId_roundTrip() {
        val event = ActivityEvent(
            packageName = "com.a",
            timestamp = 1L,
            eventType = EventType.CHECK_COMPLETED,
            source = "engine"
        )
        assertEquals(event, event.toEntity().toDomain())
    }

    @Test
    fun appPolicy_toEntity_toDomain_roundTrips() {
        val policy = AppPolicy(
            id = 11L,
            packageName = "com.android.chrome",
            enabled = true,
            monitorEnabled = true,
            notificationEnabled = false,
            retryEnabled = true
        )

        val restored = policy.toEntity().toDomain()

        assertEquals(policy, restored)
    }

    @Test
    fun appPolicy_defaults_roundTrip() {
        val policy = AppPolicy(packageName = "com.a")
        assertEquals(policy, policy.toEntity().toDomain())
    }

    @Test
    fun profile_toEntity_convertsTimeStringsToMinutes() {
        val profile = Profile(
            name = "Night",
            enabled = true,
            scheduleEnabled = true,
            startTime = "22:30",
            endTime = "07:15",
            monitoringInterval = 30 * 60_000L,
            notificationEnabled = false,
            appPackages = listOf("com.a", "com.b")
        )

        val entity = profile.toEntity()

        assertEquals(22 * 60 + 30, entity.startTime)
        assertEquals(7 * 60 + 15, entity.endTime)
        assertEquals(30, entity.monitoringInterval)
        assertEquals(false, entity.notificationEnabled)
    }

    @Test
    fun profile_nullTimes_mapToZeroMinutes() {
        val profile = Profile(
            name = "Always",
            scheduleEnabled = false,
            startTime = null,
            endTime = null
        )

        val entity = profile.toEntity()

        assertEquals(0, entity.startTime)
        assertEquals(0, entity.endTime)
    }

    @Test
    fun profileEntity_toDomain_convertsMinutesToTimeStrings() {
        val entity = ProfileEntity(
            id = 7L,
            name = "Night",
            enabled = true,
            scheduleEnabled = true,
            startTime = 22 * 60 + 30,
            endTime = 7 * 60 + 15,
            monitoringInterval = 45,
            notificationEnabled = false,
            createdAt = 999L
        )

        val domain = entity.toDomain(listOf("com.a", "com.b"))

        assertEquals("22:30", domain.startTime)
        assertEquals("07:15", domain.endTime)
        assertEquals(45 * 60_000L, domain.monitoringInterval)
        assertEquals(false, domain.notificationEnabled)
        assertEquals(listOf("com.a", "com.b"), domain.appPackages)
        assertEquals(7L, domain.id)
    }

    @Test
    fun profileEntity_zeroMinutes_mapToNull() {
        val entity = ProfileEntity(name = "Plain", startTime = 0, endTime = 0)
        val domain = entity.toDomain(emptyList())

        assertNull(domain.startTime)
        assertNull(domain.endTime)
    }

    @Test
    fun profile_roundTrip_minutesBounded() {
        val profile = Profile(
            name = "Night",
            scheduleEnabled = true,
            startTime = "22:00",
            endTime = "07:00",
            monitoringInterval = 30 * 60_000L
        )
        val restored = profile.toEntity().toDomain(profile.appPackages)

        assertEquals("22:00", restored.startTime)
        assertEquals("07:00", restored.endTime)
        assertEquals(30 * 60_000L, restored.monitoringInterval)
    }

    @Test
    fun profileEntity_monitoringIntervalForwardsFromMinutesToMillis() {
        val entity = ProfileEntity(monitoringInterval = 60, startTime = 0, endTime = 0)
        assertEquals(60 * 60_000L, entity.toDomain(emptyList()).monitoringInterval)
    }
}