package com.jotasoft.edensavesync

import com.jotasoft.edensavesync.data.scheduler.ScheduledSyncScheduler
import com.jotasoft.edensavesync.domain.model.BackgroundSyncMode
import com.jotasoft.edensavesync.domain.model.LocalSaveGame
import com.jotasoft.edensavesync.domain.model.SyncSummaryResult
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

        val delay = ScheduledSyncScheduler.calculateInitialDelayMs(4, 0, nowMs = now)
        assertEquals(86_400_000L, delay)
    }

    @Test
    fun isTimeMatchingSchedule_whenExactMatch_returnsTrue() {
        val now = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 14)
            set(Calendar.MINUTE, 30)
        }.timeInMillis

        assertTrue(ScheduledSyncScheduler.isTimeMatchingSchedule(14, 30, nowMs = now))
    }

    @Test
    fun isTimeMatchingSchedule_whenWithinTolerance_returnsTrue() {
        val now = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 14)
            set(Calendar.MINUTE, 38)
        }.timeInMillis

        assertTrue(
            ScheduledSyncScheduler.isTimeMatchingSchedule(14, 30, nowMs = now, toleranceMinutes = 15)
        )
    }

    @Test
    fun isTimeMatchingSchedule_whenOutsideTolerance_returnsFalse() {
        val now = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 15)
            set(Calendar.MINUTE, 10)
        }.timeInMillis

        assertFalse(
            ScheduledSyncScheduler.isTimeMatchingSchedule(14, 30, nowMs = now, toleranceMinutes = 15)
        )
    }

    @Test
    fun isTimeMatchingSchedule_acrossMidnightBoundary_returnsTrue() {
        val now = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 55)
        }.timeInMillis

        assertTrue(
            ScheduledSyncScheduler.isTimeMatchingSchedule(0, 5, nowMs = now, toleranceMinutes = 15)
        )
    }

    @Test
    fun isTimeMatchingSchedule_acrossMidnightBoundary_whenOutsideTolerance_returnsFalse() {
        val now = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 30)
        }.timeInMillis

        assertFalse(
            ScheduledSyncScheduler.isTimeMatchingSchedule(0, 5, nowMs = now, toleranceMinutes = 15)
        )
    }

    @Test
    fun isLocalSaveStableForAutoUpload_whenModifiedRecently_returnsFalse() {
        val nowMs = 1_700_000_010_000L
        val save = LocalSaveGame(
            titleId = "0100F2C0115B6000",
            userProfileId = "profile",
            lastModifiedMs = 1_700_000_000_000L, // Hace solo 10s (< 20s cooldown)
            totalSizeBytes = 4096L,
            fileCount = 3,
            contentHash = "hash_abc"
        )

        assertFalse(
            ScheduledSyncScheduler.isLocalSaveStableForAutoUpload(
                initial = save,
                rechecked = save,
                nowMs = nowMs
            )
        )
    }

    @Test
    fun isLocalSaveStableForAutoUpload_whenHashChangedBetweenReads_returnsFalse() {
        val nowMs = 1_700_000_030_000L
        val initial = LocalSaveGame(
            titleId = "0100F2C0115B6000",
            userProfileId = "profile",
            lastModifiedMs = 1_700_000_000_000L,
            totalSizeBytes = 4096L,
            fileCount = 3,
            contentHash = "hash_writing_1"
        )
        val rechecked = initial.copy(contentHash = "hash_writing_2", totalSizeBytes = 8192L)

        assertFalse(
            ScheduledSyncScheduler.isLocalSaveStableForAutoUpload(
                initial = initial,
                rechecked = rechecked,
                nowMs = nowMs
            )
        )
    }

    @Test
    fun isLocalSaveStableForAutoUpload_whenStableAndCooldownElapsed_returnsTrue() {
        val nowMs = 1_700_000_025_000L
        val save = LocalSaveGame(
            titleId = "0100F2C0115B6000",
            userProfileId = "profile",
            lastModifiedMs = 1_700_000_000_000L, // Hace 25s (>= 20s cooldown)
            totalSizeBytes = 4096L,
            fileCount = 3,
            contentHash = "hash_stable"
        )

        assertTrue(
            ScheduledSyncScheduler.isLocalSaveStableForAutoUpload(
                initial = save,
                rechecked = save,
                nowMs = nowMs
            )
        )
    }

    @Test
    fun isLocalSaveStableForAutoUpload_whenZeroTimestampAndHashConfirmed_returnsTrue() {
        val save = LocalSaveGame(
            titleId = "010041100D536000",
            userProfileId = "profile",
            lastModifiedMs = 0L, // Eden DocumentsProvider sin timestamp
            totalSizeBytes = 50_000_000L,
            fileCount = 500,
            contentHash = "hash_subnautica_stable"
        )

        assertTrue(
            ScheduledSyncScheduler.isLocalSaveStableForAutoUpload(
                initial = save,
                rechecked = save,
                nowMs = 1_700_000_000_000L
            )
        )
    }

    @Test
    fun shouldRunEventAutoSync_respectsModeAndThrottleInterval() {
        val nowMs = 1_700_000_200_000L

        // Modo AUTOMATIC con más de 2 minutos desde el último chequeo -> true
        assertTrue(
            ScheduledSyncScheduler.shouldRunEventAutoSync(
                isEnabled = true,
                mode = BackgroundSyncMode.AUTOMATIC,
                nowMs = nowMs,
                lastCheckMs = nowMs - 125_000L
            )
        )

        // Modo AUTOMATIC dentro de la ventana de 2 minutos -> false (throttled)
        assertFalse(
            ScheduledSyncScheduler.shouldRunEventAutoSync(
                isEnabled = true,
                mode = BackgroundSyncMode.AUTOMATIC,
                nowMs = nowMs,
                lastCheckMs = nowMs - 45_000L
            )
        )

        // Modo SCHEDULED -> false (no dispara por eventos de desbloqueo)
        assertFalse(
            ScheduledSyncScheduler.shouldRunEventAutoSync(
                isEnabled = true,
                mode = BackgroundSyncMode.SCHEDULED,
                nowMs = nowMs,
                lastCheckMs = 0L
            )
        )

        // Automatización desactivada -> false
        assertFalse(
            ScheduledSyncScheduler.shouldRunEventAutoSync(
                isEnabled = false,
                mode = BackgroundSyncMode.AUTOMATIC,
                nowMs = nowMs,
                lastCheckMs = 0L
            )
        )
    }

    @Test
    fun shouldShowSyncNotification_inAutoMode_onlyWhenChangesOrErrorsExist() {
        val noChanges = SyncSummaryResult(uploadedCount = 0, downloadedCount = 0, skippedCount = 5, errors = emptyList())
        val oneUploaded = SyncSummaryResult(uploadedCount = 1, downloadedCount = 0, skippedCount = 4, errors = emptyList())
        val oneDownloaded = SyncSummaryResult(uploadedCount = 0, downloadedCount = 1, skippedCount = 4, errors = emptyList())
        val withError = SyncSummaryResult(uploadedCount = 0, downloadedCount = 0, skippedCount = 5, errors = listOf("Network error"))

        // En modo AUTOMATIC sin cambios ni errores no debe molestar con notificación
        assertFalse(
            ScheduledSyncScheduler.shouldShowSyncNotification(
                mode = BackgroundSyncMode.AUTOMATIC,
                notificationsEnabled = true,
                summary = noChanges
            )
        )

        // En modo AUTOMATIC con subida, descarga o error sí notifica
        assertTrue(
            ScheduledSyncScheduler.shouldShowSyncNotification(
                mode = BackgroundSyncMode.AUTOMATIC,
                notificationsEnabled = true,
                summary = oneUploaded
            )
        )
        assertTrue(
            ScheduledSyncScheduler.shouldShowSyncNotification(
                mode = BackgroundSyncMode.AUTOMATIC,
                notificationsEnabled = true,
                summary = oneDownloaded
            )
        )
        assertTrue(
            ScheduledSyncScheduler.shouldShowSyncNotification(
                mode = BackgroundSyncMode.AUTOMATIC,
                notificationsEnabled = true,
                summary = withError
            )
        )

        // En modo SCHEDULED diario sí notifica aunque todo esté al día
        assertTrue(
            ScheduledSyncScheduler.shouldShowSyncNotification(
                mode = BackgroundSyncMode.SCHEDULED,
                notificationsEnabled = true,
                summary = noChanges
            )
        )

        // Si el switch de notificaciones está apagado, nunca notifica
        assertFalse(
            ScheduledSyncScheduler.shouldShowSyncNotification(
                mode = BackgroundSyncMode.AUTOMATIC,
                notificationsEnabled = false,
                summary = oneUploaded
            )
        )
    }
}
