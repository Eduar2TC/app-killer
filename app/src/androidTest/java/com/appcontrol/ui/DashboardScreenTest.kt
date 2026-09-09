package com.appcontrol

import androidx.activity.ComponentActivity
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.appcontrol.domain.model.ActivityEvent
import com.appcontrol.domain.model.AppInfo
import com.appcontrol.domain.model.EventType
import com.appcontrol.feature.AppControlTheme
import com.appcontrol.feature.navigation.AppNavGraph
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick

@RunWith(AndroidJUnit4::class)
class DashboardScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var deps: UiTestDependencies

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        deps = UiTestDependencies(context)
    }

    private fun selectedApp(packageName: String, label: String): AppInfo =
        AppInfo(packageName = packageName, label = label, isSelected = true)

    private fun setContent() {
        composeRule.setContent {
            AppControlTheme {
                val navController = rememberNavController()
                AppNavGraph(navController, deps.deps())
            }
        }
    }

    private fun waitForText(text: String) {
        composeRule.waitUntil(timeoutMillis = 8_000) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun dashboard_rendersHeaderAndMonitoredCount() {
        deps.appRepository.seed(listOf(selectedApp("com.a", "Alpha"), selectedApp("com.b", "Beta")))
        setContent()

        waitForText("Process Apps")

        composeRule.onNodeWithText("APP CONTROL").assertExists()
        composeRule.onNodeWithText("2 apps monitored").assertExists()
        composeRule.onNodeWithText("Active profile: Default").assertExists()
        composeRule.onNodeWithText("Profiles").assertExists()
        composeRule.onNodeWithText("History").assertExists()
        composeRule.onNodeWithText("Settings").assertExists()
        composeRule.onNodeWithContentDescription("Refresh").assertExists()
    }

    @Test
    fun dashboard_processApps_showsResultMessage() {
        deps.appRepository.seed(listOf(selectedApp("com.a", "Alpha"), selectedApp("com.b", "Beta")))
        setContent()

        waitForText("Process Apps")

        composeRule.onNodeWithText("Process Apps").performClick()

        waitForText("Processed 2 of 2 apps")
        composeRule.onNodeWithText("Processed 2 of 2 apps").assertExists()
    }

    @Test
    fun dashboard_activeApps_sectionShowsActiveApp() {
        deps.appRepository.seed(listOf(selectedApp("com.a", "Alpha")))
        deps.activityEventRepository.latestByPackage["com.a"] = ActivityEvent(
            packageName = "com.a",
            timestamp = System.currentTimeMillis(),
            eventType = EventType.APP_ACTIVE,
            source = "test"
        )
        setContent()

        waitForText("Active apps")

        composeRule.onNodeWithText("Active apps").assertExists()
        composeRule.onNodeWithText("com.a").assertExists()
        assertTrue(
            "Alpha should be listed in at least one app section",
            composeRule.onAllNodesWithText("Alpha").fetchSemanticsNodes().isNotEmpty()
        )
    }

    @Test
    fun dashboard_navigationToHistory_opensHistoryScreen() {
        deps.appRepository.seed(emptyList())
        setContent()

        waitForText("History")

        composeRule.onNodeWithText("History").performClick()

        waitForText("No events for the selected period")
        composeRule.onNodeWithContentDescription("Clean up history").assertExists()
    }

    @Test
    fun dashboard_navigationToProfiles_opensProfilesScreen() {
        deps.appRepository.seed(emptyList())
        setContent()

        waitForText("Profiles")

        composeRule.onNodeWithText("Profiles").performClick()

        waitForText("New profile")
        composeRule.onNodeWithText("Profiles").assertExists()
        composeRule.onNodeWithText("New profile").assertExists()
    }
}