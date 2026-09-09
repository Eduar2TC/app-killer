package com.appcontrol

import android.app.AppOpsManager
import android.content.Context
import android.os.Build
import android.os.Process
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.appcontrol.data.system.UsageStatsProvider
import com.appcontrol.data.system.UsageStatsProviderImpl
import com.appcontrol.domain.model.EventType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UsageStatsIntegrationTest {

    private lateinit var context: Context
    private lateinit var provider: UsageStatsProvider
    private lateinit var ownPackage: String

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        provider = UsageStatsProviderImpl(context)
        ownPackage = context.packageName
    }

    private fun hasUsageAccess(): Boolean {
        @Suppress("DEPRECATION")
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                ownPackage
            )
        } else {
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                ownPackage
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    @Test
    fun getRecentActivity_returnsNonNegativeDuration() = runBlocking {
        val duration = provider.getRecentActivity(ownPackage, 0L)
        assertTrue("Duration must be >= 0, was $duration", duration >= 0L)
    }

    @Test
    fun isAppActive_isConsistentWithPermissionState() = runBlocking {
        val active = provider.isAppActive(ownPackage, 0L)
        if (!hasUsageAccess()) {
            assertFalse(active)
        } else {
            // With access, exact activity depends on device usage; only assert it does not throw.
            assertTrue(true)
        }
    }

    @Test
    fun getAllRecentActivity_returnsMapOfOwnPackageIfPresent() = runBlocking {
        val map = provider.getAllRecentActivity(0L)
        map.forEach { (packageName, duration) ->
            assertTrue(packageName.isNotBlank())
            assertTrue(duration >= 0L)
        }
    }

    @Test
    fun getRecentActivityEvents_neverThrowsAndReturnsValidEvents() = runBlocking {
        val now = System.currentTimeMillis()
        val events = provider.getRecentActivityEvents(now - 24 * 60 * 60 * 1000L, now)

        events.forEach { event ->
            assertEquals(EventType.APP_ACTIVE, event.eventType)
            assertTrue(event.packageName.isNotBlank())
            assertTrue(event.timestamp in (now - 24 * 60 * 60 * 1000L)..now)
        }
    }

    @Test
    fun getRecentActivityEvents_withoutPermission_returnsEmpty() = runBlocking {
        if (!hasUsageAccess()) {
            val events = provider.getRecentActivityEvents(0L, System.currentTimeMillis())
            assertTrue(events.isEmpty())
        } else {
            // Permission granted; query must not throw.
            provider.getRecentActivityEvents(0L, System.currentTimeMillis())
            assertTrue(true)
        }
    }

    @Test
    fun unknownPackage_returnsZeroActivity() = runBlocking {
        val duration = provider.getRecentActivity("com.nonexistent.app123", 0L)
        assertEquals(0L, duration)
    }
}