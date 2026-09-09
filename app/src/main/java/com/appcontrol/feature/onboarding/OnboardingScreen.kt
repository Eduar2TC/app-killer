package com.appcontrol.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.appcontrol.feature.navigation.AppDependencies
import com.appcontrol.feature.navigation.Screen
import kotlinx.coroutines.launch

private data class OnboardingPage(
    val icon: ImageVector,
    val title: String,
    val description: String
)

private val onboardingPages = listOf(
    OnboardingPage(
        icon = Icons.Default.Home,
        title = "Welcome",
        description = "App Control monitors your installed apps and helps you manage which " +
            "apps stay active. Get started by choosing the apps you want to keep an eye on."
    ),
    OnboardingPage(
        icon = Icons.Default.List,
        title = "Monitor apps",
        description = "Select apps you want to monitor. App Control tracks their activity and " +
            "notifies you when a monitored app becomes active."
    ),
    OnboardingPage(
        icon = Icons.Default.DateRange,
        title = "Night protection",
        description = "Enable night protection to automatically check your selected apps during " +
            "night hours, keeping your device calm while you sleep."
    ),
    OnboardingPage(
        icon = Icons.Default.Notifications,
        title = "Stay informed",
        description = "Get notifications when an app reappears, so you can review what happened " +
            "and take action directly from the notification."
    ),
    OnboardingPage(
        icon = Icons.Default.Settings,
        title = "Grant permissions",
        description = "App Control needs a few permissions to work correctly: usage access, " +
            "notifications, and exact alarm scheduling. You can grant them on the next screen."
    )
)

@Composable
fun OnboardingScreen(
    navController: NavHostController,
    deps: AppDependencies
) {
    val viewModel: OnboardingViewModel = viewModel(
        factory = OnboardingViewModel.Factory(deps.preferencesManager)
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { state.totalSteps })

    LaunchedEffect(state.currentStep) {
        pagerState.animateScrollToPage(state.currentStep)
    }

    LaunchedEffect(state.isCompleted) {
        if (state.isCompleted) {
            navController.navigate(Screen.Permissions.route) {
                popUpTo(Screen.Onboarding.route) { inclusive = true }
            }
        }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                if (state.currentStep < state.totalSteps - 1) {
                    TextButton(onClick = { viewModel.skip() }) {
                        Text("Skip")
                    }
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                PageContent(page = onboardingPages[page])
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (state.currentStep > 0) {
                    TextButton(onClick = { viewModel.previous() }) {
                        Text("Back")
                    }
                } else {
                    Spacer(Modifier.width(1.dp))
                }
                PageIndicator(
                    currentPage = state.currentStep,
                    totalPages = state.totalSteps
                )
                if (state.currentStep < state.totalSteps - 1) {
                    TextButton(onClick = { viewModel.next() }) {
                        Text("Next")
                    }
                } else {
                    Button(onClick = { viewModel.complete() }) {
                        Text("Get started")
                    }
                }
            }
        }
    }
}

@Composable
private fun PageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = page.icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Spacer(Modifier.height(32.dp))
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PageIndicator(currentPage: Int, totalPages: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(totalPages) { index ->
            Box(
                modifier = Modifier
                    .size(if (index == currentPage) 10.dp else 8.dp)
                    .clip(CircleShape)
                    .background(
                        if (index == currentPage) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        }
                    )
            )
        }
    }
}