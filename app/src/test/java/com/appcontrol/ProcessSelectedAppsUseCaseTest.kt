package com.appcontrol

import com.appcontrol.domain.model.ActivityEvent
import com.appcontrol.domain.model.AppInfo
import com.appcontrol.domain.model.EventType
import com.appcontrol.domain.model.HistoryEventType
import com.appcontrol.domain.model.StopResult
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
    private lateinit var processStopper: FakeProcessStopper
    private lateinit var useCase: ProcessSelectedAppsUseCase

    @Before
    fun setUp() {
        appRepository = FakeAppRepository()
        activityRepository = FakeActivityEventRepository()
        historyRepository = FakeHistoryRepository()
        processStopper = FakeProcessStopper()
        val monitor = MonitorAppActivityUseCase(appRepository, activityRepository, historyRepository)
        useCase = ProcessSelectedAppsUseCase(
            appRepository = appRepository,
            historyRepository = historyRepository,
            activityEventRepository = activityRepository,
            monitorAppActivity = monitor,
            processStopper = processStopper
        )
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
    fun allAppsRunning_returnsStoppedForEveryApp() = runBlocking {
        appRepository.apps += selectedApp("com.a")
        appRepository.apps += selectedApp("com.b")
        activityRepository.latestByPackage["com.a"] = activeEvent("com.a")
        activityRepository.latestByPackage["com.b"] = activeEvent("com.b")

        val result = useCase.invoke()

        assertEquals(2, result.total)
        assertEquals(2, result.stopped)
        assertEquals(0, result.inactive)
        assertEquals(0, result.failed)
        assertEquals(setOf("com.a", "com.b"), result.results.keys)
        assertTrue(result.results.values.all { it.success && it.actionTaken })
        assertEquals(setOf("com.a", "com.b"), processStopper.stoppedPackages.toSet())

        val actions = historyRepository.events.filter { it.eventType == HistoryEventType.ACTION }
        assertEquals(2, actions.size)
        assertTrue(actions.all { it.title == "App stopped" })
    }

    @Test
    fun someAppsUnavailable_areFilteredOut() = runBlocking {
        appRepository.apps += selectedApp("com.excluded", isExcluded = true)
        appRepository.apps += selectedApp("com.ok")
        activityRepository.latestByPackage["com.ok"] = activeEvent("com.ok")

        val result = useCase.invoke()

        assertEquals(1, result.total)
        assertEquals(1, result.stopped)
        assertFalse(result.results.containsKey("com.excluded"))
    }

    @Test
    fun emptyList_returnsEmptyResult() = runBlocking {
        val result = useCase.invoke()

        assertEquals(0, result.total)
        assertEquals(0, result.stopped)
        assertEquals(0, result.inactive)
        assertEquals(0, result.failed)
        assertTrue(result.results.isEmpty())
        assertTrue(processStopper.stoppedPackages.isEmpty())
    }

    @Test
    fun notRunningApp_countsInactive_noAction() = runBlocking {
        appRepository.apps += selectedApp("com.inactive")
        activityRepository.latestByPackage["com.inactive"] = inactiveEvent("com.inactive")

        val result = useCase.invoke()

        assertEquals(0, result.stopped)
        assertEquals(1, result.inactive)
        val outcome = result.results["com.inactive"]!!
        assertTrue(outcome.success)
        assertFalse(outcome.actionTaken)
        assertTrue(processStopper.stoppedPackages.isEmpty())
    }

    @Test
    fun runningApp_isStopped() = runBlocking {
        appRepository.apps += selectedApp("com.running")
        activityRepository.latestByPackage["com.running"] = activeEvent("com.running")

        val result = useCase.invoke()

        assertEquals(1, result.stopped)
        val outcome = result.results["com.running"]!!
        assertTrue(outcome.success)
        assertTrue(outcome.actionTaken)
        assertEquals(listOf("com.running"), processStopper.stoppedPackages)
        val action = historyRepository.events.single { it.title == "App stopped" }
        assertEquals("com.running", action.packageName)
    }

    @Test
    fun stopperFailure_isReported() = runBlocking {
        appRepository.apps += selectedApp("com.failing")
        activityRepository.latestByPackage["com.failing"] = activeEvent("com.failing")
        processStopper.result = StopResult.FAILED

        val result = useCase.invoke()

        assertEquals(0, result.stopped)
        assertEquals(1, result.failed)
        val outcome = result.results["com.failing"]!!
        assertFalse(outcome.success)
        assertFalse(outcome.actionTaken)
        assertTrue(historyRepository.events.any { it.title == "App not stopped" })
    }

    @Test
    fun unexpectedError_isReported() = runBlocking {
        appRepository.apps += selectedApp("com.boom")
        activityRepository.throwOnGetLatest = true

        val result = useCase.invoke()

        assertEquals(1, result.failed)
        val outcome = result.results["com.boom"]!!
        assertFalse(outcome.success)
        assertFalse(outcome.actionTaken)
    }

    @Test
    fun mixedApps_producesMixedResult() = runBlocking {
        appRepository.apps += selectedApp("com.stopped")
        appRepository.apps += selectedApp("com.idle")
        appRepository.apps += selectedApp("com.blocked")
        activityRepository.latestByPackage["com.stopped"] = activeEvent("com.stopped")
        activityRepository.latestByPackage["com.idle"] = inactiveEvent("com.idle")
        activityRepository.latestByPackage["com.blocked"] = activeEvent("com.blocked")
        processStopper.resultByPackage = mapOf("com.blocked" to StopResult.FAILED)

        val result = useCase.invoke()

        assertEquals(3, result.total)
        assertEquals(1, result.stopped)
        assertEquals(1, result.inactive)
        assertEquals(1, result.failed)
        assertEquals(1, result.results.values.count { it.actionTaken })
        assertEquals(2, result.results.values.count { !it.actionTaken })
    }
}