package com.jotasoft.edensavesync.domain.usecase

import com.jotasoft.edensavesync.domain.model.GameSyncItem
import com.jotasoft.edensavesync.domain.repository.SaveSyncRepository
import javax.inject.Inject

class ScanGamesUseCase @Inject constructor(
    private val saveSyncRepository: SaveSyncRepository
) {
    suspend operator fun invoke(): Pair<String, List<GameSyncItem>> =
        saveSyncRepository.scanGames()
}
