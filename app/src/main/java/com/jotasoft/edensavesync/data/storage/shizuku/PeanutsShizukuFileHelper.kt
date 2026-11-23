package com.jotasoft.edensavesync.data.storage.shizuku

import com.jotasoft.edensavesync.domain.model.LocalSaveGame
import com.jotasoft.edensavesync.domain.model.LocalSaveSource
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Operaciones sobre archivos en la carpeta de guardados de Peanuts Native (`saves/<TITLE_ID>/`).
 * Se ejecuta dentro de `PeanutsShizukuService` (proceso Shizuku) y también es testeable en JVM local.
 */
internal object PeanutsShizukuFileHelper {

    private val HEX_16_REGEX = Regex("^[0-9A-Fa-f]{16}$")
    private const val IO_BUFFER_SIZE = 32 * 1024

    fun isDirectoryAccessible(savesDir: File): Boolean =
        (savesDir.exists() && savesDir.isDirectory && savesDir.canRead()) ||
            (savesDir.parentFile?.exists() == true && savesDir.parentFile?.canWrite() == true)

    fun scanSaves(savesDir: File): List<LocalSaveGame> {
        if (!savesDir.exists() || !savesDir.isDirectory) return emptyList()
        val children = savesDir.listFiles() ?: return emptyList()
        val result = mutableListOf<LocalSaveGame>()

        for (titleDir in children) {
            val titleName = titleDir.name.substringBefore(" (").trim().uppercase()
            if (!titleDir.isDirectory || !HEX_16_REGEX.matches(titleName)) continue

            val stats = computeDirectoryStats(titleDir)
            if (stats.fileCount == 0) continue

            result.add(
                LocalSaveGame(
                    titleId = titleName,
                    userProfileId = "",
                    lastModifiedMs = stats.lastModifiedMs,
                    totalSizeBytes = stats.totalSizeBytes,
                    fileCount = stats.fileCount,
                    contentHash = stats.contentHash,
                    source = LocalSaveSource.PEANUTS_NATIVE
                )
            )
        }
        return result.sortedByDescending { it.lastModifiedMs }
    }

    fun scanSingleSave(savesDir: File, titleId: String): LocalSaveGame? {
        if (!savesDir.exists() || !savesDir.isDirectory) return null
        val normalized = titleId.uppercase()
        val titleDir = findTitleDir(savesDir, normalized) ?: return null
        val stats = computeDirectoryStats(titleDir)
        if (stats.fileCount == 0) return null

        return LocalSaveGame(
            titleId = normalized,
            userProfileId = "",
            lastModifiedMs = stats.lastModifiedMs,
            totalSizeBytes = stats.totalSizeBytes,
            fileCount = stats.fileCount,
            contentHash = stats.contentHash,
            source = LocalSaveSource.PEANUTS_NATIVE
        )
    }

    fun exportTitleSaveToZip(savesDir: File, titleId: String, targetZipFile: File): Boolean {
        val titleDir = findTitleDir(savesDir, titleId.uppercase()) ?: return false
        targetZipFile.parentFile?.mkdirs()
        if (targetZipFile.exists()) targetZipFile.delete()

        val buffer = ByteArray(IO_BUFFER_SIZE)
        val zippedCount = ZipOutputStream(BufferedOutputStream(FileOutputStream(targetZipFile))).use { zos ->
            zipDirectory(titleDir, "", zos, buffer)
        }
        if (zippedCount == 0 || !targetZipFile.exists() || targetZipFile.length() <= 22L) {
            targetZipFile.delete()
            return false
        }
        return true
    }

