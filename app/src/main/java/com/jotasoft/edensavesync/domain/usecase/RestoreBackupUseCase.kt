package com.jotasoft.edensavesync.domain.usecase

import com.jotasoft.edensavesync.domain.model.GameSyncItem
import com.jotasoft.edensavesync.domain.repository.SaveSyncRepository
import javax.inject.Inject

class RestoreBackupUseCase @Inject constructor(
    private val saveSyncRepository: SaveSyncRepository
) {
    suspend operator fun invoke(item: GameSyncItem): Result<Unit> =
        saveSyncRepository.restoreLatestBackup(item)
}
