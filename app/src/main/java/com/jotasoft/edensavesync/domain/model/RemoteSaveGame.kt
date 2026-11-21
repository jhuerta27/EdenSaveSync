package com.jotasoft.edensavesync.domain.model

data class RemoteSaveGame(
    val titleId: String,
    val gameName: String,
    val driveFileId: String,
    val lastModifiedMs: Long,
    val totalSizeBytes: Long,
    val fileCount: Int,
    val contentHash: String,
    val sourceDeviceName: String,
    val uploadedAtMs: Long
)
