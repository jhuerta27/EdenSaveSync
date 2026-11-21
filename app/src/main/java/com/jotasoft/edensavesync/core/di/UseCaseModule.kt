package com.jotasoft.edensavesync.core.di

import com.jotasoft.edensavesync.domain.repository.AuthRepository
import com.jotasoft.edensavesync.domain.repository.SaveSyncRepository
import com.jotasoft.edensavesync.domain.usecase.DownloadSaveGameUseCase
import com.jotasoft.edensavesync.domain.usecase.PairingCodeUseCase
import com.jotasoft.edensavesync.domain.usecase.RestoreBackupUseCase
import com.jotasoft.edensavesync.domain.usecase.ScanGamesUseCase
import com.jotasoft.edensavesync.domain.usecase.SyncAllRecommendedUseCase
import com.jotasoft.edensavesync.domain.usecase.UploadSaveGameUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    @Singleton
    fun provideScanGamesUseCase(
        saveSyncRepository: SaveSyncRepository
    ): ScanGamesUseCase = ScanGamesUseCase(saveSyncRepository)

    @Provides
    @Singleton
    fun provideSyncAllRecommendedUseCase(
        saveSyncRepository: SaveSyncRepository
    ): SyncAllRecommendedUseCase = SyncAllRecommendedUseCase(saveSyncRepository)

    @Provides
    @Singleton
    fun provideUploadSaveGameUseCase(
        saveSyncRepository: SaveSyncRepository
    ): UploadSaveGameUseCase = UploadSaveGameUseCase(saveSyncRepository)

    @Provides
    @Singleton
    fun provideDownloadSaveGameUseCase(
        saveSyncRepository: SaveSyncRepository
    ): DownloadSaveGameUseCase = DownloadSaveGameUseCase(saveSyncRepository)

    @Provides
    @Singleton
    fun provideRestoreBackupUseCase(
        saveSyncRepository: SaveSyncRepository
    ): RestoreBackupUseCase = RestoreBackupUseCase(saveSyncRepository)

    @Provides
    @Singleton
    fun providePairingCodeUseCase(
        authRepository: AuthRepository
    ): PairingCodeUseCase = PairingCodeUseCase(authRepository)
}
