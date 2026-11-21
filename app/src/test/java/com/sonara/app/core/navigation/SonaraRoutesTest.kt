package com.sonara.app.core.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SonaraRoutesTest {

    @Test
    fun plainTextIsLeftAlone() {
        assertEquals("Radiohead", SonaraRoutes.encode("Radiohead"))
        assertEquals("a-b_c.d~e", SonaraRoutes.encode("a-b_c.d~e"))
    }

    @Test
    fun reservedAndNonAsciiCharactersArePercentEncoded() {
        assertEquals("AC%2FDC", SonaraRoutes.encode("AC/DC"))
        assertEquals("a%20%26%20b", SonaraRoutes.encode("a & b"))
        assertEquals("%C3%A9", SonaraRoutes.encode("é"))
        assertEquals("100%25", SonaraRoutes.encode("100%"))
        assertEquals("Music%2FRock%2F", SonaraRoutes.encode("Music/Rock/"))
    }

    @Test
    fun encodedValueNeverContainsRouteSyntax() {
        val encoded = SonaraRoutes.encode("x?y=z&w/{v}#")
        listOf('?', '&', '=', '/', '{', '}', '#', ' ').forEach { assertFalse(it in encoded) }
    }

    @Test
    fun builtRoutesMatchTheirPatterns() {
        assertEquals("album/12", SonaraRoutes.album(12))
        assertEquals("artist?name=AC%2FDC", SonaraRoutes.artist("AC/DC"))
        assertEquals("genre?name=Hip%20Hop", SonaraRoutes.genre("Hip Hop"))
        assertEquals("folder?path=Music%2F&recursive=true", SonaraRoutes.folder("Music/", true))
        assertTrue(SonaraRoutes.ALBUM.contains("{albumId}"))
        assertTrue(SonaraRoutes.FOLDER.contains("{path}") && SonaraRoutes.FOLDER.contains("{recursive}"))
    }

    @Test
    fun routePatternsDoNotCollideWithDestinations() {
        val patterns = listOf(SonaraRoutes.ALBUM, SonaraRoutes.ARTIST, SonaraRoutes.GENRE, SonaraRoutes.FOLDER)
        val taken = SonaraDestination.entries.map { it.route } + SonaraDestination.TABS_ROUTE
        patterns.forEach { pattern -> assertFalse(taken.any { pattern == it }) }
        assertEquals(patterns.size, patterns.toSet().size)
    }

    @Test
    fun playlistRouteCarriesTheId() {
        assertEquals("playlist/42", SonaraRoutes.playlist(42))
    }

    @Test
    fun playlistSongPickerRouteCarriesTheId() {
        assertEquals("playlist/42/add", SonaraRoutes.playlistAdd(42))
        assertEquals("playlist/{playlistId}/add", SonaraRoutes.PLAYLIST_ADD)
    }
}
