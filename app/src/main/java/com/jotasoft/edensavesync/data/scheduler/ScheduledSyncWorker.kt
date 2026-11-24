package com.jotasoft.edensavesync.data.scheduler

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.jotasoft.edensavesync.R
import com.jotasoft.edensavesync.data.notification.SyncNotificationManager
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ScheduledSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun getForegroundInfo(): ForegroundInfo {
        val notification = NotificationCompat.Builder(applicationContext, SyncNotificationManager.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_app_logo)
            .setContentTitle(applicationContext.getString(R.string.app_name))
            .setContentText(applicationContext.getString(R.string.hero_title_syncing))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
        return ForegroundInfo(SyncNotificationManager.NOTIFICATION_ID + 1, notification)
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val scheduler = EntryPointAccessors.fromApplication(
                applicationContext,
                ScheduledSyncScheduler.SchedulerEntryPoint::class.java
            ).scheduler()

            val ok = scheduler.executeScheduledSyncIfDue(requireExactTimeWindow = false)
            if (ok) Result.success() else Result.retry()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.retry()
        }
    }
}
