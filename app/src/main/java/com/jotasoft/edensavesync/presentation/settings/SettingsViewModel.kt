package com.jotasoft.edensavesync.presentation.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import com.jotasoft.edensavesync.data.scheduler.ScheduledSyncScheduler
import com.jotasoft.edensavesync.data.storage.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appPreferences: AppPreferences,
    private val scheduledSyncScheduler: ScheduledSyncScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            isScheduledSyncEnabled = appPreferences.isScheduledSyncEnabled,
            scheduledHour = appPreferences.scheduledSyncHour,
            scheduledMinute = appPreferences.scheduledSyncMinute,
            isNotificationEnabled = appPreferences.isScheduledSyncNotificationEnabled,
            appVersion = resolveAppVersion()
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun onToggleScheduledSync(enabled: Boolean) {
        appPreferences.isScheduledSyncEnabled = enabled
        if (enabled) {
            appPreferences.lastScheduledSyncDate = ""
            scheduledSyncScheduler.cancelDailySync()
            scheduledSyncScheduler.scheduleDailySync(
                hour = appPreferences.scheduledSyncHour,
                minute = appPreferences.scheduledSyncMinute
            )
        } else {
            scheduledSyncScheduler.cancelDailySync()
        }
        _uiState.update { it.copy(isScheduledSyncEnabled = enabled) }
    }

    fun onToggleNotification(enabled: Boolean) {
        appPreferences.isScheduledSyncNotificationEnabled = enabled
        _uiState.update { it.copy(isNotificationEnabled = enabled) }
    }

    fun onSetScheduledTime(hour: Int, minute: Int) {
        appPreferences.scheduledSyncHour = hour
        appPreferences.scheduledSyncMinute = minute
        appPreferences.lastScheduledSyncDate = ""
        if (appPreferences.isScheduledSyncEnabled) {
            scheduledSyncScheduler.cancelDailySync()
            scheduledSyncScheduler.scheduleDailySync(hour = hour, minute = minute)
        }
        _uiState.update { it.copy(scheduledHour = hour, scheduledMinute = minute) }
    }

    private fun resolveAppVersion(): String = try {
        val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        pInfo.versionName ?: "1.0.1"
    } catch (_: Exception) {
        "1.0.1"
    }
}
