package com.jotasoft.edensavesync.domain.model

data class GameSyncItem(
    val titleId: String,
    val displayName: String,
    val localSave: LocalSaveGame?,
    val remoteSave: RemoteSaveGame?,
    val recommendation: SyncRecommendation,
    val localBackupsCount: Int
)
