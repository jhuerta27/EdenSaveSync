package com.jotasoft.edensavesync.data.storage

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.jotasoft.edensavesync.data.device.DeviceDetector
import com.jotasoft.edensavesync.domain.model.SyncedRecord
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.content.edit
import androidx.core.net.toUri

@Singleton
class AppPreferences @Inject constructor(context: Context) {

    companion object {
        const val DEFAULT_GOOGLE_CLIENT_ID =
            "870389828241-vs94oq1dpc02nljfj48m7hn9fm0tru3h.apps.googleusercontent.com"
        const val DEFAULT_GOOGLE_CLIENT_SECRET = ""
        const val OAUTH_REDIRECT_SCHEME =
            "com.googleusercontent.apps.870389828241-vs94oq1dpc02nljfj48m7hn9fm0tru3h"
        const val OAUTH_REDIRECT_URI = "$OAUTH_REDIRECT_SCHEME:/oauth2redirect"
    }

    private val prefs = context.getSharedPreferences("eden_save_sync_prefs", Context.MODE_PRIVATE)

    var edenTreeUri: Uri?
        get() = prefs.getString("eden_tree_uri", null)?.toUri()
        set(value) {
            prefs.edit { putString("eden_tree_uri", value?.toString()) }
        }

    var deviceName: String
        get() = prefs.getString("device_name", null)
            ?.takeIf { it.isNotBlank() }
            ?: DeviceDetector.detectDefaultDeviceName()
        set(value) {
            prefs.edit { putString("device_name", value.trim()) }
        }

    var googleAccessToken: String
        get() = prefs.getString("google_access_token", "").orEmpty()
        set(value) {
            prefs.edit { putString("google_access_token", value.trim()) }
        }

    var googleRefreshToken: String
        get() = prefs.getString("google_refresh_token", "").orEmpty()
        set(value) {
            prefs.edit { putString("google_refresh_token", value.trim()) }
        }

    var googleClientId: String
        get() = prefs.getString("google_client_id", null)
            ?.takeIf { it.isNotBlank() }
            ?: DEFAULT_GOOGLE_CLIENT_ID
        set(value) {
            prefs.edit { putString("google_client_id", value.trim()) }
        }

    var googleClientSecret: String
        get() = prefs.getString("google_client_secret", null)
            ?.takeIf { it.isNotBlank() }
            ?: DEFAULT_GOOGLE_CLIENT_SECRET
        set(value) {
            prefs.edit { putString("google_client_secret", value.trim()) }
        }

    var googleTokenExpiryMs: Long
        get() = prefs.getLong("google_token_expiry_ms", 0L)
        set(value) {
            prefs.edit { putLong("google_token_expiry_ms", value) }
        }

    var driveFolderId: String
        get() = prefs.getString("drive_folder_id", "").orEmpty()
        set(value) {
            prefs.edit { putString("drive_folder_id", value.trim()) }
        }

    var cachedCatalogJson: String?
        get() = prefs.getString("cached_catalog_json", null)
        set(value) {
            prefs.edit { putString("cached_catalog_json", value) }
        }

    var isScheduledSyncEnabled: Boolean
        get() = prefs.getBoolean("is_scheduled_sync_enabled", false)
        set(value) {
            prefs.edit { putBoolean("is_scheduled_sync_enabled", value) }
        }

    var scheduledSyncHour: Int
        get() = prefs.getInt("scheduled_sync_hour", 4)
        set(value) {
            prefs.edit { putInt("scheduled_sync_hour", value) }
        }

    var scheduledSyncMinute: Int
        get() = prefs.getInt("scheduled_sync_minute", 0)
        set(value) {
            prefs.edit { putInt("scheduled_sync_minute", value) }
        }

    var isScheduledSyncNotificationEnabled: Boolean
        get() = prefs.getBoolean("is_scheduled_sync_notification_enabled", false)
        set(value) {
            prefs.edit { putBoolean("is_scheduled_sync_notification_enabled", value) }
        }

    var lastScheduledSyncDate: String
        get() = prefs.getString("last_scheduled_sync_date", "").orEmpty()
        set(value) {
            prefs.edit { putString("last_scheduled_sync_date", value) }
        }

    val isGoogleDriveConfigured: Boolean
        get() = googleAccessToken.isNotBlank() ||
                (googleRefreshToken.isNotBlank() && googleClientId.isNotBlank())

