package com.jotasoft.edensavesync.data.repository

import android.net.Uri
import androidx.core.net.toUri
import com.jotasoft.edensavesync.data.storage.AppPreferences
import com.jotasoft.edensavesync.data.storage.EdenStorageManager
import com.jotasoft.edensavesync.data.storage.shizuku.ShizukuPeanutsClient
import com.jotasoft.edensavesync.domain.model.LocalSaveGame
import com.jotasoft.edensavesync.domain.model.LocalSaveSource
import com.jotasoft.edensavesync.domain.repository.EdenStorageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

@Singleton
class EdenStorageRepositoryImpl @Inject constructor(
    private val storageManager: EdenStorageManager,
    private val shizukuPeanutsClient: ShizukuPeanutsClient,
    private val prefs: AppPreferences
) : EdenStorageRepository {

    override fun isFolderConfigured(): Boolean = getConfiguredSources().isNotEmpty()

    override fun getConfiguredSources(): Set<LocalSaveSource> = buildSet {
        if (prefs.edenTreeUri != null) add(LocalSaveSource.EDEN)
        if (prefs.peanutsTreeUri != null) add(LocalSaveSource.PEANUTS_NATIVE)
    }

    override fun getFolderUriString(source: LocalSaveSource): String? =
        treeUriFor(source)?.toString()

    override fun setFolderUriString(uriString: String?, source: LocalSaveSource) {
        val parsed = uriString?.takeIf { it.isNotBlank() }?.toUri()
        when (source) {
            LocalSaveSource.EDEN -> prefs.edenTreeUri = parsed
            LocalSaveSource.PEANUTS_NATIVE -> prefs.peanutsTreeUri = parsed
        }
    }

    override suspend fun scanLocalSaves(): Pair<String, List<LocalSaveGame>> =
        withContext(Dispatchers.IO) {
            val configured = getConfiguredSources()
            if (configured.isEmpty()) return@withContext "" to emptyList()

            var edenProfileId = ""
            val candidatesByTitle = mutableMapOf<String, MutableList<LocalSaveGame>>()

            for (source in configured) {
                val uri = treeUriFor(source) ?: continue
                val (profileId, saves) = if (source == LocalSaveSource.PEANUTS_NATIVE && ShizukuPeanutsClient.isShizukuUri(uri)) {
                    "" to shizukuPeanutsClient.scanLocalSaves()
                } else {
                    storageManager.scanLocalSaves(uri, source)
                }
                if (source == LocalSaveSource.EDEN && profileId.isNotBlank()) {
                    edenProfileId = profileId
                }
                for (save in saves) {
                    candidatesByTitle.getOrPut(save.titleId.uppercase()) { mutableListOf() }.add(save)
                }
            }

            val syncedRecords = prefs.getSyncedRecords()
            val merged = candidatesByTitle.mapNotNull { (titleId, candidates) ->
                SaveReconciler.selectLatestLocalSave(
                    candidates = candidates,
                    configuredSourceCount = configured.size,
                    syncedRecord = syncedRecords[titleId]
                )
            }.sortedByDescending { it.lastModifiedMs }

            edenProfileId to merged
        }

    override suspend fun scanSingleSave(titleId: String): LocalSaveGame? =
        withContext(Dispatchers.IO) {
            val configured = getConfiguredSources()
            if (configured.isEmpty()) return@withContext null

            val candidates = configured.mapNotNull { source ->
                val uri = treeUriFor(source) ?: return@mapNotNull null
                if (source == LocalSaveSource.PEANUTS_NATIVE && ShizukuPeanutsClient.isShizukuUri(uri)) {
                    shizukuPeanutsClient.scanSingleSave(titleId)
                } else {
                    storageManager.scanSingleTitleSave(uri, titleId, source)
                }
            }
            SaveReconciler.selectLatestLocalSave(
                candidates = candidates,
                configuredSourceCount = configured.size,
                syncedRecord = prefs.getSyncedRecords()[titleId.uppercase()]
            )
        }

    override suspend fun exportSaveToZip(
        titleId: String,
        destZipFile: File,
        preferredSource: LocalSaveSource?
    ): Boolean = withContext(Dispatchers.IO) {
        val configured = getConfiguredSources()
        if (configured.isEmpty()) return@withContext false

        val orderedSources = buildList {
            if (preferredSource != null && preferredSource in configured) {
                add(preferredSource)
            }
            configured.filterTo(this) { it != preferredSource }
        }

        for (source in orderedSources) {
            val uri = treeUriFor(source) ?: continue
            val exported = if (source == LocalSaveSource.PEANUTS_NATIVE && ShizukuPeanutsClient.isShizukuUri(uri)) {
                shizukuPeanutsClient.exportSaveToZip(titleId, destZipFile)
            } else {
                storageManager.exportTitleSaveToZip(uri, titleId, destZipFile, source)
            }
            if (exported) return@withContext true
        }
        false
    }

    override suspend fun importSaveFromZip(
        titleId: String,
        srcZipFile: File,
        targets: Set<LocalSaveSource>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val activeTargets = targets.filter { treeUriFor(it) != null }
        if (activeTargets.isEmpty()) {
            return@withContext Result.failure(
                IllegalStateException("No hay carpetas locales vinculadas para escribir $titleId")
            )
        }

        val failedDetails = mutableListOf<String>()
        for (source in activeTargets) {
            val uri = treeUriFor(source) ?: continue
            val errorMsg = try {
                val ok = if (source == LocalSaveSource.PEANUTS_NATIVE && ShizukuPeanutsClient.isShizukuUri(uri)) {
                    shizukuPeanutsClient.importSaveFromZip(titleId, srcZipFile) { backupZip ->
                        storageManager.saveBackupFromZip(titleId, backupZip)
                    }
                } else {
                    storageManager.importTitleSaveFromZip(uri, titleId, srcZipFile, source)
                }
                if (ok) null else sourceLabel(source)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                "${sourceLabel(source)} (${e.message ?: "error de acceso"})"
            }
            if (errorMsg != null) {
                failedDetails.add(errorMsg)
            }
        }

        if (failedDetails.isEmpty()) {
            Result.success(Unit)
        } else {
            Result.failure(
                IllegalStateException(
                    "No se pudo escribir la partida de $titleId en: ${failedDetails.joinToString(", ")}"
                )
            )
        }
    }

    override suspend fun restoreLatestBackup(titleId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            val latestBackup = storageManager.getLatestLocalBackupFile(titleId)
                ?: return@withContext Result.failure(
                    IllegalStateException("No existe copia de seguridad local para $titleId")
                )
            importSaveFromZip(titleId, latestBackup, getConfiguredSources())
        }

    override fun getLocalBackupCount(titleId: String): Int =
        storageManager.getLocalBackupCount(titleId)

    private fun treeUriFor(source: LocalSaveSource): Uri? = when (source) {
        LocalSaveSource.EDEN -> prefs.edenTreeUri
        LocalSaveSource.PEANUTS_NATIVE -> prefs.peanutsTreeUri
    }

    private fun sourceLabel(source: LocalSaveSource): String = when (source) {
        LocalSaveSource.EDEN -> "Eden"
        LocalSaveSource.PEANUTS_NATIVE -> "Peanuts Native"
    }
}
