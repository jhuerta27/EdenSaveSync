package com.jotasoft.edensavesync.data.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.jotasoft.edensavesync.data.notification.SyncNotificationManager
import com.jotasoft.edensavesync.data.storage.AppPreferences
import com.jotasoft.edensavesync.domain.usecase.SyncAllRecommendedUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
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

    companion object {
        const val WORK_NAME = "EdenScheduledSyncWork"
        const val ALARM_ACTION = "com.jotasoft.edensavesync.ACTION_SCHEDULED_SYNC"
        const val ALARM_REQUEST_CODE = 202611

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
    }

    fun scheduleDailySync(hour: Int, minute: Int) {
        cancelDailySync()

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
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            workRequest
        )
    }

    fun cancelDailySync() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        if (alarmManager != null) {
            val pendingIntent = buildAlarmPendingIntent()
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    fun applyConfiguration(resetLastSyncDate: Boolean = false) {
        if (resetLastSyncDate) {
            appPreferences.lastScheduledSyncDate = ""
        }
        if (appPreferences.isScheduledSyncEnabled) {
            scheduleDailySync(
                hour = appPreferences.scheduledSyncHour,
                minute = appPreferences.scheduledSyncMinute
            )
        } else {
            cancelDailySync()
        }
    }

    suspend fun executeScheduledSyncIfDue(requireExactTimeWindow: Boolean): Boolean {
        if (!appPreferences.isScheduledSyncEnabled) {
            cancelDailySync()
            return true
        }
        if (requireExactTimeWindow && !isTimeMatchingSchedule(
                targetHour = appPreferences.scheduledSyncHour,
                targetMinute = appPreferences.scheduledSyncMinute
            )
        ) {
            return true
        }

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        if (appPreferences.lastScheduledSyncDate == today) {
            return true
        }
        appPreferences.lastScheduledSyncDate = today

        val summary = syncAllRecommendedUseCase()
        if (appPreferences.isScheduledSyncNotificationEnabled) {
            syncNotificationManager.showSyncResultNotification(summary)
        }
        return summary.errors.isEmpty()
    }

    private fun buildAlarmPendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        ALARM_REQUEST_CODE,
        Intent(context, ScheduledSyncReceiver::class.java).apply { action = ALARM_ACTION },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}
