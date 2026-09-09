package com.appcontrol

import com.appcontrol.core.time.ScheduleCalculator
import com.appcontrol.domain.model.NightSchedule
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleCalculatorTest {

    private val defaultSchedule = NightSchedule()

    @Test
    fun isWithinWindow_sameDayWindow_inclusiveStartExclusiveEnd() {
        assertTrue(ScheduleCalculator.isWithinWindow(12 * 60, 9 * 60, 17 * 60))
        assertTrue(ScheduleCalculator.isWithinWindow(9 * 60, 9 * 60, 17 * 60))
        assertTrue(ScheduleCalculator.isWithinWindow(16 * 60 + 59, 9 * 60, 17 * 60))
        assertFalse(ScheduleCalculator.isWithinWindow(17 * 60, 9 * 60, 17 * 60))
        assertFalse(ScheduleCalculator.isWithinWindow(8 * 60 + 59, 9 * 60, 17 * 60))
    }

    @Test
    fun isWithinWindow_overnightWindow_wrapsPastMidnight() {
        assertTrue(ScheduleCalculator.isWithinWindow(22 * 60, 22 * 60, 7 * 60))
        assertTrue(ScheduleCalculator.isWithinWindow(0 * 60, 22 * 60, 7 * 60))
        assertTrue(ScheduleCalculator.isWithinWindow(6 * 60 + 59, 22 * 60, 7 * 60))
        assertFalse(ScheduleCalculator.isWithinWindow(7 * 60, 22 * 60, 7 * 60))
        assertFalse(ScheduleCalculator.isWithinWindow(21 * 60 + 59, 22 * 60, 7 * 60))
    }

    @Test
    fun isWithinWindow_equalBounds_returnsFalse() {
        assertFalse(ScheduleCalculator.isWithinWindow(10 * 60, 10 * 60, 10 * 60))
    }

    @Test
    fun isWithinWindow_sizeBasedWindow_boundaryAtEndExcluded() {
        assertTrue(ScheduleCalculator.isWithinWindow(10 * 60, 10 * 60, 30))
        assertTrue(ScheduleCalculator.isWithinWindow(10 * 60 + 29, 10 * 60, 30))
        assertFalse(ScheduleCalculator.isWithinWindow(10 * 60 + 30, 10 * 60, 30))
        assertFalse(ScheduleCalculator.isWithinWindow(9 * 60 + 59, 10 * 60, 30))
    }

    @Test
    fun isNightPeriod_currentTimeInSchedule_returnsTrue() {
        val now = Calendar.getInstance()
        val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val start = mod(currentMinutes - 10)
        val end = mod(currentMinutes + 10)
        val schedule = NightSchedule(
            enabled = true,
            startHour = start / 60,
            startMinute = start % 60,
            endHour = end / 60,
            endMinute = end % 60
        )

        assertTrue(ScheduleCalculator.isNightPeriod(schedule))
    }

    @Test
    fun isNightPeriod_currentTimeOutsideSchedule_returnsFalse() {
        val now = Calendar.getInstance()
        val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val start = mod(currentMinutes + 10)
        val end = mod(currentMinutes + 20)
        val schedule = NightSchedule(
            enabled = true,
            startHour = start / 60,
            startMinute = start % 60,
            endHour = end / 60,
            endMinute = end % 60
        )

        assertFalse(ScheduleCalculator.isNightPeriod(schedule))
    }

    @Test
    fun isNightPeriod_disabledSchedule_returnsFalse() {
        assertFalse(ScheduleCalculator.isNightPeriod(NightSchedule(enabled = false)))
    }

    @Test
    fun nextCheckTime_byMillis_addsInterval() {
        assertEquals(1_000L, ScheduleCalculator.nextCheckTime(1_000L, now = 0L))
        assertEquals(61_000L, ScheduleCalculator.nextCheckTime(60_000L, now = 1_000L))
    }

    @Test
    fun nextCheckTime_byMinutes_convertsAndAdds() {
        assertEquals(15 * 60_000L, ScheduleCalculator.nextCheckTime(15, now = 0L))
        assertEquals(61_000L + 60_000L, ScheduleCalculator.nextCheckTime(1, now = 61_000L))
    }

    @Test
    fun checksPerDay_intervalDividesDay() {
        assertEquals(48, ScheduleCalculator.checksPerDay(30))
        assertEquals(96, ScheduleCalculator.checksPerDay(15))
        assertEquals(24, ScheduleCalculator.checksPerDay(60))
    }

    @Test
    fun checksPerDay_usesScheduleInterval() {
        assertEquals(48, ScheduleCalculator.checksPerDay(NightSchedule(intervalMinutes = 30)))
        assertEquals(6, ScheduleCalculator.checksPerDay(NightSchedule(intervalMinutes = 240)))
    }

    @Test
    fun checksPerDay_nonPositiveInterval_returnsZero() {
        assertEquals(0, ScheduleCalculator.checksPerDay(0))
        assertEquals(0, ScheduleCalculator.checksPerDay(-5))
    }

    @Test
    fun checksPerDay_intervalLongerThanDay_returnsOne() {
        assertEquals(1, ScheduleCalculator.checksPerDay(24 * 60))
    }

    @Test
    fun nextNightStart_afterScheduleToday_givesTomorrow() {
        val now = atToday(12, 0)
        val result = ScheduleCalculator.nextNightStart(defaultSchedule, now)

        val cal = Calendar.getInstance().apply { timeInMillis = result }
        assertEquals(22, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, cal.get(Calendar.MINUTE))
        assertTrue(result > now)
    }

    @Test
    fun nextNightStart_beforeScheduleToday_givesToday() {
        val now = atToday(23, 0)
        val result = ScheduleCalculator.nextNightStart(defaultSchedule, now)

        val cal = Calendar.getInstance().apply { timeInMillis = result }
        assertEquals(22, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, cal.get(Calendar.MINUTE))
        assertTrue(result > now)
    }

    @Test
    fun nextNightEnd_afterToday_givesTomorrow() {
        val now = atToday(12, 0)
        val result = ScheduleCalculator.nextNightEnd(defaultSchedule, now)

        val cal = Calendar.getInstance().apply { timeInMillis = result }
        assertEquals(7, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, cal.get(Calendar.MINUTE))
        assertTrue(result > now)
    }

    private fun atToday(hour: Int, minute: Int): Long =
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun mod(minutes: Int): Int = ((minutes % 1440) + 1440) % 1440
}