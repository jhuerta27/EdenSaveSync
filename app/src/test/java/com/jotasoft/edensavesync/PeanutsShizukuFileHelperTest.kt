package com.jotasoft.edensavesync

import com.jotasoft.edensavesync.data.storage.shizuku.PeanutsShizukuFileHelper
import com.jotasoft.edensavesync.domain.model.LocalSaveSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

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
}
