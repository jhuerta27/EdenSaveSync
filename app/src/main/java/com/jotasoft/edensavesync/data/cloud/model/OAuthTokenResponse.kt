package com.jotasoft.edensavesync.data.cloud.model

data class OAuthTokenResponse(
    val accessToken: String,
    val refreshToken: String?,
    val expiresInSeconds: Long,
    val tokenType: String
)
