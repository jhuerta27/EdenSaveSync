package com.jotasoft.edensavesync.data.cloud

import android.net.Uri
import android.util.Base64
import com.jotasoft.edensavesync.data.storage.AppPreferences
import com.jotasoft.edensavesync.domain.model.LocalSaveGame
import com.jotasoft.edensavesync.domain.model.RemoteSaveGame
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleDriveSyncClient @Inject constructor(private val prefs: AppPreferences) {

    companion object {
        private const val DRIVE_FILES_URL = "https://www.googleapis.com/drive/v3/files"
        private const val DRIVE_UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3/files"
        private const val OAUTH_TOKEN_URL = "https://oauth2.googleapis.com/token"
        private const val OAUTH_AUTH_URL = "https://accounts.google.com/o/oauth2/v2/auth"
        private const val DRIVE_SCOPE = "https://www.googleapis.com/auth/drive.file"
        private const val SYNC_FOLDER_NAME = "EdenSaveSync"
    }

    @Volatile
    private var pendingOAuthCallback: CompletableDeferred<Uri>? = null

    fun onOAuthRedirect(uri: Uri): Boolean {
        if (uri.scheme != AppPreferences.OAUTH_REDIRECT_SCHEME) return false
        return pendingOAuthCallback?.complete(uri) ?: false
    }

    /**
     * Inicia un flujo OAuth 2.0 con PKCE y esquema de URI personalizado de Android
     * (`com.googleusercontent.apps.<CLIENT_ID>:/oauth2redirect`), sin Client Secret.
     */
    suspend fun runCustomSchemeOAuthFlow(
        onOpenBrowserUrl: (String) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val clientId = AppPreferences.DEFAULT_GOOGLE_CLIENT_ID
            val redirectUri = AppPreferences.OAUTH_REDIRECT_URI
            require(clientId.isNotBlank()) { "Client ID de Google OAuth no configurado." }

            val codeVerifier = generateCodeVerifier()
            val codeChallenge = generateCodeChallenge(codeVerifier)
            val state = generateCodeVerifier().take(32)

            pendingOAuthCallback?.cancel()
            val callbackDeferred = CompletableDeferred<Uri>()
            pendingOAuthCallback = callbackDeferred

            val authUrl = buildString {
                append(OAUTH_AUTH_URL)
                append("?client_id=").append(urlEncode(clientId))
                append("&redirect_uri=").append(urlEncode(redirectUri))
                append("&response_type=code")
                append("&scope=").append(urlEncode(DRIVE_SCOPE))
                append("&access_type=offline")
                append("&prompt=consent")
                append("&code_challenge=").append(urlEncode(codeChallenge))
                append("&code_challenge_method=S256")
                append("&state=").append(urlEncode(state))
            }

            try {
                onOpenBrowserUrl(authUrl)

                val callbackUri = withTimeout(180_000L) {
                    callbackDeferred.await()
                }

                val returnedState = callbackUri.getQueryParameter("state")
                if (returnedState != state) {
                    throw IllegalStateException("Estado OAuth inválido. Inténtalo de nuevo.")
                }

                val error = callbackUri.getQueryParameter("error")
                if (!error.isNullOrBlank()) {
                    throw IllegalStateException("Autorización fallida: $error")
                }

                val code = callbackUri.getQueryParameter("code")
                if (code.isNullOrBlank()) {
                    throw IllegalStateException("Autorización fallida: sin código")
                }

                // Reintentar brevemente si la red/DNS tarda en reactivarse al volver del navegador
                var lastError: Throwable? = null
                for (attempt in 1..15) {
                    val res = runCatching {
                        exchangeCodeForTokens(
                            code = code,
                            clientId = clientId,
                            clientSecret = "",
                            redirectUri = redirectUri,
                            codeVerifier = codeVerifier
                        )
                    }
                    if (res.isSuccess) {
                        lastError = null
                        break
                    }
                    val err = res.exceptionOrNull()
                    lastError = err
                    if (err is java.io.IOException) {
                        kotlinx.coroutines.delay(500L)
                    } else {
                        break
                    }
                }
                if (lastError != null) {
                    throw lastError
                }
            } finally {
                if (pendingOAuthCallback === callbackDeferred) {
                    pendingOAuthCallback = null
                }
            }
        }
    }

    private fun exchangeCodeForTokens(
        code: String,
        clientId: String,
        clientSecret: String,
        redirectUri: String,
        codeVerifier: String
    ) {
        val bodyParams = mutableListOf(
            "code=${urlEncode(code)}",
            "client_id=${urlEncode(clientId)}",
            "redirect_uri=${urlEncode(redirectUri)}",
            "grant_type=authorization_code",
            "code_verifier=${urlEncode(codeVerifier)}"
        )
        if (clientSecret.isNotBlank()) {
            bodyParams.add("client_secret=${urlEncode(clientSecret)}")
        }
        val body = bodyParams.joinToString("&")

        val conn = (URL(OAUTH_TOKEN_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        }

        conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        val responseText = readResponse(conn)
        if (conn.responseCode !in 200..299) {
            throw IllegalStateException("Error al obtener token OAuth (${conn.responseCode}): $responseText")
        }

        val json = JSONObject(responseText)
        val accessToken = json.getString("access_token")
        val refreshToken = json.optString("refresh_token", prefs.googleRefreshToken)
        val expiresInSec = json.optLong("expires_in", 3500L)

        prefs.googleClientId = clientId
        prefs.googleClientSecret = clientSecret
        prefs.googleAccessToken = accessToken
        if (refreshToken.isNotBlank()) {
            prefs.googleRefreshToken = refreshToken
        }
        prefs.googleTokenExpiryMs = System.currentTimeMillis() + (expiresInSec * 1000L)
    }

    private fun getValidAccessToken(forceRefresh: Boolean = false): String {
        val now = System.currentTimeMillis()
        val currentToken = prefs.googleAccessToken
        val expiry = prefs.googleTokenExpiryMs
        val refreshToken = prefs.googleRefreshToken
        val clientId = prefs.googleClientId

        if (!forceRefresh && currentToken.isNotBlank()) {
            // Si no hay refresh token configurado, usamos el access token directo tal cual
            if (refreshToken.isBlank() || clientId.isBlank()) {
                return currentToken
            }
            // Si hay refresh token y el access token aún no ha caducado, lo reutilizamos
            if (expiry > 0L && now < expiry - 60_000L) {
                return currentToken
            }
        }

        if (refreshToken.isBlank() || clientId.isBlank()) {
            if (!forceRefresh && currentToken.isNotBlank()) return currentToken
            throw IllegalStateException(
                "El Access Token temporal (1 hora) ha caducado. Configura Login OAuth con Client ID + Refresh Token para que sea permanente."
            )
        }

        val params = mutableListOf(
            "client_id=${urlEncode(clientId)}",
            "refresh_token=${urlEncode(refreshToken)}",
            "grant_type=refresh_token"
        )
        if (prefs.googleClientSecret.isNotBlank()) {
            params.add("client_secret=${urlEncode(prefs.googleClientSecret)}")
        }

        val conn = (URL(OAUTH_TOKEN_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
        }
        conn.outputStream.use { it.write(params.joinToString("&").toByteArray(Charsets.UTF_8)) }
        val resp = readResponse(conn)
        if (conn.responseCode !in 200..299) {
            throw IllegalStateException("No se pudo renovar el token de Google Drive (${conn.responseCode}): $resp")
        }

        val json = JSONObject(resp)
        val newAccessToken = json.getString("access_token")
        val newRefreshToken = json.optString("refresh_token", "")
        val expiresIn = json.optLong("expires_in", 3500L)
        prefs.googleAccessToken = newAccessToken
        if (newRefreshToken.isNotBlank()) {
            prefs.googleRefreshToken = newRefreshToken
        }
        prefs.googleTokenExpiryMs = System.currentTimeMillis() + (expiresIn * 1000L)
        return newAccessToken
    }

    private fun canAutoRefresh(): Boolean =
        prefs.googleRefreshToken.isNotBlank() && prefs.googleClientId.isNotBlank()

    /**
     * Busca o crea la carpeta `EdenSaveSync` en Google Drive.
     */
    suspend fun ensureSyncFolderId(): String = withContext(Dispatchers.IO) {
        var token = getValidAccessToken(forceRefresh = false)

        // 1. Buscar carpeta existente en Google Drive
        val query = "mimeType = 'application/vnd.google-apps.folder' and name = '$SYNC_FOLDER_NAME' and trashed = false"
        val searchUrl = "$DRIVE_FILES_URL?q=${urlEncode(query)}&fields=files(id,name)&spaces=drive"
        var searchConn = (URL(searchUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Authorization", "Bearer $token")
        }
        var searchResp = readResponse(searchConn)
        if (searchConn.responseCode == 401 && canAutoRefresh()) {
            token = getValidAccessToken(forceRefresh = true)
            searchConn = (URL(searchUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Authorization", "Bearer $token")
            }
            searchResp = readResponse(searchConn)
        }

        if (searchConn.responseCode in 200..299) {
            val files = JSONObject(searchResp).optJSONArray("files") ?: JSONArray()
            if (files.length() > 0) {
                val id = files.getJSONObject(0).getString("id")
                prefs.driveFolderId = id
                return@withContext id
            }
        } else if (searchConn.responseCode == 401) {
            throw IllegalStateException(
                "El Access Token temporal ha caducado (401). Vincula Google Drive con Login OAuth (Client ID) para que se renueve solo para siempre."
            )
        }

        // 2. Crear carpeta si no existe
        val metadata = JSONObject().apply {
            put("name", SYNC_FOLDER_NAME)
            put("mimeType", "application/vnd.google-apps.folder")
        }
        val createConn = (URL(DRIVE_FILES_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        }
        createConn.outputStream.use { it.write(metadata.toString().toByteArray(Charsets.UTF_8)) }
        val createResp = readResponse(createConn)
        if (createConn.responseCode !in 200..299) {
            throw IllegalStateException("Error creando carpeta en Google Drive (${createConn.responseCode}): $createResp")
        }
        val folderId = JSONObject(createResp).getString("id")
        prefs.driveFolderId = folderId
        folderId
    }

    /**
     * Lista todas las partidas guardadas (`<TitleID>.zip`) almacenadas en la carpeta `EdenSaveSync` de Google Drive.
     */
    suspend fun listRemoteSaves(): List<RemoteSaveGame> = withContext(Dispatchers.IO) {
        val token = getValidAccessToken()
        val folderId = ensureSyncFolderId()

        val query = "'$folderId' in parents and trashed = false"
        val fields = "files(id,name,size,modifiedTime,appProperties)"
        val listUrl = "$DRIVE_FILES_URL?q=${urlEncode(query)}&fields=${urlEncode(fields)}&pageSize=200"

        val conn = (URL(listUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Authorization", "Bearer $token")
        }
        val resp = readResponse(conn)
        if (conn.responseCode !in 200..299) {
            throw IllegalStateException("Error consultando Google Drive (${conn.responseCode}): $resp")
        }

        val files = JSONObject(resp).optJSONArray("files") ?: JSONArray()
        val remoteByTitle = mutableMapOf<String, RemoteSaveGame>()

        for (i in 0 until files.length()) {
            val fileObj = files.getJSONObject(i)
            val fileId = fileObj.getString("id")
            val fileName = fileObj.optString("name", "")
            if (!fileName.endsWith(".zip", ignoreCase = true)) continue

            val props = fileObj.optJSONObject("appProperties")
            val rawTitleId = props?.optString("titleId")?.takeIf { it.isNotBlank() }
                ?: fileName.removeSuffix(".zip").removeSuffix(".ZIP")
            val titleId = rawTitleId.uppercase()
            if (titleId.length != 16) continue

            val driveModTimeMs = runCatching {
                Instant.parse(fileObj.optString("modifiedTime")).toEpochMilli()
            }.getOrDefault(0L)

            val saveLastModifiedMs = props?.optString("lastModifiedMs")?.toLongOrNull()
                ?.takeIf { it > 0L }
                ?: driveModTimeMs

            val driveSize = fileObj.optLong("size", 0L)
            val totalSizeBytes = props?.optString("totalSizeBytes")?.toLongOrNull()
                ?: driveSize

            // Si el archivo en Drive es un ZIP vacío (<= 22 bytes), no contiene partidas reales
            if (driveSize in 1..22L && totalSizeBytes <= 22L) {
                continue
            }

            val fileCount = props?.optString("fileCount")?.toIntOrNull() ?: 1
            val contentHash = props?.optString("contentHash").orEmpty()
            val sourceDeviceName = props?.optString("sourceDeviceName")
                ?.takeIf { it.isNotBlank() }
                ?: "Google Drive"
            val gameName = props?.optString("gameName").orEmpty()
            val uploadedAtMs = props?.optString("uploadedAtMs")?.toLongOrNull() ?: driveModTimeMs

            val candidate = RemoteSaveGame(
                titleId = titleId,
                gameName = gameName,
                driveFileId = fileId,
                lastModifiedMs = saveLastModifiedMs,
                totalSizeBytes = totalSizeBytes,
                fileCount = fileCount,
                contentHash = contentHash,
                sourceDeviceName = sourceDeviceName,
                uploadedAtMs = uploadedAtMs
            )

            val existing = remoteByTitle[titleId]
            if (existing == null || candidate.lastModifiedMs > existing.lastModifiedMs) {
                remoteByTitle[titleId] = candidate
            }
        }

        remoteByTitle.values.sortedByDescending { it.lastModifiedMs }
    }

    /**
     * Sube o actualiza el archivo `<TitleID>.zip` en la carpeta `EdenSaveSync` de Google Drive.
     */
    suspend fun uploadSaveZip(
        localSave: LocalSaveGame,
        gameName: String,
        deviceName: String,
        zipFile: File,
        existingDriveFileId: String?
    ): RemoteSaveGame = withContext(Dispatchers.IO) {
        require(zipFile.exists() && zipFile.length() > 22L && localSave.fileCount > 0) {
            "No se puede subir una partida vacía sin archivos para ${localSave.titleId}"
        }
        val token = getValidAccessToken()
        val folderId = ensureSyncFolderId()
        val titleId = localSave.titleId.uppercase()
        val nowMs = System.currentTimeMillis()

        val appProps = JSONObject().apply {
            put("titleId", titleId)
            put("gameName", gameName.take(80))
            put("lastModifiedMs", localSave.lastModifiedMs.toString())
            put("totalSizeBytes", localSave.totalSizeBytes.toString())
            put("fileCount", localSave.fileCount.toString())
            put("contentHash", localSave.contentHash)
            put("sourceDeviceName", deviceName.take(50))
            put("uploadedAtMs", nowMs.toString())
        }

        val metadata = JSONObject().apply {
            put("name", "$titleId.zip")
            put("mimeType", "application/zip")
            put("appProperties", appProps)
            if (existingDriveFileId.isNullOrBlank()) {
                put("parents", JSONArray().put(folderId))
            }
        }

        val boundary = "EdenSyncBoundary_${System.currentTimeMillis()}"
        val targetUrl = if (existingDriveFileId.isNullOrBlank()) {
            "$DRIVE_UPLOAD_URL?uploadType=multipart&fields=id,name,size,modifiedTime,appProperties"
        } else {
            "$DRIVE_UPLOAD_URL/$existingDriveFileId?uploadType=multipart&fields=id,name,size,modifiedTime,appProperties"
        }

        val conn = (URL(targetUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = if (existingDriveFileId.isNullOrBlank()) "POST" else "PATCH"
            doOutput = true
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
        }

        BufferedOutputStream(conn.outputStream).use { out ->
            val metaPart = buildString {
                append("--").append(boundary).append("\r\n")
                append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
                append(metadata.toString()).append("\r\n")
                append("--").append(boundary).append("\r\n")
                append("Content-Type: application/zip\r\n\r\n")
            }
            out.write(metaPart.toByteArray(Charsets.UTF_8))

            BufferedInputStream(FileInputStream(zipFile)).use { input ->
                val buffer = ByteArray(16 * 1024)
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    out.write(buffer, 0, read)
                }
            }

            val endPart = "\r\n--$boundary--\r\n"
            out.write(endPart.toByteArray(Charsets.UTF_8))
            out.flush()
        }

        val resp = readResponse(conn)
        if (conn.responseCode !in 200..299) {
            throw IllegalStateException("Error subiendo partida a Google Drive (${conn.responseCode}): $resp")
        }

        val createdId = JSONObject(resp).optString("id", existingDriveFileId.orEmpty())
        RemoteSaveGame(
            titleId = titleId,
            gameName = gameName,
            driveFileId = createdId,
            lastModifiedMs = localSave.lastModifiedMs,
            totalSizeBytes = localSave.totalSizeBytes,
            fileCount = localSave.fileCount,
            contentHash = localSave.contentHash,
            sourceDeviceName = deviceName,
            uploadedAtMs = nowMs
        )
    }

    /**
     * Descarga `<TitleID>.zip` desde Google Drive a un archivo local temporal.
     */
    suspend fun downloadSaveZip(driveFileId: String, targetZipFile: File) = withContext(Dispatchers.IO) {
        val token = getValidAccessToken()
        val downloadUrl = "$DRIVE_FILES_URL/$driveFileId?alt=media"
        val conn = (URL(downloadUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Authorization", "Bearer $token")
        }

        if (conn.responseCode !in 200..299) {
            val err = readResponse(conn)
            throw IllegalStateException("Error descargando de Google Drive (${conn.responseCode}): $err")
        }

        targetZipFile.parentFile?.mkdirs()
        if (targetZipFile.exists()) targetZipFile.delete()

        BufferedInputStream(conn.inputStream).use { input ->
            BufferedOutputStream(FileOutputStream(targetZipFile)).use { out ->
                val buffer = ByteArray(16 * 1024)
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    out.write(buffer, 0, read)
                }
                out.flush()
            }
        }
    }

    private fun readResponse(conn: HttpURLConnection): String {
        val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
        if (stream == null) return ""
        return stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    private fun urlEncode(value: String): String =
        URLEncoder.encode(value, "UTF-8")

    private fun generateCodeVerifier(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(
            bytes,
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
    }

    private fun generateCodeChallenge(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(verifier.toByteArray(Charsets.US_ASCII))
        return Base64.encodeToString(
            digest,
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
        )
    }

    private fun extractQueryParam(requestLine: String, param: String): String? {
        val path = requestLine.split(" ").getOrNull(1) ?: return null
        val query = path.substringAfter("?", "")
        if (query.isEmpty()) return null
        return query.split("&")
            .map { it.split("=", limit = 2) }
            .firstOrNull { it[0] == param }
            ?.getOrNull(1)
            ?.let { java.net.URLDecoder.decode(it, "UTF-8") }
    }
}
