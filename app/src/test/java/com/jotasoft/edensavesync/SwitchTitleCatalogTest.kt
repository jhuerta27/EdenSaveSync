package com.jotasoft.edensavesync

import com.jotasoft.edensavesync.data.catalog.SwitchTitleCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.File

class SwitchTitleCatalogTest {

    @Before
    fun setUp() {
        SwitchTitleCatalog.clearCache()
    }

    @Test
    fun catalogJsonFile_isValidAndContainsExpectedTitles() {
        val file = File("../catalog/titles.json").takeIf { it.exists() }
            ?: File("catalog/titles.json")
        val content = file.readText()
        val parsed = SwitchTitleCatalog.parseCatalogJson(content)
        assertNotNull(parsed)
        assertEquals("The Legend of Zelda: Tears of the Kingdom", parsed?.get("0100F2C0115B6000"))
    }

    @Test
    fun resolveName_returnsCatalogNameOrCustomOverride() {
        SwitchTitleCatalog.init(
            context = null,
            cachedJson = """{"0100F2C0115B6000": "The Legend of Zelda: Tears of the Kingdom"}"""
        )
        val totk = SwitchTitleCatalog.resolveName("0100f2c0115b6000", emptyMap())
        assertEquals("The Legend of Zelda: Tears of the Kingdom", totk)

        val custom = SwitchTitleCatalog.resolveName(
            "0100F2C0115B6000",
            mapOf("0100F2C0115B6000" to "Zelda TOTK (Partida 100%)")
        )
        assertEquals("Zelda TOTK (Partida 100%)", custom)

        val directTitle = SwitchTitleCatalog.resolveTitle("0100f2c0115b6000")
        assertEquals("The Legend of Zelda: Tears of the Kingdom", directTitle)
        assertNull(SwitchTitleCatalog.resolveTitle("0100999999999999"))
    }

    @Test
    fun resolveName_returnsFallbackForUnknownTitle() {
        val unknown = SwitchTitleCatalog.resolveName("0100999999999999", emptyMap())
        assertEquals("Switch Game (0100999999999999)", unknown)
    }

    @Test
    fun parseCatalogJson_parsesValidJson() {
        val json = """
            {
                "0100123456789ABC": "New Remote Game",
                "0100ABCDEF123456": "Another Remote Game"
            }
        """.trimIndent()
        val parsed = SwitchTitleCatalog.parseCatalogJson(json)
        assertNotNull(parsed)
        assertEquals("New Remote Game", parsed?.get("0100123456789ABC"))
        assertEquals("Another Remote Game", parsed?.get("0100ABCDEF123456"))
    }

    @Test
    fun parseCatalogJson_handlesInvalidJsonGracefully() {
        assertNull(SwitchTitleCatalog.parseCatalogJson("not a json"))
    }

    @Test
    fun init_updatesCacheAndPreservesPrecedence() {
        val json = """
            {
                "0100999988887777": "Cached Remote Game",
                "0100F2C0115B6000": "Remote Updated Zelda Name"
            }
        """.trimIndent()

        SwitchTitleCatalog.init(context = null, cachedJson = json)

        val remoteZelda = SwitchTitleCatalog.resolveName("0100f2c0115b6000", emptyMap())
        assertEquals("Remote Updated Zelda Name", remoteZelda)

        val cachedGame = SwitchTitleCatalog.resolveName("0100999988887777", emptyMap())
        assertEquals("Cached Remote Game", cachedGame)

        val customOverride = SwitchTitleCatalog.resolveName(
            "0100999988887777",
            mapOf("0100999988887777" to "User Custom Override")
        )
        assertEquals("User Custom Override", customOverride)
    }
}
