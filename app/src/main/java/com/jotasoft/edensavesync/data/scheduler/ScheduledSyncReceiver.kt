package com.jotasoft.edensavesync.data.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ScheduledSyncReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val scheduler = EntryPointAccessors.fromApplication(
            context.applicationContext,
            ScheduledSyncScheduler.SchedulerEntryPoint::class.java
        ).scheduler()

        if (intent.action == ScheduledSyncScheduler.AUTO_ALARM_ACTION) {
            scheduler.handleAutoPulseAlarm()
            return
        }

        scheduler.ensureScheduled()

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                scheduler.executeScheduledSyncIfDue(requireExactTimeWindow = true)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
            } finally {
                pendingResult.finish()
            }
        }
    }
}
