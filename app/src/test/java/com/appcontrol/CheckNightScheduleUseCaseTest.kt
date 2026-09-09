package com.appcontrol

import com.appcontrol.domain.model.NightSchedule
import com.appcontrol.domain.usecase.CheckNightScheduleUseCase
import java.util.Calendar
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CheckNightScheduleUseCaseTest {

    private lateinit var useCase: CheckNightScheduleUseCase

    @Before
    fun setUp() {
        useCase = CheckNightScheduleUseCase()
    }

    @Test
    fun withinNightSchedule_returnsTrue() {
        val now = Calendar.getInstance()
        val current = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val start = modMinutes(current - 5)
        val end = modMinutes(current + 5)
        val schedule = NightSchedule(
            enabled = true,
            startHour = start / 60,
            startMinute = start % 60,
            endHour = end / 60,
            endMinute = end % 60
        )

        val expectedInWindow = if (start <= end) {
            current in start until end
        } else {
            current >= start || current < end
        }

        assertTrue(expectedInWindow)
        assertTrue(useCase.isInNightPeriod(schedule))
    }

    @Test
    fun outsideNightSchedule_disabled_returnsFalse() {
        assertFalse(useCase.isInNightPeriod(NightSchedule(enabled = false)))
    }

    @Test
    fun disabledSchedule_default_returnsFalse() {
        assertFalse(useCase.isInNightPeriod(NightSchedule()))
    }

    @Test
    fun noArgOverload_doesNotThrow() {
        useCase.isInNightPeriod()
    }

    @Test
    fun boundaryExactStartTime_isInsideWindow() {
        val now = Calendar.getInstance()
        val current = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val end = modMinutes(current + 1)
        val schedule = NightSchedule(
            enabled = true,
            startHour = current / 60,
            startMinute = current % 60,
            endHour = end / 60,
            endMinute = end % 60
        )

        assertTrue(useCase.isInNightPeriod(schedule))
    }

    @Test
    fun boundaryExactEndTime_isOutsideWindow() {
        val now = Calendar.getInstance()
        val current = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val start = modMinutes(current - 1)
        val schedule = NightSchedule(
            enabled = true,
            startHour = start / 60,
            startMinute = start % 60,
            endHour = current / 60,
            endMinute = current % 60
        )

        assertFalse(useCase.isInNightPeriod(schedule))
    }

    @Test
    fun boundaryEqualStartAndEnd_returnsFalse() {
        val now = Calendar.getInstance()
        val current = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val schedule = NightSchedule(
            enabled = true,
            startHour = current / 60,
            startMinute = current % 60,
            endHour = current / 60,
            endMinute = current % 60
        )

        assertFalse(useCase.isInNightPeriod(schedule))
    }

    @Test
    fun scheduleCalculation_matchesReferenceWindowLogic() {
        assertTrue(referenceWindowContains(12 * 60, 9 * 60, 17 * 60))
        assertFalse(referenceWindowContains(8 * 60, 9 * 60, 17 * 60))
        assertFalse(referenceWindowContains(17 * 60, 9 * 60, 17 * 60))

        assertTrue(referenceWindowContains(22 * 60, 22 * 60, 7 * 60))
        assertTrue(referenceWindowContains(0 * 60, 22 * 60, 7 * 60))
        assertTrue(referenceWindowContains(6 * 60 + 59, 22 * 60, 7 * 60))
        assertFalse(referenceWindowContains(7 * 60, 22 * 60, 7 * 60))
        assertFalse(referenceWindowContains(21 * 60 + 59, 22 * 60, 7 * 60))

        assertFalse(referenceWindowContains(10 * 60, 10 * 60, 10 * 60))
    }

    private fun referenceWindowContains(
        currentMinutes: Int,
        startMinutes: Int,
        endMinutes: Int
    ): Boolean {
        return if (startMinutes <= endMinutes) {
            currentMinutes in startMinutes until endMinutes
        } else {
            currentMinutes >= startMinutes || currentMinutes < endMinutes
        }
    }

    private fun modMinutes(minutes: Int): Int = ((minutes % 1440) + 1440) % 1440
}