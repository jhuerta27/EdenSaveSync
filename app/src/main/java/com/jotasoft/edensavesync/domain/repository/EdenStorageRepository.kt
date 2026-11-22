package com.jotasoft.edensavesync.domain.repository

import com.jotasoft.edensavesync.domain.model.LocalSaveGame
import java.io.File

interface EdenStorageRepository {
    fun isFolderConfigured(): Boolean
    fun getFolderUriString(): String?
    fun setFolderUriString(uriString: String)
    suspend fun scanLocalSaves(): Pair<String, List<LocalSaveGame>>
    suspend fun scanSingleSave(titleId: String): LocalSaveGame?
    suspend fun exportSaveToZip(titleId: String, destZipFile: File): Boolean
    suspend fun importSaveFromZip(titleId: String, srcZipFile: File): Boolean
    suspend fun restoreLatestBackup(titleId: String): Boolean
    fun getLocalBackupCount(titleId: String): Int
}
