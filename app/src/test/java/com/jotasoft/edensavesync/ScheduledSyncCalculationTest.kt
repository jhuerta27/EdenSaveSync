package com.jotasoft.edensavesync

import com.jotasoft.edensavesync.data.scheduler.ScheduledSyncScheduler
import com.jotasoft.edensavesync.data.storage.AppPreferences
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

        // Modo AUTOMATIC con más de 30 segundos desde el último chequeo -> true
        assertTrue(
            ScheduledSyncScheduler.shouldRunEventAutoSync(
                isEnabled = true,
                mode = BackgroundSyncMode.AUTOMATIC,
                nowMs = nowMs,
                lastCheckMs = nowMs - 35_000L
            )
        )

        // Modo AUTOMATIC dentro de la ventana de 30 segundos -> false (throttled)
        assertFalse(
            ScheduledSyncScheduler.shouldRunEventAutoSync(
                isEnabled = true,
                mode = BackgroundSyncMode.AUTOMATIC,
                nowMs = nowMs,
                lastCheckMs = nowMs - 10_000L
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
    fun calculateEventAutoSyncDelayMs_schedulesRemainingDelayInsteadOfDroppingEvent() {
        val nowMs = 1_700_000_200_000L

        // Primera ejecución o fuera de la ventana de 30s -> 0ms (inmediato)
        assertEquals(
            0L,
            ScheduledSyncScheduler.calculateEventAutoSyncDelayMs(
                isEnabled = true,
                mode = BackgroundSyncMode.AUTOMATIC,
                nowMs = nowMs,
                lastCheckMs = 0L
            )
        )
        assertEquals(
            0L,
            ScheduledSyncScheduler.calculateEventAutoSyncDelayMs(
                isEnabled = true,
                mode = BackgroundSyncMode.AUTOMATIC,
                nowMs = nowMs,
                lastCheckMs = nowMs - 35_000L
            )
        )

        // Dentro de la ventana de 30s (10s transcurridos) -> programa el tiempo restante (20s)
        assertEquals(
            20_000L,
            ScheduledSyncScheduler.calculateEventAutoSyncDelayMs(
                isEnabled = true,
                mode = BackgroundSyncMode.AUTOMATIC,
                nowMs = nowMs,
                lastCheckMs = nowMs - 10_000L
            )
        )

        // Modo SCHEDULED o desactivado -> null (no encola trabajo)
        assertEquals(
            null,
            ScheduledSyncScheduler.calculateEventAutoSyncDelayMs(
                isEnabled = true,
                mode = BackgroundSyncMode.SCHEDULED,
                nowMs = nowMs,
                lastCheckMs = 0L
            )
        )
        assertEquals(
            null,
            ScheduledSyncScheduler.calculateEventAutoSyncDelayMs(
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

    @Test
    fun shouldShowSyncNotification_inAutoMode_deduplicatesRepeatedErrorsWithinCooldown() {
        val nowMs = 1_700_050_000_000L
        val shizukuError = SyncSummaryResult(
            uploadedCount = 0,
            downloadedCount = 0,
            skippedCount = 3,
            errors = listOf("Peanuts Native: Shizuku no está iniciado")
        )
        val signature = ScheduledSyncScheduler.errorSignatureFor(shizukuError.errors)

        // Primera vez que ocurre el error -> sí notifica
        assertTrue(
            ScheduledSyncScheduler.shouldShowSyncNotification(
                mode = BackgroundSyncMode.AUTOMATIC,
                notificationsEnabled = true,
                summary = shizukuError,
                lastNotifiedErrorSignature = "",
                lastErrorNotifiedAtMs = 0L,
                nowMs = nowMs
            )
        )

        // Mismo error dentro de las 6 horas -> silenciado para evitar spam
        assertFalse(
            ScheduledSyncScheduler.shouldShowSyncNotification(
                mode = BackgroundSyncMode.AUTOMATIC,
                notificationsEnabled = true,
                summary = shizukuError,
                lastNotifiedErrorSignature = signature,
                lastErrorNotifiedAtMs = nowMs - (30 * 60_000L),
                nowMs = nowMs
            )
        )

        // Mismo error una vez superado el cooldown de 6 horas -> vuelve a notificar
        assertTrue(
            ScheduledSyncScheduler.shouldShowSyncNotification(
                mode = BackgroundSyncMode.AUTOMATIC,
                notificationsEnabled = true,
                summary = shizukuError,
                lastNotifiedErrorSignature = signature,
                lastErrorNotifiedAtMs = nowMs - ScheduledSyncScheduler.AUTO_ERROR_NOTIFICATION_COOLDOWN_MS,
                nowMs = nowMs
            )
        )

        // Error distinto dentro de las 6 horas -> sí notifica
        val otherError = shizukuError.copy(errors = listOf("Google Drive: Network timeout"))
        assertTrue(
            ScheduledSyncScheduler.shouldShowSyncNotification(
                mode = BackgroundSyncMode.AUTOMATIC,
                notificationsEnabled = true,
                summary = otherError,
                lastNotifiedErrorSignature = signature,
                lastErrorNotifiedAtMs = nowMs - (5 * 60_000L),
                nowMs = nowMs
            )
        )

        // Si además hubo cambios subidos/descargados junto con un error repetido -> sí notifica
        val partialSuccess = shizukuError.copy(uploadedCount = 1)
        assertTrue(
            ScheduledSyncScheduler.shouldShowSyncNotification(
                mode = BackgroundSyncMode.AUTOMATIC,
                notificationsEnabled = true,
                summary = partialSuccess,
                lastNotifiedErrorSignature = signature,
                lastErrorNotifiedAtMs = nowMs - (5 * 60_000L),
                nowMs = nowMs
            )
        )
    }

    @Test
    fun resolveBackgroundSyncMode_preservesScheduledForExistingUsersAndDefaultsAutoForNewUsers() {
        // Usuario existente de v1.0.2 con sync programada ya activa (sin lastAutoSyncCheckMs) -> SCHEDULED
        assertEquals(
            BackgroundSyncMode.SCHEDULED,
            AppPreferences.resolveBackgroundSyncMode(
                storedMode = null,
                isScheduledSyncEnabled = true,
                lastAutoSyncCheckMs = 0L
            )
        )

        // Usuario que ya tenía ejecuciones de auto-sync previas sin clave guardada -> AUTOMATIC
        assertEquals(
            BackgroundSyncMode.AUTOMATIC,
            AppPreferences.resolveBackgroundSyncMode(
                storedMode = null,
                isScheduledSyncEnabled = true,
                lastAutoSyncCheckMs = 1_700_000_000_000L
            )
        )

        // Usuario nuevo (sync desactivada y sin clave) -> AUTOMATIC por defecto
        assertEquals(
            BackgroundSyncMode.AUTOMATIC,
            AppPreferences.resolveBackgroundSyncMode(storedMode = null, isScheduledSyncEnabled = false)
        )

        // Valor explícitamente guardado siempre tiene prioridad
        assertEquals(
            BackgroundSyncMode.AUTOMATIC,
            AppPreferences.resolveBackgroundSyncMode(
                storedMode = BackgroundSyncMode.AUTOMATIC.name,
                isScheduledSyncEnabled = true
            )
        )
        assertEquals(
            BackgroundSyncMode.SCHEDULED,
            AppPreferences.resolveBackgroundSyncMode(
                storedMode = BackgroundSyncMode.SCHEDULED.name,
                isScheduledSyncEnabled = false
            )
        )
    }

    @Test
    fun calculateAutoPulseIntervalMs_andWakeUp_adaptToDozeIdleMode() {
        // Fuera de Doze (uso normal / recién bloqueado) -> 3 min con RTC_WAKEUP
        assertEquals(
            ScheduledSyncScheduler.AUTO_PULSE_INTERVAL_MS,
            ScheduledSyncScheduler.calculateAutoPulseIntervalMs(isDeviceIdleMode = false)
        )
        assertTrue(ScheduledSyncScheduler.shouldWakeUpForAutoPulse(isDeviceIdleMode = false))

        // En Doze (reposo profundo / nocturno) -> 15 min sin WAKEUP
        assertEquals(
            ScheduledSyncScheduler.AUTO_PULSE_IDLE_INTERVAL_MS,
            ScheduledSyncScheduler.calculateAutoPulseIntervalMs(isDeviceIdleMode = true)
        )
        assertFalse(ScheduledSyncScheduler.shouldWakeUpForAutoPulse(isDeviceIdleMode = true))
    }
}
