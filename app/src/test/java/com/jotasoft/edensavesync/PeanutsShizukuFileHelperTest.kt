package com.jotasoft.edensavesync

import com.jotasoft.edensavesync.data.storage.shizuku.PeanutsShizukuFileHelper
import com.jotasoft.edensavesync.domain.model.LocalSaveSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class PeanutsShizukuFileHelperTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val titleId = "0100606026F12000"

    @Test
    fun scanExportAndImport_preservesFilesAndIdenticalHash() {
        val sourceSavesDir = tempFolder.newFolder("peanuts_src", "saves")
        val gameDir = File(sourceSavesDir, titleId).apply { mkdirs() }
        File(gameDir, "save01.bin").writeBytes(byteArrayOf(1, 2, 3, 4, 5))
        File(gameDir, "nested").apply { mkdirs() }
        File(gameDir, "nested/slot.dat").writeText("ff-resonance-progress")

        val scanned = PeanutsShizukuFileHelper.scanSaves(sourceSavesDir)
        assertEquals(1, scanned.size)
        val original = scanned.first()
        assertEquals(titleId, original.titleId)
        assertEquals(LocalSaveSource.PEANUTS_NATIVE, original.source)
        assertEquals(2, original.fileCount)
        assertTrue(original.contentHash.length == 16)

        // Round-trip JSON serialization used over AIDL
        val decoded = PeanutsShizukuFileHelper.fromJsonArray(PeanutsShizukuFileHelper.toJsonArray(scanned))
        assertEquals(scanned, decoded)

        // Export to ZIP and import into a second saves directory
        val zipFile = File(tempFolder.root, "export.zip")
        assertTrue(PeanutsShizukuFileHelper.exportTitleSaveToZip(sourceSavesDir, titleId, zipFile))

        val targetSavesDir = tempFolder.newFolder("peanuts_dst", "saves")
        assertTrue(PeanutsShizukuFileHelper.importTitleSaveFromZip(targetSavesDir, titleId, zipFile))

        val imported = PeanutsShizukuFileHelper.scanSingleSave(targetSavesDir, titleId)
        assertNotNull(imported)
        assertEquals(original.contentHash, imported!!.contentHash)
        assertEquals(original.fileCount, imported.fileCount)
        assertEquals(original.totalSizeBytes, imported.totalSizeBytes)
    }

    @Test
    fun importTitleSaveFromZip_rejectsZipSlipAndPreservesExistingSave() {
        val savesDir = tempFolder.newFolder("zipslip", "saves")
        val existingDir = File(savesDir, titleId).apply { mkdirs() }
        val existingFile = File(existingDir, "good_save.bin").apply { writeText("original-data") }

        val maliciousZip = File(tempFolder.root, "malicious.zip")
        ZipOutputStream(FileOutputStream(maliciousZip)).use { zos ->
            zos.putNextEntry(ZipEntry("../escaped.txt"))
            zos.write("pwned".toByteArray())
            zos.closeEntry()
        }

        val ok = PeanutsShizukuFileHelper.importTitleSaveFromZip(savesDir, titleId, maliciousZip)
        assertFalse(ok)
        assertFalse(File(savesDir, "escaped.txt").exists())
        assertTrue(existingFile.exists())
        assertEquals("original-data", existingFile.readText())
    }

    @Test
    fun importTitleSaveFromZip_whenZipCorruptOrEmpty_returnsFalseWithoutDeletingExistingSave() {
        val savesDir = tempFolder.newFolder("corrupt", "saves")
        val existingDir = File(savesDir, titleId).apply { mkdirs() }
        val existingFile = File(existingDir, "slot0.bin").apply { writeText("safe-progress") }

        val corruptZip = File(tempFolder.root, "corrupt.zip").apply {
            writeBytes(ByteArray(64) { 0x42 })
        }
        assertFalse(PeanutsShizukuFileHelper.importTitleSaveFromZip(savesDir, titleId, corruptZip))
        assertTrue(existingFile.exists())
        assertEquals("safe-progress", existingFile.readText())

        val dirOnlyZip = File(tempFolder.root, "empty_dir_only.zip")
        ZipOutputStream(FileOutputStream(dirOnlyZip)).use { zos ->
            zos.putNextEntry(ZipEntry("empty_folder/"))
            zos.closeEntry()
        }
        assertFalse(PeanutsShizukuFileHelper.importTitleSaveFromZip(savesDir, titleId, dirOnlyZip))
        assertTrue(existingFile.exists())
        assertEquals("safe-progress", existingFile.readText())
    }

    @Test
    fun scanSaves_deduplicatesByTitleIdKeepingMostRecent() {
        val savesDir = tempFolder.newFolder("dedup", "saves")
        val olderDir = File(savesDir, titleId).apply { mkdirs() }
        val olderFile = File(olderDir, "save.bin").apply {
            writeText("old-save")
            setLastModified(1_700_000_000_000L)
        }
        val newerDir = File(savesDir, "$titleId (Final Fantasy)").apply { mkdirs() }
        val newerFile = File(newerDir, "save.bin").apply {
            writeText("new-save-progress")
            setLastModified(1_700_050_000_000L)
        }
        // Asegurar timestamps explícitos
        assertTrue(olderFile.lastModified() < newerFile.lastModified())

        val scanned = PeanutsShizukuFileHelper.scanSaves(savesDir)
        assertEquals(1, scanned.size)
        assertEquals(titleId, scanned.first().titleId)
        assertEquals(newerFile.length(), scanned.first().totalSizeBytes)
    }
}
