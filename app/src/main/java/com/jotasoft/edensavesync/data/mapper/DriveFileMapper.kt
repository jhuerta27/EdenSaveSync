package com.jotasoft.edensavesync.data.mapper

import com.jotasoft.edensavesync.data.cloud.model.DriveFileMetadata
import com.jotasoft.edensavesync.domain.model.RemoteSaveGame

object DriveFileMapper {

    fun toRemoteSaveGame(metadata: DriveFileMetadata): RemoteSaveGame? {
        val props = metadata.appProperties.ifEmpty { metadata.properties }
        val titleId = props["titleId"]
            ?: metadata.name.removeSuffix(".zip").takeIf { it.length == 16 }
            ?: return null

        val gameName = props["gameName"].orEmpty().ifBlank {
            metadata.name.removeSuffix(".zip")
        }
        val contentHash = props["contentHash"].orEmpty()
        val lastModifiedMs = props["lastModifiedMs"]?.toLongOrNull() ?: 0L
        val fileCount = props["fileCount"]?.toIntOrNull() ?: 0
        val totalSizeBytes = props["totalSizeBytes"]?.toLongOrNull() ?: metadata.sizeBytes
        val sourceDevice = props["sourceDeviceName"].orEmpty()
        val uploadedAtMs = props["uploadedAtMs"]?.toLongOrNull() ?: 0L

        return RemoteSaveGame(
            titleId = titleId.uppercase(),
            gameName = gameName,
            driveFileId = metadata.id,
            lastModifiedMs = lastModifiedMs,
            totalSizeBytes = totalSizeBytes,
            fileCount = fileCount,
            contentHash = contentHash,
            sourceDeviceName = sourceDevice,
            uploadedAtMs = uploadedAtMs
        )
    }
}
