package com.jotasoft.edensavesync.data.storage

import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import androidx.core.net.toUri
import com.jotasoft.edensavesync.data.device.DeviceDetector
import com.jotasoft.edensavesync.domain.model.BackgroundSyncMode
import com.jotasoft.edensavesync.domain.model.SyncedRecord
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext context: Context
) {

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

    var peanutsTreeUri: Uri?
        get() = prefs.getString("peanuts_tree_uri", null)?.toUri()
        set(value) {
            prefs.edit { putString("peanuts_tree_uri", value?.toString()) }
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

    var backgroundSyncMode: BackgroundSyncMode
        get() = when (prefs.getString("background_sync_mode", null)) {
            BackgroundSyncMode.SCHEDULED.name -> BackgroundSyncMode.SCHEDULED
            BackgroundSyncMode.AUTOMATIC.name -> BackgroundSyncMode.AUTOMATIC
            else -> BackgroundSyncMode.AUTOMATIC
        }
        set(value) {
            prefs.edit { putString("background_sync_mode", value.name) }
        }

    var lastAutoSyncCheckMs: Long
        get() = prefs.getLong("last_auto_sync_check_ms", 0L)
        set(value) {
            prefs.edit { putLong("last_auto_sync_check_ms", value) }
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

    fun getCustomGameNames(): Map<String, String> {
        val raw = prefs.getString("custom_game_names_json", null) ?: return emptyMap()
        return runCatching {
            val obj = JSONObject(raw)
            buildMap {
                obj.keys().forEach { k ->
                    put(k.uppercase(), obj.optString(k, ""))
                }
            }
        }.getOrDefault(emptyMap())
    }

    fun setCustomGameName(titleId: String, customName: String) {
        val current = getCustomGameNames().toMutableMap()
        val normalized = titleId.uppercase()
        if (customName.isBlank()) {
            current.remove(normalized)
        } else {
            current[normalized] = customName.trim()
        }
        prefs.edit { putString("custom_game_names_json", JSONObject(current as Map<*, *>).toString()) }
    }

    fun recordSyncedTitle(
        titleId: String,
        contentHash: String,
        remoteTimestampMs: Long,
        localTimestampAtSyncMs: Long
    ) {
        val normalized = titleId.uppercase()
        val all = getSyncedRecords().toMutableMap()
        all[normalized] = SyncedRecord(
            titleId = normalized,
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
        return runCatching {
            val root = JSONObject(raw)
            buildMap {
                root.keys().forEach { k ->
                    val item = root.optJSONObject(k) ?: return@forEach
                    val normalized = k.uppercase()
                    put(
                        normalized,
                        SyncedRecord(
                            titleId = normalized,
                            contentHash = item.optString("contentHash", ""),
                            remoteTimestampMs = item.optLong("remoteTimestampMs", 0L),
                            localTimestampAtSyncMs = item.optLong("localTimestampAtSyncMs", 0L)
                        )
                    )
                }
            }
        }.getOrDefault(emptyMap())
    }
}
