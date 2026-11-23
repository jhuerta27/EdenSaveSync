package com.jotasoft.edensavesync.data.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.webkit.MimeTypeMap
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.jotasoft.edensavesync.domain.model.LocalSaveGame
import com.jotasoft.edensavesync.domain.model.LocalSaveSource
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EdenStorageManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        val EDEN_PACKAGES = listOf(
            "dev.eden.eden_emulator",
            "dev.eden.eden_emulator.ea",
            "org.yuzu.yuzu_emu"
        )
        const val PEANUTS_PACKAGE = "dev.nxnative.android.peanuts"
        private val HEX_16_REGEX = Regex("^[0-9A-Fa-f]{16}$")
        private val HEX_32_REGEX = Regex("^[0-9A-Fa-f]{32}$")
        private const val ZERO_USER_DIR = "0000000000000000"
        private const val IO_BUFFER_SIZE = 32 * 1024
    }

    fun detectInstalledEdenPackage(): String =
        EDEN_PACKAGES.firstOrNull { pkg ->
            runCatching { context.packageManager.getPackageInfo(pkg, 0) }.isSuccess
        } ?: EDEN_PACKAGES.first()

    fun createOpenEdenFolderIntent(useEdenProvider: Boolean = false): Intent {
        val pkg = detectInstalledEdenPackage()
        val initialUri = if (useEdenProvider) {
            "content://$pkg.documents/tree/root".toUri()
        } else {
            val encodedPath = Uri.encode("Android/data/$pkg/files/nand/user/save")
            "content://com.android.externalstorage.documents/tree/primary%3A$encodedPath/document/primary%3A$encodedPath".toUri()
        }
        return createOpenTreeIntent(initialUri)
    }

    fun createOpenPeanutsFolderIntent(usePeanutsProvider: Boolean = false): Intent {
        val initialUri = if (usePeanutsProvider) {
            "content://$PEANUTS_PACKAGE.documents/tree/root".toUri()
        } else {
            val encodedPath = Uri.encode("Android/data/$PEANUTS_PACKAGE/files/saves")
            "content://com.android.externalstorage.documents/tree/primary%3A$encodedPath/document/primary%3A$encodedPath".toUri()
        }
        return createOpenTreeIntent(initialUri)
    }

    private fun createOpenTreeIntent(initialUri: Uri): Intent =
        Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
            )
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, initialUri)
        }

    private fun resolveTitleContainers(rootDoc: DocumentFile, source: LocalSaveSource): List<DocumentFile> =
        when (source) {
            LocalSaveSource.EDEN -> resolveProfileContainers(rootDoc)
            LocalSaveSource.PEANUTS_NATIVE -> listOf(resolvePeanutsSavesDir(rootDoc))
        }

    private fun resolvePeanutsSavesDir(rootDoc: DocumentFile): DocumentFile {
        if (rootDoc.name.equals("saves", ignoreCase = true)) {
            return rootDoc
        }
        var current = rootDoc
        for (step in listOf("files", "saves")) {
            val child = current.findFileIgnoreCase(step)
            if (child != null && child.isDirectory) {
                current = child
            }
        }
        return current
    }

    private fun resolveProfileContainers(rootDoc: DocumentFile): List<DocumentFile> {
        val name = rootDoc.name.orEmpty()

        if (HEX_32_REGEX.matches(name) && !name.equals(ZERO_USER_DIR.repeat(2), ignoreCase = true)) {
            return listOf(rootDoc)
        }

        if (name.equals(ZERO_USER_DIR, ignoreCase = true)) {
            return findProfileDirsInsideZeroDir(rootDoc)
        }

        var current: DocumentFile? = rootDoc
        for (step in listOf("files", "nand", "user", "save", ZERO_USER_DIR)) {
            val child = current?.findFileIgnoreCase(step)
            if (child != null && child.isDirectory) {
                current = child
            }
        }

        if (current != null && current.name.equals(ZERO_USER_DIR, ignoreCase = true)) {
            return findProfileDirsInsideZeroDir(current)
        }

        val zeroChild = rootDoc.findFileIgnoreCase(ZERO_USER_DIR)
        if (zeroChild != null && zeroChild.isDirectory) {
            return findProfileDirsInsideZeroDir(zeroChild)
        }

        val directProfiles = findProfileDirsInsideZeroDir(rootDoc)
        if (directProfiles.isNotEmpty()) {
            return directProfiles
        }

        if (name.equals("save", ignoreCase = true)) {
            rootDoc.createDirectory(ZERO_USER_DIR)?.let { createdZero ->
                return findProfileDirsInsideZeroDir(createdZero)
            }
        }

        return listOf(rootDoc)
    }

    private fun findProfileDirsInsideZeroDir(container: DocumentFile): List<DocumentFile> =
        container.listFiles().filter {
            it.isDirectory &&
                HEX_32_REGEX.matches(it.name.orEmpty()) &&
                !it.name.equals(ZERO_USER_DIR.repeat(2), ignoreCase = true)
        }

    private fun findTitleDirectory(containerDir: DocumentFile, titleId: String): DocumentFile? =
        containerDir.findFileIgnoreCase(titleId)
            ?: containerDir.listFiles().firstOrNull {
                it.isDirectory && it.name.orEmpty().startsWith("$titleId (", ignoreCase = true)
            }

    fun scanLocalSaves(
        treeUri: Uri,
        source: LocalSaveSource = LocalSaveSource.EDEN
    ): Pair<String, List<LocalSaveGame>> {
        val rootDoc = DocumentFile.fromTreeUri(context, treeUri) ?: return "" to emptyList()
        val containers = resolveTitleContainers(rootDoc, source)
        val savesByTitle = mutableMapOf<String, LocalSaveGame>()
        var primaryProfileId = if (source == LocalSaveSource.EDEN) {
            containers.firstOrNull()?.name.orEmpty()
        } else {
            ""
        }

        for (container in containers) {
            val profileId = if (source == LocalSaveSource.EDEN) container.name.orEmpty() else ""
            for (titleDir in container.listFiles()) {
                val titleName = titleDir.name.orEmpty().substringBefore(" (").trim().uppercase()
                if (!titleDir.isDirectory || !HEX_16_REGEX.matches(titleName)) continue

                val stats = computeDirectoryStats(titleDir)
                if (stats.fileCount == 0) continue

                val candidate = LocalSaveGame(
                    titleId = titleName,
                    userProfileId = profileId,
                    lastModifiedMs = stats.lastModifiedMs,
                    totalSizeBytes = stats.totalSizeBytes,
                    fileCount = stats.fileCount,
                    contentHash = stats.contentHash,
                    source = source
                )

                val existing = savesByTitle[titleName]
                if (existing == null || candidate.lastModifiedMs > existing.lastModifiedMs) {
                    savesByTitle[titleName] = candidate
                    if (source == LocalSaveSource.EDEN) {
                        primaryProfileId = profileId
                    }
                }
            }
        }

        return primaryProfileId to savesByTitle.values.sortedByDescending { it.lastModifiedMs }
    }

    fun scanSingleTitleSave(
        treeUri: Uri,
        titleId: String,
        source: LocalSaveSource = LocalSaveSource.EDEN
    ): LocalSaveGame? {
        val rootDoc = DocumentFile.fromTreeUri(context, treeUri) ?: return null
        val normalizedTitleId = titleId.uppercase()
        var latest: LocalSaveGame? = null

        for (container in resolveTitleContainers(rootDoc, source)) {
            val titleDir = findTitleDirectory(container, normalizedTitleId) ?: continue
            val stats = computeDirectoryStats(titleDir)
            if (stats.fileCount == 0) continue

            val candidate = LocalSaveGame(
                titleId = normalizedTitleId,
                userProfileId = if (source == LocalSaveSource.EDEN) container.name.orEmpty() else "",
                lastModifiedMs = stats.lastModifiedMs,
                totalSizeBytes = stats.totalSizeBytes,
                fileCount = stats.fileCount,
                contentHash = stats.contentHash,
                source = source
            )
            if (latest == null || candidate.lastModifiedMs > latest.lastModifiedMs) {
                latest = candidate
            }
        }
        return latest
    }

    fun exportTitleSaveToZip(
        treeUri: Uri,
        titleId: String,
        targetZipFile: File,
        source: LocalSaveSource = LocalSaveSource.EDEN
    ): Boolean {
        val rootDoc = DocumentFile.fromTreeUri(context, treeUri) ?: return false
        val candidates = resolveTitleContainers(rootDoc, source).mapNotNull { findTitleDirectory(it, titleId) }
        val titleDir = when {
            candidates.isEmpty() -> return false
            candidates.size == 1 -> candidates.first()
            else -> candidates.maxByOrNull { countSaveFiles(it) } ?: return false
        }

        targetZipFile.parentFile?.mkdirs()
        if (targetZipFile.exists()) targetZipFile.delete()

        val buffer = ByteArray(IO_BUFFER_SIZE)
        val zippedFileCount = ZipOutputStream(BufferedOutputStream(FileOutputStream(targetZipFile))).use { zos ->
            zipDocumentDirectory(titleDir, "", zos, buffer)
        }
        if (zippedFileCount == 0 || !targetZipFile.exists() || targetZipFile.length() <= 22L) {
            targetZipFile.delete()
            return false
        }
        return true
    }

    fun importTitleSaveFromZip(
        treeUri: Uri,
        titleId: String,
        sourceZipFile: File,
        source: LocalSaveSource = LocalSaveSource.EDEN
    ): Boolean {
        if (!sourceZipFile.exists() || sourceZipFile.length() <= 22L) return false

        val normalizedTitleId = titleId.uppercase()
        val rootDoc = DocumentFile.fromTreeUri(context, treeUri) ?: return false

        createLocalBackupIfPresent(treeUri, normalizedTitleId, source)

        val targetContainerDir = when (source) {
            LocalSaveSource.EDEN -> resolveOrCreateActiveProfileDir(rootDoc)
            LocalSaveSource.PEANUTS_NATIVE -> resolvePeanutsSavesDir(rootDoc)
        } ?: return false

        targetContainerDir.listFiles().forEach { child ->
            if (child.isDirectory && child.name.orEmpty().startsWith("$normalizedTitleId (", ignoreCase = true)) {
                deleteRecursively(child)
            }
        }

        val targetTitleDir = targetContainerDir.findFileIgnoreCase(normalizedTitleId)
            ?: targetContainerDir.createDirectory(normalizedTitleId)
            ?: return false

        targetTitleDir.listFiles().forEach(::deleteRecursively)

        val dirCache = mutableMapOf("" to targetTitleDir)

        fun resolveDir(relativePath: String): DocumentFile? {
            val normalized = relativePath.trim('/', '\\').replace('\\', '/')
            if (normalized.isEmpty()) return targetTitleDir
            dirCache[normalized]?.let { return it }

            var currentPath = ""
            var currentDoc: DocumentFile = targetTitleDir

            for (part in normalized.split('/').filter { it.isNotEmpty() }) {
                currentPath = if (currentPath.isEmpty()) part else "$currentPath/$part"
                val nextDoc = dirCache[currentPath]
                    ?: currentDoc.findFile(part)
                    ?: currentDoc.createDirectory(part)
                    ?: return null
                dirCache[currentPath] = nextDoc
                currentDoc = nextDoc
            }
            return currentDoc
        }

        ZipInputStream(BufferedInputStream(FileInputStream(sourceZipFile))).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            val buffer = ByteArray(IO_BUFFER_SIZE)
            while (entry != null) {
                val cleanPath = entry.name.trimStart('/', '\\').replace('\\', '/')
                if (cleanPath.isNotEmpty() && !cleanPath.contains("..")) {
                    val entryPath = if (cleanPath.startsWith("$normalizedTitleId/", ignoreCase = true)) {
                        cleanPath.substring(normalizedTitleId.length + 1)
                    } else {
                        cleanPath
                    }

                    if (entryPath.isNotEmpty()) {
                        if (entry.isDirectory) {
                            resolveDir(entryPath)
                        } else {
                            val lastSlash = entryPath.lastIndexOf('/')
                            val dirPath = if (lastSlash >= 0) entryPath.substring(0, lastSlash) else ""
                            val fileName = if (lastSlash >= 0) entryPath.substring(lastSlash + 1) else entryPath

                            val parentDir = resolveDir(dirPath)
                                ?: throw IllegalStateException("No se pudo preparar el directorio $dirPath para $fileName")

                            val ext = fileName.substringAfterLast('.', "").lowercase()
                            val mimeType = if (ext.isNotEmpty()) {
                                MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
                            } else {
                                "application/octet-stream"
                            }

                            val targetDoc = parentDir.createFile(mimeType, fileName)
                                ?: parentDir.createFile("application/octet-stream", fileName)
                                ?: parentDir.findFile(fileName)
                                ?: throw IllegalStateException("No se pudo crear el archivo $fileName en $dirPath")

                            context.contentResolver.openOutputStream(targetDoc.uri, "w")?.use { out ->
                                var read: Int
                                while (zis.read(buffer).also { read = it } != -1) {
                                    out.write(buffer, 0, read)
                                }
                                out.flush()
                            } ?: throw IllegalStateException("No se pudo abrir el stream de escritura para $fileName")
                        }
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        return true
    }

    private fun backupDirFor(titleId: String): File =
        File(context.filesDir, "backups/${titleId.uppercase()}")

    private fun createLocalBackupIfPresent(treeUri: Uri, titleId: String, source: LocalSaveSource) {
        runCatching {
            val backupDir = backupDirFor(titleId).apply { mkdirs() }
            val backupFile = File(backupDir, "backup_${System.currentTimeMillis()}.zip")
            val exported = exportTitleSaveToZip(treeUri, titleId, backupFile, source)
            if ((!exported || backupFile.length() <= 100L) && backupFile.exists()) {
                backupFile.delete()
            }
            backupDir.listFiles { f -> f.extension == "zip" }
                ?.sortedByDescending { it.lastModified() }
                ?.drop(5)
                ?.forEach { it.delete() }
        }
    }

    fun getLocalBackupCount(titleId: String): Int =
        backupDirFor(titleId).listFiles { f -> f.extension == "zip" }?.size ?: 0

    fun saveBackupFromZip(titleId: String, sourceBackupZip: File) {
        runCatching {
            if (!sourceBackupZip.exists() || sourceBackupZip.length() <= 100L) return
            val backupDir = backupDirFor(titleId).apply { mkdirs() }
            val backupFile = File(backupDir, "backup_${System.currentTimeMillis()}.zip")
            sourceBackupZip.copyTo(backupFile, overwrite = true)
            backupDir.listFiles { f -> f.extension == "zip" }
                ?.sortedByDescending { it.lastModified() }
                ?.drop(5)
                ?.forEach { it.delete() }
        }
    }

    fun getLatestLocalBackupFile(titleId: String): File? =
        backupDirFor(titleId)
            .listFiles { f -> f.extension == "zip" }
            ?.maxByOrNull { it.lastModified() }

    private fun resolveOrCreateActiveProfileDir(rootDoc: DocumentFile): DocumentFile? {
        val profiles = resolveProfileContainers(rootDoc)
        if (profiles.isNotEmpty()) {
            return profiles.maxByOrNull { profile ->
                profile.listFiles().count { it.isDirectory && HEX_16_REGEX.matches(it.name.orEmpty()) }
            } ?: profiles.first()
        }

        val zeroDir = rootDoc.findFileIgnoreCase(ZERO_USER_DIR) ?: rootDoc
        val defaultProfileName = "00000001000000000000000000000000"
        return zeroDir.findFileIgnoreCase(defaultProfileName)
            ?: zeroDir.createDirectory(defaultProfileName)
    }

    private fun deleteRecursively(doc: DocumentFile) {
        if (doc.isDirectory) {
            doc.listFiles().forEach(::deleteRecursively)
        }
        doc.delete()
    }

    private fun countSaveFiles(dir: DocumentFile): Int {
        var count = 0
        for (child in dir.listFiles()) {
            if (child.isDirectory) {
                count += countSaveFiles(child)
            } else if (!child.name.equals(".yuzu_save_size", ignoreCase = true)) {
                count++
            }
        }
        return count
    }

    private fun zipDocumentDirectory(
        dir: DocumentFile,
        parentPath: String,
        zos: ZipOutputStream,
        buffer: ByteArray
    ): Int {
        var fileCount = 0
        val children = dir.listFiles().sortedBy { it.name.orEmpty() }
        for (child in children) {
            val childName = child.name ?: continue
            val entryPath = if (parentPath.isEmpty()) childName else "$parentPath/$childName"
            if (child.isDirectory) {
                val dirEntry = ZipEntry("$entryPath/").apply {
                    val mod = child.lastModified()
                    if (mod > 0L) time = mod
                }
                zos.putNextEntry(dirEntry)
                zos.closeEntry()
                fileCount += zipDocumentDirectory(child, entryPath, zos, buffer)
            } else if (child.isFile) {
                val entry = ZipEntry(entryPath).apply {
                    val mod = child.lastModified()
                    if (mod > 0L) time = mod
                }
                zos.putNextEntry(entry)
                context.contentResolver.openInputStream(child.uri)?.use { input ->
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        zos.write(buffer, 0, read)
                    }
                }
                zos.closeEntry()
                if (!childName.equals(".yuzu_save_size", ignoreCase = true)) {
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

    private fun computeDirectoryStats(dir: DocumentFile): DirStats {
        var maxModified = 0L
        var totalSize = 0L
        var count = 0
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(IO_BUFFER_SIZE)

        fun walk(current: DocumentFile, relPrefix: String) {
            val children = current.listFiles().sortedBy { it.name.orEmpty() }
            for (child in children) {
                val name = child.name ?: continue
                val relPath = if (relPrefix.isEmpty()) name else "$relPrefix/$name"
                if (child.isDirectory) {
                    val dirMod = child.lastModified()
                    if (dirMod > maxModified) maxModified = dirMod
                    walk(child, relPath)
                } else {
                    val isYuzuMeta = name.equals(".yuzu_save_size", ignoreCase = true)
                    if (!isYuzuMeta) count++
                    val len = child.length()
                    val mod = child.lastModified()
                    totalSize += len
                    if (mod > maxModified && !isYuzuMeta) maxModified = mod

                    digest.update(relPath.toByteArray(Charsets.UTF_8))
                    digest.update(len.toString().toByteArray(Charsets.UTF_8))
                    runCatching {
                        context.contentResolver.openInputStream(child.uri)?.use { input ->
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

    private fun DocumentFile.findFileIgnoreCase(targetName: String): DocumentFile? =
        findFile(targetName)
            ?: listFiles().firstOrNull { it.name.equals(targetName, ignoreCase = true) }
}
