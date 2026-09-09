package com.appcontrol

import com.appcontrol.domain.model.ActivityEvent
import com.appcontrol.domain.model.AppInfo
import com.appcontrol.domain.model.AppStatus
import com.appcontrol.domain.model.EventType
import com.appcontrol.domain.model.HistoryEventType
import com.appcontrol.domain.usecase.MonitorAppActivityUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MonitorAppActivityUseCaseTest {

    private lateinit var appRepository: FakeAppRepository
    private lateinit var activityRepository: FakeActivityEventRepository
    private lateinit var historyRepository: FakeHistoryRepository
    private lateinit var useCase: MonitorAppActivityUseCase

    @Before
    fun setUp() {
        appRepository = FakeAppRepository()
        activityRepository = FakeActivityEventRepository()
        historyRepository = FakeHistoryRepository()
        useCase = MonitorAppActivityUseCase(appRepository, activityRepository, historyRepository)
    }

    private fun activeEvent(packageName: String): ActivityEvent =
        ActivityEvent(packageName = packageName, timestamp = 1_000L, eventType = EventType.APP_ACTIVE, source = "test")

    private fun inactiveEvent(packageName: String): ActivityEvent =
        ActivityEvent(packageName = packageName, timestamp = 1_000L, eventType = EventType.APP_INACTIVE, source = "test")

    @Test
    fun inactiveToActive_detectsReappearance() = runBlocking {
        val chrome = AppInfo(packageName = "com.android.chrome", label = "Chrome", isSelected = true)
        appRepository.apps += chrome
        activityRepository.queuedLatest["com.android.chrome"] = ArrayDeque(
            listOf(inactiveEvent("com.android.chrome"), activeEvent("com.android.chrome"))
        )

        val updated = useCase.invoke()

        assertEquals(1, updated.size)
        assertEquals(AppStatus.REAPPEARED, updated[0].status)
        assertTrue(updated[0].lastActivityTime > 0L)
        assertTrue(activityRepository.events.any { it.eventType == EventType.APP_REAPPEARED })
        assertEquals(
            1,
            historyRepository.events.count { it.eventType == HistoryEventType.REAPPEARED }
        )
    }

    @Test
    fun stillActive_recordsActiveEventOnly() = runBlocking {
        val chrome = AppInfo(packageName = "com.android.chrome", label = "Chrome", isSelected = true)
        appRepository.apps += chrome
        activityRepository.latestByPackage["com.android.chrome"] = activeEvent("com.android.chrome")

        val updated = useCase.invoke()

        assertEquals(AppStatus.ACTIVE, updated[0].status)
        assertTrue(
            activityRepository.events.any {
                it.packageName == "com.android.chrome" && it.eventType == EventType.APP_ACTIVE
            }
        )
        assertTrue(historyRepository.events.none { it.eventType == HistoryEventType.REAPPEARED })
    }

    @Test
    fun stillInactive_recordsInactiveEventOnly() = runBlocking {
        val chrome = AppInfo(packageName = "com.android.chrome", label = "Chrome", isSelected = true)
        appRepository.apps += chrome
        activityRepository.latestByPackage["com.android.chrome"] = inactiveEvent("com.android.chrome")

        val updated = useCase.invoke()

        assertEquals(AppStatus.INACTIVE, updated[0].status)
        assertTrue(
            activityRepository.events.any {
                it.packageName == "com.android.chrome" && it.eventType == EventType.APP_INACTIVE
            }
        )
        assertTrue(historyRepository.events.none { it.eventType == HistoryEventType.REAPPEARED })
    }

    @Test
    fun noPreviousEvent_treatedAsInactive() = runBlocking {
        val chrome = AppInfo(packageName = "com.android.chrome", label = "Chrome", isSelected = true)
        appRepository.apps += chrome

        val updated = useCase.invoke()

        assertEquals(AppStatus.INACTIVE, updated[0].status)
        assertTrue(activityRepository.events.any { it.eventType == EventType.APP_INACTIVE })
    }

    @Test
    fun repeatedChecksInShortTime_doNotDuplicateReappearances() = runBlocking {
        val chrome = AppInfo(packageName = "com.android.chrome", label = "Chrome", isSelected = true)
        appRepository.apps += chrome
        activityRepository.latestByPackage["com.android.chrome"] = activeEvent("com.android.chrome")

        useCase.invoke()
        useCase.invoke()

        assertTrue(activityRepository.events.none { it.eventType == EventType.APP_REAPPEARED })
        assertTrue(historyRepository.events.none { it.eventType == HistoryEventType.REAPPEARED })
        assertEquals(
            2,
            activityRepository.events.count { it.eventType == EventType.APP_ACTIVE }
        )
    }

    @Test
    fun excludedOrUnselectedApps_areNotMonitored() = runBlocking {
        appRepository.apps += AppInfo(packageName = "com.skip", label = "Skip", isSelected = true, isExcluded = true)
        appRepository.apps += AppInfo(packageName = "com.unselected", label = "Unselected", isSelected = false)

        val updated = useCase.invoke()

        assertTrue(updated.isEmpty())
        assertTrue(activityRepository.events.isEmpty())
    }

    @Test
    fun isAppActive_reflectsLatestEvent() = runBlocking {
        assertFalse(useCase.isAppActive("com.android.chrome"))

        activityRepository.latestByPackage["com.android.chrome"] = activeEvent("com.android.chrome")
        assertTrue(useCase.isAppActive("com.android.chrome"))

        activityRepository.latestByPackage["com.android.chrome"] =
            ActivityEvent(packageName = "com.android.chrome", timestamp = 2_000L, eventType = EventType.APP_REAPPEARED, source = "test")
        assertTrue(useCase.isAppActive("com.android.chrome"))

        activityRepository.latestByPackage["com.android.chrome"] = inactiveEvent("com.android.chrome")
        assertFalse(useCase.isAppActive("com.android.chrome"))
    }
}