    fun clearGoogleDriveAuth() {
        prefs.edit {
            remove("google_access_token")
                .remove("google_refresh_token")
                .remove("google_token_expiry_ms")
                .remove("drive_folder_id")
        }
    }

    /**
     * Genera un código compacto `EDEN-SYNC:...` para copiar la configuración de Google Drive
     * y los nombres personalizados de juegos entre dispositivos.
     */
    fun exportPairingCode(): String {
        val json = JSONObject().apply {
            put("accessToken", googleAccessToken)
            put("refreshToken", googleRefreshToken)
            put("clientId", googleClientId)
            put("clientSecret", googleClientSecret)
            put("tokenExpiryMs", googleTokenExpiryMs)
            put("driveFolderId", driveFolderId)
            put("customNames", JSONObject(getCustomGameNames()))
        }
        val encoded = Base64.encodeToString(
            json.toString().toByteArray(Charsets.UTF_8),
            Base64.NO_WRAP
        )
        return "EDEN-SYNC:$encoded"
    }

    fun importPairingCode(rawCode: String): Boolean {
        return try {
            val cleaned = rawCode.trim().removePrefix("EDEN-SYNC:").trim()
            val decodedBytes = Base64.decode(cleaned, Base64.DEFAULT)
            val json = JSONObject(String(decodedBytes, Charsets.UTF_8))

            googleAccessToken = json.optString("accessToken", "")
            googleRefreshToken = json.optString("refreshToken", "")
            googleClientId = json.optString("clientId", "")
            googleClientSecret = json.optString("clientSecret", "")
            googleTokenExpiryMs = json.optLong("tokenExpiryMs", 0L)
            driveFolderId = json.optString("driveFolderId", "")

            json.optJSONObject("customNames")?.let { namesObj ->
                val keys = namesObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val name = namesObj.optString(key, "")
                    if (name.isNotBlank()) {
                        setCustomGameName(key, name)
                    }
                }
            }
            isGoogleDriveConfigured
        } catch (_: Exception) {
            false
        }
    }

    fun getCustomGameNames(): Map<String, String> {
        val raw = prefs.getString("custom_game_names_json", null) ?: return emptyMap()
        return try {
            val obj = JSONObject(raw)
            val map = mutableMapOf<String, String>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                map[k.uppercase()] = obj.optString(k, "")
            }
            map
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun setCustomGameName(titleId: String, customName: String) {
        val current = getCustomGameNames().toMutableMap()
        val normalized = titleId.uppercase()
        if (customName.isBlank()) {
            current.remove(normalized)
        } else {
            current[normalized] = customName.trim()
        }
        val obj = JSONObject()
        current.forEach { (k, v) -> obj.put(k, v) }
        prefs.edit { putString("custom_game_names_json", obj.toString()) }
    }

    fun recordSyncedTitle(
        titleId: String,
        contentHash: String,
        remoteTimestampMs: Long,
        localTimestampAtSyncMs: Long
    ) {
        val all = getSyncedRecords().toMutableMap()
        all[titleId.uppercase()] = SyncedRecord(
            titleId = titleId.uppercase(),
            contentHash = contentHash,
            remoteTimestampMs = remoteTimestampMs,
            localTimestampAtSyncMs = localTimestampAtSyncMs
        )
        val root = JSONObject()
        all.forEach { (k, v) ->
            root.put(
                k,
                JSONObject().apply {
                    put("contentHash", v.contentHash)
                    put("remoteTimestampMs", v.remoteTimestampMs)
                    put("localTimestampAtSyncMs", v.localTimestampAtSyncMs)
                }
            )
        }
        prefs.edit { putString("synced_records_json", root.toString()) }
    }

    fun getSyncedRecords(): Map<String, SyncedRecord> {
        val raw = prefs.getString("synced_records_json", null) ?: return emptyMap()
        return try {
            val root = JSONObject(raw)
            val result = mutableMapOf<String, SyncedRecord>()
            val keys = root.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val item = root.optJSONObject(k) ?: continue
                result[k.uppercase()] = SyncedRecord(
                    titleId = k.uppercase(),
                    contentHash = item.optString("contentHash", ""),
                    remoteTimestampMs = item.optLong("remoteTimestampMs", 0L),
                    localTimestampAtSyncMs = item.optLong("localTimestampAtSyncMs", 0L)
                )
            }
            result
        } catch (_: Exception) {
            emptyMap()
        }
    }
}
