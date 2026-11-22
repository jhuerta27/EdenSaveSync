package com.jotasoft.edensavesync.domain.repository

interface AuthRepository {
    fun getDeviceName(): String
    fun setDeviceName(name: String)
    fun isGoogleDriveConfigured(): Boolean
    suspend fun startBrowserOAuth(onOpenBrowserUrl: (String) -> Unit): Result<Unit>
    fun handleOAuthRedirect(uriString: String): Boolean
    fun clearGoogleDriveAuth()
}
