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
import com.jotasoft.edensavesync.domain.usecase.PairingCodeUseCase
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
    private val pairingCodeUseCase: PairingCodeUseCase,
    private val authRepository: AuthRepository,
    private val storageRepository: EdenStorageRepository,
    private val saveSyncRepository: SaveSyncRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        EdenSyncUiState(
            deviceName = authRepository.getDeviceName(),
            edenFolderUri = storageRepository.getFolderUriString()?.let { Uri.parse(it) },
            isGoogleDriveConnected = authRepository.isGoogleDriveConfigured(),
            googleClientId = authRepository.getGoogleClientId(),
            googleClientSecret = authRepository.getGoogleClientSecret()
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

    fun saveDirectDriveToken(
        accessToken: String,
        refreshToken: String,
        clientId: String,
        clientSecret: String
    ) {
        authRepository.saveDirectDriveToken(accessToken, refreshToken, clientId, clientSecret)
        _uiState.update {
            it.copy(
                isGoogleDriveConnected = authRepository.isGoogleDriveConfigured(),
                googleClientId = authRepository.getGoogleClientId(),
                googleClientSecret = authRepository.getGoogleClientSecret(),
                statusBannerMessage = StringResource(R.string.msg_credentials_saved),
                isStatusError = false
            )
        }
        refreshAll()
    }

    fun onOAuthRedirect(uri: Uri) {
        authRepository.handleOAuthRedirect(uri)
    }

    fun startBrowserOAuth(
        onOpenUrl: (String) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSyncing = true,
                    syncProgressMessage = StringResource(R.string.msg_waiting_browser_auth),
                    isStatusError = false
                )
            }
            try {
                val result = authRepository.startBrowserOAuth(onOpenUrl)
                result.fold(
                    onSuccess = {
                        _uiState.update {
                            it.copy(
                                isSyncing = false,
                                syncProgressMessage = null,
                                isGoogleDriveConnected = authRepository.isGoogleDriveConfigured(),
                                googleClientId = authRepository.getGoogleClientId(),
                                googleClientSecret = authRepository.getGoogleClientSecret(),
                                statusBannerMessage = StringResource(R.string.msg_drive_connected),
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
                                statusBannerMessage = err.message?.let { msg -> StringResource(msg) }
                                    ?: StringResource(R.string.msg_drive_auth_error),
                                isStatusError = true
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        syncProgressMessage = null,
                        statusBannerMessage = e.message?.let { msg -> StringResource(msg) }
                            ?: StringResource(R.string.msg_drive_auth_error),
                        isStatusError = true
                    )
                }
            }
        }
    }

    fun exportPairingCode(): String = pairingCodeUseCase.exportCode()

    fun importPairingCode(code: String) {
        val ok = pairingCodeUseCase.importCode(code)
        if (ok) {
            _uiState.update {
                it.copy(
                    isGoogleDriveConnected = authRepository.isGoogleDriveConfigured(),
                    googleClientId = authRepository.getGoogleClientId(),
                    googleClientSecret = authRepository.getGoogleClientSecret(),
                    statusBannerMessage = StringResource(R.string.msg_pairing_imported),
                    isStatusError = false
                )
            }
            refreshAll()
        } else {
            _uiState.update {
                it.copy(
                    statusBannerMessage = StringResource(R.string.msg_pairing_invalid),
                    isStatusError = true
                )
            }
        }
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
        refreshAll()
    }

    fun dismissBanner() = _uiState.update { it.copy(statusBannerMessage = null) }

    fun onAppResumed() {
        val nowConnected = authRepository.isGoogleDriveConfigured()
        if (nowConnected != _uiState.value.isGoogleDriveConnected) {
            _uiState.update {
                it.copy(
                    isGoogleDriveConnected = nowConnected,
                    googleClientId = authRepository.getGoogleClientId(),
                    googleClientSecret = authRepository.getGoogleClientSecret()
                )
            }
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
                        statusBannerMessage = e.message?.let { msg -> StringResource(msg) }
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
                        statusBannerMessage = if (result.errors.isEmpty()) {
                            if (result.uploadedCount == 0 && result.downloadedCount == 0) {
                                StringResource(R.string.msg_all_in_sync)
                            } else {
                                StringResource(
                                    R.string.msg_sync_completed,
                                    result.uploadedCount,
                                    result.downloadedCount
                                )
                            }
                        } else {
                            StringResource(
                                R.string.msg_sync_completed_warnings,
                                result.uploadedCount,
                                result.downloadedCount,
                                result.errors.first()
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
                        statusBannerMessage = e.message?.let { msg -> StringResource(msg) }
                            ?: StringResource(R.string.msg_sync_error),
                        isStatusError = true
                    )
                }
            }
        }
    }

    fun uploadSingleGame(item: GameSyncItem) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSyncing = true,
                    syncProgressMessage = StringResource(R.string.msg_uploading_game, item.displayName)
                )
            }
            try {
                val result = uploadSaveGameUseCase(item)
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        syncProgressMessage = null,
                        statusBannerMessage = result.fold(
                            onSuccess = { StringResource(R.string.msg_game_uploaded, item.displayName) },
                            onFailure = { err ->
                                err.message?.let { msg -> StringResource(R.string.msg_upload_error, msg) }
                                    ?: StringResource(R.string.msg_sync_error)
                            }
                        ),
                        isStatusError = result.isFailure
                    )
                }
                refreshAll()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        syncProgressMessage = null,
                        statusBannerMessage = e.message?.let { msg -> StringResource(R.string.msg_upload_error, msg) }
                            ?: StringResource(R.string.msg_sync_error),
                        isStatusError = true
                    )
                }
            }
        }
    }

    fun downloadSingleGame(item: GameSyncItem) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSyncing = true,
                    syncProgressMessage = StringResource(R.string.msg_downloading_game, item.displayName)
                )
            }
            try {
                val result = downloadSaveGameUseCase(item)
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        syncProgressMessage = null,
                        statusBannerMessage = result.fold(
                            onSuccess = { StringResource(R.string.msg_game_downloaded, item.displayName) },
                            onFailure = { err ->
                                err.message?.let { msg -> StringResource(R.string.msg_download_error, msg) }
                                    ?: StringResource(R.string.msg_sync_error)
                            }
                        ),
                        isStatusError = result.isFailure
                    )
                }
                refreshAll()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        syncProgressMessage = null,
                        statusBannerMessage = e.message?.let { msg -> StringResource(R.string.msg_download_error, msg) }
                            ?: StringResource(R.string.msg_sync_error),
                        isStatusError = true
                    )
                }
            }
        }
    }

    fun restoreLatestBackup(item: GameSyncItem) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSyncing = true,
                    syncProgressMessage = StringResource(R.string.msg_restoring_backup, item.displayName)
                )
            }
            try {
                val result = restoreBackupUseCase(item)
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        syncProgressMessage = null,
                        statusBannerMessage = result.fold(
                            onSuccess = { StringResource(R.string.msg_backup_restored, item.displayName) },
                            onFailure = { err ->
                                err.message?.let { msg -> StringResource(R.string.msg_backup_restore_error, msg) }
                                    ?: StringResource(R.string.msg_sync_error)
                            }
                        ),
                        isStatusError = result.isFailure
                    )
                }
                refreshAll()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        syncProgressMessage = null,
                        statusBannerMessage = e.message?.let { msg -> StringResource(R.string.msg_backup_restore_error, msg) }
                            ?: StringResource(R.string.msg_sync_error),
                        isStatusError = true
                    )
                }
            }
        }
    }
}
