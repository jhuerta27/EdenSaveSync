package com.jotasoft.edensavesync.data.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.EntryPointAccessors

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                ScheduledSyncScheduler.SchedulerEntryPoint::class.java
            ).scheduler().applyConfiguration()
        }
    }
}
