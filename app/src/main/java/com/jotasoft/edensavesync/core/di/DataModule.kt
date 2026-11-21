package com.jotasoft.edensavesync.core.di

import android.content.Context
import com.jotasoft.edensavesync.data.cloud.GoogleDriveSyncClient
import com.jotasoft.edensavesync.data.repository.AuthRepositoryImpl
import com.jotasoft.edensavesync.data.repository.CloudSyncRepositoryImpl
import com.jotasoft.edensavesync.data.repository.EdenStorageRepositoryImpl
import com.jotasoft.edensavesync.data.repository.SaveSyncRepositoryImpl
import com.jotasoft.edensavesync.data.storage.AppPreferences
import com.jotasoft.edensavesync.data.storage.EdenStorageManager
import com.jotasoft.edensavesync.domain.repository.AuthRepository
import com.jotasoft.edensavesync.domain.repository.CloudSyncRepository
import com.jotasoft.edensavesync.domain.repository.EdenStorageRepository
import com.jotasoft.edensavesync.domain.repository.SaveSyncRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideAppPreferences(
        @ApplicationContext context: Context
    ): AppPreferences = AppPreferences(context)

    @Provides
    @Singleton
    fun provideEdenStorageManager(
        @ApplicationContext context: Context
    ): EdenStorageManager = EdenStorageManager(context)

    @Provides
    @Singleton
    fun provideGoogleDriveSyncClient(
        prefs: AppPreferences
    ): GoogleDriveSyncClient = GoogleDriveSyncClient(prefs)

    @Provides
    @Singleton
    fun provideEdenStorageRepository(
        storageManager: EdenStorageManager,
        prefs: AppPreferences
    ): EdenStorageRepository = EdenStorageRepositoryImpl(storageManager, prefs)

    @Provides
    @Singleton
    fun provideCloudSyncRepository(
        driveClient: GoogleDriveSyncClient,
        prefs: AppPreferences
    ): CloudSyncRepository = CloudSyncRepositoryImpl(driveClient, prefs)

    @Provides
    @Singleton
    fun provideAuthRepository(
        prefs: AppPreferences,
        driveClient: GoogleDriveSyncClient
    ): AuthRepository = AuthRepositoryImpl(prefs, driveClient)

    @Provides
    @Singleton
    fun provideSaveSyncRepository(
        @ApplicationContext context: Context,
        storageRepo: EdenStorageRepository,
        cloudRepo: CloudSyncRepository,
        prefs: AppPreferences
    ): SaveSyncRepository = SaveSyncRepositoryImpl(
        context = context,
        storageRepo = storageRepo,
        cloudRepo = cloudRepo,
        prefs = prefs
    )
}
