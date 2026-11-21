package com.jotasoft.edensavesync

import com.jotasoft.edensavesync.data.cloud.model.DriveFileMetadata
import com.jotasoft.edensavesync.data.mapper.DriveFileMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class DriveFileMapperTest {

    @Test
    fun toRemoteSaveGame_mapsMetadataCorrectly() {
        val metadata = DriveFileMetadata(
            id = "drive_123",
            name = "0100F2C0115B6000.zip",
            appProperties = mapOf(
                "titleId" to "0100F2C0115B6000",
                "gameName" to "Zelda Tears of the Kingdom",
                "lastModifiedMs" to "1700000000000",
                "totalSizeBytes" to "2048",
                "fileCount" to "5",
                "contentHash" to "abcd1234efgh5678",
                "sourceDeviceName" to "Samsung Galaxy Z Fold 8",
                "uploadedAtMs" to "1700000010000"
            )
        )

        val remote = DriveFileMapper.toRemoteSaveGame(metadata)
        assertNotNull(remote)
        assertEquals("0100F2C0115B6000", remote?.titleId)
        assertEquals("Zelda Tears of the Kingdom", remote?.gameName)
        assertEquals("drive_123", remote?.driveFileId)
        assertEquals(1700000000000L, remote?.lastModifiedMs)
        assertEquals("abcd1234efgh5678", remote?.contentHash)
        assertEquals("Samsung Galaxy Z Fold 8", remote?.sourceDeviceName)
    }
}
