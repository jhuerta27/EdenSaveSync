package com.jotasoft.edensavesync.data.scheduler

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ScheduledSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

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
