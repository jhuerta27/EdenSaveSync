package com.jotasoft.edensavesync.data.repository

import android.net.Uri
import androidx.core.net.toUri
import com.jotasoft.edensavesync.data.storage.AppPreferences
import com.jotasoft.edensavesync.data.storage.EdenStorageManager
import com.jotasoft.edensavesync.data.storage.shizuku.ShizukuPeanutsClient
import com.jotasoft.edensavesync.domain.model.LocalSaveGame
import com.jotasoft.edensavesync.domain.model.LocalSaveSource
import com.jotasoft.edensavesync.domain.model.SyncedRecord
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

    @Volatile
    private var lastScanWarning: String? = null

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

    override fun consumeLastScanWarning(): String? {
        val warning = lastScanWarning
        lastScanWarning = null
        return warning
    }

    override suspend fun scanLocalSaves(): Pair<String, List<LocalSaveGame>> =
        withContext(Dispatchers.IO) {
            val configured = getConfiguredSources()
            if (configured.isEmpty()) {
                lastScanWarning = null
                return@withContext "" to emptyList()
            }

            val outcome = MultiSourceStorageHelper.scanAllSources(
                configuredSources = configured,
                syncedRecords = prefs.getSyncedRecords()
            ) { source ->
                val uri = treeUriFor(source) ?: return@scanAllSources "" to emptyList()
                if (source == LocalSaveSource.PEANUTS_NATIVE && ShizukuPeanutsClient.isShizukuUri(uri)) {
                    "" to shizukuPeanutsClient.scanLocalSaves()
                } else {
                    storageManager.scanLocalSaves(uri, source)
                }
            }
            lastScanWarning = outcome.warningMessage
            outcome.edenProfileId to outcome.saves
        }

    override suspend fun scanSingleSave(titleId: String): LocalSaveGame? =
        withContext(Dispatchers.IO) {
            val configured = getConfiguredSources()
            if (configured.isEmpty()) return@withContext null

            val candidates = mutableListOf<LocalSaveGame>()
            var scannedCount = 0
            for (source in configured) {
                val uri = treeUriFor(source) ?: continue
                try {
                    val save = if (source == LocalSaveSource.PEANUTS_NATIVE && ShizukuPeanutsClient.isShizukuUri(uri)) {
                        shizukuPeanutsClient.scanSingleSave(titleId)
                    } else {
                        storageManager.scanSingleTitleSave(uri, titleId, source)
                    }
                    scannedCount++
                    if (save != null) candidates.add(save)
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                }
            }
            SaveReconciler.selectLatestLocalSave(
                candidates = candidates,
                configuredSourceCount = scannedCount.coerceAtLeast(1),
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
            val exported = try {
                if (source == LocalSaveSource.PEANUTS_NATIVE && ShizukuPeanutsClient.isShizukuUri(uri)) {
                    shizukuPeanutsClient.exportSaveToZip(titleId, destZipFile)
                } else {
                    storageManager.exportTitleSaveToZip(uri, titleId, destZipFile, source)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                false
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
                if (ok) null else MultiSourceStorageHelper.sourceLabel(source)
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                "${MultiSourceStorageHelper.sourceLabel(source)} (${e.message ?: "error de acceso"})"
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
}

internal object MultiSourceStorageHelper {

    data class ScanOutcome(
        val edenProfileId: String,
        val saves: List<LocalSaveGame>,
        val warningMessage: String?
    )

    suspend fun scanAllSources(
        configuredSources: Set<LocalSaveSource>,
        syncedRecords: Map<String, SyncedRecord>,
        scanSource: suspend (LocalSaveSource) -> Pair<String, List<LocalSaveGame>>
    ): ScanOutcome {
        var edenProfileId = ""
        val candidatesByTitle = mutableMapOf<String, MutableList<LocalSaveGame>>()
        val warnings = mutableListOf<String>()
        var succeededSources = 0

        for (source in configuredSources) {
            try {
                val (profileId, saves) = scanSource(source)
                succeededSources++
                if (source == LocalSaveSource.EDEN && profileId.isNotBlank()) {
                    edenProfileId = profileId
                }
                for (save in saves) {
                    candidatesByTitle.getOrPut(save.titleId.uppercase()) { mutableListOf() }.add(save)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                val detail = e.message?.takeIf { it.isNotBlank() } ?: "Error"
                warnings.add("${sourceLabel(source)}: $detail")
            }
        }

        val effectiveSourceCount = succeededSources.coerceAtLeast(1)
        val merged = candidatesByTitle.mapNotNull { (titleId, candidates) ->
            SaveReconciler.selectLatestLocalSave(
                candidates = candidates,
                configuredSourceCount = effectiveSourceCount,
                syncedRecord = syncedRecords[titleId]
            )
        }.sortedByDescending { it.lastModifiedMs }

        return ScanOutcome(
            edenProfileId = edenProfileId,
            saves = merged,
            warningMessage = warnings.takeIf { it.isNotEmpty() }?.joinToString(" • ")
        )
    }

    fun sourceLabel(source: LocalSaveSource): String = when (source) {
        LocalSaveSource.EDEN -> "Eden"
        LocalSaveSource.PEANUTS_NATIVE -> "Peanuts Native"
    }
}
