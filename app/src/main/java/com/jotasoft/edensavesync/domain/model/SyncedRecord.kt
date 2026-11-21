package com.jotasoft.edensavesync.domain.model

data class SyncedRecord(
    val titleId: String,
    val contentHash: String,
    val remoteTimestampMs: Long,
    val localTimestampAtSyncMs: Long
)
