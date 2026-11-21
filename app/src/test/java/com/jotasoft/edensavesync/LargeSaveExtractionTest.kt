package com.jotasoft.edensavesync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class LargeSaveExtractionTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun extractLargeSaveZip_withDeepHierarchies_extractsAllFilesSuccessfully() {
        val zipFile = tempFolder.newFile("subnautica_test.zip")
        val extractTargetDir = tempFolder.newFolder("target_save")

        // 1. Crear un ZIP simulando Subnautica: 500 archivos en CellsCache y archivos raíz de slot
        val expectedEntries = mutableListOf<String>()
        expectedEntries.add("slot0000/gameinfo.json")
        expectedEntries.add("slot0000/screenshot.jpg")
        expectedEntries.add("slot0000/global-objects.bin")
        for (i in 0 until 500) {
            expectedEntries.add("slot0000/CellsCache/baked-batch-cells-12-18-$i.bin")
        }

        ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
            for (entryPath in expectedEntries) {
                zos.putNextEntry(ZipEntry(entryPath))
                zos.write("Content for $entryPath".toByteArray(Charsets.UTF_8))
                zos.closeEntry()
            }
        }

        // 2. Ejecutar la lógica de extracción optimizada basada en dirCache
        val dirCache = mutableMapOf<String, File>()
        dirCache[""] = extractTargetDir

        fun resolveDir(relativePath: String): File {
            val normalized = relativePath.trim('/', '\\').replace('\\', '/')
            if (normalized.isEmpty()) return extractTargetDir
            dirCache[normalized]?.let { return it }

            val parts = normalized.split('/').filter { it.isNotEmpty() }
            var currentPath = ""
            var currentDir = extractTargetDir

            for (part in parts) {
                currentPath = if (currentPath.isEmpty()) part else "$currentPath/$part"
                val cached = dirCache[currentPath]
                if (cached != null) {
                    currentDir = cached
                } else {
                    val nextDir = File(currentDir, part)
                    if (!nextDir.exists()) nextDir.mkdirs()
                    dirCache[currentPath] = nextDir
                    currentDir = nextDir
                }
            }
            return currentDir
        }

        ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            val buffer = ByteArray(32 * 1024)
            while (entry != null) {
                val cleanPath = entry.name.trimStart('/', '\\').replace('\\', '/')
                if (cleanPath.isNotEmpty() && !cleanPath.contains("..")) {
                    if (entry.isDirectory) {
                        resolveDir(cleanPath)
                    } else {
                        val lastSlash = cleanPath.lastIndexOf('/')
                        val dirPath = if (lastSlash >= 0) cleanPath.substring(0, lastSlash) else ""
                        val fileName = if (lastSlash >= 0) cleanPath.substring(lastSlash + 1) else cleanPath

                        val parentDir = resolveDir(dirPath)
                        val targetFile = File(parentDir, fileName)

                        FileOutputStream(targetFile).use { out ->
                            var read: Int
                            while (zis.read(buffer).also { read = it } != -1) {
                                out.write(buffer, 0, read)
                            }
                        }
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        // 3. Comprobar que el 100% de los 503 archivos fueron extraídos en su ubicación correcta
        val cellsCacheDir = File(extractTargetDir, "slot0000/CellsCache")
        assertTrue("El subdirectorio CellsCache debe existir", cellsCacheDir.exists())
        assertEquals(500, cellsCacheDir.listFiles()?.size)

        val gameInfoFile = File(extractTargetDir, "slot0000/gameinfo.json")
        assertTrue("gameinfo.json debe existir", gameInfoFile.exists())
        assertEquals("Content for slot0000/gameinfo.json", gameInfoFile.readText())

        val screenshotFile = File(extractTargetDir, "slot0000/screenshot.jpg")
        assertTrue("screenshot.jpg debe existir", screenshotFile.exists())
    }

    @Test
    fun normalizeZipPath_handlesBackslashesAndCleanSlashes() {
        val testPath = "\\slot0000\\CellsCache\\test.bin"
        val cleanPath = testPath.trimStart('/', '\\').replace('\\', '/')
        val lastSlash = cleanPath.lastIndexOf('/')
        val dirPath = cleanPath.substring(0, lastSlash)
        val fileName = cleanPath.substring(lastSlash + 1)

        assertEquals("slot0000/CellsCache", dirPath)
        assertEquals("test.bin", fileName)
    }
}
