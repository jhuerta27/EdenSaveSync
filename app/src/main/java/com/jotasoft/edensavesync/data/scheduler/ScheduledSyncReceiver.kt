package com.jotasoft.edensavesync.data.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jotasoft.edensavesync.data.notification.SyncNotificationManager
import com.jotasoft.edensavesync.data.storage.AppPreferences
import com.jotasoft.edensavesync.domain.usecase.SyncAllRecommendedUseCase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScheduledSyncReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ReceiverEntryPoint {
        fun syncAllRecommendedUseCase(): SyncAllRecommendedUseCase
        fun appPreferences(): AppPreferences
        fun syncNotificationManager(): SyncNotificationManager
        fun scheduler(): ScheduledSyncScheduler
    }

    override fun onReceive(context: Context, intent: Intent) {
        val appContext = context.applicationContext
        val entryPoint = EntryPointAccessors.fromApplication(
            appContext,
            ReceiverEntryPoint::class.java
        )
        val prefs = entryPoint.appPreferences()

        if (!prefs.isScheduledSyncEnabled) {
            entryPoint.scheduler().cancelDailySync()
            return
        }

        // 1. Reprogramar la siguiente alarma para mañana a la hora configurada
        entryPoint.scheduler().scheduleDailySync(
            prefs.scheduledSyncHour,
            prefs.scheduledSyncMinute
        )

        // 2. Descartar alarmas rezagadas u obsoletas que no coincidan con la hora configurada
        if (!ScheduledSyncScheduler.isTimeMatchingSchedule(
                targetHour = prefs.scheduledSyncHour,
                targetMinute = prefs.scheduledSyncMinute
            )
        ) {
            return
        }

        // 3. Garantizar una única sincronización activa en segundo plano al día
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        if (prefs.lastScheduledSyncDate == today) {
            return
        }
        prefs.lastScheduledSyncDate = today

        // 4. Ejecutar la sincronización en segundo plano y notificar si está habilitado
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val summary = entryPoint.syncAllRecommendedUseCase()()
                if (prefs.isScheduledSyncNotificationEnabled) {
                    entryPoint.syncNotificationManager().showSyncResultNotification(summary)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
            } finally {
                pendingResult.finish()
            }
        }
    }
}
