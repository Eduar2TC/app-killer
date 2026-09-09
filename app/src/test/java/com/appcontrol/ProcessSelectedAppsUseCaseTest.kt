package com.appcontrol

import com.appcontrol.domain.model.ActivityEvent
import com.appcontrol.domain.model.AppInfo
import com.appcontrol.domain.model.EventType
import com.appcontrol.domain.model.ProcessOutcome
import com.appcontrol.domain.usecase.MonitorAppActivityUseCase
import com.appcontrol.domain.usecase.ProcessSelectedAppsUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ProcessSelectedAppsUseCaseTest {

    private lateinit var appRepository: FakeAppRepository
    private lateinit var activityRepository: FakeActivityEventRepository
    private lateinit var historyRepository: FakeHistoryRepository
    private lateinit var useCase: ProcessSelectedAppsUseCase

    @Before
    fun setUp() {
        appRepository = FakeAppRepository()
        activityRepository = FakeActivityEventRepository()
        historyRepository = FakeHistoryRepository()
        val monitor = MonitorAppActivityUseCase(appRepository, activityRepository, historyRepository)
        useCase = ProcessSelectedAppsUseCase(appRepository, historyRepository, activityRepository, monitor)
    }

    private fun selectedApp(packageName: String, isExcluded: Boolean = false): AppInfo =
        AppInfo(
            packageName = packageName,
            label = packageName,
            isSelected = true,
            isExcluded = isExcluded
        )

    private fun inactiveEvent(packageName: String): ActivityEvent =
        ActivityEvent(packageName = packageName, timestamp = 1_000L, eventType = EventType.APP_INACTIVE, source = "test")

    private fun activeEvent(packageName: String): ActivityEvent =
        ActivityEvent(packageName = packageName, timestamp = 1_000L, eventType = EventType.APP_ACTIVE, source = "test")

    @Test
    fun allAppsAvailable_returnsSuccessForEveryApp() = runBlocking {
        appRepository.apps += selectedApp("com.a")
        appRepository.apps += selectedApp("com.b")
        activityRepository.latestByPackage["com.a"] = inactiveEvent("com.a")
        activityRepository.latestByPackage["com.b"] = inactiveEvent("com.b")

        val result = useCase.invoke()

        assertEquals(2, result.total)
        assertEquals(2, result.processed)
        assertEquals(0, result.failed)
        assertEquals(0, result.notAllowed)
        assertEquals(setOf("com.a", "com.b"), result.results.keys)
        assertTrue(result.results.values.all { it.success && it.actionTaken })
    }

    @Test
    fun someAppsUnavailable_areFilteredOut() = runBlocking {
        appRepository.apps += selectedApp("com.excluded", isExcluded = true)
        appRepository.apps += selectedApp("com.ok")
        activityRepository.latestByPackage["com.ok"] = inactiveEvent("com.ok")

        val result = useCase.invoke()

        assertEquals(1, result.total)
        assertEquals(1, result.processed)
        assertFalse(result.results.containsKey("com.excluded"))
    }

    @Test
    fun emptyList_returnsEmptyResult() = runBlocking {
        val result = useCase.invoke()

        assertEquals(0, result.total)
        assertEquals(0, result.processed)
        assertEquals(0, result.failed)
        assertEquals(0, result.notAllowed)
        assertTrue(result.results.isEmpty())
    }

    @Test
    fun inactiveApp_successState_takesAction() = runBlocking {
        appRepository.apps += selectedApp("com.inactive")
        activityRepository.latestByPackage["com.inactive"] = inactiveEvent("com.inactive")

        val result = useCase.invoke()

        assertEquals(1, result.processed)
        val outcome = result.results["com.inactive"]!!
        assertTrue(outcome.success)
        assertTrue(outcome.actionTaken)
        assertFalse(outcome.requiresManualIntervention)
        assertTrue(result.results["com.inactive"] is ProcessOutcome)
    }

    @Test
    fun activeApp_notAllowedState_requiresManualIntervention() = runBlocking {
        appRepository.apps += selectedApp("com.active")
        activityRepository.latestByPackage["com.active"] = activeEvent("com.active")

        val result = useCase.invoke()

        assertEquals(0, result.processed)
        assertEquals(1, result.notAllowed)
        val outcome = result.results["com.active"]!!
        assertTrue(outcome.success)
        assertFalse(outcome.actionTaken)
        assertTrue(outcome.requiresManualIntervention)
    }

    @Test
    fun failingApp_manualRequiredState_isReported() = runBlocking {
        appRepository.apps += selectedApp("com.failing")
        activityRepository.throwOnGetLatest = true

        val result = useCase.invoke()

        assertEquals(0, result.processed)
        assertEquals(0, result.notAllowed)
        assertEquals(1, result.failed)
        val outcome = result.results["com.failing"]!!
        assertFalse(outcome.success)
        assertFalse(outcome.actionTaken)
        assertTrue(outcome.requiresManualIntervention)
    }

    @Test
    fun mixedApps_producesMixedResult() = runBlocking {
        appRepository.apps += selectedApp("com.inactive")
        appRepository.apps += selectedApp("com.active")
        activityRepository.latestByPackage["com.inactive"] = inactiveEvent("com.inactive")
        activityRepository.latestByPackage["com.active"] = activeEvent("com.active")

        val result = useCase.invoke()

        assertEquals(2, result.total)
        assertEquals(1, result.processed)
        assertEquals(1, result.notAllowed)
        assertEquals(0, result.failed)
        assertEquals(1, result.results.values.count { it.actionTaken })
        assertEquals(1, result.results.values.count { it.requiresManualIntervention })
    }
}