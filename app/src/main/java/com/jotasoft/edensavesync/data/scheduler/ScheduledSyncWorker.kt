package com.jotasoft.edensavesync.data.scheduler

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jotasoft.edensavesync.data.notification.SyncNotificationManager
import com.jotasoft.edensavesync.data.storage.AppPreferences
import com.jotasoft.edensavesync.domain.usecase.SyncAllRecommendedUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Worker en segundo plano que ejecuta la sincronización recomendada de partidas de Eden con Google Drive.
 */
class ScheduledSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ScheduledSyncWorkerEntryPoint {
        fun syncAllRecommendedUseCase(): SyncAllRecommendedUseCase
        fun appPreferences(): AppPreferences
        fun syncNotificationManager(): SyncNotificationManager
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                ScheduledSyncWorkerEntryPoint::class.java
            )
            val prefs = entryPoint.appPreferences()
            if (!prefs.isScheduledSyncEnabled) {
                return@withContext Result.success()
            }

            // Garantizar una única sincronización activa en segundo plano al día
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            if (prefs.lastScheduledSyncDate == today) {
                return@withContext Result.success()
            }
            prefs.lastScheduledSyncDate = today

            val syncAllUseCase = entryPoint.syncAllRecommendedUseCase()
            val summary = syncAllUseCase()

            if (prefs.isScheduledSyncNotificationEnabled) {
                entryPoint.syncNotificationManager().showSyncResultNotification(summary)
            }

            if (summary.errors.isNotEmpty()) Result.retry() else Result.success()
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            Result.retry()
        }
    }
}
