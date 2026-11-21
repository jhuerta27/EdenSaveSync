package com.jotasoft.edensavesync

import com.jotasoft.edensavesync.data.repository.SaveReconciler
import com.jotasoft.edensavesync.domain.model.LocalSaveGame
import com.jotasoft.edensavesync.domain.model.RemoteSaveGame
import com.jotasoft.edensavesync.domain.model.SyncRecommendation
import com.jotasoft.edensavesync.domain.model.SyncedRecord
import org.junit.Assert.assertEquals
import org.junit.Test

class SaveReconcilerTest {

    private val subnauticaTitleId = "010041100D536000"

    @Test
    fun evaluateRecommendation_whenHashesMatch_isInSync() {
        val local = LocalSaveGame(
            titleId = subnauticaTitleId,
            userProfileId = "00000001000000000000000000000000",
            lastModifiedMs = 0L,
            totalSizeBytes = 50_000_000L,
            fileCount = 850,
            contentHash = "hash_subnautica_abc"
        )
        val remote = RemoteSaveGame(
            titleId = subnauticaTitleId,
            gameName = "Subnautica",
            driveFileId = "drive_file_123",
            lastModifiedMs = 1_700_000_000_000L,
            totalSizeBytes = 50_000_000L,
            fileCount = 850,
            contentHash = "hash_subnautica_abc",
            sourceDeviceName = "AYN Odin 2 Portal",
            uploadedAtMs = 1_700_000_000_000L
        )

        val result = SaveReconciler.evaluateRecommendation(local, remote, syncedRecord = null)
        assertEquals(SyncRecommendation.IN_SYNC, result)
    }

    @Test
    fun evaluateRecommendation_whenLocalTimestampIsZero_andMatchesSyncedRecord_isInSync() {
        // Simula Samsung Galaxy Z Fold 8 con Eden DocumentsProvider donde COLUMN_LAST_MODIFIED = 0L
        val local = LocalSaveGame(
            titleId = subnauticaTitleId,
            userProfileId = "00000001000000000000000000000000",
            lastModifiedMs = 0L,
            totalSizeBytes = 50_000_000L,
            fileCount = 850,
            contentHash = "hash_local_fold8"
        )
        val remote = RemoteSaveGame(
            titleId = subnauticaTitleId,
            gameName = "Subnautica",
            driveFileId = "drive_file_123",
            lastModifiedMs = 1_700_000_000_000L,
            totalSizeBytes = 50_000_000L,
            fileCount = 850,
            contentHash = "hash_remote_odin",
            sourceDeviceName = "AYN Odin 2 Portal",
            uploadedAtMs = 1_700_000_000_000L
        )
        val syncedRecord = SyncedRecord(
            titleId = subnauticaTitleId,
            contentHash = "hash_local_fold8",
            remoteTimestampMs = 1_700_000_000_000L,
            localTimestampAtSyncMs = 1_700_000_000_000L
        )

        val result = SaveReconciler.evaluateRecommendation(local, remote, syncedRecord)
        // No debe forzar descarga repetida ("0 UP / 2 DOWN") porque el contenido local no ha cambiado
        assertEquals(SyncRecommendation.IN_SYNC, result)
    }

    @Test
    fun evaluateRecommendation_whenRemoteUpdatedAfterSync_recommendsDownload() {
        val local = LocalSaveGame(
            titleId = subnauticaTitleId,
            userProfileId = "00000001000000000000000000000000",
            lastModifiedMs = 0L,
            totalSizeBytes = 50_000_000L,
            fileCount = 850,
            contentHash = "hash_local_fold8"
        )
        // La Odin subió una partida más reciente posteriormente
        val remote = RemoteSaveGame(
            titleId = subnauticaTitleId,
            gameName = "Subnautica",
            driveFileId = "drive_file_123",
            lastModifiedMs = 1_700_050_000_000L, // 50s después
            totalSizeBytes = 51_000_000L,
            fileCount = 855,
            contentHash = "hash_remote_odin_v2",
            sourceDeviceName = "AYN Odin 2 Portal",
            uploadedAtMs = 1_700_050_000_000L
        )
        val syncedRecord = SyncedRecord(
            titleId = subnauticaTitleId,
            contentHash = "hash_local_fold8",
            remoteTimestampMs = 1_700_000_000_000L,
            localTimestampAtSyncMs = 1_700_000_000_000L
        )

        val result = SaveReconciler.evaluateRecommendation(local, remote, syncedRecord)
        assertEquals(SyncRecommendation.DOWNLOAD_FROM_CLOUD, result)
    }

