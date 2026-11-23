package com.jotasoft.edensavesync.domain.model

data class LocalSaveGame(
    val titleId: String,
    val userProfileId: String,
    val lastModifiedMs: Long,
    val totalSizeBytes: Long,
    val fileCount: Int,
    val contentHash: String,
    val source: LocalSaveSource = LocalSaveSource.EDEN,
    val isMirroredAcrossAllConfiguredSources: Boolean = true
)
