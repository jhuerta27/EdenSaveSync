package com.jotasoft.edensavesync.data.repository

import androidx.core.net.toUri
import com.jotasoft.edensavesync.data.cloud.GoogleDriveSyncClient
import com.jotasoft.edensavesync.data.storage.AppPreferences
import com.jotasoft.edensavesync.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val prefs: AppPreferences,
    private val driveClient: GoogleDriveSyncClient
) : AuthRepository {

    override fun getDeviceName(): String = prefs.deviceName

    override fun setDeviceName(name: String) {
        prefs.deviceName = name
    }

    override fun isGoogleDriveConfigured(): Boolean = prefs.isGoogleDriveConfigured

    override suspend fun startBrowserOAuth(
        onOpenBrowserUrl: (String) -> Unit
    ): Result<Unit> = driveClient.runCustomSchemeOAuthFlow(onOpenBrowserUrl = onOpenBrowserUrl)

    override fun handleOAuthRedirect(uriString: String): Boolean =
        driveClient.onOAuthRedirect(uriString.toUri())

    override fun clearGoogleDriveAuth() = prefs.clearGoogleDriveAuth()
}
