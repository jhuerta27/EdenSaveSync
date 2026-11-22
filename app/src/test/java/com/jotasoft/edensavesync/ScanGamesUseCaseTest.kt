package com.jotasoft.edensavesync

import com.jotasoft.edensavesync.domain.model.GameSyncItem
import com.jotasoft.edensavesync.domain.model.SyncRecommendation
import com.jotasoft.edensavesync.domain.model.SyncSummaryResult
import com.jotasoft.edensavesync.domain.repository.SaveSyncRepository
import com.jotasoft.edensavesync.domain.usecase.ScanGamesUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ScanGamesUseCaseTest {

    private class FakeSaveSyncRepository : SaveSyncRepository {
        var gamesToReturn = listOf(
            GameSyncItem(
                titleId = "0100F2C0115B6000",
                displayName = "The Legend of Zelda: Tears of the Kingdom",
                localSave = null,
                remoteSave = null,
                recommendation = SyncRecommendation.IN_SYNC,
                localBackupsCount = 0
            )
        )
        var profileIdToReturn = "00000001000000000000000000000000"

        override suspend fun scanGames(): Pair<String, List<GameSyncItem>> =
            profileIdToReturn to gamesToReturn

        override suspend fun syncAllRecommended(onProgress: (String) -> Unit): SyncSummaryResult =
            SyncSummaryResult(0, 0, 0, emptyList())

        override suspend fun uploadSingleGame(item: GameSyncItem): Result<Unit> = Result.success(Unit)
        override suspend fun downloadSingleGame(item: GameSyncItem): Result<Unit> = Result.success(Unit)
        override suspend fun restoreLatestBackup(item: GameSyncItem): Result<Unit> = Result.success(Unit)
        override fun renameGame(titleId: String, customName: String) {}
    }

    @Test
    fun invoke_returnsRepositoryScannedGames() = runBlocking {
        val fakeRepo = FakeSaveSyncRepository()
        val useCase = ScanGamesUseCase(fakeRepo)

        val (profileId, items) = useCase()

        assertEquals("00000001000000000000000000000000", profileId)
        assertEquals(1, items.size)
        assertEquals("0100F2C0115B6000", items.first().titleId)
        assertEquals(SyncRecommendation.IN_SYNC, items.first().recommendation)
    }
}
