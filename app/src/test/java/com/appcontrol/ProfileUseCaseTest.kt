package com.appcontrol

import com.appcontrol.core.time.TimeUtils
import com.appcontrol.domain.model.Profile
import com.appcontrol.domain.usecase.ManageProfileUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ProfileUseCaseTest {

    private lateinit var repository: FakeProfileRepository
    private lateinit var useCase: ManageProfileUseCase

    @Before
    fun setUp() {
        repository = FakeProfileRepository()
        useCase = ManageProfileUseCase(repository)
    }

    @Test
    fun createProfileWithValidData_returnsGeneratedId() = runBlocking {
        val profile = Profile(
            name = "Night monitor",
            enabled = true,
            scheduleEnabled = true,
            startTime = "22:00",
            endTime = "07:00",
            monitoringInterval = 30 * 60_000L,
            notificationEnabled = true,
            appPackages = listOf("com.android.chrome", "com.android.settings")
        )

        val id = useCase.create(profile)

        assertTrue(id > 0L)
        val saved = repository.profiles[id]
        assertNotNull(saved)
        assertEquals("Night monitor", saved?.name)
        assertEquals("22:00", saved?.startTime)
        assertEquals(listOf("com.android.chrome", "com.android.settings"), saved?.appPackages)
    }

    @Test
    fun createProfileWithExplicitId_keepsId() = runBlocking {
        val profile = Profile(id = 42L, name = "Fixed id")
        val id = useCase.create(profile)
        assertEquals(42L, id)
        assertEquals(42L, repository.profiles[42L]?.id)
    }

    @Test
    fun deleteProfile_removesProfileAndReturnsTrue() = runBlocking {
        val id = useCase.create(Profile(name = "Gaming"))
        assertTrue(useCase.delete(id))
        assertNull(repository.profiles[id])
    }

    @Test
    fun deleteNonExistentProfile_returnsFalse() = runBlocking {
        assertFalse(useCase.delete(999L))
    }

    @Test
    fun setActiveProfile_existingProfile_returnsTrue() = runBlocking {
        val id = useCase.create(Profile(name = "Office"))
        assertTrue(useCase.setActiveProfile(id))
        assertEquals(id, repository.activeProfileId)
    }

    @Test
    fun setActiveProfile_nonExistentProfile_returnsFalse() = runBlocking {
        assertFalse(useCase.setActiveProfile(12345L))
        assertNull(repository.activeProfileId)
    }

    @Test
    fun setActiveProfile_null_clearsActiveProfile() = runBlocking {
        val id = useCase.create(Profile(name = "Night"))
        useCase.setActiveProfile(id)
        assertTrue(useCase.setActiveProfile(null))
        assertNull(repository.activeProfileId)
    }

    @Test
    fun getActiveProfile_returnsActiveProfile() = runBlocking {
        val id = useCase.create(Profile(name = "Default"))
        useCase.setActiveProfile(id)
        assertEquals("Default", useCase.getActiveProfile()?.name)
    }

    @Test
    fun getActiveProfile_withoutActive_returnsNull() = runBlocking {
        assertNull(useCase.getActiveProfile())
    }

    @Test
    fun updateProfile_changesFields() = runBlocking {
        val id = useCase.create(Profile(name = "Original"))
        val updated = repository.profiles[id]!!.copy(name = "Renamed", notificationEnabled = false)
        assertTrue(useCase.update(updated))
        assertEquals("Renamed", repository.profiles[id]?.name)
        assertEquals(false, repository.profiles[id]?.notificationEnabled)
    }

    @Test
    fun updateProfile_nonExistent_returnsFalse() = runBlocking {
        val ghost = Profile(id = 555L, name = "Ghost")
        assertFalse(useCase.update(ghost))
    }

    @Test
    fun getAll_returnsAllProfiles() = runBlocking {
        useCase.create(Profile(name = "B"))
        useCase.create(Profile(name = "A"))
        assertEquals(listOf("A", "B"), useCase.getAll().map { it.name })
    }

    @Test
    fun scheduleValidation_validSchedule_passes() {
        val profile = Profile(
            name = "Valid",
            scheduleEnabled = true,
            startTime = "22:00",
            endTime = "07:00"
        )
        assertTrue(validateSchedule(profile))
    }

    @Test
    fun scheduleValidation_missingTimes_fails() {
        val noStart = Profile(name = "No start", scheduleEnabled = true, startTime = null, endTime = "07:00")
        val noEnd = Profile(name = "No end", scheduleEnabled = true, startTime = "22:00", endTime = null)
        assertFalse(validateSchedule(noStart))
        assertFalse(validateSchedule(noEnd))
    }

    @Test
    fun scheduleValidation_equalStartEnd_fails() {
        val same = Profile(name = "Same", scheduleEnabled = true, startTime = "09:00", endTime = "09:00")
        assertFalse(validateSchedule(same))
    }

    @Test
    fun scheduleValidation_disabledSchedule_ignoresTimes() {
        val disabled = Profile(name = "Disabled", scheduleEnabled = false, startTime = null, endTime = null)
        assertTrue(validateSchedule(disabled))
    }

    private fun validateSchedule(profile: Profile): Boolean {
        if (!profile.scheduleEnabled) return true
        val start = profile.startTime ?: return false
        val end = profile.endTime ?: return false
        return parseMinutes(start) != parseMinutes(end)
    }

    private fun parseMinutes(time: String): Int {
        val parts = time.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: return -1
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: return -1
        return TimeUtils.minutesOfDay(hour, minute)
    }
}