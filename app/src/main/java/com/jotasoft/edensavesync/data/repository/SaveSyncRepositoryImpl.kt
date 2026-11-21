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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

@Singleton
class SaveSyncRepositoryImpl @Inject constructor(
    private val context: Context,
    private val storageRepo: EdenStorageRepository,
    private val cloudRepo: CloudSyncRepository,
    private val prefs: AppPreferences
) : SaveSyncRepository {

    override suspend fun scanGames(): Pair<String, List<GameSyncItem>> = withContext(Dispatchers.IO) {
        SwitchTitleCatalog.init(context, prefs.cachedCatalogJson)
        runCatching {
            SwitchTitleCatalog.fetchRemoteCatalog()?.let { (_, rawJson) ->
                prefs.cachedCatalogJson = rawJson
            }
        }

        val (profileId, localList) = if (storageRepo.isFolderConfigured()) {
            storageRepo.scanLocalSaves()
        } else {
            "" to emptyList()
        }

        val remoteList = if (cloudRepo.isConfigured()) {
            runCatching { cloudRepo.listRemoteSaves() }.getOrDefault(emptyList())
        } else {
            emptyList()
        }

        val merged = mergeLocalAndRemote(localList, remoteList)
        profileId to merged
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
                val res = uploadSingleGame(item)
                if (res.isSuccess) {
                    uploaded++
                } else {
                    val ex = res.exceptionOrNull()
                    if (ex is CancellationException) throw ex
                    errors.add("${item.displayName}: ${ex?.message}")
                }
            }

            for (item in toDownload) {
                onProgress("Descargando ${item.displayName} desde Google Drive...")
                val res = downloadSingleGame(item)
                if (res.isSuccess) {
                    downloaded++
                } else {
                    val ex = res.exceptionOrNull()
                    if (ex is CancellationException) throw ex
                    errors.add("${item.displayName}: ${ex?.message}")
                }
            }

            SyncSummaryResult(
                uploadedCount = uploaded,
                downloadedCount = downloaded,
                skippedCount = games.size - (toUpload.size + toDownload.size),
                errors = errors
            )
        }

    override suspend fun uploadSingleGame(item: GameSyncItem): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val local = item.localSave
                ?: throw IllegalStateException("No existe partida local para ${item.displayName}")
            if (local.fileCount == 0 || local.totalSizeBytes == 0L) {
                throw IllegalStateException("No se puede subir una partida vacía sin archivos para ${item.displayName}")
            }
            val tempZip = File(context.cacheDir, "upload_${item.titleId}.zip")
            try {
                val exported = storageRepo.exportSaveToZip(item.titleId, tempZip)
                if (!exported) {
                    throw IllegalStateException("No se pudo empaquetar la partida local de ${item.titleId}")
                }
                val remote = cloudRepo.uploadSaveZip(
                    localSave = local,
                    gameName = item.displayName,
                    deviceName = prefs.deviceName,
                    zipFile = tempZip,
                    existingDriveFileId = item.remoteSave?.driveFileId
                )
                prefs.recordSyncedTitle(
                    titleId = item.titleId,
                    contentHash = local.contentHash,
                    remoteTimestampMs = remote.lastModifiedMs,
                    localTimestampAtSyncMs = if (local.lastModifiedMs > 0L) local.lastModifiedMs else remote.lastModifiedMs
                )
            } finally {
                if (tempZip.exists()) tempZip.delete()
            }
        }.onFailure { if (it is CancellationException) throw it }
    }

    override suspend fun downloadSingleGame(item: GameSyncItem): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val remote = item.remoteSave
                ?: throw IllegalStateException("No existe partida en la nube para ${item.displayName}")
            val tempZip = File(context.cacheDir, "download_${item.titleId}.zip")
            try {
                cloudRepo.downloadSaveZip(remote.driveFileId, tempZip)
                val imported = storageRepo.importSaveFromZip(item.titleId, tempZip)
                if (!imported) {
                    throw IllegalStateException("No se pudo escribir en la carpeta de Eden para ${item.titleId}")
                }
                if (remote.gameName.isNotBlank() && prefs.getCustomGameNames()[item.titleId].isNullOrBlank()) {
                    if (!remote.gameName.startsWith("Juego de Switch")) {
                        prefs.setCustomGameName(item.titleId, remote.gameName)
                    }
                }
                val (_, updatedLocals) = storageRepo.scanLocalSaves()
                val updatedLocal = updatedLocals.firstOrNull { it.titleId.equals(item.titleId, ignoreCase = true) }
                val effectiveHash = updatedLocal?.contentHash?.takeIf { it.isNotBlank() } ?: remote.contentHash
                val effectiveLocalTs = if (updatedLocal != null && updatedLocal.lastModifiedMs > 0L) {
                    updatedLocal.lastModifiedMs
                } else {
                    remote.lastModifiedMs
                }
                prefs.recordSyncedTitle(
                    titleId = item.titleId,
                    contentHash = effectiveHash,
                    remoteTimestampMs = remote.lastModifiedMs,
                    localTimestampAtSyncMs = effectiveLocalTs
                )
            } finally {
                if (tempZip.exists()) tempZip.delete()
            }
        }.onFailure { if (it is CancellationException) throw it }
    }

    override suspend fun restoreLatestBackup(item: GameSyncItem): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val ok = storageRepo.restoreLatestBackup(item.titleId)
            if (!ok) {
                throw IllegalStateException("No se pudo restaurar la copia de seguridad de ${item.titleId}")
            }
        }.onFailure { if (it is CancellationException) throw it }
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
        getLocalBackupCount = { storageRepo.getLocalBackupCount(it) }
    )
}
