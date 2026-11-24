package com.jotasoft.edensavesync.data.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.jotasoft.edensavesync.data.notification.SyncNotificationManager
import com.jotasoft.edensavesync.data.storage.AppPreferences
import com.jotasoft.edensavesync.domain.model.BackgroundSyncMode
import com.jotasoft.edensavesync.domain.model.LocalSaveGame
import com.jotasoft.edensavesync.domain.model.SyncSummaryResult
import com.jotasoft.edensavesync.domain.usecase.SyncAllRecommendedUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.sync.Mutex
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.min

@Singleton
class ScheduledSyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appPreferences: AppPreferences,
    private val syncAllRecommendedUseCase: SyncAllRecommendedUseCase,
    private val syncNotificationManager: SyncNotificationManager
) {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SchedulerEntryPoint {
        fun scheduler(): ScheduledSyncScheduler
    }

    private val syncMutex = Mutex()

    companion object {
        const val WORK_NAME = "EdenScheduledSyncWork"
        const val EVENT_WORK_NAME = "EdenAutoSyncEventWork"
        const val ALARM_ACTION = "com.jotasoft.edensavesync.ACTION_SCHEDULED_SYNC"
        const val AUTO_ALARM_ACTION = "com.jotasoft.edensavesync.ACTION_AUTO_SYNC_PULSE"
        const val ALARM_REQUEST_CODE = 202611
        const val AUTO_ALARM_REQUEST_CODE = 202612
        const val AUTO_UPLOAD_COOLDOWN_MS = 20_000L
        const val AUTO_EVENT_THROTTLE_MS = 30_000L
        const val AUTO_PULSE_INTERVAL_MS = 3 * 60_000L
        const val AUTO_UNSTABLE_RETRY_DELAY_MS = 30_000L

        fun calculateTargetTimeMs(
            targetHour: Int,
            targetMinute: Int,
            nowMs: Long = System.currentTimeMillis()
        ): Long {
            val targetCalendar = Calendar.getInstance().apply {
                timeInMillis = nowMs
                set(Calendar.HOUR_OF_DAY, targetHour)
                set(Calendar.MINUTE, targetMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= nowMs) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            return targetCalendar.timeInMillis
        }

        fun calculateInitialDelayMs(
            targetHour: Int,
            targetMinute: Int,
            nowMs: Long = System.currentTimeMillis()
        ): Long = calculateTargetTimeMs(targetHour, targetMinute, nowMs) - nowMs

        fun isTimeMatchingSchedule(
            targetHour: Int,
            targetMinute: Int,
            nowMs: Long = System.currentTimeMillis(),
            toleranceMinutes: Int = 15
        ): Boolean {
            val cal = Calendar.getInstance().apply { timeInMillis = nowMs }
            val currentMinutesOfDay = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            val targetMinutesOfDay = targetHour * 60 + targetMinute
            val diff = abs(currentMinutesOfDay - targetMinutesOfDay)
            return min(diff, 1440 - diff) <= toleranceMinutes
        }

        fun isLocalSaveStableForAutoUpload(
            initial: LocalSaveGame,
            rechecked: LocalSaveGame?,
            nowMs: Long = System.currentTimeMillis(),
            cooldownMs: Long = AUTO_UPLOAD_COOLDOWN_MS
        ): Boolean {
            if (rechecked == null || rechecked.fileCount == 0 || rechecked.totalSizeBytes <= 0L) {
                return false
            }
            if (initial.contentHash.isBlank() ||
                initial.contentHash != rechecked.contentHash ||
                initial.totalSizeBytes != rechecked.totalSizeBytes
            ) {
                return false
            }
            if (rechecked.lastModifiedMs > 0L && nowMs - rechecked.lastModifiedMs < cooldownMs) {
                return false
            }
            return true
        }

        fun calculateEventAutoSyncDelayMs(
            isEnabled: Boolean,
            mode: BackgroundSyncMode,
            nowMs: Long,
            lastCheckMs: Long,
            minIntervalMs: Long = AUTO_EVENT_THROTTLE_MS
        ): Long? {
            if (!isEnabled || mode != BackgroundSyncMode.AUTOMATIC) return null
            if (lastCheckMs <= 0L) return 0L
            val elapsed = nowMs - lastCheckMs
            return if (elapsed >= minIntervalMs) {
                0L
            } else {
                (minIntervalMs - elapsed).coerceAtLeast(1_000L)
            }
        }

        fun shouldRunEventAutoSync(
            isEnabled: Boolean,
            mode: BackgroundSyncMode,
            nowMs: Long,
            lastCheckMs: Long,
            minIntervalMs: Long = AUTO_EVENT_THROTTLE_MS
        ): Boolean = calculateEventAutoSyncDelayMs(
            isEnabled = isEnabled,
            mode = mode,
            nowMs = nowMs,
            lastCheckMs = lastCheckMs,
            minIntervalMs = minIntervalMs
        ) == 0L

        fun shouldShowSyncNotification(
            mode: BackgroundSyncMode,
            notificationsEnabled: Boolean,
            summary: SyncSummaryResult
        ): Boolean {
            if (!notificationsEnabled) return false
            return when (mode) {
                BackgroundSyncMode.SCHEDULED -> true
                BackgroundSyncMode.AUTOMATIC ->
                    summary.uploadedCount > 0 || summary.downloadedCount > 0 || summary.errors.isNotEmpty()
            }
        }
    }

    fun scheduleDailySync(
        hour: Int,
        minute: Int,
        workPolicy: ExistingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE
    ) {
        if (workPolicy == ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE) {
            cancelDailySync()
        } else {
            cancelAutoPulseAlarmOnly()
        }

        val targetTimeMs = calculateTargetTimeMs(hour, minute)
        val initialDelay = (targetTimeMs - System.currentTimeMillis()).coerceAtLeast(0L)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        if (alarmManager != null) {
            val pendingIntent = buildAlarmPendingIntent()
            val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            val showIntent = PendingIntent.getActivity(
                context,
                0,
                launchIntent ?: Intent(context, ScheduledSyncReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(targetTimeMs, showIntent), pendingIntent)
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<ScheduledSyncWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            workPolicy,
            workRequest
        )
    }

    fun scheduleAutomaticSync(
        workPolicy: ExistingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE
    ) {
        if (workPolicy == ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE) {
            cancelDailySync()
        } else {
            cancelDailyAlarmOnly()
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<ScheduledSyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            workPolicy,
            workRequest
        )

        scheduleNextAutoPulseAlarm()
    }

    fun cancelDailySync() {
        cancelDailyAlarmOnly()
        cancelAutoPulseAlarmOnly()
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork(WORK_NAME)
        workManager.cancelUniqueWork(EVENT_WORK_NAME)
    }

    private fun cancelDailyAlarmOnly() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = buildAlarmPendingIntent()
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun cancelAutoPulseAlarmOnly() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = buildAutoPulsePendingIntent()
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    fun scheduleNextAutoPulseAlarm(intervalMs: Long = AUTO_PULSE_INTERVAL_MS) {
        if (!appPreferences.isScheduledSyncEnabled ||
            appPreferences.backgroundSyncMode != BackgroundSyncMode.AUTOMATIC
        ) {
            cancelAutoPulseAlarmOnly()
            return
        }
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val triggerAtMs = System.currentTimeMillis() + intervalMs
        val pendingIntent = buildAutoPulsePendingIntent()
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        if (canExact) {
            runCatching {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent)
            }.onFailure {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent)
            }
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent)
        }
    }

    fun handleAutoPulseAlarm() {
        if (!appPreferences.isScheduledSyncEnabled ||
            appPreferences.backgroundSyncMode != BackgroundSyncMode.AUTOMATIC
        ) {
            cancelAutoPulseAlarmOnly()
            return
        }
        scheduleNextAutoPulseAlarm()
        scheduleOneTimeAutoSync(delayMs = 0L, policy = ExistingWorkPolicy.KEEP)
    }

    fun enqueueAutoSyncFromEvent(nowMs: Long = System.currentTimeMillis()) {
        val delayMs = calculateEventAutoSyncDelayMs(
            isEnabled = appPreferences.isScheduledSyncEnabled,
            mode = appPreferences.backgroundSyncMode,
            nowMs = nowMs,
            lastCheckMs = appPreferences.lastAutoSyncCheckMs
        ) ?: return

        scheduleOneTimeAutoSync(
            delayMs = delayMs,
            policy = ExistingWorkPolicy.KEEP
        )
    }

    private fun scheduleOneTimeAutoSync(
        delayMs: Long = 0L,
        policy: ExistingWorkPolicy = ExistingWorkPolicy.KEEP
    ) {
        if (!appPreferences.isScheduledSyncEnabled ||
            appPreferences.backgroundSyncMode != BackgroundSyncMode.AUTOMATIC
        ) {
            return
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val requestBuilder = OneTimeWorkRequestBuilder<ScheduledSyncWorker>()
            .setConstraints(constraints)
        if (delayMs > 0L) {
            requestBuilder.setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
        } else {
            requestBuilder.setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
        }

        WorkManager.getInstance(context).enqueueUniqueWork(
            EVENT_WORK_NAME,
            policy,
            requestBuilder.build()
        )
    }

    /**
     * Asegura que la programación activa exista sin cancelar Workers en curso ni reiniciar
     * temporizadores. Debe usarse al arrancar el proceso (`Application.onCreate`) o tras reinicio.
     */
    fun ensureScheduled() {
        if (!appPreferences.isScheduledSyncEnabled) {
            cancelDailySync()
            return
        }
        when (appPreferences.backgroundSyncMode) {
            BackgroundSyncMode.AUTOMATIC -> scheduleAutomaticSync(
                workPolicy = ExistingPeriodicWorkPolicy.KEEP
            )
            BackgroundSyncMode.SCHEDULED -> scheduleDailySync(
                hour = appPreferences.scheduledSyncHour,
                minute = appPreferences.scheduledSyncMinute,
                workPolicy = ExistingPeriodicWorkPolicy.KEEP
            )
        }
    }

    fun applyConfiguration(resetLastSyncDate: Boolean = false) {
        if (resetLastSyncDate) {
            appPreferences.lastScheduledSyncDate = ""
            appPreferences.lastAutoSyncCheckMs = 0L
        }
        if (!appPreferences.isScheduledSyncEnabled) {
            cancelDailySync()
            return
        }
        when (appPreferences.backgroundSyncMode) {
            BackgroundSyncMode.AUTOMATIC -> scheduleAutomaticSync(
                workPolicy = ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE
            )
            BackgroundSyncMode.SCHEDULED -> scheduleDailySync(
                hour = appPreferences.scheduledSyncHour,
                minute = appPreferences.scheduledSyncMinute,
                workPolicy = ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE
            )
        }
    }

    suspend fun triggerAutoSyncOnEvent(nowMs: Long = System.currentTimeMillis()): Boolean {
        if (!shouldRunEventAutoSync(
                isEnabled = appPreferences.isScheduledSyncEnabled,
                mode = appPreferences.backgroundSyncMode,
                nowMs = nowMs,
                lastCheckMs = appPreferences.lastAutoSyncCheckMs
            )
        ) {
            return true
        }
        return executeAutoSyncCycle(nowMs = nowMs)
    }

    suspend fun executeScheduledSyncIfDue(requireExactTimeWindow: Boolean): Boolean {
        if (!appPreferences.isScheduledSyncEnabled) {
            cancelDailySync()
            return true
        }

        if (appPreferences.backgroundSyncMode == BackgroundSyncMode.AUTOMATIC) {
            if (requireExactTimeWindow) {
                return true
            }
            return executeAutoSyncCycle()
        }

        if (requireExactTimeWindow && !isTimeMatchingSchedule(
                targetHour = appPreferences.scheduledSyncHour,
                targetMinute = appPreferences.scheduledSyncMinute
            )
        ) {
            return true
        }

        if (!syncMutex.tryLock()) return true
        try {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            if (appPreferences.lastScheduledSyncDate == today) {
                return true
            }
            appPreferences.lastScheduledSyncDate = today

            val summary = syncAllRecommendedUseCase(ensureLocalStability = false)
            if (shouldShowSyncNotification(
                    mode = BackgroundSyncMode.SCHEDULED,
                    notificationsEnabled = appPreferences.isScheduledSyncNotificationEnabled,
                    summary = summary
                )
            ) {
                syncNotificationManager.showSyncResultNotification(summary)
            }
            return summary.errors.isEmpty()
        } finally {
            syncMutex.unlock()
        }
    }

    private suspend fun executeAutoSyncCycle(nowMs: Long = System.currentTimeMillis()): Boolean {
        if (!syncMutex.tryLock()) return true
        try {
            appPreferences.lastAutoSyncCheckMs = nowMs
            scheduleNextAutoPulseAlarm()
            val summary = syncAllRecommendedUseCase(ensureLocalStability = true)
            if (summary.deferredCount > 0) {
                scheduleOneTimeAutoSync(
                    delayMs = AUTO_UNSTABLE_RETRY_DELAY_MS,
                    policy = ExistingWorkPolicy.REPLACE
                )
            }
            if (shouldShowSyncNotification(
                    mode = BackgroundSyncMode.AUTOMATIC,
                    notificationsEnabled = appPreferences.isScheduledSyncNotificationEnabled,
                    summary = summary
                )
            ) {
                syncNotificationManager.showSyncResultNotification(summary)
            }
            return summary.errors.isEmpty()
        } finally {
            syncMutex.unlock()
        }
    }

    private fun buildAlarmPendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        ALARM_REQUEST_CODE,
        Intent(context, ScheduledSyncReceiver::class.java).apply { action = ALARM_ACTION },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun buildAutoPulsePendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        AUTO_ALARM_REQUEST_CODE,
        Intent(context, ScheduledSyncReceiver::class.java).apply { action = AUTO_ALARM_ACTION },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}
