package com.jotasoft.edensavesync.data.repository

import android.content.Context
import com.jotasoft.edensavesync.data.catalog.SwitchTitleCatalog
import com.jotasoft.edensavesync.data.storage.AppPreferences
import com.jotasoft.edensavesync.domain.model.GameSyncItem
import com.jotasoft.edensavesync.domain.model.LocalSaveGame
import com.jotasoft.edensavesync.domain.model.RemoteSaveGame
import com.jotasoft.edensavesync.domain.model.SyncRecommendation
import com.jotasoft.edensavesync.domain.model.SyncSummaryResult
import com.jotasoft.edensavesync.domain.repository.CloudSyncRepository
import com.jotasoft.edensavesync.domain.repository.EdenStorageRepository
import com.jotasoft.edensavesync.domain.repository.SaveSyncRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

@Singleton
class SaveSyncRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val storageRepo: EdenStorageRepository,
    private val cloudRepo: CloudSyncRepository,
    private val prefs: AppPreferences
) : SaveSyncRepository {

    override suspend fun scanGames(): Pair<String, List<GameSyncItem>> = withContext(Dispatchers.IO) {
        coroutineScope {
            val catalogJob = async { SwitchTitleCatalog.ensureLoaded(context, prefs) }
            val localDeferred = async {
                if (storageRepo.isFolderConfigured()) {
                    storageRepo.scanLocalSaves()
                } else {
                    "" to emptyList()
                }
            }
            val remoteDeferred = async {
                if (cloudRepo.isConfigured()) {
                    runCatching { cloudRepo.listRemoteSaves() }.getOrDefault(emptyList())
                } else {
                    emptyList()
                }
            }

            catalogJob.await()
            val (profileId, localList) = localDeferred.await()
            val remoteList = remoteDeferred.await()
            profileId to mergeLocalAndRemote(localList, remoteList)
        }
    }

    override suspend fun syncAllRecommended(onProgress: (String) -> Unit): SyncSummaryResult =
        withContext(Dispatchers.IO) {
            val (_, games) = scanGames()
            val toUpload = games.filter { it.recommendation == SyncRecommendation.UPLOAD_TO_CLOUD }
            val toDownload = games.filter { it.recommendation == SyncRecommendation.DOWNLOAD_FROM_CLOUD }

            var uploaded = 0
            var downloaded = 0
            val errors = mutableListOf<String>()

            for (item in toUpload) {
                onProgress("Subiendo ${item.displayName} a Google Drive...")
                uploadSingleGame(item).fold(
                    onSuccess = { uploaded++ },
                    onFailure = { ex ->
                        if (ex is CancellationException) throw ex
                        errors.add("${item.displayName}: ${ex.message}")
                    }
                )
            }

            for (item in toDownload) {
                onProgress("Descargando ${item.displayName} desde Google Drive...")
                downloadSingleGame(item).fold(
                    onSuccess = { downloaded++ },
                    onFailure = { ex ->
                        if (ex is CancellationException) throw ex
                        errors.add("${item.displayName}: ${ex.message}")
                    }
                )
            }

            SyncSummaryResult(
                uploadedCount = uploaded,
                downloadedCount = downloaded,
                skippedCount = games.size - (toUpload.size + toDownload.size),
                errors = errors
            )
        }

    override suspend fun uploadSingleGame(item: GameSyncItem): Result<Unit> = withContext(Dispatchers.IO) {
        SaveSyncOperations.uploadSingleGame(
            item = item,
            cacheDir = context.cacheDir,
            storageRepo = storageRepo,
            cloudRepo = cloudRepo,
            deviceName = prefs.deviceName,
            onRecordSynced = prefs::recordSyncedTitle
        )
    }

    override suspend fun downloadSingleGame(item: GameSyncItem): Result<Unit> = withContext(Dispatchers.IO) {
        SaveSyncOperations.downloadSingleGame(
            item = item,
            cacheDir = context.cacheDir,
            storageRepo = storageRepo,
            cloudRepo = cloudRepo,
            getCustomGameNames = prefs::getCustomGameNames,
            setCustomGameName = prefs::setCustomGameName,
            onRecordSynced = prefs::recordSyncedTitle
        )
    }

    override suspend fun restoreLatestBackup(item: GameSyncItem): Result<Unit> = withContext(Dispatchers.IO) {
        SaveSyncOperations.restoreLatestBackup(item, storageRepo)
    }

    override fun renameGame(titleId: String, customName: String) =
        prefs.setCustomGameName(titleId, customName)

    private fun mergeLocalAndRemote(
        locals: List<LocalSaveGame>,
        remotes: List<RemoteSaveGame>
    ): List<GameSyncItem> = SaveReconciler.mergeLocalAndRemote(
        locals = locals,
        remotes = remotes,
        syncedRecords = prefs.getSyncedRecords(),
        customNames = prefs.getCustomGameNames(),
        getLocalBackupCount = storageRepo::getLocalBackupCount
    )
}

