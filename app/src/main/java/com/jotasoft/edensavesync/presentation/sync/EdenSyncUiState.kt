package com.jotasoft.edensavesync.presentation.sync

import android.net.Uri
import androidx.annotation.StringRes
import com.jotasoft.edensavesync.domain.model.GameSyncItem

data class StringResource(
    @StringRes val resId: Int = 0,
    val args: List<Any> = emptyList(),
    val text: String? = null
) {
    companion object {
        operator fun invoke(@StringRes resId: Int, vararg args: Any): StringResource =
            StringResource(resId = resId, args = args.toList())

        operator fun invoke(text: String): StringResource =
            StringResource(text = text)
    }
}

data class EdenSyncUiState(
    val deviceName: String = "",
    val edenFolderUri: Uri? = null,
    val activeProfileId: String = "",
    val isGoogleDriveConnected: Boolean = false,
    val isScanning: Boolean = false,
    val isSyncing: Boolean = false,
    val syncProgressMessage: StringResource? = null,
    val statusBannerMessage: StringResource? = null,
    val isStatusError: Boolean = false,
    val games: List<GameSyncItem> = emptyList(),
    val googleClientId: String = "",
    val googleClientSecret: String = ""
)
