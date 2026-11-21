package com.jotasoft.edensavesync.data.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.jotasoft.edensavesync.R
import com.jotasoft.edensavesync.domain.model.SyncSummaryResult
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        const val CHANNEL_ID = "eden_scheduled_sync_channel"
        const val NOTIFICATION_ID = 2026
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.notification_channel_name)
            val descriptionText = context.getString(R.string.notification_channel_desc)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showSyncResultNotification(summary: SyncSummaryResult) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) return
        }

        val hasErrors = summary.errors.isNotEmpty()
        val title = if (hasErrors) {
            context.getString(R.string.notification_sync_title_error)
        } else {
            context.getString(R.string.notification_sync_title_success)
        }

        val message = when {
            summary.uploadedCount > 0 || summary.downloadedCount > 0 -> {
                context.getString(
                    R.string.notification_sync_body_changes,
                    summary.uploadedCount,
                    summary.downloadedCount
                )
            }
            hasErrors -> {
                context.getString(
                    R.string.notification_sync_body_error,
                    summary.errors.firstOrNull().orEmpty()
                )
            }
            else -> {
                context.getString(R.string.notification_sync_body_up_to_date)
            }
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_app_logo)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
        } catch (_: SecurityException) {
        }
    }
}
