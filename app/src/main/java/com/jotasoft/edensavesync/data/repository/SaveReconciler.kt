package com.jotasoft.edensavesync.data.repository

import com.jotasoft.edensavesync.data.catalog.SwitchTitleCatalog
import com.jotasoft.edensavesync.domain.model.GameSyncItem
import com.jotasoft.edensavesync.domain.model.LocalSaveGame
import com.jotasoft.edensavesync.domain.model.RemoteSaveGame
import com.jotasoft.edensavesync.domain.model.SyncRecommendation
import com.jotasoft.edensavesync.domain.model.SyncedRecord

internal object SaveReconciler {

    fun selectLatestLocalSave(
        candidates: List<LocalSaveGame>,
        configuredSourceCount: Int,
        syncedRecord: SyncedRecord?
    ): LocalSaveGame? {
        if (candidates.isEmpty()) return null
        val distinctSourcesCount = candidates.map { it.source }.distinct().size
        val distinctHashes = candidates.map { it.contentHash }.distinct()
        val allSameHash = distinctHashes.size == 1 && distinctHashes.first().isNotBlank()
        val isMirrored = distinctSourcesCount >= configuredSourceCount.coerceAtLeast(1) &&
            (configuredSourceCount <= 1 || allSameHash)

        val winner = when {
            candidates.size == 1 -> candidates.first()
            allSameHash -> candidates.maxByOrNull { it.lastModifiedMs } ?: candidates.first()
            syncedRecord != null && syncedRecord.contentHash.isNotBlank() -> {
                val changed = candidates.filter { it.contentHash != syncedRecord.contentHash }
                val unchanged = candidates.filter { it.contentHash == syncedRecord.contentHash }
                if (changed.isNotEmpty() && unchanged.isNotEmpty()) {
                    changed.maxByOrNull { it.lastModifiedMs } ?: changed.first()
                } else {
                    candidates.maxByOrNull { it.lastModifiedMs } ?: candidates.first()
                }
            }
            else -> candidates.maxByOrNull { it.lastModifiedMs } ?: candidates.first()
        }

        return winner.copy(isMirroredAcrossAllConfiguredSources = isMirrored)
    }

    fun mergeLocalAndRemote(
        locals: List<LocalSaveGame>,
        remotes: List<RemoteSaveGame>,
        syncedRecords: Map<String, SyncedRecord>,
        customNames: Map<String, String>,
        getLocalBackupCount: (String) -> Int
    ): List<GameSyncItem> {
        val localMap = locals.associateBy { it.titleId.uppercase() }
        val remoteMap = remotes.associateBy { it.titleId.uppercase() }
        val allTitleIds = (localMap.keys + remoteMap.keys).distinct()

        return allTitleIds.map { titleId ->
            val local = localMap[titleId]
            val remote = remoteMap[titleId]
            val syncedRecord = syncedRecords[titleId]

            val displayName = customNames[titleId]?.takeIf { it.isNotBlank() }
                ?: SwitchTitleCatalog.resolveTitle(titleId)
                ?: remote?.gameName?.takeIf {
                    it.isNotBlank() && !it.startsWith("Juego de Switch") && !it.startsWith("Switch Game")
                }
                ?: SwitchTitleCatalog.resolveName(titleId, customNames)

            val recommendation = evaluateRecommendation(local, remote, syncedRecord)

            GameSyncItem(
                titleId = titleId,
                displayName = displayName,
                localSave = local,
                remoteSave = remote,
                recommendation = recommendation,
                localBackupsCount = getLocalBackupCount(titleId)
            )
        }.sortedWith(
            compareBy<GameSyncItem> {
                when (it.recommendation) {
                    SyncRecommendation.DOWNLOAD_FROM_CLOUD -> 0
                    SyncRecommendation.UPLOAD_TO_CLOUD -> 1
                    SyncRecommendation.IN_SYNC -> 2
                }
            }.thenByDescending {
                maxOf(it.localSave?.lastModifiedMs ?: 0L, it.remoteSave?.lastModifiedMs ?: 0L)
            }
        )
    }

    fun evaluateRecommendation(
        local: LocalSaveGame?,
        remote: RemoteSaveGame?,
        syncedRecord: SyncedRecord?
    ): SyncRecommendation {
        val base = when {
            local != null && remote == null -> SyncRecommendation.UPLOAD_TO_CLOUD
            local == null && remote != null -> SyncRecommendation.DOWNLOAD_FROM_CLOUD
            local != null && remote != null -> {
                if (local.contentHash.isNotBlank() && local.contentHash == remote.contentHash) {
                    SyncRecommendation.IN_SYNC
                } else if (
                    syncedRecord != null &&
                    syncedRecord.contentHash.isNotBlank() &&
                    syncedRecord.contentHash == local.contentHash
                ) {
                    // El archivo local no ha cambiado desde la última sincronización
                    if (remote.lastModifiedMs > syncedRecord.remoteTimestampMs + 2_000L) {
                        SyncRecommendation.DOWNLOAD_FROM_CLOUD
                    } else {
                        SyncRecommendation.IN_SYNC
                    }
                } else {
                    val effectiveLocalMs = if (local.lastModifiedMs > 0L) {
                        local.lastModifiedMs
                    } else {
                        syncedRecord?.localTimestampAtSyncMs ?: 0L
                    }

                    when {
                        effectiveLocalMs > remote.lastModifiedMs + 2_000L ->
                            SyncRecommendation.UPLOAD_TO_CLOUD
                        remote.lastModifiedMs > effectiveLocalMs + 2_000L &&
                            (syncedRecord == null || remote.lastModifiedMs > syncedRecord.remoteTimestampMs + 2_000L) ->
                            SyncRecommendation.DOWNLOAD_FROM_CLOUD
                        syncedRecord != null && remote.lastModifiedMs <= syncedRecord.remoteTimestampMs + 2_000L ->
                            SyncRecommendation.UPLOAD_TO_CLOUD
                        else -> SyncRecommendation.IN_SYNC
                    }
                }
            }
            else -> SyncRecommendation.IN_SYNC
        }

        if (base == SyncRecommendation.IN_SYNC &&
            local != null &&
            !local.isMirroredAcrossAllConfiguredSources
        ) {
            return if (remote != null) {
                SyncRecommendation.DOWNLOAD_FROM_CLOUD
            } else {
                SyncRecommendation.UPLOAD_TO_CLOUD
            }
        }
        return base
    }
}
