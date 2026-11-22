package com.jotasoft.edensavesync.presentation.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jotasoft.edensavesync.data.scheduler.ScheduledSyncScheduler
import com.jotasoft.edensavesync.data.storage.AppPreferences
import com.jotasoft.edensavesync.domain.model.BackgroundSyncMode
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
            syncMode = appPreferences.backgroundSyncMode,
            scheduledHour = appPreferences.scheduledSyncHour,
            scheduledMinute = appPreferences.scheduledSyncMinute,
            isNotificationEnabled = appPreferences.isScheduledSyncNotificationEnabled,
            appVersion = resolveAppVersion()
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun onToggleScheduledSync(enabled: Boolean) {
        appPreferences.isScheduledSyncEnabled = enabled
        scheduledSyncScheduler.applyConfiguration(resetLastSyncDate = enabled)
        _uiState.update { it.copy(isScheduledSyncEnabled = enabled) }
        if (enabled && appPreferences.backgroundSyncMode == BackgroundSyncMode.AUTOMATIC) {
            viewModelScope.launch(Dispatchers.IO) {
                scheduledSyncScheduler.triggerAutoSyncOnEvent()
            }
        }
    }

    fun onSelectSyncMode(mode: BackgroundSyncMode) {
        if (appPreferences.backgroundSyncMode == mode) return
        appPreferences.backgroundSyncMode = mode
        scheduledSyncScheduler.applyConfiguration(resetLastSyncDate = true)
        _uiState.update { it.copy(syncMode = mode) }
        if (appPreferences.isScheduledSyncEnabled && mode == BackgroundSyncMode.AUTOMATIC) {
            viewModelScope.launch(Dispatchers.IO) {
                scheduledSyncScheduler.triggerAutoSyncOnEvent()
            }
        }
    }

    fun onToggleNotification(enabled: Boolean) {
        appPreferences.isScheduledSyncNotificationEnabled = enabled
        _uiState.update { it.copy(isNotificationEnabled = enabled) }
    }

    fun onSetScheduledTime(hour: Int, minute: Int) {
        appPreferences.scheduledSyncHour = hour
        appPreferences.scheduledSyncMinute = minute
        scheduledSyncScheduler.applyConfiguration(resetLastSyncDate = true)
        _uiState.update { it.copy(scheduledHour = hour, scheduledMinute = minute) }
    }

    private fun resolveAppVersion(): String = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.2"
    }.getOrDefault("1.0.2")
}
