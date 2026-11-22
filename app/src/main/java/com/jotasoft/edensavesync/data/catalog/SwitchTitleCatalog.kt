package com.jotasoft.edensavesync.data.catalog

import android.content.Context
import com.jotasoft.edensavesync.data.storage.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL

object SwitchTitleCatalog {

    const val CATALOG_URL =
        "https://raw.githubusercontent.com/jhuerta27/EdenSaveSync/main/app/src/main/assets/titles.json"

    @Volatile
    private var titlesCache: Map<String, String> = emptyMap()

    @Volatile
    private var remoteFetchedThisSession = false

    fun clearCache() {
        titlesCache = emptyMap()
        remoteFetchedThisSession = false
    }

    fun init(context: Context? = null, cachedJson: String? = null) {
        if (!cachedJson.isNullOrBlank()) {
            parseCatalogJson(cachedJson)?.let {
                titlesCache = it
                return
            }
        }
        context?.let { ctx ->
            loadFromAssets(ctx)?.let {
                titlesCache = it
            }
        }
    }

    suspend fun ensureLoaded(context: Context, prefs: AppPreferences) {
        if (titlesCache.isEmpty()) {
            init(context, prefs.cachedCatalogJson)
        }
        if (!remoteFetchedThisSession) {
            remoteFetchedThisSession = true
            runCatching {
                fetchRemoteCatalog()?.let { (_, rawJson) ->
                    prefs.cachedCatalogJson = rawJson
                }
            }
        }
    }

    fun loadFromAssets(context: Context): Map<String, String>? = runCatching {
        context.assets.open("titles.json").bufferedReader().use(BufferedReader::readText).let(::parseCatalogJson)
    }.getOrNull()

    fun parseCatalogJson(json: String): Map<String, String>? = runCatching {
        val obj = JSONObject(json)
        buildMap {
            obj.keys().forEach { key ->
                val name = obj.optString(key, "")
                if (name.isNotBlank()) {
                    put(key.trim().uppercase(), name.trim())
                }
            }
        }
    }.getOrNull()

    suspend fun fetchRemoteCatalog(url: String = CATALOG_URL): Pair<Map<String, String>, String>? =
        withContext(Dispatchers.IO) {
            val separator = if (url.contains("?")) "&" else "?"
            downloadAndParse("${url}${separator}t=${System.currentTimeMillis()}")
        }

    private fun downloadAndParse(urlString: String): Pair<Map<String, String>, String>? = runCatching {
        var currentUrl = urlString
        for (redirect in 0 until 5) {
            val connection = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 5000
                requestMethod = "GET"
                useCaches = false
                setRequestProperty("Cache-Control", "no-cache")
                setRequestProperty("Pragma", "no-cache")
                instanceFollowRedirects = true
            }
            val code = connection.responseCode
            if (code in 300..399) {
                currentUrl = connection.getHeaderField("Location") ?: break
                continue
            }
            if (code == HttpURLConnection.HTTP_OK) {
                val content = connection.inputStream.bufferedReader().use(BufferedReader::readText)
                val parsed = parseCatalogJson(content)
                return if (!parsed.isNullOrEmpty()) {
                    titlesCache = parsed
                    parsed to content
                } else {
                    null
                }
            }
            break
        }
        null
    }.getOrNull()

    fun resolveTitle(titleId: String): String? = titlesCache[titleId.uppercase()]

    fun resolveName(titleId: String, customNames: Map<String, String>): String {
        val normalized = titleId.uppercase()
        return customNames[normalized]?.takeIf { it.isNotBlank() }
            ?: titlesCache[normalized]
            ?: "Switch Game ($normalized)"
    }
}
