package com.jotasoft.edensavesync.domain.repository

import com.jotasoft.edensavesync.domain.model.LocalSaveGame
import com.jotasoft.edensavesync.domain.model.RemoteSaveGame
import java.io.File

interface CloudSyncRepository {
    fun isConfigured(): Boolean
    suspend fun listRemoteSaves(): List<RemoteSaveGame>
    suspend fun uploadSaveZip(
        localSave: LocalSaveGame,
        gameName: String,
        deviceName: String,
        zipFile: File,
        existingDriveFileId: String?
    ): RemoteSaveGame
    suspend fun downloadSaveZip(driveFileId: String, destZipFile: File)
}
