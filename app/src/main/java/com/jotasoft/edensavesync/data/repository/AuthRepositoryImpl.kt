package com.jotasoft.edensavesync.data.repository

import android.net.Uri
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

    override fun getGoogleClientId(): String = prefs.googleClientId

    override fun getGoogleClientSecret(): String = prefs.googleClientSecret

    override fun isGoogleDriveConfigured(): Boolean = prefs.isGoogleDriveConfigured

    override fun saveDirectDriveToken(
        accessToken: String,
        refreshToken: String,
        clientId: String,
        clientSecret: String
    ) {
        prefs.googleAccessToken = accessToken
        prefs.googleRefreshToken = refreshToken
        prefs.googleClientId = clientId
        prefs.googleClientSecret = clientSecret
        prefs.googleTokenExpiryMs = if (refreshToken.isNotBlank()) {
            System.currentTimeMillis() + 3500_000L
        } else {
            0L
        }
    }

    override suspend fun startBrowserOAuth(
        onOpenBrowserUrl: (String) -> Unit
    ): Result<Unit> = driveClient.runCustomSchemeOAuthFlow(
        onOpenBrowserUrl = onOpenBrowserUrl
    )

    override fun handleOAuthRedirect(uri: Uri): Boolean = driveClient.onOAuthRedirect(uri)

    override fun importPairingCode(code: String): Boolean = prefs.importPairingCode(code)

    override fun exportPairingCode(): String = prefs.exportPairingCode()

    override fun clearGoogleDriveAuth() = prefs.clearGoogleDriveAuth()
}
