package com.jotasoft.edensavesync.data.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jotasoft.edensavesync.data.storage.AppPreferences
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

class BootCompletedReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface BootEntryPoint {
        fun appPreferences(): AppPreferences
        fun scheduler(): ScheduledSyncScheduler
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            val entryPoint = EntryPointAccessors.fromApplication(
                context.applicationContext,
                BootEntryPoint::class.java
            )
            val prefs = entryPoint.appPreferences()
            if (prefs.isScheduledSyncEnabled) {
                entryPoint.scheduler().scheduleDailySync(
                    prefs.scheduledSyncHour,
                    prefs.scheduledSyncMinute
                )
            }
        }
    }
}
