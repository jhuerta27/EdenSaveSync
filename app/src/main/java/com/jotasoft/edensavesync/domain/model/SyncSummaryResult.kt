package com.jotasoft.edensavesync.domain.model

data class SyncSummaryResult(
    val uploadedCount: Int,
    val downloadedCount: Int,
    val skippedCount: Int,
    val errors: List<String>,
    val deferredCount: Int = 0
)