    @Test
    fun evaluateRecommendation_whenUserPlayedLocallyOnFold8_recommendsUpload() {
        // El usuario avanzó partida en el Fold 8 (su hash cambió respecto al registro sincronizado)
        val local = LocalSaveGame(
            titleId = subnauticaTitleId,
            userProfileId = "00000001000000000000000000000000",
            lastModifiedMs = 0L, // Provider no reporta modTime
            totalSizeBytes = 52_000_000L,
            fileCount = 860,
            contentHash = "hash_local_fold8_new_progress"
        )
        val remote = RemoteSaveGame(
            titleId = subnauticaTitleId,
            gameName = "Subnautica",
            driveFileId = "drive_file_123",
            lastModifiedMs = 1_700_000_000_000L,
            totalSizeBytes = 50_000_000L,
            fileCount = 850,
            contentHash = "hash_remote_odin",
            sourceDeviceName = "AYN Odin 2 Portal",
            uploadedAtMs = 1_700_000_000_000L
        )
        val syncedRecord = SyncedRecord(
            titleId = subnauticaTitleId,
            contentHash = "hash_local_fold8_original",
            remoteTimestampMs = 1_700_000_000_000L,
            localTimestampAtSyncMs = 1_700_000_000_000L
        )

        val result = SaveReconciler.evaluateRecommendation(local, remote, syncedRecord)
        // Detecta que el cambio fue local porque la nube no ha avanzado
        assertEquals(SyncRecommendation.UPLOAD_TO_CLOUD, result)
    }

    @Test
    fun evaluateRecommendation_whenOnlyLocalExists_recommendsUpload() {
        val local = LocalSaveGame(
            titleId = subnauticaTitleId,
            userProfileId = "00000001000000000000000000000000",
            lastModifiedMs = 1_700_000_000_000L,
            totalSizeBytes = 50_000_000L,
            fileCount = 850,
            contentHash = "hash_local"
        )
        val result = SaveReconciler.evaluateRecommendation(local, remote = null, syncedRecord = null)
        assertEquals(SyncRecommendation.UPLOAD_TO_CLOUD, result)
    }

    @Test
    fun evaluateRecommendation_whenOnlyRemoteExists_recommendsDownload() {
        val remote = RemoteSaveGame(
            titleId = subnauticaTitleId,
            gameName = "Subnautica",
            driveFileId = "drive_file_123",
            lastModifiedMs = 1_700_000_000_000L,
            totalSizeBytes = 50_000_000L,
            fileCount = 850,
            contentHash = "hash_remote",
            sourceDeviceName = "AYN Odin 2 Portal",
            uploadedAtMs = 1_700_000_000_000L
        )
        val result = SaveReconciler.evaluateRecommendation(local = null, remote = remote, syncedRecord = null)
        assertEquals(SyncRecommendation.DOWNLOAD_FROM_CLOUD, result)
    }

    @Test
    fun mergeLocalAndRemote_sortsDownloadFirstThenUploadThenInSync() {
        val locals = listOf(
            LocalSaveGame("0100000000000001", "prof", 100L, 10L, 1, "h1"), // Upload
            LocalSaveGame("0100000000000002", "prof", 100L, 10L, 1, "h2")  // In Sync
        )
        val remotes = listOf(
            RemoteSaveGame("0100000000000002", "Game 2", "f2", 100L, 10L, 1, "h2", "Dev", 100L), // In Sync
            RemoteSaveGame("0100000000000003", "Game 3", "f3", 100L, 10L, 1, "h3", "Dev", 100L)  // Download
        )

        val merged = SaveReconciler.mergeLocalAndRemote(
            locals = locals,
            remotes = remotes,
            syncedRecords = emptyMap(),
            customNames = emptyMap(),
            getLocalBackupCount = { 0 }
        )

        assertEquals(3, merged.size)
        assertEquals(SyncRecommendation.DOWNLOAD_FROM_CLOUD, merged[0].recommendation)
        assertEquals("0100000000000003", merged[0].titleId)
        assertEquals(SyncRecommendation.UPLOAD_TO_CLOUD, merged[1].recommendation)
        assertEquals("0100000000000001", merged[1].titleId)
        assertEquals(SyncRecommendation.IN_SYNC, merged[2].recommendation)
        assertEquals("0100000000000002", merged[2].titleId)
    }
}
