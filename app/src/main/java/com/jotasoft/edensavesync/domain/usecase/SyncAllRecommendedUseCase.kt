package com.jotasoft.edensavesync.domain.usecase

import com.jotasoft.edensavesync.domain.model.SyncSummaryResult
import com.jotasoft.edensavesync.domain.repository.SaveSyncRepository
import javax.inject.Inject

class SyncAllRecommendedUseCase @Inject constructor(
    private val saveSyncRepository: SaveSyncRepository
) {
    suspend operator fun invoke(
        ensureLocalStability: Boolean = false,
        onProgress: (String) -> Unit = {}
    ): SyncSummaryResult = saveSyncRepository.syncAllRecommended(
        ensureLocalStability = ensureLocalStability,
        onProgress = onProgress
    )
}
