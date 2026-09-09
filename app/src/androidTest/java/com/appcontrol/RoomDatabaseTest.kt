package com.appcontrol

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.appcontrol.core.database.AppDatabase
import com.appcontrol.core.database.dao.ActivityEventDao
import com.appcontrol.core.database.dao.AppPolicyDao
import com.appcontrol.core.database.dao.ApplicationDao
import com.appcontrol.core.database.dao.HistoryDao
import com.appcontrol.core.database.dao.ProfileDao
import com.appcontrol.core.database.entity.ActivityEventEntity
import com.appcontrol.core.database.entity.AppPolicyEntity
import com.appcontrol.core.database.entity.ApplicationInfoEntity
import com.appcontrol.core.database.entity.HistoryEventEntity
import com.appcontrol.core.database.entity.ProfileAppEntity
import com.appcontrol.core.database.entity.ProfileEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomDatabaseTest {

    private lateinit var db: AppDatabase
    private lateinit var applicationDao: ApplicationDao
    private lateinit var appPolicyDao: AppPolicyDao
    private lateinit var profileDao: ProfileDao
    private lateinit var historyDao: HistoryDao
    private lateinit var activityEventDao: ActivityEventDao

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        applicationDao = db.applicationDao()
        appPolicyDao = db.appPolicyDao()
        profileDao = db.profileDao()
        historyDao = db.historyDao()
        activityEventDao = db.activityEventDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun sampleApp(packageName: String, label: String) = ApplicationInfoEntity(
        packageName = packageName,
        label = label,
        versionName = "1.0",
        versionCode = 1L,
        firstInstallTime = 1000L,
        lastUpdateTime = 2000L,
        isSystemApp = false,
        isSelected = false,
        isExcluded = false,
        lastChecked = 0L
    )

    // --- application_info ---

    @Test
    fun applicationDao_insertAndQueryAll_returnsApps() = runBlocking {
        applicationDao.insertApp(sampleApp("com.a", "Alpha"))
        applicationDao.insertApp(sampleApp("com.b", "Beta"))

        val all = applicationDao.getAllApps().first()

        assertEquals(2, all.size)
        assertEquals(listOf("Alpha", "Beta"), all.map { it.label })
    }

    @Test
    fun applicationDao_insertReplacesSamePackage() = runBlocking {
        applicationDao.insertApp(sampleApp("com.a", "Alpha"))
        applicationDao.insertApp(sampleApp("com.a", "Alpha Updated"))

        val all = applicationDao.getAllApps().first()

        assertEquals(1, all.size)
        assertEquals("Alpha Updated", all[0].label)
    }

    @Test
    fun applicationDao_selectionAndExclusion_filters() = runBlocking {
        applicationDao.insertApp(sampleApp("com.a", "Alpha").copy(isSelected = true))
        applicationDao.insertApp(sampleApp("com.b", "Beta").copy(isExcluded = true))

        val selected = applicationDao.getSelectedApps().first()
        val nonExcluded = applicationDao.getNonExcludedApps().first()

        assertEquals(listOf("com.a"), selected.map { it.packageName })
        assertEquals(listOf("com.a"), nonExcluded.map { it.packageName })
    }

    @Test
    fun applicationDao_setSelectedAndExcluded_updatesFlags() = runBlocking {
        applicationDao.insertApp(sampleApp("com.a", "Alpha"))
        applicationDao.setSelected("com.a", true)
        applicationDao.setExcluded("com.a", true)

        val app = applicationDao.getAppByPackageName("com.a")

        assertNotNull(app)
        assertEquals(true, app?.isSelected)
        assertEquals(true, app?.isExcluded)
    }

    @Test
    fun applicationDao_search_filtersByLabelOrPackage() = runBlocking {
        applicationDao.insertApp(sampleApp("com.alpha", "Alpha"))
        applicationDao.insertApp(sampleApp("com.beta", "Beta"))

        val alpha = applicationDao.searchApps("alpha").first()
        val beta = applicationDao.searchApps("Beta").first()

        assertEquals(1, alpha.size)
        assertEquals(1, beta.size)
    }

    @Test
    fun applicationDao_counts() = runBlocking {
        applicationDao.insertApp(sampleApp("com.a", "Alpha").copy(isSelected = true, isSystemApp = true))
        applicationDao.insertApp(sampleApp("com.b", "Beta"))

        assertEquals(2, applicationDao.getAppCount().first())
        assertEquals(1, applicationDao.getSelectedAppCount().first())
        assertEquals(1, applicationDao.getSystemAppCount().first())
    }

    @Test
    fun applicationDao_deleteByPackage_removesRow() = runBlocking {
        applicationDao.insertApp(sampleApp("com.a", "Alpha"))
        applicationDao.deleteAppByPackageName("com.a")

        assertNull(applicationDao.getAppByPackageName("com.a"))
    }

    // --- app_policy ---

    @Test
    fun appPolicyDao_insertGetUpdate() = runBlocking {
        appPolicyDao.insertPolicy(AppPolicyEntity(packageName = "com.a"))

        val policy = appPolicyDao.getPolicyByPackageName("com.a")
        assertNotNull(policy)
        assertEquals("com.a", policy?.packageName)

        appPolicyDao.setMonitorEnabled("com.a", false, timestamp = 1234L)

        val updated = appPolicyDao.getPolicyByPackageName("com.a")
        assertEquals(false, updated?.monitorEnabled)
        assertEquals(true, updated?.notificationEnabled)
    }

    @Test
    fun appPolicyDao_uniquePackageName_insertReplaces() = runBlocking {
        appPolicyDao.insertPolicy(AppPolicyEntity(packageName = "com.a", monitorEnabled = true))
        appPolicyDao.insertPolicy(AppPolicyEntity(packageName = "com.a", monitorEnabled = false))

        val all = appPolicyDao.getAllPolicies().first()

        assertEquals(1, all.size)
        assertEquals(false, all[0].monitorEnabled)
    }

    @Test
    fun appPolicyDao_enabledAndMonitored_filters() = runBlocking {
        appPolicyDao.insertPolicy(AppPolicyEntity(packageName = "com.enabled", enabled = true, monitorEnabled = true))
        appPolicyDao.insertPolicy(AppPolicyEntity(packageName = "com.disabled", enabled = false, monitorEnabled = true))
        appPolicyDao.insertPolicy(AppPolicyEntity(packageName = "com.unmonitored", enabled = true, monitorEnabled = false))

        val enabled = appPolicyDao.getEnabledPolicies().first()
        val monitored = appPolicyDao.getMonitoredPolicies().first()

        assertEquals(listOf("com.enabled", "com.unmonitored"), enabled.map { it.packageName })
        assertEquals(listOf("com.enabled"), monitored.map { it.packageName })
    }

    @Test
    fun appPolicyDao_deleteByPackage_removesRow() = runBlocking {
        appPolicyDao.insertPolicy(AppPolicyEntity(packageName = "com.a"))
        appPolicyDao.deletePolicyByPackageName("com.a")

        assertNull(appPolicyDao.getPolicyByPackageName("com.a"))
    }

    // --- profile + profile_app ---

    @Test
    fun profileDao_insertGetByNameAndId() = runBlocking {
        val id = profileDao.insertProfile(
            ProfileEntity(name = "Night", scheduleEnabled = true, startTime = 1320, endTime = 420)
        )

        assertTrue(id > 0L)
        val byId = profileDao.getProfileById(id)
        val byName = profileDao.getProfileByName("Night")

        assertEquals("Night", byId?.name)
        assertEquals(id, byName?.id)
    }

    @Test
    fun profileDao_insertProfileApps_listsApps() = runBlocking {
        val profileId = profileDao.insertProfile(ProfileEntity(name = "Work"))
        profileDao.insertProfileApp(ProfileAppEntity(profileId = profileId, packageName = "com.a"))
        profileDao.insertProfileApp(ProfileAppEntity(profileId = profileId, packageName = "com.b"))

        val apps = profileDao.getAppsForProfileList(profileId)

        assertEquals(setOf("com.a", "com.b"), apps.map { it.packageName }.toSet())
    }

    @Test
    fun profileDao_uniqueProfileApp_isReplaced() = runBlocking {
        val profileId = profileDao.insertProfile(ProfileEntity(name = "Work"))
        profileDao.insertProfileApp(ProfileAppEntity(profileId = profileId, packageName = "com.a", enabled = true))
        profileDao.insertProfileApp(ProfileAppEntity(profileId = profileId, packageName = "com.a", enabled = false))

        val apps = profileDao.getAppsForProfileList(profileId)

        assertEquals(1, apps.size)
        assertEquals(false, apps[0].enabled)
    }

    @Test
    fun profileDao_deleteProfileWithApps_removesBoth() = runBlocking {
        val profileId = profileDao.insertProfile(ProfileEntity(name = "Night"))
        profileDao.insertProfileApp(ProfileAppEntity(profileId = profileId, packageName = "com.a"))

        profileDao.deleteProfileWithApps(profileId)

        assertNull(profileDao.getProfileById(profileId))
        assertEquals(0, profileDao.getAppsForProfileList(profileId).size)
        assertEquals(0, profileDao.getProfileCount().first())
    }

    @Test
    fun profileDao_setEnabledAndScheduleEnabled_updatesFlags() = runBlocking {
        val id = profileDao.insertProfile(ProfileEntity(name = "Night", enabled = true, scheduleEnabled = false))
        profileDao.setEnabled(id, false)
        profileDao.setScheduleEnabled(id, true)

        val profile = profileDao.getProfileById(id)

        assertEquals(false, profile?.enabled)
        assertEquals(true, profile?.scheduleEnabled)
    }

    // --- history_event ---

    @Test
    fun historyDao_insertGetLatestAndCount() = runBlocking {
        historyDao.insertEvent(
            HistoryEventEntity(timestamp = 2000L, eventType = HistoryEventEntity.EVENT_TYPE_ERROR, title = "t2")
        )
        historyDao.insertEvent(
            HistoryEventEntity(timestamp = 1000L, eventType = HistoryEventEntity.EVENT_TYPE_CHECK, title = "t1")
        )

        val latest = historyDao.getLatestEvent()
        val all = historyDao.getAllEvents().first()

        assertEquals("t2", latest?.title)
        assertEquals(2, all.size)
        assertEquals(2, historyDao.getEventCount().first())
    }

    @Test
    fun historyDao_filterByTypePackageProfileAndRange() = runBlocking {
        historyDao.insertEvents(
            listOf(
                HistoryEventEntity(timestamp = 1000L, eventType = "CHECK", title = "check", packageName = "com.a", profileId = 1L),
                HistoryEventEntity(timestamp = 2000L, eventType = "ERROR", title = "err", packageName = "com.b", profileId = 1L),
                HistoryEventEntity(timestamp = 3000L, eventType = "CHECK", title = "check2", packageName = "com.a", profileId = 2L)
            )
        )

        assertEquals(listOf("check2", "check"), historyDao.getEventsByType("CHECK").first().map { it.title })
        assertEquals(listOf("check2", "check"), historyDao.getEventsByPackage("com.a").first().map { it.title })
        assertEquals(listOf("err", "check"), historyDao.getEventsByProfile(1L).first().map { it.title })
        assertEquals(listOf("check2", "err"), historyDao.getEventsByTimeRange(2000L, 3000L).first().map { it.title })
        assertEquals(listOf("check2", "err"), historyDao.getEventsSince(2000L).first().map { it.title })
    }

    @Test
    fun historyDao_deleteOlderThan_removesOnlyOldEvents() = runBlocking {
        historyDao.insertEvent(HistoryEventEntity(timestamp = 1000L, eventType = "CHECK", title = "old"))
        historyDao.insertEvent(HistoryEventEntity(timestamp = 5000L, eventType = "CHECK", title = "new"))

        val removed = historyDao.deleteEventsOlderThan(3000L)

        assertEquals(1, removed)
        assertEquals(listOf("new"), historyDao.getAllEvents().first().map { it.title })
    }

    @Test
    fun historyDao_keepMostRecent_removesOldest() = runBlocking {
        historyDao.insertEvent(HistoryEventEntity(timestamp = 1000L, eventType = "CHECK", title = "old"))
        historyDao.insertEvent(HistoryEventEntity(timestamp = 2000L, eventType = "CHECK", title = "mid"))
        historyDao.insertEvent(HistoryEventEntity(timestamp = 3000L, eventType = "CHECK", title = "new"))

        historyDao.keepMostRecentEvents(2)

        assertEquals(listOf("new", "mid"), historyDao.getAllEvents().first().map { it.title })
    }

    @Test
    fun historyDao_countEventsSince() = runBlocking {
        historyDao.insertEvent(
            HistoryEventEntity(timestamp = 1000L, eventType = "ERROR", title = "e1", packageName = "com.a")
        )
        historyDao.insertEvent(
            HistoryEventEntity(timestamp = 5000L, eventType = "ERROR", title = "e2", packageName = "com.a")
        )
        historyDao.insertEvent(
            HistoryEventEntity(timestamp = 4000L, eventType = "CHECK", title = "c1", packageName = "com.a")
        )

        assertEquals(1, historyDao.countEventsSince("com.a", "ERROR", 2000L))
        assertEquals(2, historyDao.countEventsSince("com.a", "ERROR", 0L))
    }

    // --- activity_event ---

    @Test
    fun activityEventDao_insertFiltersAndStats() = runBlocking {
        activityEventDao.insertEvents(
            listOf(
                ActivityEventEntity(packageName = "com.a", timestamp = 1000L, eventType = "APP_ACTIVE", source = "engine"),
                ActivityEventEntity(packageName = "com.a", timestamp = 2000L, eventType = "APP_ACTIVE", source = "engine"),
                ActivityEventEntity(packageName = "com.b", timestamp = 3000L, eventType = "APP_INACTIVE", source = "engine")
            )
        )

        assertEquals(3, activityEventDao.getEventCount().first())
        assertEquals(2, activityEventDao.getEventsByPackage("com.a").first().size)
        assertEquals(2, activityEventDao.getEventsByType("APP_ACTIVE").first().size)
        assertEquals(2, activityEventDao.countEventsByTypeSince("com.a", "APP_ACTIVE", 0L).first())

        val stats = activityEventDao.getEventStatsByTypeSince("APP_ACTIVE", 0L).first()
        assertEquals(listOf("com.a"), stats.map { it.packageName })
        assertEquals(2, stats[0].count)
    }

    @Test
    fun activityEventDao_deleteOlderThanAndPackage() = runBlocking {
        activityEventDao.insertEvent(
            ActivityEventEntity(packageName = "com.a", timestamp = 1000L, eventType = "APP_ACTIVE", source = "engine")
        )
        activityEventDao.insertEvent(
            ActivityEventEntity(packageName = "com.b", timestamp = 5000L, eventType = "APP_ACTIVE", source = "engine")
        )

        activityEventDao.deleteEventsForPackage("com.a")

        val remaining = activityEventDao.getAllEvents().first()
        assertEquals(listOf("com.b"), remaining.map { it.packageName })

        activityEventDao.insertEvent(
            ActivityEventEntity(packageName = "com.c", timestamp = 6000L, eventType = "APP_ACTIVE", source = "engine")
        )
        val removed = activityEventDao.deleteEventsOlderThan(5500L)
        assertEquals(1, removed)
        assertEquals(listOf("com.c"), activityEventDao.getAllEvents().first().map { it.packageName })
    }

    @Test
    fun activityEventDao_keepMostRecent_removesOldest() = runBlocking {
        activityEventDao.insertEvent(
            ActivityEventEntity(packageName = "com.a", timestamp = 1000L, eventType = "APP_ACTIVE", source = "engine")
        )
        activityEventDao.insertEvent(
            ActivityEventEntity(packageName = "com.b", timestamp = 2000L, eventType = "APP_ACTIVE", source = "engine")
        )
        activityEventDao.insertEvent(
            ActivityEventEntity(packageName = "com.c", timestamp = 3000L, eventType = "APP_ACTIVE", source = "engine")
        )

        activityEventDao.keepMostRecentEvents(2)

        val remaining = activityEventDao.getAllEvents().first()
        assertEquals(listOf("com.c", "com.b"), remaining.map { it.packageName })
    }

    @Test
    fun databaseAccessors_areNotNull() {
        assertNotNull(db.applicationDao())
        assertNotNull(db.appPolicyDao())
        assertNotNull(db.profileDao())
        assertNotNull(db.historyDao())
        assertNotNull(db.activityEventDao())
    }
}