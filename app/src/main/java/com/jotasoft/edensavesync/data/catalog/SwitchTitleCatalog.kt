package com.jotasoft.edensavesync.data.catalog

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL

object SwitchTitleCatalog {

    const val CATALOG_URL =
        "https://gist.githubusercontent.com/jhuerta27/3bb8b0b82e4d8b953db55a3b056d3bdf/raw"

    @Volatile
    private var titlesCache: Map<String, String> = emptyMap()

    fun clearCache() {
        titlesCache = emptyMap()
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

    fun loadFromAssets(context: Context): Map<String, String>? = runCatching {
        context.assets.open("titles.json").bufferedReader().use(BufferedReader::readText).let(::parseCatalogJson)
    }.getOrNull()

    fun parseCatalogJson(json: String): Map<String, String>? = runCatching {
        val obj = JSONObject(json)
        val result = mutableMapOf<String, String>()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val name = obj.optString(key, "")
            if (name.isNotBlank()) {
                result[key.trim().uppercase()] = name.trim()
            }
        }
        result
    }.getOrNull()

    suspend fun fetchRemoteCatalog(url: String = CATALOG_URL): Pair<Map<String, String>, String>? =
        withContext(Dispatchers.IO) {
            val cacheBustedUrl = if (url.contains("?")) {
                "$url&t=${System.currentTimeMillis()}"
            } else {
                "$url?t=${System.currentTimeMillis()}"
            }
            downloadAndParse(cacheBustedUrl)
        }

    private fun downloadAndParse(urlString: String): Pair<Map<String, String>, String>? {
        return try {
            var currentUrl = urlString
            var redirects = 0
            while (redirects < 5) {
                val url = URL(currentUrl)
                val connection = (url.openConnection() as HttpURLConnection).apply {
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
                    val location = connection.getHeaderField("Location") ?: break
                    currentUrl = location
                    redirects++
                    continue
                }
                if (code == HttpURLConnection.HTTP_OK) {
                    val content = connection.inputStream.bufferedReader().use(BufferedReader::readText)
                    val parsed = parseCatalogJson(content)
                    return if (parsed != null && parsed.isNotEmpty()) {
                        titlesCache = parsed
                        parsed to content
                    } else {
                        null
                    }
                }
                break
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    fun resolveTitle(titleId: String): String? = titlesCache[titleId.uppercase()]

    fun resolveName(titleId: String, customNames: Map<String, String>): String {
        val normalized = titleId.uppercase()
        customNames[normalized]?.takeIf { it.isNotBlank() }?.let { return it }
        titlesCache[normalized]?.let { return it }
        return "Switch Game ($normalized)"
    }
}