    fun importTitleSaveFromZip(savesDir: File, titleId: String, sourceZipFile: File): Boolean {
        if (!sourceZipFile.exists() || sourceZipFile.length() <= 22L) return false
        val normalized = titleId.uppercase()
        if (!savesDir.exists() && !savesDir.mkdirs()) return false

        savesDir.listFiles()?.forEach { child ->
            if (child.isDirectory && child.name.startsWith("$normalized (", ignoreCase = true)) {
                child.deleteRecursively()
            }
        }

        val targetTitleDir = findTitleDir(savesDir, normalized) ?: File(savesDir, normalized)
        if (targetTitleDir.exists()) {
            targetTitleDir.listFiles()?.forEach { it.deleteRecursively() }
        } else if (!targetTitleDir.mkdirs()) {
            return false
        }

        val canonicalRoot = targetTitleDir.canonicalPath
        val buffer = ByteArray(IO_BUFFER_SIZE)

        ZipInputStream(BufferedInputStream(FileInputStream(sourceZipFile))).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                val cleanPath = entry.name.trimStart('/', '\\').replace('\\', '/')
                if (cleanPath.isNotEmpty() && !cleanPath.contains("..")) {
                    val entryPath = if (cleanPath.startsWith("$normalized/", ignoreCase = true)) {
                        cleanPath.substring(normalized.length + 1)
                    } else {
                        cleanPath
                    }
                    if (entryPath.isNotEmpty()) {
                        val outFile = File(targetTitleDir, entryPath)
                        if (outFile.canonicalPath.startsWith(canonicalRoot)) {
                            if (entry.isDirectory) {
                                outFile.mkdirs()
                            } else {
                                outFile.parentFile?.mkdirs()
                                BufferedOutputStream(FileOutputStream(outFile)).use { out ->
                                    var read: Int
                                    while (zis.read(buffer).also { read = it } != -1) {
                                        out.write(buffer, 0, read)
                                    }
                                    out.flush()
                                }
                                if (entry.time > 0L) {
                                    outFile.setLastModified(entry.time)
                                }
                            }
                        }
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        return true
    }

    fun toJsonArray(saves: List<LocalSaveGame>): String = JSONArray().apply {
        saves.forEach { put(toJsonObject(it)) }
    }.toString()

    fun fromJsonArray(raw: String?): List<LocalSaveGame> {
        if (raw.isNullOrBlank()) return emptyList()
        val arr = JSONArray(raw)
        return buildList {
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                add(fromJsonObject(obj))
            }
        }
    }

    fun toJsonObject(save: LocalSaveGame): JSONObject = JSONObject().apply {
        put("titleId", save.titleId)
        put("lastModifiedMs", save.lastModifiedMs)
        put("totalSizeBytes", save.totalSizeBytes)
        put("fileCount", save.fileCount)
        put("contentHash", save.contentHash)
    }

    fun fromJsonObject(obj: JSONObject): LocalSaveGame = LocalSaveGame(
        titleId = obj.getString("titleId"),
        userProfileId = "",
        lastModifiedMs = obj.optLong("lastModifiedMs", 0L),
        totalSizeBytes = obj.optLong("totalSizeBytes", 0L),
        fileCount = obj.optInt("fileCount", 0),
        contentHash = obj.optString("contentHash", ""),
        source = LocalSaveSource.PEANUTS_NATIVE
    )

    private fun findTitleDir(savesDir: File, normalizedTitleId: String): File? =
        savesDir.listFiles()?.firstOrNull {
            it.isDirectory && (
                it.name.equals(normalizedTitleId, ignoreCase = true) ||
                    it.name.startsWith("$normalizedTitleId (", ignoreCase = true)
                )
        }

    private fun zipDirectory(
        dir: File,
        parentPath: String,
        zos: ZipOutputStream,
        buffer: ByteArray
    ): Int {
        var fileCount = 0
        val children = dir.listFiles()?.sortedBy { it.name }.orEmpty()
        for (child in children) {
            val entryPath = if (parentPath.isEmpty()) child.name else "$parentPath/${child.name}"
            if (child.isDirectory) {
                val dirEntry = ZipEntry("$entryPath/").apply {
                    val mod = child.lastModified()
                    if (mod > 0L) time = mod
                }
                zos.putNextEntry(dirEntry)
                zos.closeEntry()
                fileCount += zipDirectory(child, entryPath, zos, buffer)
            } else if (child.isFile) {
                val entry = ZipEntry(entryPath).apply {
                    val mod = child.lastModified()
                    if (mod > 0L) time = mod
                }
                zos.putNextEntry(entry)
                BufferedInputStream(FileInputStream(child)).use { input ->
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        zos.write(buffer, 0, read)
                    }
                }
                zos.closeEntry()
                if (!child.name.equals(".yuzu_save_size", ignoreCase = true)) {
                    fileCount++
                }
            }
        }
        return fileCount
    }

    private data class DirStats(
        val lastModifiedMs: Long,
        val totalSizeBytes: Long,
        val fileCount: Int,
        val contentHash: String
    )

    // Mismo algoritmo SHA-256 que EdenStorageManager.computeDirectoryStats para garantizar hashes idénticos
    private fun computeDirectoryStats(dir: File): DirStats {
        var maxModified = 0L
        var totalSize = 0L
        var count = 0
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(IO_BUFFER_SIZE)

        fun walk(current: File, relPrefix: String) {
            val children = current.listFiles()?.sortedBy { it.name }.orEmpty()
            for (child in children) {
                val relPath = if (relPrefix.isEmpty()) child.name else "$relPrefix/${child.name}"
                if (child.isDirectory) {
                    val dirMod = child.lastModified()
                    if (dirMod > maxModified) maxModified = dirMod
                    walk(child, relPath)
                } else {
                    val isYuzuMeta = child.name.equals(".yuzu_save_size", ignoreCase = true)
                    if (!isYuzuMeta) count++
                    val len = child.length()
                    val mod = child.lastModified()
                    totalSize += len
                    if (mod > maxModified && !isYuzuMeta) maxModified = mod

                    digest.update(relPath.toByteArray(Charsets.UTF_8))
                    digest.update(len.toString().toByteArray(Charsets.UTF_8))
                    runCatching {
                        BufferedInputStream(FileInputStream(child)).use { input ->
                            var read: Int
                            while (input.read(buffer).also { read = it } != -1) {
                                digest.update(buffer, 0, read)
                            }
                        }
                    }
                }
            }
        }

        walk(dir, "")
        if (maxModified == 0L) {
            val rootMod = dir.lastModified()
            if (rootMod > 0L) maxModified = rootMod
        }
        val hashHex = digest.digest().joinToString("") { "%02x".format(it) }.take(16)
        return DirStats(
            lastModifiedMs = maxModified,
            totalSizeBytes = totalSize,
            fileCount = count,
            contentHash = hashHex
        )
    }
}
