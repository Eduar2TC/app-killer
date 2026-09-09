package com.appcontrol

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.appcontrol.core.common.Constants
import com.appcontrol.feature.AppControlTheme
import com.appcontrol.feature.navigation.AppNavGraph
import com.appcontrol.feature.navigation.Screen
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var deps: UiTestDependencies

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        deps = UiTestDependencies(context)
        runBlocking {
            deps.preferencesManager.setShowSystemApps(true)
            deps.preferencesManager.setConfirmActions(true)
            deps.preferencesManager.setVibration(true)
            deps.preferencesManager.setDetectActivity(true)
            deps.preferencesManager.setMonitoringInterval(
                Constants.DEFAULT_MONITORING_INTERVAL_MINUTES
            )
            deps.preferencesManager.setNotificationsEnabled(true)
            deps.preferencesManager.setHistoryRetentionDays(
                Constants.DEFAULT_HISTORY_RETENTION_DAYS
            )
            deps.preferencesManager.setNotificationCooldownMinutes(
                Constants.DEFAULT_NOTIFICATION_COOLDOWN_MINUTES
            )
            deps.preferencesManager.setMinimumEventIntervalMinutes(
                Constants.DEFAULT_MINIMUM_EVENT_INTERVAL_MINUTES
            )
        }
    }

    private fun setContent() {
        composeRule.setContent {
            AppControlTheme {
                val navController = rememberNavController()
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Settings.route)
                }
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
    fun settings_rendersGeneralSection() {
        setContent()

        waitForText("Settings")

        composeRule.onNodeWithText("Settings").assertExists()
        composeRule.onNodeWithText("General").assertExists()
        composeRule.onNodeWithText("Show system apps").assertExists()
        composeRule.onNodeWithText("Confirm before actions").assertExists()
        composeRule.onNodeWithText("Vibration").assertExists()
    }

    @Test
    fun settings_rendersMonitoringSection() {
        setContent()

        waitForText("Monitoring interval (15 min)")

        composeRule.onNodeWithText("Monitoring").assertExists()
        composeRule.onNodeWithText("Usage access permission").assertExists()
        composeRule.onNodeWithText("Notification permission").assertExists()
        composeRule.onNodeWithText("Exact alarm permission").assertExists()
        composeRule.onNodeWithText("Detect app activity").assertExists()
        composeRule.onNodeWithText("Monitoring interval (15 min)").assertExists()
        composeRule.onNodeWithText("Min event interval (${Constants.DEFAULT_MINIMUM_EVENT_INTERVAL_MINUTES} min)").assertExists()
    }

    @Test
    fun settings_rendersNotificationsSection() {
        setContent()

        waitForText("Notifications enabled")

        composeRule.onNodeWithText("Notifications").assertExists()
        composeRule.onNodeWithText("Notifications enabled").assertExists()
    }

    @Test
    fun settings_deleteHistory_showsConfirmationAndMessage() {
        setContent()

        waitForText("Settings")

        composeRule.onNodeWithText("Delete all history")
            .performScrollTo()
            .performClick()

        waitForText("This will permanently delete all history and activity events from the database.")
        composeRule.onNodeWithText("This will permanently delete all history and activity events from the database.")
            .assertExists()

        composeRule.onNodeWithText("Delete").performClick()

        waitForText("Deleted 0 history and 0 activity events")
        composeRule.onNodeWithText("Deleted 0 history and 0 activity events").assertExists()
    }

    @Test
    fun settings_deleteHistory_cancelClosesDialog() {
        setContent()

        waitForText("Settings")

        composeRule.onNodeWithText("Delete all history")
            .performScrollTo()
            .performClick()

        waitForText("This will permanently delete all history and activity events from the database.")

        composeRule.onNodeWithText("Cancel").performClick()

        composeRule.waitUntil(timeoutMillis = 8_000) {
            composeRule.onAllNodesWithText(
                "This will permanently delete all history and activity events from the database."
            ).fetchSemanticsNodes().isEmpty()
        }
    }

    @Test
    fun settings_rendersPrivacySection() {
        setContent()

        waitForText("History retention (${Constants.DEFAULT_HISTORY_RETENTION_DAYS} days)")

        composeRule.onNodeWithText("Privacy").assertExists()
        composeRule.onNodeWithText("History retention (${Constants.DEFAULT_HISTORY_RETENTION_DAYS} days)")
            .assertExists()
        composeRule.onNodeWithText("Delete all history").assertExists()
        composeRule.onNodeWithText("Export").assertExists()
        composeRule.onNodeWithText("Import").assertExists()
    }
}