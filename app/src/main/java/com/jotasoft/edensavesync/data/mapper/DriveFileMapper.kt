package com.jotasoft.edensavesync.data.mapper

import com.jotasoft.edensavesync.data.cloud.model.DriveFileMetadata
import com.jotasoft.edensavesync.domain.model.RemoteSaveGame
import org.json.JSONObject
import java.time.Instant

object DriveFileMapper {

    fun fromJson(fileObj: JSONObject): RemoteSaveGame? {
        val propsObj = fileObj.optJSONObject("appProperties")
        val appProps = if (propsObj != null) {
            buildMap {
                propsObj.keys().forEach { key ->
                    put(key, propsObj.optString(key, ""))
                }
            }
        } else {
            emptyMap()
        }

        val metadata = DriveFileMetadata(
            id = fileObj.getString("id"),
            name = fileObj.optString("name", ""),
            modifiedTime = fileObj.optString("modifiedTime", "").takeIf { it.isNotBlank() },
            sizeBytes = fileObj.optLong("size", 0L),
            appProperties = appProps
        )
        return toRemoteSaveGame(metadata)
    }

    fun toRemoteSaveGame(metadata: DriveFileMetadata): RemoteSaveGame? {
        if (!metadata.name.endsWith(".zip", ignoreCase = true)) return null

        val props = metadata.appProperties.ifEmpty { metadata.properties }
        val rawTitleId = props["titleId"]?.takeIf { it.isNotBlank() }
            ?: metadata.name.substringBeforeLast('.')
        val titleId = rawTitleId.uppercase().takeIf { it.length == 16 } ?: return null

        val driveModTimeMs = metadata.modifiedTime?.let {
            runCatching { Instant.parse(it).toEpochMilli() }.getOrNull()
        } ?: 0L

        val lastModifiedMs = props["lastModifiedMs"]?.toLongOrNull()
            ?.takeIf { it > 0L }
            ?: driveModTimeMs

        val totalSizeBytes = props["totalSizeBytes"]?.toLongOrNull() ?: metadata.sizeBytes
        if (metadata.sizeBytes in 1..22L && totalSizeBytes <= 22L) return null

        val fileCount = props["fileCount"]?.toIntOrNull() ?: 1
        val contentHash = props["contentHash"].orEmpty()
        val sourceDevice = props["sourceDeviceName"]?.takeIf { it.isNotBlank() } ?: "Google Drive"
        val gameName = props["gameName"].orEmpty()
        val uploadedAtMs = props["uploadedAtMs"]?.toLongOrNull() ?: driveModTimeMs

        return RemoteSaveGame(
            titleId = titleId,
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
