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
import com.jotasoft.edensavesync.data.storage.AppPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScheduledSyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appPreferences: AppPreferences
) {
    companion object {
        const val WORK_NAME = "EdenScheduledSyncWork"
        const val ALARM_ACTION = "com.jotasoft.edensavesync.ACTION_SCHEDULED_SYNC"
        const val ALARM_REQUEST_CODE = 202611

        fun calculateTargetTimeMs(
            targetHour: Int,
            targetMinute: Int,
            nowMs: Long = System.currentTimeMillis()
        ): Long {
            val nowCalendar = Calendar.getInstance().apply { timeInMillis = nowMs }
            val targetCalendar = Calendar.getInstance().apply {
                timeInMillis = nowMs
                set(Calendar.HOUR_OF_DAY, targetHour)
                set(Calendar.MINUTE, targetMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            if (targetCalendar.timeInMillis <= nowCalendar.timeInMillis) {
                targetCalendar.add(Calendar.DAY_OF_YEAR, 1)
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
            val diff = kotlin.math.abs(currentMinutesOfDay - targetMinutesOfDay)
            val circularDiff = kotlin.math.min(diff, 1440 - diff)
            return circularDiff <= toleranceMinutes
        }
    }

    fun scheduleDailySync(hour: Int, minute: Int) {
        // Cancelar de forma explícita y completa cualquier alarma o tarea previa
        cancelDailySync()

        val targetTimeMs = calculateTargetTimeMs(hour, minute)
        val initialDelay = targetTimeMs - System.currentTimeMillis()

        // 1. Programar Alarma Exacta en AlarmManager (despierta al dispositivo con app cerrada)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        if (alarmManager != null) {
            val intent = Intent(context, ScheduledSyncReceiver::class.java).apply {
                action = ALARM_ACTION
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            val showIntent = PendingIntent.getActivity(
                context,
                0,
                launchIntent ?: Intent(context, ScheduledSyncReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarmClockInfo = AlarmManager.AlarmClockInfo(targetTimeMs, showIntent)
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
        }

        // 2. Programar además WorkManager como respaldo periódico con cancelación previa
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<ScheduledSyncWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelay.coerceAtLeast(0L), TimeUnit.MILLISECONDS)
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
            val intent = Intent(context, ScheduledSyncReceiver::class.java).apply {
                action = ALARM_ACTION
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    fun applyConfiguration() {
        if (appPreferences.isScheduledSyncEnabled) {
            scheduleDailySync(
                hour = appPreferences.scheduledSyncHour,
                minute = appPreferences.scheduledSyncMinute
            )
        } else {
            cancelDailySync()
        }
    }
}
