package com.jotasoft.edensavesync

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.jotasoft.edensavesync.data.scheduler.ScheduledSyncScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class EdenApplication : Application() {

    @Inject
    lateinit var scheduledSyncScheduler: ScheduledSyncScheduler

    override fun onCreate() {
        super.onCreate()
        scheduledSyncScheduler.ensureScheduled()

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        ContextCompat.registerReceiver(
            this,
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    if (intent.action == Intent.ACTION_USER_PRESENT ||
                        intent.action == Intent.ACTION_SCREEN_ON ||
                        intent.action == Intent.ACTION_SCREEN_OFF
                    ) {
                        scheduledSyncScheduler.enqueueAutoSyncFromEvent()
                    }
                }
            },
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }
}
