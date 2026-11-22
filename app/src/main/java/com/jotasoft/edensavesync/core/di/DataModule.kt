package com.jotasoft.edensavesync.core.di

import com.jotasoft.edensavesync.data.repository.AuthRepositoryImpl
import com.jotasoft.edensavesync.data.repository.CloudSyncRepositoryImpl
import com.jotasoft.edensavesync.data.repository.EdenStorageRepositoryImpl
import com.jotasoft.edensavesync.data.repository.SaveSyncRepositoryImpl
import com.jotasoft.edensavesync.domain.repository.AuthRepository
import com.jotasoft.edensavesync.domain.repository.CloudSyncRepository
import com.jotasoft.edensavesync.domain.repository.EdenStorageRepository
import com.jotasoft.edensavesync.domain.repository.SaveSyncRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindEdenStorageRepository(impl: EdenStorageRepositoryImpl): EdenStorageRepository

    @Binds
    @Singleton
    abstract fun bindCloudSyncRepository(impl: CloudSyncRepositoryImpl): CloudSyncRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindSaveSyncRepository(impl: SaveSyncRepositoryImpl): SaveSyncRepository
}