internal object SaveSyncOperations {

    suspend fun uploadSingleGame(
        item: GameSyncItem,
        cacheDir: File,
        storageRepo: EdenStorageRepository,
        cloudRepo: CloudSyncRepository,
        deviceName: String,
        onRecordSynced: (titleId: String, contentHash: String, remoteTs: Long, localTs: Long) -> Unit
    ): Result<Unit> = runCatching {
        val local = item.localSave
            ?: throw IllegalStateException("No existe partida local para ${item.displayName}")
        if (local.fileCount == 0 || local.totalSizeBytes == 0L) {
            throw IllegalStateException("No se puede subir una partida vacía sin archivos para ${item.displayName}")
        }
        val tempZip = File(cacheDir, "upload_${item.titleId}.zip")
        try {
            if (!storageRepo.exportSaveToZip(item.titleId, tempZip, local.source)) {
                throw IllegalStateException("No se pudo empaquetar la partida local de ${item.titleId}")
            }
            val remote = cloudRepo.uploadSaveZip(
                localSave = local,
                gameName = item.displayName,
                deviceName = deviceName,
                zipFile = tempZip,
                existingDriveFileId = item.remoteSave?.driveFileId
            )
            // Si hay otras carpetas locales vinculadas (p. ej. Eden <-> Peanuts Native),
            // replicar la versión más reciente para que todas queden igualadas.
            val otherLocalTargets = storageRepo.getConfiguredSources() - local.source
            if (otherLocalTargets.isNotEmpty()) {
                storageRepo.importSaveFromZip(item.titleId, tempZip, otherLocalTargets).getOrThrow()
            }
            onRecordSynced(
                item.titleId,
                local.contentHash,
                remote.lastModifiedMs,
                if (local.lastModifiedMs > 0L) local.lastModifiedMs else remote.lastModifiedMs
            )
        } finally {
            if (tempZip.exists()) tempZip.delete()
        }
    }.onFailure { if (it is CancellationException) throw it }

    suspend fun downloadSingleGame(
        item: GameSyncItem,
        cacheDir: File,
        storageRepo: EdenStorageRepository,
        cloudRepo: CloudSyncRepository,
        getCustomGameNames: () -> Map<String, String>,
        setCustomGameName: (String, String) -> Unit,
        onRecordSynced: (titleId: String, contentHash: String, remoteTs: Long, localTs: Long) -> Unit
    ): Result<Unit> = runCatching {
        val remote = item.remoteSave
            ?: throw IllegalStateException("No existe partida en la nube para ${item.displayName}")
        val tempZip = File(cacheDir, "download_${item.titleId}.zip")
        try {
            cloudRepo.downloadSaveZip(remote.driveFileId, tempZip)
            storageRepo.importSaveFromZip(item.titleId, tempZip).getOrThrow()
            if (remote.gameName.isNotBlank() &&
                !remote.gameName.startsWith("Juego de Switch") &&
                getCustomGameNames()[item.titleId].isNullOrBlank()
            ) {
                setCustomGameName(item.titleId, remote.gameName)
            }
            val updatedLocal = storageRepo.scanSingleSave(item.titleId)
            val effectiveHash = updatedLocal?.contentHash?.takeIf { it.isNotBlank() } ?: remote.contentHash
            val effectiveLocalTs = updatedLocal?.lastModifiedMs?.takeIf { it > 0L } ?: remote.lastModifiedMs
            onRecordSynced(item.titleId, effectiveHash, remote.lastModifiedMs, effectiveLocalTs)
        } finally {
            if (tempZip.exists()) tempZip.delete()
        }
    }.onFailure { if (it is CancellationException) throw it }

    suspend fun restoreLatestBackup(
        item: GameSyncItem,
        storageRepo: EdenStorageRepository
    ): Result<Unit> = runCatching {
        storageRepo.restoreLatestBackup(item.titleId).getOrThrow()
    }.onFailure { if (it is CancellationException) throw it }
}
