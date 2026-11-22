package com.jotasoft.edensavesync.data.repository

import androidx.core.net.toUri
import com.jotasoft.edensavesync.data.storage.AppPreferences
import com.jotasoft.edensavesync.data.storage.EdenStorageManager
import com.jotasoft.edensavesync.domain.model.LocalSaveGame
import com.jotasoft.edensavesync.domain.repository.EdenStorageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EdenStorageRepositoryImpl @Inject constructor(
    private val storageManager: EdenStorageManager,
    private val prefs: AppPreferences
) : EdenStorageRepository {

    override fun isFolderConfigured(): Boolean = prefs.edenTreeUri != null

    override fun getFolderUriString(): String? = prefs.edenTreeUri?.toString()

    override fun setFolderUriString(uriString: String) {
        prefs.edenTreeUri = uriString.toUri()
    }

    override suspend fun scanLocalSaves(): Pair<String, List<LocalSaveGame>> =
        withContext(Dispatchers.IO) {
            val uri = prefs.edenTreeUri ?: return@withContext "" to emptyList()
            storageManager.scanLocalSaves(uri)
        }

    override suspend fun scanSingleSave(titleId: String): LocalSaveGame? =
        withContext(Dispatchers.IO) {
            val uri = prefs.edenTreeUri ?: return@withContext null
            storageManager.scanSingleTitleSave(uri, titleId)
        }

    override suspend fun exportSaveToZip(titleId: String, destZipFile: File): Boolean =
        withContext(Dispatchers.IO) {
            val uri = prefs.edenTreeUri ?: return@withContext false
            storageManager.exportTitleSaveToZip(uri, titleId, destZipFile)
        }

    override suspend fun importSaveFromZip(titleId: String, srcZipFile: File): Boolean =
        withContext(Dispatchers.IO) {
            val uri = prefs.edenTreeUri ?: return@withContext false
            storageManager.importTitleSaveFromZip(uri, titleId, srcZipFile)
        }

    override suspend fun restoreLatestBackup(titleId: String): Boolean =
        withContext(Dispatchers.IO) {
            val uri = prefs.edenTreeUri ?: return@withContext false
            storageManager.restoreLatestLocalBackup(uri, titleId)
        }

    override fun getLocalBackupCount(titleId: String): Int =
        storageManager.getLocalBackupCount(titleId)
}
