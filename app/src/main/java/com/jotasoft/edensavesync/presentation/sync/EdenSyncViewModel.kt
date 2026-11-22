package com.jotasoft.edensavesync.presentation.sync

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jotasoft.edensavesync.R
import com.jotasoft.edensavesync.domain.model.GameSyncItem
import com.jotasoft.edensavesync.domain.repository.AuthRepository
import com.jotasoft.edensavesync.domain.repository.EdenStorageRepository
import com.jotasoft.edensavesync.domain.repository.SaveSyncRepository
import com.jotasoft.edensavesync.domain.usecase.DownloadSaveGameUseCase
import com.jotasoft.edensavesync.domain.usecase.RestoreBackupUseCase
import com.jotasoft.edensavesync.domain.usecase.ScanGamesUseCase
import com.jotasoft.edensavesync.domain.usecase.SyncAllRecommendedUseCase
import com.jotasoft.edensavesync.domain.usecase.UploadSaveGameUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

@HiltViewModel
class EdenSyncViewModel @Inject constructor(
    private val scanGamesUseCase: ScanGamesUseCase,
    private val syncAllRecommendedUseCase: SyncAllRecommendedUseCase,
    private val uploadSaveGameUseCase: UploadSaveGameUseCase,
    private val downloadSaveGameUseCase: DownloadSaveGameUseCase,
    private val restoreBackupUseCase: RestoreBackupUseCase,
    private val authRepository: AuthRepository,
    private val storageRepository: EdenStorageRepository,
    private val saveSyncRepository: SaveSyncRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        EdenSyncUiState(
            deviceName = authRepository.getDeviceName(),
            edenFolderUri = storageRepository.getFolderUriString()?.let(Uri::parse),
            isGoogleDriveConnected = authRepository.isGoogleDriveConfigured()
        )
    )
    val uiState: StateFlow<EdenSyncUiState> = _uiState.asStateFlow()

    init {
        refreshAll()
    }

    fun onEdenFolderSelected(uri: Uri) {
        storageRepository.setFolderUriString(uri.toString())
        _uiState.update {
            it.copy(
                edenFolderUri = uri,
                statusBannerMessage = StringResource(R.string.msg_folder_linked),
                isStatusError = false
            )
        }
        refreshAll()
    }

    fun updateDeviceName(newName: String) {
        if (newName.isBlank()) return
        authRepository.setDeviceName(newName)
        _uiState.update { it.copy(deviceName = authRepository.getDeviceName()) }
    }

    fun onOAuthRedirect(uri: Uri) {
        authRepository.handleOAuthRedirect(uri.toString())
    }

    fun startBrowserOAuth(onOpenUrl: (String) -> Unit) = executeSyncAction(
        progressMessage = StringResource(R.string.msg_waiting_browser_auth),
        onSuccessMessage = { StringResource(R.string.msg_drive_connected) },
        onErrorMessage = { err ->
            err?.message?.let(StringResource::invoke) ?: StringResource(R.string.msg_drive_auth_error)
        },
        updateDriveStatusOnSuccess = true
    ) {
        authRepository.startBrowserOAuth(onOpenUrl)
    }

    fun disconnectGoogleDrive() {
        authRepository.clearGoogleDriveAuth()
        _uiState.update {
            it.copy(
                isGoogleDriveConnected = false,
                statusBannerMessage = StringResource(R.string.msg_drive_disconnected),
                isStatusError = false
            )
        }
        refreshAll()
    }

    fun renameGame(titleId: String, customName: String) {
        saveSyncRepository.renameGame(titleId, customName)
        val trimmed = customName.trim()
        if (trimmed.isNotEmpty()) {
            _uiState.update { state ->
                state.copy(
                    games = state.games.map { item ->
                        if (item.titleId.equals(titleId, ignoreCase = true)) {
                            item.copy(displayName = trimmed)
                        } else {
                            item
                        }
                    }
                )
            }
        } else {
            refreshAll()
        }
    }

    fun dismissBanner() = _uiState.update { it.copy(statusBannerMessage = null) }

    fun onAppResumed() {
        val nowConnected = authRepository.isGoogleDriveConfigured()
        if (nowConnected != _uiState.value.isGoogleDriveConnected) {
            _uiState.update { it.copy(isGoogleDriveConnected = nowConnected) }
            refreshAll()
        }
    }

    fun refreshAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true) }
            try {
                val (profileId, list) = scanGamesUseCase()
                _uiState.update { state ->
                    state.copy(
                        isScanning = false,
                        activeProfileId = profileId,
                        games = list
                    )
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        statusBannerMessage = e.message?.let(StringResource::invoke)
                            ?: StringResource(R.string.msg_scan_error),
                        isStatusError = true
                    )
                }
            }
        }
    }

    fun syncAllRecommended() {
        if (!storageRepository.isFolderConfigured()) {
            _uiState.update {
                it.copy(
                    statusBannerMessage = StringResource(R.string.msg_link_folder_first),
                    isStatusError = true
                )
            }
            return
        }
        if (!authRepository.isGoogleDriveConfigured()) {
            _uiState.update {
                it.copy(
                    statusBannerMessage = StringResource(R.string.msg_connect_drive_first),
                    isStatusError = true
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSyncing = true,
                    syncProgressMessage = StringResource(R.string.msg_sync_starting)
                )
            }
            try {
                val result = syncAllRecommendedUseCase { progressMsg ->
                    _uiState.update { it.copy(syncProgressMessage = StringResource(progressMsg)) }
                }
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        syncProgressMessage = null,
                        statusBannerMessage = when {
                            result.errors.isNotEmpty() -> StringResource(
                                R.string.msg_sync_completed_warnings,
                                result.uploadedCount,
                                result.downloadedCount,
                                result.errors.first()
                            )
                            result.uploadedCount == 0 && result.downloadedCount == 0 ->
                                StringResource(R.string.msg_all_in_sync)
                            else -> StringResource(
                                R.string.msg_sync_completed,
                                result.uploadedCount,
                                result.downloadedCount
                            )
                        },
                        isStatusError = result.errors.isNotEmpty()
                    )
                }
                refreshAll()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        syncProgressMessage = null,
                        statusBannerMessage = e.message?.let(StringResource::invoke)
                            ?: StringResource(R.string.msg_sync_error),
                        isStatusError = true
                    )
                }
            }
        }
    }

    fun uploadSingleGame(item: GameSyncItem) = executeSyncAction(
        progressMessage = StringResource(R.string.msg_uploading_game, item.displayName),
        onSuccessMessage = { StringResource(R.string.msg_game_uploaded, item.displayName) },
        onErrorMessage = { err ->
            err?.message?.let { StringResource(R.string.msg_upload_error, it) }
                ?: StringResource(R.string.msg_sync_error)
        }
    ) {
        uploadSaveGameUseCase(item)
    }

    fun downloadSingleGame(item: GameSyncItem) = executeSyncAction(
        progressMessage = StringResource(R.string.msg_downloading_game, item.displayName),
        onSuccessMessage = { StringResource(R.string.msg_game_downloaded, item.displayName) },
        onErrorMessage = { err ->
            err?.message?.let { StringResource(R.string.msg_download_error, it) }
                ?: StringResource(R.string.msg_sync_error)
        }
    ) {
        downloadSaveGameUseCase(item)
    }

    fun restoreLatestBackup(item: GameSyncItem) = executeSyncAction(
        progressMessage = StringResource(R.string.msg_restoring_backup, item.displayName),
        onSuccessMessage = { StringResource(R.string.msg_backup_restored, item.displayName) },
        onErrorMessage = { err ->
            err?.message?.let { StringResource(R.string.msg_backup_restore_error, it) }
                ?: StringResource(R.string.msg_sync_error)
        }
    ) {
        restoreBackupUseCase(item)
    }

    private fun executeSyncAction(
        progressMessage: StringResource,
        onSuccessMessage: () -> StringResource,
        onErrorMessage: (Throwable?) -> StringResource,
        updateDriveStatusOnSuccess: Boolean = false,
        action: suspend () -> Result<Unit>
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSyncing = true,
                    syncProgressMessage = progressMessage,
                    isStatusError = false
                )
            }
            val result = runCatching { action().getOrThrow() }
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isSyncing = false,
                            syncProgressMessage = null,
                            isGoogleDriveConnected = if (updateDriveStatusOnSuccess) {
                                authRepository.isGoogleDriveConfigured()
                            } else {
                                it.isGoogleDriveConnected
                            },
                            statusBannerMessage = onSuccessMessage(),
                            isStatusError = false
                        )
                    }
                    refreshAll()
                },
                onFailure = { err ->
                    if (err is CancellationException) throw err
                    _uiState.update {
                        it.copy(
                            isSyncing = false,
                            syncProgressMessage = null,
                            statusBannerMessage = onErrorMessage(err),
                            isStatusError = true
                        )
                    }
                }
            )
        }
    }
}
