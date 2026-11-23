package com.jotasoft.edensavesync.domain.repository

import com.jotasoft.edensavesync.domain.model.LocalSaveGame
import com.jotasoft.edensavesync.domain.model.LocalSaveSource
import java.io.File

interface EdenStorageRepository {
    fun isFolderConfigured(): Boolean
    fun getConfiguredSources(): Set<LocalSaveSource>
    fun getFolderUriString(source: LocalSaveSource = LocalSaveSource.EDEN): String?
    fun setFolderUriString(uriString: String?, source: LocalSaveSource = LocalSaveSource.EDEN)
    suspend fun scanLocalSaves(): Pair<String, List<LocalSaveGame>>
    suspend fun scanSingleSave(titleId: String): LocalSaveGame?
    suspend fun exportSaveToZip(
        titleId: String,
        destZipFile: File,
        preferredSource: LocalSaveSource? = null
    ): Boolean
    suspend fun importSaveFromZip(
        titleId: String,
        srcZipFile: File,
        targets: Set<LocalSaveSource> = getConfiguredSources()
    ): Result<Unit>
    suspend fun restoreLatestBackup(titleId: String): Result<Unit>
    fun getLocalBackupCount(titleId: String): Int
    fun consumeLastScanWarning(): String? = null
}
