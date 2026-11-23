package com.jotasoft.edensavesync

import com.jotasoft.edensavesync.data.repository.SaveSyncOperations
import com.jotasoft.edensavesync.domain.model.GameSyncItem
import com.jotasoft.edensavesync.domain.model.LocalSaveGame
import com.jotasoft.edensavesync.domain.model.LocalSaveSource
import com.jotasoft.edensavesync.domain.model.RemoteSaveGame
import com.jotasoft.edensavesync.domain.model.SyncRecommendation
import com.jotasoft.edensavesync.domain.repository.CloudSyncRepository
import com.jotasoft.edensavesync.domain.repository.EdenStorageRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SaveSyncMultiSourceTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val ffResonanceTitleId = "0100606026F12000"

    private class FakeStorageRepo(
        initialConfigured: Set<LocalSaveSource>
    ) : EdenStorageRepository {
        val uris = mutableMapOf<LocalSaveSource, String>().apply {
            initialConfigured.forEach { put(it, "content://fake/${it.name.lowercase()}") }
        }
        val exportedFrom = mutableListOf<LocalSaveSource?>()
        val importedTo = mutableListOf<Set<LocalSaveSource>>()
        val restoredTo = mutableListOf<Set<LocalSaveSource>>()
        var scanSingleResult: LocalSaveGame? = null

        override fun isFolderConfigured(): Boolean = uris.isNotEmpty()
        override fun getConfiguredSources(): Set<LocalSaveSource> = uris.keys.toSet()
        override fun getFolderUriString(source: LocalSaveSource): String? = uris[source]
        override fun setFolderUriString(uriString: String?, source: LocalSaveSource) {
            if (uriString.isNullOrBlank()) uris.remove(source) else uris[source] = uriString
        }

        override suspend fun scanLocalSaves(): Pair<String, List<LocalSaveGame>> = "" to emptyList()
        override suspend fun scanSingleSave(titleId: String): LocalSaveGame? = scanSingleResult

        override suspend fun exportSaveToZip(
            titleId: String,
            destZipFile: File,
            preferredSource: LocalSaveSource?
        ): Boolean {
            exportedFrom.add(preferredSource)
            destZipFile.writeText("fake-zip-content")
            return true
        }

        override suspend fun importSaveFromZip(
            titleId: String,
            srcZipFile: File,
            targets: Set<LocalSaveSource>
        ): Result<Unit> {
            importedTo.add(targets)
            return Result.success(Unit)
        }

        override suspend fun restoreLatestBackup(titleId: String): Result<Unit> {
            restoredTo.add(getConfiguredSources())
            return Result.success(Unit)
        }

        override fun getLocalBackupCount(titleId: String): Int = 1
    }

    private class FakeCloudRepo : CloudSyncRepository {
        val uploadedSources = mutableListOf<LocalSaveSource>()
        var downloadedCount = 0

        override fun isConfigured(): Boolean = true
        override suspend fun listRemoteSaves(): List<RemoteSaveGame> = emptyList()

        override suspend fun uploadSaveZip(
            localSave: LocalSaveGame,
            gameName: String,
            deviceName: String,
            zipFile: File,
            existingDriveFileId: String?
        ): RemoteSaveGame {
            uploadedSources.add(localSave.source)
            return RemoteSaveGame(
                titleId = localSave.titleId,
                gameName = gameName,
                driveFileId = existingDriveFileId ?: "drive_new_id",
                lastModifiedMs = localSave.lastModifiedMs,
                totalSizeBytes = localSave.totalSizeBytes,
                fileCount = localSave.fileCount,
                contentHash = localSave.contentHash,
                sourceDeviceName = deviceName,
                uploadedAtMs = localSave.lastModifiedMs
            )
        }

        override suspend fun downloadSaveZip(driveFileId: String, destinationZip: File) {
            downloadedCount++
            destinationZip.writeText("fake-remote-zip")
        }
    }

    @Test
    fun uploadFromPeanutsNative_whenBothConfigured_uploadsToDriveAndMirrorsToEden() = runBlocking {
        val storage = FakeStorageRepo(setOf(LocalSaveSource.EDEN, LocalSaveSource.PEANUTS_NATIVE))
        val cloud = FakeCloudRepo()
        val item = GameSyncItem(
            titleId = ffResonanceTitleId,
            displayName = "Final Fantasy Resonance",
            localSave = LocalSaveGame(
                titleId = ffResonanceTitleId,
                userProfileId = "",
                lastModifiedMs = 1_700_005_000_000L,
                totalSizeBytes = 2048L,
                fileCount = 3,
                contentHash = "hash_peanuts_v2",
                source = LocalSaveSource.PEANUTS_NATIVE,
                isMirroredAcrossAllConfiguredSources = false
            ),
            remoteSave = null,
            recommendation = SyncRecommendation.UPLOAD_TO_CLOUD,
            localBackupsCount = 0
        )

        var recordedHash = ""
        val result = SaveSyncOperations.uploadSingleGame(
            item = item,
            cacheDir = tempFolder.root,
            storageRepo = storage,
            cloudRepo = cloud,
            deviceName = "Z Fold 8",
            onRecordSynced = { _, hash, _, _ -> recordedHash = hash }
        )

        assertTrue(result.isSuccess)
        assertEquals(listOf(LocalSaveSource.PEANUTS_NATIVE), storage.exportedFrom)
        assertEquals(listOf(LocalSaveSource.PEANUTS_NATIVE), cloud.uploadedSources)
        assertEquals(listOf(setOf(LocalSaveSource.EDEN)), storage.importedTo)
        assertEquals("hash_peanuts_v2", recordedHash)
    }

    @Test
    fun uploadFromPeanutsOnly_whenEdenNotConfigured_uploadsWithoutLocalMirroring() = runBlocking {
        val storage = FakeStorageRepo(setOf(LocalSaveSource.PEANUTS_NATIVE))
        val cloud = FakeCloudRepo()
        val item = GameSyncItem(
            titleId = ffResonanceTitleId,
            displayName = "Final Fantasy Resonance",
            localSave = LocalSaveGame(
                titleId = ffResonanceTitleId,
                userProfileId = "",
                lastModifiedMs = 1_700_005_000_000L,
                totalSizeBytes = 2048L,
                fileCount = 3,
                contentHash = "hash_peanuts_only",
                source = LocalSaveSource.PEANUTS_NATIVE,
                isMirroredAcrossAllConfiguredSources = true
            ),
            remoteSave = null,
            recommendation = SyncRecommendation.UPLOAD_TO_CLOUD,
            localBackupsCount = 0
        )

        val result = SaveSyncOperations.uploadSingleGame(
            item = item,
            cacheDir = tempFolder.root,
            storageRepo = storage,
            cloudRepo = cloud,
            deviceName = "Z Fold 8",
            onRecordSynced = { _, _, _, _ -> }
        )

        assertTrue(result.isSuccess)
        assertTrue(storage.isFolderConfigured())
        assertEquals(listOf(LocalSaveSource.PEANUTS_NATIVE), storage.exportedFrom)
        assertTrue(storage.importedTo.isEmpty())
    }

    @Test
    fun download_whenBothConfigured_writesToEdenAndPeanutsNative() = runBlocking {
        val storage = FakeStorageRepo(setOf(LocalSaveSource.EDEN, LocalSaveSource.PEANUTS_NATIVE))
        val cloud = FakeCloudRepo()
        val item = GameSyncItem(
            titleId = ffResonanceTitleId,
            displayName = "Final Fantasy Resonance",
            localSave = null,
            remoteSave = RemoteSaveGame(
                titleId = ffResonanceTitleId,
                gameName = "Final Fantasy Resonance",
                driveFileId = "drive_ff_1",
                lastModifiedMs = 1_700_010_000_000L,
                totalSizeBytes = 4096L,
                fileCount = 4,
                contentHash = "hash_cloud_ff",
                sourceDeviceName = "AYN Odin 2",
                uploadedAtMs = 1_700_010_000_000L
            ),
            recommendation = SyncRecommendation.DOWNLOAD_FROM_CLOUD,
            localBackupsCount = 0
        )

        val result = SaveSyncOperations.downloadSingleGame(
            item = item,
            cacheDir = tempFolder.root,
            storageRepo = storage,
            cloudRepo = cloud,
            getCustomGameNames = { emptyMap() },
            setCustomGameName = { _, _ -> },
            onRecordSynced = { _, _, _, _ -> }
        )

        assertTrue(result.isSuccess)
        assertEquals(1, cloud.downloadedCount)
        assertEquals(
            listOf(setOf(LocalSaveSource.EDEN, LocalSaveSource.PEANUTS_NATIVE)),
            storage.importedTo
        )
    }

    @Test
    fun restoreLatestBackup_whenBothConfigured_restoresToAllConfiguredSources() = runBlocking {
        val storage = FakeStorageRepo(setOf(LocalSaveSource.EDEN, LocalSaveSource.PEANUTS_NATIVE))
        val item = GameSyncItem(
            titleId = ffResonanceTitleId,
            displayName = "Final Fantasy Resonance",
            localSave = null,
            remoteSave = null,
            recommendation = SyncRecommendation.IN_SYNC,
            localBackupsCount = 2
        )

        val result = SaveSyncOperations.restoreLatestBackup(item, storage)

        assertTrue(result.isSuccess)
        assertEquals(
            listOf(setOf(LocalSaveSource.EDEN, LocalSaveSource.PEANUTS_NATIVE)),
            storage.restoredTo
        )
    }

    @Test
    fun unlinkOneSource_preservesOtherConfiguredSource() {
        val storage = FakeStorageRepo(setOf(LocalSaveSource.EDEN, LocalSaveSource.PEANUTS_NATIVE))
        storage.setFolderUriString(null, LocalSaveSource.PEANUTS_NATIVE)

        assertTrue(storage.isFolderConfigured())
        assertEquals(setOf(LocalSaveSource.EDEN), storage.getConfiguredSources())

        storage.setFolderUriString(null, LocalSaveSource.EDEN)
        assertFalse(storage.isFolderConfigured())
    }
}
