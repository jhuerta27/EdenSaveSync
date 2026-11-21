package com.jotasoft.edensavesync.domain.repository

import android.net.Uri

interface AuthRepository {
    fun getDeviceName(): String
    fun setDeviceName(name: String)
    fun getGoogleClientId(): String
    fun getGoogleClientSecret(): String
    fun isGoogleDriveConfigured(): Boolean
    fun saveDirectDriveToken(
        accessToken: String,
        refreshToken: String,
        clientId: String,
        clientSecret: String
    )
    suspend fun startBrowserOAuth(
        onOpenBrowserUrl: (String) -> Unit
    ): Result<Unit>
    fun handleOAuthRedirect(uri: Uri): Boolean
    fun importPairingCode(code: String): Boolean
    fun exportPairingCode(): String
    fun clearGoogleDriveAuth()
}
