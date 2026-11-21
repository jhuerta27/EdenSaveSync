package com.jotasoft.edensavesync.data.cloud.model

data class DriveFileMetadata(
    val id: String,
    val name: String,
    val modifiedTime: String? = null,
    val sizeBytes: Long = 0L,
    val properties: Map<String, String> = emptyMap(),
    val appProperties: Map<String, String> = emptyMap()
)
