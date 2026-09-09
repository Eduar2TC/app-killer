package com.appcontrol

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.appcontrol.domain.model.AppInfo
import com.appcontrol.feature.AppControlTheme
import com.appcontrol.feature.navigation.AppNavGraph
import com.appcontrol.feature.navigation.Screen
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ApplicationsScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var deps: UiTestDependencies

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        deps = UiTestDependencies(context)
        deps.appRepository.seed(
            listOf(
                AppInfo(packageName = "com.alpha", label = "Alpha", isSelected = true),
                AppInfo(packageName = "com.beta", label = "Beta"),
                AppInfo(packageName = "com.sys", label = "Sys Tool", isSystemApp = true)
            )
        )
    }

    private fun setContent() {
        composeRule.setContent {
            AppControlTheme {
                val navController = rememberNavController()
                LaunchedEffect(Unit) {
                    navController.navigate(Screen.Applications.route)
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
    fun applications_rendersListAndActions() {
        setContent()

        waitForText("Applications")

        composeRule.onNodeWithText("Applications").assertExists()
        composeRule.onNodeWithText("1 selected").assertExists()
        composeRule.onNodeWithText("Select all").assertExists()
        composeRule.onNodeWithText("Deselect all").assertExists()
        composeRule.onNodeWithText("Search apps").assertExists()
        composeRule.onNodeWithText("Alpha").assertExists()
        composeRule.onNodeWithText("Beta").assertExists()
        composeRule.onNodeWithText("Sys Tool").assertExists()
        composeRule.onNodeWithText("USER").assertExists()
        composeRule.onNodeWithText("SYSTEM").assertExists()
        composeRule.onNodeWithText("SELECTED").assertExists()
    }

    @Test
    fun applications_searchFiltersList() {
        setContent()

        waitForText("Alpha")

        composeRule.onNode(hasSetTextAction()).performTextInput("Bet")

        waitForText("Beta")
        composeRule.onNodeWithText("Beta").assertExists()
        composeRule.onAllNodesWithText("Alpha").assertCountEquals(0)
        composeRule.onAllNodesWithText("Sys Tool").assertCountEquals(0)
    }

    @Test
    fun applications_clearSearch_restoresList() {
        setContent()

        waitForText("Alpha")

        composeRule.onNode(hasSetTextAction()).performTextInput("XYZ")

        composeRule.waitUntil(timeoutMillis = 8_000) {
            composeRule.onAllNodesWithText("Alpha").fetchSemanticsNodes().isEmpty()
        }

        composeRule.onNode(hasSetTextAction()).performTextClearance()

        waitForText("Alpha")
        composeRule.onNodeWithText("Alpha").assertExists()
        composeRule.onNodeWithText("Sys Tool").assertExists()
    }

    @Test
    fun applications_userFilter_showsOnlyUserApps() {
        setContent()

        waitForText("Alpha")

        composeRule.onNodeWithText("USER").performClick()

        waitForText("Beta")
        composeRule.onNodeWithText("Alpha").assertExists()
        composeRule.onNodeWithText("Beta").assertExists()
        composeRule.onAllNodesWithText("Sys Tool").assertCountEquals(0)
    }

    @Test
    fun applications_systemFilter_showsOnlySystemApps() {
        setContent()

        waitForText("Alpha")

        composeRule.onNodeWithText("SYSTEM").performClick()

        waitForText("Sys Tool")
        composeRule.onNodeWithText("Sys Tool").assertExists()
        composeRule.onAllNodesWithText("Alpha").assertCountEquals(0)
        composeRule.onAllNodesWithText("Beta").assertCountEquals(0)
    }

    @Test
    fun applications_selectedFilter_showsOnlySelectedApps() {
        setContent()

        waitForText("Alpha")

        composeRule.onNodeWithText("SELECTED").performClick()

        waitForText("Alpha")
        composeRule.onNodeWithText("Alpha").assertExists()
        composeRule.onAllNodesWithText("Beta").assertCountEquals(0)
        composeRule.onAllNodesWithText("Sys Tool").assertCountEquals(0)
    }

    @Test
    fun applications_selectAll_incrementsSelectedCount() {
        setContent()

        waitForText("1 selected")

        composeRule.onNodeWithText("Select all").performClick()

        waitForText("3 selected")
        composeRule.onNodeWithText("3 selected").assertExists()
    }
}