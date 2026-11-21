package com.jotasoft.edensavesync.presentation.settings

import java.util.Locale

data class SettingsUiState(
    val isScheduledSyncEnabled: Boolean = false,
    val scheduledHour: Int = 4,
    val scheduledMinute: Int = 0,
    val isNotificationEnabled: Boolean = false,
    val appVersion: String = ""
) {
    val formattedTime: String
        get() = String.format(Locale.getDefault(), "%02d:%02d", scheduledHour, scheduledMinute)
}
