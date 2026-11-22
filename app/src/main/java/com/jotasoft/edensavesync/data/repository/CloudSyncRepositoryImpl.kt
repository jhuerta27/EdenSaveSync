package com.jotasoft.edensavesync.data.repository

import com.jotasoft.edensavesync.data.cloud.GoogleDriveSyncClient
import com.jotasoft.edensavesync.data.storage.AppPreferences
import com.jotasoft.edensavesync.domain.model.LocalSaveGame
import com.jotasoft.edensavesync.domain.model.RemoteSaveGame
import com.jotasoft.edensavesync.domain.repository.CloudSyncRepository
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CloudSyncRepositoryImpl @Inject constructor(
    private val driveClient: GoogleDriveSyncClient,
    private val prefs: AppPreferences
) : CloudSyncRepository {

    override fun isConfigured(): Boolean = prefs.isGoogleDriveConfigured

    override suspend fun listRemoteSaves(): List<RemoteSaveGame> =
        driveClient.listRemoteSaves()

    override suspend fun uploadSaveZip(
        localSave: LocalSaveGame,
        gameName: String,
        deviceName: String,
        zipFile: File,
        existingDriveFileId: String?
    ): RemoteSaveGame = driveClient.uploadSaveZip(
        localSave = localSave,
        gameName = gameName,
        deviceName = deviceName,
        zipFile = zipFile,
        existingDriveFileId = existingDriveFileId
    )

    override suspend fun downloadSaveZip(driveFileId: String, destZipFile: File) =
        driveClient.downloadSaveZip(driveFileId, destZipFile)
}
