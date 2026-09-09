package com.appcontrol

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.appcontrol.data.system.PackageManagerProvider
import com.appcontrol.data.system.PackageManagerProviderImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PackageManagerIntegrationTest {

    private lateinit var context: Context
    private lateinit var provider: PackageManagerProvider
    private lateinit var ownPackage: String

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        provider = PackageManagerProviderImpl(context)
        ownPackage = context.packageName
    }

    @Test
    fun getInstalledApplications_returnsOwnApp() = runBlocking {
        val apps = provider.getInstalledApplications()

        assertTrue(apps.any { it.packageName == ownPackage })
        val own = apps.first { it.packageName == ownPackage }
        assertTrue(own.label.isNotBlank())
        assertTrue(own.versionCode > 0L)
    }

    @Test
    fun getInstalledApplications_sortedByLabel() = runBlocking {
        val apps = provider.getInstalledApplications()
        val labels = apps.map { it.label.lowercase() }
        assertEquals(labels.sorted(), labels)
        assertTrue(apps.isNotEmpty())
    }

    @Test
    fun getAppInfo_returnsOwnAppFields() = runBlocking {
        val info = provider.getAppInfo(ownPackage)

        assertNotNull(info)
        assertEquals(ownPackage, info?.packageName)
        assertTrue(info?.label?.isNotBlank() == true)
        assertTrue(info?.versionCode?.let { it > 0L } == true)
        assertFalse(info?.firstInstallTime == info?.lastUpdateTime && info?.firstInstallTime == 0L)
    }

    @Test
    fun getAppInfo_unknownPackage_returnsNull() = runBlocking {
        assertNull(provider.getAppInfo("com.nonexistent.app123"))
    }

    @Test
    fun getAppLabel_unknownPackage_fallsBackToPackageName() = runBlocking {
        assertEquals("com.nonexistent.app123", provider.getAppLabel("com.nonexistent.app123"))
    }

    @Test
    fun getAppLabel_ownPackage_isNotBlank() = runBlocking {
        assertTrue(provider.getAppLabel(ownPackage).isNotBlank())
    }

    @Test
    fun isSystemApp_unknownPackage_returnsFalse() = runBlocking {
        assertFalse(provider.isSystemApp("com.nonexistent.app123"))
    }

    @Test
    fun isSystemApp_ownPackage_returnsValueWithoutThrowing() = runBlocking {
        provider.isSystemApp(ownPackage)
        assertTrue(true)
    }

    @Test
    fun getAppIcon_ownPackage_returnsDrawable() = runBlocking {
        val icon = provider.getAppIcon(ownPackage)
        assertNotNull(icon)
    }

    @Test
    fun getAppIcon_unknownPackage_returnsNull() = runBlocking {
        assertNull(provider.getAppIcon("com.nonexistent.app123"))
    }

    @Test
    fun installedApps_hasDistinctPackages() = runBlocking {
        val apps = provider.getInstalledApplications()
        val packageNames = apps.map { it.packageName }
        assertEquals(packageNames.size, packageNames.distinct().size)
    }
}