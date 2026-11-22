package com.jotasoft.edensavesync

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.jotasoft.edensavesync.data.scheduler.ScheduledSyncScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class EdenApplication : Application() {

    @Inject
    lateinit var scheduledSyncScheduler: ScheduledSyncScheduler

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        scheduledSyncScheduler.applyConfiguration()

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    if (intent.action == Intent.ACTION_USER_PRESENT ||
                        intent.action == Intent.ACTION_SCREEN_OFF
                    ) {
                        appScope.launch {
                            scheduledSyncScheduler.triggerAutoSyncOnEvent()
                        }
                    }
                }
            },
            filter
        )
    }
}
