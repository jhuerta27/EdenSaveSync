package com.jotasoft.edensavesync.data.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.webkit.MimeTypeMap
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.jotasoft.edensavesync.domain.model.LocalSaveGame
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
class EdenStorageManager @Inject constructor(private val context: Context) {

    companion object {
        val EDEN_PACKAGES = listOf(
            "dev.eden.eden_emulator",
            "dev.eden.eden_emulator.ea",
            "org.yuzu.yuzu_emu"
        )
        private val HEX_16_REGEX = Regex("^[0-9A-Fa-f]{16}$")
        private val HEX_32_REGEX = Regex("^[0-9A-Fa-f]{32}$")
        private const val ZERO_USER_DIR = "0000000000000000"
    }

    /**
     * Detecta si Eden está instalado en el dispositivo y devuelve su packageName.
     */
    fun detectInstalledEdenPackage(): String {
        val pm = context.packageManager
        for (pkg in EDEN_PACKAGES) {
            try {
                pm.getPackageInfo(pkg, 0)
                return pkg
            } catch (_: Exception) {
            }
        }
        return EDEN_PACKAGES.first()
    }

    /**
     * Construye el Intent para abrir el selector de carpetas SAF apuntando directamente
     * a Android/data/dev.eden.eden_emulator/files/nand/user/save o al DocumentsProvider de Eden.
     */
    fun createOpenEdenFolderIntent(useEdenProvider: Boolean = false): Intent {
        val pkg = detectInstalledEdenPackage()
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION or
                        Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
            )
        }

        val initialUri = if (useEdenProvider) {
            "content://$pkg.documents/tree/root".toUri()
        } else {
            val encodedPath = Uri.encode("Android/data/$pkg/files/nand/user/save")
            "content://com.android.externalstorage.documents/tree/primary%3A$encodedPath/document/primary%3A$encodedPath".toUri()
        }

        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, initialUri)
        return intent
    }

    /**
     * Localiza el directorio `0000000000000000` (donde residen los perfiles de usuario de Switch)
     * o directamente un directorio de perfil de 32 caracteres hexadecimales, sin importar qué nivel
     * de la carpeta de Eden haya seleccionado el usuario.
     */
    private fun resolveProfileContainers(rootDoc: DocumentFile): List<DocumentFile> {
        val name = rootDoc.name.orEmpty()

        // Caso 1: El usuario seleccionó directamente la carpeta de un perfil (<32-hex>)
        if (HEX_32_REGEX.matches(name) && name != ZERO_USER_DIR.repeat(2)) {
            return listOf(rootDoc)
        }

        // Caso 2: El usuario seleccionó `0000000000000000`
        if (name.equals(ZERO_USER_DIR, ignoreCase = true)) {
            return findProfileDirsInsideZeroDir(rootDoc)
        }

        // Caso 3: Navegar hacia abajo desde raíz de Eden / files / nand / user / save
        var current: DocumentFile? = rootDoc
        val steps = listOf("files", "nand", "user", "save", ZERO_USER_DIR)
        for (step in steps) {
            val child = current?.findFileIgnoreCase(step)
            if (child != null && child.isDirectory) {
                current = child
            }
        }

        if (current != null && current.name.equals(ZERO_USER_DIR, ignoreCase = true)) {
            return findProfileDirsInsideZeroDir(current)
        }

        // Caso 4: ¿Ya estamos en `save` y tiene `0000000000000000` dentro?
        val zeroChild = rootDoc.findFileIgnoreCase(ZERO_USER_DIR)
        if (zeroChild != null && zeroChild.isDirectory) {
            return findProfileDirsInsideZeroDir(zeroChild)
        }

        // Caso 5: ¿La carpeta seleccionada contiene directamente subcarpetas de perfiles 32-hex?
        val directProfiles = rootDoc.listFiles().filter {
            it.isDirectory && HEX_32_REGEX.matches(it.name.orEmpty()) &&
                    !it.name.equals(ZERO_USER_DIR.repeat(2), ignoreCase = true)
        }
        if (directProfiles.isNotEmpty()) {
            return directProfiles
        }

        // Caso 6: Si el usuario seleccionó `save` pero aún está vacío, creamos la estructura base
        if (name.equals("save", ignoreCase = true)) {
            val createdZero = rootDoc.createDirectory(ZERO_USER_DIR)
            if (createdZero != null) {
                return findProfileDirsInsideZeroDir(createdZero)
            }
        }

        return listOf(rootDoc)
    }

    private fun findProfileDirsInsideZeroDir(zeroDir: DocumentFile): List<DocumentFile> {
        val profiles = zeroDir.listFiles().filter {
            it.isDirectory && HEX_32_REGEX.matches(it.name.orEmpty()) &&
                    !it.name.equals(ZERO_USER_DIR.repeat(2), ignoreCase = true)
        }
        return profiles
    }

    /**
     * Escanea todas las partidas guardadas locales en la carpeta vinculada de Eden.
     */
    fun scanLocalSaves(treeUri: Uri): Pair<String, List<LocalSaveGame>> {
        val rootDoc = DocumentFile.fromTreeUri(context, treeUri)
            ?: return "" to emptyList()

        val profileDirs = resolveProfileContainers(rootDoc)
        val savesByTitle = mutableMapOf<String, LocalSaveGame>()
        var primaryProfileId = profileDirs.firstOrNull()?.name.orEmpty()

        for (profileDir in profileDirs) {
            val profileId = profileDir.name.orEmpty()
            val children = profileDir.listFiles()
            for (titleDir in children) {
                val rawName = titleDir.name.orEmpty()
                val titleName = rawName.substringBefore(" (").trim().uppercase()
                if (!titleDir.isDirectory || !HEX_16_REGEX.matches(titleName)) continue

                val stats = computeDirectoryStats(titleDir)
                if (stats.fileCount == 0) continue

                val candidate = LocalSaveGame(
                    titleId = titleName,
                    userProfileId = profileId,
                    lastModifiedMs = stats.lastModifiedMs,
                    totalSizeBytes = stats.totalSizeBytes,
                    fileCount = stats.fileCount,
                    contentHash = stats.contentHash
                )

                val existing = savesByTitle[titleName]
                if (existing == null || candidate.lastModifiedMs > existing.lastModifiedMs) {
                    savesByTitle[titleName] = candidate
                    primaryProfileId = profileId
                }
            }
        }

        return primaryProfileId to savesByTitle.values.sortedByDescending { it.lastModifiedMs }
    }

    /**
     * Empaqueta la carpeta de guardado de un `<TitleID>` en un archivo `.zip` temporal para subir a Google Drive.
     */
    fun exportTitleSaveToZip(treeUri: Uri, titleId: String, targetZipFile: File): Boolean {
        val rootDoc = DocumentFile.fromTreeUri(context, treeUri) ?: return false
        val profileDirs = resolveProfileContainers(rootDoc)

        val titleDir = profileDirs
            .mapNotNull { profile ->
                profile.findFileIgnoreCase(titleId)
                    ?: profile.listFiles().firstOrNull {
                        it.isDirectory && it.name.orEmpty().startsWith("$titleId (", ignoreCase = true)
                    }
            }
            .maxByOrNull { computeDirectoryStats(it).fileCount }
            ?: return false

        val stats = computeDirectoryStats(titleDir)
        if (stats.fileCount == 0) return false

        targetZipFile.parentFile?.mkdirs()
        if (targetZipFile.exists()) targetZipFile.delete()

        ZipOutputStream(BufferedOutputStream(FileOutputStream(targetZipFile))).use { zos ->
            zipDocumentDirectory(titleDir, "", zos)
        }
        if (targetZipFile.exists() && targetZipFile.length() <= 22L) {
            targetZipFile.delete()
            return false
        }
        return targetZipFile.exists() && targetZipFile.length() > 22L
    }

    /**
     * Importa un `.zip` descargado de Google Drive dentro del perfil activo local en este dispositivo.
     * Antes de sobrescribir, guarda automáticamente un backup local por seguridad.
     */
    fun importTitleSaveFromZip(treeUri: Uri, titleId: String, sourceZipFile: File): Boolean {
        if (!sourceZipFile.exists() || sourceZipFile.length() <= 22L) {
            return false
        }
        val normalizedTitleId = titleId.uppercase()
        val rootDoc = DocumentFile.fromTreeUri(context, treeUri) ?: return false

        // 1. Crear backup local de la partida existente si la hay
        createLocalBackupIfPresent(treeUri, normalizedTitleId)

        // 2. Localizar el directorio de perfil de usuario activo en este dispositivo
        val targetProfileDir = resolveOrCreateActiveProfileDir(rootDoc) ?: return false

        // Limpiar cualquier carpeta residual duplicada como "0100... (1)" de intentos previos
        targetProfileDir.listFiles().forEach { child ->
            val childName = child.name.orEmpty()
            if (child.isDirectory && childName.startsWith("$normalizedTitleId (", ignoreCase = true)) {
                deleteRecursively(child)
            }
        }

        // 3. Obtener o crear la carpeta del TitleID sin borrar el directorio en sí (evita duplicados " (1)")
        val targetTitleDir = targetProfileDir.findFileIgnoreCase(normalizedTitleId)
            ?: targetProfileDir.createDirectory(normalizedTitleId)
            ?: return false

        // Vaciar el contenido anterior de targetTitleDir para garantizar una extracción limpia
        targetTitleDir.listFiles().forEach { child ->
            deleteRecursively(child)
        }

        // Caché en memoria de subdirectorios creados para resolución O(1) sin saturar Binder
        val dirCache = mutableMapOf<String, DocumentFile>()
        dirCache[""] = targetTitleDir

        fun resolveDir(relativePath: String): DocumentFile? {
            val normalized = relativePath.trim('/', '\\').replace('\\', '/')
            if (normalized.isEmpty()) return targetTitleDir
            dirCache[normalized]?.let { return it }

            val parts = normalized.split('/').filter { it.isNotEmpty() }
            var currentPath = ""
            var currentDoc: DocumentFile = targetTitleDir

            for (part in parts) {
                currentPath = if (currentPath.isEmpty()) part else "$currentPath/$part"
                val cached = dirCache[currentPath]
                if (cached != null) {
                    currentDoc = cached
                } else {
                    val nextDoc = currentDoc.findFile(part) ?: currentDoc.createDirectory(part)
                    if (nextDoc == null) return null
                    dirCache[currentPath] = nextDoc
                    currentDoc = nextDoc
                }
            }
            return currentDoc
        }

        // 4. Extraer el contenido del ZIP dentro de targetTitleDir
        ZipInputStream(BufferedInputStream(FileInputStream(sourceZipFile))).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            val buffer = ByteArray(32 * 1024)
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

    /**
     * Crea una copia de seguridad local antes de sobrescribir una partida (mantiene las 5 más recientes).
     */
    private fun createLocalBackupIfPresent(treeUri: Uri, titleId: String) {
        try {
            val backupDir = File(context.filesDir, "backups/${titleId.uppercase()}")
            backupDir.mkdirs()
            val backupFile = File(backupDir, "backup_${System.currentTimeMillis()}.zip")
            val exported = exportTitleSaveToZip(treeUri, titleId, backupFile)
            if ((!exported || backupFile.length() <= 100L) && backupFile.exists()) {
                backupFile.delete()
            }
            // Conservar máximo 5 backups por juego
            val allBackups = backupDir.listFiles { f -> f.extension == "zip" }
                ?.sortedByDescending { it.lastModified() }
                .orEmpty()
            if (allBackups.size > 5) {
                allBackups.drop(5).forEach { it.delete() }
            }
        } catch (_: Exception) {
        }
    }

    fun getLocalBackupCount(titleId: String): Int {
        val backupDir = File(context.filesDir, "backups/${titleId.uppercase()}")
        return backupDir.listFiles { f -> f.extension == "zip" }?.size ?: 0
    }

    fun restoreLatestLocalBackup(treeUri: Uri, titleId: String): Boolean {
        val backupDir = File(context.filesDir, "backups/${titleId.uppercase()}")
        val latestBackup = backupDir.listFiles { f -> f.extension == "zip" }
            ?.maxByOrNull { it.lastModified() }
            ?: return false
        return importTitleSaveFromZip(treeUri, titleId, latestBackup)
    }

    private fun resolveOrCreateActiveProfileDir(rootDoc: DocumentFile): DocumentFile? {
        val profiles = resolveProfileContainers(rootDoc)
        if (profiles.isNotEmpty()) {
            // Elegir el perfil que tenga más juegos guardados en este dispositivo
            return profiles.maxByOrNull { profile ->
                profile.listFiles()
                    .count { it.isDirectory && HEX_16_REGEX.matches(it.name.orEmpty()) }
            } ?: profiles.first()
        }

        // Si `0000000000000000` existe pero aún no tiene perfil, crear o buscar
        val zeroDir = rootDoc.findFileIgnoreCase(ZERO_USER_DIR) ?: rootDoc
        val defaultProfileName = "00000001000000000000000000000000"
        return zeroDir.findFileIgnoreCase(defaultProfileName)
            ?: zeroDir.createDirectory(defaultProfileName)
    }

    private fun findOrCreateSubdirectory(
        baseDir: DocumentFile,
        relativePath: String
    ): DocumentFile? {
        var current: DocumentFile? = baseDir
        val parts = relativePath.split('/').filter { it.isNotEmpty() }
        for (part in parts) {
            val next = current?.findFile(part) ?: current?.createDirectory(part)
            current = next
        }
        return current
    }

    private fun deleteRecursively(doc: DocumentFile) {
        if (doc.isDirectory) {
            doc.listFiles().forEach { child ->
                deleteRecursively(child)
            }
        }
        doc.delete()
    }

    private fun zipDocumentDirectory(
        dir: DocumentFile,
        parentPath: String,
        zos: ZipOutputStream
    ) {
        val buffer = ByteArray(32 * 1024)
        val children = dir.listFiles().sortedBy { it.name.orEmpty() }
        for (child in children) {
            val childName = child.name ?: continue
            val entryPath = if (parentPath.isEmpty()) childName else "$parentPath/$childName"
            if (child.isDirectory) {
                val dirEntry = ZipEntry("$entryPath/")
                val mod = child.lastModified()
                if (mod > 0L) dirEntry.time = mod
                zos.putNextEntry(dirEntry)
                zos.closeEntry()
                zipDocumentDirectory(child, entryPath, zos)
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
            }
        }
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
        val buffer = ByteArray(32 * 1024)

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
                    if (!isYuzuMeta) {
                        count++
                    }
                    val len = child.length()
                    val mod = child.lastModified()
                    totalSize += len
                    if (mod > maxModified && !isYuzuMeta) maxModified = mod

                    digest.update(relPath.toByteArray(Charsets.UTF_8))
                    digest.update(len.toString().toByteArray(Charsets.UTF_8))
                    // Hash del contenido del archivo para garantizar exactitud entre dispositivos
                    try {
                        context.contentResolver.openInputStream(child.uri)?.use { input ->
                            var read: Int
                            while (input.read(buffer).also { read = it } != -1) {
                                digest.update(buffer, 0, read)
                            }
                        }
                    } catch (_: Exception) {
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

    private fun DocumentFile.findFileIgnoreCase(targetName: String): DocumentFile? {
        findFile(targetName)?.let { return it }
        return listFiles().firstOrNull { it.name.equals(targetName, ignoreCase = true) }
    }
}
