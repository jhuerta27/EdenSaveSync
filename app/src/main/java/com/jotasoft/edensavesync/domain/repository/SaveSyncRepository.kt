package com.jotasoft.edensavesync.domain.repository

import com.jotasoft.edensavesync.domain.model.GameSyncItem
import com.jotasoft.edensavesync.domain.model.SyncSummaryResult

interface SaveSyncRepository {
    suspend fun scanGames(): Pair<String, List<GameSyncItem>>
    suspend fun syncAllRecommended(onProgress: (String) -> Unit): SyncSummaryResult
    suspend fun uploadSingleGame(item: GameSyncItem): Result<Unit>
    suspend fun downloadSingleGame(item: GameSyncItem): Result<Unit>
    suspend fun restoreLatestBackup(item: GameSyncItem): Result<Unit>
    fun renameGame(titleId: String, customName: String)
}
