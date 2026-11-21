package com.jotasoft.edensavesync

import com.jotasoft.edensavesync.data.scheduler.ScheduledSyncScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ScheduledSyncCalculationTest {

    @Test
    fun calculateInitialDelay_whenTargetIsLaterToday_returnsDifference() {
        val now = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        // Target: 14:30 (4 horas y 30 minutos = 270 minutos = 16.200.000 ms)
        val delay = ScheduledSyncScheduler.calculateInitialDelayMs(14, 30, nowMs = now)
        assertEquals(16_200_000L, delay)
    }

    @Test
    fun calculateInitialDelay_whenTargetPassedToday_schedulesForTomorrow() {
        val now = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 20)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        // Target: 04:00 de mañana (8 horas restantes = 8 * 3600 * 1000 = 28.800.000 ms)
        val delay = ScheduledSyncScheduler.calculateInitialDelayMs(4, 0, nowMs = now)
        assertEquals(28_800_000L, delay)
    }

    @Test
    fun calculateInitialDelay_whenTargetIsNow_schedulesFor24HoursLater() {
        val now = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 4)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        // Target: 04:00 (mismo momento exacto -> pasa a mañana = 24 horas = 86.400.000 ms)
        val delay = ScheduledSyncScheduler.calculateInitialDelayMs(4, 0, nowMs = now)
        assertEquals(86_400_000L, delay)
    }

    @Test
    fun isTimeMatchingSchedule_whenExactMatch_returnsTrue() {
        val now = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 14)
            set(Calendar.MINUTE, 30)
        }.timeInMillis

        val matches = ScheduledSyncScheduler.isTimeMatchingSchedule(14, 30, nowMs = now)
        assertTrue(matches)
    }

    @Test
    fun isTimeMatchingSchedule_whenWithinTolerance_returnsTrue() {
        val now = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 14)
            set(Calendar.MINUTE, 38) // +8 minutos
        }.timeInMillis

        val matches = ScheduledSyncScheduler.isTimeMatchingSchedule(14, 30, nowMs = now, toleranceMinutes = 15)
        assertTrue(matches)
    }

    @Test
    fun isTimeMatchingSchedule_whenOutsideTolerance_returnsFalse() {
        val now = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 15)
            set(Calendar.MINUTE, 10) // 40 minutos de diferencia (alarma obsoleta)
        }.timeInMillis

        val matches = ScheduledSyncScheduler.isTimeMatchingSchedule(14, 30, nowMs = now, toleranceMinutes = 15)
        assertFalse(matches)
    }

    @Test
    fun isTimeMatchingSchedule_acrossMidnightBoundary_returnsTrue() {
        // Target 00:05, now es 23:55 (10 minutos de diferencia a través de medianoche)
        val now = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 55)
        }.timeInMillis

        val matches = ScheduledSyncScheduler.isTimeMatchingSchedule(0, 5, nowMs = now, toleranceMinutes = 15)
        assertTrue(matches)
    }

    @Test
    fun isTimeMatchingSchedule_acrossMidnightBoundary_whenOutsideTolerance_returnsFalse() {
        // Target 00:05, now es 23:30 (35 minutos de diferencia a través de medianoche)
        val now = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 30)
        }.timeInMillis

        val matches = ScheduledSyncScheduler.isTimeMatchingSchedule(0, 5, nowMs = now, toleranceMinutes = 15)
        assertFalse(matches)
    }
}
